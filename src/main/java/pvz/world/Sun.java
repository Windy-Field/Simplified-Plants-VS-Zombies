package pvz.world;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * 一颗可以点击收集的阳光。
 *
 * 阳光有两种来源：天上掉下来的，和向日葵／阳光菇产出的。
 * 它先朝目标点移动，走到之后原地停七秒，然后消失。
 */
public class Sun extends Sprite {
    /** 收集后能给多少阳光；大阳光 25，小阳光 12。 */
    public int value;

    /** 要移动到的目标中心横坐标。 */
    public int targetX;

    /** 要移动到的目标底部纵坐标。 */
    public int targetY;

    /** 到达目标的时刻；还没到就是 0。 */
    public long arrived;

    /** 是否正在飞向左上角的阳光数字处。 */
    public boolean flyingToCounter;

    /** 飞向左上角的起点横坐标，用来算减速。 */
    private double collectStartX;

    /** 飞向左上角的起点纵坐标，用来算减速。 */
    private double collectStartY;

    /** 飞向左上角的起始时刻。 */
    private long collectStartTime;

    /**
     * 创建一颗阳光。
     *
     * 参数：center 和 bottom 是出生位置；destinationX 和 destinationY 是目标位置；
     *       big 表示是不是大阳光；assets 提供图片。
     * 说明：大阳光来自天空和向日葵，小阳光只有阳光菇才产。
     */
    public Sun(int center, int bottom, int destinationX, int destinationY, boolean big, Assets assets) {
        super("Sun", center, bottom, 0, 1, assets);
        value = 25;
        if (!big) {
            value = 12;
            scale = 0.6;
        } else {
            scale = 0.9;
        }
        // 父类按未缩放的图片定位，缩放之后要重新对一次底边中央。
        BufferedImage image = picture(assets, 0);
        x = center - image.getWidth() / 2.0;
        y = bottom - image.getHeight();
        targetX = destinationX;
        targetY = destinationY;
    }

    /**
     * 朝目标移动一帧，到了就开始计时。
     *
     * 参数：assets 提供图片；time 是当前时刻；
     *       speedMultiplier 是当前倍速，1 表示原速，2 表示两倍速。
     * 说明：倍速要乘在步长上，这样 2 倍速下阳光掉落也是原来的两倍快。
     */
    public void update(Assets assets, long time, int speedMultiplier) {
        if (flyingToCounter) {
            updateFlyingToCounter(assets, time);
            return;
        }

        Rectangle rect = bounds(assets, time);
        int center = (int) rect.getCenterX();
        int bottom = (int) rect.getMaxY();

        // 这一帧横向、纵向各走多远；倍速越高走得越远。
        int step = Layout.SUN_SPEED * speedMultiplier;

        // 横向和纵向各自靠近目标，所以阳光走的是斜线。
        // 每边走之前先看看离目标还剩多少，最多只走剩下的距离，
        // 免得一步跨过目标，害得后面永远差一点、到不了目的地。
        if (center < targetX) {
            x = x + Math.min(step, targetX - center);
        } else if (center > targetX) {
            x = x - Math.min(step, center - targetX);
        }
        if (bottom < targetY) {
            y = y + Math.min(step, targetY - bottom);
        } else if (bottom > targetY) {
            y = y - Math.min(step, bottom - targetY);
        }

        if (center == targetX && bottom == targetY) {
            if (arrived == 0) {
                arrived = time;
            } else if (time - arrived > Layout.SUN_STAY_DURATION) {
                alive = false;
            }
        }
    }

    /**
     * 开始收集动画：飞向左上角的阳光数字。
     *
     * 参数：assets 提供图片；time 是当前时刻。
     */
    public void startCollect(Assets assets, long time) {
        Rectangle rect = bounds(assets, time);
        collectStartX = rect.getCenterX();
        collectStartY = rect.getCenterY();
        collectStartTime = time;
        flyingToCounter = true;
    }

    /**
     * 推进收集动画：先匀速、接近目标后减速到零。
     *
     * 参数：assets 提供图片；time 是当前时刻。
     */
    private void updateFlyingToCounter(Assets assets, long time) {
        // 目标是左上角阳光数字的中心。
        final double GOAL_X = 50.0;
        final double GOAL_Y = 50.0;
        final double UNIFORM_RATIO = 0.8;  // 前 80% 匀速，后 20% 减速

        // 总路程和已经飞行的时间（毫秒）。
        double totalDist = Math.sqrt(
            (GOAL_X - collectStartX) * (GOAL_X - collectStartX)
            + (GOAL_Y - collectStartY) * (GOAL_Y - collectStartY));
        long elapsed = time - collectStartTime;

        // 按时间计算应该飞到的位置（每像素约 2ms）。
        double durationMs = totalDist / Layout.SUN_COLLECT_SPEED * 16;
        double progress = Math.min(1.0, elapsed / durationMs);

        // 前 80% 匀速，后 20% 三次缓出减速。
        double traveled;
        if (progress <= UNIFORM_RATIO) {
            // 匀速阶段：线性推进。
            traveled = totalDist * progress / UNIFORM_RATIO * UNIFORM_RATIO;
        } else {
            // 减速阶段：三次缓出曲线 (cubic ease-out)。
            double slowProgress = (progress - UNIFORM_RATIO) / (1.0 - UNIFORM_RATIO);
            double slowCurve = 1 - (1 - slowProgress) * (1 - slowProgress) * (1 - slowProgress);
            traveled = totalDist * (UNIFORM_RATIO + (1.0 - UNIFORM_RATIO) * slowCurve);
        }

        // 按行进比例计算新位置。
        double angle = Math.atan2(GOAL_Y - collectStartY, GOAL_X - collectStartX);
        double newCenterX = collectStartX + Math.cos(angle) * traveled;
        double newCenterY = collectStartY + Math.sin(angle) * traveled;

        // 转换成左上角坐标。
        BufferedImage image = picture(assets, time);
        x = newCenterX - image.getWidth() / 2.0;
        y = newCenterY - image.getHeight() / 2.0;

        // 到达终点就标记为死。
        if (progress >= 1.0) {
            alive = false;
        }
    }
}
