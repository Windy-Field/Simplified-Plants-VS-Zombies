package pvz.plant;

import java.awt.Rectangle;
import pvz.game.GameState;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.world.Sun;

/**
 * 管理产阳光植物的生产间隔、产量和阳光菇成长
 *
 * 参数通过构造函数传入，处理结果直接写回这一局的游戏状态
 */
public class SunProducerActions {
    /** 图片和动画资源 */
    private final Assets assets;
    /** 当前游戏的数据 */
    private final GameState state;

    /**
     * 创建产阳光行为入口，使用资料表中的产量处理单颗和多颗阳光
     *
     * 参数：assets 提供素材；gameState 是当前游戏的数据
     */
    public SunProducerActions(Assets assets, GameState gameState) {
        this.assets = assets;
        state = gameState;
    }

    /**
     * 向日葵和阳光菇产阳光
     *
     * 先等待首次生产时间，再按生产间隔生成阳光；数量从植物资料读取
     * 等待时间由 Layout 维护，避免调整数值后这里仍沿用旧规则
     *
     * 参数：plant 是那株植物
     */
    public void update(Plant plant) {
        long alive = state.time - plant.placed;

        // 阳光菇长够时间会变大，变大之后产的是大阳光
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
        PlantDefinition definition = PlantCatalog.definitionOf(plant.name);
        for (int index = 0; index < definition.sunCount; index = index + 1) {
            int sourceCenter = center - (definition.sunCount - 1) * 10 + index * 20;
            int targetX = sunTargetX - (definition.sunCount - 1) * 10 + index * 20;
            state.suns.add(new Sun(sourceCenter, bottom, targetX, sunTargetY,
                big, assets));
        }
        plant.lastAction = state.time;
    }
}
