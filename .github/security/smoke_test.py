"""Security checks against a running Clarity, as started by docker compose.

Reads every endpoint from the api's sources, so a new endpoint is covered
without touching this file, and checks from the outside what the unit tests
check from the inside: without a session nothing but setup and login
answers, state-changing requests need the panel's header, every answer
carries its security headers, and repeated wrong logins lock the address
out, also when the browser claims to be someone else.

Usage: python3 .github/security/smoke_test.py http://127.0.0.1:8080
"""

import json
import pathlib
import re
import sys
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOURCES = ROOT / "api" / "src" / "main" / "java"
MAPPING = re.compile(
    r'@RequestMapping\(\s*path\s*=\s*"([^"]+)",\s*'
    r'method\s*=\s*RequestMethod\.([A-Z]+)\s*\)')
PUBLIC = {("GET", "/setup/status/"), ("POST", "/setup/"),
          ("POST", "/setup/confirm/"), ("POST", "/login/")}
PANEL_HEADERS = {
    "Content-Security-Policy": "default-src 'self'",
    "X-Content-Type-Options": "nosniff",
    "X-Frame-Options": "DENY",
    "Referrer-Policy": "no-referrer",
    "Permissions-Policy": "camera=()",
    "Cross-Origin-Opener-Policy": "same-origin",
}
API_HEADERS = {
    "Cache-Control": "no-store",
    "X-Content-Type-Options": "nosniff",
    "Content-Security-Policy": "default-src 'none'",
}

failures = []


def endpoints():
    """Every (method, path) the api maps, and whether it is public."""
    found = set()
    for source in SOURCES.rglob("*.java"):
        text = source.read_text(encoding="utf-8")
        for match in MAPPING.finditer(text):
            start = max(text.rfind(";", 0, match.start()),
                        text.rfind("}", 0, match.start()))
            end = text.find("(", match.end())
            marked = "@PublicEndpoint" in (text[start:match.start()] +
                                           text[match.end():end])
            found.add((match.group(2), match.group(1), marked))
    return found


def request(base, method, path, headers=None, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data=data, method=method,
                                 headers=headers or {})
    if data is not None:
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            return response.status, dict(response.headers), response.read()
    except urllib.error.HTTPError as error:
        return error.code, dict(error.headers), error.read()


def check(condition, message):
    print(("ok    " if condition else "FAIL  ") + message)
    if not condition:
        failures.append(message)


def check_headers(headers, expected, where):
    for name, part in expected.items():
        value = headers.get(name, "")
        check(part in value, f"{where} sends {name}: {part}")


def error_code(body):
    try:
        return json.loads(body).get("error", {}).get("code")
    except ValueError:
        return None


def main(base):
    base = base.rstrip("/")
    mapped = endpoints()
    public = {(method, path) for method, path, marked in mapped if marked}
    check(len(mapped) > 20, f"found {len(mapped)} endpoints in the sources")
    check(public == PUBLIC, "only setup and login are public: "
          + ", ".join(sorted(f"{m} {p}" for m, p in public)))

    for method, path, marked in sorted(mapped):
        if marked:
            continue
        status, headers, _ = request(base, method, "/v1" + path,
                                     {"X-Clarity-Request": "1"},
                                     {} if method == "POST" else None)
        check(status == 401, f"{method} {path} without a session: {status}")

    for method, path, _ in sorted(mapped):
        if method != "POST":
            continue
        status, _, _ = request(base, method, "/v1" + path, body={})
        check(status == 403, f"POST {path} without the panel header: {status}")

    status, headers, _ = request(base, "GET", "/v1/setup/status/")
    check(status == 200, f"GET /setup/status/ is reachable: {status}")
    check_headers(headers, API_HEADERS, "the api")
    check("Server" not in headers or not re.search(r"\d", headers["Server"]),
          f"the api hides its server version: {headers.get('Server')}")

    status, headers, _ = request(base, "GET", "/")
    check(status == 200, f"the panel loads: {status}")
    check_headers(headers, PANEL_HEADERS, "the panel")
    check("Server" not in headers or not re.search(r"\d", headers["Server"]),
          f"the panel hides its server version: {headers.get('Server')}")

    credentials = {"name": "nobody", "password": "wrong-password", "code": "0"}
    codes = [error_code(request(base, "POST", "/v1/login/",
                                {"X-Clarity-Request": "1"}, credentials)[2])
             for _ in range(6)]
    check(codes[:5] == [1000] * 5 and codes[5] == 1001,
          f"five wrong logins lock the address out: {codes}")
    spoofed = request(base, "POST", "/v1/login/",
                      {"X-Clarity-Request": "1", "X-Real-IP": "203.0.113.7",
                       "X-Forwarded-For": "203.0.113.7"}, credentials)
    check(error_code(spoofed[2]) == 1001,
          "a forged client address does not lift the lock")

    print()
    if failures:
        print(f"{len(failures)} check(s) failed")
        return 1
    print("all checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else
                  "http://127.0.0.1:8080"))
