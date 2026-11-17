# Infinity What

简体中文 | [English](#english)

---

## 简体中文

无限资源，告别刷材料。

本模组新增两种无限源与一个配置复制工具。往无限源里放入一桶流体或一个物品，打开无限源开关，它就会永远产出。输出速率、输出方向、允许进入的内容，全部可配置。

### 功能

- **无限流体发生器** —— 放入任意允许的流体一次，储罐即变为无限源。输出速率可自由设置，输出可按面单独开启或关闭，并支持红石控制与批量输出。
- **无限物品发生器** —— 放入一个物品，即可获得近乎无限的供给，同样支持按面输出、速率设置、红石控制与批量输出。
- **无限源配置复制器** —— 复制一台机器的完整配置（输出开关、输出方向、每个面的速率、红石设置、批量值、物品无限模式），粘贴到另一台同类型机器上；支持全部、仅输出、仅速率、仅红石、仅批量五种模式。
- **红石与比较器** —— 始终、有信号、无信号、脉冲四种红石模式；脉冲工作/停止各 1–12000 tick；比较器输出随速率变化。
- **速率单位切换** —— 输入框支持 `/t`、`/s`、`/min`，内部统一换算为每 tick。
- **物品与流体 ID 过滤** —— 通过 `config/infwhat.toml` 按类别独立设置黑名单或白名单，精确控制每台机器接受什么。管道、漏斗、存储总线以及 GUI 内手动放入的物品/流体都受此限制。
- **创造模式标签页** —— 所有物品集中展示在独立的 `Infinity What` 标签页中。

> 红石控制、批量输出、速率单位切换和复制器多模式已随 2.2.0 覆盖全部支持版本。

### 支持的版本

本仓库的每个分支都是一个**对应单个 Minecraft 版本的完整独立工程**。选择与你的游戏版本匹配的分支进行构建，或直接从 Releases 页面下载 jar。

| Minecraft | 加载器 | 加载器版本 |
|---|---|---|
| 1.6.4 | Forge | 9.11.1.1345 |
| 1.7.10 | Forge | 10.13.4.1614 |
| 1.8 | Forge | 11.14.4.1577 |
| 1.8.8 | Forge | 11.15.0.1655 |
| 1.8.9 | Forge | 11.15.1.2318 |
| 1.9 | Forge | 12.16.1.1887 |
| 1.9.4 | Forge | 12.17.0.2317 |
| 1.10.2 | Forge | 12.18.3.2511 |
| 1.11 | Forge | 13.19.1.2199 |
| 1.11.2 | Forge | 13.20.1.2386 |
| 1.12 | Forge | 14.21.1.2443 |
| 1.12.1 | Forge | 14.22.1.2485 |
| 1.12.2 | Forge | 14.23.5.2860 |
| 1.13.2 | Forge | 25.0.223 |
| 1.14 | Fabric | 0.2.7 / 0.3.0（旧版） |
| 1.14.1 | Fabric | 0.2.7 / 0.3.0（旧版） |
| 1.14.2 | Forge | 26.0.63 |
| 1.14.2 | Fabric | 0.2.7 / 0.3.0（旧版） |
| 1.14.3 | Forge | 27.0.60 |
| 1.14.3 | Fabric | 0.2.7 / 0.3.0（旧版） |
| 1.14.4 | Forge | 28.2.30 |
| 1.14.4 | Fabric | 0.28.5+1.14 |
| 1.15 | Forge | 29.0.4 |
| 1.15 | Fabric | 0.28.5+1.15 |
| 1.15.1 | Forge | 30.0.51 |
| 1.15.1 | Fabric | 0.28.5+1.15 |
| 1.15.2 | Forge | 31.2.62 |
| 1.15.2 | Fabric | 0.28.5+1.15 |
| 1.16 | Fabric | 0.42.0+1.16 |
| 1.16.1 | Forge | 32.0.108 |
| 1.16.1 | Fabric | 0.42.0+1.16 |
| 1.16.2 | Forge | 33.0.61 |
| 1.16.2 | Fabric | 0.42.0+1.16 |
| 1.16.3 | Forge | 34.1.42 |
| 1.16.3 | Fabric | 0.42.0+1.16 |
| 1.16.4 | Forge | 35.1.37 |
| 1.16.4 | Fabric | 0.42.0+1.16 |
| 1.16.5 | Forge | 36.2.42 |
| 1.16.5 | Fabric | 0.42.0+1.16 |
| 1.17 | Fabric | 0.36.0+1.17 |
| 1.17.1 | Forge | 37.1.1 |
| 1.17.1 | Fabric | 0.46.1+1.17 |
| 1.18 | Forge | 38.0.17 |
| 1.18 | Fabric | 0.46.0+1.18 |
| 1.18.1 | Forge | 39.1.2 |
| 1.18.1 | Fabric | 0.46.6+1.18 |
| 1.18.2 | Forge | 40.3.12 |
| 1.18.2 | Fabric | 0.77.0+1.18.2 |
| 1.19 | Forge | 41.1.0 |
| 1.19 | Fabric | 0.58.0+1.19 |
| 1.19.1 | Forge | 42.0.9 |
| 1.19.1 | Fabric | 0.58.5+1.19.1 |
| 1.19.2 | Forge | 43.5.2 |
| 1.19.2 | Fabric | 0.77.0+1.19.2 |
| 1.19.3 | Forge | 44.1.23 |
| 1.19.3 | Fabric | 0.76.1+1.19.3 |
| 1.19.4 | Forge | 45.4.5 |
| 1.19.4 | Fabric | 0.87.2+1.19.4 |
| 1.20 | Forge | 46.0.14 |
| 1.20 | Fabric | 0.83.0+1.20 |
| 1.20.1 | Forge | 47.4.10 |
| 1.20.1 | Fabric | 0.92.12+1.20.1 |
| 1.20.1 | NeoForge | 47.1.106 |
| 1.20.2 | Forge | 48.1.0 |
| 1.20.2 | Fabric | 0.91.6+1.20.2 |
| 1.20.2 | NeoForge | 20.2.93 |
| 1.20.3 | Forge | 49.0.2 |
| 1.20.3 | Fabric | 0.91.1+1.20.3 |
| 1.20.3 | NeoForge | 20.3.8-beta |
| 1.20.4 | Forge | 49.2.13 |
| 1.20.4 | Fabric | 0.97.3+1.20.4 |
| 1.20.4 | NeoForge | 20.4.253 |
| 1.20.5 | Fabric | 0.97.8+1.20.5 |
| 1.20.5 | NeoForge | 20.5.21-beta |
| 1.20.6 | Forge | 50.2.15 |
| 1.20.6 | Fabric | 0.100.8+1.20.6 |
| 1.20.6 | NeoForge | 20.6.144 |
| 1.21 | Forge | 51.0.33 |
| 1.21 | Fabric | 0.102.0+1.21 |
| 1.21 | NeoForge | 21.0.167 |
| 1.21.1 | Forge | 52.1.21 |
| 1.21.1 | Fabric | 0.116.17+1.21.1 |
| 1.21.1 | NeoForge | 21.1.262 |
| 1.21.2 | Fabric | 0.106.1+1.21.2 |
| 1.21.2 | NeoForge | 21.2.1-beta |
| 1.21.3 | Forge | 53.1.16 |
| 1.21.3 | Fabric | 0.114.1+1.21.3 |
| 1.21.3 | NeoForge | 21.3.100 |
| 1.21.4 | Forge | 54.1.22 |
| 1.21.4 | Fabric | 0.119.4+1.21.4 |
| 1.21.4 | NeoForge | 21.4.161 |
| 1.21.5 | Forge | 55.1.18 |
| 1.21.5 | Fabric | 0.128.2+1.21.5 |
| 1.21.5 | NeoForge | 21.5.101 |
| 1.21.6 | Forge | 56.0.9 |
| 1.21.6 | Fabric | 0.128.2+1.21.6 |
| 1.21.6 | NeoForge | 21.6.20-beta |
| 1.21.7 | Forge | 57.0.3 |
| 1.21.7 | Fabric | 0.129.0+1.21.7 |
| 1.21.7 | NeoForge | 21.7.25-beta |
| 1.21.8 | Forge | 58.1.26 |
| 1.21.8 | Fabric | 0.136.1+1.21.8 |
| 1.21.8 | NeoForge | 21.8.57 |
| 1.21.9 | Forge | 59.0.5 |
| 1.21.9 | Fabric | 0.134.1+1.21.9 |
| 1.21.9 | NeoForge | 21.9.16-beta |
| 1.21.10 | Forge | 60.1.18 |
| 1.21.10 | Fabric | 0.138.4+1.21.10 |
| 1.21.10 | NeoForge | 21.10.64 |
| 1.21.11 | Forge | 61.2.5 |
| 1.21.11 | Fabric | 0.141.6+1.21.11 |
| 1.21.11 | NeoForge | 21.11.48 |
| 26.1 | Forge | 62.0.9 |
| 26.1 | Fabric | 0.145.1+26.1 |
| 26.1 | NeoForge | 26.1.2.121 |
| 26.1.1 | Forge | 63.0.2 |
| 26.1.1 | Fabric | 0.145.1+26.1 |
| 26.1.1 | NeoForge | 26.1.1.15-beta |
| 26.1.2 | Forge | 64.1.6 |
| 26.1.2 | Fabric | 0.155.3+26.1.2 |
| 26.1.2 | NeoForge | 26.1.2.121 |
| 26.2 | Forge | 65.1.6 |
| 26.2 | Fabric | 0.161.0+26.2 |
| 26.2 | NeoForge | 26.2.0.93 |
| 26.3 | Forge | 66.1.8 |
| 26.3 | Fabric | 0.165.0+26.3 |
| 26.3 | NeoForge | 26.3.0.83 |

Fabric 行的版本号是 **Fabric API** 版本；Fabric Loader 统一要求 0.16 及以上（实测 0.20.1）。NeoForge 行是 NeoForge 版本。Fabric 1.14–1.14.3 需使用旧版 API（0.2.7 / 0.3.0），0.28.5+1.14 只适用于 1.14.4。

`26.3` 的 NeoForge 工程位于仓库默认分支 `main`，其余版本各自对应同名分支。

上表覆盖全部发行构建；jar 请从 Releases 页面下载。

### 2.2.0 发布状态

2.2.0 已发布：上表所有版本都包含红石、脉冲、比较器、批量输出与复制器多模式。

> 功能差异：Fabric 1.14–1.15.2 与 1.17.0 没有可用的通用流体/物品能力，主动输出仅支持原版炼药锅（1.17.0 的物品机仅支持原版容器）；1.16+ 的 Fabric 版本使用 Fabric Transfer API，可对接其他模组的储罐与管道。

### 安装

1. 安装与你的游戏版本对应的 Minecraft Forge、NeoForge 或 Fabric。
2. 将匹配的 jar 放入 `mods` 文件夹。
3. 启动游戏。首次启动时会生成 `config/infwhat.toml`；Forge 1.12.2 及更早版本为 `config/infwhat.cfg`。

Forge / NeoForge 版无需前置；Fabric 版需要 **Fabric API**（按下表版本安装）与 Fabric Loader 0.16+。

### 配置

所有行为由 `config/infwhat.toml` 控制（Forge 1.12.2 及更早版本使用 `config/infwhat.cfg`，键名完全相同）。

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
| Forge 1.6.4 – 1.16.5 | 8 |
| Forge 1.17.1 – 1.20.4、NeoForge 1.20.1 – 1.20.4 | 17 |
| Forge 1.20.6+、NeoForge 1.20.5+、Fabric 全系 | 21 |
| 26.x | 25 |

### 许可证

[Apache License 2.0](LICENSE)

---

## English

Infinite resources, zero farming.

Infinity What adds two generator machines plus a config-copying tool. Feed a generator a single bucket or a single item, flip its infinite-source switch, and it produces forever. Every knob — output rate, output sides, what may enter — is configurable.

### Features

- **Infinite Fluid Generator** — insert any allowed fluid once and the tank becomes an endless source. Output rate is freely configurable, output can be enabled per side, and redstone/batch control is supported.
- **Infinite Item Generator** — insert a single item and it becomes an effectively unlimited supply, with the same per-side, rate, redstone and batch controls.
- **Config Replicator** — copy a machine's full configuration (output switches, sides, per-side rates, redstone settings, batch size, infinite mode) and paste it onto another machine of the same type; modes include all, output-only, rate-only, redstone-only and batch-only.
- **Redstone & comparator** — always, high-signal, low-signal and pulse modes; pulse work/stop each 1–12000 ticks; comparator output follows the configured rate.
- **Rate units** — input fields accept `/t`, `/s` and `/min`, converted to per-tick internally.
- **Item & fluid ID filters** — blacklist or whitelist exactly what each machine accepts, per category, through `config/infwhat.toml`. Filters apply to pipes, hoppers, storage buses and manual GUI insertion alike.
- **Creative tab** — all items collected in a dedicated `Infinity What` tab.

> Redstone control, batch output, rate units and replicator modes are available on every supported version in 2.2.0.

### Supported versions

Every branch of this repository is a **complete, standalone project for one Minecraft version**. Pick the branch matching your game version, build it, or grab the jar from the Releases page.

| Minecraft | Loader | Loader version |
|---|---|---|
| 1.6.4 | Forge | 9.11.1.1345 |
| 1.7.10 | Forge | 10.13.4.1614 |
| 1.8 | Forge | 11.14.4.1577 |
| 1.8.8 | Forge | 11.15.0.1655 |
| 1.8.9 | Forge | 11.15.1.2318 |
| 1.9 | Forge | 12.16.1.1887 |
| 1.9.4 | Forge | 12.17.0.2317 |
| 1.10.2 | Forge | 12.18.3.2511 |
| 1.11 | Forge | 13.19.1.2199 |
| 1.11.2 | Forge | 13.20.1.2386 |
| 1.12 | Forge | 14.21.1.2443 |
| 1.12.1 | Forge | 14.22.1.2485 |
| 1.12.2 | Forge | 14.23.5.2860 |
| 1.13.2 | Forge | 25.0.223 |
| 1.14 | Fabric | 0.2.7 / 0.3.0（旧版） |
| 1.14.1 | Fabric | 0.2.7 / 0.3.0（旧版） |
| 1.14.2 | Forge | 26.0.63 |
| 1.14.2 | Fabric | 0.2.7 / 0.3.0（旧版） |
| 1.14.3 | Forge | 27.0.60 |
| 1.14.3 | Fabric | 0.2.7 / 0.3.0（旧版） |
| 1.14.4 | Forge | 28.2.30 |
| 1.14.4 | Fabric | 0.28.5+1.14 |
| 1.15 | Forge | 29.0.4 |
| 1.15 | Fabric | 0.28.5+1.15 |
| 1.15.1 | Forge | 30.0.51 |
| 1.15.1 | Fabric | 0.28.5+1.15 |
| 1.15.2 | Forge | 31.2.62 |
| 1.15.2 | Fabric | 0.28.5+1.15 |
| 1.16 | Fabric | 0.42.0+1.16 |
| 1.16.1 | Forge | 32.0.108 |
| 1.16.1 | Fabric | 0.42.0+1.16 |
| 1.16.2 | Forge | 33.0.61 |
| 1.16.2 | Fabric | 0.42.0+1.16 |
| 1.16.3 | Forge | 34.1.42 |
| 1.16.3 | Fabric | 0.42.0+1.16 |
| 1.16.4 | Forge | 35.1.37 |
| 1.16.4 | Fabric | 0.42.0+1.16 |
| 1.16.5 | Forge | 36.2.42 |
| 1.16.5 | Fabric | 0.42.0+1.16 |
| 1.17 | Fabric | 0.36.0+1.17 |
| 1.17.1 | Forge | 37.1.1 |
| 1.17.1 | Fabric | 0.46.1+1.17 |
| 1.18 | Forge | 38.0.17 |
| 1.18 | Fabric | 0.46.0+1.18 |
| 1.18.1 | Forge | 39.1.2 |
| 1.18.1 | Fabric | 0.46.6+1.18 |
| 1.18.2 | Forge | 40.3.12 |
| 1.18.2 | Fabric | 0.77.0+1.18.2 |
| 1.19 | Forge | 41.1.0 |
| 1.19 | Fabric | 0.58.0+1.19 |
| 1.19.1 | Forge | 42.0.9 |
| 1.19.1 | Fabric | 0.58.5+1.19.1 |
| 1.19.2 | Forge | 43.5.2 |
| 1.19.2 | Fabric | 0.77.0+1.19.2 |
| 1.19.3 | Forge | 44.1.23 |
| 1.19.3 | Fabric | 0.76.1+1.19.3 |
| 1.19.4 | Forge | 45.4.5 |
| 1.19.4 | Fabric | 0.87.2+1.19.4 |
| 1.20 | Forge | 46.0.14 |
| 1.20 | Fabric | 0.83.0+1.20 |
| 1.20.1 | Forge | 47.4.10 |
| 1.20.1 | Fabric | 0.92.12+1.20.1 |
| 1.20.1 | NeoForge | 47.1.106 |
| 1.20.2 | Forge | 48.1.0 |
| 1.20.2 | Fabric | 0.91.6+1.20.2 |
| 1.20.2 | NeoForge | 20.2.93 |
| 1.20.3 | Forge | 49.0.2 |
| 1.20.3 | Fabric | 0.91.1+1.20.3 |
| 1.20.3 | NeoForge | 20.3.8-beta |
| 1.20.4 | Forge | 49.2.13 |
| 1.20.4 | Fabric | 0.97.3+1.20.4 |
| 1.20.4 | NeoForge | 20.4.253 |
| 1.20.5 | Fabric | 0.97.8+1.20.5 |
| 1.20.5 | NeoForge | 20.5.21-beta |
| 1.20.6 | Forge | 50.2.15 |
| 1.20.6 | Fabric | 0.100.8+1.20.6 |
| 1.20.6 | NeoForge | 20.6.144 |
| 1.21 | Forge | 51.0.33 |
| 1.21 | Fabric | 0.102.0+1.21 |
| 1.21 | NeoForge | 21.0.167 |
| 1.21.1 | Forge | 52.1.21 |
| 1.21.1 | Fabric | 0.116.17+1.21.1 |
| 1.21.1 | NeoForge | 21.1.262 |
| 1.21.2 | Fabric | 0.106.1+1.21.2 |
| 1.21.2 | NeoForge | 21.2.1-beta |
| 1.21.3 | Forge | 53.1.16 |
| 1.21.3 | Fabric | 0.114.1+1.21.3 |
| 1.21.3 | NeoForge | 21.3.100 |
| 1.21.4 | Forge | 54.1.22 |
| 1.21.4 | Fabric | 0.119.4+1.21.4 |
| 1.21.4 | NeoForge | 21.4.161 |
| 1.21.5 | Forge | 55.1.18 |
| 1.21.5 | Fabric | 0.128.2+1.21.5 |
| 1.21.5 | NeoForge | 21.5.101 |
| 1.21.6 | Forge | 56.0.9 |
| 1.21.6 | Fabric | 0.128.2+1.21.6 |
| 1.21.6 | NeoForge | 21.6.20-beta |
| 1.21.7 | Forge | 57.0.3 |
| 1.21.7 | Fabric | 0.129.0+1.21.7 |
| 1.21.7 | NeoForge | 21.7.25-beta |
| 1.21.8 | Forge | 58.1.26 |
| 1.21.8 | Fabric | 0.136.1+1.21.8 |
| 1.21.8 | NeoForge | 21.8.57 |
| 1.21.9 | Forge | 59.0.5 |
| 1.21.9 | Fabric | 0.134.1+1.21.9 |
| 1.21.9 | NeoForge | 21.9.16-beta |
| 1.21.10 | Forge | 60.1.18 |
| 1.21.10 | Fabric | 0.138.4+1.21.10 |
| 1.21.10 | NeoForge | 21.10.64 |
| 1.21.11 | Forge | 61.2.5 |
| 1.21.11 | Fabric | 0.141.6+1.21.11 |
| 1.21.11 | NeoForge | 21.11.48 |
| 26.1 | Forge | 62.0.9 |
| 26.1 | Fabric | 0.145.1+26.1 |
| 26.1 | NeoForge | 26.1.2.121 |
| 26.1.1 | Forge | 63.0.2 |
| 26.1.1 | Fabric | 0.145.1+26.1 |
| 26.1.1 | NeoForge | 26.1.1.15-beta |
| 26.1.2 | Forge | 64.1.6 |
| 26.1.2 | Fabric | 0.155.3+26.1.2 |
| 26.1.2 | NeoForge | 26.1.2.121 |
| 26.2 | Forge | 65.1.6 |
| 26.2 | Fabric | 0.161.0+26.2 |
| 26.2 | NeoForge | 26.2.0.93 |
| 26.3 | Forge | 66.1.8 |
| 26.3 | Fabric | 0.165.0+26.3 |
| 26.3 | NeoForge | 26.3.0.83 |

The version shown for Fabric rows is the **Fabric API** version; Fabric Loader 0.16+ is required (tested with 0.20.1). NeoForge rows show the NeoForge version. Fabric 1.14–1.14.3 require the legacy API line (0.2.7 / 0.3.0); 0.28.5+1.14 only targets 1.14.4.

The 26.3 NeoForge project lives on the repository's default branch `main`; every other version has its own same-named branch.

The table above covers all release builds; download jars from the Releases page.

### 2.2.0 status

2.2.0 is released: every version above includes redstone, pulse, comparator, batch output and replicator modes.

> Capability note: Fabric 1.14–1.15.2 and 1.17.0 have no general-purpose fluid/item capability, so active output only supports vanilla cauldrons (the 1.17.0 item generator only supports vanilla containers). Fabric 1.16+ uses the Fabric Transfer API and interoperates with other mods' tanks and pipes.

### Installation

1. Install Minecraft Forge, NeoForge or Fabric for your game version.
2. Drop the matching jar into your `mods` folder.
3. Launch the game. `config/infwhat.toml` is generated on first start; Forge 1.12.2 and older use `config/infwhat.cfg`.

The Forge / NeoForge builds have no dependencies; the Fabric builds require **Fabric API** (version as listed above) and Fabric Loader 0.16+.

### Configuration

All behaviour is controlled by `config/infwhat.toml` (Forge 1.12.2 and older use `config/infwhat.cfg` with the same keys).

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
| Forge 1.6.4 – 1.16.5 | 8 |
| Forge 1.17.1 – 1.20.4, NeoForge 1.20.1 – 1.20.4 | 17 |
| Forge 1.20.6+, NeoForge 1.20.5+, all Fabric | 21 |
| 26.x | 25 |

### License

[Apache License 2.0](LICENSE)
