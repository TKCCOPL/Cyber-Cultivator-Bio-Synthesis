"""TCP client for connecting to Minecraft Forge MCP Bridge mod."""

from __future__ import annotations

import asyncio
import json
import logging
from typing import Any

from .protocol import TcpRequest, TcpResponse, HandshakeMessage, parse_message

logger = logging.getLogger(__name__)


class TcpClient:
    def __init__(self, host: str = "localhost", port: int = 25580, token: str = "change-me"):
        self.host = host
        self.port = port
        self.token = token
        self._reader: asyncio.StreamReader | None = None
        self._writer: asyncio.StreamWriter | None = None
        self._pending: dict[str, asyncio.Future[TcpResponse]] = {}
        self._connected = False
        self._listen_task: asyncio.Task | None = None

    async def connect(self) -> None:
        """Connect to the Minecraft MCP Bridge TCP server."""
        self._reader, self._writer = await asyncio.open_connection(self.host, self.port)
        # Send handshake
        hs = HandshakeMessage(token=self.token)
        self._writer.write(hs.to_json().encode())
        await self._writer.drain()

        # Read handshake ack
        line = await self._reader.readline()
        ack = parse_message(line.decode())
        if ack.get("status") != "ok":
            raise ConnectionError(f"Handshake failed: {ack.get('message', 'Unknown error')}")

        self._connected = True
        self._listen_task = asyncio.create_task(self._listen())
        logger.info("Connected to Minecraft MCP Bridge at %s:%d", self.host, self.port)

    async def disconnect(self) -> None:
        """Disconnect from the server."""
        self._connected = False
        if self._listen_task:
            self._listen_task.cancel()
        if self._writer:
            self._writer.close()

    async def send(self, method: str, params: dict[str, Any] | None = None, timeout: float = 15.0) -> TcpResponse:
        """Send a request and wait for response."""
        if not self._connected:
            raise ConnectionError("Not connected")

        req = TcpRequest(method=method, params=params or {})
        future: asyncio.Future[TcpResponse] = asyncio.get_event_loop().create_future()
        self._pending[req.id] = future

        self._writer.write(req.to_json().encode())
        await self._writer.drain()

        try:
            return await asyncio.wait_for(future, timeout)
        except asyncio.TimeoutError:
            self._pending.pop(req.id, None)
            return TcpResponse(id=req.id, status="error", error={"code": "TIMEOUT", "message": "Request timed out"})

    async def _listen(self) -> None:
        """Listen for responses from the server."""
        try:
            while self._connected and self._reader:
                line = await self._reader.readline()
                if not line:
                    break

                data = parse_message(line.decode())
                msg_type = data.get("type", "")

                if msg_type == "ping":
                    if self._writer:
                        self._writer.write(b'{"type":"pong"}\n')
                        await self._writer.drain()
                    continue

                if msg_type == "response":
                    resp = TcpResponse.from_json(data)
                    future = self._pending.pop(resp.id, None)
                    if future and not future.done():
                        future.set_result(resp)
        except Exception as e:
            logger.error("Listen error: %s", e)
        finally:
            self._connected = False

    @property
    def is_connected(self) -> bool:
        return self._connected
