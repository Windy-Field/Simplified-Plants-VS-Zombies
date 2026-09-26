package pvz.zombie;

/**
 * 僵尸拥有的特殊能力类别。
 *
 * 普通僵尸使用 NONE；有特殊规则的僵尸在 ZombieDefinition 中填写对应能力。
 */
public enum ZombieAbility {
    /** 没有特殊能力。 */
    NONE,

    /** 碰到植物或死亡时会爆炸。 */
    EXPLODES_ON_PLANT
}
