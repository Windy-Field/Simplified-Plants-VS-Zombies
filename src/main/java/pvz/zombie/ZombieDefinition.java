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
    public final int speed;

    /** 僵尸失去头盔或报纸后使用的速度。 */
    public final int speedAfterHelmet;

    /**
     * 创建一种僵尸的固定资料。
     *
     * 参数：name 是内部名字；maxHealth 是初始血量；helmet 表示是否戴帽子；
     * hasNoArmArt 表示是否有独臂动画；speed 是初始每步移动像素；
     * speedAfterHelmet 是失去头盔或报纸后的每步移动像素。
     */
    public ZombieDefinition(String name, int maxHealth, boolean helmet,
            boolean hasNoArmArt, int speed, int speedAfterHelmet) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.helmet = helmet;
        this.hasNoArmArt = hasNoArmArt;
        this.speed = speed;
        this.speedAfterHelmet = speedAfterHelmet;
    }
}
