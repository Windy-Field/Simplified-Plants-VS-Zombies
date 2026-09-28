# 植物大战僵尸（Java 版）

# 简介（README.md）

这是一款使用 **Java (Swing)** 编写的植物大战僵尸，目前不仅包含了冒险模式的基础玩法，还有传送带和坚果保龄球两种特殊关卡。

**【注意】本项目代码较为简单（大一~大二作品），可供参考，但不允许商用、抄袭。**

作者：**Windy-Field / Octorange**

> **特别说明**：
> 1. 由于**版权问题**，资源包请**主动联系作者**获取：3519936734@qq.com；
> 2. 当前版本较为原始，部分卡片功能仍在开发中，个别植物的行为、动画或数值与预期可能存在差异，后续会逐步完善。

## 游戏截图

| 主菜单 | 选卡界面 |
| --- | --- |
| ![主菜单](screenshots/menu.png) | ![选卡界面](screenshots/level-1.png) |

| 经典关卡 | 传送带关卡 |
| --- | --- |
| ![普通关卡](screenshots/level-0-play.png) | ![传送带关卡](screenshots/level-4-play.png) |

| 坚果保龄球关卡 | 关卡编辑器 |
| --- | --- |
| ![保龄球关卡](screenshots/level-5-play.png) | ![关卡编辑器](screenshots/editor.png) |

## 更新日志

### V1.7

- 新增双发向日葵，使用 `Plants/TwinSunflower/` 动画；每次生产阳光时同时生成两颗阳光；
- 每种植物的产阳光数量写在 `PlantDefinition` 资料中，普通植物默认生成一颗阳光；
- 优化项目结构，拆分类功能，统一常量名、变量名、方法名；

### V1.6

- 新增火炬树桩，加入正常选卡的植物候选列表；火炬树桩使用 `Plants/Torchwood/Torchwood.gif` 动画和对应卡片；
- 新增火焰豌豆：普通豌豆经过火炬树桩的火焰区域后变成火焰豌豆，伤害变为普通豌豆的 2 倍；
- 冰豌豆经过火炬树桩后会变回普通豌豆，冰冻减速效果随之消失；
- 子弹转换规则改为写入植物资料，由通用的子弹转换流程处理，新增类似植物时不需要在 `Game` 中增加品种判断；

### V1.5

- 正常选卡关卡的卡槽在选卡后常驻画面顶端；传送带和保龄球关卡的卡槽会在开局第二段从画面上方落到顶端，正式开打后保持在顶端；
- 右上角设置了 1x / 1.5x / 2x 加速按钮（原来的 2 倍速现在作为新的 1 倍速）。倍速只影响正式战斗，菜单、选卡、镜头移动、倒计时和结算画面按正常速度运行；
- 战斗使用固定时间小步推进，僵尸、子弹、阳光、出怪和其他定时规则在不同倍速下保持一致；
- 战斗中的基础血量和伤害统一放大 10 倍，具体数值集中在 `world/CombatValues.java`；
- 新增小丑僵尸：可自爆消除 3 * 3 的植物，并伤害范围内的魅惑僵尸；
- **代码整理：品种差异一律写进资料表**。植物和僵尸"跟别人不一样"的地方（绘制偏移、攻击动画、专属动画名）都填在 `PlantCatalog` / `ZombieCatalog` 对应那条资料里，不再散成 `if (植物名.equals("某某"))` 这样的特判；
- 常用的坐标换算、界面按钮位置、飞行动画曲线都收进 `world/Layout.java`，代码里不再出现重复写死的数字；

### V1.3

- **现在每关开局后都有一段演出**：镜头先向右推移，把本关会出现的僵尸各摆一只给玩家看，再移回草坪、倒数「准备 - 安放 - 开始」三秒，然后才正式开打。演出期间僵尸不出场、阳光不掉、卡片不进冷却，所以不会吃亏；
- 经典关卡开始前需从 18 张候选卡中挑选指定数量的卡片放入卡槽（**卡槽数量可在编辑器里设置为 1~8 张**）。玩家必须恰好选中设定数量后才能开始战斗。**挑卡是在镜头对着僵尸的时候进行的**：选卡面板从画面下方升起、停在左下角，右侧仍能看到那排僵尸，方便照着僵尸选卡；点「开始战斗」后它再沉回画面下方，然后镜头才移回草坪；

