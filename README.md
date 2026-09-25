# 植物大战僵尸（Java 版）

这是一款使用 Java (Swing) 编写的植物大战僵尸小游戏，目前包含冒险模式的基础玩法，还有传送带和坚果保龄球两种特殊关卡。

**代码十分简单，可供学习参考。**

作者：**Windy-Field / Octorange**　|　联系邮箱：3519936734@qq.com

> **特别说明**：
> 1. 由于**版权问题**，资源包请**主动联系作者**获取：3519936734@qq.com
> 2. 当前版本较为原始，部分卡片功能仍在开发中，个别植物的行为、动画或数值与预期可能存在差异，后续会逐步完善。

## 游戏截图

| 主菜单 | 选卡界面 |
| --- | --- |
| ![主菜单](screenshots/menu.png) | ![选卡界面](screenshots/level-1.png) |

| 普通关卡（选卡后开局） | 传送带关卡 |
| --- | --- |
| ![普通关卡](screenshots/level-0-play.png) | ![传送带关卡](screenshots/level-4-play.png) |

| 坚果保龄球关卡 | 关卡编辑器 |
| --- | --- |
| ![保龄球关卡](screenshots/level-5-play.png) | ![关卡编辑器](screenshots/editor.png) |

## 功能概览（v1.0）

- 共 6 个关卡（第 0 关到第 5 关），通关后自动进入下一关。
- 普通关卡开始前从 17 张候选卡中挑 8 张放入卡槽，选卡时带飞行动画。
- 阳光掉落与收集、卡片冷却、阳光不足提示、小推车防线等基础机制。
- 传送带关卡（卡片随机送出，不花阳光）和坚果保龄球关卡。
- 右上角 1x / 2x / 3x 加速按钮。
- 可视化关卡编辑器：拖动僵尸编排每一波出怪，调节初始阳光、阳光生成速度和出怪速度，
  还能指定某格随机行出怪、禁用或必选某些植物。

## 运行环境

- Windows 系统（启动脚本基于 PowerShell）
- JDK 17 或更高版本（通过 IntelliJ IDEA / Maven 运行时需 JDK 21）
- 首次运行需联网，脚本会自动下载并校验 JSON 解析库 Gson 2.11.0，之后可离线运行

## 准备资源包

资源包**不随代码分发**（原因见上方特别说明）。拿到后解压到 `java/assets`，至少要有这些子目录：

```text
java/assets/
├── Cards/      卡片图片
├── Map/        关卡背景
├── Plants/     植物动画
├── Screen/     菜单、按钮等界面图片
├── Zombies/    僵尸动画
├── bullets/    子弹图片
└── levels/     关卡配置 level_0.json ~ level_5.json
```

放好后可以先运行一次 `run.bat test`，检查素材是否齐全。

## 快速开始

**双击运行**：双击 `java/run.bat`，自动编译并从第 1 关开始。也可以在命令行带参数：

```bat
run.bat            :: 从第 1 关开始
run.bat 3          :: 从第 3 关开始（可选 0 到 5）
run.bat test       :: 运行自检
run.bat editor     :: 打开关卡编辑器（默认第 1 关）
run.bat editor 3   :: 打开关卡编辑器并载入第 3 关
```

**PowerShell**：在工作区根目录执行 `.\java\build.ps1`，可加 `-Level 5`、`-Test`、`-Editor`。

**IntelliJ IDEA**：用 IDEA 打开 `java` 文件夹，等 Maven 导入完成后：

- 玩游戏：打开 `src/main/java/pvz/Main.java`，点 `main` 左侧的绿色按钮；
- 改关卡：打开 `src/main/java/pvz/EditorMain.java`，同样点绿色按钮。

运行配置的工作目录必须是 `java` 文件夹（IDEA 默认就是），否则找不到 `assets`。

## 自检

`run.bat test` 会在不开窗口的情况下运行 `SelfCheckTest`，检查素材能否读取、植物资料表是否对齐、
关卡文件格式是否正确、选卡和种植流程能否走通、编辑器存盘再读回是否一致。
全部通过时输出 `PASS`，并把各画面的截图存到 `build` 目录。

自检会借用 `assets/levels/level_99.json` 当临时文件，跑完即删；这个编号已被占用时会停下来提醒，不会覆盖。

## 关卡编辑器

窗口左边是僵尸、中间是出怪网格、右边是关卡参数。
网格横向是波次，纵向是草坪的 5 行；一个格子表示"这一波、这一行"出哪种僵尸、出几只。

| 操作 | 说明 |
| --- | --- |
| 从左边把僵尸拖进格子 | 放上这种僵尸 |
| 左键 / 右键点格子 | 选中该格，不改数量 |
| Ctrl + 左键 / Ctrl + 右键 | 数量 -1 / +1（空格子上 +1 是放下当前选中的僵尸） |
| 滚轮 | 快速增减数量 |
| 把格子拖到别的格子 | 移动；目标格有内容时交换 |
| 把格子拖出网格，或选中后按 Delete | 删除该格 |
| 选中格子后按 R，或勾左边「随机行出怪」 | 这一格的僵尸随机在某一行出现 |

