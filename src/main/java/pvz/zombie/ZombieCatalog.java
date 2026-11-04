package pvz.zombie;

import pvz.world.Layout;

/**
 * 游戏中已经实现的僵尸固定资料表
 *
 * 编辑器也从这里取得名字和显示文字，避免游戏和编辑器各维护一份僵尸名单
 */
public final class ZombieCatalog {
    // TODO【14-僵尸资料】：新增品种时同时补充 DEFINITIONS 和同位置的 LABELS，编辑器会自动读取
    // 固定血量和速度在资料中设置，中文能力介绍按内部名加入 AlmanacDescriptions 的 ZOMBIES 表
    /** 编辑器目前支持的僵尸资料 */
    public static final ZombieDefinition[] DEFINITIONS = {
        new ZombieDefinition("Zombie", Layout.NORMAL_ZOMBIE_HEALTH, false, true, 1, 1, null, null),
        new ZombieDefinition("ConeheadZombie", Layout.CONEHEAD_ZOMBIE_HEALTH, true, true, 1, 1, null, null),
        new ZombieDefinition("BucketheadZombie", Layout.BUCKETHEAD_ZOMBIE_HEALTH, true, true, 1, 1, null, null),
        new ZombieDefinition("FlagZombie", Layout.NORMAL_ZOMBIE_HEALTH, false, false, 1, 1, null, null),
        new ZombieDefinition("NewspaperZombie", Layout.NEWSPAPER_ZOMBIE_HEALTH, true, false, 1, 2, null, null),
        new ZombieDefinition("JokerZombie", Layout.JOKER_ZOMBIE_HEALTH, false, false, 1, 1,
            "JokerZombie", "JokerZombieExplode"),
        new ZombieDefinition("FootballZombie", Layout.FOOTBALL_ZOMBIE_HEALTH,
            true, false, 1.5, 1.5, null, null,
            "FootballZombieOrnLost", "FootballZombieLostHead"),
        new ZombieDefinition("Gargantuar", Layout.GARGANTUAR_HEALTH,
            false, false, Layout.GARGANTUAR_SPEED, Layout.GARGANTUAR_SPEED, null, null),
        new ZombieDefinition("Imp", Layout.IMP_HEALTH,
            false, false, Layout.IMP_SPEED, Layout.IMP_SPEED, null, null)
    };

    /** 和 DEFINITIONS 一一对应的编辑器中文名 */
    public static final String[] LABELS = {
        "普通僵尸", "路障僵尸", "铁桶僵尸", "旗帜僵尸", "读报僵尸", "小丑僵尸",
        "橄榄球僵尸", "巨人僵尸", "小鬼僵尸"
    };

    /** 这个类只提供固定资料，不允许创建对象 */
    private ZombieCatalog() {
    }

    /**
     * 按名字取得僵尸资料
     *
     * 参数：zombieName 是僵尸内部名字
     * 返回：对应的资料；找不到时抛出异常
     */
    public static ZombieDefinition definitionOf(String zombieName) {
        for (int index = 0; index < DEFINITIONS.length; index = index + 1) {
            if (DEFINITIONS[index].name.equals(zombieName)) {
                return DEFINITIONS[index];
            }
        }
        throw new IllegalArgumentException("未知的僵尸：" + zombieName);
    }

    /** 返回编辑器支持的僵尸名字数组 */
    public static String[] names() {
        String[] names = new String[DEFINITIONS.length];
        for (int index = 0; index < DEFINITIONS.length; index = index + 1) {
            names[index] = DEFINITIONS[index].name;
        }
        return names;
    }

    /** 返回编辑器显示的僵尸中文名数组 */
    public static String[] labels() {
        String[] labels = new String[LABELS.length];
        for (int index = 0; index < LABELS.length; index = index + 1) {
            labels[index] = LABELS[index];
        }
        return labels;
    }
}
