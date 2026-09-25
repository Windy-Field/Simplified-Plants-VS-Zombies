package pvz.plant;

/**
 * 给植物分类：哪些是射手、哪些一次性生效、哪些靠僵尸走近才生效、哪些晚上才醒着。
 *
 * PlantActions 按这里的分类决定每株植物每一帧干什么。
 * 新植物如果和已有植物的套路一样，只要把名字加进对应名单就行。
 */
// TODO：【必做-6】新增植物时需要添加植物类型（向日葵系单独在 PlantActions 中改）
public final class PlantRules {
    /** 白天会睡觉的蘑菇类植物。 */
    private static final String[] NIGHT_PLANTS = {
        "PuffShroom", "ScaredyShroom", "SunShroom", "IceShroom", "HypnoShroom"
    };

    /** 会发射子弹的植物。 */
    private static final String[] SHOOTER_PLANTS = {
        "Peashooter", "SnowPea", "RepeaterPea", "Threepeater", "PuffShroom", "ScaredyShroom"
    };

    /** 种下后立刻生效、播完动画就消失的植物。 */
    private static final String[] INSTANT_PLANTS = {
        "CherryBomb", "Jalapeno", "IceShroom"
    };

    /**
     * 靠僵尸走近才生效的植物（地雷、窝瓜、食人花、地刺）。
     * 保龄球也算这一类，但它有两种，统一用名字后缀判断，见 isBowling。
     */
    private static final String[] CLOSE_ATTACK_PLANTS = {
        "PotatoMine", "Squash", "Chomper", "Spikeweed"
    };

    /** 这个类只提供静态判断，不允许创建对象。 */
    private PlantRules() {
    }

    /**
     * 判断一种植物是否属于给定名单。
     *
     * 参数：plantName 是植物名字；names 是候选名单。
     * 返回：在名单里就返回真。
     */
    private static boolean inList(String plantName, String[] names) {
        for (int index = 0; index < names.length; index++) {
            if (names[index].equals(plantName)) {
                return true;
            }
        }
        return false;
    }

    /** 判断是不是保龄球类植物（坚果保龄球或红坚果保龄球）。 */
    public static boolean isBowling(String plantName) {
        return plantName.endsWith("Bowling");
    }

    /** 判断白天是否需要睡觉。 */
    public static boolean sleepsAtDay(String plantName) {
        return inList(plantName, NIGHT_PLANTS);
    }

    /** 判断是不是射手（含小喷菇、胆小菇）。 */
    public static boolean isShooter(String plantName) {
        return inList(plantName, SHOOTER_PLANTS);
    }

    /** 判断是不是种下就立刻生效的一次性植物。 */
    public static boolean isInstant(String plantName) {
        if (isBowling(plantName)) {
            return false;
        }
        return inList(plantName, INSTANT_PLANTS);
    }

    /** 判断是不是靠近距离接触生效的植物。 */
    public static boolean isCloseAttack(String plantName) {
        if (isBowling(plantName)) {
            return true;
        }
        return inList(plantName, CLOSE_ATTACK_PLANTS);
    }

    /**
     * 判断僵尸能不能吃掉这株植物。
     *
     * 地刺长在地上、保龄球一直在滚，僵尸都咬不到，所以排除掉。
     *
     * 参数：plantName 是植物名字。
     * 返回：能被吃掉就返回真。
     */
    public static boolean canBeEaten(String plantName) {
        if (plantName.equals("Spikeweed")) {
            return false;
        }
        if (isBowling(plantName)) {
            return false;
        }
        return true;
    }
}
