#!/usr/bin/env python3
"""Reproduce inventory races using 100 authenticated HTTP clients and fresh fixtures.

Creates 100 users and one 10-unit listing in the baseline environment per run.
No cleanup/reset, no payments, and no changes to application purchasing logic.
"""
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import threading
import time
from urllib.error import HTTPError
from urllib.request import Request, build_opener, ProxyHandler
from uuid import uuid4

ROOT = Path(__file__).resolve().parents[2]
BASE = 'http://127.0.0.1:8081'


def http(path, payload, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = Request(BASE+path, data=json.dumps(payload).encode(), headers=headers, method='POST')
    try:
        with build_opener(ProxyHandler({})).open(req, timeout=30) as r:
            status, raw = r.status, r.read()
    except HTTPError as e:
        status, raw = e.code, e.read()
    return status, json.loads(raw) if raw else {}


def main():
    run_id = uuid4().hex[:12]
    env = os.environ.copy()
    dev = (ROOT/'lastcall-api/src/main/resources/application-dev.yml').read_text()
    env['MYSQL_PWD'] = os.environ.get('DB_PASSWORD', re.search(r'\$\{DB_PASSWORD:([^}]+)\}',dev).group(1))
    cmd = ['mysql','--protocol=TCP','--host=127.0.0.1','--port=3306',
           '--user='+os.environ.get('DB_USERNAME','root'),'-N','lastcall_eats_baseline']

    def sql(statement):
        return subprocess.run(cmd,input=statement,text=True,env=env,capture_output=True,check=True).stdout.strip()

    print('Run:',run_id,'Preparing 100 distinct users through registration API.',flush=True)
    password = uuid4().hex

    def register(i):
        status, body = http('/api/auth/register/user',
            {'email':f'race-{run_id}-{i}@example.com','password':password,'nickname':f'Race {i}'})
        if status != 200 or body.get('code') != 200:
            raise RuntimeError(f'Registration {i} failed: HTTP {status}')
        return body['data']

    with ThreadPoolExecutor(max_workers=10) as pool:
        users = list(pool.map(register,range(100)))
    # Confirm the HTTP app wrote the fixture users into the intended database.
    if int(sql(f"SELECT COUNT(*) FROM user WHERE email LIKE 'race-{run_id}-%@example.com';")) != 100:
        raise SystemExit('HTTP app does not match the baseline database; stopping before purchases.')
    if len({u['userId'] for u in users}) != 100:
        raise SystemExit('Expected 100 distinct user IDs')
    listing_id = int(sql(f"""
START TRANSACTION;
INSERT INTO product_template (merchant_id,name,description,original_price,is_active)
SELECT MIN(id),'Concurrency {run_id}','Dedicated 10-unit race fixture',20,1 FROM merchant;
SET @race_template=LAST_INSERT_ID();
INSERT INTO product_listing (merchant_id,template_id,discount_price,quantity,remaining_quantity,pickup_start,pickup_end,date,is_available)
SELECT merchant_id,id,10,10,10,'00:00:00','23:59:59',CURRENT_DATE,1 FROM product_template WHERE id=@race_template;
SET @race_listing=LAST_INSERT_ID();
COMMIT;
SELECT @race_listing;
"""))

    def snapshot():
        stock = sql(f'SELECT quantity,remaining_quantity,is_available FROM product_listing WHERE id={listing_id};').split('\t')
        counts = sql(f'SELECT COUNT(*),COUNT(DISTINCT user_id) FROM orders WHERE listing_id={listing_id};').split('\t')
        return {'initial_quantity':int(stock[0]),'remaining_quantity':int(stock[1]),
                'available':int(stock[2]),'orders':int(counts[0]),'distinct_buyers':int(counts[1])}

    before = snapshot()
    if before['remaining_quantity'] != 10 or before['orders'] != 0:
        raise SystemExit('Fresh inventory preconditions failed')
    print('Listing:',listing_id,'Before:',before,flush=True)
    gate = threading.Barrier(101, timeout=30)

    def buy(user):
        gate.wait()
        start = time.perf_counter()
        try:
            status, body = http('/api/orders',{'listingId':listing_id},user['accessToken'])
            data = body.get('data') or {}
            return {'user_id':user['userId'],'start':start,'elapsed_ms':(time.perf_counter()-start)*1000,
                    'http_status':status,'business_code':body.get('code'),
                    'message':body.get('message'),'order_id':data.get('id'),'order_status':data.get('status')}
        except Exception as e:
            return {'user_id':user['userId'],'start':start,'elapsed_ms':(time.perf_counter()-start)*1000,
                    'http_status':None,'error_type':type(e).__name__}

    print('Releasing 100 waiting HTTP client threads together; no retries.',flush=True)
    with ThreadPoolExecutor(max_workers=100) as pool:
        futures = [pool.submit(buy,u) for u in users]
        gate.wait()
        results = [f.result() for f in futures]
    after = snapshot()
    first = min(r['start'] for r in results)
    for r in results:
        r['dispatch_offset_ms'] = (r.pop('start')-first)*1000
    success = sum(r['http_status']==200 and r.get('business_code')==200 for r in results)
    source = ROOT/'lastcall-order/src/main/java/com/lastcalleats/order/service/impl/OrderServiceImpl.java'
    report = {'run_id':run_id,'time_utc':datetime.now(timezone.utc).isoformat(),'base_url':BASE,
              'listing_id':listing_id,'clients':100,'before':before,'after':after,
              'http_successes':success,'http_status_counts':dict(Counter(str(r['http_status']) for r in results)),
              'oversold':after['orders']>10,
              'inventory_consistent':after['orders']+after['remaining_quantity']==10,
              'max_dispatch_offset_ms':max(r['dispatch_offset_ms'] for r in results),
              'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),
              'jar_sha256':hashlib.sha256((ROOT/'lastcall-api/target/lastcall-api-0.0.1-SNAPSHOT.jar').read_bytes()).hexdigest(),
              'results':results,
              'limitations':'Client barrier approximates simultaneous arrival; OS/server/DB pool schedule requests. '
              '100 clients does not imply 100 simultaneously executing DB transactions. No payment or fault injection.'}
    directory = ROOT/'docs/concurrency-results'/run_id
    directory.mkdir(parents=True,exist_ok=True)
    (directory/'result.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:v for k,v in report.items() if k!='results'},indent=2))
    print('Report:',directory/'result.json')


if __name__=='__main__':
    main()
