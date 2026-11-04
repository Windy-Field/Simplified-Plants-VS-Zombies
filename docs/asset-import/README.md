# 素材整理记录

本次整理后的素材保存在 `assets/new_assets/` 下。
旧库中没有的独立素材另存于 `assets/new_assets/UniqueAssets/`，与老库完全相同的素材副本移入
`assets/new_assets/delete/ImportedAssetPack/`，老素材本身没有改动。

## 当前素材目录

项目实际使用的素材仍放在 `assets/` 根目录下：

| 目录 | 内容 |
| --- | --- |
| `Cards/` | 植物卡片 |
| `Map/` | 关卡背景 |
| `Plants/` | 当前已接入植物动画和植物效果 |
| `Screen/` | 菜单、卡槽、铲子、结算画面等界面素材 |
| `Zombies/` | 当前已接入僵尸动画 |
| `bullets/` | 豌豆、蘑菇孢子等子弹素材 |
| `levels/` | 关卡 JSON 文件 |
| `new_assets/` | 新增素材、整理包、筛选结果和待删除素材 |

`new_assets/joker/`、`new_assets/torch_wood/`、`new_assets/normal_zombie/` 和
`new_assets/no_hand/` 已经被当前代码使用。新增素材包中的其他文件暂时只作为素材库保存，尚未接入游戏。

## 本次新增素材分类

`UniqueAssets/` 中的 147 个文件是旧素材库中没有的独立内容，按用途分为：

| 分类 | 内容 |
| --- | --- |
| `Plants/` | 冰西瓜、猫尾草、路灯花的动画变体 |
| `Zombies/BungeeZombie/` | 蹦极僵尸动画 |
| `Props/` | Crazy Dave、草坪、迷雾、阳光计数、铲子、铲子槽、小推车等素材 |
| `UI/` | 大波僵尸、僵尸获胜、金色奖杯和新版“准备-安放-开始”素材 |
| `Extras/` | 图鉴、关卡选择、挑战缩略图、背景、对话框、进度条和小游戏入口素材 |

完整的逐文件清单和 SHA256 校验值保存在 `inventory.json`；原始路径、英文路径和重复判断保存在 `path-mapping.md`。

`ImportedAssetPack/` 当前保留 331 个文件，包括 147 个独立新增素材、181 个整理包内部重复项和 3 个说明文件或压缩包。
原整理包中与老库完全相同的 333 个文件已移入 `delete/ImportedAssetPack/`，老素材仍保留在原目录中。
新增功能时应优先查看 `UniqueAssets/`，再回到整理包寻找对应的动作帧。

## 重复素材处理

333 个新老重叠文件经过 SHA256 和素材帧比对后确认与老素材完全相同，没有新素材帧数更高的情况。
因此本次没有删除老素材，也没有修改动画速度、缩放倍数或 Java 代码。

这些新包副本已经按原整理包中的相对路径移入 `assets/new_assets/delete/ImportedAssetPack/`，方便以后复查或恢复。
其中 181 个只是在新素材包内部重复的文件不属于新老重叠，本次仍保留在 `ImportedAssetPack/` 中。

## 比对规则

先比较 SHA256；再比较完整画布、全部 RGBA 帧、帧时长和循环次数。
只消除透明像素中的隐藏颜色差异；尺寸、画面、时长或帧数不同的素材均保留。
源包中的 ZIP 仅修改外部文件名，压缩包内部路径和原作者说明内容保留原样。

## 命名规则

角色名使用游戏英文名称，文件名去掉空格和连字符，采用 PascalCase。
动作、按钮、背景、来源包和自制关卡缩略图没有统一的官方文件名，采用内容对应的英文描述。
已有英文资源名保留；中文别名、拼音和明显截断的名字已整理。

## 文件统计

| 分类 | 文件数 |
| --- | ---: |
| 旧库相同，已移入 `delete/` | 333 |
| 新增素材 | 147 |
| 原始说明或压缩包 | 3 |
| 包内重复 | 181 |

