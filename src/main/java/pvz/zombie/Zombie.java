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

    /**
     * 手臂是否已经被打断。
     *
     * 普通僵尸、路障和铁桶的本体掉到一半血时会掉臂，之后换成独臂那套动画。
     * 报纸僵尸和旗帜僵尸没有独臂素材，这个标记对它们始终是假。
     */
    public boolean armLost;

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

    /** 失去头盔或报纸后每走一步前进的像素。 */
    public int speedAfterHelmet = 1;

    /** 是否正在播放小丑爆炸动画。 */
    public boolean exploding;

    /** 是否已经执行过小丑爆炸效果，防止重复爆炸。 */
    public boolean explosionTriggered;

    /**
     * 创建一只僵尸，从屏幕右侧进场。
     *
     * 参数：kind 是品种名；lane 是所在行；bottom 是脚下的纵坐标；assets 提供图片。
     */
    public Zombie(String kind, int lane, int bottom, Assets assets) {
        super(kind, Layout.ZOMBIE_START_X, bottom, lane,
            ZombieCatalog.definitionOf(kind).maxHealth, assets);
        ZombieDefinition definition = ZombieCatalog.definitionOf(kind);
        BufferedImage image = picture(assets, 0);
        x = Layout.ZOMBIE_START_X - image.getWidth() / 2.0;
        y = bottom - image.getHeight();

        helmet = definition.helmet;
        speed = definition.speed;
        speedAfterHelmet = definition.speedAfterHelmet;
        frameInterval = (int) Layout.ZOMBIE_ANIMATION_INTERVAL;
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
        if (hasAbility(ZombieAbility.EXPLODES_ON_PLANT)) {
            if (exploding) {
                return "JokerZombieExplode";
            }
            return "JokerZombie";
        }

        // 还戴着帽子的僵尸，动图已经把帽子画进身体里了，不用换名字。
        if (helmet) {
            if (fight) {
                return name + "Attack";
            }
            return name;
        }

        // 掉了臂的普通僵尸、路障和铁桶换成独臂那套图。
        // 注意放在 helmet 判断之后：还戴着帽子时用的是把帽子画进身体的那套图，
        // 那时还没有独臂素材可用。
        if (hasNoArmArt()) {
            String base = "ZombieNoArm";
            if (headLost) {
                base = base + "LostHead";
            }
            if (fight) {
                base = base + "Attack";
            }
            return base;
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
     * 这只僵尸有没有独臂素材可用。
     *
     * 只有普通僵尸、路障和铁桶做了独臂图；旗帜僵尸和报纸僵尸没有，
     * 它们掉臂后只能继续用原来的动画。
     *
     * 返回：掉了臂并且有独臂图时返回真。
     */
    private boolean hasNoArmArt() {
        if (!armLost) {
            return false;
        }
        return ZombieCatalog.definitionOf(name).hasNoArmArt;
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

        // 小丑无论因为什么死亡，都使用自己的爆炸动画。
        if (hasAbility(ZombieAbility.EXPLODES_ON_PLANT)) {
            exploding = true;
            frameInterval = animationIntervalFor("JokerZombieExplode");
            change("JokerZombieExplode", assets, time);
            return;
        }

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
        // 独臂僵尸有自己的一套倒地动作；被炸死时仍然用通用的灰烬图，
        // 因为那套图是所有僵尸共用的。
        if (!explosion && hasNoArmArt()) {
            next = "ZombieNoArmDie";
        }

        frameInterval = animationIntervalFor(next);
        change(next, assets, time);
    }

    /**
     * 判断这只僵尸是否拥有指定能力。
     *
     * 参数：ability 是要查询的能力。
     * 返回：拥有该能力时返回真。
     */
    public boolean hasAbility(ZombieAbility ability) {
        ZombieDefinition definition = ZombieCatalog.definitionOf(name);
        return definition.ability == ability;
    }

    /**
     * 算出某段动画该用多少毫秒一帧。
     *
     * 独臂那套素材是按 80 毫秒一帧导出的（掉头后啃那张更快，40 毫秒），
     * 而原版僵尸走路是 150、啃食是 100、倒地是 200。统一成一个值会让
     * 独臂僵尸比原版慢一倍，所以这里按动画名分别给值。
     *
     * 参数：animation 是动画名。
     * 返回：该动画的帧间隔毫秒数。
     */
    public static int animationIntervalFor(String animation) {
        if (animation.equals("JokerZombieExplode")) {
            return (int) Layout.DEFAULT_ANIMATION_INTERVAL;
        }

        // 独臂素材：只有"掉头后啃"那张是 40 毫秒，别的都是 80。
        if (animation.equals("ZombieNoArmLostHeadAttack")) {
            return Layout.ZOMBIE_NO_ARM_FAST_INTERVAL;
        }
        if (animation.startsWith("ZombieNoArm")) {
            return Layout.ZOMBIE_NO_ARM_ANIMATION_INTERVAL;
        }
        // 倒地那几段（含被炸死的灰烬图）播得慢一些。
        if (animation.equals("ZombieDie") || animation.equals("NewspaperZombieDie")
                || animation.equals("BoomDie") || animation.equals("NewspaperZombieBoomDie")) {
            return (int) Layout.ZOMBIE_DIE_ANIMATION_INTERVAL;
        }
        // 剩下的"啃食"那几段统一是 Attack 结尾。
        if (animation.endsWith("Attack")) {
            return (int) Layout.ZOMBIE_ATTACK_ANIMATION_INTERVAL;
        }
        return (int) Layout.ZOMBIE_ANIMATION_INTERVAL;
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
