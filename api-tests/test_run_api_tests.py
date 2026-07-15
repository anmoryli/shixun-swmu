"""Unit tests for the dependency-free API regression runner."""

from __future__ import annotations

import importlib.util
import json
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("run_api_tests.py")
SPEC = importlib.util.spec_from_file_location("medicine_api_runner", MODULE_PATH)
runner = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
sys.modules[SPEC.name] = runner
SPEC.loader.exec_module(runner)


class FakeClient:
    base_url = "http://example.test"

    def __init__(self, result=None, error=None):
        self.result = result
        self.error = error

    def request(self, method, path, **_kwargs):
        if self.error:
            raise self.error
        return self.result


class ApiRunnerTest(unittest.TestCase):

    def test_redact_removes_nested_and_raw_secrets(self):
        value = {
            "token": "raw-token",
            "profile": {"password": "secret", "name": "tester"},
            "items": [{"authorization": "Bearer raw"}],
        }

        self.assertEqual(runner.redact(value)["token"], "<redacted>")
        self.assertEqual(runner.redact(value)["profile"]["password"], "<redacted>")
        self.assertEqual(runner.redact(value)["profile"]["name"], "tester")
        self.assertEqual(runner.redact('{"secret":"value"}'), '{"secret":"<redacted>"}')

    def test_response_helpers_tolerate_missing_shapes(self):
        self.assertEqual(runner.api_data({"data": {"id": 1}}), {"id": 1})
        self.assertIsNone(runner.api_data(None))
        self.assertEqual(runner.page_rows({"data": {"page": {"list": [{"id": 1}]}}}, "page"), [{"id": 1}])
        self.assertEqual(runner.page_rows({"data": {}}, "page"), [])

    def test_first_exact_matches_string_equivalent_values(self):
        rows = [{"id": 1, "name": "one"}, {"id": "2", "name": "two"}]
        self.assertEqual(runner.first_exact(rows, "id", 2)["name"], "two")
        self.assertIsNone(runner.first_exact(rows, "id", 3))

    def test_tiny_png_returns_a_real_png_signature(self):
        self.assertTrue(runner.tiny_png().startswith(b"\x89PNG\r\n\x1a\n"))

    def test_expect_api_records_pass_and_redacts_response(self):
        result = runner.HttpResult(
            "GET", "http://example.test/api/session", 200, 12,
            {"code": runner.SUCCESS, "data": {"token": "raw-token"}}, "",
        )
        suite = runner.Suite(FakeClient(result=result))

        body = suite.expect_api("session", "GET", "/api/session")

        self.assertEqual(body["code"], runner.SUCCESS)
        self.assertEqual(suite.cases[0].status, "passed")
        self.assertEqual(suite.cases[0].response["data"]["token"], "<redacted>")

    def test_expect_api_records_assertion_and_transport_failures(self):
        mismatch = runner.HttpResult(
            "GET", "http://example.test/api/data", 200, 4,
            {"code": runner.SUCCESS, "data": []}, "",
        )
        suite = runner.Suite(FakeClient(result=mismatch))
        suite.expect_api(
            "non-empty", "GET", "/api/data",
            validate=lambda body: "data is empty" if not body["data"] else None,
        )
        self.assertEqual(suite.cases[0].status, "failed")
        self.assertEqual(suite.cases[0].detail, "data is empty")

        failing_suite = runner.Suite(FakeClient(error=TimeoutError("offline")))
        self.assertIsNone(failing_suite.expect_api("health", "GET", "/health"))
        self.assertEqual(failing_suite.cases[0].status, "failed")
        self.assertIn("TimeoutError", failing_suite.cases[0].detail)

    def test_write_reports_creates_json_junit_and_markdown(self):
        suite = runner.Suite(FakeClient())
        suite.cases.extend([
            runner.CaseResult(name="ok", status="passed", method="GET", url="http://example/ok", elapsed_ms=5),
            runner.CaseResult(name="skip", status="skipped", detail="environment unavailable"),
        ])

        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory)
            exit_code = runner.write_reports(suite, output, "20260715T000000Z", "unit-run", "http://example")

            self.assertEqual(exit_code, 0)
            report = json.loads((output / "api-test-20260715T000000Z.json").read_text(encoding="utf-8"))
            self.assertEqual(report["summary"], {"total": 2, "passed": 1, "failed": 0, "skipped": 1})
            testsuite = ET.parse(output / "api-test-20260715T000000Z.xml").getroot()
            self.assertEqual(testsuite.attrib["tests"], "2")
            self.assertIn("通过 **1**，失败 **0**，跳过 **1**", (output / "latest-summary.md").read_text(encoding="utf-8"))

    def test_write_reports_returns_failure_exit_code(self):
        suite = runner.Suite(FakeClient())
        suite.cases.append(runner.CaseResult(name="failed", status="failed", detail="mismatch"))

        with tempfile.TemporaryDirectory() as directory:
            self.assertEqual(
                runner.write_reports(suite, Path(directory), "20260715T000001Z", "failed-run", "http://example"),
                1,
            )


if __name__ == "__main__":
    unittest.main()
