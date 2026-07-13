#!/usr/bin/env python3
"""Generate the checked-in Postman collection from compact endpoint metadata."""

from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Any

logging.basicConfig(level=logging.INFO, format="%(message)s")


SUCCESS_TEST = """
const payload = pm.response.json();
pm.test("HTTP status is 200", () => pm.response.to.have.status(200));
pm.test("API code is {code}", () => pm.expect(payload.code).to.eql({code}));
""".strip()


def tests(code: int = 20000, extra: str = "") -> list[dict[str, Any]]:
    script = SUCCESS_TEST.format(code=code)
    if extra:
        script += "\n" + extra.strip()
    return [{"listen": "test", "script": {"type": "text/javascript", "exec": script.splitlines()}}]


def request(
    name: str,
    method: str,
    path: str,
    *,
    auth: str | None = "adminToken",
    json_body: Any = None,
    form: dict[str, str] | None = None,
    formdata: list[dict[str, Any]] | None = None,
    expected: int = 20000,
    extra_tests: str = "",
    pre_request: str = "",
) -> dict[str, Any]:
    headers: list[dict[str, str]] = [{"key": "Accept", "value": "application/json"}]
    if auth:
        headers.append({"key": "Authorization", "value": f"{{{{{auth}}}}}", "type": "text"})
    body = None
    if json_body is not None:
        headers.append({"key": "Content-Type", "value": "application/json"})
        body = {
            "mode": "raw",
            "raw": json.dumps(json_body, ensure_ascii=False, indent=2),
            "options": {"raw": {"language": "json"}},
        }
    elif form is not None:
        body = {
            "mode": "urlencoded",
            "urlencoded": [
                {"key": key, "value": value, "type": "text"} for key, value in form.items()
            ],
        }
    elif formdata is not None:
        body = {"mode": "formdata", "formdata": formdata}
    events = tests(expected, extra_tests)
    if pre_request:
        events.insert(
            0,
            {
                "listen": "prerequest",
                "script": {"type": "text/javascript", "exec": pre_request.strip().splitlines()},
            },
        )
    item = {
        "name": name,
        "event": events,
        "request": {
            "method": method,
            "header": headers,
            "url": {"raw": f"{{{{baseUrl}}}}{path}", "host": ["{{baseUrl}}"], "path": []},
            "description": f"Contract source: {method} {path}",
        },
        "response": [],
    }
    if body is not None:
        item["request"]["body"] = body
    return item


def folder(name: str, *items: dict[str, Any], description: str = "") -> dict[str, Any]:
    return {"name": name, "description": description, "item": list(items)}


