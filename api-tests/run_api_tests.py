#!/usr/bin/env python3
"""Black-box API regression runner for the medicine backend.

Only Python's standard library is required. The runner reads credentials from
environment variables, creates uniquely named disposable records, cleans them
up in dependency order, and writes JSON, JUnit XML and Markdown evidence.
"""

from __future__ import annotations

import json
import logging
import mimetypes
import os
import sys
import time
import traceback
import urllib.error
import urllib.parse
import urllib.request
import uuid
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable

logging.basicConfig(level=logging.INFO, format="%(message)s")


SUCCESS = 20000
INVALID_ARGUMENT = 10000
DUPLICATE_DATA = 10001
LOGIN_FAILED = 10002
FORBIDDEN = 10003
TOKEN_EXPIRED = 10006


@dataclass
class HttpResult:
    method: str
    url: str
    status: int
    elapsed_ms: int
    body: Any
    raw_body: str


@dataclass
class CaseResult:
    name: str
    status: str
    method: str = ""
    url: str = ""
    http_status: int | None = None
    api_code: int | None = None
    expected: str = ""
    elapsed_ms: int = 0
    detail: str = ""
    response: Any = None


def redact(value: Any) -> Any:
    """Remove secrets from evidence while retaining response structure."""
    if isinstance(value, dict):
        result: dict[str, Any] = {}
        for key, item in value.items():
            normalized = str(key).lower().replace("_", "").replace("-", "")
            result[key] = "<redacted>" if normalized in {
                "token",
                "authorization",
                "password",
                "pwd",
                "secret",
            } else redact(item)
        return result
    if isinstance(value, list):
        return [redact(item) for item in value]
    if isinstance(value, tuple):
        return [redact(item) for item in value]
    if isinstance(value, str):
        # Fallback for non-JSON server output that still resembles a JSON secret field.
        import re

        return re.sub(
            r'(?i)("(?:token|authorization|password|pwd|secret)"\s*:\s*")[^"]*(")',
            r'\1<redacted>\2',
            value,
        )
    return value


class ApiClient:
    def __init__(self, base_url: str, timeout: float = 15.0):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def request(
        self,
        method: str,
        path: str,
        *,
        token: str | None = None,
        json_body: Any = None,
        form: dict[str, Any] | None = None,
        multipart: dict[str, tuple[str, bytes, str]] | None = None,
    ) -> HttpResult:
        url = path if path.startswith("http") else f"{self.base_url}{path}"
        headers = {"Accept": "application/json"}
        if token:
            headers["Authorization"] = token
        data: bytes | None = None
        if json_body is not None:
            headers["Content-Type"] = "application/json; charset=UTF-8"
            data = json.dumps(json_body, ensure_ascii=False).encode("utf-8")
        elif form is not None:
            headers["Content-Type"] = "application/x-www-form-urlencoded; charset=UTF-8"
            data = urllib.parse.urlencode(form).encode("utf-8")
        elif multipart is not None:
            boundary = f"----medicine-api-test-{uuid.uuid4().hex}"
            headers["Content-Type"] = f"multipart/form-data; boundary={boundary}"
            chunks: list[bytes] = []
            for field, (filename, content, content_type) in multipart.items():
                chunks.extend(
                    [
                        f"--{boundary}\r\n".encode(),
                        (
                            f'Content-Disposition: form-data; name="{field}"; '
                            f'filename="{filename}"\r\n'
                        ).encode(),
                        f"Content-Type: {content_type}\r\n\r\n".encode(),
                        content,
                        b"\r\n",
                    ]
                )
            chunks.append(f"--{boundary}--\r\n".encode())
            data = b"".join(chunks)

        request = urllib.request.Request(url, data=data, headers=headers, method=method.upper())
        started = time.perf_counter()
        try:
            with urllib.request.urlopen(request, timeout=self.timeout) as response:
                status = response.status
                raw = response.read().decode("utf-8", errors="replace")
        except urllib.error.HTTPError as error:
            status = error.code
            raw = error.read().decode("utf-8", errors="replace")
        elapsed = round((time.perf_counter() - started) * 1000)
        try:
            body = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            body = None
        return HttpResult(method.upper(), url, status, elapsed, body, raw[:4000])


