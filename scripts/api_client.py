#!/usr/bin/env python3
"""CLI client for NQuiz AI Service API."""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from typing import Any


DEFAULT_BASE_URL = "http://localhost:8080"

PROVIDERS = ("GROQ", "OPENAI", "GEMINI", "DEEPSEEK")
MODES = ("ONE_CORRECT", "MULTI_CORRECT", "ORDERING", "MATCHING")
DIFFICULTIES = ("A1", "A2", "B1", "B2", "C1", "C2")
QUESTION_TYPES = (
    "GRAMMAR", "TENSES", "ARTICLES", "VOCABULARY", "PHRASAL_VERBS", "IDIOMS",
    "PREPOSITIONS", "WORD_FORMATION", "COLLOCATIONS", "SYNONYMS", "READING",
    "LISTENING", "DIALOGUES", "ERROR_CORRECTION", "TRANSFORMATION", "GAP_FILLING",
    "WORD_ORDER", "PRONUNCIATION", "REAL_LIFE",
)

MAIN_EXAMPLES = """
examples:
  python scripts/api_client.py health --pretty

  python scripts/api_client.py capabilities --lang en

  python scripts/api_client.py token-info --token sk_user_...

  python scripts/api_client.py questions \\
    --token sk_user_... \\
    --provider DEEPSEEK \\
    --count 3 \\
    --mode ONE_CORRECT \\
    --language ENGLISH \\
    --difficulty B1 \\
    --type GRAMMAR \\
    --keywords "present tense" \\
    --pretty

  python scripts/api_client.py create-user \\
    --admin-token sk_admin_... \\
    --license-no 100 \\
    --license-org "Acme Corp" \\
    --email user@acme.com \\
    --balance 50

  python scripts/api_client.py list-tokens --admin-token sk_admin_... --pretty

  python scripts/api_client.py topup-license \\
    --admin-token sk_admin_... \\
    --license-no 100 \\
    --amount 25
"""

COMMAND_EXAMPLES = {
    "health": """
example:
  python scripts/api_client.py health --pretty
""",
    "capabilities": """
example:
  python scripts/api_client.py capabilities --lang en
""",
    "token-info": """
example:
  python scripts/api_client.py token-info --token sk_user_...
""",
    "questions": """
example:
  python scripts/api_client.py questions \\
    --token sk_user_... \\
    --provider DEEPSEEK \\
    --count 3 \\
    --mode ONE_CORRECT \\
    --language ENGLISH \\
    --difficulty B1 \\
    --type GRAMMAR \\
    --keywords "present tense" \\
    --pretty
""",
    "create-user": """
example:
  python scripts/api_client.py create-user \\
    --admin-token sk_admin_... \\
    --license-no 100 \\
    --license-org "Acme Corp" \\
    --email user@acme.com \\
    --balance 50
""",
    "create-admin": """
example:
  python scripts/api_client.py create-admin --admin-token sk_admin_...
""",
    "deactivate": """
example:
  python scripts/api_client.py deactivate --admin-token sk_admin_... --license-no 1
""",
    "activate": """
example:
  python scripts/api_client.py activate --admin-token sk_admin_... --license-no 1
""",
    "list-tokens": """
example:
  python scripts/api_client.py list-tokens --admin-token sk_admin_... --pretty
""",
    "topup-token": """
example:
  python scripts/api_client.py topup-token \\
    --admin-token sk_admin_... \\
    --token-value sk_user_... \\
    --amount 25
""",
    "topup-license": """
example:
  python scripts/api_client.py topup-license \\
    --admin-token sk_admin_... \\
    --license-no 100 \\
    --amount 25
""",
    "webhook": """
example:
  python scripts/api_client.py webhook --file payment-event.json
""",
}


