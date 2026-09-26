package pvz.zombie;

/**
 * 关卡 JSON 里的一条僵尸出场记录。
 *
 * 原关卡文件里写着"第几毫秒、在第几行、出现哪种僵尸"，
 * 读出来之后就存成这个对象，放到一个列表里排队用。
 */
public class ZombieSpawn {
    /**
     * row 取这个值表示"随机行"，出场时随便挑一行，不固定在某一排。
     *
     * 编辑器给某一格勾了随机行，展开成出场表时就会写成这个值；
     * 游戏侧见到它才去摇一个行号，两边认的是同一个约定。
     */
    public static final int RANDOM_ROW = -1;

    /** 从关卡开始算起，第几毫秒出现。 */
    public int spawnTime;

    /** 出现在第几行；是 {@link #RANDOM_ROW} 时表示随机行。 */
    public int row;

    /** 僵尸品种名，比如 "Zombie" 或 "ConeheadZombie"。 */
    public String name;

    /**
     * 创建一条出场记录。
     *
     * 参数：spawnTime 是出场时刻（毫秒）；lane 是行号；kind 是僵尸品种名。
     */
    public ZombieSpawn(int spawnTime, int lane, String kind) {
        this.spawnTime = spawnTime;
        row = lane;
        name = kind;
    }
}
