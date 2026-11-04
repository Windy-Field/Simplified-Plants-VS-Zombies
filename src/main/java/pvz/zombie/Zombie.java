package pvz.zombie;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.world.Sprite;

/**
 * 一只从右边走过来的僵尸
 *
 * 僵尸比植物复杂的地方在于它会换装：戴帽子的、拿报纸的、掉头的，
 * 每种状态都对应一段不同的动图，要用 stateAnimation 统一算出来
 */
public class Zombie extends Sprite {
    /** 是否还戴着帽子（铁桶、路障或报纸） */
    public boolean helmet;

    /**
     * 手臂是否已经被打断
     *
     * 普通僵尸、路障和铁桶的本体掉到一半血时会掉臂，之后换成独臂那套动画
     * 普通受伤流程都会记录这个状态，是否换独臂动画还取决于资料中的 hasNoArmArt
     */
    public boolean armLost;

    /** 头是否已经被打掉；掉头后换成 LostHead 系列动画，并开始持续流血 */
    public boolean headLost;

    /** 上一次因为掉头而流血的时刻 */
    public long lastBleed;

    /** 是否被魅惑菇反转了阵营，会反过来打僵尸 */
    public boolean hypno;

    /** 当前是否正在攻击植物或敌方僵尸，巨人使用挥棒动作 */
    public boolean attacking;

    /** 是否正在播放死亡动画 */
    public boolean dying;

    /** 上一次走一步的时刻 */
    public long lastStep;

    /** 上一次咬一口的时刻 */
    public long lastAttack;

    /** 被寒冰射手打中后，减速状态持续到这个时刻 */
    public long slowedUntil;

    /** 被寒冰菇冻住时，不能动直到这个时刻 */
    public long frozenUntil;

    /** 开始播放死亡动画的时刻 */
    public long deathTime;

    /** 每走一步前进的像素；报纸僵尸掉报纸后加速变快 */
    public double speed = 1;

    /** 失去头盔或报纸后每走一步前进的像素 */
    public double speedAfterHelmet = 1;

    /** 小丑开盒前的躯干位置，动画结束时用它确定爆炸中心 */
    public Rectangle explosionOrigin;

    /** 是否正在播放掉头后再进入死亡动画的过渡 */
    public boolean waitingForDeath;

    /** 掉头动画开始的时刻 */
    public long headLossTime;

    /** 掉头动画结束后是否使用普通爆炸死亡图 */
    public boolean pendingDeathExplosion;

    /** 是否已经进入最后的死亡动画 */
    public boolean deathAnimationStarted;

    /** 巨人是否已经用掉背上的小鬼，每只巨人最多投掷一次 */
    public boolean impThrown;

    /** 巨人是否正在播放投掷动作 */
    public boolean throwingImp;

    /** 巨人当前这次挥棒是否已经造成伤害，防止一段动画重复砸击 */
    public boolean smashResolved;

    /** 投掷或砸击动作已经播放的游戏时间，冻结期间不增加 */
    public long actionElapsed;

    /** 上次更新巨人动作的游戏时刻 */
    public long lastActionUpdate;

    /** 小鬼是否还在空中；落地前不能攻击，也不参与地面碰撞 */
    public boolean airborne;

    /** 小鬼飞行已经经过的游戏时间 */
    public long flightElapsed;

    /** 上次更新小鬼飞行的游戏时刻 */
    public long lastFlightUpdate;

    /** 小鬼飞行的起点和落点，单位是屏幕像素 */
    public double flightStartX;
    public double flightStartY;
    public double flightTargetX;
    public double flightTargetY;

