"""Integration tests for MCP tools via mock TCP server."""

import asyncio
import json
import pytest
from unittest.mock import AsyncMock, MagicMock

from mcp_bridge_server.protocol import TcpResponse


@pytest.fixture
def mock_tcp_client():
    """Create a mock TCP client that returns predefined responses."""
    client = AsyncMock()
    client.send = AsyncMock()
    client.is_connected = True
    return client


@pytest.mark.asyncio
async def test_give_item_success(mock_tcp_client):
    mock_tcp_client.send.return_value = TcpResponse(
        id="test-1", status="ok", result={"message": "Gave 1x minecraft:diamond"}
    )
    resp = await mock_tcp_client.send("world.give_item", {"player": "Steve", "item": "minecraft:diamond"})
    assert resp.is_ok
    assert "diamond" in resp.result["message"]


@pytest.mark.asyncio
async def test_give_item_player_not_found(mock_tcp_client):
    mock_tcp_client.send.return_value = TcpResponse(
        id="test-2", status="error",
        error={"code": "PLAYER_NOT_FOUND", "message": "Player not found"}
    )
    resp = await mock_tcp_client.send("world.give_item", {"player": "Nobody", "item": "minecraft:diamond"})
    assert not resp.is_ok
    assert resp.error["code"] == "PLAYER_NOT_FOUND"


@pytest.mark.asyncio
async def test_simulate_splice(mock_tcp_client):
    mock_tcp_client.send.return_value = TcpResponse(
        id="test-3", status="ok",
        result={
            "child": {"speed": 6, "yield": 5, "potency": 7},
            "mutation": {"speed": 0, "yield": 0, "potency": 0},
            "formula": "(parent_a + parent_b) / 2 + mutation(-2..+2)",
        }
    )
    resp = await mock_tcp_client.send("mod.simulate_splice", {
        "parent_a_genes": {"speed": 5, "yield": 5, "potency": 5},
        "parent_b_genes": {"speed": 7, "yield": 5, "potency": 9},
    })
    assert resp.is_ok
    assert resp.result["child"]["speed"] == 6


@pytest.mark.asyncio
async def test_create_seed(mock_tcp_client):
    mock_tcp_client.send.return_value = TcpResponse(
        id="test-4", status="ok",
        result={"message": "Created seed with genes S=8 Y=3 P=7"}
    )
    resp = await mock_tcp_client.send("mod.create_seed", {
        "speed": 8, "yield": 3, "potency": 7, "player": "Steve"
    })
    assert resp.is_ok
