package pvz.plant;

import pvz.game.GameState;
import pvz.world.Assets;

/**
 * 植物行为的总入口。
 *
 * 这个类只负责遍历植物、检查植物是否还活着，并把植物交给对应的行为家族。
 * 具体的产阳光、射击、爆炸和近战规则分别放在独立的处理类中。
 */
public class PlantActions {
    /** 当前游戏的数据。 */
    private final GameState state;

    /** 阳光植物的行为处理器。 */
    private final SunProducerActions sunProducerActions;

    /** 射手植物的行为处理器。 */
    private final ShooterActions shooterActions;

    /** 坚果植物的行为处理器。 */
    private final WallNutActions wallNutActions;

    /** 一次性植物的行为处理器。 */
    private final InstantPlantActions instantPlantActions;

    /** 近战和保龄球植物的行为处理器。 */
    private final CloseAttackActions closeAttackActions;

    /**
     * 创建植物行为总入口。
     *
     * 参数：originalAssets 提供素材；gameState 是当前游戏的数据。
     */
    public PlantActions(Assets originalAssets, GameState gameState) {
        state = gameState;
        sunProducerActions = new SunProducerActions(originalAssets, gameState);
        shooterActions = new ShooterActions(originalAssets, gameState);
        wallNutActions = new WallNutActions(originalAssets, gameState);
        instantPlantActions = new InstantPlantActions(originalAssets, gameState);
        closeAttackActions = new CloseAttackActions(originalAssets, gameState);
    }

    /**
     * 遍历所有植物，并把每株植物交给正确的行为处理器。
     */
    public void updateAll() {
        for (Plant plant : state.plants) {
            if (!plant.alive) {
                continue;
            }
            if (plant.health <= 0) {
                killPlant(plant);
                continue;
            }
            if (plant.sleeping) {
                continue;
            }
            dispatchPlant(plant);
        }
    }

    /**
     * 按植物固定资料里的行为类别分发植物。
     *
     * 参数：plant 是要更新的植物。
     */
    private void dispatchPlant(Plant plant) {
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
            wallNutActions.update(plant);
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

    /**
     * 植物死亡后释放它占用的格子。
     *
     * 参数：plant 是已经死亡的植物。
     */
    private void killPlant(Plant plant) {
        plant.alive = false;
        if (state.barType == GameState.BAR_BOWLING) {
            return;
        }
        state.occupied[plant.row][plant.column] = false;
    }
}
