# AutoFish+ (Minecraft 1.21.11 Fabric)

[![Release](https://img.shields.io/github/v/release/sensen0025/AutoFishPlus?color=brightgreen)](https://github.com/sensen0025/AutoFishPlus/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-blue.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-Loader_%3E%3D0.15.0-orange.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-CC0_1.0-lightgrey.svg)](LICENSE)

**AutoFish+** 是专为 **Minecraft 1.21.11 Fabric** 深度适配的高灵敏度自动钓鱼与自动化挂机模组。  
包含防封禁（Anti-Ban）拟人化延迟、背包自动换竿、Hypixel 主大厅自动寻路补竿与挂机闭环宏、以及防挂机踢出（Anti-AFK）等全套自动化系统。

---

## ✨ 核心特性

- 🎣 **高灵敏自动钓鱼**：基于客户端声音事件与浮漂运动双重侦测，支持极速咬钩识别与毫秒级拟人化收抛竿。
- 🤖 **Hypixel 主大厅全自动挂机宏 (`/macro`)**：
  - 一键启动全流程自动化；
  - **Baritone 智能寻路联动**：背包无竿时自动寻路前往码头 NPC (`22, 62, -36`)；
  - **动态 NPC 实体追踪**：自动检索最近 NPC 并动态计算注视角度，自动右键打开商店并购买新鱼竿；
  - **自动返回钓位**：自动前往钓鱼点 (`36, 59, -48`)，就位后自动对准正北方（Yaw = 180.0°, Pitch = -2.0°）抛竿；
  - **爆竿自动重走流程**：钓鱼期间一旦损坏/爆竿，自动停止钓鱼并返回 NPC 购买新鱼竿，实现永动巡航。
- 🛡️ **智能 Anti-AFK 防挂机移动**：
  - 每隔约 35 秒触发一次防挂机动作；
  - **不收杆无缝钓鱼**：移动期间浮漂稳留在水中，不影响咬钩判定与钓鱼效率；
  - **严格防掉水**：针对前方水池地形，采用「先向后退 1 格 ➔ 前进归位并在触及原点瞬间急刹」的刚性闭环坐标控制，绝不会落入水中。
- ⚡ **超强鲁棒性自愈机制**：
  - 钓鱼过程中每帧保持正北锁定；
  - 若处于钓位超过 2 秒未处于抛竿状态（`player.fishing == null`），自动校准正北并重抛，彻底解决漏抛或被打断的问题。
- ⌨️ **按键绑定与 GUI 配置**：
  - 快捷键 `J`：切换 AutoFish 开关（Actionbar 实时通知）；
  - 快捷键 `'`：呼出图形化配置菜单（支持 ModMenu 集成）；
  - 指令 `/macro`：一键开启/暂停全自动宏。

---

## 📦 依赖环境与前置模组

本模组运行需要以下前置支持：
1. **Minecraft**：`1.21.11`
2. **Fabric Loader**：`>= 0.15.0`
3. **Fabric API**：`fabric-api-0.141.6+1.21.11.jar`（可在 Release 页面直接下载）
4. **Baritone**：`baritone-fabric-1.21.11.jar`（可在 Release 页面直接下载）

---

## 🚀 安装指南

1. 安装 Fabric Loader 对应 Minecraft 1.21.11 的版本；
2. 前往 [GitHub Releases](https://github.com/sensen0025/AutoFishPlus/releases) 下载：
   - `autofishplus-1.0.0-1.21.11.jar`
   - `fabric-api-0.141.6+1.21.11.jar`
   - `baritone-fabric-1.21.11.jar`
3. 将上述三个 `.jar` 文件放入游戏的 `.minecraft/mods` 目录；
4. 启动游戏即可尽情体验！

---

## 📜 许可证

本项目基于 [CC0 1.0 Universal](LICENSE) 协议发布。
