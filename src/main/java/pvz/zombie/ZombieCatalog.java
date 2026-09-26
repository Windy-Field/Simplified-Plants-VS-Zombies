package pvz.zombie;

/**
 * 游戏中已经实现的僵尸固定资料表。
 *
 * 编辑器也从这里取得名字和显示文字，避免游戏和编辑器各维护一份僵尸名单。
 */
public final class ZombieCatalog {
    /** 编辑器目前支持的僵尸资料。 */
    public static final ZombieDefinition[] DEFINITIONS = {
        new ZombieDefinition("Zombie", 10, false, true, 1, 1),
        new ZombieDefinition("ConeheadZombie", 20, true, true, 1, 1),
        new ZombieDefinition("BucketheadZombie", 30, true, true, 1, 1),
        new ZombieDefinition("FlagZombie", 15, false, false, 1, 1),
        new ZombieDefinition("NewspaperZombie", 15, true, false, 1, 2)
    };

    /** 和 DEFINITIONS 一一对应的编辑器中文名。 */
    public static final String[] LABELS = {
        "普通僵尸", "路障僵尸", "铁桶僵尸", "旗帜僵尸", "读报僵尸"
    };

    /** 这个类只提供固定资料，不允许创建对象。 */
    private ZombieCatalog() {
    }

    /**
     * 按名字取得僵尸资料。
     *
     * 参数：zombieName 是僵尸内部名字。
     * 返回：对应的资料；找不到时抛出异常。
     */
    public static ZombieDefinition definitionOf(String zombieName) {
        for (int index = 0; index < DEFINITIONS.length; index++) {
            if (DEFINITIONS[index].name.equals(zombieName)) {
                return DEFINITIONS[index];
            }
        }
        throw new IllegalArgumentException("未知的僵尸：" + zombieName);
    }

    /** 返回编辑器支持的僵尸名字数组。 */
    public static String[] names() {
        String[] names = new String[DEFINITIONS.length];
        for (int index = 0; index < DEFINITIONS.length; index++) {
            names[index] = DEFINITIONS[index].name;
        }
        return names;
    }

    /** 返回编辑器显示的僵尸中文名数组。 */
    public static String[] labels() {
        String[] labels = new String[LABELS.length];
        for (int index = 0; index < LABELS.length; index++) {
            labels[index] = LABELS[index];
        }
        return labels;
    }

    /** 查一种僵尸是否是编辑器支持的品种。 */
    public static boolean isKnown(String zombieName) {
        for (int index = 0; index < DEFINITIONS.length; index++) {
            if (DEFINITIONS[index].name.equals(zombieName)) {
                return true;
            }
        }
        return false;
    }
}
