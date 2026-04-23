#!/usr/bin/env python3
"""
f33d MCP server (stdio transport) — stdlib only, no pip required.

Exposes one tool: send_message

Config via env vars:
  F33D_URL    — server base URL  (default: http://localhost:8080)
  F33D_TOKEN  — auth token

Claude Desktop / claude.json config:
  {
    "mcpServers": {
      "f33d": {
        "command": "python3",
        "args": ["/path/to/f33d/clients/mcp-server/f33d-mcp.py"],
        "env": {
          "F33D_URL": "https://your-f33d-host",
          "F33D_TOKEN": "your-token"
        }
      }
    }
  }
"""

import json
import os
import sys
import urllib.request
import urllib.error

F33D_URL = os.environ.get("F33D_URL", "http://localhost:8080").rstrip("/")
F33D_TOKEN = os.environ.get("F33D_TOKEN", "")

TOOLS = [
    {
        "name": "send_message",
        "description": (
            "Send a notification message to the f33d feed. "
            "Use this to notify the user when a task completes, "
            "a build finishes, an analysis is done, or anything noteworthy happens."
        ),
        "inputSchema": {
            "type": "object",
            "properties": {
                "message": {
                    "type": "string",
                    "description": "The message text to send"
                }
            },
            "required": ["message"]
        }
    }
]


def post_message(message: str) -> dict:
    payload = json.dumps({"message": message}).encode()
    req = urllib.request.Request(
        f"{F33D_URL}/api/message",
        data=payload,
        headers={
            "Authorization": f"Bearer {F33D_TOKEN}",
            "Content-Type": "application/json"
        },
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return {"ok": True}
    except urllib.error.HTTPError as e:
        return {"ok": False, "error": f"HTTP {e.code}: {e.reason}"}
    except Exception as e:
        return {"ok": False, "error": str(e)}


def handle(req: dict):
    method = req.get("method", "")
    req_id = req.get("id")

    if method == "initialize":
        return {
            "jsonrpc": "2.0", "id": req_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {"tools": {}},
                "serverInfo": {"name": "f33d", "version": "1.0.0"}
            }
        }

    if method == "tools/list":
        return {"jsonrpc": "2.0", "id": req_id, "result": {"tools": TOOLS}}

    if method == "tools/call":
        params = req.get("params", {})
        name = params.get("name")
        args = params.get("arguments", {})
        if name == "send_message":
            result = post_message(args.get("message", ""))
            text = "Message sent." if result["ok"] else f"Failed: {result.get('error')}"
            return {
                "jsonrpc": "2.0", "id": req_id,
                "result": {
                    "content": [{"type": "text", "text": text}],
                    "isError": not result["ok"]
                }
            }
        return {
            "jsonrpc": "2.0", "id": req_id,
            "error": {"code": -32601, "message": f"Unknown tool: {name}"}
        }

    if method in ("notifications/initialized",):
        return None

    if req_id is not None:
        return {
            "jsonrpc": "2.0", "id": req_id,
            "error": {"code": -32601, "message": f"Method not found: {method}"}
        }
    return None


def main():
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            response = handle(json.loads(line))
            if response is not None:
                sys.stdout.write(json.dumps(response) + "\n")
                sys.stdout.flush()
        except json.JSONDecodeError as e:
            sys.stdout.write(json.dumps({
                "jsonrpc": "2.0", "id": None,
                "error": {"code": -32700, "message": f"Parse error: {e}"}
            }) + "\n")
            sys.stdout.flush()


if __name__ == "__main__":
    main()