def main() -> None:
    bootstrap = """
const now = Date.now().toString();
const runId = `postman-${now}`;
const phone = `19${now.slice(-9)}`;
const doctorName = `Postman测试医生${now.slice(-6)}`;
pm.collectionVariables.set("runId", runId);
pm.collectionVariables.set("phone", phone);
pm.collectionVariables.set("doctorName", doctorName);
pm.collectionVariables.set("doctorPassword", "Postman@Test123456");
pm.collectionVariables.set("doctorUsername", `${doctorName}${phone.slice(-4)}`);
["adminToken", "doctorToken", "doctorResetToken", "cityId", "companyId", "saleId",
 "doctorId", "doctorAccountId", "drugId", "materialId", "medicalPolicyId", "companyPolicyId"]
  .forEach(key => pm.collectionVariables.unset(key));
"""
    health_tests = """
const payload = pm.response.json();
pm.test("HTTP status is 200", () => pm.response.to.have.status(200));
pm.test("Health status is UP", () => pm.expect(payload.status).to.eql("UP"));
""".strip()
    health = request(
        "初始化运行并检查健康",
        "GET",
        "/actuator/health",
        auth=None,
        pre_request=bootstrap,
    )
    health["event"][-1]["script"]["exec"] = health_tests.splitlines()

    auth = folder(
        "00 认证、权限与错误场景",
        health,
        request("未登录访问仪表盘", "GET", "/api/dashboard", auth=None, expected=10006),
        request(
            "伪造 Token 访问权限",
            "GET",
            "/api/permissions",
            auth="invalidToken",
            expected=10006,
        ),
        request(
            "登录缺少密码",
            "POST",
            "/api/login",
            auth=None,
            form={"username": "{{adminUsername}}"},
            expected=10000,
        ),
        request(
            "错误密码登录",
            "POST",
            "/api/login",
            auth=None,
            form={"username": "{{adminUsername}}", "password": "definitely-wrong-password"},
            expected=10002,
        ),
        request(
            "管理员表单登录",
            "POST",
            "/api/login",
            auth=None,
            form={"username": "{{adminUsername}}", "password": "{{adminPassword}}"},
            extra_tests="""
pm.test("Token and administrator user are returned", () => {
  pm.expect(payload.data.token).to.be.a("string").and.not.empty;
  pm.expect(payload.data.userInfo.utype).to.eql(1);
});
pm.collectionVariables.set("adminToken", payload.data.token);
""",
        ),
        request(
            "管理员 JSON 登录兼容",
            "POST",
            "/api/login",
            auth=None,
            json_body={"username": "{{adminUsername}}", "password": "{{adminPassword}}"},
            extra_tests="pm.collectionVariables.set(\"jsonLoginToken\", payload.data.token);",
        ),
        request("JSON 登录会话退出", "POST", "/api/logout", auth="jsonLoginToken"),
        request(
            "客户端 roleName 篡改不改变权限来源",
            "GET",
            "/api/permissions?roleName=ROLE_2",
            extra_tests="""
pm.test("Permission tree is an array", () => {
  pm.expect(payload.data.permissions).to.be.an("array");
});
""",
        ),
        request(
            "仪表盘聚合数据",
            "GET",
            "/api/dashboard",
            extra_tests="pm.test(\"Dashboard data is an object\", () => pm.expect(payload.data).to.be.an(\"object\"));",
        ),
    )

    cities = folder(
        "01 城市管理",
        request(
            "城市全量列表",
            "GET",
            "/api/citys",
            extra_tests="""
const rows = payload.data.cityPageInfo.list;
pm.test("City list is an array", () => pm.expect(rows).to.be.an("array"));
if (rows.length) pm.collectionVariables.set("existingCityId", rows[0].cityId);
""",
        ),
        request("城市分页列表", "GET", "/api/citys/1/200"),
        request(
            "新增城市",
            "POST",
            "/api/citys?cityNumber={{testCityNumber}}",
        ),
        request(
            "查询并保存新增城市 ID",
            "GET",
            "/api/citys",
            extra_tests="""
const rows = payload.data.cityPageInfo.list;
const row = rows.find(item => String(item.cityNumber) === String(pm.environment.get("testCityNumber")));
pm.test("Created city can be found", () => pm.expect(row).to.be.an("object"));
if (row) pm.collectionVariables.set("cityId", row.cityId);
""",
        ),
        request(
            "重复城市错误",
            "POST",
            "/api/citys?cityNumber={{testCityNumber}}",
            expected=10001,
        ),
        request("删除城市", "DELETE", "/api/citys/{{cityId}}"),
        description="城市模型无更新接口，因此覆盖新增、分页/全量查询、重复校验和删除。",
    )

    companies = folder(
        "02 医药公司管理",
        request(
            "新增医药公司",
            "POST",
            "/api/companys",
            json_body={"companyName": "{{runId}}-company", "companyPhone": "{{phone}}"},
        ),
        request(
            "查询并保存公司 ID",
            "GET",
            "/api/companys/1/200?name={{runId}}-company",
            extra_tests="""
const rows = payload.data.pageInfo.list;
const row = rows.find(item => item.companyName === `${pm.collectionVariables.get("runId")}-company`);
pm.test("Created company can be found", () => pm.expect(row).to.be.an("object"));
if (row) pm.collectionVariables.set("companyId", row.companyId);
""",
        ),
        request(
            "修改医药公司",
            "PUT",
            "/api/companys/{{companyId}}",
            json_body={"companyName": "{{runId}}-company-updated", "companyPhone": "{{phone}}"},
        ),
        request("医药公司全量列表", "GET", "/api/companys"),
    )

    sales = folder(
        "03 销售地点管理",
        request(
            "新增销售地点",
            "POST",
            "/api/sales",
            json_body={
                "saleName": "{{runId}}-sale",
                "salePhone": "{{phone}}",
                "address": "Postman 自动化测试地址",
                "longitude": "120.3826",
                "latitude": "36.0671",
            },
        ),
        request(
            "查询并保存销售地点 ID",
            "GET",
            "/api/sales/1/200?name={{runId}}-sale",
            extra_tests="""
const rows = payload.data.salePageInfo.list;
const row = rows.find(item => item.saleName === `${pm.collectionVariables.get("runId")}-sale`);
pm.test("Created sale can be found", () => pm.expect(row).to.be.an("object"));
if (row) pm.collectionVariables.set("saleId", row.saleId);
""",
        ),
        request(
            "修改销售地点",
            "PUT",
            "/api/sales/{{saleId}}",
            json_body={
                "saleName": "{{runId}}-sale-updated",
                "salePhone": "{{phone}}",
                "address": "Postman 自动化测试地址-修改",
                "longitude": "120.3827",
                "latitude": "36.0672",
            },
        ),
        request("销售地点全量列表", "GET", "/api/sales"),
    )

    doctors = folder(
        "04 医生管理与越权",
        request(
            "医生级别与诊疗类型",
            "GET",
            "/api/doctors/info",
            extra_tests="""
pm.test("Doctor dictionaries are not empty", () => {
  pm.expect(payload.data.allLevel).to.be.an("array").and.not.empty;
  pm.expect(payload.data.allTreatType).to.be.an("array").and.not.empty;
});
pm.collectionVariables.set("levelId", payload.data.allLevel[0].id);
pm.collectionVariables.set("typeId", payload.data.allTreatType[0].id);
""",
        ),
        request(
            "新增医生",
            "POST",
            "/api/doctors",
            json_body={
                "name": "{{doctorName}}",
                "age": 30,
                "sex": 1,
                "levelId": "{{levelId}}",
                "typeId": "{{typeId}}",
                "phoneNumber": "{{phone}}",
                "pwd": "{{doctorPassword}}",
            },
        ),
        request(
            "查询并保存医生 ID",
            "GET",
            "/api/doctors?pn=1&size=200&keyword={{doctorName}}",
            extra_tests="""
const rows = payload.data.doctorInfo.list;
const row = rows.find(item => item.name === pm.collectionVariables.get("doctorName"));
pm.test("Created doctor can be found", () => pm.expect(row).to.be.an("object"));
if (row) {
  pm.collectionVariables.set("doctorId", row.id);
  pm.collectionVariables.set("doctorAccountId", row.accountId);
}
""",
        ),
        request(
            "修改医生",
            "PUT",
            "/api/doctors/{{doctorId}}",
            json_body={
                "accountId": "{{doctorAccountId}}",
                "name": "{{doctorName}}",
                "age": 31,
                "sex": 1,
                "levelId": "{{levelId}}",
                "typeId": "{{typeId}}",
                "phoneNumber": "{{phone}}",
            },
        ),
        request(
            "重复医生手机号错误",
            "POST",
            "/api/doctors",
            json_body={
                "name": "{{doctorName}}重复",
                "age": 30,
                "sex": 1,
                "levelId": "{{levelId}}",
                "typeId": "{{typeId}}",
                "phoneNumber": "{{phone}}",
                "pwd": "{{doctorPassword}}",
            },
            expected=10001,
        ),
        request(
            "医生账号登录",
            "POST",
            "/api/login",
            auth=None,
            form={"username": "{{doctorUsername}}", "password": "{{doctorPassword}}"},
            extra_tests="pm.collectionVariables.set(\"doctorToken\", payload.data.token);",
        ),
        request(
            "医生越权新增材料",
            "POST",
            "/api/materials",
            auth="doctorToken",
            json_body={"title": "{{runId}}-forbidden", "message": "不应写入"},
            expected=10003,
        ),
        request("医生只读访问仪表盘", "GET", "/api/dashboard", auth="doctorToken"),
        request("管理员重置医生密码", "PUT", "/api/doctors/reset/{{doctorAccountId}}"),
        request(
            "旧密码在重置后失效",
            "POST",
            "/api/login",
            auth=None,
            form={"username": "{{doctorUsername}}", "password": "{{doctorPassword}}"},
            expected=10002,
        ),
        request(
            "默认重置密码登录",
            "POST",
            "/api/login",
            auth=None,
            form={"username": "{{doctorUsername}}", "password": "123456"},
            extra_tests="pm.collectionVariables.set(\"doctorResetToken\", payload.data.token);",
        ),
        request("医生退出登录", "POST", "/api/logout", auth="doctorResetToken"),
    )

    uploads = folder(
        "05 图片上传",
        request(
            "上传 JPG/PNG 图片",
            "POST",
            "/api/base/upload",
            formdata=[
                {"key": "file", "type": "file", "src": "{{testImagePath}}", "contentType": "image/png"}
            ],
            extra_tests=(
                "pm.test(\"Image URL is returned\", () => "
                "pm.expect(payload.data.url).to.be.a(\"string\").and.not.empty);"
            ),
        ),
        request(
            "拒绝非图片 MIME",
            "POST",
            "/api/base/upload",
            formdata=[
                {"key": "file", "type": "file", "src": "{{testImagePath}}", "contentType": "text/plain"}
            ],
            expected=10000,
        ),
    )

    materials = folder(
        "06 必备材料管理",
        request(
            "新增必备材料",
            "POST",
            "/api/materials",
            json_body={"title": "{{runId}}-material", "message": "Postman 自动化测试材料"},
        ),
        request(
            "查询并保存材料 ID",
            "GET",
            "/api/materials?pn=1&size=200&keyword={{runId}}-material",
            extra_tests="""
const rows = payload.data.materialInfo.list;
const row = rows.find(item => item.title === `${pm.collectionVariables.get("runId")}-material`);
pm.test("Created material can be found", () => pm.expect(row).to.be.an("object"));
if (row) pm.collectionVariables.set("materialId", row.id);
""",
        ),
        request(
            "修改必备材料",
            "PUT",
            "/api/materials/{{materialId}}",
            json_body={"title": "{{runId}}-material-updated", "message": "已修改"},
        ),
        request("删除必备材料", "DELETE", "/api/materials/{{materialId}}"),
    )

    company_policies = folder(
        "07 医药公司政策管理",
        request(
            "新增医药公司政策",
            "POST",
            "/api/company_policys",
            json_body={
                "companyId": "{{companyId}}",
                "title": "{{runId}}-company-policy",
                "message": "Postman 自动化测试公司政策",
            },
        ),
        request(
            "查询并保存公司政策 ID",
            "GET",
            "/api/company_policys?pn=1&size=200&keyword={{runId}}-company-policy",
            extra_tests="""
const rows = payload.data.policyInfo.list;
const row = rows.find(item => item.title === `${pm.collectionVariables.get("runId")}-company-policy`);
pm.test("Created company policy can be found", () => pm.expect(row).to.be.an("object"));
if (row) pm.collectionVariables.set("companyPolicyId", row.id);
""",
        ),
        request(
            "修改医药公司政策",
            "PUT",
            "/api/company_policys/{{companyPolicyId}}",
            json_body={
                "companyId": "{{companyId}}",
                "title": "{{runId}}-company-policy-updated",
                "message": "已修改",
            },
        ),
        request("删除医药公司政策", "DELETE", "/api/company_policys/{{companyPolicyId}}"),
    )

    medical_policies = folder(
        "08 医保政策管理",
        request(
            "新增医保政策",
            "POST",
            "/api/medical_policys",
            json_body={
                "cityId": "{{existingCityId}}",
                "title": "{{runId}}-medical-policy",
                "message": "Postman 自动化测试医保政策",
                "updateTime": "2026-07-13",
            },
        ),
        request(
            "查询并保存医保政策 ID",
            "GET",
            "/api/medical_policys?pn=1&size=200&title={{runId}}-medical-policy",
            extra_tests="""
const rows = payload.data.policyInfo.list;
const row = rows.find(item => item.title === `${pm.collectionVariables.get("runId")}-medical-policy`);
pm.test("Created medical policy can be found", () => pm.expect(row).to.be.an("object"));
if (row) pm.collectionVariables.set("medicalPolicyId", row.id);
""",
        ),
        request(
            "修改医保政策",
            "PUT",
            "/api/medical_policys/{{medicalPolicyId}}",
            json_body={
                "cityId": "{{existingCityId}}",
                "title": "{{runId}}-medical-policy-updated",
                "message": "已修改",
                "updateTime": "2026-07-13",
            },
        ),
        request("删除医保政策", "DELETE", "/api/medical_policys/{{medicalPolicyId}}"),
    )

    drugs = folder(
        "09 药品管理",
        request(
            "新增药品",
            "POST",
            "/api/drugs",
            json_body={
                "drugName": "{{runId}}-drug",
                "drugInfo": "Postman 自动化测试药品",
                "drugEffect": "仅用于接口验证",
                "drugImg": "/image/postman-test.png",
                "drugPublisher": "Postman Test",
                "saleIds": ["{{saleId}}"],
            },
        ),
        request(
            "查询并保存药品 ID",
            "GET",
            "/api/drugs/1/200?name={{runId}}-drug",
            extra_tests="""
const rows = payload.data.drugPageInfo.list;
const row = rows.find(item => item.drugName === `${pm.collectionVariables.get("runId")}-drug`);
pm.test("Created drug can be found", () => pm.expect(row).to.be.an("object"));
if (row) pm.collectionVariables.set("drugId", row.drugId);
""",
        ),
        request(
            "修改药品",
            "PUT",
            "/api/drugs/{{drugId}}",
            json_body={
                "drugName": "{{runId}}-drug-updated",
                "drugInfo": "已修改",
                "drugEffect": "已修改",
                "drugImg": "/image/postman-test.png",
                "drugPublisher": "Postman Test",
                "saleIds": ["{{saleId}}"],
            },
        ),
        request("删除药品", "DELETE", "/api/drugs/{{drugId}}"),
    )

    cleanup = folder(
        "10 清理与退出",
        request("删除医生", "DELETE", "/api/doctors/{{doctorId}}"),
        request("删除销售地点", "DELETE", "/api/sales/{{saleId}}"),
        request("删除医药公司", "DELETE", "/api/companys/{{companyId}}"),
        request("管理员退出登录", "POST", "/api/logout"),
        request("退出后 Token 失效", "GET", "/api/permissions", expected=10006),
        description="请完整运行集合；若中途停止，按 runId 查询并手工清理临时记录。",
    )

    collection = {
        "info": {
            "_postman_id": "b4e3e34c-302d-45ec-8573-57bf14f9c99d",
            "name": "慧医数字医疗应用系统 API 全流程回归",
            "description": (
                "由后端控制器和 Vue API 契约生成。覆盖认证、权限、仪表盘、8 个业务模块、"
                "上传、错误与越权场景。使用唯一 runId 创建临时数据并在末尾按依赖顺序删除。"
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "item": [
            auth,
            cities,
            companies,
            sales,
            doctors,
            uploads,
            materials,
            company_policies,
            medical_policies,
            drugs,
            cleanup,
        ],
        "variable": [
            {"key": "adminToken", "value": "", "type": "secret"},
            {"key": "invalidToken", "value": "invalid-api-test-token", "type": "string"},
        ],
    }
    output = Path(__file__).parent / "postman" / "medicine-api.postman_collection.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(collection, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    logging.info(output)


if __name__ == "__main__":
    main()
