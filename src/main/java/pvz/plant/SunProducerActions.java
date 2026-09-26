package pvz.plant;

import java.awt.Rectangle;
import pvz.game.GameState;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.world.Sun;

/**
 * SunProducerActions 负责这一类植物每一帧的行为。
 *
 * 参数通过构造函数传入，处理结果直接写回这一局的游戏状态。
 */
public class SunProducerActions {
    /** 图片和动画资源。 */
    private final Assets assets;
    /** 当前游戏的数据。 */
    private final GameState state;

    /**
     * 创建SunProducerActions。
     *
     * 参数：originalAssets 提供素材；gameState 是当前游戏的数据。
     */
    public SunProducerActions(Assets originalAssets, GameState gameState) {
        assets = originalAssets;
        state = gameState;
    }

    /**
     * 向日葵和阳光菇产阳光。
     *
     * 刚种下的六秒内不产，之后每隔二十二秒产一颗。
     *
     * 参数：plant 是那株植物。
     */
    public void update(Plant plant) {
        long alive = state.time - plant.placed;

        // 阳光菇长够时间会变大，变大之后产的是大阳光。
        boolean grownUp = plant.animation.equals("SunShroomBig");
        if (plant.name.equals("SunShroom") && alive > Layout.SUN_SHROOM_GROW_TIME && !grownUp) {
            plant.change("SunShroomBig", assets, state.time);
        }

        if (alive < Layout.SUN_PRODUCE_FIRST_DELAY) {
            return;
        }
        boolean firstTime = plant.lastAction == 0;
        if (!firstTime && state.time - plant.lastAction < Layout.SUN_PRODUCE_INTERVAL) {
            return;
        }

        Rectangle bounds = plant.bounds(assets, state.time);
        boolean big = !plant.name.equals("SunShroom") || plant.animation.equals("SunShroomBig");
        int center = (int) bounds.getCenterX();
        int bottom = (int) bounds.getMaxY();
        int sunTargetX = bounds.x + bounds.width;
        int sunTargetY = bottom + bounds.height / 2;
        state.suns.add(new Sun(center, bottom, sunTargetX, sunTargetY, big, assets));
        plant.lastAction = state.time;
    }
}