右侧参数里，「天空阳光间隔」就是阳光生成速度，「出怪间隔」就是出怪速度；
阳光间隔只对白天草坪的选卡关卡起作用。

「传送带 / 保龄球卡池」「禁用植物」「必选植物」都是点一下就勾上的清单：

- **卡池**只有传送带和保龄球关卡看，正常选卡关卡用不到。
- **禁用 / 必选**反过来，只对正常选卡关卡生效。禁用的植物在选卡界面保留卡位、
  显示为灰色锁定，点不动；必选的植物在进入选卡界面时自动飞进卡槽，玩家取消不掉。
- 必选最多 8 张（卡槽上限），两份清单不能出现同一种植物，存盘前都会提醒。

「保存关卡」写回 `assets/levels/level_N.json`；「保存并试玩」另开一个游戏窗口跑这一关，
关掉试玩窗口不影响编辑器。没有 `editor` 段的老关卡也能打开。编辑器会按出场时间近似归拢成波次。

关卡编号落在现有关卡范围之外时，「保存关卡」会变灰，只能用「另存为…」存到别处。

## 操作说明

| 操作 | 说明 |
| --- | --- |
| 点击“冒险模式” | 从主菜单进入游戏 |
| 点击候选卡 / 卡槽中的卡 | 选入卡槽 / 退回候选区 |
| 选满 8 张后点击开始按钮 | 开始本关 |
| 点击卡片，再点击草坪空格 | 种下植物 |
| 点击阳光 | 收集阳光 |
| 鼠标右键 | 取消手中的卡片 |
| 点击右上角倍速按钮 | 切换 1x、2x、3x 速度 |

## 项目结构

源码按主题分成 6 个文件夹（包），想改什么就进对应的文件夹：

```text
src/main/java/pvz/
├── Main.java             游戏入口
├── EditorMain.java       关卡编辑器入口
├── plant/                植物 —— 改植物只看这里（外加 Assets 登记动画）
│   ├── Cards.java            植物资料表：名字、卡片图、花费、冷却
│   ├── PlantRules.java       植物分类：射手 / 一次性 / 近身 / 夜间
│   ├── PlantActions.java     植物每一帧的行为：产阳光、开火、爆炸、吞食……
│   ├── Plant.java            植物对象，血量和初始状态
│   └── Card.java             一张卡片（卡槽、传送带、选卡飞行）
├── zombie/               僵尸
│   ├── Zombie.java           僵尸对象：血量、速度、各状态的动画
│   └── ZombieSpawn.java      一条出场记录：第几毫秒、第几行、什么僵尸
├── game/                 游戏主循环
│   ├── Game.java             总控：鼠标输入、出怪、僵尸行为、子弹、胜负
│   ├── GameState.java        一局游戏里所有会变的数据
│   ├── GameRenderer.java     把 GameState 画出来
│   └── GameScreen.java       菜单 / 选卡 / 游戏中等画面的编号
├── level/                关卡文件
│   ├── Level.java            一个关卡的内容
│   └── LevelLoader.java      把关卡 JSON 读成 Level
├── world/                公共底座
│   ├── Assets.java           图片和动画的加载，"动画名 → 文件"对照表
│   ├── Layout.java           所有坐标、时间常量
│   ├── Sprite.java           草坪上会动的东西的基类
│   └── Bullet.java / Sun.java / Car.java   子弹、阳光、小推车
└── editor/               关卡编辑器
    ├── LevelEditor.java      主窗口，兼拖放协调者
    ├── LevelDesign.java      波次网格数据和关卡文件读写
    ├── EditorGrid.java / EditorPalette.java   出怪网格 / 僵尸列表
    ├── CheckListPanel.java   一列可打勾的植物清单
    └── EditorIcons.java / EditorDragController.java   图标缓存 / 拖放接口
```

自检在 `src/test/java/pvz/SelfCheckTest.java`。`build/` 和 IDEA 生成的 `target/` 都是编译产物，可随时删除。

几条主要关系：

- **逻辑、数据、画面三分**：`Game` 管"怎么变"，`GameState` 存数据，`GameRenderer` 管"怎么画"。
  `Game` 每帧把植物交给 `PlantActions`、自己处理僵尸和子弹，然后让 `GameRenderer` 画出来。
- **继承**：只有一处。`Plant`、`Zombie`、`Bullet`、`Sun` 继承 `Sprite`（位置、血量、当前动画）。
- **关卡**：`LevelLoader` 把 JSON 读成 `Level`，`Game.loadLevel()` 再摊进 `GameState`。
  编辑器用 `LevelDesign` 读写同一份 JSON，这是游戏和编辑器之间唯一的接口。
- **编辑器拖放**：网格和僵尸列表都只认 `EditorDragController` 接口，由 `LevelEditor` 实现并居中协调，
  这样两块面板不必互相持有。

## 新增 / 修改一种植物

### 只改数值

