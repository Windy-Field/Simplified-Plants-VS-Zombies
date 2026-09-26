package pvz.plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import pvz.world.Layout;

/**
 * 卡片相关的工具方法。
 *
 * 植物的固定资料已经集中在 PlantCatalog，这个类只负责把植物资料变成卡片，
 * 以及按编号查找植物。这样旧的调用处仍然有一个清楚的卡片入口。
 */
public final class Cards {
    /** 选卡界面里可以出现的植物数量。 */
    public static final int CHOOSER_CARD_COUNT = PlantCatalog.CHOOSER_COUNT;

    /** 这个类只提供静态方法，不允许创建对象。 */
    private Cards() {
    }

    /**
     * 查一种植物排在第几号。
     *
     * 参数：plantName 是植物名字。
     * 返回：找到就返回下标；找不到返回 -1。
     */
    public static int indexOf(String plantName) {
        return PlantCatalog.indexOf(plantName);
    }

    /**
     * 按编号取得植物名字。
     *
     * 参数：plantIndex 是植物编号。
     * 返回：植物名字。
     */
    public static String nameAt(int plantIndex) {
        return PlantCatalog.nameAt(plantIndex);
    }

    /**
     * 按编号取得植物资料。
     *
     * 参数：plantIndex 是植物编号。
     * 返回：植物固定资料。
     */
    public static PlantDefinition definitionAt(int plantIndex) {
        return PlantCatalog.definitionAt(plantIndex);
    }

    /**
     * 按选好的植物编号摆出一排静态卡片。
     *
     * 参数：indices 是玩家在选卡界面选中的植物编号，按选择先后排列。
     * 返回：摆好的卡片列表，位置从左上角往右排。
     */
    public static List<Card> staticBar(List<Integer> indices) {
        List<Card> result = new ArrayList<Card>();
        for (int position = 0; position < indices.size(); position++) {
            int plantIndex = indices.get(position).intValue();
            result.add(new Card(plantIndex, Layout.cardSlotLeft(position), Layout.CARD_BAR_TOP));
        }
        return result;
    }

    /**
     * 从可出卡池里随机挑一张，做成传送带上的新卡片。
     *
     * 参数：pool 是可出卡池；random 是随机数生成器；time 是当前时刻。
     * 返回：一张标记为在移动的新卡片。
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
