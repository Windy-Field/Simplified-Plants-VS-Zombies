package pvz.level;

import java.util.ArrayList;
import java.util.List;
import pvz.game.GameState;
import pvz.world.Layout;
import pvz.zombie.ZombieSpawn;

/**
 * 从关卡 JSON 里读出来的全部关卡数据。
 *
 * 一个关卡需要知道：用哪张背景图、卡槽是哪一种、开局给多少阳光、
 * 僵尸什么时候出场、传送带能出哪些卡。这些数据全部装在这个对象里。
 */
public class Level {
    /** 使用第几张背景图，对应 Background 动画的第几帧。 */
    public int backgroundIndex;

    /** 卡槽模式：0 是正常选卡，1 是传送带，2 是坚果保龄球。 */
    public int barType;

    /** 开局赠送的阳光数量。 */
    public int initialSun;

    /** 天空每隔多少毫秒掉一颗阳光；关卡文件没写时用 Layout 里的默认值。 */
    public long skySunInterval = Layout.SKY_SUN_INTERVAL;

    /** 按时间排好序的僵尸出场表。 */
    public final List<ZombieSpawn> spawns = new ArrayList<ZombieSpawn>();

    /** 传送带或保龄球模式可以出的卡片编号，正常关卡是空的。 */
    public final List<Integer> cardPool = new ArrayList<Integer>();

    /** 这个关卡有没有阳光条（也就是正常选卡模式）。 */
    public boolean isNormalMode() {
        return barType == GameState.BAR_NORMAL;
    }
}
