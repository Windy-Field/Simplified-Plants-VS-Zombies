package pvz.level;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import pvz.world.Assets;
import pvz.world.Layout;

/**
 * 负责把关卡 JSON 文件读成 Level 对象。
 *
 * 读文件、认字段、排顺序这些事都放在这里，
 * 游戏主类就不必关心 JSON 长什么样了。
 *
 * 字段名、默认值和容错规则本身不在这里，而在 {@link LevelJson}：
 * 那份约定是游戏和编辑器共用的，改格式只要改那一个地方。
 * 这个类只管一件事——游戏要的 Level 对象长什么样。
 */
public class LevelLoader {
    /** 提供关卡文件的位置。 */
    private final Assets assets;

    /**
     * 创建读取器。
     *
     * 参数：assets 用来找到某个关卡的 JSON 文件。
     */
    public LevelLoader(Assets assets) {
        this.assets = assets;
    }

    /**
     * 读入指定关卡。
     *
     * 参数：levelNumber 是关卡编号，从 0 开始。
     * 返回：读好的关卡数据。
     * 异常：文件读不了、或缺少 zombie_list 字段时抛 IllegalStateException；
     *       其余字段缺失或格式非法时不做包装，按 Gson 与解析逻辑各自抛出
     *       （例如缺 background_type 时的 NPE、JSON 语法错时的 JsonSyntaxException、
     *       植物名认不出时的 IllegalArgumentException）。
     */
    public Level load(int levelNumber) {
        try {
            Path path = assets.levelPath(levelNumber);
            JsonObject json = Assets.readObject(path);

            Level level = new Level();
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

            // 出场表是这一关的骨架。一条都没有的话，这关会在开场演出结束后立刻判定通关，
            // 玩家看到的是一个刚亮起来就结束的关卡。与其这样，不如在这里把话说明白。
            if (!json.has(LevelJson.ZOMBIE_LIST)) {
                throw new IllegalStateException("第 " + (levelNumber + 1)
                    + " 关的关卡文件里没有 " + LevelJson.ZOMBIE_LIST + " 字段");
            }
            level.spawns.addAll(
                LevelJson.readSpawns(json.getAsJsonArray(LevelJson.ZOMBIE_LIST)));
            LevelJson.sortByTime(level.spawns);

            // 卡池只有传送带和保龄球关卡才有；正常选卡关卡没有这一项，
            // 读出来自然是空的，所以不必再按卡槽模式判断一次。
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
