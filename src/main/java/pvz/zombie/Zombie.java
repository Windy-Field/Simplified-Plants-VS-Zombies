package pvz.zombie;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import pvz.plant.Plant;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.world.Sprite;

/**
 * 一只从右边走过来的僵尸。
 *
 * 僵尸比植物复杂的地方在于它会换装：戴帽子的、拿报纸的、掉头的，
 * 每种状态都对应一段不同的动图，要用 stateAnimation 统一算出来。
 */
public class Zombie extends Sprite {
    /** 是否还戴着帽子（铁桶、路障或报纸）。 */
    public boolean helmet;

    /** 头是否已经被打掉；掉头后换成 LostHead 系列动画，并开始持续流血。 */
    public boolean headLost;

    /** 上一次因为掉头而流血的时刻。 */
    public long lastBleed;

    /** 是否被魅惑菇反转了阵营，会反过来打僵尸。 */
    public boolean hypno;

    /** 当前是否正在啃植物。 */
    public boolean attacking;

    /** 是否正在播放死亡动画。 */
    public boolean dying;

    /** 上一次走一步的时刻。 */
    public long lastStep;

    /** 上一次咬一口的时刻。 */
    public long lastAttack;

    /** 被寒冰射手打中后，减速状态持续到这个时刻。 */
    public long slowedUntil;

    /** 被寒冰菇冻住时，不能动直到这个时刻。 */
    public long frozenUntil;

    /** 开始播放死亡动画的时刻。 */
    public long deathTime;

    /** 正在啃的植物。 */
    public Plant prey;

    /** 被魅惑后正在攻击的另一只僵尸。 */
    public Zombie opponent;

    /** 每走一步前进的像素；报纸僵尸掉报纸后加速变快。 */
    public int speed = 1;

    /**
     * 创建一只僵尸，从屏幕右侧进场。
     *
     * 参数：kind 是品种名；lane 是所在行；bottom 是脚下的纵坐标；assets 提供图片。
     */
    // TODO：【必做-9】新增僵尸时可以改变僵尸血量（默认是 10，有帽子/盔甲就在这里加判断设 helmet = true）
    public Zombie(String kind, int lane, int bottom, Assets assets) {
        super(kind, Layout.ZOMBIE_START_X, bottom, lane, 10, assets);
        BufferedImage image = picture(assets, 0);
        x = Layout.ZOMBIE_START_X - image.getWidth() / 2.0;
        y = bottom - image.getHeight();

        // 路障和铁桶多一层血，多出来的部分就是头上那顶帽子。
        if (kind.equals("ConeheadZombie")) {
            health = 20;
            helmet = true;
        }
        if (kind.equals("BucketheadZombie")) {
            health = 30;
            helmet = true;
        }
        if (kind.equals("FlagZombie") || kind.equals("NewspaperZombie")) {
            health = 15;
        }
        // 报纸僵尸手里的报纸相当于一顶帽子，被打掉之后换成没报纸的动画。
        if (kind.equals("NewspaperZombie")) {
            helmet = true;
        }
        interval = (int) Layout.ZOMBIE_ANIMATION_INTERVAL;
    }

    /**
     * 根据当前状态挑出该播哪个动画。
     *
     * 参数：fight 表示此刻是否正在啃植物或打僵尸。
     * 返回：对应的动画名。
     */
    // TODO：【选做-6】新增僵尸时如果换装规则和默认不同（比如掉帽子后用特殊动画而非退化成普通僵尸），
    //                需要在这里加判断返回正确的动画名
    public String stateAnimation(boolean fight) {
        // 还戴着帽子的僵尸，动图已经把帽子画进身体里了，不用换名字。
        if (helmet) {
            if (fight) {
                return name + "Attack";
            }
            return name;
        }

        String base = name;
        // 帽子和身体是分开的图，掉了帽子之后就退化成普通僵尸。
        if (name.equals("ConeheadZombie") || name.equals("BucketheadZombie")) {
            base = "Zombie";
        }
        // 报纸僵尸掉报纸之后要用 NoPaper 那套图。
        if (name.equals("NewspaperZombie") && !headLost) {
            base = "NewspaperZombieNoPaper";
        }
        if (headLost) {
            base = base + "LostHead";
        }
        if (fight) {
            base = base + "Attack";
        }
        return base;
    }

