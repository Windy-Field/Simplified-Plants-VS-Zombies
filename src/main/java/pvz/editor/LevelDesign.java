package pvz.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import pvz.game.GameState;
import pvz.plant.Cards;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieSpawn;

/**
 * 关卡编辑器眼里的一个关卡。
 *
 * 关卡文件里的僵尸是一条条"第几毫秒、第几行、什么僵尸"的流水账，
 * 人看不出波次，也不好改。这个类把它换成一张表格：
 * 横向是第几波，纵向是草坪第几行，一个格子记着"这一波这一行出哪种僵尸、出几只"。
 * 编辑时改表格，存盘时再把表格摊平回流水账。
 */
public class LevelDesign {
    /** 编辑器能放置的僵尸品种，必须都是游戏真正实现了的。 */
    public static final String[] ZOMBIE_KINDS = {
        "Zombie", "ConeheadZombie", "BucketheadZombie", "FlagZombie", "NewspaperZombie"
    };

    /** 和 ZOMBIE_KINDS 一一对应的中文名，只用来显示。 */
    public static final String[] ZOMBIE_LABELS = {
        "普通僵尸", "路障僵尸", "铁桶僵尸", "旗帜僵尸", "读报僵尸"
    };

    /** 背景选项，下标就是关卡文件里的 background_type。 */
    public static final String[] BACKGROUND_LABELS = {
        "0 - 白天草坪", "1 - 夜晚草坪", "2 - 白天泳池", "3 - 夜晚泳池", "4 - 保龄球草坪"
    };

    /** 卡槽模式选项，下标就是关卡文件里的 choosebar_type。 */
    public static final String[] BAR_LABELS = {
        "0 - 正常选卡", "1 - 传送带", "2 - 坚果保龄球"
    };

    /** 最少要有一波，否则表格就空了。 */
    public static final int MIN_WAVE_COUNT = 1;

    /** 最多支持这么多波，再多表格就宽得没法看了。 */
    public static final int MAX_WAVE_COUNT = 40;

    /** 一个格子里最多放这么多只僵尸。 */
    public static final int MAX_CELL_COUNT = 20;

    /** 两波之间的最短间隔。 */
    public static final long MIN_WAVE_INTERVAL = 1000;

    /** 同一格里两只僵尸之间的最短间隔。 */
    public static final long MIN_SPAWN_SPACING = 50;

    /** 新建关卡时，第一波默认等 20 秒再出。 */
    public static final long DEFAULT_FIRST_WAVE_DELAY = 20000;

    /** 新建关卡时，两波之间默认隔 20 秒。 */
    public static final long DEFAULT_WAVE_INTERVAL = 20000;

    /** 新建关卡时，同一格里的僵尸默认错开 0.6 秒。 */
    public static final long DEFAULT_SPAWN_SPACING = 600;

    /** 新建关卡时默认给 8 波。 */
    public static final int DEFAULT_WAVE_COUNT = 8;

    /**
     * 导入老关卡时，两只僵尸相隔超过这个时间就算换了一波。
     *
     * 原版关卡里同一波的僵尸都挤在几秒内出场，波与波之间往往隔十几秒，
     * 拿 5 秒当分界线能把绝大多数关卡分对。
     */
    private static final long IMPORT_WAVE_GAP = 5000;

    /** 用第几张背景图。 */
    public int backgroundIndex;

    /** 卡槽模式：正常选卡、传送带还是坚果保龄球。 */
    public int barType = GameState.BAR_NORMAL;

    /** 开局赠送的阳光。 */
    public int initialSun = 50;

    /** 天空每隔多少毫秒掉一颗阳光，也就是阳光自然生成的速度。 */
    public long skySunInterval = Layout.SKY_SUN_INTERVAL;

    /** 进入关卡后等多少毫秒出第一波。 */
    public long firstWaveDelay = DEFAULT_FIRST_WAVE_DELAY;

    /** 相邻两波之间隔多少毫秒，也就是出怪速度。 */
    public long waveInterval = DEFAULT_WAVE_INTERVAL;

    /** 同一个格子里的僵尸之间错开多少毫秒，免得几只叠在一起出来。 */
    public long spawnSpacing = DEFAULT_SPAWN_SPACING;

    /** 传送带和保龄球模式可以出的卡片编号，正常选卡模式用不到。 */
    public final List<Integer> cardPool = new ArrayList<Integer>();