### V1.2

- 目前一共制作了 6 个关卡（第 1~6 关），通关后自动进入下一关。最后一关通关后，回到主菜单，关卡进度重置为第 1 关；
- 已完成阳光掉落与收集动画、卡片冷却、小推车防线等基础机制；
- 传送带关卡（卡片随机送出，不花阳光）和坚果保龄球关卡；
- **GUI 关卡编辑器：拖动僵尸编排每一波出怪，调节初始阳光、阳光生成速度和出怪速度，还能指定某格随机行出怪、禁用或必选某些植物、限制卡槽数量。**

## 运行环境

- 建议在 **Windows** 系统上运行（启动脚本基于 PowerShell 编写），其他系统不保证能够正常运行。
- 需要 **JDK 17 或更高版本**。
- 首次运行需要联网，脚本会自动下载并校验 JSON 解析库 Gson 2.11.0，此后即可离线运行。

> 如果只是**想玩**、不想安装 Java，可以直接使用打包好的免安装版，见下文「分发给朋友（免安装版）」。

## 准备资源包

再次说明：资源包**不随代码分发**（原因见上方特别说明），但**仍可发邮件向作者索取**。

取得资源包后，请解压到 `java/assets`，其中至少应包含以下子目录：

```text
java/assets/
├── Cards/      卡片图片
├── Map/        关卡背景
├── Plants/     植物动画
├── Screen/     菜单、按钮等界面图片
├── Zombies/    僵尸动画
├── bullets/    子弹图片
├── new_assets/ 新增特殊素材，例如 joker/ 小丑僵尸动画和 torch_wood/ 火焰豌豆
└── levels/     关卡配置 level_0.json ~ level_5.json（内部编号 0~5，显示为第 1~6 关）
```

放置完成后，可以先执行一次 `run.bat test`，检查素材是否齐全。

## 快速开始

**双击运行**：双击 `java/run.bat`，脚本会自动编译并从第 1 关开始。也可以在命令行中带参数运行：

```bat
run.bat            :: 从第 1 关开始
run.bat 3          :: 从第 (3+1) 关开始（参数 0~5 对应第 1~6 关）
run.bat test       :: 运行自检
run.bat editor     :: 打开关卡编辑器（默认第 1 关）
run.bat editor 3   :: 打开关卡编辑器并载入第 4 关
```

**PowerShell**：在**根目录**执行 `.\java\build.ps1`，可附加 `-Level 5`、`-Test`、`-Editor`。

**素材工具**：素材预处理工具位于 `src/main/java/pvz/tools/AssetToolkit.java`，可以规范图片尺寸、把图片序列合成 GIF，也可以调用本机的 FFmpeg 把视频转成 GIF。请先编译项目，再在 `java` 目录下执行：

```bat
java -cp "build\classes;build\gson-2.11.0.jar" pvz.tools.AssetToolkit normalize input.png output.png 64 89
java -cp "build\classes;build\gson-2.11.0.jar" pvz.tools.AssetToolkit images-to-gif output.gif 100 frame1.png frame2.png frame3.png
java -cp "build\classes;build\gson-2.11.0.jar" pvz.tools.AssetToolkit video-to-gif input.mp4 output.gif 73 87 10
```

图片序列转 GIF 时，所有帧会统一成第一帧的尺寸；视频转 GIF 需要系统中能够直接调用 `ffmpeg`。如果没有安装 FFmpeg，只使用前两个图片工具即可。

**IntelliJ IDEA**：用 IDEA 打开 `java` 文件夹，等待 Maven 导入完成：

- 游玩游戏：运行 `src/main/java/pvz/Main.java`；
- 编辑关卡：运行 `src/main/java/pvz/EditorMain.java`。

