package pvz.level;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import pvz.world.Assets;
import pvz.world.Layout;

/**
 * 负责把关卡 JSON 文件读成 Level 对象
 *
 * 读文件、认字段、排顺序这些事都放在这里，
 * 游戏主类就不必关心 JSON 长什么样了
 *
 * 字段名、默认值和容错规则本身不在这里，而在 {@link LevelJson}：
 * 那份约定是游戏和编辑器共用的，改格式只要改那一个地方
 * 这个类只管一件事——游戏要的 Level 对象长什么样
 */
public class LevelLoader {
    /** 提供关卡文件的位置 */
    private final Assets assets;

    /**
     * 创建读取器
     *
     * 参数：assets 用来找到某个关卡的 JSON 文件
     */
    public LevelLoader(Assets assets) {
        this.assets = assets;
    }

    /** 返回目录里实际存在的关卡编号并排序，允许编辑器保存的关卡编号中间有空缺 */
    public List<Integer> levelNumbers() {
        List<Integer> numbers = new ArrayList<Integer>();
        Path firstLevel = assets.levelPath(0);
        Path directory = firstLevel.getParent();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "level_*.json")) {
            for (Path file : files) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                Path fileName = file.getFileName();
                String name = fileName.toString();
                String numberText = name.substring(6, name.length() - 5);
                if (!numberText.matches("0|[1-9][0-9]*")) {
                    continue;
                }
                try {
                    numbers.add(Integer.parseInt(numberText));
                } catch (NumberFormatException exception) {
                    // 超过整数范围的编号不是游戏支持的关卡，忽略而不是阻止其他关卡显示
                    continue;
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取关卡目录：" + directory, exception);
        }
        Collections.sort(numbers);
        return numbers;
    }

    /**
     * 读入指定关卡
     *
     * 参数：levelNumber 是关卡编号，从 0 开始
     * 返回：读好的关卡数据
     * 异常：文件读不了、或缺少 zombie_list 字段时抛 IllegalStateException；
     *       其余字段缺失或格式非法时不做包装，按 Gson 与解析逻辑各自抛出
     *       （例如缺 background_type 时的 NPE、JSON 语法错时的 JsonSyntaxException、
     *       植物名认不出时的 IllegalArgumentException）
     */
    public Level load(int levelNumber) {
        try {
            Path path = assets.levelPath(levelNumber);
            JsonObject json = Assets.readObject(path);

            Level level = new Level();
            level.screenshot = LevelJson.readScreenshot(json);
            level.backgroundIndex = json.get(LevelJson.BACKGROUND_TYPE).getAsInt();
            level.barType = Layout.BAR_NORMAL;
            if (json.has(LevelJson.CHOOSEBAR_TYPE)) {
                level.barType = json.get(LevelJson.CHOOSEBAR_TYPE).getAsInt();
            }
            level.initialSun = 0;
            if (json.has(LevelJson.INIT_SUN_VALUE)) {
                level.initialSun = json.get(LevelJson.INIT_SUN_VALUE).getAsInt();
            }
            level.skySunInterval = LevelJson.readSkySunInterval(json);
            level.maxCards = LevelJson.readCardSlots(json);

            // 出场表字段是关卡格式的必需项，缺失时明确报错；空数组仍允许读取
            if (!json.has(LevelJson.ZOMBIE_LIST)) {
                throw new IllegalStateException("第 " + (levelNumber + 1)
                    + " 关的关卡文件里没有 " + LevelJson.ZOMBIE_LIST + " 字段");
            }
            level.spawns.addAll(
                LevelJson.readSpawns(json.getAsJsonArray(LevelJson.ZOMBIE_LIST)));
            LevelJson.sortByTime(level.spawns);

            // 卡池只有传送带和保龄球关卡才有；正常选卡关卡没有这一项，
            // 读出来自然是空的，所以不必再按卡槽模式判断一次
            level.cardPool.addAll(LevelJson.readPlantList(json, LevelJson.CARD_POOL));
            level.bannedPlants.addAll(
                LevelJson.readPlantList(json, LevelJson.BANNED_PLANTS));
            level.requiredPlants.addAll(
                LevelJson.readPlantList(json, LevelJson.REQUIRED_PLANTS));
            return level;
        } catch (IOException exception) {
            throw new IllegalStateException(
                "无法读取第 " + (levelNumber + 1) + " 关的关卡文件", exception);
        }
    }
}
