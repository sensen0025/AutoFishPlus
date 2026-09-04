## AutoFish+ v1.0.0 (Minecraft 1.21.11 Fabric)

专为 Minecraft 1.21.11 Fabric 打造的高灵敏度客户端自动钓鱼与自动化闭环挂机模组。

### 🌟 Release 包含附件（全套前置开箱即用）：
1. **`autofishplus-1.0.0-1.21.11.jar`**：AutoFish+ 1.21.11 核心模组；
2. **`fabric-api-0.141.6+1.21.11.jar`**：适配 1.21.11 的 Fabric API 官方前置；
3. **`baritone-fabric-1.21.11.jar`**：适配 1.21.11 的 Baritone 智能寻路前置。

### ✨ 主要更新与功能：
- **完整适配 Minecraft 1.21.11 Intermediary**：全量比对官方映射，彻底消除各类 NoSuchMethodError；
- **默认 `J` 键一键统领全套自动化（屏蔽 `/af run` 跑路宏）**：
  - 屏蔽大地图自动寻路买竿宏，将所有非移动类核心逻辑全面并入默认 `J` 键开关；
  - 按 `J` 键一键启闭：普通鱼自动收抛 + 神话鱼 QTE 拉扯战斗 + 原地 Anti-AFK 防挂机微动 + 2秒漏抛自愈 + 自动背包换竿；
- **智能 Anti-AFK 防挂机**：
  - 前后安全移动防踢，全程浮漂在水不收杆；
  - 坐标闭环急刹控制，绝对防落水；
- **鲁棒性断线与漏抛自愈**：
  - 处于钓鱼点持续 2 秒未检测到浮漂自动重置朝向并补抛；
- **Hypixel 神话鱼拉扯 QTE 战斗系统 (Mythical Fish Combat)**：
  - 深度逆向适配主大厅神话鱼（Archimedes, Aphrodite, Demeter, Helios, Hades, Nyx, Selene, Zeus 等）；
  - 绿灯（REEL）自动 6.6 CPS 点按削减 HP，红灯（STOP）绝对停手，从根源上彻底解决**脱钩与爆竿**问题；
  - 战斗期间自动挂起声音收抛与防 AFK 移动，捕获后自动重新抛竿恢复挂机；
- **拟人化防封禁与 ModMenu 支持**。
