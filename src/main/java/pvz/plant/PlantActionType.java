package pvz.plant;

/**
 * 植物每一帧要执行的行为类别。
 *
 * 资料表只保存类别，具体动作交给对应的行为处理类完成。
 */
public enum PlantActionType {
    /** 不需要主动行为的植物。 */
    NONE,

    /** 会产生阳光的植物。 */
    SUN_PRODUCER,

    /** 会发射子弹的植物。 */
    SHOOTER,

    /** 会根据血量切换裂纹图的坚果。 */
    WALL_NUT,

    /** 种下后马上触发一次效果的植物。 */
    INSTANT,

    /** 需要等僵尸靠近才生效的植物。 */
    CLOSE_ATTACK
}