class Suite:
    def __init__(self, client: ApiClient):
        self.client = client
        self.cases: list[CaseResult] = []

    def skip(self, name: str, reason: str) -> None:
        self.cases.append(CaseResult(name=name, status="skipped", detail=reason))

    def expect_api(
        self,
        name: str,
        method: str,
        path: str,
        *,
        expected_code: int = SUCCESS,
        token: str | None = None,
        json_body: Any = None,
        form: dict[str, Any] | None = None,
        multipart: dict[str, tuple[str, bytes, str]] | None = None,
        validate: Callable[[Any], str | None] | None = None,
    ) -> Any:
        expected = f"HTTP 200 and API code {expected_code}"
        try:
            result = self.client.request(
                method,
                path,
                token=token,
                json_body=json_body,
                form=form,
                multipart=multipart,
            )
            api_code = result.body.get("code") if isinstance(result.body, dict) else None
            detail = ""
            passed = result.status == 200 and api_code == expected_code
            if passed and validate:
                detail = validate(result.body) or ""
                passed = not detail
            self.cases.append(
                CaseResult(
                    name=name,
                    status="passed" if passed else "failed",
                    method=result.method,
                    url=result.url,
                    http_status=result.status,
                    api_code=api_code,
                    expected=expected,
                    elapsed_ms=result.elapsed_ms,
                    detail=detail or ("" if passed else "响应不符合预期"),
                    response=redact(result.body if result.body is not None else result.raw_body),
                )
            )
            return result.body
        except Exception as error:  # keep the full suite and evidence alive
            self.cases.append(
                CaseResult(
                    name=name,
                    status="failed",
                    method=method,
                    url=f"{self.client.base_url}{path}",
                    expected=expected,
                    detail=f"{type(error).__name__}: {error}",
                )
            )
            return None

    def expect_raw(
        self,
        name: str,
        method: str,
        path: str,
        validate: Callable[[HttpResult], str | None],
    ) -> Any:
        try:
            result = self.client.request(method, path)
            detail = validate(result) or ""
            self.cases.append(
                CaseResult(
                    name=name,
                    status="passed" if not detail else "failed",
                    method=result.method,
                    url=result.url,
                    http_status=result.status,
                    expected="custom schema assertion",
                    elapsed_ms=result.elapsed_ms,
                    detail=detail,
                    response=redact(result.body if result.body is not None else result.raw_body),
                )
            )
            return result.body
        except Exception as error:
            self.cases.append(
                CaseResult(
                    name=name,
                    status="failed",
                    method=method,
                    url=f"{self.client.base_url}{path}",
                    expected="custom schema assertion",
                    detail=f"{type(error).__name__}: {error}",
                )
            )
            return None


def api_data(body: Any) -> Any:
    return body.get("data") if isinstance(body, dict) else None


def page_rows(body: Any, key: str) -> list[dict[str, Any]]:
    data = api_data(body)
    page = data.get(key) if isinstance(data, dict) else None
    rows = page.get("list") if isinstance(page, dict) else None
    return rows if isinstance(rows, list) else []


def first_exact(rows: list[dict[str, Any]], field: str, value: Any) -> dict[str, Any] | None:
    return next((row for row in rows if str(row.get(field)) == str(value)), None)


def tiny_png() -> bytes:
    # Valid 1x1 transparent PNG; avoids depending on a local fixture path.
    import base64

    return base64.b64decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUB"
        "AScY42YAAAAASUVORK5CYII="
    )


