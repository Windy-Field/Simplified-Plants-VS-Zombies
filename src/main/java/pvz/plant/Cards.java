package pvz.plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import pvz.world.Layout;

/**
 * 植物的卡片资料表。
 *
 * 四个数组必须一一对应：第 index 号植物叫 PLANTS[index]，
 * 用 PICTURES[index] 这张图，花 COST[index] 阳光，冷却 COOLDOWN[index] 毫秒。
 * 这个顺序也决定了选卡界面里卡片的摆放位置；增删植物时几个数组要同步修改，否则卡片会错位。
 * 最后两种是保龄球，只在保龄球关里用，新植物要插在它们前面。
 */
public final class Cards {
    /** 排在最后、不进选卡界面的保龄球有几种。 */
    private static final int BOWLING_COUNT = 2;

    /** 全部植物的名字。 */
    // TODO：【必做-2】新增植物时需要注册植物名（必须和 Assets.loadPlants() 中的植物名一致，
    //                且四个数组必须在同一位置各插一项，都插在 "WallNutBowling" 之前）
    public static final String[] PLANTS = {
        "SunFlower", "Peashooter", "SnowPea", "WallNut", "CherryBomb",
        "Threepeater", "RepeaterPea", "Chomper", "PuffShroom", "PotatoMine",
        "Squash", "Spikeweed", "Jalapeno", "ScaredyShroom", "SunShroom",
        "IceShroom", "HypnoShroom",
        "WallNutBowling", "RedWallNutBowling"
    };

    /** 和植物一一对应的卡片图片名。 */
    // TODO：【必做-3】新增植物时需要添加植物卡片材质路径（即 assets/Cards/ 下的文件名（不含.png），
    //                必须和 PLANTS 数组同一位置对应）
    public static final String[] PICTURES = {
        "card_sunflower", "card_peashooter", "card_snowpea", "card_wallnut",
        "card_cherrybomb", "card_threepeashooter", "card_repeaterpea", "card_chomper",
        "card_puffshroom", "card_potatomine", "card_squash", "card_spikeweed",
        "card_jalapeno", "card_scaredyshroom", "card_sunshroom", "card_iceshroom",
        "card_hypnoshroom", "card_wallnut", "card_redwallnut_move"
    };

    /** 每种植物要花多少阳光；传送带和保龄球植物是 0，因为不花阳光。 */
    // TODO：【必做-4】新增植物时需要添加植物消耗阳光数（必须和 PLANTS 数组同一位置对应）
    public static final int[] COST = {
        50, 100, 175, 50, 150, 325, 200, 150, 0, 25,
        50, 100, 125, 25, 25, 75, 75, 0, 0
    };

    /** 每种植物用完之后要等多少毫秒；传送带和保龄球植物是 0，没有冷却。 */
    // TODO：【必做-5】新增植物时需要添加植物种植冷却时长（必须和 PLANTS 数组同一位置对应）
    public static final int[] COOLDOWN = {
        7500, 7500, 7500, 30000, 50000, 7500, 7500, 7500, 7500,
        30000, 30000, 7500, 50000, 7500, 7500, 50000, 30000, 0, 0
    };

    /**
     * 选卡界面里一共摆几张候选卡，也就是除了保龄球以外的全部植物。
     * 它跟着 PLANTS 的长度自动变，加了新植物不用再改这里。
     */
    public static final int CHOOSER_CARD_COUNT = PLANTS.length - BOWLING_COUNT;

    /** 这个类只提供静态数据，不允许创建对象。 */
    private Cards() {
    }

    /**
     * 查一种植物排在第几号。
     *
     * 参数：plantName 是植物名字。
     * 返回：找到就返回下标；找不到返回 -1。
     */
    public static int indexOf(String plantName) {
        for (int index = 0; index < PLANTS.length; index++) {
            if (PLANTS[index].equals(plantName)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 按选好的植物编号摆出一排静态卡片。
     *
     * 参数：indices 是玩家在选卡界面选中的植物编号，按选择先后排列。
     * 返回：摆好的卡片列表，位置从左上角往右排。
     */
    public static List<Card> staticBar(List<Integer> indices) {
        List<Card> result = new ArrayList<Card>();
        int left = Layout.CARD_BAR_START;
        for (int position = 0; position < indices.size(); position++) {
            int plantIndex = indices.get(position).intValue();
            left = left + Layout.CARD_BAR_SPACING;
            result.add(new Card(plantIndex, left, Layout.CARD_BAR_TOP));
        }
        return result;
    }

    /**
     * 从可出卡池里随机挑一张，做成传送带上的新卡片。
     *
     * 参数：pool 是可出卡池；random 是随机数生成器；time 是当前时刻。
     * 返回：一张标记为"在移动"的新卡片，出现在传送带右端。
     */
    public static Card newMovingCard(List<Integer> pool, Random random, long time) {
        int choice = random.nextInt(pool.size());
        int plantIndex = pool.get(choice).intValue();

        Card card = new Card(plantIndex, Layout.CONVEYOR_CARD_START_X, Layout.CONVEYOR_CARD_Y);
        card.created = time;
        card.moving = true;
        return card;
    }
}
