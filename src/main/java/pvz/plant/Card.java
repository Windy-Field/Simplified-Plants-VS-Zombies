package pvz.plant;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import pvz.world.Assets;

/**
 * 卡槽上的一张卡片，比如"向日葵 50 阳光"。
 *
 * 卡片有两种：正常关卡里选好之后固定摆在左上角，用一次进一次冷却；
 * 传送带上的卡片用完就没了，所以额外记录了创建时间。
 */
public class Card {
    /** 这代表哪一种植物，是 PlantCatalog 里的下标。 */
    public int index;

    /** 卡片左上角的横坐标。 */
    public int x;

    /** 卡片左上角的纵坐标。 */
    public int y;

    /** 上一次使用这张卡的时刻，用来算冷却。 */
    public long lastUsed;

    /** 这张卡是什么时候出现的，传送带卡片靠它控制移动速度。 */
    public long created;

    /** 是不是传送带上的卡片（传送带用另一套图片）。 */
    public boolean moving;

    /** 是否正在飞向目标位置（选卡界面的动画）。 */
    public boolean flying;

    /** 飞行动画是飞向卡槽（true）还是飞回候选区（false）。 */
    public boolean flyingToBar;

    /** 飞行动画的起点横坐标。 */
    private double flyStartX;

    /** 飞行动画的起点纵坐标。 */
    private double flyStartY;

    /** 飞行动画的目标横坐标。 */
    private double flyTargetX;

    /** 飞行动画的目标纵坐标。 */
    private double flyTargetY;

    /** 飞行动画的起始时刻。 */
    private long flyStartTime;

    /**
     * 创建一张卡片。
     *
     * 参数：plantIndex 是植物下标；left 和 top 是左上角坐标。
     * 说明：lastUsed 设成负的冷却时间，等于"一开始就是冷却好的"。
     */
    public Card(int plantIndex, int left, int top) {
        index = plantIndex;
        x = left;
        y = top;
        PlantDefinition definition = Cards.definitionAt(index);
        lastUsed = -definition.cooldown;
    }

    /**
     * 算出卡片在屏幕上占的范围，用来判断有没有点到它。
     *
     * 参数：assets 提供图片；scale 是缩放比例。
     * 返回：卡片的矩形区域。
     */
    public Rectangle bounds(Assets assets, double scale) {
        BufferedImage image = assets.image(pictureName());
        int width = (int) (image.getWidth() * scale);
        int height = (int) (image.getHeight() * scale);
        return new Rectangle(x, y, width, height);
    }

    /**
     * 画出卡片。
     *
     * 分三种情况：不能用的时候压一层深色；在冷却中就从下往上盖一块黑；
     * 正常可用就什么都不盖。
     *
     * 参数：painter 是画笔；assets 提供图片；time 是当前时刻；
     *       sun 是当前阳光数；scale 是缩放比例；available 表示这张卡现在能不能用。
     */
    public void draw(Graphics2D painter, Assets assets, long time, int sun,
                     double scale, boolean available) {
        BufferedImage image = assets.image(pictureName());
        Rectangle rect = bounds(assets, scale);
        painter.drawImage(image, x, y, rect.width, rect.height, null);

        PlantDefinition definition = Cards.definitionAt(index);
        boolean notEnoughSun = !moving && sun < definition.cost;
        if (!available || notEnoughSun) {
            painter.setColor(new Color(0, 0, 0, 100));
            painter.fillRect(x, y, rect.width, rect.height);
            return;
        }

        long remaining = definition.cooldown - (time - lastUsed);
        if (remaining > 0) {
            // 冷却条从下往上退，所以盖住的高度按剩余时间算。
            int covered = (int) (remaining * rect.height / definition.cooldown);
            painter.setColor(new Color(0, 0, 0, 120));
            painter.fillRect(x, y, rect.width, covered);
        }
    }

    /**
     * 算出该用哪张图片。
     *
     * 传送带上的卡片用带 _move 后缀的图（表示在移动），
     * 但如果这张图本来就叫 _move，就不要重复加后缀。
     */
    private String pictureName() {
        PlantDefinition definition = Cards.definitionAt(index);
        String name = definition.cardPicture;
        if (moving && !name.endsWith("_move")) {
            return name + "_move";
        }
        return name;
    }

    /**
     * 开始飞行动画：从当前位置飞到目标位置。
     *
     * 参数：targetX 和 targetY 是目标左上角坐标；time 是当前时刻；toBar 表示是否飞向卡槽。
     */
    public void startFly(int targetX, int targetY, long time, boolean toBar) {
        flyStartX = x;
        flyStartY = y;
        flyTargetX = targetX;
        flyTargetY = targetY;
        flyStartTime = time;
        flying = true;
        flyingToBar = toBar;
    }

    /**
     * 更新飞行动画：先匀速、接近目标后减速固定。
     *
     * 参数：time 是当前时刻。
     */
    public void updateFly(long time) {
        if (!flying) {
            return;
        }

        double totalDistX = flyTargetX - flyStartX;
        double totalDistY = flyTargetY - flyStartY;
        double totalDist = Math.sqrt(totalDistX * totalDistX + totalDistY * totalDistY);

        // 飞行总时长 300ms，前 80% 匀速，后 20% 三次缓出减速。
        final double FLIGHT_DURATION_MS = 300.0;
        final double UNIFORM_RATIO = 0.8;  // 前 80% 匀速
        long elapsed = time - flyStartTime;
        double progress = Math.min(1.0, elapsed / FLIGHT_DURATION_MS);

        double traveled;
        if (progress <= UNIFORM_RATIO) {
            // 匀速阶段：线性推进。
            traveled = progress / UNIFORM_RATIO * UNIFORM_RATIO;
        } else {
            // 减速阶段：三次缓出曲线 (cubic ease-out)。
            double slowProgress = (progress - UNIFORM_RATIO) / (1.0 - UNIFORM_RATIO);
            double slowCurve = 1 - (1 - slowProgress) * (1 - slowProgress) * (1 - slowProgress);
            traveled = UNIFORM_RATIO + (1.0 - UNIFORM_RATIO) * slowCurve;
        }

        x = (int) (flyStartX + totalDistX * traveled);
        y = (int) (flyStartY + totalDistY * traveled);

        if (progress >= 1.0) {
            x = (int) flyTargetX;
            y = (int) flyTargetY;
            flying = false;
        }
    }
}