    /** 本关禁止使用的植物编号；只对正常选卡模式生效。 */
    public final List<Integer> bannedPlants = new ArrayList<Integer>();

    /** 本关强制携带、玩家不能取消的植物编号；只对正常选卡模式生效。 */
    public final List<Integer> requiredPlants = new ArrayList<Integer>();

    /** 导入老关卡时如果做了近似处理，把说明记在这里给用户看。 */
    public String importNote = "";

    /** 表格里每个格子放的僵尸品种，没放就是 null。第一维是行，第二维是波。 */
    private String[][] kinds = new String[Layout.ROW_COUNT][DEFAULT_WAVE_COUNT];

    /** 表格里每个格子放了几只僵尸，和 kinds 一一对应。 */
    private int[][] counts = new int[Layout.ROW_COUNT][DEFAULT_WAVE_COUNT];

    /** 每个格子是否随机行出怪，和 kinds 一一对应。 */
    private boolean[][] randomRows = new boolean[Layout.ROW_COUNT][DEFAULT_WAVE_COUNT];

    /** 当前一共有几波，也就是表格有几列。 */
    private int waveCount = DEFAULT_WAVE_COUNT;

    /**
     * 查询当前有几波。
     *
     * 返回：波数。
     */
    public int waveCount() {
        return waveCount;
    }

    /**
     * 改变波数，也就是给表格加列或减列。
     *
     * 减少波数会把后面几波的内容一起丢掉，这是用户主动删列的预期结果。
     *
     * 参数：newCount 是想要的波数，超出允许范围时会被夹回来。
     */
    public void setWaveCount(int newCount) {
        int target = newCount;
        if (target < MIN_WAVE_COUNT) {
            target = MIN_WAVE_COUNT;
        }
        if (target > MAX_WAVE_COUNT) {
            target = MAX_WAVE_COUNT;
        }
        if (target == waveCount) {
            return;
        }

        // 换成新尺寸的表格，再把两者都有的那部分内容搬过去。
        String[][] newKinds = new String[Layout.ROW_COUNT][target];
        int[][] newCounts = new int[Layout.ROW_COUNT][target];
        boolean[][] newRandomRows = new boolean[Layout.ROW_COUNT][target];
        int sharedWaves = Math.min(target, waveCount);
        for (int row = 0; row < Layout.ROW_COUNT; row++) {
            for (int wave = 0; wave < sharedWaves; wave++) {
                newKinds[row][wave] = kinds[row][wave];
                newCounts[row][wave] = counts[row][wave];
                newRandomRows[row][wave] = randomRows[row][wave];
            }
        }
        kinds = newKinds;
        counts = newCounts;
        randomRows = newRandomRows;
        waveCount = target;
    }

    /**
     * 判断行号和波号是不是都落在表格里。
     *
     * 参数：row 是行号；wave 是波号。
     * 返回：都在范围内就返回真。
     */
    public boolean insideGrid(int row, int wave) {
        if (row < 0 || row >= Layout.ROW_COUNT) {
            return false;
        }
        if (wave < 0 || wave >= waveCount) {
            return false;
        }
        return true;
    }

    /**
     * 查某个格子放的是哪种僵尸。
     *
     * 参数：row 是行号；wave 是波号。
     * 返回：僵尸品种名；格子是空的或者坐标越界都返回 null。
     */
    public String kindAt(int row, int wave) {
        if (!insideGrid(row, wave)) {
            return null;
        }
        return kinds[row][wave];
    }

    /**
     * 查某个格子放了几只僵尸。
     *
     * 参数：row 是行号；wave 是波号。
     * 返回：僵尸数量；格子是空的或者坐标越界都返回 0。
     */
    public int countAt(int row, int wave) {
        if (!insideGrid(row, wave)) {
            return 0;
        }
        return counts[row][wave];
    }

    /**
     * 查某个格子是否随机行出怪。
     *
     * 参数：row 是行号；wave 是波号。
     * 返回：随机行就返回真；格子是空的或者坐标越界都返回假。
     */
    public boolean randomRowAt(int row, int wave) {
        if (!insideGrid(row, wave)) {
            return false;
        }
        return randomRows[row][wave];
    }

