#!/usr/bin/env python3
"""Run one k6 measurement and sample local process/database resource counters."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import time

p = argparse.ArgumentParser()
p.add_argument('--vus', type=int, default=1)
p.add_argument('--count', type=int, default=10000)
p.add_argument('--duration', default='30s')
p.add_argument('--label')
p.add_argument('--size', type=int, default=20)
args = p.parse_args()
root = Path(__file__).resolve().parents[2]
out = root / 'target/baseline'
out.mkdir(parents=True, exist_ok=True)
name = f'{args.label}-vu{args.vus}' if args.label else f'{args.count}-vu{args.vus}'
env = os.environ.copy()
dev = (root/'lastcall-api/src/main/resources/application-dev.yml').read_text()
env['MYSQL_PWD'] = os.environ.get('DB_PASSWORD', re.search(r'\$\{DB_PASSWORD:([^}]+)\}',dev).group(1))
mysql = ['mysql','--protocol=TCP','--host=127.0.0.1','--port=3306',
         '--user='+os.environ.get('DB_USERNAME','root'),'-N','-e',
         "SHOW GLOBAL STATUS WHERE Variable_name IN ('Com_select','Threads_connected','Threads_running');"]

def db_status():
    r = subprocess.run(mysql, env=env, capture_output=True, text=True, check=True)
    return {k:int(v) for k,v in (line.split('\t') for line in r.stdout.splitlines())}

before = db_status()
start = time.monotonic()
samples = []
with (out/(name+'.log')).open('w') as log:
    proc = subprocess.Popen(['k6','run','--quiet','--no-usage-report',
        '-e',f'VUS={args.vus}','-e',f'EXPECTED_COUNT={args.count}',
        '-e',f'SIZE={args.size}',
        '-e',f'DURATION={args.duration}','-e',f'REPORT={out/(name+".json")}',
        str(root/'scripts/baseline/browse.js')],stdout=log,stderr=log)
    while proc.poll() is None:
        ps = subprocess.run(['ps','-axo','pid,pcpu,rss,comm'],capture_output=True,text=True,check=True)
        processes = [line.strip() for line in ps.stdout.splitlines()
                     if any(x in line.lower() for x in ['java','mysqld','k6'])]
        samples.append({'seconds':round(time.monotonic()-start,2),
                        'processes_pid_cpu_percent_rss_kib_command':processes,
                        'mysql_global':db_status()})
        try:
            proc.wait(timeout=2)
        except subprocess.TimeoutExpired:
            pass
after = db_status()
elapsed = time.monotonic()-start
report = {'exit_code':proc.returncode,'elapsed_seconds':elapsed,'mysql_before':before,
          'mysql_after':after,'mysql_select_delta':after['Com_select']-before['Com_select'],
          'samples':samples,'note':'MySQL counters cover the shared server, not just this app. '
          'Observer opens a transient connection per sample. ps CPU is OS-reported, not instantaneous.'}
(out/(name+'-resources.json')).write_text(json.dumps(report,indent=2)+'\n')
print(name,'exit:',proc.returncode,'MySQL SELECT delta:',report['mysql_select_delta'])
raise SystemExit(proc.returncode)
