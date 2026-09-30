package pvz.zombie;

/**
 * 一种僵尸的固定资料。
 *
 * 这里保存初始血量、帽子、移动速度等不会随单只僵尸改变的设定。
 */
public class ZombieDefinition {
    /** 僵尸内部名字，也是动画名的起点。 */
    public final String name;

    /** 僵尸初始血量。 */
    public final int maxHealth;

    /** 是否一开始戴着帽子或拿着报纸。 */
    public final boolean helmet;

    /** 是否拥有独臂动画。 */
    public final boolean hasNoArmArt;

    /** 僵尸每次走一步前进的像素。 */
    public final double speed;

    /** 僵尸失去头盔或报纸后使用的速度。 */
    public final double speedAfterHelmet;

    /**
     * 平常该播的动画名；普通僵尸就是自己的名字，填 null 表示用名字。
     *
     * 有专属走路图和普通僵尸不一样的品种（比如小丑）才需要单独填。
     */
    public final String idleAnimation;

    /**
     * 触发特殊能力时替换掉的死亡动画名；null 表示没有，
     * 按普通的掉头 → 倒地那套流程走。
     */
    public final String abilityAnimation;

    /** 失去头盔后播放的动画基础名；null 表示继续使用默认规则。 */
    public final String helmetLostAnimation;

    /** 掉头后播放的动画基础名；null 表示在原动画名后加 LostHead。 */
    public final String headLostAnimation;

    /**
     * 创建一种僵尸的完整固定资料。
     *
     * 参数：name 是内部名字；maxHealth 是初始血量；helmet 表示是否戴帽子；
     * hasNoArmArt 表示是否有独臂动画；speed 是初始每步移动像素；
     * speedAfterHelmet 是失去头盔或报纸后的每步移动像素；
     * idleAnimation 是平常该播的动画名（null 表示用名字）；
     * abilityAnimation 是触发特殊能力时替换掉的死亡动画名（null 表示没有）。
     */
    public ZombieDefinition(String name, int maxHealth, boolean helmet,
            boolean hasNoArmArt, double speed, double speedAfterHelmet,
            String idleAnimation, String abilityAnimation) {
        this(name, maxHealth, helmet, hasNoArmArt, speed, speedAfterHelmet,
            idleAnimation, abilityAnimation, null, null);
    }

    /**
     * 创建带有专属动画名称的僵尸资料。
     *
     * 参数：name、maxHealth、helmet、hasNoArmArt、speed、speedAfterHelmet、
     * idleAnimation 和 abilityAnimation 与普通构造方法相同；最后两项表示失去头盔
     * 和掉头后的动画基础名。
     */
    public ZombieDefinition(String name, int maxHealth, boolean helmet,
            boolean hasNoArmArt, double speed, double speedAfterHelmet,
            String idleAnimation, String abilityAnimation,
            String helmetLostAnimation, String headLostAnimation) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.helmet = helmet;
        this.hasNoArmArt = hasNoArmArt;
        this.speed = speed;
        this.speedAfterHelmet = speedAfterHelmet;
        this.idleAnimation = idleAnimation;
        this.abilityAnimation = abilityAnimation;
        this.helmetLostAnimation = helmetLostAnimation;
        this.headLostAnimation = headLostAnimation;
    }
}
