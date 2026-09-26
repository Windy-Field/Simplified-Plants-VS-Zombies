package pvz;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import pvz.editor.EditorDragController;
import pvz.editor.EditorGrid;
import pvz.editor.EditorIcons;
import pvz.editor.EditorPalette;
import pvz.editor.LevelDesign;
import pvz.game.Game;
import pvz.game.GameState;
import pvz.level.Level;
import pvz.level.LevelLoader;
import pvz.plant.Cards;
import pvz.plant.PlantCatalog;
import pvz.plant.PlantDefinition;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.CombatValues;
import pvz.world.Layout;
import pvz.world.Sun;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieAbility;
import pvz.zombie.ZombieCatalog;
import pvz.zombie.ZombieDefinition;
import pvz.zombie.ZombieSpawn;

/**
 * 自检程序：确认 assets 目录里的图片和关卡数据都能被游戏正确读取和使用。
 *
 * 它不打开窗口，而是把画面画到内存里的图片上，然后逐项检查：素材有没有缺、关卡数据对不对、增改植物有无遗漏、选卡和种植能不能走通。
 * 全部通过时打印一行 PASS，任意一项不合格就抛异常并说明是哪一项。
 */
public class SelfCheckTest {
    /** 需要检查的界面图片名单。 */
    private static final String[] SCREEN_IMAGES = {
        "MainMenu", "Adventure_0", "Adventure_1", "ChooserBackground",
        "MoveBackground", "PanelBackground", "StartButton", "GameVictory", "GameLoose", "car", "Boom"
    };

    /** 关卡里会用到的动画名单。 */
    // TODO：【必做-7】新增植物时需要把植物的所有动画状态名加到这个数组里，否则自检不会检查它
    // TODO：【必做-10】新增僵尸时需要把僵尸的所有动画状态名加到这个数组里，否则自检不会检查它
    private static final String[] ANIMATIONS = {
        "Sun", "SunFlower", "Peashooter", "SnowPea", "WallNut", "WallNut_cracked1",
        "WallNut_cracked2", "CherryBomb", "CherryBombExplode", "Threepeater", "RepeaterPea", "Chomper", "ChomperAttack",
        "ChomperDigest", "PuffShroom", "PuffShroomSleep", "PotatoMineInit", "PotatoMine",
        "PotatoMineExplode", "Squash", "SquashAttack", "Spikeweed", "Jalapeno", "JalapenoExplode",
        "ScaredyShroom", "ScaredyShroomCry", "ScaredyShroomSleep", "SunShroom", "SunShroomBig",
        "SunShroomSleep", "IceShroom", "IceShroomSnow", "IceShroomSleep", "IceShroomTrap",
        "HypnoShroom", "HypnoShroomSleep", "WallNutBowling", "RedWallNutBowling",
        "RedWallNutBowlingExplode", "PeaNormal", "PeaIce", "BulletMushRoom", "PeaNormalExplode",
        "BulletMushRoomExplode", "Zombie", "ZombieAttack", "ZombieLostHead", "ZombieLostHeadAttack",
        "ZombieDie", "ZombieHead", "BoomDie", "ConeheadZombie", "ConeheadZombieAttack", "BucketheadZombie",
        "BucketheadZombieAttack", "ZombieNoArm", "ZombieNoArmAttack", "ZombieNoArmDie",
        "ZombieNoArmLostHead", "ZombieNoArmLostHeadAttack", "FlagZombie", "FlagZombieAttack", "FlagZombieLostHead",
        "FlagZombieLostHeadAttack", "NewspaperZombie", "NewspaperZombieAttack", "NewspaperZombieNoPaper",
        "NewspaperZombieNoPaperAttack", "NewspaperZombieLostHead", "NewspaperZombieLostHeadAttack",
        "NewspaperZombieDie", "NewspaperZombieHead", "NewspaperZombieBoomDie",
        "JokerZombie", "JokerZombieExplode", "JokerBoom"
    };

    /** 传送带模式专用卡片的图片名。 */
    private static final String[] CONVEYOR_CARDS = {
        "card_peashooter_move", "card_snowpea_move", "card_wallnut_move",
        "card_cherrybomb_move", "card_repeaterpea_move", "card_chomper_move", "card_potatomine_move",
        "card_redwallnut_move"
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
        checkCards(assets);
        checkAnimations(assets);
        checkLevelData(assets);
        checkLevelEditor(assets, project);

        // 渲染各模式的静态画面，核对菜单和背景没有空白或裁错。
        renderMenu(assets, project);
        renderGallery(assets, project);
        // 关卡数量从 assets/levels 目录实际有哪些文件数出来，以后加第 7 关不用改自检。
        int levelCount = countLevels(assets);
        for (int level = 0; level < levelCount; level++) {
            renderLevel(assets, project, level);
        }
        checkInteraction(assets, project);

        System.out.println("PASS：" + levelCount + " 个关卡、菜单、背景、卡片、全部动画和关卡编辑器检查通过");
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
        for (int index = 0; index < 5; index++) {
            BufferedImage frame = assets.frame("Background", index);
            check(frame.getWidth() >= 800, "缺少背景图：" + index);
        }
    }

