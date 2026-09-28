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

    /** 这株植物可以触发的子弹转换资料。 */
    public final BulletTransformation[] bulletTransformations;

    /** 这株植物每次产出几颗阳光。 */
    public final int sunCount;

    /** 这株植物可以提前索敌的前方格子数。 */
    public final int forwardAttackRange;

    /**
     * 创建一种植物的完整固定资料。
     *
     * 参数：前面几个参数是植物基本资料；sunCount 是每次产出的阳光数；
     * forwardRange 是提前索敌的前方格子数；
     * rootShift 和 verticalShift 是绘制偏移；attackAnimation 是攻击动画，
     * 没有时传 null；transformations 是子弹转换列表。
     */
    public PlantDefinition(String name, String cardPicture, int cost, int cooldown,
            int maxHealth, boolean sleepsAtDay, boolean canBeEaten,
            PlantActionType actionType, boolean bowling, int sunCount,
            int forwardRange, int rootShift, int verticalShift, String attackAnimation,
            BulletTransformation[] transformations) {
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
        bulletTransformations = transformations;
        this.sunCount = sunCount;
        forwardAttackRange = forwardRange;
    }

    /**
     * 查找能处理指定子弹的转换资料。
     *
     * 参数：bulletName 是当前子弹名字。
     * 返回：找到转换规则时返回它，否则返回 null。
     */
    public BulletTransformation transformationFor(String bulletName) {
        for (int index = 0; index < bulletTransformations.length; index++) {
            BulletTransformation transformation = bulletTransformations[index];
            if (transformation.sourceBullet.equals(bulletName)) {
                return transformation;
            }
        }
        return null;
    }
}