    /**
     * 让僵尸开始播放死亡动画，动画放完才真正消失。
     *
     * 参数：assets 提供图片；time 是当前时刻；explosion 表示是不是被炸死的，
     *       被炸死要换成 BoomDie 那套灰烬图。
     */
    // TODO：【选做-9】新增僵尸时如果有专属死亡动画（比如机器人断成两截、巨人倒地砸坑），
    //                需要在这里加判断返回正确的死亡动画名
    public void die(Assets assets, long time, boolean explosion) {
        if (dying) {
            return;
        }
        dying = true;
        attacking = false;
        deathTime = time;
        interval = (int) Layout.ZOMBIE_DIE_ANIMATION_INTERVAL;

        String next = "ZombieDie";
        if (name.equals("NewspaperZombie")) {
            next = "NewspaperZombieDie";
        }
        if (explosion) {
            next = "BoomDie";
            // 报纸僵尸有自己的一套灰烬图，画布大小和它的其他动画一致。
            if (name.equals("NewspaperZombie")) {
                next = "NewspaperZombieBoomDie";
            }
        }
        change(next, assets, time);
    }

    /**
     * 算出僵尸用于碰撞检测的矩形：只算躯干，不算往前伸的脑袋和手臂。
     *
     * 僵尸走路时脑袋和手臂探在身体前面，如果把它们也算进去，
     * 脸一碰到植物的叶子就停下开啃，豌豆也是一挨到脸的前沿就炸开，
     * 看起来都"提前"了。所以从看得见的范围里，前面去掉一截、后面去掉一小截，只留躯干。
     *
     * 另外，不管此刻是在走还是在啃，都按"走路"那套动画的范围来算。
     * 走路图和啃食图的轮廓宽窄略有不同，如果碰撞盒跟着换，
     * 刚切到啃食就可能"松开"植物、切回走路又立刻碰上，
     * 啃食动画反复从头播放，僵尸看起来一直在颤抖，也咬不到植物。
     *
     * 参数：assets 提供图片；time 是当前时刻。
     * 返回：躯干范围对应的矩形。
     */
    public Rectangle collisionBox(Assets assets, long time) {
        if (dying) {
            return bounds(assets, time);
        }
        int[] visible = assets.animationBounds(stateAnimation(false));
        int visibleLeft = visible[0];
        int visibleTop = visible[1];
        int visibleRight = visible[2];
        int visibleBottom = visible[3];
        int width = visibleRight - visibleLeft;

        // 正常僵尸面朝左，脑袋和手臂在左边；被魅惑的僵尸画的时候左右翻转了，前面就变成右边。
        int leftTrim = width * Layout.ZOMBIE_FRONT_TRIM_PERCENT / 100;
        int rightTrim = width * Layout.ZOMBIE_BACK_TRIM_PERCENT / 100;
        if (hypno) {
            leftTrim = width * Layout.ZOMBIE_BACK_TRIM_PERCENT / 100;
            rightTrim = width * Layout.ZOMBIE_FRONT_TRIM_PERCENT / 100;
        }

        int[] body = {visibleLeft + leftTrim, visibleTop, visibleRight - rightTrim, visibleBottom};
        return placeBox(body);
    }

    /**
     * 画出僵尸；被魅惑的僵尸要水平翻转，让玩家一眼看出它叛变了。
     *
     * 参数：painter 是画笔；assets 提供图片；time 是当前时刻。
     */
    public void draw(Graphics2D painter, Assets assets, long time) {
        if (!hypno) {
            super.draw(painter, assets, time);
            return;
        }
        BufferedImage image = picture(assets, time);
        // 宽度取负就能左右翻转，此时传入的横坐标是翻转后图片的右边缘。
        // 动图里身体不一定在画布正中，所以要绕"身体中线"翻转，翻转后身体才停在原地；
        // 中线取整段动画的总范围，不随帧变化，翻转后的画面才不会左右晃。
        int[] visible = assets.animationBounds(animation);
        int visibleLeft = visible[0];
        int visibleRight = visible[2];
        // 身体中线在屏幕上的横坐标。
        double bodyCenter = x + (visibleLeft + visibleRight) / 2.0 * scale;
        // 图片左边缘到中线的距离，翻转后变成中线到右边缘的距离。
        double halfSpan = bodyCenter - x;
        int flippedRight = (int) (bodyCenter + halfSpan);
        painter.drawImage(image, flippedRight, (int) y, -image.getWidth(), image.getHeight(), null);
    }
}
