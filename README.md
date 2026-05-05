# MCP Bridge Server

桥接 Claude Code 到 Minecraft Forge 的 MCP 服务器，通过 TCP 协议实现 AI 驱动的游戏自动化。

## 架构

```
Claude Code ──stdio──→ MCP Bridge Server (Python) ──TCP:25580──→ Minecraft Forge (Java)
```

## 安装

```bash
pip install -e .
```

## 使用

```bash
# 启动桥接服务器
mcp-bridge-server --host localhost --port 25580 --token change-me

# 或作为 Python 模块运行
python -m mcp_bridge_server --host localhost --port 25580 --token change-me
```

### Claude Code 配置

在项目根目录创建 `.mcp.json`：

```json
{
  "mcpServers": {
    "minecraft": {
      "command": "mcp-bridge-server",
      "args": ["--host", "localhost", "--port", "25580", "--token", "change-me"]
    }
  }
}
```

## 工具列表 (32 个)

### World
| 工具 | 说明 |
|------|------|
| `world.give_item` | 给予玩家物品 |
| `world.place_block` | 放置方块 |
| `world.break_block` | 破坏方块 |
| `world.run_command` | 执行服务端命令 |

### Player
| 工具 | 说明 |
|------|------|
| `player.set_state` | 设置血量/饥饿/位置/游戏模式 |
| `player.apply_effect` | 施加药水效果 |
| `player.clear_effects` | 清除所有效果 |
| `player.clear_inventory` | 清空背包 |

### Mod — 培养槽
| 工具 | 说明 |
|------|------|
| `mod.insert_seed` | 放入基因种子 |
| `mod.inject_nutrition` | 注入营养液 |
| `mod.inject_purity` | 注入纯净度 |
| `mod.inject_signal` | 注入数据信号 |
| `mod.trigger_tick` | 加速生长 |
| `mod.extract_crop` | 取出作物 |

### Mod — 基因系统
| 工具 | 说明 |
|------|------|
| `mod.create_seed` | 创建基因种子 |
| `mod.get_seed_genes` | 读取种子基因 |
| `mod.set_seed_genes` | 修改种子基因 |
| `mod.simulate_splice` | 模拟基因拼接 |
| `mod.splice_genes` | 基因拼接机操作 |
| `mod.splice_with_forced_mutation` | 强制变异拼接 |
| `mod.save_to_genelibrary` | 保存基因档案 |
| `mod.load_from_genelibrary` | 加载基因档案 |
| `mod.query_genelibrary` | 查询基因库 |

### Mod — 血清系统
| 工具 | 说明 |
|------|------|
| `mod.craft_berry` | 合成神经莓 |
| `mod.craft_serum` | 合成血清 (S01/S02/S03) |
| `mod.craft_serum_with_genes` | 带基因参数合成血清 |
| `mod.test_serum_effect` | 测试血清效果 |

### Mod — 饰品
| 工具 | 说明 |
|------|------|
| `mod.equip_curio` | 装备饰品 |
| `mod.unequip_curio` | 卸下饰品 |

### Mod — 系统
| 工具 | 说明 |
|------|------|
| `mod.set_mutation_params` | 查询变异参数 |
| `mod.reset_mutation_params` | 重置变异参数 |
| `mod.reset_world` | 重置世界状态 |

## 资源 (2 个)

| 资源 | 说明 |
|------|------|
| `world://info` | 世界信息 |
| `serum/preview` | 血清效果预估 |

## 开发

```bash
# 安装开发依赖
pip install -e ".[dev]"

# 运行测试
pytest

# 运行单元测试
pytest tests/test_protocol.py

# 运行集成测试
pytest tests/test_tools.py
```

## Minecraft 模组端配置

游戏内配置文件 `serverconfig/cybercultivator-mcp.toml`：

```toml
[mcp_bridge]
enabled = true
port = 25580
token = "change-me"
```

## 协议

TCP JSON 行协议，每条消息以 `\n` 分隔：

```json
// 请求
{"id": "uuid", "type": "request", "method": "world.give_item", "params": {"player": "Steve", "item": "minecraft:diamond"}}

// 响应
{"id": "uuid", "type": "response", "status": "ok", "result": {"message": "Gave 1x minecraft:diamond"}}

// 握手
{"type": "handshake", "token": "change-me", "version": "1.0.0"}

// 心跳
{"type": "ping"} → {"type": "pong"}
```
