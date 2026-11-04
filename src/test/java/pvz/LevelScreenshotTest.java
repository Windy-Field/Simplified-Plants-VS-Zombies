package pvz;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import pvz.editor.LevelDesign;
import pvz.plant.PlantCatalog;
import pvz.world.Layout;
import pvz.game.LevelPreview;
import pvz.level.Level;
import pvz.level.LevelLoader;
import pvz.level.LevelScreenshot;
import pvz.world.Assets;
import pvz.zombie.ZombieSpawn;

/** 验证截图导入和关卡读写使用同一条路径，临时文件结束即清理，不改现有关卡 */
public class LevelScreenshotTest {
    /** 参数 assets 和 root 是共用素材；覆盖自定义截图、自动封面和图片缺失的完整流程 */
    public static void run(Assets assets, Path root) throws Exception {
        checkJsonRoundTrip();
        LevelLoader loader = new LevelLoader(assets);
        List<Integer> numbers = loader.levelNumbers();
        int last = numbers.get(numbers.size() - 1);
        int number = last + 1;
        Path levelFile = assets.levelPath(number);
        Path build = root.resolve("../build");
        Path source = Files.createTempFile(build, "screenshot-test-", ".png");
        Path invalid = Files.createTempFile(build, "invalid-screenshot-", ".png");
        Path imported = null;
        boolean saved = false;
        try {
            BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
            Graphics2D painter = image.createGraphics();
            painter.setColor(Color.MAGENTA);
            painter.fillRect(0, 0, 200, 100);
            painter.dispose();
            ImageIO.write(image, "png", source.toFile());

            String relative = LevelScreenshot.importImage(root, source);
            imported = root.resolve(relative);
            check(!Path.of(relative).isAbsolute(), "截图不应保存为绝对路径");
            check(Files.isRegularFile(imported), "截图没有导入素材目录");
            LevelDesign design = LevelDesign.load(assets.levelPath(0));
            design.screenshot = relative;
            check(!Files.exists(levelFile), "测试不能覆盖现有关卡");
            design.save(levelFile);
            saved = true;
            LevelDesign reloaded = LevelDesign.load(levelFile);
            check(reloaded.screenshot.equals(relative), "编辑器读回后截图路径丢失");
            Level level = loader.load(number);
            check(level.screenshot.equals(relative), "游戏没有读取截图字段");
            LevelPreview preview = new LevelPreview(assets);
            BufferedImage custom = preview.create(number);
            check(custom.getRGB(400, 300) == Color.MAGENTA.getRGB(), "选关封面未使用自定义截图");
            check(custom.getRGB(400, 0) != Color.MAGENTA.getRGB(), "宽截图被拉伸而没有等比例留边");

            // 删除原文件后仍能读取，证明关卡不依赖用户的外部截图位置
            Files.delete(source);
            check(LevelScreenshot.read(root, relative).getWidth() == 200, "导入后仍依赖外部原图");
            design.screenshot = "";
            design.save(levelFile);
            check(loader.load(number).screenshot.isEmpty(), "恢复自动后截图字段没有清空");
            BufferedImage automatic = preview.create(number);
            check(automatic.getRGB(400, 300) != Color.MAGENTA.getRGB(), "恢复自动后仍使用旧截图");
            design.screenshot = relative;
            design.save(levelFile);
            Files.delete(imported);
            BufferedImage fallback = preview.create(number);
            check(fallback.getRGB(400, 300) == automatic.getRGB(400, 300), "截图缺失时没有回退自动封面");

            boolean rejected = false;
            try {
                LevelScreenshot.importImage(root, invalid);
            } catch (java.io.IOException exception) {
                rejected = true;
            }
            check(rejected, "损坏图片未被拒绝");

            // JSON 库应负责转义路径中的特殊字符，避免另存时破坏配置
            design.screenshot = "levels/screenshots/quoted\"image.png";
            design.save(levelFile);
            reloaded = LevelDesign.load(levelFile);
            check(reloaded.screenshot.equals(design.screenshot), "截图路径没有正确转义");
        } finally {
            if (saved) {
                Files.deleteIfExists(levelFile);
            }
            if (imported != null) {
                Files.deleteIfExists(imported);
            }
            Files.deleteIfExists(source);
            Files.deleteIfExists(invalid);
        }
        System.out.println("PASS: Screenshot import, level round-trip, aspect ratio and fallback");
    }

    /** 检查 Gson 写入后卡池、限制和随机行仍能恢复，覆盖空数组和需要转义的截图路径 */
    private static void checkJsonRoundTrip() {
        LevelDesign design = new LevelDesign();
        design.barType = Layout.BAR_CONVEYOR;
        design.maxCards = 5;
        design.screenshot = "levels/screenshots/测试\\quoted\"image.png";
        int sunflower = PlantCatalog.indexOf("SunFlower");
        int peashooter = PlantCatalog.indexOf("Peashooter");
        design.cardPool.add(sunflower);
        design.cardPool.add(peashooter);
        design.bannedPlants.add(sunflower);
        design.requiredPlants.add(peashooter);
        design.setCell(2, 1, "Gargantuar", 3);
        design.setRandomRow(2, 1, true);
        String text = design.toJsonText();
        JsonElement parsed = JsonParser.parseString(text);
        JsonObject json = parsed.getAsJsonObject();
        LevelDesign reloaded = LevelDesign.fromJson(json);
        check(reloaded.cardPool.equals(design.cardPool), "保存后卡池内容或顺序变化");
        check(reloaded.bannedPlants.equals(design.bannedPlants), "保存后禁用植物丢失");
        check(reloaded.requiredPlants.equals(design.requiredPlants), "保存后必选植物丢失");
        check(reloaded.maxCards == design.maxCards, "保存后卡槽数量变化");
        check(reloaded.screenshot.equals(design.screenshot), "保存后截图特殊字符变化");
        check(reloaded.randomRowAt(2, 1), "保存后格子的随机行设置丢失");
        check(reloaded.countAt(2, 1) == 3, "保存后格子的僵尸数量变化");
        List<ZombieSpawn> spawns = reloaded.buildSpawns();
        check(spawns.get(0).row == ZombieSpawn.RANDOM_ROW,
            "随机行设置未传给游戏出场记录");
        design.clearAllCells();
        design.cardPool.clear();
        text = design.toJsonText();
        parsed = JsonParser.parseString(text);
        json = parsed.getAsJsonObject();
        reloaded = LevelDesign.fromJson(json);
        check(reloaded.totalZombies() == 0 && reloaded.cardPool.isEmpty(),
            "空出场表或空卡池不能保存读取");
    }

    /** 参数 condition 是测试结果，失败时报告 message，避免静默使用错误封面 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