    /**
     * 创建一只僵尸，从屏幕右侧进场
     *
     * 参数：kind 是品种名；row 是所在行；bottom 是脚下的纵坐标；assets 提供图片
     */
    public Zombie(String kind, int row, int bottom, Assets assets) {
        super(kind, Layout.ZOMBIE_START_X, bottom, row,
            ZombieCatalog.definitionOf(kind).maxHealth, assets);
        ZombieDefinition definition = ZombieCatalog.definitionOf(kind);
        BufferedImage image = picture(assets, 0);
        x = Layout.ZOMBIE_START_X - image.getWidth() / 2.0;
        y = bottom - image.getHeight();

        if (isGargantuar()) {
            // 巨人画布留白较大，按实际脚底对齐地面，不能按整张画布底边对齐
            Rectangle body = bounds(assets, 0);
            y = y + bottom - body.getMaxY();
        }

        helmet = definition.helmet;
        speed = definition.speed;
        speedAfterHelmet = definition.speedAfterHelmet;
        frameInterval = animationIntervalFor(animation);
    }

    /**
     * 根据当前状态挑出该播哪个动画
     *
     * 参数：fight 表示此刻是否正在啃植物或打僵尸
     * 返回：对应的动画名
     */
    public String stateAnimation(boolean fight) {
        // TODO【15-僵尸状态动画】：换装动画先通过 ZombieDefinition 的 helmetLostAnimation 和 headLostAnimation 配置
        // 只有资料表达不了的状态切换才在这里增加判断，并确认行走和攻击两种动画都已登记
        if (isGargantuar()) {
            String base = "Gargantuar";
            if (impThrown) {
                base = "GargantuarNoImp";
            }
            return animationWithAttack(base, fight);
        }
        if (isImp()) {
            return animationWithAttack("Imp", fight);
        }
        ZombieDefinition zombieDefinition = definition();
        // 有专属立绘的品种（比如小丑）先用它，不走后面那套"名字加后缀"的规律
        if (zombieDefinition.idleAnimation != null) {
            return zombieDefinition.idleAnimation;
        }

        // 还戴着头盔的僵尸，动图已经把头盔画进身体里了
        if (helmet) {
            return animationWithAttack(name, fight);
        }

        // 掉了臂的普通僵尸、路障和铁桶换成独臂那套图
        // 注意放在 helmet 判断之后：还戴着帽子时用的是把帽子画进身体的那套图，
        // 那时还没有独臂素材可用
        if (hasNoArmArt()) {
            String base = "ZombieNoArm";
            if (headLost) {
                base = base + "LostHead";
            }
            return animationWithAttack(base, fight);
        }

        String base = name;
        if (zombieDefinition.helmetLostAnimation != null) {
            base = zombieDefinition.helmetLostAnimation;
        } else if (name.equals("ConeheadZombie") || name.equals("BucketheadZombie")) {
            // 路障和铁桶没有掉帽子后的专属图，用普通僵尸的身体
            base = "Zombie";
        }
        // 报纸僵尸掉报纸之后要用 NoPaper 那套图
        if (name.equals("NewspaperZombie") && !headLost) {
            base = "NewspaperZombieNoPaper";
        }
        if (headLost) {
            if (zombieDefinition.headLostAnimation != null) {
                base = zombieDefinition.headLostAnimation;
            } else {
                base = base + "LostHead";
            }
        }
        return animationWithAttack(base, fight);
    }

    /**
     * 根据是否正在啃食，返回基础动画名或对应的攻击动画名
     *
     * 参数：base 是基础动画名；fight 表示是否正在攻击
     * 返回：应该播放的动画名
     */
    private String animationWithAttack(String base, boolean fight) {
        if (fight) {
            return base + "Attack";
        }
        return base;
    }

    /**
     * 这只僵尸有没有独臂素材可用
     *
     * 只有普通僵尸、路障和铁桶做了独臂图；旗帜僵尸和报纸僵尸没有，
     * 它们掉臂后只能继续用原来的动画
     *
     * 返回：掉了臂并且有独臂图时返回真
     */
    private boolean hasNoArmArt() {
        if (!armLost) {
            return false;
        }
        return ZombieCatalog.definitionOf(name).hasNoArmArt;
    }

