"""TCP protocol types for MCP Bridge Server <-> Minecraft communication."""

from __future__ import annotations

import json
import uuid
from dataclasses import dataclass, field
from typing import Any


@dataclass
class TcpRequest:
    method: str
    params: dict[str, Any] = field(default_factory=dict)
    id: str = field(default_factory=lambda: str(uuid.uuid4()))
    type: str = "request"

    def to_json(self) -> str:
        return json.dumps({
            "id": self.id,
            "type": self.type,
            "method": self.method,
            "params": self.params,
        }) + "\n"


@dataclass
class TcpResponse:
    id: str
    status: str
    result: dict[str, Any] | None = None
    error: dict[str, Any] | None = None

    @classmethod
    def from_json(cls, data: dict) -> TcpResponse:
        return cls(
            id=data.get("id", ""),
            status=data.get("status", "error"),
            result=data.get("result"),
            error=data.get("error"),
        )

    @property
    def is_ok(self) -> bool:
        return self.status == "ok"


@dataclass
class HandshakeMessage:
    token: str
    version: str = "1.0.0"

    def to_json(self) -> str:
        return json.dumps({
            "type": "handshake",
            "token": self.token,
            "version": self.version,
        }) + "\n"


def parse_message(line: str) -> dict:
    """Parse a JSON line message."""
    return json.loads(line.strip())
