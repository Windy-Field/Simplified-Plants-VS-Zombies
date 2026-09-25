package pvz.plant;

import pvz.world.Assets;
import pvz.world.Layout;
import pvz.world.Sprite;
import pvz.zombie.Zombie;

/**
 * 一株种在草坪上的植物。
 *
 * 除了从父类继承的位置和血量，它还记住自己在第几列、什么时候被种下、
 * 上一次生效是什么时候，以及各种状态的开关。
 */
public class Plant extends Sprite {
    /** 上一次产生阳光或发射子弹的时刻。 */
    public long lastAction;

    /** 被种下的时刻，用来算"种下多久了"。 */
    public long placed;

    /** 当前状态（比如咬住、滚动）开始的时刻。 */
    public long stateStart;

    /** 白天是否在睡觉。睡觉的植物不干活。 */
    public boolean sleeping;

    /** 当前是否处于攻击动作中。 */
    public boolean attacking;

    /** 是否已经触发过它的特殊效果（比如窝瓜是否已经跳起来）。 */
    public boolean triggered;

    /** 所在的列号，释放种植格时要靠它。 */
    public int column;

    /** 窝瓜或食人花盯上的目标僵尸。 */
    public Zombie target;

    /**
     * 种下一株植物，并按品种设置血量、初始动画和透明色。
     *
     * 参数：kind 是品种名（如 "Peashooter"）；center 和 bottom 是期望的底边中央位置；
     *       lane 是所在行；gridColumn 是所在列；assets 提供图片；
     *       time 是种下的时刻；day 表示是不是白天关卡。
     */
    public Plant(String kind, int center, int bottom, int lane, int gridColumn,
                 Assets assets, long time, boolean day) {
        super(kind, center, bottom, lane, 5, assets);
        // 父类是按整张图的正中对准格子中心的，根部不在正中的植物要再挪一下。
        x = x + rootShift(kind);
        column = gridColumn;
        placed = time;
        stateStart = time;

        // 坚果是肉盾，血比普通植物厚得多。
        if (kind.equals("WallNut")) {
            health = 30;
        }
        // 魅惑菇和保龄球一碰就碎，血量设成 1 就够了。
        if (kind.equals("HypnoShroom") || PlantRules.isBowling(kind)) {
            health = 1;
        }
        // 蘑菇类在白天要睡觉，睡觉时换成带 Sleep 后缀的动画。
        if (day && PlantRules.sleepsAtDay(kind)) {
            sleeping = true;
            change(kind + "Sleep", assets, time);
        }
        // 土豆雷刚种下是埋着的，要用 Init 动画表示还没出土。
        if (kind.equals("PotatoMine")) {
            change("PotatoMineInit", assets, time);
        }
    }

    /**
     * 这种植物的图要往右挪多少，根部才正好落在格子中心。
     *
     * 大多数植物的动图里，根部就在图片正中，不用挪。
     * 大嘴花的图右边留了一大片空白给"往前扑咬"的动作，根部偏在左边，
     * 按图片正中摆放的话整株会往左偏出小半格。
     * 种植和画落点预览都要用它，两边才对得上。
     *
     * 参数：kind 是植物名。
     * 返回：向右挪的像素数。
     */
    public static int rootShift(String kind) {
        if (kind.equals("Chomper")) {
            return Layout.CHOMPER_ROOT_SHIFT;
        }
        return 0;
    }
}
