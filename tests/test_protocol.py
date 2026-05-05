import json
import pytest
from mcp_bridge_server.protocol import TcpRequest, TcpResponse, HandshakeMessage, parse_message


def test_tcp_request_to_json():
    req = TcpRequest(method="world.give_item", params={"player": "Steve"})
    data = json.loads(req.to_json())
    assert data["type"] == "request"
    assert data["method"] == "world.give_item"
    assert data["params"]["player"] == "Steve"
    assert "id" in data


def test_tcp_response_from_json():
    resp = TcpResponse.from_json({
        "id": "test-123",
        "status": "ok",
        "result": {"message": "done"},
    })
    assert resp.is_ok
    assert resp.id == "test-123"
    assert resp.result["message"] == "done"


def test_tcp_response_error():
    resp = TcpResponse.from_json({
        "id": "test-456",
        "status": "error",
        "error": {"code": "PLAYER_NOT_FOUND", "message": "No player"},
    })
    assert not resp.is_ok
    assert resp.error["code"] == "PLAYER_NOT_FOUND"


def test_handshake_to_json():
    hs = HandshakeMessage(token="secret123")
    data = json.loads(hs.to_json())
    assert data["type"] == "handshake"
    assert data["token"] == "secret123"


def test_parse_message():
    result = parse_message('{"type": "ping"}')
    assert result["type"] == "ping"