    /**
     * 检查植物资料表和卡片图。
     *
     * 加新植物时最容易出的错是资料、动画或卡片图没有对齐，这里都会拦下来。
     */
    private static void checkCards(Assets assets) {
        int plantCount = PlantCatalog.DEFINITIONS.length;

        for (int index = 0; index < plantCount; index++) {
            PlantDefinition definition = PlantCatalog.definitionAt(index);
            String plantName = definition.name;
            check(assets.hasAnimation(plantName), "植物没有在 Assets.loadPlants 里登记动画：" + plantName);
            BufferedImage image = assets.image(definition.cardPicture);
            check(image.getHeight() > 0, "缺少卡片：" + definition.cardPicture);
        }
        for (int index = 0; index < CONVEYOR_CARDS.length; index++) {
            String name = CONVEYOR_CARDS[index];
            BufferedImage image = assets.image(name);
            check(image.getWidth() > 0, "缺少传送带卡片：" + name);
        }
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
            check(mode >= GameState.BAR_NORMAL && mode <= GameState.BAR_BOWLING,
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
            check(Cards.indexOf(name) >= 0, "卡池里有不认识的卡：" + name);
        }
    }

    /** 模拟一遍真实操作：选八张卡、种植、等僵尸出场、传送带出卡。 */
    private static void checkInteraction(Assets assets, Path project) throws Exception {
        checkSpeedChoices();
        checkBulletSpeed(assets);
        checkFixedStepMovement(assets);
        checkNormalLevel(assets, project);
        checkConveyorLevel(assets, project);
        checkBowlingLevel(assets, project);
    }
    
    /**
     * 检查游戏速度选项正好是新的 1 倍、1.5 倍和 2 倍。
     */
    private static void checkSpeedChoices() {
        check(Layout.SPEED_MULTIPLIERS.length == 3, "游戏速度选项数量不对");
        check(Layout.SPEED_MULTIPLIERS[0] == 2, "新的 1 倍速没有使用原来的 2 倍速");
        check(Layout.SPEED_MULTIPLIERS[1] == 3, "新的 1.5 倍速内部倍率不对");
        check(Layout.SPEED_MULTIPLIERS[2] == 4, "新的 2 倍速内部倍率不对");
        check("1x".equals(Layout.speedLabelFor(2)), "新的 1 倍速显示文字不对");
        check("1.5x".equals(Layout.speedLabelFor(3)), "新的 1.5 倍速显示文字不对");
        check("2x".equals(Layout.speedLabelFor(4)), "新的 2 倍速显示文字不对");
    }

    /**
     * 检查每个固定战斗小步只让子弹移动基础距离。
     *
     * 参数：assets 提供子弹图片。
     * 异常：子弹素材缺失时抛出异常。
     */
    private static void checkBulletSpeed(Assets assets) {
        Bullet normalBullet = new Bullet("PeaNormal", 100, 120, 0, 120, assets);
        normalBullet.update(1000);
        double distance = normalBullet.x - 100;
        check(distance == Layout.BULLET_SPEED, "固定战斗小步中的子弹位移不正确");
    }

    /**
     * 检查一次大步推进和多次小步推进的僵尸结果一致。
     *
     * 这能防止高倍速时因为一帧只处理一次移动，导致不同倍速产生不同规则结果。
     * 参数：assets 提供游戏素材。
     * 异常：关卡读取失败时抛出异常。
     */
    private static void checkFixedStepMovement(Assets assets) throws Exception {
        int level = findLevelWithBar(assets, GameState.BAR_NORMAL);
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
        Game game = new Game(assets, level, false);
        game.loadLevel();

        for (int index = 0; index < PlantCatalog.CHOOSER_COUNT; index++) {
            int column = index % 8;
            int row = index / 8;
            int left = 25 + column * 53;
            int top = 135 + row * 74;
            game.click(left, top);
        }
        game.click(160, 550);
        return game;
    }

    /** 正常关卡：选满八张卡之后种一株，再推进一秒看僵尸有没有出场。 */
    private static void checkNormalLevel(Assets assets, Path project) throws Exception {
        int level = findLevelWithBar(assets, GameState.BAR_NORMAL);
        if (level < 0) {
            System.out.println("跳过选卡流程检查：现在没有哪一关是正常选卡模式");
            return;
        }

        Game normal = new Game(assets, level, false);
        normal.loadLevel();

        for (int column = 0; column < 8; column++) {
            normal.click(25 + column * 53, 135);
        }
        normal.click(160, 550);
        normal.click(90, 12);
        normal.click(75, 160);
        check(normal.getPlantCount() == 1, "选卡或种植失败，关卡 " + level);

        // 推到这一关最早那只僵尸该出场之后，再看它到了没有。
        // 写死"过 1 秒就该有僵尸"的话，把出怪时间往后挪一点自检就会报错。
        normal.step(firstSpawnTime(assets, level) + 500);
        check(normal.getZombieCount() >= 1, "僵尸没有按时出场，关卡 " + level);
        render(normal, project.resolve("../build/level-" + level + "-play.png"));
    }

