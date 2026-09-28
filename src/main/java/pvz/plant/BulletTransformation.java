package pvz.plant;

/**
 * 记录一种植物如何改变经过它的子弹。
 *
 * 一条记录只描述来源子弹、转换后的子弹、伤害倍率和触发区域。
 * 游戏主循环只读取这些资料，不需要知道具体是哪一种植物。
 */
public class BulletTransformation {
    /** 触发转换的子弹名字。 */
    public final String sourceBullet;

    /** 转换完成后的子弹名字。 */
    public final String targetBullet;

    /** 转换完成后使用的伤害倍率。 */
    public final int damageMultiplier;

    /** 触发区域相对植物图片左边的横坐标。 */
    public final int zoneLeft;

    /** 触发区域相对植物图片上边的纵坐标。 */
    public final int zoneTop;

    /** 触发区域的宽度。 */
    public final int zoneWidth;

    /** 触发区域的高度。 */
    public final int zoneHeight;

    /**
     * 创建一条子弹转换资料。
     *
     * 参数：sourceBullet 是原来的子弹；targetBullet 是转换后的子弹；
     * damageMultiplier 是伤害倍率；其余参数是植物图片中的触发区域。
     */
    public BulletTransformation(String sourceBullet, String targetBullet,
            int damageMultiplier,
            int zoneLeft, int zoneTop, int zoneWidth, int zoneHeight) {
        this.sourceBullet = sourceBullet;
        this.targetBullet = targetBullet;
        this.damageMultiplier = damageMultiplier;
        this.zoneLeft = zoneLeft;
        this.zoneTop = zoneTop;
        this.zoneWidth = zoneWidth;
        this.zoneHeight = zoneHeight;
    }
}