    /**
     * 设置某个格子是否随机行出怪。
     *
     * 参数：row 是行号；wave 是波号；random 是否随机行。
     */
    public void setRandomRow(int row, int wave, boolean random) {
        if (!insideGrid(row, wave)) {
            return;
        }
        randomRows[row][wave] = random;
    }

    /**
     * 往某个格子里放僵尸。
     *
     * 参数：row 是行号；wave 是波号；kind 是僵尸品种名；count 是数量。
     *       kind 传 null 或者 count 不大于 0，都表示把这个格子清空。
     */
    public void setCell(int row, int wave, String kind, int count) {
        if (!insideGrid(row, wave)) {
            return;
        }
        if (kind == null || count <= 0) {
            kinds[row][wave] = null;
            counts[row][wave] = 0;
            randomRows[row][wave] = false;
            return;
        }
        kinds[row][wave] = kind;
        counts[row][wave] = Math.min(count, MAX_CELL_COUNT);
    }

    /**
     * 清空某个格子。
     *
     * 参数：row 是行号；wave 是波号。
     */
    public void clearCell(int row, int wave) {
        setCell(row, wave, null, 0);
    }

    /**
     * 给某个格子增减僵尸数量。
     *
     * 参数：row 是行号；wave 是波号；delta 是增减的只数，负数表示减少。
     *       减到 0 时格子会被清空。
     */
    public void addCount(int row, int wave, int delta) {
        if (!insideGrid(row, wave)) {
            return;
        }
        if (kinds[row][wave] == null) {
            return;
        }
        int newCount = counts[row][wave] + delta;
        if (newCount < 0) {
            newCount = 0;
        }
        setCell(row, wave, kinds[row][wave], newCount);
    }

    /**
     * 拼出一个格子的文字摘要，比如"路障僵尸 × 2"。
     *
     * 编辑器用它来提示当前选中的是哪个格子。
     *
     * 参数：row 是行号；wave 是波号。
     * 返回：摘要文字；格子是空的或者坐标越界都返回 null。
     */
    public String cellSummary(int row, int wave) {
        String kind = kindAt(row, wave);
        if (kind == null) {
            return null;
        }
        int position = kindIndex(kind);
        String label = kind;
        if (position >= 0) {
            label = ZOMBIE_LABELS[position];
        }
        return label + " × " + countAt(row, wave);
    }

