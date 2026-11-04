package pvz.plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import pvz.world.Layout;
import pvz.world.Assets;
import java.awt.Rectangle;

/**
 * 卡片相关的工具方法
 *
 * 按 PlantCatalog 的资料生成静态卡槽或传送带卡片，避免两种模式各写一份创建逻辑
 */
public final class Cards {
    /** 这个类只提供静态方法，不允许创建对象 */
    private Cards() {
    }

    /**
     * 按选好的植物编号摆出一排静态卡片
     *
     * 参数：indices 是玩家在选卡界面选中的植物编号，按选择先后排列
     * 返回：摆好的卡片列表，位置从左上角往右排
     */
    public static List<Card> staticBar(List<Integer> indices) {
        List<Card> result = new ArrayList<Card>();
        for (int position = 0; position < indices.size(); position = position + 1) {
            int plantIndex = indices.get(position).intValue();
            result.add(new Card(plantIndex, Layout.cardSlotLeft(position), Layout.CARD_BAR_TOP));
        }
        return result;
    }

    /**
     * 从可出卡池里随机挑一张，做成传送带上的新卡片
     *
     * 参数：pool 是可出卡池；random 是随机数生成器；time 是当前时刻；assets 提供卡片真实尺寸
     * 返回：一张标记为在移动的新卡片
     */
    public static Card newMovingCard(List<Integer> pool, Random random, long time, Assets assets) {
        int choice = random.nextInt(pool.size());
        int plantIndex = pool.get(choice).intValue();

        Card card = new Card(plantIndex, Layout.CONVEYOR_LEFT, Layout.CONVEYOR_CARD_Y);
        card.created = time;
        card.moving = true;
        Rectangle bounds = card.bounds(assets, Layout.CARD_SCALE);
        int beltRight = Layout.CONVEYOR_LEFT + assets.image("MoveBackground").getWidth();
        card.x = beltRight - bounds.width - Layout.CONVEYOR_CARD_GAP;
        return card;
    }
}
