#!/usr/bin/env python3
"""Verify successful orders evict browse caches and rejected orders do not."""
import json
from pathlib import Path
import subprocess
from urllib.error import HTTPError
from urllib.request import ProxyHandler, Request, build_opener

BASE = "http://127.0.0.1:8081"
ROOT = Path(__file__).resolve().parents[2]
CLIENT = build_opener(ProxyHandler({}))
KEY_20 = "lastcall-eats:product-browse:page:0:size:20"
KEY_100 = "lastcall-eats:product-browse:page:0:size:100"


def request(method, path, payload=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = Request(BASE + path, headers=headers, method=method,
                  data=None if payload is None else json.dumps(payload).encode())
    try:
        with CLIENT.open(req, timeout=15) as response:
            return response.status, json.loads(response.read())
    except HTTPError as error:
        return error.code, json.loads(error.read())


def success(method, path, payload=None, token=None):
    status, body = request(method, path, payload, token)
    if status != 200 or body.get("code") != 200:
        raise SystemExit(f"FAIL {method} {path}: HTTP {status} {body}")
    return body["data"]


def redis(*arguments):
    command = ["docker", "exec", "lastcall-redis", "redis-cli", *arguments]
    return subprocess.run(command, check=True, capture_output=True, text=True).stdout.strip()


def main():
    login = success("POST", "/api/auth/login/user",
                    {"email": "alice@example.com", "password": "111111"})
    token = login["accessToken"]
    existing_orders = success("GET", "/api/orders", token=token)
    bought = {order["listingId"] for order in existing_orders}

    redis("DEL", KEY_20, KEY_100)
    success("GET", "/api/products/browse?page=0&size=20")
    page = success("GET", "/api/products/browse?page=0&size=100")
    before_exists = [int(redis("EXISTS", KEY_20)), int(redis("EXISTS", KEY_100))]
    if before_exists != [1, 1]:
        raise SystemExit(f"FAIL: expected two primed cache keys, got {before_exists}")

    listing = next((item for item in page["content"]
                    if item["listingId"] not in bought and item["remainingQuantity"] >= 2), None)
    if listing is None:
        raise SystemExit("FAIL: no unused listing with at least two units")

    listing_id = listing["listingId"]
    stock_before = listing["remainingQuantity"]
    order = success("POST", "/api/orders", {"listingId": listing_id}, token)
    after_commit_exists = [int(redis("EXISTS", KEY_20)), int(redis("EXISTS", KEY_100))]
    if after_commit_exists != [0, 0]:
        raise SystemExit(f"FAIL: committed order did not evict both keys: {after_commit_exists}")

    refreshed = success("GET", "/api/products/browse?page=0&size=100")
    refreshed_listing = next(item for item in refreshed["content"]
                             if item["listingId"] == listing_id)
    stock_after = refreshed_listing["remainingQuantity"]
    if stock_after != stock_before - 1:
        raise SystemExit(f"FAIL: expected stock {stock_before - 1}, got {stock_after}")

    ttl_before_rejection = int(redis("TTL", KEY_100))
    rejected_status, _ = request("POST", "/api/orders", {"listingId": listing_id}, token)
    key_after_rejection = int(redis("EXISTS", KEY_100))
    ttl_after_rejection = int(redis("TTL", KEY_100))
    if rejected_status != 400 or key_after_rejection != 1:
        raise SystemExit("FAIL: rejected order unexpectedly removed the cache")

    result = {
        "result": "PASS",
        "listingId": listing_id,
        "orderId": order["id"],
        "stockBefore": stock_before,
        "stockAfter": stock_after,
        "primedKeysBeforeOrder": before_exists,
        "keysAfterCommittedOrder": after_commit_exists,
        "rejectedDuplicateHttpStatus": rejected_status,
        "keyExistsAfterRejectedOrder": key_after_rejection,
        "ttlBeforeRejectedOrder": ttl_before_rejection,
        "ttlAfterRejectedOrder": ttl_after_rejection
    }
    output = ROOT / "target" / "baseline" / "cache-invalidation.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
