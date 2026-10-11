#!/usr/bin/env python3
"""
Generate a Postman v2.1 collection from the bundled OpenAPI contract, limited to the
routes the Spring controllers actually implement.

    python3 tools/generate-postman.py

Outputs:
    postman/GetKempt.postman_collection.json
    postman/GetKempt-Local.postman_environment.json

Requires PyYAML (`pip install pyyaml`).
"""
from __future__ import annotations

import glob
import json
import re
import uuid
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
SPEC = ROOT / "src/main/resources/static/openapi.yaml"
CONTROLLERS = ROOT / "src/main/java/au/com/getkempt/controllers"
OUT_DIR = ROOT / "postman"

# Legacy placeholders — not part of the contract, not worth testing.
EXCLUDED_PREFIXES = ("/api/v1/jwt", "/api/v1/shop")

# Operations whose JSON response carries tokens; a test script stores them.
TOKEN_SETTERS = {
    ("POST", "/auth/signup"): "session",
    ("POST", "/auth/login"): "session",
    ("POST", "/auth/reset-password"): "session",
    ("POST", "/auth/refresh"): "tokens",
}

# Sensible defaults for example bodies, keyed by property name.
SAMPLE_BY_NAME = {
    "email": "owner@example.com",
    "password": "Password123",
    "currentPassword": "Password123",
    "newPassword": "Password456",
    "firstName": "Demo",
    "lastName": "Owner",
    "businessName": "Demo Salon",
    "placeId": "ChIJ-demo-place",
    "phone": "+61400000000",
    "name": "Example",
    "notes": "Example note",
    "note": "Example note",
    "reason": "Example reason",
}


# ── implemented routes ─────────────────────────────────────────────────────────

def implemented_routes() -> set[tuple[str, str]]:
    """(METHOD, /api/v1/...) for every live handler method in controllers/**."""
    routes: set[tuple[str, str]] = set()
    for file in glob.glob(str(CONTROLLERS / "**/*.java"), recursive=True):
        src = Path(file).read_text()
        # Drop comments so commented-out handlers aren't counted.
        src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
        src = re.sub(r"//[^\n]*", "", src)
        base_match = re.search(r'@RequestMapping\(\s*"([^"]*)"', src)
        base = base_match.group(1) if base_match else ""
        for m in re.finditer(
            r'@(Get|Post|Put|Patch|Delete)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)"[^)]*\))?', src
        ):
            path = base + (m.group(2) or "")
            if not path.startswith(EXCLUDED_PREFIXES):
                routes.add((m.group(1).upper(), path))
    return routes


# ── schema → example ──────────────────────────────────────────────────────────

class Exampler:
    def __init__(self, spec: dict):
        self.spec = spec

    def resolve(self, node: dict) -> dict:
        while isinstance(node, dict) and "$ref" in node:
            ref = node["$ref"]
            assert ref.startswith("#/"), ref
            target = self.spec
            for part in ref[2:].split("/"):
                target = target[part]
            node = target
        return node

    def example(self, schema: dict, name: str = "", depth: int = 0) -> object:
        schema = self.resolve(schema)
        if "example" in schema:
            return schema["example"]
        if "default" in schema:
            return schema["default"]
        if "enum" in schema:
            return schema["enum"][0]
        if "allOf" in schema:
            merged: dict = {}
            for part in schema["allOf"]:
                value = self.example(part, name, depth)
                if isinstance(value, dict):
                    merged.update(value)
            return merged
        for key in ("oneOf", "anyOf"):
            if key in schema:
                return self.example(schema[key][0], name, depth)

        kind = schema.get("type")
        if kind == "object" or "properties" in schema:
            if depth > 6:
                return {}
            out = {}
            for prop, sub in (schema.get("properties") or {}).items():
                sub_resolved = self.resolve(sub)
                if sub_resolved.get("readOnly"):
                    continue
                out[prop] = self.example(sub, prop, depth + 1)
            return out
        if kind == "array":
            return [self.example(schema.get("items", {}), name, depth + 1)]
        if kind == "integer":
            return int(schema.get("minimum", 1))
        if kind == "number":
            return float(schema.get("minimum", 1))
        if kind == "boolean":
            return True
        if kind == "string":
            return self.string_example(schema, name)
        return None

    @staticmethod
    def string_example(schema: dict, name: str) -> str:
        if name in SAMPLE_BY_NAME:
            return SAMPLE_BY_NAME[name]
        fmt = schema.get("format")
        if fmt == "date-time":
            return "2026-11-01T09:00:00Z"
        if fmt == "date":
            return "2026-11-01"
        if fmt == "email":
            return "owner@example.com"
        if fmt == "uri":
            return "https://example.com"
        if name.endswith("Id") or name == "id":
            return f"{{{{{name}}}}}"  # becomes a Postman variable, e.g. {{clientId}}
        if fmt == "uuid":
            return "00000000-0000-4000-8000-000000000000"
        if re.fullmatch(r"\d{2}:\d{2}", str(schema.get("pattern", ""))):
            return "09:00"
        if "time" in name.lower() and "Time" in name:
            return "09:00"
        return "string"


