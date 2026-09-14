#!/usr/bin/env python3
"""Verify the authenticated merchant notification read flow without printing the JWT."""

import json
import urllib.request


BASE_URL = "http://127.0.0.1:8081"


def request(path: str, method: str = "GET", body=None, token: str | None = None):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(BASE_URL + path, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req) as response:
        return response.status, json.load(response)


login_status, login = request(
    "/api/auth/login/merchant",
    method="POST",
    body={"email": "sushi@example.com", "password": "111111"},
)
token = login["data"]["accessToken"]

list_status, notifications = request("/api/merchant/notifications", token=token)
order_notification = next(
    item for item in notifications["data"] if item["orderId"] == 95
)

read_status, marked_read = request(
    f"/api/merchant/notifications/{order_notification['id']}/read",
    method="PATCH",
    token=token,
)

print(json.dumps({
    "loginStatus": login_status,
    "listStatus": list_status,
    "notificationCount": len(notifications["data"]),
    "orderId": marked_read["data"]["orderId"],
    "markReadStatus": read_status,
    "read": marked_read["data"]["read"],
    "readAt": marked_read["data"]["readAt"],
}, indent=2))