    /** 传送带关卡：推进一秒应该自动出一张卡。 */
    private static void checkConveyorLevel(Assets assets, Path project) throws Exception {
        int level = findLevelWithBar(assets, GameState.BAR_CONVEYOR);
        if (level < 0) {
            System.out.println("跳过传送带检查：现在没有哪一关是传送带模式");
            return;
        }

        Game conveyor = new Game(assets, level, false);
        conveyor.loadLevel();
        conveyor.step(1000);
        check(conveyor.getCardCount() >= 1, "传送带没有出卡，关卡 " + level);
        render(conveyor, project.resolve("../build/level-" + level + "-play.png"));
    }

    /** 保龄球关卡：出一张卡，点在草坪上应该能种下球。 */
    private static void checkBowlingLevel(Assets assets, Path project) throws Exception {
        int level = findLevelWithBar(assets, GameState.BAR_BOWLING);
        if (level < 0) {
            System.out.println("跳过保龄球检查：现在没有哪一关是保龄球模式");
            return;
        }

        Game bowling = new Game(assets, level, false);
        bowling.loadLevel();
        bowling.step(1000);
        check(bowling.getCardCount() >= 1, "保龄球关卡没有出卡，关卡 " + level);

        bowling.click(615, 25);
        bowling.click(75, 160);
        check(bowling.getPlantCount() == 1, "保龄球没能种下，关卡 " + level);
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
        Game game = new Game(assets, 1, false);
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
        int expected = GameState.BAR_NORMAL;
        if (map.has("choosebar_type")) {
            expected = map.get("choosebar_type").getAsInt();
        }

        Game game = new Game(assets, level, false);
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
            int mode = GameState.BAR_NORMAL;
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
        design.setCell(0, 0, "Zombie", 2);
        design.setCell(4, 2, "BucketheadZombie", 3);

        // 网格上的"倍数"要展开成若干条出场记录，同一格里的僵尸按间隔错开。
        List<ZombieSpawn> spawns = design.buildSpawns();
        check(spawns.size() == 5, "网格摊平后的僵尸总数不对");
        check(spawns.get(0).spawnTime == 10000, "第一波的出场时间不对");
        check(spawns.get(0).row == 0, "第一波的行号不对");
        check(spawns.get(1).spawnTime == 10500, "同一格里的第二只僵尸没有按间隔错开");
        check(spawns.get(4).spawnTime == 41000, "最后一波的时间不对");
        check(spawns.get(4).name.equals("BucketheadZombie"), "最后一波的僵尸品种不对");

        // 存盘再读回，网格和参数都要原样还原。
        // 借 99 号当临时关卡，用完就删；万一这个编号已经被占用，先停下来提醒，免得覆盖别人的文件。
        Path temporary = project.resolve("levels/level_99.json");
        check(!Files.exists(temporary), "自检需要用 level_99.json 当临时文件，请先把已有的同名文件挪走");
        try {
            design.save(temporary);
            LevelDesign reloaded = LevelDesign.load(temporary);
            check(reloaded.initialSun == 125, "读回后初始阳光不对");
            check(reloaded.skySunInterval == 3500, "读回后阳光生成速度不对");
            check(reloaded.waveInterval == 15000, "读回后出怪间隔不对");
            check(reloaded.waveCount() == 3, "读回后波数不对");
            check(reloaded.maxCards == 5, "读回后卡槽数量不对");
            check(reloaded.totalZombies() == 5, "读回后僵尸总数不对");
            check("Zombie".equals(reloaded.kindAt(0, 0)), "读回后第一格的僵尸品种不对");
            check(reloaded.countAt(0, 0) == 2, "读回后第一格的僵尸只数不对");
            check("BucketheadZombie".equals(reloaded.kindAt(4, 2)), "读回后最后一格的僵尸品种不对");
            check(reloaded.countAt(4, 2) == 3, "读回后最后一格的僵尸只数不对");

            // 游戏本身走的是 LevelLoader 这条路，它也必须认得编辑器写出的文件。
            Level level = new LevelLoader(assets).load(99);
            check(level.initialSun == 125, "游戏读到的初始阳光不对");
            check(level.skySunInterval == 3500, "游戏没有读到编辑器设置的阳光生成速度");
            check(level.maxCards == 5, "游戏没有读到编辑器设置的卡槽数量");
            check(level.spawns.size() == 5, "游戏读到的出怪表长度不对");
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
        Game game = new Game(assets, 1);
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