| 想改什么 | 改哪里 |
| --- | --- |
| 花费、冷却 | `plant/Cards.java` 的 `COST`、`COOLDOWN` |
| 血量 | `plant/Plant.java` 构造函数（默认 5，坚果 30） |
| 射速、产阳光间隔、土豆雷出土时间等 | `world/Layout.java` 里对应的常量 |
| 攻击范围、子弹种类等行为细节 | `plant/PlantActions.java` 里对应的方法（见下表） |

### 新增一种植物

以新增一个"寒冰双发射手"为例，按顺序做：

1. **准备素材**：把动图放进 `assets/Plants/…`，卡片图放进 `assets/Cards/`。
2. **登记动画**：`world/Assets.java` 的 `loadPlants()` 里加一行
   `animation("植物名", "Plants/…/1.gif");`。有额外状态（睡觉、爆炸等）的，每个状态各登记一个动画。
   新的卡片图在 `loadCards()` 里登记。
3. **加进资料表**：`plant/Cards.java` 的 `PLANTS`、`PICTURES`、`COST`、`COOLDOWN` 四个数组**同一位置**各加一项，
   插在最后两个保龄球之前。选卡界面的卡片数量会自动跟着变。
4. **归类**：如果行为和某类现有植物相同，在 `plant/PlantRules.java` 对应名单里加上名字即可：

   | 名单 | 这类植物会…… | 行为写在 PlantActions 的 |
   | --- | --- | --- |
   | `SHOOTER_PLANTS` | 同行有僵尸就开火 | `updateShooter`、`fireBullets`、`bulletNameFor` |
   | `INSTANT_PLANTS` | 种下播完动画就生效，然后消失 | `updateInstant`、`triggerInstantEffect` |
   | `CLOSE_ATTACK_PLANTS` | 僵尸走到身上才生效 | `updateCloseAttack`、`handleCloseHit` |
   | `NIGHT_PLANTS` | 白天睡觉（需要登记"植物名Sleep"动画） | `Plant` 构造函数 |

   产阳光的植物写在 `PlantActions.dispatchPlant()` 开头的名字判断里。
5. **写特有行为**（可选）：套路全新、归不进上面任何一类时，在 `PlantActions.dispatchPlant()` 加一个分支，
   再写一个自己的处理方法。同类里只有少许差别（比如子弹换一种）时，在上表对应方法里加一个名字判断就够了。
6. **跑自检**：`run.bat test`。四个数组项数不一致、忘了登记动画、缺卡片图，都会在这里报出来。

关卡的 `card_pool` 按名字引用植物，所以插入新植物不会打乱已有的关卡文件。
`banned_plants`（禁用）和 `required_plants`（必选）也是按名字引用，格式和 `card_pool` 一样，
都写在文件顶层、和 `card_pool` 同级；两份清单为空时编辑器不写这两个字段，老关卡文件不受影响。
候选卡按每行 8 张往下排，超过 24 张时选卡界面需要重新排版。

新增僵尸的思路类似：`world/Assets.java` 的 `loadZombies()` 登记各状态动画，`zombie/Zombie.java` 设血量和速度，
想让编辑器也能放它，再加进 `editor/LevelDesign.java` 的 `ZOMBIE_KINDS` 和 `ZOMBIE_LABELS`。

## 技术说明

- 画面用 Java Swing 绘制，不依赖游戏引擎；动画直接读取 GIF 动图逐帧播放。
- 关卡配置采用 JSON 格式，由 Gson 解析。
- 碰撞检测用矩形相交，碰撞范围取整段动画可见像素的总范围，避免僵尸啃食时来回抖动。
  僵尸的碰撞范围再去掉前面探出的脑袋和手臂（`Layout.ZOMBIE_FRONT_TRIM_PERCENT` 等），只算躯干，
  这样啃食和豌豆溅射都发生在身体上。

## 常见问题

| 现象 | 解决办法 |
| --- | --- |
| 提示"找不到素材目录 assets" | 按"准备资源包"一节把素材放到 `java/assets` 下 |
| 提示"需要 JDK 17 或更高版本" | 安装 JDK 17 及以上，并把 `JAVA_HOME` 指向安装目录 |
| 提示"Gson 文件校验失败" | 删除 `build/gson-2.11.0.jar` 后联网重新运行 |
| IDEA 中运行报 `NoSuchFileException` | 把运行配置的工作目录改成 `java` 文件夹 |

## 版权与许可

本项目**源代码**采用 [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/)
（署名 - 非商业性使用 4.0 国际）许可协议，详见 [LICENSE](LICENSE)。

简单说：你可以自由地学习、修改和分享这份代码，但要**保留作者署名**，并且**不能用于商业用途**。

作者：**Windy-Field / Octorange**（3519936734@qq.com）

> **注意**：本许可只覆盖源代码，**不包含游戏素材**。
> 图片、动画、音频等素材版权归其原始权利人所有，不随代码分发（见 `.gitignore` 中的 `assets/`）。
> 想运行本项目请自行准备合法来源的素材。
