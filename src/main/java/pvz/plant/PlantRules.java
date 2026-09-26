package pvz.plant;

/**
 * 游戏中查询植物分类的入口。
 *
 * 分类资料只在 PlantCatalog 中登记一次，这里保留简单的判断方法供游戏使用。
 */
public final class PlantRules {
    /** 这个类只提供静态判断，不允许创建对象。 */
    private PlantRules() {
    }

    /** 判断是不是保龄球类植物。 */
    public static boolean isBowling(String plantName) {
        return PlantCatalog.isBowling(plantName);
    }

    /** 判断是不是射手。 */
    public static boolean isShooter(String plantName) {
        PlantDefinition definition = PlantCatalog.definitionOf(plantName);
        return definition.actionType == PlantActionType.SHOOTER;
    }

    /** 判断是不是种下后生效的一次性植物。 */
    public static boolean isInstant(String plantName) {
        PlantDefinition definition = PlantCatalog.definitionOf(plantName);
        return definition.actionType == PlantActionType.INSTANT;
    }

    /** 判断是不是靠近僵尸才生效的植物。 */
    public static boolean isCloseAttack(String plantName) {
        PlantDefinition definition = PlantCatalog.definitionOf(plantName);
        return definition.actionType == PlantActionType.CLOSE_ATTACK;
    }

    /** 判断僵尸能不能吃掉这种植物。 */
    public static boolean canBeEaten(String plantName) {
        return PlantCatalog.canBeEaten(plantName);
    }

    /**
     * 判断植物当前是否处于可以躲过爆炸僵尸的攻击动作。
     *
     * 参数：plant 是要检查的植物。
     * 返回：正在攻击中的大嘴花或窝瓜返回真。
     */
    public static boolean isProtectedFromExplodingZombie(Plant plant) {
        if (plant.name.equals("Chomper")
                && plant.animation.equals("ChomperAttack")) {
            return true;
        }
        if (plant.name.equals("Squash")
                && plant.animation.equals("SquashAttack")) {
            return true;
        }
        return false;
    }
}
