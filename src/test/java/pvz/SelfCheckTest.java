package pvz;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import pvz.editor.EditorDragController;
import pvz.editor.EditorGrid;
import pvz.editor.EditorIcons;
import pvz.editor.EditorPalette;
import pvz.editor.LevelDesign;
import pvz.game.CombatSystem;
import pvz.game.Game;
import pvz.game.GameState;
import pvz.level.Level;
import pvz.level.LevelLoader;
import pvz.plant.CloseAttackActions;
import pvz.plant.BulletTransformation;
import pvz.plant.Card;
import pvz.plant.InstantPlantActions;
import pvz.plant.Plant;
import pvz.plant.PlantActions;
import pvz.plant.PlantCatalog;
import pvz.plant.PlantDefinition;
import pvz.plant.PlantActionType;
import pvz.plant.ShooterActions;
import pvz.plant.SunProducerActions;
import pvz.plant.WallNutActions;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.Layout;
import pvz.world.Sun;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieCatalog;
import pvz.zombie.ZombieDefinition;
import pvz.zombie.ZombieEffects;
import pvz.zombie.ZombieSpawn;

/**
 * 自检程序：确认 assets 目录里的图片和关卡数据都能被游戏正确读取和使用。
 *
 * 它不打开窗口，而是把画面画到内存里的图片上，然后逐项检查：素材有没有缺、关卡数据对不对、增改植物有无遗漏、选卡和种植能不能走通。
 * 全部通过时打印一行 PASS，任意一项不合格就抛异常并说明是哪一项。
 */
public class SelfCheckTest {
    /** 行为测试统一使用草坪中间的行和列。 */
    private static final int TEST_ROW = Layout.ROW_COUNT / 2;
    private static final int TEST_COLUMN = Layout.COLUMN_COUNT / 3;

    /** 避开未启动状态的游戏时刻。 */
    private static final long TEST_TIME = Layout.DEFAULT_ANIMATION_INTERVAL;

    /** 至少需要一张关卡背景，具体数量由素材目录决定。 */
    private static final int MIN_BACKGROUND_COUNT = 1;

    /** 需要检查的界面图片名单。 */
    private static final String[] SCREEN_IMAGES = {
        "MainMenu", "Adventure_0", "Adventure_1", "ChooserBackground",
        "MoveBackground", "PanelBackground", "StartButton", "GameVictory", "GameLoose", "car", "Boom",
        "shovelSlot", "shovel"
    };

    /** 关卡里会用到的动画名单。 */
    // TODO【必做-植物-6】：新增植物时把所有实际使用的动画名加入这个数组。
    // TODO【必做-植物-7】：新增植物时只核对通用自检是否覆盖它，禁止加入品种专用测试分支。
    // TODO：【必做-10】新增僵尸时需要把僵尸的所有动画状态名加到这个数组里，否则自检不会检查它
    private static final String[] ANIMATIONS = {
        "Sun", "SunFlower", "TwinSunflower", "Peashooter", "SnowPea", "WallNut", "WallNut_cracked1",
        "WallNut_cracked2", "CherryBomb", "CherryBombExplode", "Threepeater", "RepeaterPea", "Chomper", "ChomperAttack",
        "ChomperDigest", "PuffShroom", "PuffShroomSleep", "PotatoMineInit", "PotatoMine",
        "PotatoMineExplode", "Squash", "SquashAttack", "Spikeweed", "Jalapeno", "JalapenoExplode",
        "ScaredyShroom", "ScaredyShroomCry", "ScaredyShroomSleep", "SunShroom", "SunShroomBig",
        "SunShroomSleep", "IceShroom", "IceShroomSnow", "IceShroomSleep", "IceShroomTrap",
        "HypnoShroom", "HypnoShroomSleep", "WallNutBowling", "RedWallNutBowling",
        "RedWallNutBowlingExplode", "PeaNormal", "PeaIce", "BulletMushRoom", "PeaNormalExplode",
        "BulletMushRoomExplode", "PeaFire", "Torchwood", "Zombie", "ZombieAttack", "ZombieLostHead", "ZombieLostHeadAttack",
        "ZombieDie", "ZombieHead", "BoomDie", "ConeheadZombie", "ConeheadZombieAttack", "BucketheadZombie",
        "BucketheadZombieAttack", "ZombieNoArm", "ZombieNoArmAttack", "ZombieNoArmDie",
        "ZombieNoArmLostHead", "ZombieNoArmLostHeadAttack", "FlagZombie", "FlagZombieAttack", "FlagZombieLostHead",
        "FlagZombieLostHeadAttack", "NewspaperZombie", "NewspaperZombieAttack", "NewspaperZombieNoPaper",
        "NewspaperZombieNoPaperAttack", "NewspaperZombieLostHead", "NewspaperZombieLostHeadAttack",
        "NewspaperZombieDie", "NewspaperZombieHead", "NewspaperZombieBoomDie",
        "FootballZombie", "FootballZombieAttack", "FootballZombieOrnLost",
        "FootballZombieOrnLostAttack", "FootballZombieLostHead",
        "FootballZombieLostHeadAttack", "FootballZombieDie", "FootballZombieBoomDie",
        "JokerZombie", "JokerZombieExplode", "JokerBoom"
    };

    /** 传送带模式专用卡片的图片名。 */
    private static final String[] CONVEYOR_CARDS = {
        "card_peashooter_move", "card_snowpea_move", "card_wallnut_move",
        "card_cherrybomb_move", "card_repeaterpea_move", "card_chomper_move", "card_potatomine_move",
        "card_redwallnut_move", "card_torchwood_move", "card_twin_sunflower_move"
    };

    /**
     * 主流程：先检查素材，再检查关卡数据，最后模拟操作并输出画面。
     *
     * 参数：arguments[0] 是素材目录（即 assets）。
     */
    public static void main(String[] arguments) throws Exception {
        Path project = Path.of(arguments[0]);
        Assets assets = new Assets(project);

        checkScreenImages(assets);
        checkBackgrounds(assets);
        checkPlantCatalog(assets);
        checkZombieCatalog(assets);
        checkAnimations(assets);
        checkLevelData(assets);
        checkPlantBehavior(assets);
        checkZombieBehavior(assets);
        checkInteraction(assets, project);
        checkLevelEditor(assets, project);

        // 渲染各模式的静态画面，核对菜单和背景没有空白或裁错。
        renderMenu(assets, project);
        renderGallery(assets, project);
        // 关卡数量从 assets/levels 目录实际有哪些文件数出来，以后加第 7 关不用改自检。
        int levelCount = countLevels(assets);
        for (int level = 0; level < levelCount; level++) {
            renderLevel(assets, project, level);
        }
        System.out.println("PASS：" + levelCount + " 个关卡、植物与僵尸资料、战斗、动画和关卡编辑器检查通过");
    }

    /**
     * 数一数 assets/levels 目录里一共有几个关卡文件。
     *
     * 数量由目录内容决定，加了新关卡自检会自动把新关一起检一遍。
     *
     * 参数：assets 提供关卡文件的位置。
     * 返回：连续存在的关卡个数（从第 0 关开始数，遇到缺号就停）。
     */
    private static int countLevels(Assets assets) {
        int count = 0;
        while (Files.exists(assets.levelPath(count))) {
            count = count + 1;
        }
        check(count > 0, "assets/levels 目录里一个关卡文件都没有");
        return count;
    }

    /** 检查菜单、胜负画面这些整张的界面图片都在。 */
    private static void checkScreenImages(Assets assets) {
        for (int index = 0; index < SCREEN_IMAGES.length; index++) {
            String name = SCREEN_IMAGES[index];
            BufferedImage image = assets.image(name);
            check(image.getWidth() > 0, "缺少界面图片：" + name);
        }
    }

    /** 检查五张背景图都是完整宽度。 */
    private static void checkBackgrounds(Assets assets) {
        int backgroundCount = assets.count("Background");
        check(backgroundCount >= MIN_BACKGROUND_COUNT, "缺少关卡背景");
        for (int index = 0; index < backgroundCount; index++) {
            BufferedImage frame = assets.frame("Background", index);
            check(frame.getWidth() >= Layout.WINDOW_WIDTH, "背景宽度不足：" + index);
        }
    }

