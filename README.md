# Infinity What

简体中文 | [English](#english)

---

## 简体中文

无限资源，告别刷材料。

本模组新增两种发电机与一个配置复制工具。往发电机里放入一桶流体或一个物品，打开无限源开关，它就会永远产出。输出速率、输出方向、允许进入的内容，全部可配置。

### 功能

- **无限流体发生器** —— 放入任意允许的流体一次，储罐即变为无限源。输出速率可自由设置，输出可按面单独开启或关闭。
- **无限物品发生器** —— 放入一个物品，即可获得近乎无限的供给，同样支持按面输出与速率设置。
- **无限源配置复制器** —— 复制一台机器的完整配置（输出开关、输出方向、输出速率、物品无限模式），粘贴到另一台同类型机器上。
- **物品与流体 ID 过滤** —— 通过 `config/infwhat.toml` 按类别独立设置黑名单或白名单，精确控制每台机器接受什么。管道、漏斗、存储总线以及 GUI 内手动放入的物品/流体都受此限制。
- **创造模式标签页** —— 所有物品集中展示在独立的 `Infinity What` 标签页中。

### 支持的版本

本仓库的每个分支都是一个**对应单个 Minecraft 版本的完整独立工程**。选择与你的游戏版本匹配的分支进行构建，或直接从 Releases 页面下载 jar。

| Minecraft | 加载器 | 加载器版本 |
|---|---|---|
| 1.20.1 | Forge | 47.4.10+ |
| 1.20.1 | NeoForge | 47.1.106 |
| 1.20.2 | NeoForge | 20.2.93 |
| 1.20.4 | NeoForge | 20.4.253 |
| 1.20.6 | NeoForge | 20.6.144 |
| 1.21 | NeoForge | 21.0.167 |
| 1.21.1 | NeoForge | 21.1.262 |
| 1.21.4 | NeoForge | 21.4.161 |
| 1.21.5 | NeoForge | 21.5.101 |
| 1.21.8 | NeoForge | 21.8.57 |
| 1.21.10 | NeoForge | 21.10.63 |
| 1.21.11 | NeoForge | 21.11.48 |
| 26.1 | NeoForge | 26.1.2.121 |
| 26.1.2 | NeoForge | 26.1.2.121 |
| 26.2 | NeoForge | 26.2.0.93 |
| 26.3 | NeoForge | 26.3.0.83 |

### 安装

1. 安装与你的游戏版本对应的 Minecraft Forge 或 NeoForge。
2. 将匹配的 jar 放入 `mods` 文件夹。
3. 启动游戏。首次启动时会生成 `config/infwhat.toml`。

无需其他模组作为前置。

### 配置

所有行为由 `config/infwhat.toml` 控制。

```toml
[balance]
	# Master switch for the infinite behaviour of both generators.
	enableInfiniteSource = true
	# Capacity in mB used when enableInfiniteSource is false.
	finiteTankCapacity = 16000

[item_filter]
	# BLACKLIST: listed item ids can never be inserted.
	# WHITELIST: only listed item ids can be inserted.
	mode = "BLACKLIST"
	# Item ids (e.g. "minecraft:diamond") the mode above applies to.
	list = []

[fluid_filter]
	# BLACKLIST: listed fluid ids can never be inserted.
	# WHITELIST: only listed fluid ids can be inserted.
	mode = "BLACKLIST"
	# Fluid ids (e.g. "minecraft:water") the mode above applies to.
	list = []
```

名单默认为空，因此在黑名单模式下不会过滤任何内容。将 `mode` 设为 `"WHITELIST"` 并填入名单，即可仅允许名单中的条目。

### 从源码构建

切换到需要的版本分支后执行：

```bash
./gradlew build
```

产物位于 `build/libs/`。各分支所需的 JDK：

| Minecraft | JDK |
|---|---|
| 1.20.1 – 1.20.4 | 17 |
| 1.20.6 – 1.21.11 | 21 |
| 26.x | 25 |

### 许可证

[Apache License 2.0](LICENSE)

---

## English

Infinite resources, zero farming.

Infinity What adds two generator machines plus a config-copying tool. Feed a generator a single bucket or a single item, flip its infinite-source switch, and it produces forever. Every knob — output rate, output sides, what may enter — is configurable.

### Features

- **Infinity Fluid Generator** — insert any allowed fluid once and the tank becomes an endless source. Output rate is freely configurable and output can be enabled per side.
- **Infinity Item Generator** — insert a single item and it becomes an effectively unlimited supply, with the same per-side and rate controls.
- **Config Replicator** — copy a machine's full configuration (output switches, sides, rate, infinite mode) and paste it onto another machine of the same type.
- **Item & fluid ID filters** — blacklist or whitelist exactly what each machine accepts, per category, through `config/infwhat.toml`. Filters apply to pipes, hoppers, storage buses and manual GUI insertion alike.
- **Creative tab** — all items collected in a dedicated `Infinity What` tab.

### Supported versions

Every branch of this repository is a **complete, standalone project for one Minecraft version**. Pick the branch matching your game version, build it, or grab the jar from the Releases page.

| Minecraft | Loader | Loader version |
|---|---|---|
| 1.20.1 | Forge | 47.4.10+ |
| 1.20.1 | NeoForge | 47.1.106 |
| 1.20.2 | NeoForge | 20.2.93 |
| 1.20.4 | NeoForge | 20.4.253 |
| 1.20.6 | NeoForge | 20.6.144 |
| 1.21 | NeoForge | 21.0.167 |
| 1.21.1 | NeoForge | 21.1.262 |
| 1.21.4 | NeoForge | 21.4.161 |
| 1.21.5 | NeoForge | 21.5.101 |
| 1.21.8 | NeoForge | 21.8.57 |
| 1.21.10 | NeoForge | 21.10.63 |
| 1.21.11 | NeoForge | 21.11.48 |
| 26.1 | NeoForge | 26.1.2.121 |
| 26.1.2 | NeoForge | 26.1.2.121 |
| 26.2 | NeoForge | 26.2.0.93 |
| 26.3 | NeoForge | 26.3.0.83 |

### Installation

1. Install Minecraft Forge or NeoForge for your game version.
2. Drop the matching jar into your `mods` folder.
3. Launch the game. `config/infwhat.toml` is generated on first start.

No other mods are required.

### Configuration

All behaviour is controlled by `config/infwhat.toml`.

```toml
[balance]
	# Master switch for the infinite behaviour of both generators.
	enableInfiniteSource = true
	# Capacity in mB used when enableInfiniteSource is false.
	finiteTankCapacity = 16000

[item_filter]
	# BLACKLIST: listed item ids can never be inserted.
	# WHITELIST: only listed item ids can be inserted.
	mode = "BLACKLIST"
	# Item ids (e.g. "minecraft:diamond") the mode above applies to.
	list = []

[fluid_filter]
	# BLACKLIST: listed fluid ids can never be inserted.
	# WHITELIST: only listed fluid ids can be inserted.
	mode = "BLACKLIST"
	# Fluid ids (e.g. "minecraft:water") the mode above applies to.
	list = []
```

The lists are empty by default, so in blacklist mode nothing is filtered out. Set `mode = "WHITELIST"` and fill a list to allow only those entries.

### Building from source

Pick the branch for the version you need and run:

```bash
./gradlew build
```

The jar lands in `build/libs/`. Java version required per branch:

| Minecraft | JDK |
|---|---|
| 1.20.1 – 1.20.4 | 17 |
| 1.20.6 – 1.21.11 | 21 |
| 26.x | 25 |

### License

[Apache License 2.0](LICENSE)
