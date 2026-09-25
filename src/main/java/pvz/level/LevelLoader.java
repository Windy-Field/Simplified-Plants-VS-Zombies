package pvz.level;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import pvz.game.GameState;
import pvz.plant.Cards;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.zombie.ZombieSpawn;

/**
 * 负责把关卡 JSON 文件读成 Level 对象。
 *
 * 读文件、认字段、排顺序这些事都放在这里，
 * 游戏主类就不必关心 JSON 长什么样了。
 */
public class LevelLoader {
    /** 提供关卡文件的位置。 */
    private final Assets assets;

    /**
     * 创建读取器。
     *
     * 参数：originalAssets 用来找到某个关卡的 JSON 文件。
     */
    public LevelLoader(Assets originalAssets) {
        assets = originalAssets;
    }

    /**
     * 读入指定关卡。
     *
     * 参数：levelNumber 是关卡编号（0 到 5）。
     * 返回：读好的关卡数据。
     * 异常：文件读不了或格式不对时抛出 IllegalStateException。
     */
    public Level load(int levelNumber) {
        try {
            Path path = assets.levelPath(levelNumber);
            JsonObject json = Assets.readObject(path);

            Level level = new Level();
            level.backgroundIndex = json.get("background_type").getAsInt();
            level.barType = GameState.BAR_NORMAL;
            if (json.has("choosebar_type")) {
                level.barType = json.get("choosebar_type").getAsInt();
            }
            level.initialSun = 0;
            if (json.has("init_sun_value")) {
                level.initialSun = json.get("init_sun_value").getAsInt();
            }
            // 关卡编辑器可以调快或调慢天空阳光。间隔太小会导致一帧掉一颗，这里兜底。
            if (json.has("sky_sun_interval")) {
                level.skySunInterval = Math.max(Layout.MIN_SKY_SUN_INTERVAL,
                    json.get("sky_sun_interval").getAsLong());
            }

            readSpawns(json, level);
            sortSpawnsByTime(level.spawns);
            readCardPool(json, level);
            readPlantList(json, "banned_plants", level.bannedPlants);
            readPlantList(json, "required_plants", level.requiredPlants);
            return level;
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取第 " + (levelNumber + 1) + " 关的关卡文件", exception);
        }
    }

    /** 把 JSON 里的僵尸出场表读进 level.spawns。 */
    private void readSpawns(JsonObject json, Level level) {
        JsonArray wave = json.getAsJsonArray("zombie_list");
        for (int index = 0; index < wave.size(); index++) {
            JsonElement item = wave.get(index);
            JsonObject entry = item.getAsJsonObject();
            int spawnTime = entry.get("time").getAsInt();
            int lane = entry.get("map_y").getAsInt();
            String name = entry.get("name").getAsString();
            level.spawns.add(new ZombieSpawn(spawnTime, lane, name));
        }
    }

    /**
     * 按出场时间给僵尸表排序。
     *
     * 原关卡文件通常已经排好了，但这里再排一次更保险，
     * 万一以后手改了 JSON 顺序也不会出问题。用的是插入排序，逻辑最直观。
     */
    private void sortSpawnsByTime(List<ZombieSpawn> spawns) {
        for (int index = 1; index < spawns.size(); index++) {
            ZombieSpawn current = spawns.get(index);
            int position = index - 1;
            while (position >= 0 && spawns.get(position).at > current.at) {
                spawns.set(position + 1, spawns.get(position));
                position = position - 1;
            }
            spawns.set(position + 1, current);
        }
    }

    /**
     * 读传送带或保龄球模式的可出卡列表。
     *
     * 正常选卡模式没有这个字段，所以先判断有没有再读。
     */
    private void readCardPool(JsonObject json, Level level) {
        if (level.isNormalMode()) {
            return;
        }
        JsonArray choices = json.getAsJsonArray("card_pool");
        for (int index = 0; index < choices.size(); index++) {
            JsonElement item = choices.get(index);
            JsonObject entry = item.getAsJsonObject();
            String name = entry.get("name").getAsString();
            int cardIndex = Cards.indexOf(name);
            if (cardIndex < 0) {
                throw new IllegalArgumentException("关卡里出现了未知的卡片：" + name);
            }
            level.cardPool.add(Integer.valueOf(cardIndex));
        }
    }

    /**
     * 读一份植物清单，比如禁用清单或必选清单。
     *
     * 没有这个字段就当清单是空的，老关卡文件照常能玩。
     * 编辑器写进去的植物名一定认得出，认不出就是文件被手改坏了，
     * 所以这里和卡池不一样：不跳过，直接报错，免得玩家莫名其妙少了一张卡。
     *
     * 参数：json 是关卡文件的根对象；field 是字段名；target 是读到哪个清单里。
     */
    private static void readPlantList(JsonObject json, String field, List<Integer> target) {
        if (!json.has(field)) {
            return;
        }
        JsonArray choices = json.getAsJsonArray(field);
        for (int index = 0; index < choices.size(); index++) {
            JsonElement item = choices.get(index);
            JsonObject entry = item.getAsJsonObject();
            String name = entry.get("name").getAsString();
            int plantIndex = Cards.indexOf(name);
            if (plantIndex < 0) {
                throw new IllegalArgumentException("关卡里的 " + field + " 出现了未知的植物：" + name);
            }
            target.add(Integer.valueOf(plantIndex));
        }
    }
}
