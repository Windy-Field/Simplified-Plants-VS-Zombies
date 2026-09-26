package pvz.plant;

import pvz.game.GameState;
import pvz.world.Assets;
import pvz.world.CombatValues;

/**
 * WallNutActions 负责这一类植物每一帧的行为。
 *
 * 参数通过构造函数传入，处理结果直接写回这一局的游戏状态。
 */
public class WallNutActions {
    /** 图片和动画资源。 */
    private final Assets assets;
    /** 当前游戏的数据。 */
    private final GameState state;

    /**
     * 创建WallNutActions。
     *
     * 参数：originalAssets 提供素材；gameState 是当前游戏的数据。
     */
    public WallNutActions(Assets originalAssets, GameState gameState) {
        assets = originalAssets;
        state = gameState;
    }

    /**
     * 坚果被咬坏之后会换成有裂纹的图，分两个阶段。
     *
     * 参数：plant 是那株坚果。
     */
    public void update(Plant plant) {
        boolean cracked2 = plant.animation.equals("WallNut_cracked2");
        if (plant.health <= CombatValues.WALL_NUT_SECOND_CRACK_HEALTH
                && !cracked2) {
            plant.change("WallNut_cracked2", assets, state.time);
            return;
        }
        boolean fresh = plant.animation.equals("WallNut");
        if (plant.health <= CombatValues.WALL_NUT_FIRST_CRACK_HEALTH
                && fresh) {
            plant.change("WallNut_cracked1", assets, state.time);
        }
    }
}
