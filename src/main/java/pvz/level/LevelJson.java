package pvz.level;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import pvz.plant.PlantCatalog;
import pvz.world.Layout;
import pvz.zombie.ZombieSpawn;

/**
 * 关卡 JSON 的格式约定与公共解析。
 *
 * 游戏侧（LevelLoader）和关卡编辑器（LevelDesign）读写的是同一批文件。
 * 以前两边各写一份解析代码，于是同一个字段有了两处默认值、两套容错策略，
 * 还各抄了一份一模一样的排序。结果出现过"游戏读不进去、编辑器却打得开"——
 * 同一份文件，两边对它的理解不一样。
 *
 * 现在字段名、默认值、容错规则都收在这里，两边共用：改格式只改一处，
 * 读出来的结果也就只有一种。
 *
 * 这个类只认识 JSON 和几个数据对象，不认识 Level，也不认识 LevelDesign，
 * 所以游戏和编辑器都能放心依赖它，不会绕出循环依赖。
 */
public final class LevelJson {
    /** 用第几张背景图。 */
    public static final String BACKGROUND_TYPE = "background_type";

    /** 卡槽模式，取值见 Layout 里的 BAR 系列常量；不写就按正常选卡处理。 */
    public static final String CHOOSEBAR_TYPE = "choosebar_type";

    /** 开局赠送的阳光；不写就是 0。 */
    public static final String INIT_SUN_VALUE = "init_sun_value";

    /** 天空掉阳光的间隔；不写就用 Layout.SKY_SUN_INTERVAL。 */
    public static final String SKY_SUN_INTERVAL = "sky_sun_interval";

    /** 僵尸出场流水账，游戏和编辑器都靠它。 */
    public static final String ZOMBIE_LIST = "zombie_list";

    /** 传送带和保龄球模式能出的卡片；正常选卡关卡没有这一项。 */
    public static final String CARD_POOL = "card_pool";

    /** 本关禁用的植物。 */
    public static final String BANNED_PLANTS = "banned_plants";

    /** 本关必选的植物。 */
    public static final String REQUIRED_PLANTS = "required_plants";

    /** 本关的卡槽数量；不写就用 Layout.DEFAULT_CARD_SLOTS。 */
    public static final String MAX_CARDS = "max_cards";

    /**
     * 编辑器自己存的一段表格数据。
     *
     * 游戏不认识这一段，读到会直接忽略；编辑器靠它把关卡原样还原成表格。
     */
    public static final String EDITOR = "editor";

    /** 出场记录里的出场时刻，单位毫秒。 */
    public static final String TIME = "time";

    /** 出场记录里的行号；-1 表示随机行，见 ZombieSpawn.RANDOM_ROW。 */
    public static final String MAP_Y = "map_y";

    /** 出场记录和植物清单里通用的名字字段。 */
    public static final String NAME = "name";

    /** 这个类只提供约定和静态解析方法，不允许创建对象。 */
    private LevelJson() {
    }

    /**
     * 把 zombie_list 数组读成出场表。
     *
     * 参数：list 是关卡文件里的 zombie_list 数组。
     * 返回：还没排序的出场表。
     */
    public static List<ZombieSpawn> readSpawns(JsonArray list) {
        List<ZombieSpawn> spawns = new ArrayList<ZombieSpawn>();
        for (int index = 0; index < list.size(); index++) {
            JsonObject entry = list.get(index).getAsJsonObject();
            spawns.add(new ZombieSpawn(
                entry.get(TIME).getAsInt(),
                entry.get(MAP_Y).getAsInt(),
                entry.get(NAME).getAsString()));
        }
        return spawns;
    }

    /**
     * 按出场时间给出场表排序，用的是最直观的插入排序。
     *
     * 原关卡文件通常已经排好了，这里再排一次更保险：游戏按出场表的下标顺序推进出怪
     * （GameState.nextSpawnIndex 只看下一条、从不回头），万一以后手改了 JSON 顺序，
     * 排好序才不会漏掉晚出场却排在前面那条。
     *
     * 参数：spawns 是待排序的出场表，排完直接改在原列表上。
     */
    public static void sortByTime(List<ZombieSpawn> spawns) {
        for (int index = 1; index < spawns.size(); index++) {
            ZombieSpawn current = spawns.get(index);
            int position = index - 1;
            while (position >= 0 && spawns.get(position).spawnTime > current.spawnTime) {
                spawns.set(position + 1, spawns.get(position));
                position = position - 1;
            }
            spawns.set(position + 1, current);
        }
    }