# ── postman building blocks ───────────────────────────────────────────────────

def postman_path(path: str) -> list[str]:
    """'/clients/{clientId}' -> ['api','v1','clients',':clientId']"""
    segs = ["api", "v1"] + [s for s in path.split("/") if s]
    return [re.sub(r"\{(\w+)}", r":\1", s) for s in segs]


def build_request(method: str, path: str, op: dict, ex: Exampler) -> dict:
    params = [ex.resolve(p) for p in op.get("parameters", [])]
    query = []
    variables = []
    headers = [{"key": "Accept", "value": "application/json"}]

    for p in params:
        where = p.get("in")
        schema = p.get("schema", {})
        sample = ex.example(schema, p["name"])
        if where == "query":
            query.append({
                "key": p["name"],
                "value": "" if sample is None else str(sample),
                "disabled": not p.get("required", False),
                "description": (p.get("description") or "").strip(),
            })
        elif where == "path":
            variables.append({
                "key": p["name"],
                "value": f"{{{{{p['name']}}}}}",
                "description": (p.get("description") or "").strip(),
            })
        elif where == "header" and p["name"].lower() != "authorization":
            headers.append({
                "key": p["name"],
                "value": "" if sample is None else str(sample),
                "disabled": not p.get("required", False),
            })

    # Path params that the spec declares at path-level may not be in op params.
    declared = {v["key"] for v in variables}
    for name in re.findall(r"\{(\w+)}", path):
        if name not in declared:
            variables.append({"key": name, "value": f"{{{{{name}}}}}"})

    request = {
        "method": method,
        "header": headers,
        "url": {
            "raw": "{{baseUrl}}/" + "/".join(postman_path(path))
                   + ("?" + "&".join(f"{q['key']}={q['value']}" for q in query if not q["disabled"])
                      if any(not q["disabled"] for q in query) else ""),
            "host": ["{{baseUrl}}"],
            "path": postman_path(path),
            "query": query,
            "variable": variables,
        },
        "description": (op.get("description") or op.get("summary") or "").strip(),
    }

    body = op.get("requestBody")
    if body:
        content = ex.resolve(body).get("content", {})
        if "application/json" in content:
            sample = ex.example(content["application/json"].get("schema", {}))
            headers.append({"key": "Content-Type", "value": "application/json"})
            request["body"] = {
                "mode": "raw",
                "raw": json.dumps(sample, indent=2),
                "options": {"raw": {"language": "json"}},
            }
        elif "multipart/form-data" in content:
            request["body"] = {"mode": "formdata", "formdata": []}

    if op.get("security") == []:
        request["auth"] = {"type": "noauth"}

    return request


def token_script(kind: str) -> dict:
    if kind == "session":
        lines = [
            "if (pm.response.code < 300) {",
            "  const body = pm.response.json();",
            "  pm.environment.set('accessToken', body.tokens.accessToken);",
            "  pm.environment.set('refreshToken', body.tokens.refreshToken);",
            "  if (body.user && body.user.id) pm.environment.set('userId', body.user.id);",
            "  if (body.user && body.user.shopId) pm.environment.set('shopId', body.user.shopId);",
            "}",
        ]
    else:
        lines = [
            "if (pm.response.code < 300) {",
            "  const body = pm.response.json();",
            "  pm.environment.set('accessToken', body.accessToken);",
            "  pm.environment.set('refreshToken', body.refreshToken);",
            "}",
        ]
    return {"listen": "test", "script": {"type": "text/javascript", "exec": lines}}


