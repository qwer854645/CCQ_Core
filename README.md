# CCQ Core

Create Craft & Quiet（CCQ）整合包的核心辅助 Mod：AE2 思索装饰方块、内嵌 CCA 材质包与 **Create Aeronautics Physics Gantry（CAPG）**、CMR 兼容补丁、TaCZ 默认枪包运行时精简，以及 Applied Armorer 枪械终端补丁。

| 项目 | 说明 |
|------|------|
| Mod ID | `ccq_core` |
| 版本 | 0.4.17 |
| Minecraft | 1.21.1 |
| 加载器 | NeoForge 21.1+ |
| 许可证 | [MIT](LICENSE)（源码）；嵌入的 CAPG、CCA 材质包及运行时枪包素材见 [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt) |
| 仓库 | https://github.com/qwer854645/CCQ_Core |

## 依赖

| Mod | 必需 | 说明 |
|-----|------|------|
| NeoForge | 是 | 21.1 及以上 |
| [Create](https://www.curseforge.com/minecraft/mc-mods/create) | 是 | 6.0.10 及以上 |
| [Create Aeronautics](https://modrinth.com/mod/create-aeronautics)（`aeronautics_bundled`） | 是 | CAPG 运行所需 |
| Sable（`sable`） | 是 | Aeronautics 依赖 |
| Sable Companion（`sablecompanion`） | 是 | Aeronautics 依赖 |
| [Timeless and Classics Zero (TaCZ)](https://www.curseforge.com/minecraft/mc-mods/timeless-and-classics-zero) | 否 | 未安装时跳过枪包相关功能 |
| Create Crafts & Additions | 否 | 安装后自动启用内置 CCA 材质包；与 CMR 联用时启用液体雪人冷却室 |
| Create More Recipes（`cmr`） | 否 | 安装后启用雪人冷却室补丁、护目镜 / Jade / JEI 集成 |
| Applied Energistics 2 | 否 | **未在 mod 元数据中声明依赖**；安装后才会注册思索装饰方块（模型引用 AE2 资源） |
| JEI / Jade | 否 | 客户端可选；安装后为 CMR 雪人冷却室提供额外 UI |

> **CAPG 已内嵌在 ccq_core JAR 中**（mod id：`createaerophysicsgantry`）。**请勿**在 `mods/` 中再放置独立的 `createaerophysicsgantry` jar，也不要与 **createthrusters** 等同样包含 Physics Gantry 的 mod 重复安装。

## 功能

### 1. AE2 思索装饰方块（Ponder Props）

注册一组**纯装饰**方块，外观仿照 AE2 线缆与 ME 设备，用于 Ponder 教程、建筑展示或整合包任务场景。**仅当 AE2 已安装时注册**；未安装 AE2 时 Mod 仍可正常加载，仅跳过思索方块。

- **无实际功能**：不可接入 ME 网络，不参与物流或合成
- **不可破坏掉落**：无战利品表，适合作为教程专用方块
- **线缆类**支持六向连接状态，放置相邻同类方块时会显示连接臂

包含 6 种线缆 / 纤维与 18 种 ME 部件，例如：

- 线缆：玻璃 / 包层 / 智能 / 致密 / 致密智能 / 石英纤维
- 终端：ME 终端、合成终端、样板编码终端、样板访问终端
- 总线与设备：存储总线、输入 / 输出总线、切换总线、等级发射器、存储监控器、交换监控器、破坏 / 成型平面、ME P2P 隧道等
- 扩展：源质接收器、源质 P2P 隧道、法术 P2P 隧道

方块 ID 均以 `ccq_core:ponder_` 为前缀，可通过 `/give` 或创造模式物品栏获取（若整合包配置了获取方式）。

### 2. CCA 材质包（Dinar's Crafts Additions）

内置 **[Dinar's Crafts Additions v2.1](https://modrinth.com/resourcepack/dinars-crafts-additions)** 资源包（`createaddition` 模型/贴图替换）。**仅当 Create Crafts & Additions（`createaddition`）已安装时**，客户端启动会自动启用该包，无需手动在资源包列表中勾选。

| 项目 | 说明 |
|------|------|
| 作者 | [Din37r](https://modrinth.com/resourcepack/dinars-crafts-additions) |
| 协议 | [MIT License](https://modrinth.com/resourcepack/dinars-crafts-additions) |
| 来源 | https://modrinth.com/resourcepack/dinars-crafts-additions |
| 内置版本 | v2.1 |

> 该材质包以 MIT 协议随 ccq_core 分发；完整授权说明见 [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt)。

### 3. Create Aeronautics Physics Gantry（CAPG，内嵌）

ccq_core 通过 **Jar-in-Jar** 内嵌 **[Create Aeronautics Physics Gantry v1.0.2](https://github.com/blorbee1/createaerophysicsgantry)**（作者 blorbee，MIT），运行时以 mod id `createaerophysicsgantry` 加载。

| 项目 | 说明 |
|------|------|
| 作者 | [blorbee1](https://github.com/blorbee1/createaerophysicsgantry) |
| 协议 | [MIT License](https://github.com/blorbee1/createaerophysicsgantry/blob/main/LICENSE) |
| 来源 | https://github.com/blorbee1/createaerophysicsgantry |
| 内置版本 | 1.0.2 |

新增方块：

- **Physics Gantry Shaft**（物理龙门导轨）
- **Physics Gantry Carriage**（物理龙门滑架）
- **Belt Wheel**（皮带轮）

ccq_core 另提供 CAPG 的 **`zh_cn` 中文语言文件**（`assets/createaerophysicsgantry/lang/zh_cn.json`），原版 CAPG jar 仅含英文。

> 完整嵌入说明见 [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt)。构建时需要 `libs/createaerophysicsgantry-1.0.2.jar`（已随仓库提供）。

### 4. CMR 兼容（Create More Recipes）

当 **Create More Recipes（`cmr`）** 与 **Create** 同时安装时启用以下补丁：

#### 4.1 雪人冷却室 × CCA 吸管（需 `createaddition`）

当 **Create Crafts & Additions** 也已安装时：

1. 用 `createaddition:straw` **右键** `cmr:snowman_cooler` → 变为 `ccq_core:liquid_snowman_cooler`（与 CCA 液体烈焰人同理，是**新方块**）
2. 液体版**只接受管道液体**，不再吃固体燃料
3. **破坏**液体雪人冷却室 → 掉落 `cmr:snowman_cooler` + `createaddition:straw`

| 液体标签 | 默认成员 | 冷却等级 | 每桶（1000 mB）时长 |
|----------|----------|----------|---------------------|
| `#ccq_core:liquid_cooling_regular` | `minecraft:water` | COOLING | 1600 tick |
| `#ccq_core:liquid_cooling_special` | `ccq_core:cooling_liquid` | FREEZING | 3200 tick |

#### 4.2 护目镜、Jade 与 JEI

| 功能 | 条件 | 说明 |
|------|------|------|
| 工程师护目镜 tooltip | `cmr` + `create` | 雪人冷却室热量 / 燃料 / 剩余 tick；方块喷注处理进度 |
| Jade | 同上 + Jade | 液体雪人冷却室剩余冷却与热量状态 |
| JEI | 同上 + JEI | 雪人冷却室相关配方分类 |

护目镜文本通过 ccq_core 自己的 `CcqLang` 解析（`ccq_core.goggles.*` 语言键），避免误用 Create 命名空间导致显示翻译键。

#### 4.3 其他 CMR 修复

- 空雪人冷却室（`cmr:empty_snowman_cooler`）战利品表补全
- 扳手可正常拆除空雪人冷却室

### 5. CCQ 默认枪包（`ccq_default_gun`）

**不在 Mod 内嵌任何 TACZ 默认枪包文件。** 当 TaCZ 已加载时，Mod 会在启动阶段按以下顺序处理：

1. 若 `ccq_default_gun` 已存在且完整 → 跳过，并清理残留的 `tacz_default_gun*`
2. 若 `tacz/` 下已有 `tacz_default_gun/` 或 `tacz_default_gun*.zip` → 从中复制白名单文件，输出为 `ccq_default_gun/`，然后删除原包
3. 若本地没有默认枪包 → **从已安装的 TaCZ mod JAR**（`assets/tacz/custom/tacz_default_gun/`）提取白名单文件，直接生成 `ccq_default_gun/`

保留 12 号霰弹、共享音效（如 CZ75 / 弹匣落地声）、共享贴图、`default_state_machine.lua` 等第三方枪包依赖资源，并运行时生成 CCQ 元数据与语言文件。生成完成后会清除 GunsmithLib 对 `ccq_default_gun` / `tacz_default_gun` 的升级缓存。

#### 启动时机

枪包文件必须在 TaCZ / GunsmithLib 首次扫描 `.minecraft/tacz/` **之前**就绪，否则可能出现首次进游戏贴图异常、重进才恢复的问题。本 Mod 因此：

- 在 `FMLCommonSetup` 中**同步**执行（不使用后台 `enqueueWork`）
- 在客户端 `FMLClientSetup` 中再次检查（兜底，早于 TaCZ 资源包注册）
- 先写入临时目录 `ccq_default_gun.__ccq_build`，完成后再整体替换为 `ccq_default_gun/`，避免扫描到写了一半的目录

> 素材源自玩家本地已安装的 TACZ Default Gun Pack（CC BY-NC-ND 4.0）。详见 [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt)。

### 6. Applied Armorer 枪械终端补丁

**不内置 Applied Armorer 枪包本体。** 仅当玩家已在 `.minecraft/tacz/` 中安装 Applied Armorer（zip 或解压文件夹）时，Mod 会在启动阶段自动写入枪械终端（Guns Terminal）的 `tabs` 配置。

#### 行为说明

```
启动 → tacz/ 目录
  → 扫描所有 Applied Armorer*.zip 及 applied_armorer 命名空间文件夹
  → 校验 gunpack.meta.json 中的 namespace
  → 若 workbench 数据已与预期一致 → 跳过
  → 若缺失、损坏或 tabs 不正确 → 运行时生成 JSON 并写入
  → 单个包失败不影响其他包
```

写入的配置包含 13 个分类标签（手枪、霰弹枪、步枪、狙击、冲锋枪、机枪、榴弹、弹药、瞄具、扩容弹匣、握把、枪口、枪托），使枪械终端 UI 能正常显示 Applied Armorer 物品。

#### 设计原则

- Mod JAR 内**不包含** Applied Armorer 原始资源
- 不随 Mod 分发任何枪包补丁 JSON 文件；配置在运行时由代码生成
- 支持 zip 包原地替换与解压文件夹两种安装形式
- 内容语义与 CCQ 整合包使用的 `-fixed` 参考包一致

> Applied Armorer 为 CC BY-NC-ND 4.0（Koei / Ithaqua）。运行时修改本地已安装的枪包文件可能涉及 ND 条款，请自行评估并在需要时联系作者。详见 [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt)。

## 构建

需要 **Java 21**。`libs/` 目录需包含 CAPG 源 jar（`createaerophysicsgantry-1.0.2.jar`）及 compileOnly 用的 Create / JEI / Jade API jar（见 `build.gradle.kts`）。

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

输出 JAR 位于 `build/libs/ccq_core-<version>.jar`。内嵌 CAPG 位于 `META-INF/jarjar/createaerophysicsgantry-1.0.2.jar`。

运行单元测试：

```bash
gradlew.bat test
```

部分测试依赖本机已安装的 TaCZ mod JAR 或参考枪包路径；若文件不存在，对应用例会跳过（`assumeTrue`），不影响 CI 或其他开发环境通过。

## 项目结构

```
src/main/java/ccq/core/
├── CcqCoreMod.java              # Mod 入口
├── CcqBlocks.java               # 思索装饰方块注册
├── CcqLang.java                 # ccq_core 语言键（护目镜等）
├── block/                       # 方块实现
├── compat/
│   ├── cmr/                     # CMR 补丁、液体雪人冷却室、Mixin 护目镜
│   ├── jade/                    # Jade 集成
│   └── jei/                     # JEI 集成
├── resourcepack/                # CCA 材质包自动启用
└── tacz/
    ├── CcqGunPackBootstrap.java       # 枪包引导入口（CommonSetup + ClientSetup）
    ├── CcqDefaultGunPackPatch.java    # TACZ 默认枪包精简
    ├── CcqDefaultGunPackContent.java  # CCQ 枪包元数据与白名单
    ├── GunPackFileOperations.java     # 枪包文件复制 / 发布 / 删除
    ├── AppliedArmorerPatch.java       # AA 补丁调度
    ├── AppliedArmorerWorkbenchData.java  # tabs JSON 生成
    └── GunPackZipPatcher.java         # zip 条目替换

libs/
└── createaerophysicsgantry-1.0.2.jar   # CAPG 源 jar（构建时嵌入 ccq_core）

src/test/java/ccq/core/tacz/     # 枪包逻辑单元测试

src/main/resources/
├── META-INF/neoforge.mods.toml
├── ccq_core.mixins.json         # CMR 护目镜 Mixin（条件加载）
├── resourcepacks/dinars_crafts_additions/  # Dinar's Crafts Additions v2.1 (MIT, Din37r)
├── assets/ccq_core/             # 模型、方块状态、语言文件
└── assets/createaerophysicsgantry/lang/zh_cn.json  # CAPG 中文
```

## 日志

启动时可关注以下日志（Logger 名称 `ccq_core`）：

| 级别 | 含义 |
|------|------|
| `INFO` | 已精简 TACZ 默认枪包 / 已从 mod JAR 提取 / 已补丁 AA workbench / CMR fixes enabled / 已删除原始 tacz_default_gun* |
| `DEBUG` | 未找到 TACZ 默认枪包或 AA 包 / 数据已是最新 |
| `WARN` | 跳过非 AA 包 / JSON 无效 / 目录读取失败 |
| `ERROR` | 安装或补丁失败（单个包错误不会中断其他包） |

## 许可证

本仓库源代码与编译产物 `ccq_core` Mod JAR 采用 [MIT License](LICENSE)。

Mod JAR 内嵌 **Create Aeronautics Physics Gantry**（blorbee，MIT）与 **Dinar's Crafts Additions** 资源包（Din37r，[MIT](https://modrinth.com/resourcepack/dinars-crafts-additions)）。运行时还可能从玩家本地已安装的 TaCZ / Applied Armorer 枪包读取或改写文件；相关素材版权与限制见 [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt)，不受 ccq_core 源码 MIT 覆盖。

## 第三方内容

本 Mod 嵌入或可能修改的第三方资源授权说明见 [`THIRD_PARTY_NOTICES.txt`](THIRD_PARTY_NOTICES.txt)。

## 相关整合包

本 Mod 为 **Create Craft & Quiet** 整合包开发，与以下组件配合使用：

- Create Aeronautics 与内嵌 CAPG（物理龙门、皮带轮）
- TaCZ 及 CCQ 精简默认枪包
- Applied Armorer、Create Armorer、Ars Armorer 等枪包
- Create More Recipes 及 CCA 液体冷却兼容
- KubeJS 脚本（如 AA 工作台配方调整，独立于本 Mod 的枪包补丁逻辑）

---

如有问题或建议，请在 [GitHub Issues](https://github.com/qwer854645/CCQ_Core/issues) 提交。