运行配置的工作目录必须是 `java` 文件夹（IDEA 默认为此），否则程序找不到 `assets`。

## 自检

`run.bat test` 会在不开窗口的情况下运行 `SelfCheckTest`，检查**素材能否读取、植物和僵尸资料是否对齐、战斗数值是否正确、关卡文件格式是否正确、选卡和种植流程能否走通、固定战斗小步是否一致、编辑器存盘再读回是否一致**等内容。
全部通过时输出 `PASS`，**并把各画面的截图存到 `build` 目录**。

自检会借用 `assets/levels/level_99.json` 作为临时文件，跑完即删；**若该编号已被占用，自检会停下并给出提醒，不会覆盖原有文件**。

## 分发给朋友（免安装版）

如果对方的电脑没有安装 Java，也不希望自行配置运行环境，可以用打包脚本制作一个**自带运行时的绿色免安装包**：

```powershell
.\package.ps1
```

脚本会依次完成以下步骤：编译源码 → 生成 `game.jar` → 用 `jlink` 裁出一份只含 `java.desktop` 的精简运行时（约 45MB）→ 把游戏、素材、运行时组装进 `dist/MyPVZ` → 压缩成 `dist/MyPVZ.zip`。

（调试时可以附加 `-SkipPackage` 跳过最后一步压缩，只组装文件夹；附加 `-Name 自定义名` 可以更换包名。）

打包结果如下：

```text
dist/MyPVZ/                 解压后就是这个文件夹，约 92MB
├── play-game.bat           ← 朋友双击这个就能玩
├── level-editor.bat        ← 双击打开关卡编辑器
├── diagnose.bat            打不开时双击它看详细报错
├── readme.txt              给玩家看的简版说明
├── game.jar
├── gson-2.11.0.jar
├── jre/                    精简 Java 运行时，约 45MB
└── assets/                 素材，约 47MB
```

**对方只需要做一件事：解压，然后双击 `play-game.bat`。** 既不需要安装 Java，也不需要联网。

> **为什么文件名都是英文的**：中文文件名在部分解压工具下会解出乱码，命令行里的引号转义也更麻烦。文件名使用 ASCII、内容使用中文，是兼容性最好的组合。批处理内容必须用 **GBK** 编码写出——cmd.exe 按系统代码页逐字节解析 `.bat`，中文 Windows 默认即为 GBK，写成 UTF-8 会乱码，`chcp` 也无法挽救；`readme.txt` 则使用**带 BOM 的 UTF-8**，记事本依靠 BOM 识别编码。

其余几点说明：

- 压缩包约 **73MB**，走网盘比走聊天软件更稳妥（部分聊天软件对文件大小有限制）。
- 对方从网上下载后首次运行时，Windows 可能弹出「未知发布者」提示，点击**仍要运行**即可。
- 启动脚本开头会执行 `cd /d "%~dp0"` 切到自身所在目录，因此整个 `MyPVZ` 文件夹**可以随意改名、放在桌面或拷进 U 盘**。
- 编辑器保存的关卡写在发行包自己的 `assets/levels/` 里，**不会影响仓库中的素材**。
- **打包前请确认 `assets` 中已包含你自己修改过的关卡**，发行包是 `assets` 的完整快照。
- `dist/` 已加入 `.gitignore`，打包产物不会被误提交进仓库。

## 关卡编辑器

![](screenshots/editor.png)

窗口的网格横向表示波次，纵向表示草坪的 5 行；每个格子表示「这一波的这一行」出现哪种僵尸、出现几只。

| 操作 | 说明 |
| --- | --- |
| 从左边把僵尸拖进格子 | 在该格放上这种僵尸 |
| 左键 / 右键点格子 | 选中该格，不改变僵尸数量 |
| Ctrl + 左键 / Ctrl + 右键 | 僵尸数量 -1 / +1 |
| 滚轮 | 快速增减僵尸数量 |
| 把格子拖到别的格子 | 移动；**目标格有内容时两边交换** |
| 把格子拖出网格，或选中后按 Delete | 删除该格的僵尸 |
| 选中格子后按 R，或勾选左边「随机行出怪」 | **该格的僵尸随机出现在这一波的某一行** |