def main() -> int:
    base_url = os.getenv("BASE_URL", "http://localhost:8082").rstrip("/")
    admin_username = os.getenv("ADMIN_USERNAME", "").strip()
    admin_password = os.getenv("ADMIN_PASSWORD", "")
    timeout = float(os.getenv("API_TIMEOUT_SECONDS", "15"))
    evidence_dir = Path(
        os.getenv(
            "API_EVIDENCE_DIR",
            str(Path(__file__).resolve().parents[1] / "process-docs" / "evidence" / "api"),
        )
    )
    evidence_dir.mkdir(parents=True, exist_ok=True)

    run_stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    run_id = f"api-{run_stamp}-{uuid.uuid4().hex[:6]}"
    suffix = str(int(time.time()))[-9:]
    phone = f"19{suffix}"
    doctor_password = os.getenv("TEST_DOCTOR_PASSWORD", "ApiTest@123456")
    suite = Suite(ApiClient(base_url, timeout))
    admin_token: str | None = None
    doctor_token: str | None = None
    created: dict[str, int] = {}
    values: dict[str, Any] = {}

    suite.expect_raw(
        "健康检查",
        "GET",
        "/actuator/health",
        lambda r: None
        if r.status == 200 and isinstance(r.body, dict) and r.body.get("status") == "UP"
        else "预期 HTTP 200 且 status=UP",
    )
    suite.expect_api(
        "未登录访问仪表盘",
        "GET",
        "/api/dashboard",
        expected_code=TOKEN_EXPIRED,
    )
    suite.expect_api(
        "伪造 Token 访问权限",
        "GET",
        "/api/permissions",
        token="invalid-api-test-token",
        expected_code=TOKEN_EXPIRED,
    )
    suite.expect_api(
        "登录缺少密码",
        "POST",
        "/api/login",
        form={"username": admin_username or "admin_1"},
        expected_code=INVALID_ARGUMENT,
    )
    suite.expect_api(
        "错误密码登录",
        "POST",
        "/api/login",
        form={"username": admin_username or "admin_1", "password": "definitely-wrong-password"},
        expected_code=LOGIN_FAILED,
    )

    if not admin_username or not admin_password:
        suite.skip("管理员登录", "缺少 ADMIN_USERNAME 或 ADMIN_PASSWORD")
    else:
        json_login = suite.expect_api(
            "管理员 JSON 登录兼容",
            "POST",
            "/api/login",
            json_body={"username": admin_username, "password": admin_password},
            validate=lambda body: None
            if isinstance(api_data(body), dict) and api_data(body).get("token")
            else "响应未包含 data.token",
        )
        json_login_data = api_data(json_login)
        json_token = json_login_data.get("token") if isinstance(json_login_data, dict) else None
        if json_token:
            suite.expect_api("JSON 登录会话退出", "POST", "/api/logout", token=json_token)
        login = suite.expect_api(
            "管理员表单登录",
            "POST",
            "/api/login",
            form={"username": admin_username, "password": admin_password},
            validate=lambda body: None
            if isinstance(api_data(body), dict) and api_data(body).get("token")
            else "响应未包含 data.token",
        )
        login_data = api_data(login)
        admin_token = login_data.get("token") if isinstance(login_data, dict) else None

    if not admin_token:
        suite.skip("认证后接口与 CRUD", "管理员登录失败，无法安全执行写入型测试")
        return write_reports(suite, evidence_dir, run_stamp, run_id, base_url)

    try:
        suite.expect_api(
            "管理员权限树",
            "GET",
            "/api/permissions?roleName=ROLE_2",
            token=admin_token,
            validate=lambda body: None
            if isinstance(api_data(body), dict) and isinstance(api_data(body).get("permissions"), list)
            else "data.permissions 不是数组",
        )
        suite.expect_api(
            "仪表盘聚合数据",
            "GET",
            "/api/dashboard",
            token=admin_token,
            validate=lambda body: None if isinstance(api_data(body), dict) else "data 不是对象",
        )

        # Cities: this table references sysregion, so choose a valid but rarely used candidate.
        city_list = suite.expect_api("城市全量列表", "GET", "/api/citys", token=admin_token)
        existing_cities = page_rows(city_list, "cityPageInfo")
        suite.expect_api("城市分页列表", "GET", "/api/citys/1/200", token=admin_token)
        existing_numbers = {int(row["cityNumber"]) for row in existing_cities if row.get("cityNumber") is not None}
        configured_city = os.getenv("TEST_CITY_NUMBER", "").strip()
        candidates = ([int(configured_city)] if configured_city else []) + [713435, 713434, 713433, 712939]
        city_number = next((number for number in candidates if number not in existing_numbers), None)
        if city_number is None:
            suite.skip("新增城市", "候选 sysregion 城市均已配置；可通过 TEST_CITY_NUMBER 指定未使用城市")
        else:
            add_city = suite.expect_api(
                "新增城市",
                "POST",
                f"/api/citys?cityNumber={city_number}",
                token=admin_token,
            )
            if isinstance(add_city, dict) and add_city.get("code") == SUCCESS:
                city_after = suite.expect_api("查询新增城市", "GET", "/api/citys", token=admin_token)
                city_row = first_exact(page_rows(city_after, "cityPageInfo"), "cityNumber", city_number)
                if city_row:
                    created["city"] = int(city_row["cityId"])
                    values["city_number"] = city_number
                else:
                    suite.skip("定位新增城市 ID", "新增响应成功但城市全量列表未找到记录")
                suite.expect_api(
                    "重复城市错误",
                    "POST",
                    f"/api/citys?cityNumber={city_number}",
                    token=admin_token,
                    expected_code=DUPLICATE_DATA,
                )
        policy_city_id = created.get("city") or (
            int(existing_cities[0]["cityId"]) if existing_cities else None
        )

        # Company.
        company_name = f"{run_id}-company"
        suite.expect_api(
            "新增医药公司",
            "POST",
            "/api/companys",
            token=admin_token,
            json_body={"companyName": company_name, "companyPhone": phone},
        )
        company_page = suite.expect_api(
            "查询新增医药公司",
            "GET",
            f"/api/companys/1/200?name={urllib.parse.quote(company_name)}",
            token=admin_token,
        )
        company = first_exact(page_rows(company_page, "pageInfo"), "companyName", company_name)
        if company:
            created["company"] = int(company["companyId"])
            suite.expect_api(
                "修改医药公司",
                "PUT",
                f"/api/companys/{created['company']}",
                token=admin_token,
                json_body={"companyName": f"{company_name}-updated", "companyPhone": phone},
            )

        # Sale location.
        sale_name = f"{run_id}-sale"
        suite.expect_api(
            "新增销售地点",
            "POST",
            "/api/sales",
            token=admin_token,
            json_body={
                "saleName": sale_name,
                "salePhone": phone,
                "address": "API 自动化测试地址",
                "longitude": "120.3826",
                "latitude": "36.0671",
            },
        )
        sale_page = suite.expect_api(
            "查询新增销售地点",
            "GET",
            f"/api/sales/1/200?name={urllib.parse.quote(sale_name)}",
            token=admin_token,
        )
        sale = first_exact(page_rows(sale_page, "salePageInfo"), "saleName", sale_name)
        if sale:
            created["sale"] = int(sale["saleId"])
            suite.expect_api(
                "修改销售地点",
                "PUT",
                f"/api/sales/{created['sale']}",
                token=admin_token,
                json_body={
                    "saleName": f"{sale_name}-updated",
                    "salePhone": phone,
                    "address": "API 自动化测试地址-修改",
                    "longitude": "120.3827",
                    "latitude": "36.0672",
                },
            )
        suite.expect_api("销售地点全量列表", "GET", "/api/sales", token=admin_token)

        # Lookup values and doctor.
        doctor_info = suite.expect_api("医生级别与科室字典", "GET", "/api/doctors/info", token=admin_token)
        dictionary = api_data(doctor_info) if isinstance(api_data(doctor_info), dict) else {}
        levels = dictionary.get("allLevel") or []
        types = dictionary.get("allTreatType") or []
        doctor_name = f"API测试医生{run_id[-6:]}"
        if not levels or not types:
            suite.skip("新增医生", "医生级别或诊疗类型字典为空")
        else:
            level_id = levels[0].get("id")
            type_id = types[0].get("id")
            doctor_payload = {
                "name": doctor_name,
                "age": 30,
                "sex": 1,
                "levelId": level_id,
                "typeId": type_id,
                "phoneNumber": phone,
                "pwd": doctor_password,
            }
            suite.expect_api("新增医生", "POST", "/api/doctors", token=admin_token, json_body=doctor_payload)
            doctor_page = suite.expect_api(
                "查询新增医生",
                "GET",
                f"/api/doctors?pn=1&size=200&keyword={urllib.parse.quote(doctor_name)}",
                token=admin_token,
            )
            doctor = first_exact(page_rows(doctor_page, "doctorInfo"), "name", doctor_name)
            if doctor:
                created["doctor"] = int(doctor["id"])
                values["doctor_account_id"] = int(doctor["accountId"])
                doctor_payload.update({"accountId": doctor["accountId"], "age": 31})
                suite.expect_api(
                    "修改医生",
                    "PUT",
                    f"/api/doctors/{created['doctor']}",
                    token=admin_token,
                    json_body=doctor_payload,
                )
                suite.expect_api(
                    "重复医生手机号错误",
                    "POST",
                    "/api/doctors",
                    token=admin_token,
                    json_body={**doctor_payload, "accountId": None, "name": f"{doctor_name}重复"},
                    expected_code=DUPLICATE_DATA,
                )
                doctor_username = f"{doctor_name}{phone[-4:]}"
                doctor_login = suite.expect_api(
                    "医生账号登录",
                    "POST",
                    "/api/login",
                    form={"username": doctor_username, "password": doctor_password},
                )
                doctor_data = api_data(doctor_login)
                doctor_token = doctor_data.get("token") if isinstance(doctor_data, dict) else None
                if doctor_token:
                    suite.expect_api(
                        "医生越权新增材料",
                        "POST",
                        "/api/materials",
                        token=doctor_token,
                        json_body={"title": f"{run_id}-forbidden", "message": "不应写入"},
                        expected_code=FORBIDDEN,
                    )
                    suite.expect_api("医生只读访问仪表盘", "GET", "/api/dashboard", token=doctor_token)
                suite.expect_api(
                    "管理员重置医生密码",
                    "PUT",
                    f"/api/doctors/reset/{values['doctor_account_id']}",
                    token=admin_token,
                )
                suite.expect_api(
                    "旧密码在重置后失效",
                    "POST",
                    "/api/login",
                    form={"username": doctor_username, "password": doctor_password},
                    expected_code=LOGIN_FAILED,
                )
                suite.expect_api(
                    "默认重置密码登录",
                    "POST",
                    "/api/login",
                    form={"username": doctor_username, "password": "123456"},
                )

        # Uploads.
        suite.expect_api(
            "上传 PNG 图片",
            "POST",
            "/api/base/upload",
            token=admin_token,
            multipart={"file": ("api-test.png", tiny_png(), "image/png")},
            validate=lambda body: None
            if isinstance(api_data(body), dict) and api_data(body).get("url")
            else "响应未包含 data.url",
        )
        suite.expect_api(
            "拒绝非图片上传",
            "POST",
            "/api/base/upload",
            token=admin_token,
            multipart={"file": ("api-test.txt", b"not-an-image", "text/plain")},
            expected_code=INVALID_ARGUMENT,
        )

        # Material.
        material_title = f"{run_id}-material"
        suite.expect_api(
            "新增必备材料",
            "POST",
            "/api/materials",
            token=admin_token,
            json_body={"title": material_title, "message": "API 自动化测试材料"},
        )
        material_page = suite.expect_api(
            "查询新增必备材料",
            "GET",
            f"/api/materials?pn=1&size=200&keyword={urllib.parse.quote(material_title)}",
            token=admin_token,
        )
        material = first_exact(page_rows(material_page, "materialInfo"), "title", material_title)
        if material:
            created["material"] = int(material["id"])
            suite.expect_api(
                "修改必备材料",
                "PUT",
                f"/api/materials/{created['material']}",
                token=admin_token,
                json_body={"title": f"{material_title}-updated", "message": "已修改"},
            )

        # Policies use created references and are deleted before their parents.
        if created.get("company"):
            company_policy_title = f"{run_id}-company-policy"
            suite.expect_api(
                "新增医药公司政策",
                "POST",
                "/api/company_policys",
                token=admin_token,
                json_body={
                    "companyId": created["company"],
                    "title": company_policy_title,
                    "message": "API 自动化测试公司政策",
                },
            )
            policy_page = suite.expect_api(
                "查询新增医药公司政策",
                "GET",
                f"/api/company_policys?pn=1&size=200&keyword={urllib.parse.quote(company_policy_title)}",
                token=admin_token,
            )
            row = first_exact(page_rows(policy_page, "policyInfo"), "title", company_policy_title)
            if row:
                created["company_policy"] = int(row["id"])
                suite.expect_api(
                    "修改医药公司政策",
                    "PUT",
                    f"/api/company_policys/{created['company_policy']}",
                    token=admin_token,
                    json_body={
                        "companyId": created["company"],
                        "title": f"{company_policy_title}-updated",
                        "message": "已修改",
                    },
                )
        else:
            suite.skip("医药公司政策 CRUD", "未取得临时医药公司 ID")

        if policy_city_id:
            medical_policy_title = f"{run_id}-medical-policy"
            suite.expect_api(
                "新增医保政策",
                "POST",
                "/api/medical_policys",
                token=admin_token,
                json_body={
                    "cityId": policy_city_id,
                    "title": medical_policy_title,
                    "message": "API 自动化测试医保政策",
                    "updateTime": datetime.now(tz=timezone.utc).strftime("%Y-%m-%d"),
                },
            )
            policy_page = suite.expect_api(
                "查询新增医保政策",
                "GET",
                f"/api/medical_policys?pn=1&size=200&title={urllib.parse.quote(medical_policy_title)}",
                token=admin_token,
            )
            row = first_exact(page_rows(policy_page, "policyInfo"), "title", medical_policy_title)
            if row:
                created["medical_policy"] = int(row["id"])
                suite.expect_api(
                    "修改医保政策",
                    "PUT",
                    f"/api/medical_policys/{created['medical_policy']}",
                    token=admin_token,
                    json_body={
                        "cityId": policy_city_id,
                        "title": f"{medical_policy_title}-updated",
                        "message": "已修改",
                        "updateTime": datetime.now(tz=timezone.utc).strftime("%Y-%m-%d"),
                    },
                )
        else:
            suite.skip("医保政策 CRUD", "城市表为空，无法取得 cityId")

        # Drug, with an optional relation to the temporary sale location.
        drug_name = f"{run_id}-drug"
        suite.expect_api(
            "新增药品",
            "POST",
            "/api/drugs",
            token=admin_token,
            json_body={
                "drugName": drug_name,
                "drugInfo": "API 自动化测试药品",
                "drugEffect": "仅用于接口验证",
                "drugImg": "/image/api-test.png",
                "drugPublisher": "API Test",
                "saleIds": [created["sale"]] if created.get("sale") else [],
            },
        )
        drug_page = suite.expect_api(
            "查询新增药品",
            "GET",
            f"/api/drugs/1/200?name={urllib.parse.quote(drug_name)}",
            token=admin_token,
        )
        drug = first_exact(page_rows(drug_page, "drugPageInfo"), "drugName", drug_name)
        if drug:
            created["drug"] = int(drug["drugId"])
            suite.expect_api(
                "修改药品",
                "PUT",
                f"/api/drugs/{created['drug']}",
                token=admin_token,
                json_body={
                    "drugName": f"{drug_name}-updated",
                    "drugInfo": "已修改",
                    "drugEffect": "已修改",
                    "drugImg": "/image/api-test.png",
                    "drugPublisher": "API Test",
                    "saleIds": [created["sale"]] if created.get("sale") else [],
                },
            )

        # Explicit deletes are part of CRUD coverage. Dependencies are removed first.
        delete_order = [
            ("drug", "删除药品", "/api/drugs/{}"),
            ("company_policy", "删除医药公司政策", "/api/company_policys/{}"),
            ("medical_policy", "删除医保政策", "/api/medical_policys/{}"),
            ("material", "删除必备材料", "/api/materials/{}"),
            ("doctor", "删除医生", "/api/doctors/{}"),
            ("sale", "删除销售地点", "/api/sales/{}"),
            ("company", "删除医药公司", "/api/companys/{}"),
            ("city", "删除城市", "/api/citys/{}"),
        ]
        for key, name, path_template in delete_order:
            if key in created:
                response = suite.expect_api(
                    name, "DELETE", path_template.format(created[key]), token=admin_token
                )
                if isinstance(response, dict) and response.get("code") == SUCCESS:
                    created.pop(key, None)

        suite.expect_api("管理员退出登录", "POST", "/api/logout", token=admin_token)
        suite.expect_api(
            "退出后 Token 失效",
            "GET",
            "/api/permissions",
            token=admin_token,
            expected_code=TOKEN_EXPIRED,
        )
        admin_token = None
    except Exception as error:
        suite.cases.append(
            CaseResult(
                name="测试编排异常",
                status="failed",
                detail=f"{type(error).__name__}: {error}\n{traceback.format_exc(limit=5)}",
            )
        )
    finally:
        # Best-effort cleanup for interrupted/partially failed suites; cleanup outcomes are evidence too.
        if admin_token and created:
            cleanup_order = [
                ("drug", "/api/drugs/{}"),
                ("company_policy", "/api/company_policys/{}"),
                ("medical_policy", "/api/medical_policys/{}"),
                ("material", "/api/materials/{}"),
                ("doctor", "/api/doctors/{}"),
                ("sale", "/api/sales/{}"),
                ("company", "/api/companys/{}"),
                ("city", "/api/citys/{}"),
            ]
            for key, path_template in cleanup_order:
                if key in created:
                    suite.expect_api(
                        f"故障清理: {key}",
                        "DELETE",
                        path_template.format(created[key]),
                        token=admin_token,
                    )

    return write_reports(suite, evidence_dir, run_stamp, run_id, base_url)