## 官方角色名称对照

| 文件名用词 | 游戏英文名 |
| --- | --- |
| `Sunflower` | Sunflower |
| `TwinSunflower` | Twin Sunflower |
| `Peashooter` | Peashooter |
| `SnowPea` | Snow Pea |
| `Repeater` | Repeater |
| `Threepeater` | Threepeater |
| `SplitPea` | Split Pea |
| `GatlingPea` | Gatling Pea |
| `WallNut` | Wall-nut |
| `CherryBomb` | Cherry Bomb |
| `Chomper` | Chomper |
| `Spikeweed` | Spikeweed |
| `Spikerock` | Spikerock |
| `Cactus` | Cactus |
| `Blover` | Blover |
| `GraveBuster` | Grave Buster |
| `CoffeeBean` | Coffee Bean |
| `FlowerPot` | Flower Pot |
| `Garlic` | Garlic |
| `Plantern` | Plantern |
| `Torchwood` | Torchwood |
| `Jalapeno` | Jalapeño |
| `WinterMelon` | Winter Melon |
| `Cattail` | Cattail |
| `Starfruit` | Starfruit |
| `Pumpkin` | Pumpkin |
| `PuffShroom` | Puff-shroom |
| `FumeShroom` | Fume-shroom |
| `GloomShroom` | Gloom-shroom |
| `ScaredyShroom` | Scaredy-shroom |
| `SunShroom` | Sun-shroom |
| `HypnoShroom` | Hypno-shroom |
| `IceShroom` | Ice-shroom |
| `DoomShroom` | Doom-shroom |
| `SeaShroom` | Sea-shroom |
| `TangleKelp` | Tangle Kelp |
| `PotatoMine` | Potato Mine |
| `Squash` | Squash |
| `Zombie` | Zombie |
| `JackInTheBoxZombie` | Jack-in-the-Box Zombie |
| `Zomboni` | Zomboni |
| `Imp` | Imp |
| `DuckyTubeZombie` | Ducky Tube Zombie |
| `FlagZombie` | Flag Zombie |
| `FootballZombie` | Football Zombie |
| `BucketheadZombie` | Buckethead Zombie |
| `ConeheadZombie` | Conehead Zombie |
| `ScreenDoorZombie` | Screen Door Zombie |
| `SnorkelZombie` | Snorkel Zombie |
| `BungeeZombie` | Bungee Zombie |
| `DolphinRiderZombie` | Dolphin Rider Zombie |
| `CrazyDave` | Crazy Dave |
| `GiantWallNut` | Giant Wall-nut |
| `ExplodeONut` | Explode-o-nut |

