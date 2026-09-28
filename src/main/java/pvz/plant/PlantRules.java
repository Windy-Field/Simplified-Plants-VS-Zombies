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

    /**
     * 判断植物当前是否处于可以躲过爆炸僵尸的攻击动作。
     *
     * 正在播自己的攻击动画，就说明这一下已经打出去了、收不回来，
     * 爆炸僵尸不该把它打断。这条对所有植物都成立，不需要按品种逐个列举。
     *
     * 参数：plant 是要检查的植物。
     * 返回：正在攻击中返回真。
     */
    public static boolean isProtectedFromExplodingZombie(Plant plant) {
        String attack = PlantCatalog.definitionOf(plant.name).attackAnimation;
        if (attack == null) {
            return false;
        }
        return plant.animation.equals(attack);
    }
}