def write_reports(
    suite: Suite, evidence_dir: Path, run_stamp: str, run_id: str, base_url: str
) -> int:
    passed = sum(case.status == "passed" for case in suite.cases)
    failed = sum(case.status == "failed" for case in suite.cases)
    skipped = sum(case.status == "skipped" for case in suite.cases)
    report = {
        "schemaVersion": 1,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "runId": run_id,
        "baseUrl": base_url,
        "credentials": "read from environment and never persisted",
        "summary": {"total": len(suite.cases), "passed": passed, "failed": failed, "skipped": skipped},
        "cases": [asdict(case) for case in suite.cases],
    }
    stem = f"api-test-{run_stamp}"
    json_path = evidence_dir / f"{stem}.json"
    junit_path = evidence_dir / f"{stem}.xml"
    markdown_path = evidence_dir / f"{stem}.md"
    latest_path = evidence_dir / "latest-summary.md"
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    testsuite = ET.Element(
        "testsuite",
        name="medicine-api",
        tests=str(len(suite.cases)),
        failures=str(failed),
        skipped=str(skipped),
        timestamp=report["generatedAt"],
    )
    for case in suite.cases:
        node = ET.SubElement(
            testsuite,
            "testcase",
            name=case.name,
            classname="medicine.api",
            time=f"{case.elapsed_ms / 1000:.3f}",
        )
        if case.status == "failed":
            failure = ET.SubElement(node, "failure", message=case.detail or "assertion failed")
            failure.text = json.dumps(case.response, ensure_ascii=False, indent=2)
        elif case.status == "skipped":
            ET.SubElement(node, "skipped", message=case.detail)
        output = ET.SubElement(node, "system-out")
        output.text = f"{case.method} {case.url}\nexpected={case.expected}\napi_code={case.api_code}"
    ET.ElementTree(testsuite).write(junit_path, encoding="utf-8", xml_declaration=True)

    lines = [
        f"# API 测试报告 `{run_id}`",
        "",
        f"- 生成时间：`{report['generatedAt']}`",
        f"- 目标：`{base_url}`",
        f"- 结果：通过 **{passed}**，失败 **{failed}**，跳过 **{skipped}**",
        "- 凭据：仅从进程环境读取，报告不落盘用户名、密码或 Token",
        "",
        "| 状态 | 用例 | 方法 | HTTP/API | 耗时 | 说明 |",
        "|---|---|---|---|---:|---|",
    ]
    symbols = {"passed": "PASS", "failed": "FAIL", "skipped": "SKIP"}
    for case in suite.cases:
        detail = (case.detail or "-").replace("|", "\\|").replace("\n", " ")[:240]
        codes = f"{case.http_status or '-'} / {case.api_code if case.api_code is not None else '-'}"
        lines.append(
            f"| {symbols.get(case.status, '-')} | {case.name} | {case.method or '-'} | {codes} | "
            f"{case.elapsed_ms} ms | {detail} |"
        )
    markdown = "\n".join(lines) + "\n"
    markdown_path.write_text(markdown, encoding="utf-8")
    latest_path.write_text(markdown, encoding="utf-8")

    logging.info(json.dumps(report["summary"], ensure_ascii=False))
    logging.info(f"JSON: {json_path}")
    logging.info(f"JUnit: {junit_path}")
    logging.info(f"Markdown: {markdown_path}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