参考：游戏英文图鉴名称；[游戏介绍](https://en.wikipedia.org/wiki/Plants_vs._Zombies_(video_game))。
动作后缀和界面描述为本项目整理命名，不代表原游戏内部文件名。

## 新增素材

| 文件 | 尺寸 | 帧数 |
| --- | --- | ---: |
| `assets/new_assets/UniqueAssets/Extras/AjaxLoader.gif` | 100×100 | 12 |
| `assets/new_assets/UniqueAssets/Extras/Almanac_CloseButton.png` | 89×52 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Almanac_Ground.jpg` | 1200×200 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Almanac_IndexBack.jpg` | 800×600 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Almanac_IndexBack.png` | 442×310 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Almanac_IndexButton.png` | 164×52 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Almanac_PlantBack.jpg` | 800×600 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Almanac_PlantCard.png` | 316×473 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Almanac_ZombieBack.jpg` | 800×600 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Almanac_ZombieCard.png` | 319×490 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Almanac_ZombieWindow2.png` | 76×76 | 1 |
| `assets/new_assets/UniqueAssets/Extras/AwardScreen_Back.jpg` | 800×600 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Dave2.gif` | 309×519 | 6 |
| `assets/new_assets/UniqueAssets/Extras/Dave3.gif` | 316×519 | 13 |
| `assets/new_assets/UniqueAssets/Extras/FinalWave.gif` | 252×71 | 1 |
| `assets/new_assets/UniqueAssets/Extras/FlagMeterEmpty.png` | 157×21 | 1 |
| `assets/new_assets/UniqueAssets/Extras/FlagMeterFull.png` | 157×21 | 1 |
| `assets/new_assets/UniqueAssets/Extras/FlagMeterLevelProgress.png` | 86×11 | 1 |
| `assets/new_assets/UniqueAssets/Extras/FlagMeterParts1.png` | 22×23 | 1 |
| `assets/new_assets/UniqueAssets/Extras/FlagMeterParts2.png` | 20×18 | 1 |
| `assets/new_assets/UniqueAssets/Extras/GrowSoil.gif` | 61×34 | 6 |
| `assets/new_assets/UniqueAssets/Extras/GrowSpray.gif` | 61×34 | 5 |
| `assets/new_assets/UniqueAssets/Extras/Help.png` | 613×399 | 1 |
| `assets/new_assets/UniqueAssets/Extras/LargeWave.gif` | 286×34 | 1 |
| `assets/new_assets/UniqueAssets/Extras/LoadBar.png` | 332×94 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Logo.jpg` | 900×600 | 1 |
| `assets/new_assets/UniqueAssets/Extras/LogoWord.jpg` | 555×115 | 1 |
| `assets/new_assets/UniqueAssets/Extras/OptionsBackButton32.png` | 360×200 | 1 |
| `assets/new_assets/UniqueAssets/Extras/OptionsBackButton8.png` | 360×200 | 1 |
| `assets/new_assets/UniqueAssets/Extras/OptionsMenuback32.png` | 412×483 | 1 |
| `assets/new_assets/UniqueAssets/Extras/OptionsMenuback8.png` | 412×483 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Peashooterline.jpg` | 640×53 | 1 |
| `assets/new_assets/UniqueAssets/Extras/PointerDown.gif` | 32×35 | 4 |
| `assets/new_assets/UniqueAssets/Extras/PointerUP.gif` | 32×35 | 4 |
| `assets/new_assets/UniqueAssets/Extras/PoolCleaner.png` | 47×64 | 1 |
| `assets/new_assets/UniqueAssets/Extras/RoofCleaner.png` | 56×43 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Scary_Pot.png` | 240×202 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SeedChooser_Background.png` | 465×554 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenAdventure.png` | 331×292 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenAdventure_32.png` | 331×292 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenAdventure_8.png` | 331×292 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenChallenges.png` | 286×244 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenChallenges_32.png` | 286×244 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenChallenges_8.png` | 286×244 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenStartAdventure.png` | 331×292 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenStartAdventure_32.png` | 331×292 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenStartAdventure_8.png` | 331×292 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenSurvival.png` | 313×262 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenSurvival_32.png` | 313×266 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreenSurvival_8.png` | 313×266 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_Almanac.png` | 99×198 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_Almanac_32.png` | 99×198 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_Almanac_8.png` | 99×198 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_Shadow_Adventure.png` | 350×150 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_Shadow_Challenge.png` | 289×127 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_Shadow_Survival.png` | 318×136 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_WoodSign1_32.png` | 293×150 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_WoodSign1_8.png` | 293×150 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_WoodSign2_32.png` | 291×142 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_WoodSign2_8.png` | 291×142 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_WoodSign3_32.png` | 92×40 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SelectorScreen_WoodSign3_8.png` | 92×40 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SodRoll.png` | 68×141 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SodRollCap.png` | 73×71 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Stripe.png` | 22×502 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Sunflower_trophy8.png` | 314×269 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Surface.jpg` | 900×600 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Surface.png` | 900×600 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Tombstone_mounds.png` | 430×273 | 1 |
| `assets/new_assets/UniqueAssets/Extras/ZombieHand.png` | 2310×330 | 1 |
| `assets/new_assets/UniqueAssets/Extras/ZombieNoteSmall.png` | 78×52 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Zombie_seaweed.png` | 232×58 | 1 |
| `assets/new_assets/UniqueAssets/Extras/Zombieline.jpg` | 190×40 | 1 |
| `assets/new_assets/UniqueAssets/Extras/background1unsodded.jpg` | 1400×600 | 1 |
| `assets/new_assets/UniqueAssets/Extras/background1unsodded_1.jpg` | 1400×600 | 1 |
| `assets/new_assets/UniqueAssets/Extras/WallNutBowling.png` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Extras/BungeeZombie.gif` | 279×355 | 4 |
| `assets/new_assets/UniqueAssets/Extras/bgBody.jpg` | 700×1 | 1 |
| `assets/new_assets/UniqueAssets/Extras/bgFooter.jpg` | 700×30 | 1 |
| `assets/new_assets/UniqueAssets/Extras/bgHeader.jpg` | 700×29 | 1 |
| `assets/new_assets/UniqueAssets/Extras/blank.png` | 1×1 | 1 |
| `assets/new_assets/UniqueAssets/Extras/crater1.png` | 90×61 | 1 |
| `assets/new_assets/UniqueAssets/Extras/dialog_bottomleft.png` | 107×97 | 1 |
| `assets/new_assets/UniqueAssets/Extras/dialog_bottommiddle.png` | 93×97 | 1 |
| `assets/new_assets/UniqueAssets/Extras/dialog_bottomright.png` | 108×97 | 1 |
| `assets/new_assets/UniqueAssets/Extras/dialog_centerleft.png` | 107×54 | 1 |
| `assets/new_assets/UniqueAssets/Extras/dialog_centermiddle.png` | 93×54 | 1 |
| `assets/new_assets/UniqueAssets/Extras/dialog_centerright.png` | 105×54 | 1 |
| `assets/new_assets/UniqueAssets/Extras/dialog_header.png` | 187×64 | 1 |
| `assets/new_assets/UniqueAssets/Extras/dialog_topleft.png` | 107×97 | 1 |
| `assets/new_assets/UniqueAssets/Extras/dialog_topmiddle.png` | 93×97 | 1 |
| `assets/new_assets/UniqueAssets/Extras/dialog_topright.png` | 120×97 | 1 |
| `assets/new_assets/UniqueAssets/Extras/fog0.gif` | 210×190 | 1 |
| `assets/new_assets/UniqueAssets/Extras/fog1.gif` | 210×190 | 1 |
| `assets/new_assets/UniqueAssets/Extras/fog2.gif` | 210×190 | 1 |
| `assets/new_assets/UniqueAssets/Extras/fog3.gif` | 210×190 | 1 |
| `assets/new_assets/UniqueAssets/Extras/footer.jpg` | 117×50 | 1 |
| `assets/new_assets/UniqueAssets/Extras/DoomShroomChallengeThumbnail.png` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Extras/icon_poolcleaner.png` | 47×64 | 1 |
| `assets/new_assets/UniqueAssets/Extras/icon_roofcleaner.png` | 56×43 | 1 |
| `assets/new_assets/UniqueAssets/Extras/ZombieRunningChallengeThumbnail.png` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Extras/SunChallengeThumbnail.png` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Extras/GraveyardChallengeThumbnail.png` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Extras/LawnChallengeThumbnail.png` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Extras/plantshadow32.png` | 86×36 | 1 |
| `assets/new_assets/UniqueAssets/Extras/plantshadow8.gif` | 86×36 | 1 |
| `assets/new_assets/UniqueAssets/Extras/plantshadow8.png` | 86×36 | 1 |
| `assets/new_assets/UniqueAssets/Extras/postsbg.jpg` | 180×200 | 1 |
| `assets/new_assets/UniqueAssets/Extras/sod1row.png` | 755×117 | 1 |
| `assets/new_assets/UniqueAssets/Extras/sod3row.png` | 766×330 | 1 |
| `assets/new_assets/UniqueAssets/Extras/splash.png` | 776×88 | 1 |
| `assets/new_assets/UniqueAssets/Extras/talk.png` | 841×585 | 1 |
| `assets/new_assets/UniqueAssets/Extras/trophy.png` | 166×134 | 1 |
| `assets/new_assets/UniqueAssets/Extras/IZombieThumbnail.png` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Extras/BalloonZombieChallengeThumbnail.png` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Extras/ZombieHordeChallengeThumbnail.png` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Extras/ZombieCrowdChallengeThumbnail.gif` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Extras/CherryBombChallengeThumbnail.gif` | 82×60 | 1 |
| `assets/new_assets/UniqueAssets/Props/Notice.jpg` | 842×585 | 1 |
| `assets/new_assets/UniqueAssets/Props/LevelPanel.png` | 118×120 | 1 |
| `assets/new_assets/UniqueAssets/Props/ReadySetPlant.png` | 255×324 | 1 |
| `assets/new_assets/UniqueAssets/Props/Trophy.png` | 314×269 | 1 |
| `assets/new_assets/UniqueAssets/Props/LawnMower.png` | 70×57 | 1 |
| `assets/new_assets/UniqueAssets/Props/CrazyDave.gif` | 309×519 | 17 |
| `assets/new_assets/UniqueAssets/Props/Button.png` | 113×41 | 1 |
| `assets/new_assets/UniqueAssets/Props/HaveFun.png` | 177×178 | 1 |
| `assets/new_assets/UniqueAssets/Props/DayLawn.jpg` | 747×320 | 1 |
| `assets/new_assets/UniqueAssets/Props/Brain.png` | 32×31 | 1 |
| `assets/new_assets/UniqueAssets/Props/Fog1.png` | 210×190 | 1 |
| `assets/new_assets/UniqueAssets/Props/Fog2.png` | 210×190 | 1 |
| `assets/new_assets/UniqueAssets/Props/Fog3.png` | 210×190 | 1 |
| `assets/new_assets/UniqueAssets/Props/Fog4.png` | 210×190 | 1 |
| `assets/new_assets/UniqueAssets/Props/LevelSelector.jpg` | 800×600 | 1 |
| `assets/new_assets/UniqueAssets/Props/Shovel.png` | 76×34 | 1 |
| `assets/new_assets/UniqueAssets/Props/ShovelSlot.png` | 71×35 | 1 |
| `assets/new_assets/UniqueAssets/Props/SunCounter.png` | 123×34 | 1 |
| `assets/new_assets/UniqueAssets/UI/HugeWave.png` | 572×68 | 1 |
| `assets/new_assets/UniqueAssets/UI/ZombiesWon.png` | 564×468 | 1 |
| `assets/new_assets/UniqueAssets/UI/Trophy.png` | 664×536 | 1 |
| `assets/new_assets/UniqueAssets/Plants/WinterMelon.gif` | 126×110 | 9 |
| `assets/new_assets/UniqueAssets/Plants/Cattail.gif` | 122×83 | 14 |
| `assets/new_assets/UniqueAssets/Plants/Plantern_Variant2.gif` | 86×88 | 19 |
| `assets/new_assets/UniqueAssets/Zombies/BungeeZombie/BungeeZombie.gif` | 164×152 | 8 |
| `assets/new_assets/UniqueAssets/UI/SeedBank/SeedBank.png` | 446×87 | 1 |
| `assets/new_assets/UniqueAssets/UI/ReadySetPlant/GameOfTheYear/Set.png` | 652×384 | 1 |
| `assets/new_assets/UniqueAssets/UI/ReadySetPlant/GameOfTheYear/Ready.png` | 662×266 | 1 |
| `assets/new_assets/UniqueAssets/UI/ReadySetPlant/GameOfTheYear/Plant.png` | 836×388 | 1 |
