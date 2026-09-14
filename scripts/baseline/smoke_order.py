#!/usr/bin/env python3
"""Exercise a sequential order flow against the local baseline app (creates one order)."""
import json
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, build_opener, ProxyHandler

BASE = "http://127.0.0.1:8081"
ROOT = Path(__file__).resolve().parents[2]
CLIENT = build_opener(ProxyHandler({}))


def request(method, path, payload=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = Request(BASE + path, headers=headers, method=method,
                  data=None if payload is None else json.dumps(payload).encode())
    try:
        with CLIENT.open(req, timeout=15) as response:
            status, body = response.status, response.read()
    except HTTPError as error:
        status, body = error.code, error.read()
    print(method, path, "-> HTTP", status)
    return status, json.loads(body) if body else None


def check(condition, message):
    if not condition:
        raise SystemExit("FAIL: " + message)


def success(method, path, payload=None, token=None):
    status, result = request(method, path, payload, token)
    check(status == 200 and result and result.get("code") == 200,
          "Unexpected response: " + str(result))
    return result["data"]


def main():
    # Token is retained only in memory; it is never printed or written to the report.
    login = success("POST", "/api/auth/login/user",
                    {"email": "alice@example.com", "password": "111111"})
    token = login["accessToken"]
    print("Logged in as Alice; access token kept in memory.")
    before_orders = success("GET", "/api/orders", token=token)
    bought = {order["listingId"] for order in before_orders}
    listings = success("GET", "/api/products/browse?size=100")["content"]
    listing = next((item for item in listings
                    if item["listingId"] not in bought and item["remainingQuantity"] >= 2), None)
    check(listing is not None, "No unused listing with >= 2 units. Stop; do not reset the database automatically.")
    listing_id = listing["listingId"]
    before_stock = listing["remainingQuantity"]
    print("Selected listing", listing_id, listing["productName"], "stock:", before_stock)

    order = success("POST", "/api/orders", {"listingId": listing_id}, token)
    detail = success("GET", "/api/orders/" + str(order["id"]), token=token)
    check(detail["id"] == order["id"] and detail["listingId"] == listing_id,
          "Created order does not match the detail response")
    check(detail["status"] == "PENDING_PAYMENT", "New order must await payment")
    after_stock = next(item["remainingQuantity"] for item in
                       success("GET", "/api/products/browse?size=100")["content"]
                       if item["listingId"] == listing_id)
    check(after_stock == before_stock - 1, "Stock must decrease by exactly one")

    # A sequential repeat checks the existing business rule, NOT concurrent idempotency.
    status, rejection = request("POST", "/api/orders", {"listingId": listing_id}, token)
    check(status == 400 and rejection and
          rejection.get("message") == "You have already ordered this item today",
          "Expected same-day duplicate purchase rejection")
    final_orders = success("GET", "/api/orders", token=token)
    final_stock = next(item["remainingQuantity"] for item in
                       success("GET", "/api/products/browse?size=100")["content"]
                       if item["listingId"] == listing_id)
    check(len(final_orders) == len(before_orders) + 1, "Exactly one new order expected")
    check(final_stock == after_stock, "Rejected purchase must not consume stock")
    report = {"base_url": BASE, "user_id": login["userId"], "listing_id": listing_id,
              "order_id": order["id"], "order_status": detail["status"],
              "stock_before": before_stock, "stock_after": after_stock,
              "stock_after_rejection": final_stock, "orders_before": len(before_orders),
              "orders_after": len(final_orders), "duplicate_http_status": status,
              "result": "PASS", "scope": "Sequential smoke test, not load or concurrency test"}
    output = ROOT / "target/baseline/smoke-order.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))
    print("Report:", output)


if __name__ == "__main__":
    main()