    /**
     * 查一种僵尸在 ZOMBIE_KINDS 里排第几。
     *
     * 参数：kind 是僵尸品种名。
     * 返回：找到就返回下标；找不到返回 -1。
     */
    private static int kindIndex(String kind) {
        for (int index = 0; index < ZOMBIE_KINDS.length; index++) {
            if (ZOMBIE_KINDS[index].equals(kind)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 切换某个格子的随机行状态。
     *
     * 参数：row 是行号；wave 是波号。
     */
    public void toggleRandomRow(int row, int wave) {
        if (!insideGrid(row, wave)) {
            return;
        }
        randomRows[row][wave] = !randomRows[row][wave];
    }

    /**
     * 把一个格子的内容整个搬到另一个格子，源格子清空。
     *
     * 参数：fromRow、fromWave 是源格子的坐标；toRow、toWave 是目标格子的坐标。
     */
    public void moveCell(int fromRow, int fromWave, int toRow, int toWave) {
        if (!insideGrid(fromRow, fromWave) || !insideGrid(toRow, toWave)) {
            return;
        }
        if (fromRow == toRow && fromWave == toWave) {
            return;
        }
        String kind = kinds[fromRow][fromWave];
        int count = counts[fromRow][fromWave];
        boolean random = randomRows[fromRow][fromWave];
        clearCell(fromRow, fromWave);
        setCell(toRow, toWave, kind, count);
        randomRows[toRow][toWave] = random;
    }

    /**
     * 交换两个格子的内容。
     *
     * 拖动时如果目标格子已经有僵尸，直接覆盖会把原来的内容弄丢，所以改成交换。
     *
     * 参数：firstRow、firstWave 是第一个格子的坐标；secondRow、secondWave 是第二个格子的坐标。
     */
    public void swapCells(int firstRow, int firstWave, int secondRow, int secondWave) {
        if (!insideGrid(firstRow, firstWave) || !insideGrid(secondRow, secondWave)) {
            return;
        }
        String keptKind = kinds[firstRow][firstWave];
        int keptCount = counts[firstRow][firstWave];
        boolean keptRandom = randomRows[firstRow][firstWave];
        kinds[firstRow][firstWave] = kinds[secondRow][secondWave];
        counts[firstRow][firstWave] = counts[secondRow][secondWave];
        randomRows[firstRow][firstWave] = randomRows[secondRow][secondWave];
        kinds[secondRow][secondWave] = keptKind;
        counts[secondRow][secondWave] = keptCount;
        randomRows[secondRow][secondWave] = keptRandom;
    }

    /** 把整张表格清空，但保留波数和各项参数。 */
    public void clearAllCells() {
        for (int row = 0; row < Layout.ROW_COUNT; row++) {
            for (int wave = 0; wave < waveCount; wave++) {
                clearCell(row, wave);
            }
        }
    }

    /**
     * 算出某一波的出场时刻。
     *
     * 参数：wave 是波号，从 0 开始。
     * 返回：从关卡开始算起的第几毫秒。
     */
    public long waveTime(int wave) {
        return firstWaveDelay + (long) wave * waveInterval;
    }

    /**
     * 数一数整张表格里一共有多少只僵尸。
     *
     * 返回：僵尸总数。
     */
    public int totalZombies() {
        int total = 0;
        for (int row = 0; row < Layout.ROW_COUNT; row++) {
            for (int wave = 0; wave < waveCount; wave++) {
                total = total + counts[row][wave];
            }
        }
        return total;
    }

    /**
     * 判断某个名字是不是编辑器支持的僵尸品种。
     *
     * 参数：kind 是僵尸品种名。
     * 返回：支持就返回真。
     */
    public static boolean isKnownKind(String kind) {
        return kindIndex(kind) >= 0;
    }

    /**
     * 把毫秒换成"分:秒"的样子，给表头显示用。
     *
     * 参数：milliseconds 是毫秒数。
     * 返回：形如 1:30 的字符串。
     */
    public static String formatTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 把表格摊平成游戏要的出场表。
     *
     * 一个格子里写着几只，就展开成几条记录，彼此按 spawnSpacing 错开，
     * 免得同一毫秒挤出来一堆僵尸。
     * 如果格子标记了随机行，僵尸会随机出现在任意一行而非固定行。
     *
     * 返回：按出场时间排好序的僵尸出场表。
     */
    public List<ZombieSpawn> buildSpawns() {
        List<ZombieSpawn> result = new ArrayList<ZombieSpawn>();
        for (int wave = 0; wave < waveCount; wave++) {
            for (int row = 0; row < Layout.ROW_COUNT; row++) {
                String kind = kinds[row][wave];
                if (kind == null) {
                    continue;
                }
                for (int index = 0; index < counts[row][wave]; index++) {
                    long at = waveTime(wave) + index * spawnSpacing;
                    int targetRow = row;
                    if (randomRows[row][wave]) {
                        targetRow = ZombieSpawn.RANDOM_ROW;
                    }
                    result.add(new ZombieSpawn((int) at, targetRow, kind));
                }
            }
        }
        sortByTime(result);
        return result;
    }

    /**
     * 按出场时间给僵尸表排序，用的是最直观的插入排序。
     *
     * 参数：spawns 是待排序的出场表，排完直接改在原列表上。
     */
    private static void sortByTime(List<ZombieSpawn> spawns) {
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
     * 把整个关卡写成 JSON 文本。
     *
     * 格式和手写的关卡文件保持一致：一条僵尸占一行，方便用文本工具比对改动。
     * 除了游戏要看的字段，最后还多写一段 editor，把表格本身记下来，
     * 这样下次打开还能还原成一模一样的表格；游戏不认识这段，会直接忽略。
     *
     * 返回：完整的 JSON 文本。
     */
    public String toJsonText() {
        StringBuilder text = new StringBuilder();
        text.append("{\n");
        text.append("    \"background_type\":" + backgroundIndex + ",\n");
        text.append("    \"choosebar_type\":" + barType + ",\n");
        text.append("    \"init_sun_value\":" + initialSun + ",\n");
        text.append("    \"sky_sun_interval\":" + skySunInterval + ",\n");

        // 只有传送带和保龄球模式才有卡池，正常选卡模式写了反而多余。
        if (barType != GameState.BAR_NORMAL) {
            writePlantList(text, "card_pool", cardPool);
        }
        // 两份清单只对正常选卡模式有意义，空着就不写，免得老关卡文件平白多两行。
        if (!bannedPlants.isEmpty()) {
            writePlantList(text, "banned_plants", bannedPlants);
        }
        if (!requiredPlants.isEmpty()) {
            writePlantList(text, "required_plants", requiredPlants);
        }

        List<ZombieSpawn> spawns = buildSpawns();
        text.append("    \"zombie_list\":[\n");
        for (int index = 0; index < spawns.size(); index++) {
            ZombieSpawn spawn = spawns.get(index);
            text.append("        {\"time\":" + spawn.at);
            text.append(", \"map_y\":" + spawn.row);
            text.append(", \"name\":\"" + spawn.name + "\"}");
            text.append(lineEnd(index, spawns.size()));
        }
        text.append("    ],\n");

        text.append(editorBlockText());
        text.append("}\n");
        return text.toString();
    }

    /**
     * 往 JSON 里写一段植物清单。
     *
     * 卡池、禁用清单、必选清单的格式完全一样，都是"一行一种植物"，
     * 所以共用这一个方法，改动格式时只要改一处。
     *
     * 参数：text 是正在拼的 JSON 文本；field 是字段名；
     *       plants 是植物编号列表。
     */
    private static void writePlantList(StringBuilder text, String field, List<Integer> plants) {
        text.append("    \"" + field + "\":[\n");
        for (int index = 0; index < plants.size(); index++) {
            int plantIndex = plants.get(index).intValue();
            text.append("        {\"name\":\"" + Cards.PLANTS[plantIndex] + "\"}");
            text.append(lineEnd(index, plants.size()));
        }
        text.append("    ],\n");
    }

    /**
     * 拼出 editor 那一段文本，也就是表格本身的存档。
     *
     * 返回：editor 段的 JSON 文本。
     */
    private String editorBlockText() {
        // 先把有内容的格子收集成一行行文本，才知道总共几行、哪一行是最后一行。
        List<String> cellLines = new ArrayList<String>();
        for (int wave = 0; wave < waveCount; wave++) {
            for (int row = 0; row < Layout.ROW_COUNT; row++) {
                if (kinds[row][wave] == null) {
                    continue;
                }
                String line = "            {\"wave\":" + wave
                    + ", \"row\":" + row
                    + ", \"name\":\"" + kinds[row][wave] + "\""
                    + ", \"count\":" + counts[row][wave];
                if (randomRows[row][wave]) {
                    line = line + ", \"random_row\":true";
                }
                line = line + "}";
                cellLines.add(line);
            }
        }

        StringBuilder text = new StringBuilder();
        text.append("    \"editor\":{\n");
        text.append("        \"wave_count\":" + waveCount + ",\n");
        text.append("        \"first_wave_delay\":" + firstWaveDelay + ",\n");
        text.append("        \"wave_interval\":" + waveInterval + ",\n");
        text.append("        \"spawn_spacing\":" + spawnSpacing + ",\n");
        text.append("        \"cells\":[\n");
        for (int index = 0; index < cellLines.size(); index++) {
            text.append(cellLines.get(index));
            text.append(lineEnd(index, cellLines.size()));
        }
        text.append("        ]\n");
        text.append("    }\n");
        return text.toString();
    }

    /**
     * 决定 JSON 数组里一项的结尾：不是最后一项就要补逗号。
     *
     * 参数：index 是当前项的下标；size 是数组一共多少项。
     * 返回：该补在行尾的字符串。
     */
    private static String lineEnd(int index, int size) {
        if (index + 1 < size) {
            return ",\n";
        }
        return "\n";
    }

    /**
     * 把关卡存成文件。
     *
     * 参数：path 是要写到哪里，目录不存在时会自动建好。
     * 异常：写文件失败时抛出 IOException。
     */
    public void save(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        byte[] content = toJsonText().getBytes(StandardCharsets.UTF_8);
        Files.write(path, content);
    }

    /**
     * 从文件里读出一个关卡。
     *
     * 参数：path 是关卡文件位置。
     * 返回：读好的关卡。
     * 异常：读文件失败时抛出 IOException。
     */
    public static LevelDesign load(Path path) throws IOException {
        JsonObject json = Assets.readObject(path);
        return fromJson(json);
    }

    /**
     * 从 JSON 对象里读出一个关卡。
     *
     * 编辑器自己存的文件带 editor 那一段，照着读就能原样还原；
     * 手写的老关卡没有这一段，只好按出场时间反推出波次。
     *
     * 参数：json 是关卡文件的根对象。
     * 返回：读好的关卡。
     */
    public static LevelDesign fromJson(JsonObject json) {
        LevelDesign design = new LevelDesign();
        if (json.has("background_type")) {
            design.backgroundIndex = json.get("background_type").getAsInt();
        }
        if (json.has("choosebar_type")) {
            design.barType = json.get("choosebar_type").getAsInt();
        }
        if (json.has("init_sun_value")) {
            design.initialSun = json.get("init_sun_value").getAsInt();
        }
        if (json.has("sky_sun_interval")) {
            long interval = json.get("sky_sun_interval").getAsLong();
            design.skySunInterval = Math.max(Layout.MIN_SKY_SUN_INTERVAL, interval);
        }
        if (json.has("card_pool")) {
            readPlantList(json.getAsJsonArray("card_pool"), design.cardPool);
        }
        if (json.has("banned_plants")) {
            readPlantList(json.getAsJsonArray("banned_plants"), design.bannedPlants);
        }
        if (json.has("required_plants")) {
            readPlantList(json.getAsJsonArray("required_plants"), design.requiredPlants);
        }

        if (json.has("editor")) {
            design.readEditorBlock(json.getAsJsonObject("editor"));
            return design;
        }
        if (json.has("zombie_list")) {
            design.rebuildFromSpawns(json.getAsJsonArray("zombie_list"));
        }
        return design;
    }

    /**
     * 读一段植物清单。
     *
     * 卡池、禁用清单、必选清单的格式一样，所以共用这一个方法。
     * 认不出的植物直接跳过，总比让整个关卡打不开强。
     *
     * 参数：array 是关卡文件里的清单数组；target 是读到哪个列表里。
     */
    private static void readPlantList(JsonArray array, List<Integer> target) {
        for (int index = 0; index < array.size(); index++) {
            JsonObject entry = array.get(index).getAsJsonObject();
            String name = entry.get("name").getAsString();
            int cardIndex = Cards.indexOf(name);
            if (cardIndex >= 0) {
                target.add(Integer.valueOf(cardIndex));
            }
        }
    }

    /**
     * 读 editor 那一段，把表格原样恢复出来。
     *
     * 参数：editor 是关卡文件里的 editor 对象。
     */
    private void readEditorBlock(JsonObject editor) {
        if (editor.has("first_wave_delay")) {
            firstWaveDelay = editor.get("first_wave_delay").getAsLong();
        }
        if (editor.has("wave_interval")) {
            long interval = editor.get("wave_interval").getAsLong();
            waveInterval = Math.max(MIN_WAVE_INTERVAL, interval);
        }
        if (editor.has("spawn_spacing")) {
            long spacing = editor.get("spawn_spacing").getAsLong();
            spawnSpacing = Math.max(MIN_SPAWN_SPACING, spacing);
        }
        if (editor.has("wave_count")) {
            setWaveCount(editor.get("wave_count").getAsInt());
        }
        if (!editor.has("cells")) {
            return;
        }

        JsonArray cells = editor.getAsJsonArray("cells");
        for (int index = 0; index < cells.size(); index++) {
            JsonObject cell = cells.get(index).getAsJsonObject();
            int wave = cell.get("wave").getAsInt();
            int row = cell.get("row").getAsInt();
            String name = cell.get("name").getAsString();
            int count = cell.get("count").getAsInt();
            boolean random = false;
            if (cell.has("random_row")) {
                random = cell.get("random_row").getAsBoolean();
            }
            // 万一文件里的波号比 wave_count 还大，就把表格撑大，免得内容读丢。
            if (wave >= waveCount) {
                setWaveCount(wave + 1);
            }
            if (isKnownKind(name)) {
                setCell(row, wave, name, count);
                randomRows[row][wave] = random;
            }
        }
    }

    /**
     * 拿老关卡的出场表反推出表格。
     *
     * 分三步：先按出场时间把僵尸切成一波一波，再从波与波的间距反推出各项时间参数，
     * 最后统计每一波每一行出了多少只僵尸，填进格子里。
     *
     * 参数：list 是关卡文件里的 zombie_list 数组。
     */
    private void rebuildFromSpawns(JsonArray list) {
        List<ZombieSpawn> spawns = new ArrayList<ZombieSpawn>();
        for (int index = 0; index < list.size(); index++) {
            JsonObject entry = list.get(index).getAsJsonObject();
            int at = entry.get("time").getAsInt();
            int row = entry.get("map_y").getAsInt();
            String name = entry.get("name").getAsString();
            spawns.add(new ZombieSpawn(at, row, name));
        }
        if (spawns.isEmpty()) {
            clearAllCells();
            return;
        }
        sortByTime(spawns);

        List<List<ZombieSpawn>> waves = splitIntoWaves(spawns);
        readTimingFromWaves(spawns, waves);
        fillCellsFromWaves(waves);
    }

    /**
     * 按出场时间把僵尸切成一波一波。
     *
     * 相邻两只僵尸隔得比 IMPORT_WAVE_GAP 还久，就认为中间换了一波。
     *
     * 参数：spawns 是已经按时间排好序的出场表。
     * 返回：每一波一个列表。
     */
    private static List<List<ZombieSpawn>> splitIntoWaves(List<ZombieSpawn> spawns) {
        List<List<ZombieSpawn>> waves = new ArrayList<List<ZombieSpawn>>();
        List<ZombieSpawn> current = new ArrayList<ZombieSpawn>();
        current.add(spawns.get(0));
        for (int index = 1; index < spawns.size(); index++) {
            ZombieSpawn spawn = spawns.get(index);
            ZombieSpawn previous = current.get(current.size() - 1);
            if (spawn.at - previous.at > IMPORT_WAVE_GAP) {
                waves.add(current);
                current = new ArrayList<ZombieSpawn>();
            }
            current.add(spawn);
        }
        waves.add(current);
        return waves;
    }

    /**
     * 从切好的波次里反推出各项时间参数。
     *
     * 第一波什么时候出，就是第一波延迟；各波起点之间的平均间距，就是出怪间隔；
     * 一波之内两只僵尸最小的间距，就当成同格僵尸的错开时间。
     *
     * 参数：spawns 是排好序的出场表；waves 是切好的波次。
     */
    private void readTimingFromWaves(List<ZombieSpawn> spawns, List<List<ZombieSpawn>> waves) {
        List<ZombieSpawn> firstWave = waves.get(0);
        firstWaveDelay = firstWave.get(0).at;

        if (waves.size() > 1) {
            List<ZombieSpawn> lastWave = waves.get(waves.size() - 1);
            long span = lastWave.get(0).at - firstWave.get(0).at;
            long average = span / (waves.size() - 1);
            waveInterval = Math.max(MIN_WAVE_INTERVAL, average);
        }

        long smallestGap = Long.MAX_VALUE;
        for (int index = 1; index < spawns.size(); index++) {
            long gap = spawns.get(index).at - spawns.get(index - 1).at;
            // 跨波的大间距不能拿来当同格间隔，所以只看波内的那些小间距。
            if (gap > 0 && gap <= IMPORT_WAVE_GAP && gap < smallestGap) {
                smallestGap = gap;
            }
        }
        if (smallestGap != Long.MAX_VALUE) {
            spawnSpacing = Math.max(MIN_SPAWN_SPACING, smallestGap);
        }
    }

    /**
     * 统计每一波每一行的僵尸，填进表格的格子里。
     *
     * 一个格子只能记一种僵尸，所以同一波同一行混着几种时只能取数量最多的那种，
     * 数量仍按该行的总数算，这样僵尸总数不会变少。做了这种合并会写进 importNote 告诉用户。
     *
     * 参数：waves 是切好的波次。
     */
    private void fillCellsFromWaves(List<List<ZombieSpawn>> waves) {
        setWaveCount(waves.size());
        clearAllCells();
        boolean merged = false;
        boolean unknown = false;

        for (int wave = 0; wave < waves.size(); wave++) {
            List<ZombieSpawn> group = waves.get(wave);
            if (hasUnknownKind(group)) {
                unknown = true;
            }
            int[][] tally = countByRowAndKind(group);
            for (int row = 0; row < Layout.ROW_COUNT; row++) {
                if (fillOneCell(row, wave, tally[row])) {
                    merged = true;
                }
            }
        }

        String note = "";
        if (merged) {
            note = note + "同一波同一行出现了多种僵尸，已按数量最多的品种合并。";
        }
        if (unknown) {
            note = note + "存在编辑器不支持的僵尸品种，已跳过。";
        }
        importNote = note;
    }

    /**
     * 看看这一波里有没有编辑器不支持的僵尸品种。
     *
     * 参数：group 是这一波的出场记录。
     * 返回：有不认识的品种就返回真。
     */
    private static boolean hasUnknownKind(List<ZombieSpawn> group) {
        for (int index = 0; index < group.size(); index++) {
            if (indexOfKind(group.get(index).name) < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 数一数这一波里，每一行各出了几只哪种僵尸。
     *
     * 参数：group 是这一波的出场记录。
     * 返回：一张表，第一维是行号，第二维是品种在 ZOMBIE_KINDS 里的下标。
     */
    private static int[][] countByRowAndKind(List<ZombieSpawn> group) {
        int[][] tally = new int[Layout.ROW_COUNT][ZOMBIE_KINDS.length];
        for (int index = 0; index < group.size(); index++) {
            ZombieSpawn spawn = group.get(index);
            int kindIndex = indexOfKind(spawn.name);
            if (kindIndex < 0) {
                continue;
            }
            if (spawn.row < 0 || spawn.row >= Layout.ROW_COUNT) {
                continue;
            }
            tally[spawn.row][kindIndex] = tally[spawn.row][kindIndex] + 1;
        }
        return tally;
    }

    /**
     * 按统计结果填好一个格子。
     *
     * 一个格子只放得下一种僵尸，所以混着几种时只能取数量最多的那种，
     * 数量仍按这一行的总数算，这样僵尸总数不会变少。
     *
     * 参数：row 是行号；wave 是波号；rowTally 是这一行各品种的只数。
     * 返回：这一行原本混着好几种、做了合并就返回真。
     */
    private boolean fillOneCell(int row, int wave, int[] rowTally) {
        int bestKind = 0;
        int total = 0;
        int kindsPresent = 0;
        for (int kindIndex = 0; kindIndex < rowTally.length; kindIndex++) {
            total = total + rowTally[kindIndex];
            if (rowTally[kindIndex] > 0) {
                kindsPresent = kindsPresent + 1;
            }
            if (rowTally[kindIndex] > rowTally[bestKind]) {
                bestKind = kindIndex;
            }
        }
        if (total > 0) {
            setCell(row, wave, ZOMBIE_KINDS[bestKind], total);
        }
        return kindsPresent > 1;
    }

    /**
     * 查一种僵尸排在 ZOMBIE_KINDS 的第几位。
     *
     * 参数：kind 是僵尸品种名。
     * 返回：找到就返回下标；找不到返回 -1。
     */
    private static int indexOfKind(String kind) {
        for (int index = 0; index < ZOMBIE_KINDS.length; index++) {
            if (ZOMBIE_KINDS[index].equals(kind)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 存盘前检查一遍，把容易踩的坑提前说清楚。
     *
     * 返回：需要提醒用户的话；没什么问题就返回空字符串。
     */
    public String validate() {
        if (totalZombies() == 0) {
            return "网格里一只僵尸都没有，进入关卡后会立刻通关。";
        }
        // 游戏读传送带和保龄球关卡时一定会去取卡池，没有卡池会直接读不下去。
        if (barType != GameState.BAR_NORMAL && cardPool.isEmpty()) {
            return "传送带和保龄球模式必须至少选一张卡池里的植物，否则关卡无法载入。";
        }
        if (barType == GameState.BAR_NORMAL && backgroundIndex != 0) {
            return "只有白天草坪（背景 0）的正常选卡关卡会从天上掉阳光，当前设置下阳光生成速度不起作用。";
        }
        // 必选超过八张就塞不进卡槽了，开始按钮永远不会亮。
        if (requiredPlants.size() > 8) {
            return "必选植物最多八张，现在选了 " + requiredPlants.size() + " 张，进了关卡没法开始。";
        }
        // 同一植物既禁用又必选，游戏里必选优先，这里提醒一句免得用户以为禁掉了。
        for (int index = 0; index < bannedPlants.size(); index++) {
            int plant = bannedPlants.get(index).intValue();
            if (requiredPlants.contains(Integer.valueOf(plant))) {
                return Cards.PLANTS[plant] + " 同时出现在禁用和必选清单里，游戏里会按必选处理。";
            }
        }
        return "";
    }
}
