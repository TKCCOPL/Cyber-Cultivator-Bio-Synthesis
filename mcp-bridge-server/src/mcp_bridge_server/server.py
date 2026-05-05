"""MCP Bridge Server for Minecraft Forge Cyber-Cultivator mod.

This module creates an MCP server that exposes 32 tools and 2 resources,
bridging Claude Code to a running Minecraft instance via TCP.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import logging
import signal
import sys
from typing import Any

import mcp.types as types
from mcp.server.lowlevel import Server
from mcp.server.stdio import stdio_server

from .tcp_client import TcpClient

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Tool definitions — (name, description, inputSchema)
# The tool name matches the Java-side registered method name exactly.
# ---------------------------------------------------------------------------

TOOLS: list[tuple[str, str, dict[str, Any]]] = [
    # ── World ──────────────────────────────────────────────────────────────
    (
        "world.give_item",
        "Give an item to the player. Supports optional NBT tag.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
                "item": {"type": "string", "description": "Item ID, e.g. minecraft:diamond"},
                "count": {"type": "integer", "default": 1, "description": "Amount to give"},
                "nbt": {
                    "type": "object",
                    "description": "Optional NBT tag as JSON object",
                },
            },
            "required": ["player", "item"],
        },
    ),
    (
        "world.place_block",
        "Place a block at the specified position.",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "X coordinate"},
                "y": {"type": "integer", "description": "Y coordinate"},
                "z": {"type": "integer", "description": "Z coordinate"},
                "block_state": {
                    "type": "string",
                    "description": "Block ID, e.g. cybercultivator:bio_incubator",
                },
            },
            "required": ["x", "y", "z", "block_state"],
        },
    ),
    (
        "world.break_block",
        "Break (destroy) a block at the specified position, dropping items.",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "X coordinate"},
                "y": {"type": "integer", "description": "Y coordinate"},
                "z": {"type": "integer", "description": "Z coordinate"},
            },
            "required": ["x", "y", "z"],
        },
    ),
    (
        "world.run_command",
        "Run an arbitrary server command (without the leading /).",
        {
            "type": "object",
            "properties": {
                "command": {"type": "string", "description": "Command string, e.g. 'give player diamond 1'"},
            },
            "required": ["command"],
        },
    ),
    # ── Player ─────────────────────────────────────────────────────────────
    (
        "player.set_state",
        "Modify the player's health, hunger, position, or gamemode.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
                "health": {"type": "number", "description": "Set health (0-20)"},
                "hunger": {"type": "integer", "description": "Set food level (0-20)"},
                "pos": {
                    "type": "object",
                    "properties": {
                        "x": {"type": "number"},
                        "y": {"type": "number"},
                        "z": {"type": "number"},
                    },
                    "description": "Teleport to position",
                },
                "gamemode": {
                    "type": "string",
                    "enum": ["survival", "creative", "adventure", "spectator"],
                    "description": "Set gamemode",
                },
            },
            "required": ["player"],
        },
    ),
    (
        "player.apply_effect",
        "Apply a mob effect to the player.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
                "effect": {
                    "type": "string",
                    "description": "Effect ID, e.g. minecraft:speed or cybercultivator:synaptic_overclock",
                },
                "duration": {
                    "type": "integer",
                    "default": 600,
                    "description": "Duration in ticks (20 ticks = 1 second)",
                },
                "amplifier": {
                    "type": "integer",
                    "default": 0,
                    "description": "Amplifier level (0 = I, 1 = II, etc.)",
                },
            },
            "required": ["player", "effect"],
        },
    ),
    (
        "player.clear_effects",
        "Remove all mob effects from the player.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
            },
            "required": ["player"],
        },
    ),
    (
        "player.clear_inventory",
        "Clear the player's entire inventory.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
            },
            "required": ["player"],
        },
    ),
    # ── Incubator ──────────────────────────────────────────────────────────
    (
        "mod.inject_nutrition",
        "Inject nutrition into a Bio-Incubator at the given position.",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
                "amount": {
                    "type": "integer",
                    "default": 20,
                    "description": "Amount of nutrition to inject (0-100)",
                },
            },
            "required": ["x", "y", "z"],
        },
    ),
    (
        "mod.inject_purity",
        "Inject purity into a Bio-Incubator at the given position.",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
                "amount": {
                    "type": "integer",
                    "default": 20,
                    "description": "Amount of purity to inject (0-100)",
                },
            },
            "required": ["x", "y", "z"],
        },
    ),
    (
        "mod.inject_signal",
        "Inject data signal into a Bio-Incubator at the given position.",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
                "amount": {
                    "type": "integer",
                    "default": 20,
                    "description": "Amount of signal to inject (0-100)",
                },
            },
            "required": ["x", "y", "z"],
        },
    ),
    (
        "mod.insert_seed",
        "Insert a genetic seed into a Bio-Incubator with optional gene values.",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
                "seed_type": {
                    "type": "string",
                    "default": "fiber_reed_seeds",
                    "description": "Seed type ID, e.g. fiber_reed_seeds, protein_soy_seeds, alcohol_bloom_seeds",
                },
                "gene_speed": {
                    "type": "integer",
                    "description": "Speed gene value (1-10). Omit to use default.",
                },
                "gene_yield": {
                    "type": "integer",
                    "description": "Yield gene value (1-10). Omit to use default.",
                },
                "gene_potency": {
                    "type": "integer",
                    "description": "Potency gene value (1-10). Omit to use default.",
                },
            },
            "required": ["x", "y", "z"],
        },
    ),
    (
        "mod.extract_crop",
        "Extract the seed/crop from a Bio-Incubator (returns to inventory).",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
            },
            "required": ["x", "y", "z"],
        },
    ),
    (
        "mod.trigger_tick",
        "Manually advance a Bio-Incubator by N ticks (simulates time passing).",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
                "count": {
                    "type": "integer",
                    "default": 1,
                    "description": "Number of ticks to execute",
                },
            },
            "required": ["x", "y", "z"],
        },
    ),
    # ── Bottler ────────────────────────────────────────────────────────────
    (
        "mod.craft_serum",
        "Auto-craft a serum in a Serum Bottler (s01/s02/s03). Loads ingredients and ticks to completion.",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
                "serum_type": {
                    "type": "string",
                    "default": "s01",
                    "enum": ["s01", "s02", "s03"],
                    "description": "Serum type to craft",
                },
            },
            "required": ["x", "y", "z"],
        },
    ),
    (
        "mod.craft_berry",
        "Auto-craft a Synaptic Neural Berry in a Serum Bottler (berry recipe).",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
            },
            "required": ["x", "y", "z"],
        },
    ),
    (
        "mod.craft_serum_with_genes",
        "Craft a serum with specific gene-derived activity values.",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
                "serum_type": {
                    "type": "string",
                    "default": "s01",
                    "enum": ["s01", "s02", "s03"],
                    "description": "Serum type",
                },
                "activity": {
                    "type": "integer",
                    "default": 5,
                    "description": "Synaptic activity value for the berry (1-10)",
                },
                "potency": {
                    "type": "integer",
                    "default": 5,
                    "description": "Potency value for berry ingredient (1-10)",
                },
                "purity": {
                    "type": "integer",
                    "default": 5,
                    "description": "Purity value for ingredient (1-10)",
                },
                "concentration": {
                    "type": "integer",
                    "default": 5,
                    "description": "Concentration value for ingredient (1-10)",
                },
            },
            "required": ["x", "y", "z"],
        },
    ),
    (
        "mod.test_serum_effect",
        "Apply a serum effect directly to the player for testing.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
                "effect": {
                    "type": "string",
                    "default": "cybercultivator:synaptic_overclock",
                    "description": "Effect ID to apply",
                },
                "duration": {
                    "type": "integer",
                    "default": 500,
                    "description": "Duration in ticks",
                },
                "amplifier": {
                    "type": "integer",
                    "default": 0,
                    "description": "Amplifier level",
                },
            },
            "required": ["player"],
        },
    ),
    # ── Splicer ────────────────────────────────────────────────────────────
    (
        "mod.splice_genes",
        "Splice two seeds in a Gene Splicer. Inserts both seeds, triggers craft, returns output genes.",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
                "seed_type": {
                    "type": "string",
                    "default": "fiber_reed_seeds",
                    "description": "Seed type ID for both parents",
                },
                "speed_a": {"type": "integer", "default": 5, "description": "Parent A speed gene (1-10)"},
                "yield_a": {"type": "integer", "default": 5, "description": "Parent A yield gene (1-10)"},
                "potency_a": {"type": "integer", "default": 5, "description": "Parent A potency gene (1-10)"},
                "speed_b": {"type": "integer", "default": 5, "description": "Parent B speed gene (1-10)"},
                "yield_b": {"type": "integer", "default": 5, "description": "Parent B yield gene (1-10)"},
                "potency_b": {"type": "integer", "default": 5, "description": "Parent B potency gene (1-10)"},
            },
            "required": ["x", "y", "z"],
        },
    ),
    (
        "mod.simulate_splice",
        "Simulate a gene splice without a splicer block. Returns predicted output.",
        {
            "type": "object",
            "properties": {
                "speed_a": {"type": "integer", "default": 5, "description": "Parent A speed gene (1-10)"},
                "yield_a": {"type": "integer", "default": 5, "description": "Parent A yield gene (1-10)"},
                "potency_a": {"type": "integer", "default": 5, "description": "Parent A potency gene (1-10)"},
                "speed_b": {"type": "integer", "default": 5, "description": "Parent B speed gene (1-10)"},
                "yield_b": {"type": "integer", "default": 5, "description": "Parent B yield gene (1-10)"},
                "potency_b": {"type": "integer", "default": 5, "description": "Parent B potency gene (1-10)"},
            },
            "required": [],
        },
    ),
    (
        "mod.splice_with_forced_mutation",
        "Splice two seeds and force a mutation on a specific gene.",
        {
            "type": "object",
            "properties": {
                "x": {"type": "integer", "description": "Block X coordinate"},
                "y": {"type": "integer", "description": "Block Y coordinate"},
                "z": {"type": "integer", "description": "Block Z coordinate"},
                "seed_type": {
                    "type": "string",
                    "default": "fiber_reed_seeds",
                    "description": "Seed type ID",
                },
                "speed_a": {"type": "integer", "default": 5, "description": "Parent A speed gene"},
                "yield_a": {"type": "integer", "default": 5, "description": "Parent A yield gene"},
                "potency_a": {"type": "integer", "default": 5, "description": "Parent A potency gene"},
                "speed_b": {"type": "integer", "default": 5, "description": "Parent B speed gene"},
                "yield_b": {"type": "integer", "default": 5, "description": "Parent B yield gene"},
                "potency_b": {"type": "integer", "default": 5, "description": "Parent B potency gene"},
                "target": {
                    "type": "string",
                    "default": "speed",
                    "enum": ["speed", "yield", "potency"],
                    "description": "Gene to mutate",
                },
                "bonus": {
                    "type": "integer",
                    "default": 4,
                    "description": "Bonus applied to target gene (can be negative)",
                },
            },
            "required": ["x", "y", "z"],
        },
    ),
    # ── Gene ───────────────────────────────────────────────────────────────
    (
        "mod.get_seed_genes",
        "Read the gene values from a seed in the player's inventory.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
                "slot": {
                    "type": "integer",
                    "description": "Inventory slot index. Omit for main hand.",
                },
            },
            "required": ["player"],
        },
    ),
    (
        "mod.set_seed_genes",
        "Overwrite gene values on a seed in the player's inventory.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
                "slot": {
                    "type": "integer",
                    "description": "Inventory slot index. Omit for main hand.",
                },
                "gene_speed": {"type": "integer", "description": "New speed gene (1-10)"},
                "gene_yield": {"type": "integer", "description": "New yield gene (1-10)"},
                "gene_potency": {"type": "integer", "description": "New potency gene (1-10)"},
            },
            "required": ["player"],
        },
    ),
    (
        "mod.create_seed",
        "Create a new genetic seed and add it to the player's inventory.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
                "seed_type": {
                    "type": "string",
                    "default": "fiber_reed_seeds",
                    "description": "Seed type: fiber_reed_seeds, protein_soy_seeds, or alcohol_bloom_seeds",
                },
                "gene_speed": {"type": "integer", "default": 5, "description": "Speed gene (1-10)"},
                "gene_yield": {"type": "integer", "default": 5, "description": "Yield gene (1-10)"},
                "gene_potency": {"type": "integer", "default": 5, "description": "Potency gene (1-10)"},
                "count": {"type": "integer", "default": 1, "description": "Number of seeds to create"},
            },
            "required": ["player"],
        },
    ),
    (
        "mod.set_mutation_params",
        "Query the current mutation parameters (informational, parameters are internal).",
        {
            "type": "object",
            "properties": {},
            "required": [],
        },
    ),
    (
        "mod.reset_mutation_params",
        "Reset mutation parameters to defaults (informational, parameters are internal).",
        {
            "type": "object",
            "properties": {},
            "required": [],
        },
    ),
    (
        "mod.query_genelibrary",
        "Query the Gene Library for stored gene profiles.",
        {
            "type": "object",
            "properties": {},
            "required": [],
        },
    ),
    # ── Gene Library ───────────────────────────────────────────────────────
    (
        "mod.save_to_genelibrary",
        "Save a seed's gene profile to the Gene Library.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
                "slot": {
                    "type": "integer",
                    "description": "Inventory slot index. Omit for main hand.",
                },
            },
            "required": ["player"],
        },
    ),
    (
        "mod.load_from_genelibrary",
        "Load a gene profile from the Gene Library.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
            },
            "required": ["player"],
        },
    ),
    # ── Curios ─────────────────────────────────────────────────────────────
    (
        "mod.equip_curio",
        "Equip an item into a Curios accessory slot.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
                "item": {"type": "string", "description": "Item ID to equip, e.g. cybercultivator:bio_pulse_belt"},
            },
            "required": ["player", "item"],
        },
    ),
    (
        "mod.unequip_curio",
        "Unequip an item from a Curios accessory slot.",
        {
            "type": "object",
            "properties": {
                "player": {"type": "string", "description": "Player name"},
                "item": {"type": "string", "description": "Item ID to unequip"},
            },
            "required": ["player", "item"],
        },
    ),
    # ── Environment ────────────────────────────────────────────────────────
    (
        "mod.reset_world",
        "Reset world state (currently a stub).",
        {
            "type": "object",
            "properties": {},
            "required": [],
        },
    ),
]

# ---------------------------------------------------------------------------
# Resource definitions
# ---------------------------------------------------------------------------

RESOURCES: list[types.Resource] = [
    types.Resource(
        uri="world://server/info",
        name="Server Info",
        description="Current Minecraft server info (TPS, version, etc.)",
        mimeType="application/json",
    ),
    types.Resource(
        uri="world://server/players",
        name="Player List",
        description="List of online players with health, position, and gamemode",
        mimeType="application/json",
    ),
]


# ---------------------------------------------------------------------------
# Build the MCP Server
# ---------------------------------------------------------------------------


def create_server() -> tuple[Server, TcpClient]:
    """Create and wire up the MCP server with a TCP client handle."""
    server = Server("minecraft-bridge")
    client = TcpClient()  # placeholder; configured before connect

    # -- tools/list ----------------------------------------------------------
    @server.list_tools()
    async def handle_list_tools() -> list[types.Tool]:
        return [
            types.Tool(
                name=name,
                description=description,
                inputSchema=input_schema,
            )
            for name, description, input_schema in TOOLS
        ]

    # -- tools/call ----------------------------------------------------------
    @server.call_tool()
    async def handle_call_tool(
        name: str, arguments: dict[str, Any] | None
    ) -> list[types.TextContent]:
        args = arguments or {}
        try:
            resp = await client.send(method=name, params=args)
        except ConnectionError as exc:
            return [
                types.TextContent(
                    type="text",
                    text=json.dumps(
                        {"error": {"code": "NOT_CONNECTED", "message": str(exc)}}
                    ),
                )
            ]
        except Exception as exc:
            return [
                types.TextContent(
                    type="text",
                    text=json.dumps(
                        {"error": {"code": "INTERNAL_ERROR", "message": str(exc)}}
                    ),
                )
            ]

        # Build a JSON payload mirroring the TcpResponse shape so the caller
        # can inspect status, result, and error uniformly.
        payload: dict[str, Any] = {"status": resp.status}
        if resp.result is not None:
            payload["result"] = resp.result
        if resp.error is not None:
            payload["error"] = resp.error
        return [types.TextContent(type="text", text=json.dumps(payload))]

    # -- resources/list ------------------------------------------------------
    @server.list_resources()
    async def handle_list_resources() -> list[types.Resource]:
        return RESOURCES

    # -- resources/read ------------------------------------------------------
    @server.read_resource()
    async def handle_read_resource(uri: Any) -> str:
        uri_str = str(uri)
        if uri_str == "world://server/info":
            try:
                resp = await client.send(method="server.get_tps", params={}, timeout=5.0)
                if resp.is_ok and resp.result:
                    return json.dumps(resp.result)
                return json.dumps({"error": "Could not retrieve server info"})
            except Exception as exc:
                return json.dumps({"error": str(exc)})
        elif uri_str == "world://server/players":
            try:
                resp = await client.send(method="server.get_players", params={}, timeout=5.0)
                if resp.is_ok and resp.result:
                    return json.dumps(resp.result)
                return json.dumps({"error": "Could not retrieve player list"})
            except Exception as exc:
                return json.dumps({"error": str(exc)})
        else:
            return json.dumps({"error": f"Unknown resource: {uri_str}"})

    return server, client


# ---------------------------------------------------------------------------
# CLI entry point
# ---------------------------------------------------------------------------


async def run(host: str, port: int, token: str) -> None:
    """Connect TCP client and run the MCP server over stdio."""
    server, client = create_server()
    client.host = host
    client.port = port
    client.token = token

    logger.info("Connecting to Minecraft at %s:%d ...", host, port)
    await client.connect()
    logger.info("Connected. Starting MCP server over stdio ...")

    try:
        async with stdio_server() as (read_stream, write_stream):
            await server.run(
                read_stream,
                write_stream,
                server.create_initialization_options(),
                raise_exceptions=False,
            )
    finally:
        await client.disconnect()
        logger.info("Disconnected.")


def main() -> None:
    """CLI entry point for `mcp-bridge-server`."""
    parser = argparse.ArgumentParser(
        description="MCP Bridge Server for Minecraft Forge Cyber-Cultivator mod",
    )
    parser.add_argument(
        "--host",
        default="localhost",
        help="Minecraft TCP bridge host (default: localhost)",
    )
    parser.add_argument(
        "--port",
        type=int,
        default=25580,
        help="Minecraft TCP bridge port (default: 25580)",
    )
    parser.add_argument(
        "--token",
        default="change-me",
        help="Authentication token for the TCP handshake (default: change-me)",
    )
    parser.add_argument(
        "--log-level",
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR"],
        help="Logging level (default: INFO)",
    )
    args = parser.parse_args()

    logging.basicConfig(
        level=getattr(logging, args.log_level),
        format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
        stream=sys.stderr,
    )

    loop = asyncio.new_event_loop()

    def _signal_handler() -> None:
        logger.info("Received interrupt, shutting down ...")
        for task in asyncio.all_tasks(loop):
            task.cancel()

    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(sig, _signal_handler)
        except NotImplementedError:
            # add_signal_handler is not supported on Windows
            pass

    try:
        loop.run_until_complete(run(args.host, args.port, args.token))
    except asyncio.CancelledError:
        pass
    except KeyboardInterrupt:
        logger.info("Interrupted by user.")
    finally:
        loop.close()


if __name__ == "__main__":
    main()
