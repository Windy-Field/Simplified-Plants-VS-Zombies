package pvz.world;

/**
 * 集中保存战斗中的血量和伤害数值。
 *
 * 所有基础血量和直接伤害都按 10 倍保存，这样以后可以使用整数表示更细的伤害。
 * 例如，原来的一点伤害现在写成 10 点，原来的十点血现在写成 100 点。
 */
public final class CombatValues {
    /** 血量统一放大十倍。 */
    public static final int HEALTH_MULTIPLIER = 10;

    /** 伤害统一放大十倍。 */
    public static final int DAMAGE_MULTIPLIER = 10;

    /** 普通植物的初始血量。 */
    public static final int DEFAULT_PLANT_HEALTH = 5 * HEALTH_MULTIPLIER;

    /** 坚果的初始血量。 */
    public static final int WALL_NUT_HEALTH = 30 * HEALTH_MULTIPLIER;

    /** 魅惑菇和保龄球的初始血量。 */
    public static final int FRAGILE_PLANT_HEALTH = 1 * HEALTH_MULTIPLIER;

    /** 普通僵尸的初始血量。 */
    public static final int NORMAL_ZOMBIE_HEALTH = 10 * HEALTH_MULTIPLIER;

    /** 僵尸失去头盔或报纸时的血量阈值。 */
    public static final int ZOMBIE_HELMET_LOST_HEALTH = 10 * HEALTH_MULTIPLIER;

    /** 坚果进入第二种裂纹状态时的血量阈值。 */
    public static final int WALL_NUT_FIRST_CRACK_HEALTH = 20 * HEALTH_MULTIPLIER;

    /** 坚果进入最严重裂纹状态时的血量阈值。 */
    public static final int WALL_NUT_SECOND_CRACK_HEALTH = 10 * HEALTH_MULTIPLIER;

    /** 路障僵尸的初始血量。 */
    public static final int CONEHEAD_ZOMBIE_HEALTH = 20 * HEALTH_MULTIPLIER;

    /** 铁桶僵尸的初始血量。 */
    public static final int BUCKETHEAD_ZOMBIE_HEALTH = 30 * HEALTH_MULTIPLIER;

    /** 旗帜僵尸和读报僵尸的初始血量。 */
    public static final int LIGHT_ARMOR_ZOMBIE_HEALTH = 15 * HEALTH_MULTIPLIER;

    /** 子弹每次命中的伤害。 */
    public static final int BULLET_DAMAGE = 1 * DAMAGE_MULTIPLIER;

    /** 僵尸每次啃咬造成的伤害。 */
    public static final int ZOMBIE_BITE_DAMAGE = 1 * DAMAGE_MULTIPLIER;

    /** 僵尸掉头后的持续流血伤害。 */
    public static final int ZOMBIE_BLEED_DAMAGE = 1 * DAMAGE_MULTIPLIER;

    /** 地刺每次扎出的伤害。 */
    public static final int SPIKEWEED_DAMAGE = 1 * DAMAGE_MULTIPLIER;

    /** 坚果保龄球每次撞击的伤害。 */
    public static final int BOWLING_DAMAGE = 10 * DAMAGE_MULTIPLIER;

    /** 小丑爆炸对魅惑僵尸造成的伤害。 */
    public static final int JOKER_EXPLOSION_DAMAGE = 10 * DAMAGE_MULTIPLIER;

    /** 这个类只提供常量，不允许创建对象。 */
    private CombatValues() {
    }
}