    /**
     * 让僵尸开始播放死亡动画，动画放完才真正消失
     *
     * 参数：assets 提供图片；time 是当前时刻；explosion 表示是不是被炸死的，
     *       被炸死要换成 BoomDie 那套灰烬图
     */
    public void die(Assets assets, long time, boolean explosion) {
        // TODO【16-僵尸死亡动画】：普通专属死亡图按内部名加 Die 或 BoomDie 登记，deathAnimation 会自动查找
        // abilityAnimation 会替换普通死亡流程，只有开盒等特殊能力需要它，新增能力还要在行为代码中实现效果
        if (dying && deathAnimationStarted) {
            return;
        }
        dying = true;
        waitingForDeath = false;
        deathAnimationStarted = true;
        attacking = false;
        throwingImp = false;
        airborne = false;
        deathTime = time;

        if (isGargantuar()) {
            String next = "GargantuarDie";
            if (impThrown) {
                next = "GargantuarNoImpDie";
            }
            frameInterval = animationIntervalFor(next);
            change(next, assets, time);
            return;
        }

        // 资料里写了专属死亡动画的品种（比如小丑），无论因为什么死亡都先播它
        String replacement = definition().abilityAnimation;
        if (replacement != null) {
            frameInterval = animationIntervalFor(replacement);
            change(replacement, assets, time);
            return;
        }

        String next = deathAnimation(assets, explosion);
        // 独臂僵尸有自己的一套倒地动作；被炸死时仍然用通用的灰烬图，
        // 因为那套图是所有僵尸共用的
        if (!explosion && hasNoArmArt()) {
            next = "ZombieNoArmDie";
        }

        frameInterval = animationIntervalFor(next);
        change(next, assets, time);
    }

    /**
     * 根据死亡方式选本品种的素材；缺少专属素材时使用通用素材
     *
     * 参数：assets 是已加载的素材；explosion 表示是否被炸死
     * 返回：应该播放的死亡动画名
     */
    private String deathAnimation(Assets assets, boolean explosion) {
        String suffix = "Die";
        if (explosion) {
            suffix = "BoomDie";
        }
        String preferred = name + suffix;
        if (assets.hasAnimation(preferred)) {
            return preferred;
        }
        if (explosion) {
            return "BoomDie";
        }
        return "ZombieDie";
    }

    /**
     * 这只僵尸有没有自己的专属动画
     *
     * 返回是否配置了替代普通死亡过程的能力动画，例如小丑开盒
     * 这类品种跳过普通受伤状态，能力动画结束后由 ZombieEffects 结算效果
     *
     * 返回：资料里登记了专属动画就返回真
     */
    public boolean hasOwnAnimation() {
        return definition().abilityAnimation != null;
    }

    /** 返回这只僵尸是否使用巨人的砸击和投掷规则 */
    public boolean isGargantuar() {
        return name.equals("Gargantuar");
    }

    /** 返回这只僵尸是否是小鬼 */
    public boolean isImp() {
        return name.equals("Imp");
    }

    /** 返回是否使用普通僵尸的掉臂、掉头流程；巨人和小鬼直接播放自己的死亡图 */
    public boolean usesStandardDamageStates() {
        return !hasOwnAnimation() && !isGargantuar() && !isImp();
    }

    /**
     * 取得这只僵尸的固定资料
     *
     * 按名字查询血量、速度和动画配置，避免每只僵尸重复保存固定资料
     * 巨人投掷等动作仍由 ZombieActions 中的具名方法处理
     *
     * 返回：该品种的固定资料
     */
    private ZombieDefinition definition() {
        return ZombieCatalog.definitionOf(name);
    }