右侧参数列表中，「天空阳光间隔」即阳光生成速度，「出怪间隔」即出怪速度。**阳光间隔只对白天草坪的选卡关卡起作用。**

「卡槽数量」决定这一关开局必须携带几张卡片，可选范围为 1~8，默认为 8。玩家必须恰好选中该数量，「开始战斗」按钮才会出现；调低卡槽数量即可用更少的卡片开局。

「传送带 / 保龄球卡池」「禁用植物」「必选植物」三组参数都可以单独指定植物：

- **传送带 / 保龄球卡池只对传送带、保龄球关卡有效**，正常选卡关卡不会用到。
- **禁用、必选只对正常选卡关卡生效**。被禁用的植物在选卡界面仍然占用卡位，只是显示为灰色锁定，无法点击；被必选的植物在进入选卡界面时会自动飞进卡槽，玩家无法取消。
- **必选张数不能超过卡槽数量**，两份清单中也不能出现同一种植物，存盘前编辑器都会给出提醒。

被禁用的植物同样占用候选区的位置，因此**禁用和必选之外剩余的植物必须足够填满卡槽**，否则玩家凑不齐卡片数量，「开始战斗」按钮永远不会点亮。编辑器会在存盘前提醒，但确认后仍然允许保存；手工改坏的关卡文件也可能导致无法开局。

「保存关卡」会把结果写回 `assets/levels/level_N.json`；「保存并试玩」则会另开一个游戏窗口运行这一关，关闭试玩窗口不影响编辑器。

在工具栏勾选「开发者模式」后再点击「保存并试玩」，试玩画面会显示种植网格、植物、僵尸、子弹和阳光的碰撞箱，以及血量和场上数量。该选项只作用于这一次试玩，不会写入关卡文件，也不影响正常进入游戏的入口。

当关卡编号落在现有关卡范围之外时（但允许紧接着的下一关），「保存关卡」会变为灰色不可用，只能用「另存为…」保存到其他位置。

## 特殊操作说明

| 操作 | 说明 |
| --- | --- |
| 鼠标右键 | 取消手中的卡片 |
| 点击右上角倍速按钮 | 切换 1x、1.5x、2x 速度 |

## 项目结构

源码按主题分成 7 个包：