    /**
     * 读天空掉阳光的间隔。
     *
     * 关卡编辑器可以调快或调慢它，但间隔太小会变成几乎每帧掉一颗，
     * 所以统一兜到 Layout.MIN_SKY_SUN_INTERVAL 这个下限。
     *
     * 参数：json 是关卡文件的根对象。
     * 返回：夹好之后的间隔毫秒数。
     */
    public static long readSkySunInterval(JsonObject json) {
        if (!json.has(SKY_SUN_INTERVAL)) {
            return Layout.SKY_SUN_INTERVAL;
        }
        return Math.max(Layout.MIN_SKY_SUN_INTERVAL,
            json.get(SKY_SUN_INTERVAL).getAsLong());
    }

    /**
     * 读本关的卡槽数量。
     *
     * 老关卡文件没有这一项，那就保持默认的 8 张，和原版一样。
     * 文件里的数字可能被手改到离谱的值，所以夹回游戏允许的范围再收下。
     *
     * 参数：json 是关卡文件的根对象。
     * 返回：夹好之后的卡槽数量。
     */
    public static int readCardSlots(JsonObject json) {
        if (!json.has(MAX_CARDS)) {
            return Layout.DEFAULT_CARD_SLOTS;
        }
        return Layout.clampCardSlots(json.get(MAX_CARDS).getAsInt());
    }

    /**
     * 读一份植物清单，认不出的植物名按错误处理。
     *
     * 游戏侧用这个。清单都是编辑器写进去的，里面的名字本来就该认得出来；
     * 真读不出来说明文件被改坏了，这时候宁可当场报错，也不能悄悄少一张卡——
     * 否则玩家会遇到"必选植物没进卡槽、卡槽数凑不满、这一关根本开不了局"，
     * 而画面上什么提示都没有。
     *
     * 参数：json 是关卡文件的根对象；field 是字段名。
     * 返回：植物编号清单；文件里没有这一项时返回空清单。
     */
    public static List<Integer> readPlantList(JsonObject json, String field) {
        List<Integer> plants = new ArrayList<Integer>();
        readPlantListInto(json, field, plants, null);
        return plants;
    }

    /**
     * 读一份植物清单，认不出的植物名跳过、不报错。
     *
     * 编辑器用这个。它得能打开任何文件，包括别的版本写出来的、带了新植物的关卡，
     * 好让用户看到问题再自己把那项删掉。但跳过不能一声不吭：
     * 认不出的名字会记进 unknownNames，由编辑器拼成提示给用户看。
     *
     * 参数：json 是关卡文件的根对象；field 是字段名；
     *       unknownNames 是收集认不出名字的清单，直接改在原清单上。
     * 返回：植物编号清单；文件里没有这一项时返回空清单。
     */
    public static List<Integer> readPlantListSkippingUnknown(JsonObject json,
            String field, List<String> unknownNames) {
        List<Integer> plants = new ArrayList<Integer>();
        readPlantListInto(json, field, plants, unknownNames);
        return plants;
    }

    /**
     * 读一份植物清单的公共实现。
     *
     * 卡池、禁用清单、必选清单的格式完全一样，都是"一行一种植物"，
     * 所以共用这一份；三者的区别只在认不出植物名时怎么办。
     *
     * 参数：json 是关卡文件的根对象；field 是字段名；target 是读到哪个清单里；
     *       unknownNames 不为 null 时跳过认不出的名字并记下来，为 null 时直接报错。
     */
    private static void readPlantListInto(JsonObject json, String field,
            List<Integer> target, List<String> unknownNames) {
        if (!json.has(field)) {
            return;
        }
        JsonArray choices = json.getAsJsonArray(field);
        for (int index = 0; index < choices.size(); index++) {
            JsonObject entry = choices.get(index).getAsJsonObject();
            String name = entry.get(NAME).getAsString();
            int plantIndex = PlantCatalog.indexOf(name);
            if (plantIndex >= 0) {
                target.add(Integer.valueOf(plantIndex));
                continue;
            }
            if (unknownNames == null) {
                throw new IllegalArgumentException(
                    "关卡里的 " + field + " 出现了未知的植物：" + name);
            }
            // 同一个名字只记一次：用户看到"少了三种植物"，比看到同一个名字重复十遍有用。
            if (!unknownNames.contains(name)) {
                unknownNames.add(name);
            }
        }
    }
}