    /**
     * 算出某段动画该用多少毫秒一帧
     *
     * 独臂那套素材是按 80 毫秒一帧导出的（掉头后啃那张更快，40 毫秒），
     * 而原版僵尸走路是 150、啃食是 100、倒地是 200。统一成一个值会让
     * 独臂僵尸比原版慢一倍，所以这里按动画名分别给值
     *
     * 参数：animation 是动画名
     * 返回：该动画的帧间隔毫秒数
     */
    public static int animationIntervalFor(String animation) {
        if (animation.startsWith("Gargantuar")) {
            return Layout.GARGANTUAR_FRAME_INTERVAL;
        }
        if (animation.equals("JokerZombieExplode")) {
            return (int) Layout.DEFAULT_ANIMATION_INTERVAL;
        }

        // 独臂素材：只有"掉头后啃"那张是 40 毫秒，别的都是 80
        if (animation.equals("ZombieNoArmLostHeadAttack")) {
            return Layout.ZOMBIE_NO_ARM_FAST_INTERVAL;
        }
        if (animation.startsWith("ZombieNoArm")) {
            return Layout.ZOMBIE_NO_ARM_ANIMATION_INTERVAL;
        }
        // 倒地那几段（含被炸死的灰烬图）播得慢一些
        if (animation.endsWith("Die")) {
            return (int) Layout.ZOMBIE_DIE_ANIMATION_INTERVAL;
        }
        // 剩下的"啃食"那几段统一是 Attack 结尾
        if (animation.endsWith("Attack")) {
            return (int) Layout.ZOMBIE_ATTACK_ANIMATION_INTERVAL;
        }
        return (int) Layout.ZOMBIE_ANIMATION_INTERVAL;
    }

    /**
     * 算出僵尸用于碰撞检测的矩形：只算躯干，不算往前伸的脑袋和手臂
     *
     * 僵尸走路时脑袋和手臂探在身体前面，如果把它们也算进去，
     * 脸一碰到植物的叶子就停下开啃，豌豆也是一挨到脸的前沿就炸开，
     * 看起来都"提前"了。所以从看得见的范围里，前面去掉一截、后面去掉一小截，只留躯干
     *
     * 另外，不管此刻是在走还是在啃，都按"走路"那套动画的范围来算
     * 走路图和啃食图的轮廓宽窄略有不同，如果碰撞盒跟着换，
     * 刚切到啃食就可能"松开"植物、切回走路又立刻碰上，
     * 啃食动画反复从头播放，僵尸看起来一直在颤抖，也咬不到植物
     *
     * 参数：assets 提供图片；time 是当前时刻
     * 返回：躯干范围对应的矩形
     */
    public Rectangle collisionBox(Assets assets, long time) {
        if (airborne) {
            return new Rectangle();
        }
        if (dying) {
            return bounds(assets, time);
        }
        String walking = stateAnimation(false);
        if (isGargantuar()) {
            // 投出小鬼后身体位置不变，碰撞盒继续用同一段行走图，避免换图时跳动
            walking = "Gargantuar";
        }
        int[] visible = assets.animationBounds(walking);
        int visibleLeft = visible[0];
        int visibleTop = visible[1];
        int visibleRight = visible[2];
        int visibleBottom = visible[3];
        int width = visibleRight - visibleLeft;

        // 正常僵尸面朝左，脑袋和手臂在左边；被魅惑的僵尸画的时候左右翻转了，前面就变成右边
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
     * 画出僵尸；被魅惑的僵尸要水平翻转，让玩家一眼看出它叛变了
     *
     * 参数：painter 是画笔；assets 提供图片；time 是当前时刻
     */
    public void draw(Graphics2D painter, Assets assets, long time) {
        if (!hypno) {
            super.draw(painter, assets, time);
            return;
        }
        BufferedImage image = picture(assets, time);
        // 宽度取负就能左右翻转，此时传入的横坐标是翻转后图片的右边缘
        // 动图里身体不一定在画布正中，所以要绕"身体中线"翻转，翻转后身体才停在原地；
        // 中线取整段动画的总范围，不随帧变化，翻转后的画面才不会左右晃
        int[] visible = assets.animationBounds(animation);
        int visibleLeft = visible[0];
        int visibleRight = visible[2];
        // 身体中线在屏幕上的横坐标
        double bodyCenter = x + (visibleLeft + visibleRight) / 2.0 * scale;
        // 图片左边缘到中线的距离，翻转后变成中线到右边缘的距离
        double halfSpan = bodyCenter - x;
        int flippedRight = (int) (bodyCenter + halfSpan);
        painter.drawImage(image, flippedRight, (int) y, -image.getWidth(), image.getHeight(), null);
    }
}