```text
src/main/java/pvz/
├── Main.java             游戏入口
├── EditorMain.java       关卡编辑器入口
├── plant/                植物 —— 改植物只看这里（外加 Assets 登记动画）
│   ├── PlantDefinition.java   一种植物的完整固定资料（含绘制偏移、攻击动画）
│   ├── PlantCatalog.java      全部植物资料表和编号查询
│   ├── BulletTransformation.java 子弹经过植物时的转换资料
│   ├── PlantActionType.java   植物行为类别
│   ├── Cards.java             卡片生成和资料查询工具
│   ├── PlantRules.java        植物之间的公共判断规则
│   ├── PlantActions.java      遍历植物并分发行为
│   ├── SunProducerActions.java 向日葵和阳光菇行为
│   ├── ShooterActions.java    射手植物行为
│   ├── WallNutActions.java    坚果裂纹行为
│   ├── InstantPlantActions.java 一次性植物行为
│   ├── CloseAttackActions.java 近战和保龄球行为
│   ├── Plant.java            植物对象，血量和初始状态
│   └── Card.java             一张卡片（卡槽、传送带、选卡飞行）
├── zombie/               僵尸
│   ├── ZombieDefinition.java 僵尸固定资料：血量、帽子、速度、动画能力
│   ├── ZombieCatalog.java    僵尸资料表和编辑器名单
│   ├── ZombieAbility.java    僵尸特殊能力类别
│   ├── ZombieEffects.java    僵尸死亡和特殊爆炸效果
│   ├── Zombie.java           僵尸对象：运行状态和各状态动画
│   └── ZombieSpawn.java      一条出场记录：第几毫秒、第几行、什么僵尸
├── game/                 游戏主循环和运行系统
│   ├── Game.java             总控：鼠标输入、画面切换和系统调度
│   ├── LevelSystem.java      出怪、传送带、天空阳光和通关判断
│   ├── CombatSystem.java     僵尸、子弹、阳光、小推车和战斗特效
│   ├── GameState.java        一局游戏里所有会变的数据
│   ├── GameRenderer.java     把 GameState 画出来
│   └── GameScreen.java       菜单 / 选卡 / 游戏中等画面的编号
├── level/                关卡文件
│   ├── Level.java            一个关卡的内容
│   └── LevelLoader.java      把关卡 JSON 读成 Level
├── world/                公共底座
│   ├── Assets.java           图片和动画的加载，"动画名 → 文件"对照表
│   ├── Layout.java           所有坐标、时间常量
│   ├── CombatValues.java     血量、子弹伤害和僵尸伤害
│   ├── Sprite.java           草坪上会动的东西的基类
│   └── Bullet.java / Sun.java / Car.java   子弹、阳光、小推车
├── tools/                素材预处理工具
│   └── AssetToolkit.java     图片规范化、图片序列转 GIF、视频转 GIF
└── editor/               关卡编辑器
    ├── LevelEditor.java      主窗口，兼拖放协调者
    ├── LevelDesign.java      波次网格数据和关卡文件读写
    ├── EditorGrid.java / EditorPalette.java   出怪网格 / 僵尸列表
    ├── CheckListPanel.java   一列可打勾的植物清单
    └── EditorIcons.java / EditorDragController.java   图标缓存 / 拖放接口
```

**主要关系：**

- **逻辑、数据与画面**：`Game` 每帧把植物交给 `PlantActions`，自身处理僵尸和子弹，然后由 `GameRenderer` 绘制画面。

- **植物行为分层**：`Plant` 只保存一株植物的运行状态；`PlantDefinition` 和 `PlantCatalog` 保存固定资料；`PlantActions` 只负责分发，具体行为放在 `SunProducerActions`、`ShooterActions`、`WallNutActions`、`InstantPlantActions` 和 `CloseAttackActions` 中。

- **游戏逻辑分层**：`Game` 负责窗口输入、画面切换和总调度；`LevelSystem` 负责出怪、传送带、天空阳光和通关判断；`CombatSystem` 负责僵尸、子弹、阳光、小推车和战斗特效。

- **品种差异写在资料里，不写成 `if (是某个植物)`**：一株植物与其他植物不同之处——绘制偏移、是否白天睡觉、能否被吃掉、攻击动画的名称——全部填在 `PlantCatalog` 中对应的那条 `PlantDefinition` 里；僵尸同理，填在 `ZombieCatalog` 的 `ZombieDefinition` 里（血量、帽子、速度、专属动画名）。行为处理类只按通用规则办事，新增品种时通常不必修改它们。

- **僵尸资料分层**：`Zombie` 保存单只僵尸的运行状态，`ZombieDefinition` 和 `ZombieCatalog` 保存初始血量、帽子、速度和动画能力。相似僵尸不重复建立子类，固定差异放在定义资料中。

- **僵尸的能力**：`ZombieAbility` 说明「这只僵尸具有哪项额外技能」，`ZombieDefinition.abilityAnimation` 说明「该技能对应哪段动画」。判断一只僵尸是否为小丑时，无需比较名字，查看它的资料即可（`hasOwnAnimation()` 即「该品种是否具有专属动画」这条通用判断）。

- **战斗数值**：`CombatValues` 集中保存植物血量、僵尸血量、子弹伤害和僵尸伤害。所有基础血量和伤害均按 10 倍保存，修改战斗数值时请优先查看此处。

