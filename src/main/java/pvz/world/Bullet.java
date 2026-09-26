package pvz.world;

/**
 * 一颗植物射出的子弹。
 *
 * 子弹只会往右飞，并且会慢慢靠近目标行的高度，
 * 这样三线射手打出去的三颗子弹才会在不同行上落到位。
 */
public class Bullet extends Sprite {
    /** 是否是冰冻子弹，打中僵尸会让它减速。 */
    public boolean ice;

    /** 是否已经打中目标，进入爆炸动画阶段。 */
    public boolean exploded;

    /** 打中的时刻，用来算爆炸动画该结束了。 */
    public long hitTime;

    /** 期望落到的纵坐标。 */
    public int targetY;

    /**
     * 在植物枪口位置产生一颗子弹。
     *
     * 参数：kind 是子弹素材名；left 和 top 是子弹左上角坐标；
     *       lane 是所在行；target 是期望落到的纵坐标；assets 提供图片。
     */
    // TODO：【选做-2.75（一）】新增植物时想让子弹有冰冻减速效果，需要把子弹名加进下面的判断里
    public Bullet(String kind, int left, int top, int lane, int target, Assets assets) {
        super(kind, left, top, lane, 1, assets);
        // 父类是按底边中央对齐的，子弹要按左上角对齐，所以这里再覆盖一次。
        x = left;
        y = top;
        targetY = target;
        ice = kind.equals("PeaIce") || kind.equals("BulletMushRoom"); // 减速效果
    }

    /**
     * 推进子弹一帧。
     *
     * 参数：time 是当前时刻。
     * 说明：已经打中的子弹只等动画放完，不再移动。
     */
    public void update(long time) {
        if (exploded) {
            if (time - hitTime > Layout.BULLET_EXPLODE_DURATION) {
                alive = false;
            }
            return;
        }

        // 战斗会按固定时间小步推进，子弹在每一步只走基础距离。
        int step = Layout.BULLET_SPEED;
        x = x + step;
        // 逐渐把高度调到目标行，避免子弹在行之间瞬移。
        if (y < targetY) {
            y = Math.min(targetY, y + step);
        } else if (y > targetY) {
            y = Math.max(targetY, y - step);
        }
        // 飞出屏幕右边就不用再算了。
        if (x > Layout.WINDOW_WIDTH) {
            alive = false;
        }
    }

    /**
     * 打中僵尸，换成原版的爆炸图。
     *
     * 参数：assets 提供图片；time 是打中的时刻。
     */
    // TODO：【选做-2.75】新增植物时植物子弹的特殊效果（二）（必须和 ShooterActions 的 bulletNameFor() 中返回的字符串一致）
    public void explode(Assets assets, long time) {
        exploded = true;
        hitTime = time;
        String next = "PeaNormalExplode";
        if (name.equals("BulletMushRoom")) {
            next = "BulletMushRoomExplode";
        }
        change(next, assets, time);
    }
}