    /** 检查每种僵尸的资料、编辑器名单以及运行时使用的状态动画。 */
    private static void checkZombieCatalog(Assets assets) {
        String[] zombieNames = ZombieCatalog.names();
        String[] zombieLabels = ZombieCatalog.labels();
        check(zombieNames.length == zombieLabels.length, "僵尸资料与编辑器名字数量不一致");
        for (int index = 0; index < zombieNames.length; index++) {
            String name = zombieNames[index];
            check(!zombieLabels[index].isBlank(), "僵尸缺少编辑器中文名：" + name);
            ZombieDefinition definition = ZombieCatalog.definitionOf(name);
            check(definition.name.equals(name), "僵尸资料查询错误：" + name);
            check(definition.maxHealth > 0, "僵尸血量无效：" + name);
            check(definition.speed > 0, "僵尸速度无效：" + name);
            check(definition.speedAfterHelmet > 0, "僵尸掉装备后速度无效：" + name);
            for (int previous = 0; previous < index; previous++) {
                check(!name.equals(zombieNames[previous]), "僵尸名字重复：" + name);
            }

            Zombie zombie = new Zombie(name, TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
            check(zombie.health == definition.maxHealth, "僵尸初始血量不对：" + name);
            check(zombie.helmet == definition.helmet, "僵尸初始装备状态不对：" + name);
            check(zombie.speed == definition.speed, "僵尸初始速度不对：" + name);
            checkZombieAnimations(zombie, assets);
        }
    }

    /** 检查僵尸走路、攻击、掉装备、掉臂和掉头后请求的动画都能读取。 */
    private static void checkZombieAnimations(Zombie zombie, Assets assets) {
        checkZombieState(zombie, assets);
        if (zombie.hasOwnAnimation()) {
            check(assets.hasAnimation(ZombieCatalog.definitionOf(zombie.name).abilityAnimation),
                "缺少特殊动作动画：" + zombie.name);
            return;
        }
        zombie.helmet = false;
        checkZombieState(zombie, assets);
        zombie.headLost = true;
        checkZombieState(zombie, assets);
        zombie.headLost = false;
        zombie.armLost = true;
        checkZombieState(zombie, assets);
        zombie.headLost = true;
        checkZombieState(zombie, assets);

        long deathTime = TEST_TIME;
        zombie.die(assets, deathTime, false);
        check(assets.hasAnimation(zombie.animation), "缺少死亡动画：" + zombie.name);
        zombie.dying = false;
        zombie.deathAnimationStarted = false;
        deathTime = deathTime + Layout.DEFAULT_ANIMATION_INTERVAL;
        zombie.die(assets, deathTime, true);
        check(assets.hasAnimation(zombie.animation), "缺少爆炸死亡动画：" + zombie.name);
    }

    /** 检查僵尸当前状态下的行走和攻击动画。 */
    private static void checkZombieState(Zombie zombie, Assets assets) {
        String walking = zombie.stateAnimation(false);
        String attacking = zombie.stateAnimation(true);
        check(assets.hasAnimation(walking), "缺少行走动画：" + zombie.name + " / " + walking);
        check(assets.hasAnimation(attacking), "缺少攻击动画：" + zombie.name + " / " + attacking);
        check(hasOpaquePixel(assets.sprite(walking, 0, 1.0)), "行走动画是空图：" + walking);
        check(hasOpaquePixel(assets.sprite(attacking, 0, 1.0)), "攻击动画是空图：" + attacking);
    }

    /**
     * 检查植物资料表和卡片图。
     *
     * 加新植物时最容易出的错是资料、动画或卡片图没有对齐，这里都会拦下来。
     */
    private static void checkPlantCatalog(Assets assets) {
        int plantCount = PlantCatalog.DEFINITIONS.length;

        for (int index = 0; index < plantCount; index++) {
            PlantDefinition definition = PlantCatalog.definitionAt(index);
            String plantName = definition.name;
            check(PlantCatalog.indexOf(plantName) == index, "植物名字重复或编号不对：" + plantName);
            check(PlantCatalog.definitionOf(plantName) == definition, "植物资料查询错误：" + plantName);
            check(definition.maxHealth > 0, "植物血量无效：" + plantName);
            check(definition.cost >= 0, "植物阳光花费无效：" + plantName);
            check(definition.cooldown >= 0, "植物冷却时间无效：" + plantName);
            check(definition.sunCount >= 0, "植物产阳光数量无效：" + plantName);
            if (definition.actionType == PlantActionType.SUN_PRODUCER) {
                check(definition.sunCount > 0, "产阳光植物的数量无效：" + plantName);
            }
            check(definition.actionType != null, "植物缺少行为类别：" + plantName);
            check(PlantCatalog.nameAt(index).equals(plantName), "植物编号查找错误：" + plantName);
            check(assets.hasAnimation(plantName), "植物没有在 Assets.loadPlants 里登记动画：" + plantName);
            BufferedImage image = assets.image(definition.cardPicture);
            check(image.getHeight() > 0, "缺少卡片：" + definition.cardPicture);
            if (definition.attackAnimation != null) {
                check(assets.hasAnimation(definition.attackAnimation),
                    "植物缺少攻击动画：" + plantName);
            }
            if (definition.sleepsAtDay) {
                check(assets.hasAnimation(plantName + "Sleep"), "植物缺少睡眠动画：" + plantName);
            }
            if (definition.actionType == PlantActionType.INSTANT) {
                checkInstantAnimations(definition, assets);
            }
            for (int rule = 0; rule < definition.bulletTransformations.length; rule++) {
                checkBulletTransformation(definition.bulletTransformations[rule], assets);
            }
        }
        for (int index = 0; index < CONVEYOR_CARDS.length; index++) {
            String name = CONVEYOR_CARDS[index];
            BufferedImage image = assets.image(name);
            check(image.getWidth() > 0, "缺少传送带卡片：" + name);
        }
    }

    /** 检查一次性植物触发后会请求的动画。 */
    private static void checkInstantAnimations(PlantDefinition definition, Assets assets) {
        String effect = "";
        if (definition.name.equals("CherryBomb")) {
            effect = "CherryBombExplode";
        } else if (definition.name.equals("Jalapeno")) {
            effect = "JalapenoExplode";
        } else if (definition.name.equals("IceShroom")) {
            effect = "IceShroomSnow";
        }
        check(!effect.isEmpty(), "一次性植物未登记自检：" + definition.name);
        check(assets.hasAnimation(effect), "一次性植物缺少特效：" + effect);
    }

    /** 检查植物的子弹转换规则引用已登记的图片和有效范围。 */
    private static void checkBulletTransformation(
            BulletTransformation transformation, Assets assets) {
        check(assets.hasAnimation(transformation.sourceBullet),
            "缺少转换前子弹：" + transformation.sourceBullet);
        check(assets.hasAnimation(transformation.targetBullet),
            "缺少转换后子弹：" + transformation.targetBullet);
        check(transformation.damage > 0, "子弹伤害无效");
        check(transformation.zoneWidth > 0, "子弹转换区域宽度无效");
        check(transformation.zoneHeight > 0, "子弹转换区域高度无效");
    }

    /** 检查每个动画都能取到帧，而且第一帧不是完全透明的空图。 */
    private static void checkAnimations(Assets assets) {
        for (int index = 0; index < ANIMATIONS.length; index++) {
            String name = ANIMATIONS[index];
            check(assets.count(name) > 0, "缺少动画：" + name);
            for (int frame = 0; frame < assets.count(name); frame++) {
                assets.visibleBounds(name, frame);
            }
            check(hasOpaquePixel(assets.sprite(name, 0, 1.0)), "动画第一帧是空白的：" + name);
        }
    }

    /** 判断图片里是否至少有一个看得见的像素。 */
    private static boolean hasOpaquePixel(BufferedImage image) {
        for (int row = 0; row < image.getHeight(); row++) {
            for (int column = 0; column < image.getWidth(); column++) {
                if (((image.getRGB(column, row) >>> 24) & 0xFF) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 检查每个关卡的 JSON：卡槽模式、僵尸行号、卡池里的卡都要认得出来。 */
    private static void checkLevelData(Assets assets) throws Exception {
        int levelCount = countLevels(assets);
        for (int level = 0; level < levelCount; level++) {
            JsonObject map = Assets.readObject(assets.levelPath(level));
            JsonArray wave = map.getAsJsonArray("zombie_list");
            check(wave.size() > 0, "僵尸列表为空，关卡 " + level);

            // 只查模式是不是游戏认得的那三种。具体哪一关用哪种模式是可以随便改的，
            // 写死"第 4 关必须是传送带"只会让改过关卡之后自检无故报错。
            int mode = 0;
            if (map.has("choosebar_type")) {
                mode = map.get("choosebar_type").getAsInt();
            }
            check(mode >= Layout.BAR_NORMAL && mode <= Layout.BAR_BOWLING,
                "卡槽模式是游戏不认识的值，关卡 " + level);

            for (int index = 0; index < wave.size(); index++) {
                JsonElement item = wave.get(index);
                JsonObject zombie = item.getAsJsonObject();
                int row = zombie.get("map_y").getAsInt();
                // -1 是编辑器写的"随机行"约定，表示这一只随便挑一行出场，不是越界。
                boolean inRange = row >= 0 && row < Layout.ROW_COUNT;
                check(inRange || row == ZombieSpawn.RANDOM_ROW, "僵尸行号越界，关卡 " + level);

                String name = zombie.get("name").getAsString();
                check(assets.count(name) > 0, "缺少僵尸动画：" + name);
            }

            if (mode != 0) {
                checkCardPool(map);
            }
        }
    }

    /** 检查传送带和保龄球的卡池里没有不认识的卡。 */
    private static void checkCardPool(JsonObject map) {
        JsonArray pool = map.getAsJsonArray("card_pool");
        for (int index = 0; index < pool.size(); index++) {
            JsonElement item = pool.get(index);
            String name = item.getAsJsonObject().get("name").getAsString();
            check(PlantCatalog.indexOf(name) >= 0, "卡池里有不认识的卡：" + name);
        }
    }

    /** 模拟游戏选卡、种植、出怪和传送带流程。 */
    private static void checkInteraction(Assets assets, Path project) throws Exception {
        checkSpeedChoices();
        checkBulletSpeed(assets);
        checkFixedStepMovement(assets);
        checkNormalLevel(assets, project);
        checkConveyorLevel(assets, project);
        checkBowlingLevel(assets, project);
    }
    
    /** 检查速度选项有效且按钮文字与倍率一一对应。 */
    private static void checkSpeedChoices() {
        int[] multipliers = Layout.SPEED_MULTIPLIERS;
        String[] labels = Layout.SPEED_LABELS;
        check(multipliers.length > 0, "没有可用的速度选项");
        check(multipliers.length == labels.length, "速度选项与按钮文字数量不一致");
        for (int index = 0; index < multipliers.length; index++) {
            check(multipliers[index] > 0, "速度倍率无效");
            check(!labels[index].isBlank(), "速度按钮缺少文字");
            check(Layout.speedLabelFor(multipliers[index]).equals(labels[index]),
                "速度按钮文字和倍率不匹配");
            if (index > 0) {
                check(multipliers[index] > multipliers[index - 1], "速度倍率未按大小排列");
            }
        }
    }

    /**
     * 检查每个固定战斗小步只让子弹移动基础距离。
     *
     * 参数：assets 提供子弹图片。
     * 异常：子弹素材缺失时抛出异常。
     */
    private static void checkBulletSpeed(Assets assets) {
        int startX = Layout.GRID_LEFT;
        int startY = Layout.rowBottom(TEST_ROW);
        Bullet normalBullet = new Bullet("PeaNormal", startX, startY,
            TEST_ROW, startY, assets);
        normalBullet.update(TEST_TIME);
        double distance = normalBullet.x - startX;
        check(distance == Layout.BULLET_SPEED, "固定战斗小步中的子弹位移不正确");
    }

    /** 依次核对植物的产阳光、射击、状态变化和子弹转换。 */
    private static void checkPlantBehavior(Assets assets) {
        checkPlantDispatch(assets);
        checkSunProduction(assets);
        checkSleepingPlant(assets);
        checkShooters(assets);
        checkWallNutDamage(assets);
        checkTorchwoodTransformation(assets);
        checkCherryBomb(assets);
        checkJalapeno(assets);
        checkIceShroom(assets);
        checkPotatoMine(assets);
        checkChomper(assets);
        checkSpikeweed(assets);
        checkBowlingPlants(assets);
        checkHypnoShroom(assets);
        checkExplodingZombieProtection(assets);
        checkSquashTarget(assets);
    }

    /** 检查资料表中的每种植物都能被总分发器接收。 */
    private static void checkPlantDispatch(Assets assets) {
        GameState state = new GameState();
        state.time = TEST_TIME;
        for (int index = 0; index < PlantCatalog.DEFINITIONS.length; index++) {
            PlantDefinition definition = PlantCatalog.definitionAt(index);
            Plant plant = new Plant(definition.name, Layout.columnCenter(TEST_COLUMN),
                Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets,
                state.time, false);
            state.plants.add(plant);
        }
        PlantActions actions = new PlantActions(assets, state);
        actions.updateAll();
        check(state.plants.size() == PlantCatalog.DEFINITIONS.length,
            "植物分发测试丢失了植物");
    }

    /** 检查普通向日葵与双发向日葵的产量由资料表控制。 */
    private static void checkSunProduction(Assets assets) {
        String[] names = {"SunFlower", "TwinSunflower", "SunShroom"};
        for (int index = 0; index < names.length; index++) {
            GameState state = new GameState();
            state.time = TEST_TIME;
            Plant plant = new Plant(names[index], Layout.columnCenter(TEST_COLUMN),
                Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, false);
            SunProducerActions actions = new SunProducerActions(assets, state);
            actions.update(plant);
            check(state.suns.isEmpty(), "植物过早产阳光：" + plant.name);
            state.time = state.time + Layout.SUN_PRODUCE_FIRST_DELAY + 1;
            actions.update(plant);
            int expectedCount = PlantCatalog.definitionOf(plant.name).sunCount;
            check(state.suns.size() == expectedCount, "植物产阳光数量不对：" + plant.name);
            if (plant.name.equals("SunShroom")) {
                state.time = plant.placed + Layout.SUN_SHROOM_GROW_TIME + 1;
                actions.update(plant);
                check(plant.animation.equals("SunShroomBig"), "阳光菇长大后没有换动画");
            }
        }
    }

    /** 检查白天睡眠的蘑菇不会执行产阳光行为。 */
    private static void checkSleepingPlant(Assets assets) {
        GameState state = new GameState();
        state.time = TEST_TIME;
        Plant mushroom = new Plant("SunShroom", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, true);
        check(mushroom.sleeping, "阳光菇白天没有睡觉");
        check(mushroom.animation.equals("SunShroomSleep"), "阳光菇缺少睡眠动画");
        state.plants.add(mushroom);
        state.time = state.time + Layout.SUN_PRODUCE_FIRST_DELAY + 1;
        PlantActions actions = new PlantActions(assets, state);
        actions.updateAll();
        check(state.suns.isEmpty(), "睡眠中的阳光菇不应产阳光");
    }

    /** 检查单发、双发、三线和冰豌豆射手的实际子弹。 */
    private static void checkShooters(Assets assets) {
        String[] names = {"Peashooter", "RepeaterPea", "Threepeater", "SnowPea",
            "PuffShroom", "ScaredyShroom"};
        for (int index = 0; index < names.length; index++) {
            GameState state = new GameState();
            state.time = Layout.SHROOM_SHOOT_INTERVAL + 1;
            Plant plant = new Plant(names[index], Layout.columnCenter(TEST_COLUMN),
                Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, 0, false);
            Zombie zombie = new Zombie("Zombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
            int targetColumn = TEST_COLUMN + 2;
            if (plant.name.equals("ScaredyShroom")) {
                targetColumn = TEST_COLUMN + 4;
            }
            placeZombieAt(zombie, Layout.columnCenter(targetColumn), assets, state.time);
            state.zombies.add(zombie);
            ShooterActions actions = new ShooterActions(assets, state);
            actions.update(plant);
            check(!state.bullets.isEmpty(), "射手没有发射子弹：" + plant.name);
            if (plant.name.equals("RepeaterPea")) {
                check(state.bullets.size() > 1, "双发射手没有发射多颗子弹");
            }
            if (plant.name.equals("Threepeater")) {
                check(state.bullets.size() > 1, "三线射手没有发射多条子弹");
                checkBulletRows(state.bullets, TEST_ROW);
            }
            if (plant.name.equals("SnowPea")) {
                check(state.bullets.get(0).name.equals("PeaIce"), "寒冰射手没有发射冰豌豆");
            }
            if (plant.name.equals("PuffShroom") || plant.name.equals("ScaredyShroom")) {
                check(state.bullets.get(0).name.equals("BulletMushRoom"),
                    "蘑菇射手的子弹品种不对：" + plant.name);
            }
            if (plant.name.equals("ScaredyShroom")) {
                placeZombieAt(zombie, Layout.columnCenter(TEST_COLUMN + 1), assets, state.time);
                actions.update(plant);
                check(plant.animation.equals("ScaredyShroomCry"),
                    "僵尸靠近后胆小菇没有受惊");
            }
        }
    }

    /** 检查三线射手的子弹分布在目标行上下。 */
    private static void checkBulletRows(List<Bullet> bullets, int centerRow) {
        boolean foundAbove = false;
        boolean foundCenter = false;
        boolean foundBelow = false;
        for (Bullet bullet : bullets) {
            if (bullet.row == centerRow - 1) {
                foundAbove = true;
            }
            if (bullet.row == centerRow) {
                foundCenter = true;
            }
            if (bullet.row == centerRow + 1) {
                foundBelow = true;
            }
        }
        check(foundAbove && foundCenter && foundBelow, "三线射手的子弹行号不完整");
    }

    /** 检查坚果按血量依次切换两种裂纹动画。 */
    private static void checkWallNutDamage(Assets assets) {
        GameState state = new GameState();
        state.time = TEST_TIME;
        Plant wallNut = new Plant("WallNut", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, false);
        WallNutActions actions = new WallNutActions(assets, state);
        wallNut.health = Layout.WALL_NUT_FIRST_CRACK_HEALTH;
        actions.update(wallNut);
        check(wallNut.animation.equals("WallNut_cracked1"), "坚果第一层裂纹没有出现");
        wallNut.health = Layout.WALL_NUT_SECOND_CRACK_HEALTH;
        actions.update(wallNut);
        check(wallNut.animation.equals("WallNut_cracked2"), "坚果第二层裂纹没有出现");
    }

    /** 检查火炬树桩把普通豌豆转为火豌豆，并防止重复转换。 */
    private static void checkTorchwoodTransformation(Assets assets) {
        long time = TEST_TIME;
        Plant torchwood = new Plant("Torchwood", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, time, false);
        BulletTransformation rule = PlantCatalog.definitionOf("Torchwood")
            .transformationFor("PeaNormal");
        check(rule != null, "火炬树桩缺少普通豌豆转换规则");
        Rectangle zone = torchwood.transformationBounds(rule);
        Bullet bullet = new Bullet("PeaNormal", zone.x + 1, zone.y + 1,
            TEST_ROW, zone.y + 1, assets);
        boolean converted = torchwood.tryTransformBullet(bullet, assets, time);
        check(converted, "普通豌豆经过火炬树桩没有转换");
        check(bullet.name.equals("PeaFire"), "火炬树桩转换的子弹品种不对");
        check(!torchwood.tryTransformBullet(bullet, assets, time),
            "同一颗子弹在同一株植物内重复转换");

        BulletTransformation iceRule = PlantCatalog.definitionOf("Torchwood")
            .transformationFor("PeaIce");
        check(iceRule != null, "火炬树桩缺少冰豌豆转换规则");
        Rectangle iceZone = torchwood.transformationBounds(iceRule);
        Bullet iceBullet = new Bullet("PeaIce", iceZone.x + 1, iceZone.y + 1,
            TEST_ROW, iceZone.y + 1, assets);
        check(torchwood.tryTransformBullet(iceBullet, assets, time),
            "冰豌豆经过火炬树桩没有转换");
        check(iceBullet.name.equals("PeaNormal"), "冰豌豆没有转成普通豌豆");
    }

    /** 检查樱桃炸弹只在动画准备好之后炸伤附近的敌方僵尸。 */
    private static void checkCherryBomb(Assets assets) {
        GameState state = new GameState();
        state.time = TEST_TIME;
        Plant cherry = new Plant("CherryBomb", Layout.columnCenter(3),
            Layout.rowBottom(2), 2, 3, assets, state.time, false);
        Zombie nearby = new Zombie("Zombie", 2, Layout.rowBottom(2), assets);
        placeZombieAt(nearby, Layout.columnCenter(4), assets, state.time);
        Zombie distant = new Zombie("Zombie", 2, Layout.rowBottom(2), assets);
        placeZombieAt(distant, Layout.columnCenter(6), assets, state.time);
        state.zombies.add(nearby);
        state.zombies.add(distant);

        InstantPlantActions actions = new InstantPlantActions(assets, state);
        actions.update(cherry);
        check(!nearby.dying, "樱桃炸弹没有等待准备动画");
        long growTime = assets.count(cherry.animation) * Layout.DEFAULT_ANIMATION_INTERVAL;
        state.time = state.time + growTime + 1;
        actions.update(cherry);
        check(nearby.dying, "樱桃炸弹没有命中相邻格");
        check(!distant.dying, "樱桃炸弹误伤范围外的僵尸");
    }

    /** 检查火爆辣椒只攻击所在行。 */
    private static void checkJalapeno(Assets assets) {
        GameState state = new GameState();
        state.time = 1000;
        Plant pepper = new Plant("Jalapeno", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, false);
        Zombie sameRow = new Zombie("Zombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        Zombie otherRow = new Zombie("Zombie", TEST_ROW - 1,
            Layout.rowBottom(TEST_ROW - 1), assets);
        state.zombies.add(sameRow);
        state.zombies.add(otherRow);
        long growTime = assets.count(pepper.animation) * Layout.DEFAULT_ANIMATION_INTERVAL;
        state.time = state.time + growTime;
        InstantPlantActions actions = new InstantPlantActions(assets, state);
        actions.update(pepper);
        check(sameRow.dying, "辣椒没有击中所在行僵尸");
        check(!otherRow.dying, "辣椒误伤其他行僵尸");
        check(pepper.animation.equals("JalapenoExplode"), "辣椒没有播放火焰动画");
    }

    /** 检查寒冰菇使所有敌方僵尸冻结，但不冻结魅惑僵尸。 */
    private static void checkIceShroom(Assets assets) {
        GameState state = new GameState();
        state.time = TEST_TIME;
        Plant iceShroom = new Plant("IceShroom", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, false);
        Zombie enemy = new Zombie("Zombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        Zombie friend = new Zombie("Zombie", TEST_ROW - 1,
            Layout.rowBottom(TEST_ROW - 1), assets);
        friend.hypno = true;
        state.zombies.add(enemy);
        state.zombies.add(friend);
        long growTime = assets.count(iceShroom.animation) * Layout.DEFAULT_ANIMATION_INTERVAL;
        state.time = state.time + growTime;
        InstantPlantActions actions = new InstantPlantActions(assets, state);
        actions.update(iceShroom);
        check(enemy.frozenUntil == state.time + Layout.ZOMBIE_FREEZE_DURATION,
            "寒冰菇没有冻结敌方僵尸");
        check(friend.frozenUntil == 0, "寒冰菇误冻魅惑僵尸");
        check(iceShroom.animation.equals("IceShroomSnow"), "寒冰菇没有播放雪花动画");
    }

    /** 检查土豆雷出土前无效，出土后会炸中碰到它的僵尸。 */
    private static void checkPotatoMine(Assets assets) {
        GameState state = new GameState();
        state.time = TEST_TIME;
        Plant mine = new Plant("PotatoMine", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, false);
        Zombie zombie = new Zombie("Zombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        placeZombieAt(zombie, Layout.columnCenter(TEST_COLUMN), assets, state.time);
        state.zombies.add(zombie);
        CloseAttackActions actions = new CloseAttackActions(assets, state);
        actions.update(mine);
        check(!zombie.dying, "土豆雷未出土就引爆了");
        state.time = mine.placed + Layout.POTATO_MINE_ARM_TIME + 1;
        actions.update(mine);
        check(zombie.dying, "土豆雷出土后没有炸中僵尸");
        check(mine.animation.equals("PotatoMineExplode"), "土豆雷没有播放爆炸动画");
    }

    /** 检查大嘴花锁敌后等待吞咽时间，再吞掉目标。 */
    private static void checkChomper(Assets assets) {
        GameState state = new GameState();
        state.time = TEST_TIME;
        Plant chomper = new Plant("Chomper", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, false);
        Zombie zombie = new Zombie("Zombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        placeZombieAt(zombie, Layout.columnCenter(TEST_COLUMN), assets, state.time);
        state.zombies.add(zombie);
        CloseAttackActions actions = new CloseAttackActions(assets, state);
        actions.update(chomper);
        check(chomper.target == zombie, "大嘴花没有锁定僵尸");
        check(!zombie.dying, "大嘴花尚未吞咽就杀死目标");
        state.time = chomper.stateStart + Layout.CHOMPER_SWALLOW_DELAY + 1;
        actions.update(chomper);
        check(zombie.dying, "大嘴花没有吞掉目标");
        check(chomper.animation.equals("ChomperDigest"), "大嘴花没有进入消化状态");
    }

    /** 检查地刺接触僵尸时造成一次伤害。 */
    private static void checkSpikeweed(Assets assets) {
        GameState state = new GameState();
        state.time = Layout.SPIKEWEED_DAMAGE_INTERVAL + TEST_TIME;
        Plant spikeweed = new Plant("Spikeweed", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, 0, false);
        Zombie zombie = new Zombie("Zombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        placeZombieAt(zombie, Layout.columnCenter(TEST_COLUMN), assets, state.time);
        state.zombies.add(zombie);
        int healthBefore = zombie.health;
        CloseAttackActions actions = new CloseAttackActions(assets, state);
        actions.update(spikeweed);
        check(healthBefore - zombie.health == Layout.SPIKEWEED_DAMAGE,
            "地刺没有造成预定伤害");
    }

    /** 检查普通保龄球会滚动并伤敌，红保龄球会爆炸。 */
    private static void checkBowlingPlants(Assets assets) {
        GameState state = new GameState();
        state.time = Layout.BOWLING_HIT_INTERVAL + Layout.BOWLING_MOVE_INTERVAL + TEST_TIME;
        state.barType = Layout.BAR_BOWLING;
        Plant bowling = new Plant("WallNutBowling", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, 0, false);
        Zombie target = new Zombie("Zombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        placeZombieAt(target, Layout.columnCenter(TEST_COLUMN), assets, state.time);
        state.zombies.add(target);
        double startingX = bowling.x;
        int startingHealth = target.health;
        CloseAttackActions actions = new CloseAttackActions(assets, state);
        actions.update(bowling);
        check(bowling.x > startingX, "保龄球没有向右滚动");
        check(startingHealth - target.health == Layout.BOWLING_DAMAGE,
            "普通保龄球没有造成预定伤害");

        GameState redState = new GameState();
        redState.time = TEST_TIME;
        redState.barType = Layout.BAR_BOWLING;
        Plant redBowling = new Plant("RedWallNutBowling", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, 0, false);
        Zombie redTarget = new Zombie("Zombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        placeZombieAt(redTarget, Layout.columnCenter(TEST_COLUMN), assets, redState.time);
        redState.zombies.add(redTarget);
        CloseAttackActions redActions = new CloseAttackActions(assets, redState);
        redActions.update(redBowling);
        check(redBowling.animation.equals("RedWallNutBowlingExplode"),
            "红保龄球没有播放爆炸动画");
        check(redTarget.dying, "红保龄球没有炸中僵尸");
    }

    /** 检查僵尸吃掉醒着的魅惑菇后改变阵营。 */
    private static void checkHypnoShroom(Assets assets) {
        GameState state = new GameState();
        state.time = TEST_TIME;
        Plant mushroom = new Plant("HypnoShroom", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, false);
        mushroom.health = Layout.ZOMBIE_BITE_DAMAGE;
        state.plants.add(mushroom);
        Zombie zombie = new Zombie("Zombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        placeZombieAt(zombie, Layout.columnCenter(TEST_COLUMN), assets, state.time);
        state.zombies.add(zombie);
        CombatSystem combat = new CombatSystem(state, assets);
        combat.updateAll();
        check(zombie.attacking, "僵尸没有开始啃魅惑菇");
        state.time = state.time + Layout.ZOMBIE_ATTACK_INTERVAL + 1;
        combat.updateAll();
        check(zombie.hypno, "僵尸吃掉醒着的魅惑菇后没有变成友军");
    }

    /** 依次核对所有品种的移动、装备损失、掉头和特殊死亡流程。 */
    private static void checkZombieBehavior(Assets assets) {
        checkZombieMovement(assets);
        checkZombieDamageStates(assets);
        checkZombieDeathOrder(assets);
        checkJokerExplosion(assets);
    }

    /** 检查所有僵尸在实际战斗中按各自资料的速度移动。 */
    private static void checkZombieMovement(Assets assets) {
        String[] names = ZombieCatalog.names();
        for (int index = 0; index < names.length; index++) {
            Zombie zombie = new Zombie(names[index], TEST_ROW,
                Layout.rowBottom(TEST_ROW), assets);
            GameState state = new GameState();
            state.time = Layout.ZOMBIE_STEP_INTERVAL + 1;
            state.zombies.add(zombie);
            double startingX = zombie.x;
            CombatSystem combat = new CombatSystem(state, assets);
            combat.updateAll();
            double distance = startingX - zombie.x;
            double configuredSpeed = ZombieCatalog.definitionOf(zombie.name).speed;
            check(Math.abs(distance - configuredSpeed) < 0.001,
                "僵尸实际移动距离与资料不符：" + zombie.name);
        }
    }

    /** 检查有装备的僵尸掉装备后换动画，再检查所有普通死亡流程的掉头。 */
    private static void checkZombieDamageStates(Assets assets) {
        String[] names = ZombieCatalog.names();
        for (int index = 0; index < names.length; index++) {
            ZombieDefinition definition = ZombieCatalog.definitionOf(names[index]);
            if (definition.abilityAnimation != null) {
                continue;
            }
            GameState state = new GameState();
            state.time = TEST_TIME;
            Zombie zombie = new Zombie(names[index], TEST_ROW,
                Layout.rowBottom(TEST_ROW), assets);
            state.zombies.add(zombie);
            CombatSystem combat = new CombatSystem(state, assets);

            if (definition.helmet) {
                zombie.health = Layout.ZOMBIE_HELMET_LOST_HEALTH;
                combat.updateAll();
                check(!zombie.helmet, "僵尸失去装备后仍戴着装备：" + zombie.name);
                check(zombie.speed == definition.speedAfterHelmet,
                    "僵尸失去装备后速度与资料不符：" + zombie.name);
                check(zombie.animation.equals(zombie.stateAnimation(false)),
                    "僵尸失去装备后没有切换动画：" + zombie.name);
            }

            zombie.health = Layout.ZOMBIE_HEAD_LOST_HEALTH;
            state.time = state.time + 1;
            combat.updateAll();
            check(zombie.headLost, "僵尸受伤后没有掉头：" + zombie.name);
            check(zombie.animation.equals(zombie.stateAnimation(false)),
                "僵尸掉头后没有切换动画：" + zombie.name);
            check(!state.heads.isEmpty(), "僵尸掉头后没有产生头部效果：" + zombie.name);
        }
    }

    /** 检查普通僵尸必须先播放掉头动画，再播放死亡动画。 */
    private static void checkZombieDeathOrder(Assets assets) {
        GameState state = new GameState();
        state.time = TEST_TIME;
        Zombie zombie = new Zombie("Zombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        ZombieEffects.die(zombie, assets, state, state.time, false);
        check(zombie.waitingForDeath, "普通僵尸没有等待掉头动画");
        check(zombie.headLost, "普通僵尸死亡时没有掉头");
        check(!state.heads.isEmpty(), "普通僵尸掉头后没有头部效果");

        long headDuration = assets.count(zombie.animation) * zombie.frameInterval;
        state.time = state.time + headDuration;
        ZombieEffects.updateDeath(zombie, assets, state, state.time);
        check(zombie.animation.equals("ZombieDie"), "掉头后没有播放死亡动画");
        long deathDuration = assets.count(zombie.animation) * zombie.frameInterval;
        state.time = state.time + deathDuration;
        ZombieEffects.updateDeath(zombie, assets, state, state.time);
        check(!zombie.alive, "死亡动画结束后僵尸仍然存活");
    }

    /** 检查小丑僵尸的专属动画结束后才清除范围内的植物。 */
    private static void checkJokerExplosion(Assets assets) {
        GameState state = new GameState();
        state.time = TEST_TIME;
        Plant plant = new Plant("WallNut", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, false);
        state.plants.add(plant);
        state.occupied[TEST_ROW][TEST_COLUMN] = true;
        Zombie joker = new Zombie("JokerZombie", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        placeZombieAt(joker, Layout.columnCenter(TEST_COLUMN), assets, state.time);
        state.zombies.add(joker);
        ZombieEffects.die(joker, assets, state, state.time, false);
        check(joker.animation.equals("JokerZombieExplode"), "小丑僵尸没有播放专属动画");
        check(plant.alive, "小丑动画未结束就提前清除了植物");
        long duration = assets.count(joker.animation) * joker.frameInterval;
        state.time = state.time + duration + 1;
        ZombieEffects.updateDeath(joker, assets, state, state.time);
        check(!plant.alive, "小丑僵尸爆炸后没有清除植物");
        check(!state.occupied[TEST_ROW][TEST_COLUMN], "小丑僵尸爆炸后没有释放种植格");
    }

    /**
     * 检查植物只有在播放资料中登记的攻击动画时才躲过爆炸僵尸。
     *
     * 参数：assets 提供植物动画。
     */
    private static void checkExplodingZombieProtection(Assets assets) {
        long time = TEST_TIME;
        int row = TEST_ROW;
        int column = TEST_COLUMN;
        int center = Layout.columnCenter(column);
        int bottom = Layout.rowBottom(row);
        Plant squash = new Plant("Squash", center, bottom, row, column,
            assets, time, true);
        check(!squash.isProtectedFromExplodingZombie(),
            "未攻击的窝瓜不应躲过爆炸僵尸");
        squash.change("SquashAttack", assets, time);
        check(squash.isProtectedFromExplodingZombie(),
            "攻击中的窝瓜应躲过爆炸僵尸");

        Plant peashooter = new Plant("Peashooter", center, bottom, row, column,
            assets, time, true);
        check(!peashooter.isProtectedFromExplodingZombie(),
            "没有专属攻击动画的植物不应躲过爆炸僵尸");
    }

    /**
     * 检查窝瓜会跳过无效目标，并选中同一行里距离最近的僵尸。
     *
     * 参数：assets 提供植物和僵尸的动画素材。
     */
    private static void checkSquashTarget(Assets assets) {
        GameState gameState = new GameState();
        gameState.time = TEST_TIME;
        int row = TEST_ROW;
        int column = TEST_COLUMN;
        Plant squash = new Plant("Squash", Layout.columnCenter(column),
            Layout.rowBottom(row), row, column, assets, gameState.time, true);
        CloseAttackActions actions = new CloseAttackActions(assets, gameState);

        Zombie outside = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        placeZombieAt(outside, Layout.columnCenter(column + 2), assets, gameState.time);
        gameState.zombies.add(outside);

        Zombie hypnotized = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        placeZombieAt(hypnotized, Layout.columnCenter(column), assets, gameState.time);
        hypnotized.hypno = true;
        gameState.zombies.add(hypnotized);

        actions.update(squash);
        check(!squash.triggered, "窝瓜误把范围外或被魅惑的僵尸当成目标");

        Zombie farther = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        placeZombieAt(farther, Layout.columnCenter(column + 1), assets, gameState.time);
        gameState.zombies.add(farther);

        Zombie nearer = new Zombie("FootballZombie", row, Layout.rowBottom(row), assets);
        placeZombieAt(nearer, Layout.columnCenter(column) + 10, assets, gameState.time);
        gameState.zombies.add(nearer);

        actions.update(squash);
        check(squash.target == nearer, "窝瓜没有选中距离最近的僵尸");
        int[] squashBody = assets.animationBounds(squash.animation);
        double centerInImage = (squashBody[0] + squashBody[2]) / 2.0;
        double squashCenter = squash.x + centerInImage;
        double zombieCenter = nearer.collisionBox(assets, gameState.time).getCenterX();
        double landingDistance = Math.abs(squashCenter - zombieCenter);
        check(landingDistance < 1,
            "窝瓜没有落在选中的僵尸身上");

        // 半格范围以锁定目标为中心，不再波及整片左中右三格。
        Zombie nearby = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        double nearbyCenter = squashCenter + Layout.CELL_WIDTH / 4.0;
        placeZombieAt(nearby, nearbyCenter, assets, gameState.time);
        gameState.zombies.add(nearby);

        Zombie nearbyLeft = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        double nearbyLeftCenter = squashCenter - Layout.CELL_WIDTH / 4.0;
        placeZombieAt(nearbyLeft, nearbyLeftCenter, assets, gameState.time);
        gameState.zombies.add(nearbyLeft);

        Zombie beyond = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        double beyondWidth = beyond.collisionBox(assets, gameState.time).getWidth();
        double beyondCenter = squashCenter + Layout.CELL_WIDTH / 2.0;
        beyondCenter = beyondCenter + beyondWidth / 2.0 + 10;
        placeZombieAt(beyond, beyondCenter, assets, gameState.time);
        gameState.zombies.add(beyond);

        Zombie beyondLeft = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        double beyondLeftWidth = beyondLeft.collisionBox(assets, gameState.time).getWidth();
        double beyondLeftCenter = squashCenter - Layout.CELL_WIDTH / 2.0;
        beyondLeftCenter = beyondLeftCenter - beyondLeftWidth / 2.0 - 10;
        placeZombieAt(beyondLeft, beyondLeftCenter, assets, gameState.time);
        gameState.zombies.add(beyondLeft);

        gameState.time = squash.stateStart + Layout.SQUASH_HIT_DELAY + 1;
        actions.update(squash);
        check(nearer.dying, "窝瓜没有砸中锁定的目标");
        check(nearby.dying, "窝瓜没有砸中落点半格内的僵尸");
        check(nearbyLeft.dying, "窝瓜没有砸中落点左侧半格内的僵尸");
        check(!beyond.dying, "窝瓜误伤落点半格外的僵尸");
        check(!beyondLeft.dying, "窝瓜误伤落点左侧半格外的僵尸");
        check(!farther.dying, "窝瓜仍按原来的三格范围命中僵尸");

        checkSquashAdjacentCell(assets);
    }

    /**
     * 检查相邻格仍可索敌，移动目标身边的僵尸也会一起被砸。
     *
     * 参数：assets 提供植物和僵尸的动画素材。
     */
    private static void checkSquashAdjacentCell(Assets assets) {
        GameState gameState = new GameState();
        gameState.time = 1000;
        int row = 2;
        int column = 3;
        Plant squash = new Plant("Squash", Layout.columnCenter(column),
            Layout.rowBottom(row), row, column, assets, gameState.time, true);
        CloseAttackActions actions = new CloseAttackActions(assets, gameState);

        Zombie target = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        double targetCenter = Layout.columnCenter(column + 1);
        placeZombieAt(target, targetCenter, assets, gameState.time);
        gameState.zombies.add(target);
        actions.update(squash);
        check(squash.target == target, "窝瓜没有锁定相邻格内的僵尸");

        // 等待砸下期间，锁定目标继续往左走。
        double movedTargetCenter = targetCenter - 30;
        placeZombieAt(target, movedTargetCenter, assets, gameState.time);

        Zombie nearby = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        double nearbyCenter = movedTargetCenter - Layout.CELL_WIDTH / 4.0;
        placeZombieAt(nearby, nearbyCenter, assets, gameState.time);
        gameState.zombies.add(nearby);

        Zombie edge = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        double halfCell = Layout.CELL_WIDTH / 2.0;
        double edgeCenter = movedTargetCenter + halfCell + 5;
        placeZombieAt(edge, edgeCenter, assets, gameState.time);
        gameState.zombies.add(edge);

        Zombie originalCell = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        double originalCenter = Layout.columnCenter(column - 1);
        placeZombieAt(originalCell, originalCenter, assets, gameState.time);
        gameState.zombies.add(originalCell);

        gameState.time = squash.stateStart + Layout.SQUASH_HIT_DELAY + 1;
        actions.update(squash);
        check(target.dying, "窝瓜没有砸中相邻格的锁定目标");
        check(nearby.dying, "窝瓜没有砸中移动目标半格内的僵尸");
        check(edge.dying, "窝瓜没有砸中躯干进入半格范围的僵尸");
        check(!originalCell.dying, "窝瓜仍按种植格位置压扁僵尸");
    }

    /**
     * 将测试僵尸的躯干中心放到指定横坐标。
     *
     * 参数：zombie 是要移动的僵尸；center 是目标横坐标；assets 提供动画；time 是当前时刻。
     */
    private static void placeZombieAt(Zombie zombie, double center,
            Assets assets, long time) {
        double currentCenter = zombie.collisionBox(assets, time).getCenterX();
        double movement = center - currentCenter;
        zombie.x = zombie.x + movement;
    }

    /**
     * 检查一次大步推进和多次小步推进的僵尸结果一致。
     *
     * 这能防止高倍速时因为一帧只处理一次移动，导致不同倍速产生不同规则结果。
     * 参数：assets 提供游戏素材。
     * 异常：关卡读取失败时抛出异常。
     */
    private static void checkFixedStepMovement(Assets assets) throws Exception {
        int level = findLevelWithBar(assets, Layout.BAR_NORMAL);
        if (level < 0) {
            return;
        }

        long firstSpawn = firstSpawnTime(assets, level);
        long targetElapsed = firstSpawn + 400;
        Game oneStepCall = prepareNormalGame(assets, level);
        Game manyStepCalls = prepareNormalGame(assets, level);

        oneStepCall.step(targetElapsed);

        long elapsed = 0;
        while (elapsed < targetElapsed) {
            elapsed = Math.min(targetElapsed, elapsed + 16);
            manyStepCalls.step(elapsed);
        }

        check(oneStepCall.getZombieCount() == manyStepCalls.getZombieCount(),
            "不同推进方式产生的僵尸数量不一致");
        check(oneStepCall.getZombieCount() > 0, "固定步长检查没有等到僵尸出场");
        double firstX = oneStepCall.getZombieX(0);
        double secondX = manyStepCalls.getZombieX(0);
        check(Math.abs(firstX - secondX) < 0.001,
            "不同推进方式产生的僵尸位置不一致");
    }

    /**
     * 创建一个已经进入正式战斗的正常选卡关卡。
     *
     * 参数：assets 提供游戏素材；level 是关卡编号。
     * 返回：可以用 step 推进的游戏对象。
     * 异常：关卡读取失败时抛出异常。
     */
    private static Game prepareNormalGame(Assets assets, int level) throws Exception {
        Game game = new Game(assets, level, false, false);
        game.loadLevel();
        chooseLevelCards(game, assets, level);
        return game;
    }

    /** 按关卡要求选择卡片，并返回实际选中的植物编号。 */
    private static List<Integer> chooseLevelCards(Game game, Assets assets, int level)
            throws Exception {
        Level levelData = new LevelLoader(assets).load(level);
        List<Integer> selected = new ArrayList<Integer>();
        for (int index = 0; index < levelData.requiredPlants.size(); index++) {
            selected.add(levelData.requiredPlants.get(index));
        }
        for (int index = 0; index < PlantCatalog.CHOOSER_COUNT; index++) {
            if (selected.size() >= levelData.maxCards) {
                break;
            }
            if (levelData.bannedPlants.contains(Integer.valueOf(index))) {
                continue;
            }
            if (levelData.requiredPlants.contains(Integer.valueOf(index))) {
                continue;
            }
            if (selected.contains(Integer.valueOf(index))) {
                continue;
            }
            int column = index % 8;
            int row = index / 8;
            int left = Layout.CHOOSER_LEFT + column * Layout.CHOOSER_COLUMN_SPACING;
            int top = Layout.CHOOSER_TOP + row * Layout.CHOOSER_ROW_SPACING;
            Card candidate = new Card(index, left, top);
            Rectangle bounds = candidate.bounds(assets, Layout.CHOOSER_CARD_SCALE);
            game.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
            selected.add(Integer.valueOf(index));
        }
        check(selected.size() == levelData.maxCards, "关卡可选卡片不足：" + level);
        BufferedImage startButton = assets.image("StartButton");
        int startX = Layout.START_BUTTON_LEFT + startButton.getWidth() / 2;
        int startY = Layout.START_BUTTON_TOP + startButton.getHeight() / 2;
        game.click(startX, startY);
        return selected;
    }

    /** 正常关卡：按配置选卡、种植和收铲，再核对僵尸出场。 */
    private static void checkNormalLevel(Assets assets, Path project) throws Exception {
        int level = findLevelWithBar(assets, Layout.BAR_NORMAL);
        if (level < 0) {
            System.out.println("跳过选卡流程检查：现在没有哪一关是正常选卡模式");
            return;
        }

        Game normal = new Game(assets, level, false, false);
        normal.loadLevel();
        List<Integer> selected = chooseLevelCards(normal, assets, level);
        check(selected.size() >= 2, "种植与铲子自检至少需要两张卡：" + level);
        int plantX = Layout.columnCenter(0);
        int plantY = Layout.rowBottom(0);
        clickCardSlot(normal, assets, selected.get(0).intValue(), 0);
        normal.click(plantX, plantY);
        int expectedPlants = 1;
        check(normal.getPlantCount() == expectedPlants, "选卡或种植失败，关卡 " + level);

        // 铲子移除植物后，原来的格子必须重新变为空闲。
        clickShovelSlot(normal);
        normal.click(plantX, plantY - Layout.CELL_HEIGHT / 4);
        check(normal.getPlantCount() == 0, "铲子没有移除植物，关卡 " + level);
        clickCardSlot(normal, assets, selected.get(1).intValue(), 1);
        normal.click(plantX, plantY);
        check(normal.getPlantCount() == expectedPlants,
            "铲掉植物后原格子不能重新种植，关卡 " + level);

        // 点空草坪后铲子应收回，后续点卡和种植不该再被铲子拦住。
        int emptyX = Layout.columnCenter(Layout.COLUMN_COUNT / 2);
        int emptyY = Layout.rowBottom(Layout.ROW_COUNT / 2);
        clickShovelSlot(normal);
        normal.click(emptyX, emptyY);
        int emptyCardPosition = 0;
        if (selected.size() > 2) {
            emptyCardPosition = 2;
        } else {
            int firstPlant = selected.get(0).intValue();
            long cooldown = PlantCatalog.definitionAt(firstPlant).cooldown;
            normal.step(cooldown + TEST_TIME);
        }
        int emptyPlant = selected.get(emptyCardPosition).intValue();
        clickCardSlot(normal, assets, emptyPlant, emptyCardPosition);
        normal.click(emptyX, emptyY);
        expectedPlants = expectedPlants + 1;
        check(normal.getPlantCount() == expectedPlants,
            "铲子点空格后没有收回，关卡 " + level);

        // 推到这一关最早那只僵尸该出场之后，再看它到了没有。
        // 写死"过 1 秒就该有僵尸"的话，把出怪时间往后挪一点自检就会报错。
        normal.step(firstSpawnTime(assets, level) + TEST_TIME);
        check(normal.getZombieCount() > 0, "僵尸没有按时出场，关卡 " + level);
        render(normal, project.resolve("../build/level-" + level + "-play.png"));
    }

    /** 点击卡槽中指定位置的实际卡片中心。 */
    private static void clickCardSlot(Game game, Assets assets,
            int plantIndex, int position) {
        Card card = new Card(plantIndex, Layout.cardSlotLeft(position), Layout.CARD_BAR_TOP);
        Rectangle bounds = card.bounds(assets, Layout.CARD_SCALE);
        game.click(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    }

    /** 点击铲子卡槽的中心，避免测试依赖图片边缘。 */
    private static void clickShovelSlot(Game game) {
        int centerX = Layout.SHOVEL_SLOT_LEFT + Layout.SHOVEL_SLOT_WIDTH / 2;
        int centerY = Layout.SHOVEL_SLOT_TOP + Layout.SHOVEL_SLOT_HEIGHT / 2;
        game.click(centerX, centerY);
    }

    /** 传送带关卡：推进一秒应该自动出一张卡。 */
    private static void checkConveyorLevel(Assets assets, Path project) throws Exception {
        int level = findLevelWithBar(assets, Layout.BAR_CONVEYOR);
        if (level < 0) {
            System.out.println("跳过传送带检查：现在没有哪一关是传送带模式");
            return;
        }

        Game conveyor = new Game(assets, level, false, false);
        conveyor.loadLevel();
        conveyor.step(Layout.CONVEYOR_CARD_INTERVAL + TEST_TIME);
        check(conveyor.getCardCount() > 0, "传送带没有出卡，关卡 " + level);
        render(conveyor, project.resolve("../build/level-" + level + "-play.png"));
    }

    /** 保龄球关卡：出一张卡，点在草坪上应该能种下球。 */
    private static void checkBowlingLevel(Assets assets, Path project) throws Exception {
        int level = findLevelWithBar(assets, Layout.BAR_BOWLING);
        if (level < 0) {
            System.out.println("跳过保龄球检查：现在没有哪一关是保龄球模式");
            return;
        }

        Game bowling = new Game(assets, level, false, false);
        bowling.loadLevel();
        bowling.step(Layout.CONVEYOR_CARD_INTERVAL + TEST_TIME);
        check(bowling.getCardCount() > 0, "保龄球关卡没有出卡，关卡 " + level);

        bowling.click(615, 25);
        bowling.click(75, 160);
        check(bowling.getPlantCount() > 0, "保龄球没能种下，关卡 " + level);
        render(bowling, project.resolve("../build/level-" + level + "-play.png"));
    }

    /**
     * 把每个动画的第一帧排成一张总览图，方便人工核对名字和图片有没有对错。
     */
    private static void renderGallery(Assets assets, Path project) throws Exception {
        int cellWidth = 170;
        int cellHeight = 190;
        int columns = 10;
        int rows = (ANIMATIONS.length + columns - 1) / columns;
        BufferedImage sheet = new BufferedImage(columns * cellWidth, rows * cellHeight,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = sheet.createGraphics();
        painter.setColor(new Color(90, 140, 70));
        painter.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int index = 0; index < ANIMATIONS.length; index++) {
            String name = ANIMATIONS[index];
            int left = (index % columns) * cellWidth;
            int top = (index / columns) * cellHeight;
            BufferedImage image = assets.sprite(name, 0, 1.0);
            // 太大的特效图按比例缩小，放得进格子就行。
            double fit = Math.min(1.0, Math.min((cellWidth - 10.0) / image.getWidth(),
                (cellHeight - 30.0) / image.getHeight()));
            int width = (int) (image.getWidth() * fit);
            int height = (int) (image.getHeight() * fit);
            painter.drawImage(image, left + 5, top + 5, width, height, null);
            painter.setColor(Color.WHITE);
            painter.drawString(name + " x" + assets.count(name), left + 5, top + cellHeight - 8);
            painter.setColor(new Color(90, 140, 70));
        }
        painter.dispose();
        ImageIO.write(sheet, "png", project.resolve("../build/gallery.png").toFile());
    }

    /** 不显示窗口，输出菜单的 800×600 渲染图供人工核对。 */
    private static void renderMenu(Assets assets, Path project) throws Exception {
        // 这里画的是主菜单，没有载入关卡，所以传第几关都一样。
        Game game = new Game(assets, 1, false, false);
        render(game, project.resolve("../build/menu.png"));
    }

    /**
     * 不显示窗口，输出指定关卡的静态画面，并核对游戏读到的卡槽模式和文件里写的一致。
     *
     * 参数：assets 提供素材；project 是 assets 目录；level 是关卡编号。
     * 异常：读关卡或写截图失败时抛出异常。
     */
    private static void renderLevel(Assets assets, Path project, int level) throws Exception {
        JsonObject map = Assets.readObject(assets.levelPath(level));
        int expected = Layout.BAR_NORMAL;
        if (map.has("choosebar_type")) {
            expected = map.get("choosebar_type").getAsInt();
        }

        Game game = new Game(assets, level, false, false);
        game.loadLevel();
        check(game.getBarType() == expected, "游戏读到的卡槽模式和文件里写的不一致，关卡 " + level);

        render(game, project.resolve("../build/level-" + level + ".png"));
    }

    /**
     * 在现有关卡里找出第一个用指定卡槽模式的关卡。
     *
     * 关卡是拿来给人改的，哪一关是传送带、哪一关是保龄球随时可能变，
     * 所以不写死关卡号，而是翻一遍关卡文件按模式去找。
     *
     * 参数：assets 提供关卡位置；barType 是要找的卡槽模式。
     * 返回：关卡编号；一关都没用这种模式就返回 -1。
     * 异常：读关卡文件失败时抛出异常。
     */
    private static int findLevelWithBar(Assets assets, int barType) throws Exception {
        int levelCount = countLevels(assets);
        for (int level = 0; level < levelCount; level++) {
            JsonObject map = Assets.readObject(assets.levelPath(level));
            int mode = Layout.BAR_NORMAL;
            if (map.has("choosebar_type")) {
                mode = map.get("choosebar_type").getAsInt();
            }
            if (mode == barType) {
                return level;
            }
        }
        return -1;
    }

    /**
     * 读出某一关最早那只僵尸的出场时刻。
     *
     * 参数：assets 提供关卡位置；level 是关卡编号。
     * 返回：最早的出场时刻（毫秒）。
     * 异常：读关卡文件失败时抛出异常。
     */
    private static long firstSpawnTime(Assets assets, int level) throws Exception {
        JsonObject map = Assets.readObject(assets.levelPath(level));
        JsonArray wave = map.getAsJsonArray("zombie_list");
        long earliest = Long.MAX_VALUE;
        for (int index = 0; index < wave.size(); index++) {
            long spawnTime = wave.get(index).getAsJsonObject().get("time").getAsLong();
            if (spawnTime < earliest) {
                earliest = spawnTime;
            }
        }
        return earliest;
    }

    /** 让 Swing 画到内存图片并保存，顺便检查中间那个像素不是空的。 */
    private static void render(Game game, Path output) throws Exception {
        game.setSize(Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT);
        BufferedImage image = new BufferedImage(Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = image.createGraphics();
        game.paint(painter);
        painter.dispose();
        ImageIO.write(image, "png", output.toFile());

        int middleColor = image.getRGB(Layout.WINDOW_WIDTH / 2, Layout.WINDOW_HEIGHT / 2);
        check(middleColor != 0, "渲染结果是空白：" + output);
    }

    /**
     * 检查关卡编辑器：网格能正确摊平成出怪表，存盘再读回后内容不变，
     * 游戏侧也确实认得编辑器写出的新字段。
     *
     * 参数：assets 提供素材和关卡位置；project 是 assets 目录。
     * 异常：读写关卡文件失败时抛出异常。
     */
    private static void checkLevelEditor(Assets assets, Path project) throws Exception {
        LevelDesign design = new LevelDesign();
        design.initialSun = 125;
        design.skySunInterval = 3500;
        design.firstWaveDelay = 10000;
        design.waveInterval = 15000;
        design.spawnSpacing = 500;
        design.maxCards = 5;
        design.setWaveCount(3);
        int firstRow = 0;
        int firstWave = 0;
        int lastRow = Layout.ROW_COUNT - 1;
        int lastWave = design.waveCount() - 1;
        String firstKind = ZombieCatalog.names()[0];
        String lastKind = ZombieCatalog.names()[2];
        int firstCount = 2;
        int lastCount = 3;
        design.setCell(firstRow, firstWave, firstKind, firstCount);
        design.setCell(lastRow, lastWave, lastKind, lastCount);

        // 网格上的"倍数"要展开成若干条出场记录，同一格里的僵尸按间隔错开。
        List<ZombieSpawn> spawns = design.buildSpawns();
        int totalCount = firstCount + lastCount;
        check(spawns.size() == totalCount, "网格摊平后的僵尸总数不对");
        check(spawns.get(0).spawnTime == design.firstWaveDelay,
            "第一波的出场时间不对");
        check(spawns.get(0).row == firstRow, "第一波的行号不对");
        long secondTime = design.firstWaveDelay + design.spawnSpacing;
        check(spawns.get(1).spawnTime == secondTime,
            "同一格里的第二只僵尸没有按间隔错开");
        int lastIndex = spawns.size() - 1;
        long lastTime = design.waveTime(lastWave) + (lastCount - 1) * design.spawnSpacing;
        check(spawns.get(lastIndex).spawnTime == lastTime, "最后一波的时间不对");
        check(spawns.get(lastIndex).name.equals(lastKind), "最后一波的僵尸品种不对");

        // 存盘再读回，网格和参数都要原样还原。
        // 借 99 号当临时关卡，用完就删；万一这个编号已经被占用，先停下来提醒，免得覆盖别人的文件。
        Path temporary = project.resolve("levels/level_99.json");
        check(!Files.exists(temporary), "自检需要用 level_99.json 当临时文件，请先把已有的同名文件挪走");
        try {
            design.save(temporary);
            LevelDesign reloaded = LevelDesign.load(temporary);
            check(reloaded.initialSun == design.initialSun, "读回后初始阳光不对");
            check(reloaded.skySunInterval == design.skySunInterval, "读回后阳光生成速度不对");
            check(reloaded.waveInterval == design.waveInterval, "读回后出怪间隔不对");
            check(reloaded.waveCount() == design.waveCount(), "读回后波数不对");
            check(reloaded.maxCards == design.maxCards, "读回后卡槽数量不对");
            check(reloaded.totalZombies() == totalCount, "读回后僵尸总数不对");
            check(firstKind.equals(reloaded.kindAt(firstRow, firstWave)),
                "读回后第一格的僵尸品种不对");
            check(reloaded.countAt(firstRow, firstWave) == firstCount,
                "读回后第一格的僵尸只数不对");
            check(lastKind.equals(reloaded.kindAt(lastRow, lastWave)),
                "读回后最后一格的僵尸品种不对");
            check(reloaded.countAt(lastRow, lastWave) == lastCount,
                "读回后最后一格的僵尸只数不对");

            // 游戏本身走的是 LevelLoader 这条路，它也必须认得编辑器写出的文件。
            Level level = new LevelLoader(assets).load(99);
            check(level.initialSun == design.initialSun, "游戏读到的初始阳光不对");
            check(level.skySunInterval == design.skySunInterval,
                "游戏没有读到编辑器设置的阳光生成速度");
            check(level.maxCards == design.maxCards, "游戏没有读到编辑器设置的卡槽数量");
            check(level.spawns.size() == totalCount, "游戏读到的出怪表长度不对");
        } finally {
            Files.deleteIfExists(temporary);
        }

        // 现成的关卡也要能读进编辑器继续改，而且一只僵尸都不能丢。
        // 这里不写死具体数字：关卡本来就是拿来改的，改过之后这项检查照样得成立，
        // 所以拿文件里实际有多少条出场记录来对。
        checkImportKeepsEveryZombie(assets, 1);

        checkEditorGrid(assets, project, design);
        checkGameCanBeStopped(assets);
    }

    /**
     * 检查把现成的关卡读进编辑器时，内容不会走样。
     *
     * 僵尸总数、初始阳光都拿关卡文件里写的做对照，不写死具体数字，
     * 这样用编辑器改过关卡之后，这项检查依然成立。
     *
     * 参数：assets 提供关卡位置；levelNumber 是要检查第几关。
     * 异常：读关卡文件失败时抛出异常。
     */
    private static void checkImportKeepsEveryZombie(Assets assets, int levelNumber)
            throws Exception {
        JsonObject json = Assets.readObject(assets.levelPath(levelNumber));
        int recorded = json.getAsJsonArray("zombie_list").size();
        int expectedSun = 0;
        if (json.has("init_sun_value")) {
            expectedSun = json.get("init_sun_value").getAsInt();
        }

        LevelDesign imported = LevelDesign.load(assets.levelPath(levelNumber));
        check(imported.initialSun == expectedSun,
            "导入第 " + levelNumber + " 关时初始阳光和文件里写的不一致");
        check(imported.totalZombies() == recorded,
            "导入第 " + levelNumber + " 关时僵尸总数和文件里的记录条数对不上");
        check(imported.waveCount() >= 1, "导入第 " + levelNumber + " 关时没有分出波次");

        // 关卡文件没写阳光速度时，应该退回默认值而不是留个 0。
        if (!json.has("sky_sun_interval")) {
            check(imported.skySunInterval == Layout.SKY_SUN_INTERVAL,
                "关卡没写阳光速度时应该用默认值");
        }
    }

    /**
     * 检查一局游戏能被真正停下来。
     *
     * 编辑器的"保存并试玩"每按一次就开一局。Swing 的计时器不跟着窗口收摊，
     * 要是关窗口时没人叫停，这一局会在看不见的地方一直跑，按几次就堆起好几局。
     *
     * 参数：assets 提供素材。
     */
    private static void checkGameCanBeStopped(Assets assets) {
        // 只看计时器停不停得下来，不载入关卡，所以传第几关都一样。
        Game game = new Game(assets, 1, true, false);
        check(game.isRunning(), "刚创建的一局应该是在跑的");
        game.stop();
        check(!game.isRunning(), "调用 stop 之后这一局还在跑，试玩窗口关掉后会白占处理器");
    }

    /**
     * 检查编辑器网格：鼠标点能算回正确的格子，格子内容的增删改符合预期。
     *
     * 这里只创建面板不开窗口，所以自检仍然可以在没有显示器的环境下跑完。
     *
     * 参数：assets 提供僵尸图标；project 是 assets 目录；design 是要摆弄的关卡数据。
     * 异常：写截图失败时抛出异常。
     */
    private static void checkEditorGrid(Assets assets, Path project, LevelDesign design)
            throws Exception {
        EditorGrid grid = new EditorGrid(design, new EditorIcons(assets), new StubDragController());

        // 网格左上角那一格应该是第 0 行第 0 波。
        int left = EditorGrid.ROW_HEADER_WIDTH;
        int top = EditorGrid.COLUMN_HEADER_HEIGHT;
        int[] first = grid.cellAt(new Point(left + 5, top + 5));
        check(first != null, "网格左上角那一格没算出坐标");
        check(first[0] == 0, "网格左上角那一格的行号不对");
        check(first[1] == 0, "网格左上角那一格的波号不对");

        // 往右挪两格、往下挪一格，应该落在第 1 行第 2 波。
        int middleX = left + 2 * EditorGrid.CELL_WIDTH + 5;
        int middleY = top + EditorGrid.CELL_HEIGHT + 5;
        int[] middle = grid.cellAt(new Point(middleX, middleY));
        check(middle != null, "网格中间那一格没算出坐标");
        check(middle[0] == 1, "网格中间那一格的行号不对");
        check(middle[1] == 2, "网格中间那一格的波号不对");

        // 表头和网格外面都不是格子，点上去不能误判。
        check(grid.cellAt(new Point(5, 5)) == null, "点在表头上不该算成格子");
        int outsideX = left + 99 * EditorGrid.CELL_WIDTH;
        check(grid.cellAt(new Point(outsideX, top + 5)) == null, "点在网格右边外面不该算成格子");

        // 拖动落格后走的就是这几个操作，逐个确认它们不会把内容弄丢。
        design.moveCell(0, 0, 2, 1);
        check(design.kindAt(0, 0) == null, "移动之后源格没有清空");
        check("Zombie".equals(design.kindAt(2, 1)), "移动之后目标格的僵尸品种不对");
        check(design.countAt(2, 1) == 2, "移动之后目标格的僵尸只数不对");
        design.swapCells(2, 1, 4, 2);
        check("BucketheadZombie".equals(design.kindAt(2, 1)), "交换之后第一格的内容不对");
        check("Zombie".equals(design.kindAt(4, 2)), "交换之后第二格的内容不对");
        design.addCount(4, 2, 3);
        check(design.countAt(4, 2) == 5, "增加倍数之后数量不对");
        design.addCount(4, 2, -99);
        check(design.kindAt(4, 2) == null, "倍数减到 0 之后格子没有清空");

        // 把网格和僵尸列表画到图片里，确认绘制这条路没有异常、也不是一片空白。
        renderComponent(grid, project.resolve("../build/editor-grid.png"));
        EditorPalette palette = new EditorPalette(new EditorIcons(assets), new StubDragController());
        renderComponent(palette, project.resolve("../build/editor-palette.png"));
    }

    /**
     * 把一个不依赖窗口的面板画到图片文件里，并检查画面不是全透明的。
     *
     * 参数：component 是要画的面板；output 是图片存到哪里。
     * 异常：写图片失败时抛出异常。
     */
    private static void renderComponent(JComponent component, Path output) throws Exception {
        Dimension size = component.getPreferredSize();
        component.setSize(size);
        component.doLayout();
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = image.createGraphics();
        component.paint(painter);
        painter.dispose();
        ImageIO.write(image, "png", output.toFile());
        check(hasOpaquePixel(image), "编辑器面板画出来是空的：" + output);
    }

    /**
     * 自检时顶替主窗口用的空壳控制器。
     *
     * 网格和僵尸列表都要有人接收拖动报告才能创建出来，
     * 但自检只验证画面和数据，不真的去拖，所以这里的方法全都留空。
     */
    private static class StubDragController implements EditorDragController {
        /** 自检不会真的拖动，收到开始的报告也不用做什么。 */
        public void beginDrag(String kind, int count, int fromRow, int fromWave,
            Point screenPoint) {
        }

        /** 自检不会真的拖动，收到移动的报告也不用做什么。 */
        public void updateDrag(Point screenPoint) {
        }

        /** 自检不会真的拖动，收到松手的报告也不用做什么。 */
        public void finishDrag(Point screenPoint) {
        }

        /** 自检时永远不在拖动中，这样网格会按平常的样子画。 */
        public boolean isDragging() {
            return false;
        }

        /** 固定当成选中了第一种僵尸。 */
        public String selectedKind() {
            return LevelDesign.ZOMBIE_KINDS[0];
        }

        /** 自检不关心选了哪种僵尸。 */
        public void selectKind(String kind) {
        }

        /** 自检里没有要刷新的界面。 */
        public void designChanged() {
        }

        /** 自检里没有要同步的复选框。 */
        public void selectionChanged() {
        }
    }

    /** 遇到缺失素材时立即说明具体是哪一项。 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