- **继承关系**：`Plant`、`Zombie`、`Bullet`、`Sun` 继承 `Sprite`（位置、血量、当前动画）。植物和僵尸的具体品种目前使用资料对象区分，只有确实出现独特行为时才需要新增专门的类。

- **关卡**：`LevelLoader` 把 JSON 读成 `Level`，`Game.loadLevel()` 再展开进 `GameState`。编辑器使用 `LevelDesign` 读写同一份 JSON，这是游戏与编辑器之间唯一的接口。

- **编辑器拖放**：网格和僵尸列表都只识别 `EditorDragController` 接口，该接口由 `LevelEditor` 实现并居中协调。

## 新增 / 修改植物示例

### 只改数值

| 改什么 | 改哪里 |
| --- | --- |
| 名字、卡片图、花费、冷却、初始血量 | `plant/PlantCatalog.java` 中对应的 `PlantDefinition` |
| 图片绘制偏移（根部不在正中、图偏高） | 同一条 `PlantDefinition` 的 `rootShift` / `verticalShift` |
| 攻击动画名（用于判断能不能被打断） | 同一条 `PlantDefinition` 的 `attackAnimation` |
| 前方索敌范围（例如大嘴花提前一格攻击） | 同一条 `PlantDefinition` 的 `forwardAttackRange` |
| 子弹、僵尸伤害和血量、植物血量 | `world/CombatValues.java` |
| 射速、产阳光间隔、土豆雷出土时间等 | `world/Layout.java` 里对应的常量 |
| 攻击范围、子弹种类等行为细节 | `plant/ShooterActions.java` 或其他行为处理器 |
| 碰撞箱和基础矩形相交 | `world/Sprite.java`；僵尸躯干碰撞箱在 `zombie/Zombie.java` |

> **一条原则**：品种之间的差别应尽量写成「资料里的一个字段」，而不是散落在各处的 `if (植物名.equals("某某"))`。
> 编写判断时应询问「这份资料写了什么」，而不是「这是哪个植物」。

### 新增一种植物

本节以新增一个「寒冰双发射手」为例。

下列 TODO 分为「不做就不能正常使用」和「只有需要时才做」两类。新增植物时可逐项检查，每完成一项即删除对应条目。

#### 必做 TODO

- TODO【必做-植物-1】准备植物动画：把基础动画和实际会用到的状态动画放进 `assets/Plants/植物名/`。
- TODO【必做-植物-2】准备卡片图片：把卡片放进 `assets/Cards/`，并使文件名与 `PlantDefinition.cardPicture` 完全一致。`Assets.loadCards()` 会自动读取 PNG，普通卡片无需再编写登记代码。
- TODO【必做-植物-3】登记植物动画：在 `world/Assets.java` 的 `loadPlants()` 中为每个动画调用 `animation()`、`sequence()` 或 `stretchedAnimation()`。
- TODO【必做-植物-4】登记植物资料：在 `plant/PlantCatalog.java` 中增加一条完整的 `PlantDefinition`，填写名字、卡片、花费、冷却、血量、白天是否睡觉、能否被吃、行为类别、产阳光数量、绘制偏移、攻击动画和子弹转换资料。
- TODO【必做-植物-5】选择行为类别：应优先复用已有的 `PlantActionType` 和行为处理器；仅当现有处理器无法表达新规则时，才新增处理器并将其接入 `PlantActions`。
- TODO【必做-植物-6】补充自检：在 `SelfCheckTest.java` 的 `ANIMATIONS` 中加入新动画；若新增了传送带卡片，还需加入 `CONVEYOR_CARDS`。
- TODO【必做-植物-7】确认通用自检能够覆盖新植物的素材、资料表、卡片和基本流程；不应在 `SelfCheckTest` 中加入植物专用分支，也不应新增植物专用的测试类。
- TODO【必做-植物-8】运行验证：执行 `run.bat test`，确认素材、资料表、卡片、渲染和行为测试均通过。

#### 选做 TODO

