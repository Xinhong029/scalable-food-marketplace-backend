#!/usr/bin/env python3
"""Check local dependency health and the messaging metrics exposed by Actuator."""

import json
import urllib.request


BASE_URL = "http://127.0.0.1:8081"
REQUIRED_COMPONENTS = ("db", "redis", "rabbit")
REQUIRED_METRICS = (
    "lastcall.outbox.events",
    "lastcall.outbox.publish",
    "lastcall.order.created.consumer",
    "lastcall.order.created.end.to.end",
)


def get(path: str) -> str:
    with urllib.request.urlopen(BASE_URL + path) as response:
        return response.read().decode()


health = json.loads(get("/actuator/health"))
if health["status"] != "UP":
    raise SystemExit(f"Application health is {health['status']}")

component_status = {
    name: health["components"][name]["status"] for name in REQUIRED_COMPONENTS
}
if any(status != "UP" for status in component_status.values()):
    raise SystemExit(f"Dependency health check failed: {component_status}")

metric_names = set(json.loads(get("/actuator/metrics"))["names"])
missing = sorted(set(REQUIRED_METRICS) - metric_names)
if missing:
    raise SystemExit(f"Missing messaging metrics: {missing}")

prometheus_lines = [
    line for line in get("/actuator/prometheus").splitlines()
    if line.startswith((
        "lastcall_outbox_publish_total",
        "lastcall_order_created_consumer_total",
        "lastcall_order_created_end_to_end_seconds_count",
        "lastcall_order_created_end_to_end_seconds_sum",
        "lastcall_outbox_events",
    ))
]

print(json.dumps({
    "application": health["status"],
    "components": component_status,
    "requiredMetrics": "present",
    "prometheusSamples": prometheus_lines,
}, indent=2))
