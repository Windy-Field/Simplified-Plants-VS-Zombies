package pvz.plant;

/**
 * 一种植物的固定资料。
 *
 * 这里保存不会随着一局游戏改变的数据，植物对象本身只保存位置、血量和运行状态。
 */
public class PlantDefinition {
    /** 植物动画名，也是植物的内部名字。 */
    public final String name;

    /** 卡片图片名，不包含文件扩展名。 */
    public final String cardPicture;

    /** 正常选卡模式种植需要的阳光。 */
    public final int cost;

    /** 正常选卡模式再次使用这张卡前需要等待的毫秒数。 */
    public final int cooldown;

    /** 植物的初始血量。 */
    public final int maxHealth;

    /** 植物白天是否睡觉。 */
    public final boolean sleepsAtDay;

    /** 僵尸是否可以吃掉它。 */
    public final boolean canBeEaten;

    /** 植物每一帧所属的行为类别。 */
    public final PlantActionType actionType;

    /** 是否是保龄球植物。 */
    public final boolean bowling;

    /**
     * 创建一种植物的固定资料。
     *
     * 参数：name 是动画名；cardPicture 是卡片图名；cost 是阳光花费；
     * cooldown 是冷却时间；maxHealth 是初始血量；sleepsAtDay 表示白天是否睡觉；
     * canBeEaten 表示能否被僵尸吃掉；actionType 是行为类别；bowling 表示是否为保龄球。
     */
    public PlantDefinition(String name, String cardPicture, int cost, int cooldown,
            int maxHealth, boolean sleepsAtDay, boolean canBeEaten,
            PlantActionType actionType, boolean bowling) {
        this.name = name;
        this.cardPicture = cardPicture;
        this.cost = cost;
        this.cooldown = cooldown;
        this.maxHealth = maxHealth;
        this.sleepsAtDay = sleepsAtDay;
        this.canBeEaten = canBeEaten;
        this.actionType = actionType;
        this.bowling = bowling;
    }
}