- TODO【选做-植物-1】新增传送带或保龄球卡片：准备 `card_植物名_move.png`，并在 `Assets.loadCards()` 中为它增加别名或登记；仅当该植物会进入传送带 / 保龄球卡池时才需要。
- TODO【选做-植物-2】新增白天睡觉状态：准备 `植物名Sleep` 动画，并在资料中将 `sleepsAtDay` 设为 `true`。
- TODO【选做-植物-3】调整绘制位置：若根部不在格子中心，或图片底部偏高，请在资料中填写 `rootShift` 或 `verticalShift`。
- TODO【选做-植物-4】调整攻击保护：若该植物发动攻击后不能被小丑僵尸打断，请在资料中填写 `attackAnimation`。
- TODO【选做-植物-5】新增子弹素材：在 `assets/bullets/` 或 `assets/new_assets/` 中准备图片，并在 `Assets.loadBullets()` 中登记；随后在资料中填写 `BulletTransformation`，或在已有的射手规则中复用对应子弹。
- TODO【选做-植物-6】调整射手枪口：若出射位置与现有射手不同，请在 `Layout.java` 和 `ShooterActions.java` 中补充出射高度。
- TODO【选做-植物-7】增加特殊绘制：仅当普通的 `Sprite.draw()` 无法满足需求时，才在 `GameRenderer.drawPlant()` 中增加绘制方式。
- TODO【选做-植物-8】更新说明：在 README 的更新日志、项目结构或植物新增说明中补充该植物的规则。

#### 基本步骤

1. **准备素材**：把植物**动图**放进 `assets/Plants/…`，把卡片图放进 `assets/Cards/`。

2. **登记动画**：在 `world/Assets.java` 的 `loadPlants()` 中增加一行 `animation("植物名", "Plants/…/1.gif");`。若该植物具有额外状态（睡觉、爆炸等），每个状态各登记一个动画。

3. **注册资料表**：在 `plant/PlantCatalog.java` 中新增一条完整的 `PlantDefinition`，插入位置在最后两个保龄球之前。名字、卡片图、花费、冷却、初始血量和行为类别均写在同一条记录中，无需再维护多组平行的数组。

4. **归类**：在 `PlantDefinition` 中填写对应的 `PlantActionType`：

   | 类别 | 特性 | 处理器 |
   | --- | --- | --- |
   | `SUN_PRODUCER` | 产生阳光 | `SunProducerActions` |
   | `SHOOTER` | 同行有僵尸就开火 | `ShooterActions` |
   | `INSTANT` | 种下播完动画就生效，然后消失 | `InstantPlantActions` |
   | `CLOSE_ATTACK` | 僵尸走到身上才生效 | `CloseAttackActions` |
   | `WALL_NUT` | 根据血量切换裂纹图 | `WallNutActions` |

  白天睡觉与能否被吃掉同样直接写在 `PlantDefinition` 中；蘑菇仍需登记对应的「植物名Sleep」动画。

5. **特殊行为**（可选）：当同一类中仅有少许差别（例如更换一种子弹）时，在对应的行为处理器中增加一个清楚的品种判断即可。只有当该行为完全无法归入现有行为家族时，才应考虑增加新的处理器或专门的类。

6. **自检**：**新增完成后强烈建议执行一次 `run.bat test`。**当资料表、动画或卡片图未对齐时，自检会指出具体是哪一个项目。

新增僵尸的思路与此基本相同：

1. **准备素材**：把僵尸动画放进 `assets/Zombies/`，特殊或新增素材也可以放进 `assets/new_assets/`。
2. **登记动画**：在 `world/Assets.java` 的 `loadZombies()` 中登记走路、攻击、死亡和特殊效果动画。
3. **注册资料**：在 `zombie/ZombieCatalog.java` 中增加一个 `ZombieDefinition`，填写血量、帽子、速度和 `ZombieAbility`。若该品种的动画名不符合通用规律（通用规律为「名字 / 名字+Attack / 名字+LostHead」），就把专属的那段填入 `idleAnimation`（平常播放的）和 `abilityAnimation`（触发技能时播放的）；两者均填 null 表示走通用规律。**不应为了某个品种而在各处的判断中增加 `if (是小丑)`。**
4. **加入自检**：在 `SelfCheckTest.java` 的动画名单中加入新的动画名。
5. **编辑器名单**：编辑器支持的僵尸名字和中文名会从 `ZombieCatalog` 自动取得，无需再单独修改 `LevelDesign.java` 的两组名单。

