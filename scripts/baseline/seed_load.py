#!/usr/bin/env python3
"""Append synthetic listings until baseline contains 10,000 rows. Never reset data."""
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
env = os.environ.copy()
dev = (ROOT / 'lastcall-api/src/main/resources/application-dev.yml').read_text()
env['MYSQL_PWD'] = os.environ.get('DB_PASSWORD', re.search(r'\$\{DB_PASSWORD:([^}]+)\}', dev).group(1))
cmd = ['mysql', '--protocol=TCP', '--host=127.0.0.1', '--port=3306',
       '--user=' + os.environ.get('DB_USERNAME', 'root'), '--batch', '--skip-column-names',
       '--default-character-set=utf8mb4', 'lastcall_eats_baseline']

def sql(statement):
    return subprocess.run(cmd, input=statement, text=True, env=env,
                          check=True, capture_output=True).stdout.strip()

count = int(sql('SELECT COUNT(*) FROM product_listing;'))
if count >= 10000:
    raise SystemExit(f'Already {count} listings; no data changed.')
check = int(sql('SELECT COUNT(*) FROM product_template WHERE id >= 1000000;'))
check += int(sql('SELECT COUNT(*) FROM product_listing WHERE id >= 1000000;'))
if check:
    raise SystemExit('Synthetic ID range already in use; inspect before retrying.')
merchants = [int(x) for x in sql('SELECT id FROM merchant ORDER BY id;').splitlines()]
if not merchants:
    raise SystemExit('Initialize the baseline database first.')
statements = ['START TRANSACTION;']
for start in range(0, 10000-count, 500):
    templates, listings = [], []
    for i in range(start, min(start+500, 10000-count)):
        item = 1000000+i
        merchant = merchants[i % len(merchants)]
        templates.append(f"({item},{merchant},'Baseline meal {i}','Synthetic benchmark fixture',20.00,1)")
        listings.append(f"({item},{merchant},{item},10.00,100,100,'17:00:00','22:00:00',CURRENT_DATE,1)")
    statements.append('INSERT INTO product_template (id,merchant_id,name,description,original_price,is_active) VALUES '+','.join(templates)+';')
    statements.append('INSERT INTO product_listing (id,merchant_id,template_id,discount_price,quantity,remaining_quantity,pickup_start,pickup_end,date,is_available) VALUES '+','.join(listings)+';')
statements += ['COMMIT;', 'SELECT COUNT(*) FROM product_listing;']
print('Baseline listing count:', sql('\n'.join(statements)))
print('Synthetic listings each have a distinct template and cycle across existing merchants.')
