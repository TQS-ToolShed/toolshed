import http from "k6/http";
import { check, sleep } from "k6";
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";

export const options = {
  stages: [
    { duration: "30s", target: 20 },
    { duration: "1m30s", target: 10 },
    { duration: "20s", target: 0 },
  ],
  thresholds: {
    http_req_duration: ["p(95)<300"], // 95% of requests must complete below 300ms
    http_req_failed: ["rate<0.01"], // http errors should be less than 1%
  },
};

export default function runTest() {
  const res = http.get("http://backend:8080/api/tools");
  check(res, {
    "status is 200": (r) => r.status === 200,
  });
  sleep(1);
}

export function handleSummary(data) {
  return {
    "/scripts/summary.html": htmlReport(data),
  };
}