def capture_id_script(path: str) -> dict | None:
    """After a successful POST /<resource>, remember the new id as {{<singular>Id}}."""
    segs = [s for s in path.split("/") if s]
    if len(segs) != 1:
        return None
    resource = segs[0]
    singular = resource[:-1] if resource.endswith("s") else resource
    var = f"{singular}Id"
    return {
        "listen": "test",
        "script": {
            "type": "text/javascript",
            "exec": [
                "if (pm.response.code < 300) {",
                "  const body = pm.response.json();",
                f"  if (body && body.id) pm.environment.set('{var}', body.id);",
                "}",
            ],
        },
    }


# ── main ──────────────────────────────────────────────────────────────────────

def main() -> None:
    spec = yaml.safe_load(SPEC.read_text())
    ex = Exampler(spec)
    live = implemented_routes()

    folders: dict[str, list] = {}
    tag_order = [t["name"] for t in spec.get("tags", [])]
    included = skipped = 0

    for path, item in spec["paths"].items():
        for method, op in item.items():
            if method not in ("get", "post", "put", "patch", "delete"):
                continue
            full = "/api/v1" + path
            if (method.upper(), full) not in live:
                skipped += 1
                continue
            included += 1
            tag = (op.get("tags") or ["Other"])[0]
            entry = {
                "name": op.get("summary") or f"{method.upper()} {path}",
                "request": build_request(method.upper(), path, op, ex),
                "response": [],
            }
            events = []
            setter = TOKEN_SETTERS.get((method.upper(), path))
            if setter:
                events.append(token_script(setter))
            elif method == "post":
                script = capture_id_script(path)
                if script:
                    events.append(script)
            if events:
                entry["event"] = events
            folders.setdefault(tag, []).append(entry)

    ordered = sorted(folders, key=lambda t: tag_order.index(t) if t in tag_order else 999)
    collection = {
        "info": {
            "_postman_id": str(uuid.uuid5(uuid.NAMESPACE_URL, "getkempt-collection")),
            "name": "GetKempt API",
            "description": (
                "Generated from src/main/resources/static/openapi.yaml by tools/generate-postman.py, "
                "limited to routes implemented by the backend.\n\n"
                "1. Select the *GetKempt Local* environment.\n"
                "2. Run **Auth → Signup** (or **Login**); the test script stores `accessToken`.\n"
                "3. Every other request sends `Authorization: Bearer {{accessToken}}` automatically.\n"
                "Create requests store the new resource id (e.g. `clientId`) for the follow-up calls."
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        },
        "auth": {"type": "bearer", "bearer": [{"key": "token", "value": "{{accessToken}}", "type": "string"}]},
        "variable": [{"key": "baseUrl", "value": "http://localhost:4000"}],
        "item": [{"name": tag, "item": folders[tag]} for tag in ordered],
    }

    environment = {
        "id": str(uuid.uuid5(uuid.NAMESPACE_URL, "getkempt-local-env")),
        "name": "GetKempt Local",
        "values": [
            {"key": "baseUrl", "value": "http://localhost:4000", "enabled": True},
            {"key": "accessToken", "value": "", "enabled": True},
            {"key": "refreshToken", "value": "", "enabled": True},
            {"key": "userId", "value": "", "enabled": True},
            {"key": "shopId", "value": "", "enabled": True},
        ],
        "_postman_variable_scope": "environment",
    }

    OUT_DIR.mkdir(exist_ok=True)
    (OUT_DIR / "GetKempt.postman_collection.json").write_text(json.dumps(collection, indent=2) + "\n")
    (OUT_DIR / "GetKempt-Local.postman_environment.json").write_text(json.dumps(environment, indent=2) + "\n")
    print(f"{included} requests in {len(folders)} folders written to {OUT_DIR} "
          f"({skipped} spec operations skipped as not implemented)")


if __name__ == "__main__":
    main()
