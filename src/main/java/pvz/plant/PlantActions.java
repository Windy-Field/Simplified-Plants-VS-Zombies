package pvz.plant;

import pvz.game.GameState;
import pvz.world.Assets;
import pvz.world.Layout;

/**
 * 植物行为的总入口
 *
 * 这里检查植物存活和睡眠状态，并分派产阳光、射击、爆炸和近战行为
 * 坚果的少量换图逻辑直接保留为本类方法，不为单个品种另建处理类
 */
public class PlantActions {
    /** 供坚果按血量切换动画，其他行为类也使用同一份素材 */
    private final Assets assets;

    /** 当前游戏的数据 */
    private final GameState state;

    /** 阳光植物的行为处理器 */
    private final SunProducerActions sunProducerActions;

    /** 射手植物的行为处理器 */
    private final ShooterActions shooterActions;

    /** 一次性植物的行为处理器 */
    private final InstantPlantActions instantPlantActions;

    /** 近战和保龄球植物的行为处理器 */
    private final CloseAttackActions closeAttackActions;

    /**
     * 创建植物行为总入口
     *
     * 参数：assets 提供素材；gameState 是当前游戏的数据
     */
    public PlantActions(Assets assets, GameState gameState) {
        this.assets = assets;
        state = gameState;
        sunProducerActions = new SunProducerActions(assets, gameState);
        shooterActions = new ShooterActions(assets, gameState);
        instantPlantActions = new InstantPlantActions(assets, gameState);
        closeAttackActions = new CloseAttackActions(assets, gameState);
    }

    /**
     * 遍历所有植物，并把每株植物交给正确的行为处理器
     */
    public void updateAll() {
        for (Plant plant : state.plants) {
            if (!plant.alive) {
                continue;
            }
            if (plant.health <= 0) {
                removePlant(plant);
                continue;
            }
            if (plant.sleeping) {
                continue;
            }
            dispatchPlant(plant);
        }
    }

    /**
     * 按植物固定资料里的行为类别分发植物
     *
     * 参数：plant 是要更新的植物
     */
    private void dispatchPlant(Plant plant) {
        // TODO【10-植物行为】：新品种先选择已有类别，复用对应处理器中的索敌、间隔和效果
        // 少量特殊逻辑可写成本类的具名方法，只有形成一类共用行为时才考虑独立文件
        PlantDefinition definition = PlantCatalog.definitionOf(plant.name);
        if (definition.actionType == PlantActionType.SUN_PRODUCER) {
            sunProducerActions.update(plant);
            return;
        }
        if (definition.actionType == PlantActionType.SHOOTER) {
            shooterActions.update(plant);
            return;
        }
        if (definition.actionType == PlantActionType.WALL_NUT) {
            updateWallNut(plant);
            return;
        }
        if (definition.actionType == PlantActionType.INSTANT) {
            instantPlantActions.update(plant);
            return;
        }
        if (definition.actionType == PlantActionType.CLOSE_ATTACK) {
            closeAttackActions.update(plant);
        }
    }

    /** 参数 plant 是坚果；按当前血量切换两阶段裂纹，不重复重启动画 */
    private void updateWallNut(Plant plant) {
        boolean cracked2 = plant.animation.equals("WallNut_cracked2");
        if (plant.health <= Layout.WALL_NUT_SECOND_CRACK_HEALTH && !cracked2) {
            plant.change("WallNut_cracked2", assets, state.time);
            return;
        }
        boolean fresh = plant.animation.equals("WallNut");
        if (plant.health <= Layout.WALL_NUT_FIRST_CRACK_HEALTH && fresh) {
            plant.change("WallNut_cracked1", assets, state.time);
        }
    }

    /**
     * 从场上移除一株植物，并释放它占用的格子
     *
     * 参数：plant 是要移除的植物
     * 铲子和植物死亡都使用这条入口，避免两边各写一份清理逻辑
     */
    public void removePlant(Plant plant) {
        plant.alive = false;
        if (state.barType == Layout.BAR_BOWLING) {
            return;
        }
        state.occupied[plant.row][plant.column] = false;
    }
}
