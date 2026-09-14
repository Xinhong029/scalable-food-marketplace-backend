#!/usr/bin/env python3
"""Create a NEW local baseline database; never reset an existing database."""
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
DATABASE = "lastcall_eats_baseline"


def main():
    # Reuse the existing local password default without printing it or putting it in argv.
    dev = (ROOT / "lastcall-api/src/main/resources/application-dev.yml").read_text()
    default = re.search(r"\$\{DB_PASSWORD:([^}]+)\}", dev)
    password = os.environ.get("DB_PASSWORD")
    if password is None:
        if not default:
            raise SystemExit("Set DB_PASSWORD before running this script.")
        password = default.group(1)
    env = os.environ.copy()
    env["MYSQL_PWD"] = password
    command = ["mysql", "--protocol=TCP", "--host=127.0.0.1", "--port=3306",
               "--connect-timeout=5", "--default-character-set=utf8mb4",
               "--user=" + os.environ.get("DB_USERNAME", "root")]

    schema = (ROOT / "sql/schema.sql").read_text()
    seed = (ROOT / "sql/seed.sql").read_text()
    # Keep the original SQL files unchanged. Rewrite their explicit database selection.
    schema = re.sub(r"\blastcall_eats\b", DATABASE, schema)
    seed = re.sub(r"\blastcall_eats\b", DATABASE, seed)
    expected = "CREATE DATABASE IF NOT EXISTS " + DATABASE
    if schema.count(expected) != 1 or seed.count("USE " + DATABASE + ";") != 1:
        raise SystemExit("SQL layout changed; review the initializer before proceeding.")
    # An existing DB causes the FIRST statement to fail; mysql batch mode stops there.
    schema = schema.replace(expected, "CREATE DATABASE " + DATABASE, 1)
    # These statements are unnecessary in a fresh database.
    seed = re.sub(r"^TRUNCATE TABLE .*;\s*$", "", seed, flags=re.MULTILINE)
    seed = re.sub(r"^SET FOREIGN_KEY_CHECKS = [01];\s*$", "", seed, flags=re.MULTILINE)
    verification = """
SELECT DATABASE() AS database_name, VERSION() AS mysql_version;
SELECT 'users' AS entity, COUNT(*) AS row_count FROM `user`
UNION ALL SELECT 'merchants', COUNT(*) FROM merchant
UNION ALL SELECT 'templates', COUNT(*) FROM product_template
UNION ALL SELECT 'listings', COUNT(*) FROM product_listing
UNION ALL SELECT 'orders', COUNT(*) FROM orders;
"""
    print("Creating new database:", DATABASE, flush=True)
    result = subprocess.run(command, input=schema + "\n" + seed + verification,
                            text=True, env=env)
    if result.returncode:
        raise SystemExit("Initialization stopped. No automatic reset is performed. "
                         "If import failed midway, inspect the baseline database before retrying.")


if __name__ == "__main__":
    main()
