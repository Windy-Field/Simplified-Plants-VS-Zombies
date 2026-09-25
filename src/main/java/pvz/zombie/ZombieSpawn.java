package pvz.zombie;

/**
 * 关卡 JSON 里的一条僵尸出场记录。
 *
 * 原关卡文件里写着"第几毫秒、在第几行、出现哪种僵尸"，
 * 读出来之后就存成这个对象，放到一个列表里排队用。
 */
public class ZombieSpawn {
    /** 从关卡开始算起，第几毫秒出现。 */
    public int at;

    /** 出现在第几行。 */
    public int row;

    /** 僵尸品种名，比如 "Zombie" 或 "ConeheadZombie"。 */
    public String name;

    /**
     * 创建一条出场记录。
     *
     * 参数：spawnTime 是出场时刻（毫秒）；lane 是行号；kind 是僵尸品种名。
     */
    public ZombieSpawn(int spawnTime, int lane, String kind) {
        at = spawnTime;
        row = lane;
        name = kind;
    }
}