class ApiClient:
    def __init__(self, base_url: str, timeout: int = 120) -> None:
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout

    def request(
        self,
        method: str,
        path: str,
        *,
        token: str | None = None,
        query: dict[str, str] | None = None,
        body: Any | None = None,
        raw_body: str | None = None,
        content_type: str = "application/json",
    ) -> tuple[int, str]:
        url = f"{self.base_url}{path}"
        if query:
            url = f"{url}?{urllib.parse.urlencode(query)}"

        headers: dict[str, str] = {"Accept": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"

        data: bytes | None = None
        if raw_body is not None:
            data = raw_body.encode("utf-8")
            headers["Content-Type"] = content_type
        elif body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = content_type

        req = urllib.request.Request(url, data=data, headers=headers, method=method.upper())

        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                payload = resp.read().decode("utf-8")
                return resp.status, payload
        except urllib.error.HTTPError as exc:
            payload = exc.read().decode("utf-8", errors="replace")
            return exc.code, payload


def print_response(status: int, payload: str, pretty: bool) -> int:
    print(f"HTTP {status}")
    if not payload:
        return 0 if 200 <= status < 300 else 1

    try:
        parsed = json.loads(payload)
        if pretty:
            print(json.dumps(parsed, indent=2, ensure_ascii=False))
        else:
            print(json.dumps(parsed, ensure_ascii=False))
    except json.JSONDecodeError:
        print(payload)

    return 0 if 200 <= status < 300 else 1


def add_global_args(parser: argparse.ArgumentParser) -> None:
    parser.add_argument(
        "--base-url",
        default=DEFAULT_BASE_URL,
        help=f"Service base URL (default: {DEFAULT_BASE_URL})",
    )
    parser.add_argument("--timeout", type=int, default=120, help="Request timeout in seconds")
    parser.add_argument("--pretty", action="store_true", help="Pretty-print JSON responses")


def cmd_health(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    status, payload = client.request("GET", "/health")
    return print_response(status, payload, args.pretty)


def cmd_capabilities(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    query = {"lang": args.lang} if args.lang else None
    status, payload = client.request("GET", "/api/capabilities", query=query)
    return print_response(status, payload, args.pretty)


def cmd_token_info(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    status, payload = client.request("GET", "/api/token/info", token=args.token)
    return print_response(status, payload, args.pretty)


def cmd_questions(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    body = {
        "provider": args.provider,
        "count": args.count,
        "mode": args.mode,
        "language": args.language,
        "difficulty": args.difficulty,
        "type": args.type,
        "keywords": args.keywords,
    }
    status, payload = client.request("POST", "/api/questions", token=args.token, body=body)
    return print_response(status, payload, args.pretty)


def cmd_create_user(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    body = {
        "license_no": args.license_no,
        "license_org": args.license_org,
        "email": args.email,
        "balance": args.balance,
        "admin": args.admin,
    }
    status, payload = client.request(
        "POST", "/admin/tokens/create-user", token=args.admin_token, body=body
    )
    return print_response(status, payload, args.pretty)


def cmd_create_admin(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    body = {
        "license_no": 0,
        "license_org": "NQuiz-AI-service Admin",
        "email": "root@localhost",
        "balance": 0,
        "admin": True,
    }
    status, payload = client.request(
        "POST", "/admin/tokens/create-admin", token=args.admin_token, body=body
    )
    return print_response(status, payload, args.pretty)


def cmd_deactivate(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    status, payload = client.request(
        "POST",
        f"/admin/tokens/deactivate/{args.license-no}",
        token=args.admin_token,
    )
    return print_response(status, payload, args.pretty)


def cmd_activate(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    status, payload = client.request(
        "POST",
        f"/admin/tokens/activate/{args.license-no}",
        token=args.admin_token,
    )
    return print_response(status, payload, args.pretty)


def cmd_list_tokens(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    status, payload = client.request("GET", "/admin/tokens/all", token=args.admin_token)
    return print_response(status, payload, args.pretty)


def cmd_get_token(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    status, payload = client.request(
        "GET", 
        f"/admin/tokens/info/{args.license-no}", 
        token=args.admin_token)
    return print_response(status, payload, args.pretty)


def cmd_topup_token(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    status, payload = client.request(
        "POST",
        f"/admin/tokens/{args.token_value}/topup",
        token=args.admin_token,
        body={"amount": args.amount},
    )
    return print_response(status, payload, args.pretty)


def cmd_topup_license(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    status, payload = client.request(
        "POST",
        f"/admin/tokens/{args.license_no}/topup",
        token=args.admin_token,
        body={"amount": args.amount},
    )
    return print_response(status, payload, args.pretty)


def cmd_webhook(args: argparse.Namespace) -> int:
    client = ApiClient(args.base_url, args.timeout)
    raw = args.json
    if args.file:
        with open(args.file, encoding="utf-8") as fh:
            raw = fh.read()
    if raw is None:
        print("Provide --json or --file", file=sys.stderr)
        return 2

    status, payload = client.request("POST", "/admin/tokens/webhook", raw_body=raw)
    return print_response(status, payload, args.pretty)


def add_subparser(sub, name: str, help_text: str) -> argparse.ArgumentParser:
    return sub.add_parser(
        name,
        help=help_text,
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=COMMAND_EXAMPLES.get(name, "").strip() or None,
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Call NQuiz AI Service API endpoints from the command line.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=MAIN_EXAMPLES.strip(),
    )
    add_global_args(parser)
    sub = parser.add_subparsers(dest="command", required=True)

    add_subparser(sub, "health", "GET /health").set_defaults(handler=cmd_health)

    p = add_subparser(sub, "capabilities", "GET /api/capabilities")
    p.add_argument("--lang", choices=("en", "ru"), help="Response language")
    p.set_defaults(handler=cmd_capabilities)

    p = add_subparser(sub, "token-info", "GET /api/token/info")
    p.add_argument("--token", required=True, help="User API token (Bearer)")
    p.set_defaults(handler=cmd_token_info)

    p = add_subparser(sub, "questions", "POST /api/questions")
    p.add_argument("--token", required=True, help="User API token (Bearer)")
    p.add_argument("--provider", required=True, choices=PROVIDERS)
    p.add_argument("--count", type=int, default=1)
    p.add_argument("--mode", default="ONE_CORRECT", choices=MODES)
    p.add_argument("--language", default="ENGLISH")
    p.add_argument("--difficulty", default="B1", choices=DIFFICULTIES)
    p.add_argument("--type", default="GRAMMAR", choices=QUESTION_TYPES)
    p.add_argument("--keywords", default="", help="Comma-separated topic keywords")
    p.set_defaults(handler=cmd_questions)

    p = add_subparser(sub, "create-user", "POST /admin/tokens/create-user")
    p.add_argument("--admin-token", required=True)
    p.add_argument("--license-no", type=int, required=True)
    p.add_argument("--license-org", required=True)
    p.add_argument("--email", required=True)
    p.add_argument("--balance", type=int, default=0)
    p.add_argument("--admin", action="store_true", help="Create admin-capable user token")
    p.set_defaults(handler=cmd_create_user)

    p = add_subparser(sub, "create-admin", "POST /admin/tokens/create-admin")
    p.add_argument("--admin-token", required=True)
    p.set_defaults(handler=cmd_create_admin)

    p = add_subparser(sub, "deactivate", "POST /admin/tokens/deactivate/{license_no}")
    p.add_argument("--admin-token", required=True)
    p.add_argument("--license-no", type=int, required=True, help="Nibelung license number")
    p.set_defaults(handler=cmd_deactivate)

    p = add_subparser(sub, "activate", "POST /admin/tokens/activate/{license_no}")
    p.add_argument("--admin-token", required=True)
    p.add_argument("--license-no", type=int, required=True, help="Nibelung license number")
    p.set_defaults(handler=cmd_activate)

    p = add_subparser(sub, "list-tokens", "GET /admin/tokens/all")
    p.add_argument("--admin-token", required=True)
    p.set_defaults(handler=cmd_list_tokens)

    p = add_subparser(sub, "get-token", "GET /admin/tokens/info/{license_no}")
    p.add_argument("--admin-token", required=True)
    p.add_argument("--license-no", type=int, required=True, help="Nibelung license number")
    p.set_defaults(handler=cmd_get_token)

    p = add_subparser(sub, "topup-token", "POST /admin/tokens/{token}/topup")
    p.add_argument("--admin-token", required=True)
    p.add_argument("--token-value", required=True, help="Full user token string")
    p.add_argument("--amount", type=int, required=True)
    p.set_defaults(handler=cmd_topup_token)

    p = add_subparser(sub, "topup-license", "POST /admin/tokens/{license_no}/topup")
    p.add_argument("--admin-token", required=True)
    p.add_argument("--license-no", type=int, required=True)
    p.add_argument("--amount", type=int, required=True)
    p.set_defaults(handler=cmd_topup_license)

    p = add_subparser(sub, "webhook", "POST /admin/tokens/webhook")
    p.add_argument("--json", help="Raw webhook JSON string")
    p.add_argument("--file", help="Path to JSON file with webhook payload")
    p.set_defaults(handler=cmd_webhook)

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    return args.handler(args)


if __name__ == "__main__":
    raise SystemExit(main())