小丑僵尸在 `ZombieCatalog` 中填写的是 `ZombieAbility.EXPLODES_ON_PLANT`，外加两个动画名（`JokerZombie` / `JokerZombieExplode`）：接触第一株普通植物或因其他原因死亡时，先播放打开盒子的动画；动画结束后，以小丑触发时自身所在格为中心爆炸，清除 3×3 范围内的植物，不伤害普通僵尸，但会伤害范围内的魅惑僵尸。正在攻击的大嘴花和窝瓜不会触发它（判断依据是「该植物正在播放自己的攻击动画」，而非「它是大嘴花」）。子弹、小推车和植物效果杀死小丑时，也遵循同一顺序。

所有僵尸死亡时都会先播放掉头动画，再进入死亡动画；填写了 `abilityAnimation` 的品种（目前只有小丑）以自己那段动画替代普通掉头动画。**整个流程中没有任何一处比较僵尸的名字**——判断一只僵尸是否具有专属动画，调用 `hasOwnAnimation()` 查看资料即可。只有行为规则完全不同的僵尸，才应考虑新增专门的类。

## 额外技术说明

- 画面完全使用 Java（Swing）绘制，不依赖任何游戏引擎；
- 动画通过直接读取 GIF 逐帧播放；
- 关卡配置采用 JSON 格式，由 Gson 解析；
- 碰撞检测使用矩形相交，碰撞范围取整段动画可见像素的总范围，以避免僵尸啃食时来回抖动；
  僵尸的碰撞范围还需去掉前面探出的脑袋和手臂（`Layout.ZOMBIE_FRONT_TRIM_PERCENT` 等），只计算躯干，这样啃食和豌豆溅射都发生在身体上。
- 范围规则目前按职责分开保存：基础碰撞位于 `Sprite`，僵尸躯干碰撞位于 `Zombie`，植物攻击范围位于对应的行为处理器，小丑爆炸范围位于 `ZombieEffects`。
- 常用的坐标换算集中在 `world/Layout.java`，不应在其他位置写死数字：横坐标转列号使用 `Layout.columnAt`，纵坐标转行号使用 `rowAt`，格子的中心和底部使用 `columnCenter`、`rowBottom`，僵尸的落脚基准线使用 `ZOMBIE_FOOT_BASE`（第 row 行即 `ZOMBIE_FOOT_BASE + row * CELL_HEIGHT`）。
- 界面中「画在哪」和「点在哪」必须是同一条数据：菜单按钮、开始按钮等位置都写在 `Layout` 中，绘制和点击判定都从这里取值，以免移动了画面却忘记修改判定。
- 飞行类动画（卡片飞入卡槽、阳光飞向左上角）共用 `Layout.flyProgress` 这一条曲线：前 80% 匀速，后 20% 三次缓出后停住。

## 常见问题

| 现象 | 解决办法 |
| --- | --- |
| 提示「找不到素材目录 assets」 | **请先联系作者**，再按「准备资源包」一节把素材放到 `java/assets` 下 |

## 版权与许可

本项目**源代码**采用 [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/)
（署名 - 非商业性使用 4.0 国际）许可协议，详见 [LICENSE](LICENSE)。

简单来说，你可以自由地学习、修改和分享这份代码，但必须**保留作者署名**，并且**不能用于商业用途**。

作者：**Windy-Field / Octorange**（3519936734@qq.com）

> **注意**：本许可只覆盖源代码，**不包含游戏素材**。
> 图片、动画、音频等素材的版权归其原始权利人所有，不随代码分发（见 `.gitignore` 中的 `assets/`）。
> 如需运行本项目，请自行准备来源合法的素材。
