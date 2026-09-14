import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const page = Number(__ENV.PAGE || 0);
const size = Number(__ENV.SIZE || 20);
const bodyCharacters = new Trend('response_body_characters');
export const options = {
  vus: Number(__ENV.VUS || 1),
  duration: __ENV.DURATION || '30s',
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(95)', 'p(99)'],
  thresholds: { http_req_failed: ['rate<0.01'], checks: ['rate==1'] },
};

export default function () {
  const response = http.get(`http://127.0.0.1:8081/api/products/browse?page=${page}&size=${size}`,
    { timeout: '15s' });
  bodyCharacters.add(response.body ? response.body.length : 0);
  let body;
  try { body = response.json(); } catch (_) { body = null; }
  check(response, {
    'HTTP 200': r => r.status === 200,
    'valid paginated listing response': () => body !== null && body.code === 200 &&
      body.data.page === page && body.data.size === size &&
      Array.isArray(body.data.content) && body.data.content.length <= size &&
      body.data.content.every(item => Number.isInteger(item.listingId) && item.remainingQuantity > 0),
  });
}

export function handleSummary(data) {
  return { [__ENV.REPORT || 'target/baseline/browse-summary.json']: JSON.stringify(data, null, 2) };
}
