package pvz.plant;

/**
 * 全部植物的固定资料表。
 *
 * 每种植物的资料放在同一个 PlantDefinition 对象里，新增植物时只需要新增一条完整记录。
 */
public final class PlantCatalog {
    /** 全部植物，最后两种是保龄球植物。 */
    public static final PlantDefinition[] DEFINITIONS = {
        new PlantDefinition("SunFlower", "card_sunflower", 50, 7500,
            5, false, true, PlantActionType.SUN_PRODUCER, false),
        new PlantDefinition("Peashooter", "card_peashooter", 100, 7500,
            5, false, true, PlantActionType.SHOOTER, false),
        new PlantDefinition("SnowPea", "card_snowpea", 175, 7500,
            5, false, true, PlantActionType.SHOOTER, false),
        new PlantDefinition("WallNut", "card_wallnut", 50, 30000,
            30, false, true, PlantActionType.WALL_NUT, false),
        new PlantDefinition("CherryBomb", "card_cherrybomb", 150, 50000,
            5, false, true, PlantActionType.INSTANT, false),
        new PlantDefinition("Threepeater", "card_threepeashooter", 325, 7500,
            5, false, true, PlantActionType.SHOOTER, false),
        new PlantDefinition("RepeaterPea", "card_repeaterpea", 200, 7500,
            5, false, true, PlantActionType.SHOOTER, false),
        new PlantDefinition("Chomper", "card_chomper", 150, 7500,
            5, false, true, PlantActionType.CLOSE_ATTACK, false),
        new PlantDefinition("PuffShroom", "card_puffshroom", 0, 7500,
            5, true, true, PlantActionType.SHOOTER, false),
        new PlantDefinition("PotatoMine", "card_potatomine", 25, 30000,
            5, false, true, PlantActionType.CLOSE_ATTACK, false),
        new PlantDefinition("Squash", "card_squash", 50, 30000,
            5, false, true, PlantActionType.CLOSE_ATTACK, false),
        new PlantDefinition("Spikeweed", "card_spikeweed", 100, 7500,
            5, false, false, PlantActionType.CLOSE_ATTACK, false),
        new PlantDefinition("Jalapeno", "card_jalapeno", 125, 50000,
            5, false, true, PlantActionType.INSTANT, false),
        new PlantDefinition("ScaredyShroom", "card_scaredyshroom", 25, 7500,
            5, true, true, PlantActionType.SHOOTER, false),
        new PlantDefinition("SunShroom", "card_sunshroom", 25, 7500,
            5, true, true, PlantActionType.SUN_PRODUCER, false),
        new PlantDefinition("IceShroom", "card_iceshroom", 75, 50000,
            5, true, true, PlantActionType.INSTANT, false),
        new PlantDefinition("HypnoShroom", "card_hypnoshroom", 75, 30000,
            1, true, true, PlantActionType.NONE, false),
        new PlantDefinition("WallNutBowling", "card_wallnut", 0, 0,
            1, false, false, PlantActionType.CLOSE_ATTACK, true),
        new PlantDefinition("RedWallNutBowling", "card_redwallnut_move", 0, 0,
            1, false, false, PlantActionType.CLOSE_ATTACK, true)
    };

    /** 选卡界面中不包含保龄球植物。 */
    public static final int CHOOSER_COUNT = DEFINITIONS.length - 2;

    /** 这个类只提供固定资料和查询方法，不允许创建对象。 */
    private PlantCatalog() {
    }

    /**
     * 按编号取得植物资料。
     *
     * 参数：plantIndex 是植物编号。
     * 返回：对应的植物资料。
     */
    public static PlantDefinition definitionAt(int plantIndex) {
        return DEFINITIONS[plantIndex];
    }

    /**
     * 按植物名取得固定资料。
     *
     * 参数：plantName 是植物内部名字。
     * 返回：对应的资料；找不到时抛出异常。
     */
    public static PlantDefinition definitionOf(String plantName) {
        int index = indexOf(plantName);
        if (index < 0) {
            throw new IllegalArgumentException("未知的植物：" + plantName);
        }
        return DEFINITIONS[index];
    }

    /**
     * 查植物编号。
     *
     * 参数：plantName 是植物内部名字。
     * 返回：找到就返回编号；找不到返回 -1。
     */
    public static int indexOf(String plantName) {
        for (int index = 0; index < DEFINITIONS.length; index++) {
            if (DEFINITIONS[index].name.equals(plantName)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 按编号取得植物名。
     *
     * 参数：plantIndex 是植物编号。
     * 返回：植物内部名字。
     */
    public static String nameAt(int plantIndex) {
        return DEFINITIONS[plantIndex].name;
    }

    /** 判断一种植物是不是保龄球。 */
    public static boolean isBowling(String plantName) {
        return definitionOf(plantName).bowling;
    }

    /** 判断僵尸能不能吃掉一种植物。 */
    public static boolean canBeEaten(String plantName) {
        return definitionOf(plantName).canBeEaten;
    }
}
