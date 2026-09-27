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
     * 种植时图片要往右挪多少像素，根部才正好落在格子中心。
     *
     * 大多数植物的动图里根部就在正中，填 0 即可。大嘴花的图右边留了一大片空白
     * 给"往前扑咬"的动作，根部偏在左边，不挪的话整株会往左偏出小半格。
     */
    public final int rootShift;

    /**
     * 种植时图片要往上挪多少像素（负数表示向上）。
     *
     * 大多数植物填 0。窝瓜的图比其他植物高，用默认的落脚点会让它底部超出格子。
     */
    public final int verticalShift;

    /**
     * 发动攻击时播的动画名；null 表示这种植物没有攻击动作。
     *
     * 正在播这个动画，就说明它这一下已经打出去了、收不回来。
     * 大嘴花和窝瓜在这段时间里不该被爆炸僵尸打断，所以这条也用来判断"能不能被打断"。
     */
    public final String attackAnimation;

    /**
     * 创建一种植物的固定资料，位置不需要额外偏移，也没有攻击动画。
     *
     * 参数：name 是动画名；cardPicture 是卡片图名；cost 是阳光花费；
     * cooldown 是冷却时间；maxHealth 是初始血量；sleepsAtDay 表示白天是否睡觉；
     * canBeEaten 表示能否被僵尸吃掉；actionType 是行为类别；bowling 表示是否为保龄球。
     */
    public PlantDefinition(String name, String cardPicture, int cost, int cooldown,
            int maxHealth, boolean sleepsAtDay, boolean canBeEaten,
            PlantActionType actionType, boolean bowling) {
        this(name, cardPicture, cost, cooldown, maxHealth, sleepsAtDay, canBeEaten,
            actionType, bowling, 0, 0, null);
    }

    /**
     * 创建一种植物的固定资料，并指定绘制偏移。
     *
     * 参数：前面几个和上面一样；rootShift 是向右挪的像素；
     * verticalShift 是向上挪的像素（负数表示向上）。
     */
    public PlantDefinition(String name, String cardPicture, int cost, int cooldown,
            int maxHealth, boolean sleepsAtDay, boolean canBeEaten,
            PlantActionType actionType, boolean bowling, int rootShift, int verticalShift) {
        this(name, cardPicture, cost, cooldown, maxHealth, sleepsAtDay, canBeEaten,
            actionType, bowling, rootShift, verticalShift, null);
    }

    /**
     * 创建一种植物的完整固定资料。
     *
     * 参数：前面几个和上面一样；attackAnimation 是发动攻击时播的动画名，没有就传 null。
     */
    public PlantDefinition(String name, String cardPicture, int cost, int cooldown,
            int maxHealth, boolean sleepsAtDay, boolean canBeEaten,
            PlantActionType actionType, boolean bowling, int rootShift, int verticalShift,
            String attackAnimation) {
        this.name = name;
        this.cardPicture = cardPicture;
        this.cost = cost;
        this.cooldown = cooldown;
        this.maxHealth = maxHealth;
        this.sleepsAtDay = sleepsAtDay;
        this.canBeEaten = canBeEaten;
        this.actionType = actionType;
        this.bowling = bowling;
        this.rootShift = rootShift;
        this.verticalShift = verticalShift;
        this.attackAnimation = attackAnimation;
    }
}
