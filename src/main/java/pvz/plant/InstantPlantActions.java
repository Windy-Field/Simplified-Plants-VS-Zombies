package pvz.plant;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import pvz.game.GameState;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieEffects;

/**
 * InstantPlantActions 负责这一类植物每一帧的行为。
 *
 * 参数通过构造函数传入，处理结果直接写回这一局的游戏状态。
 */
public class InstantPlantActions {
    /** 樱桃炸弹横向波及相邻一格的距离。 */
    private static final int CHERRY_BLAST_RADIUS = Layout.CELL_WIDTH * 3 / 2;

    /** 图片和动画资源。 */
    private final Assets assets;
    /** 当前游戏的数据。 */
    private final GameState state;

    /**
     * 创建InstantPlantActions。
     *
     * 参数：originalAssets 提供素材；gameState 是当前游戏的数据。
     */
    public InstantPlantActions(Assets originalAssets, GameState gameState) {
        assets = originalAssets;
        state = gameState;
    }

    /**
     * 一次性植物：种下后先播一段动画，播完触发效果，效果演完就消失。
     *
     * 参数：plant 是那株一次性植物。
     */
    public void update(Plant plant) {
        if (!plant.triggered) {
            long growTime = assets.count(plant.animation) * Layout.DEFAULT_ANIMATION_INTERVAL;
            if (state.time - plant.placed < growTime) {
                return;
            }
            plant.triggered = true;
            plant.stateStart = state.time;
            triggerInstantEffect(plant);
        }

        // 特效要等它自己那段动画播完，否则火球还没炸开就消失了。
        // 单帧的特效（比如雪花）没有长度可言，就至少停留一小会儿。
        long animationTime = assets.count(plant.animation) * Layout.DEFAULT_ANIMATION_INTERVAL;
        long effectDuration = Math.max(animationTime, Layout.SHORT_EFFECT_DURATION);
        if (state.time - plant.stateStart > effectDuration) {
            plant.health = 0;
        }
    }

    /**
     * 樱桃炸弹炸周围一格，火爆辣椒烧一整行，寒冰菇冻住全场。
     *
     * 参数：plant 是刚触发的那株一次性植物。
     */
    private void triggerInstantEffect(Plant plant) {
        if (plant.name.equals("CherryBomb")) {
            // 炸伤判定要用樱桃自己的位置算，所以必须赶在换成火球之前做完；
            // 换动画会把坐标挪到火球上去，先换的话炸的就不是这一片了。
            //
            // 距离要按"身体中心"比，不能拿精灵的 x 直接相减：x 是整张图片的左上角，
            // 动图四周留着大片透明边距，而且樱桃和僵尸的图宽不同（89 对 85），
            // 相减的话左边一列会被算得特别远，炸不到，右边一列却没问题。
            Rectangle cherryBody = plant.collisionBox(assets, state.time);
            for (Zombie zombie : state.zombies) {
                if (zombie.hypno) {
                    continue;
                }
                Rectangle zombieBody = zombie.collisionBox(assets, state.time);
                double distanceX = Math.abs(zombieBody.getCenterX() - cherryBody.getCenterX());
                if (Math.abs(zombie.row - plant.row) <= 1 && distanceX <= CHERRY_BLAST_RADIUS) {
                    ZombieEffects.die(zombie, assets, state, state.time, true);
                }
            }
            // 再换成爆炸的火球。不换的话樱桃只是原地闪两下就凭空没了，看不出炸过。
            explodeInPlace(plant, "CherryBombExplode");
            return;
        }

        if (plant.name.equals("Jalapeno")) {
            plant.change("JalapenoExplode", assets, state.time);
            plant.x = Layout.GRID_LEFT;
            for (Zombie zombie : state.zombies) {
                if (!zombie.hypno && zombie.row == plant.row) {
                    ZombieEffects.die(zombie, assets, state, state.time, true);
                }
            }
            return;
        }

        // 剩下的就是寒冰菇：把雪花放大后铺在屏幕正中，冻住所有僵尸。
        plant.scale = 1.5;
        plant.change("IceShroomSnow", assets, state.time);
        BufferedImage snow = plant.picture(assets, state.time);
        plant.x = (Layout.WINDOW_WIDTH - snow.getWidth()) / 2.0;
        plant.y = (Layout.WINDOW_HEIGHT - snow.getHeight()) / 2.0;
        for (Zombie zombie : state.zombies) {
            if (!zombie.hypno && !zombie.dying) {
                zombie.frozenUntil = state.time + Layout.ZOMBIE_FREEZE_DURATION;
            }
        }
    }

    /**
     * 换成爆炸动画，并让火球正对着这株植物所在的那一格。
     *
     * 换动画时默认是"新图的底边对准旧图的底边"，这对换装、啃食那类动画是对的，
     * 但火球比植物高得多，底边对齐会把整团火顶到上一行去，看着像炸偏了。
     * 这里改成对准格子中心：植物的图矮、贴着格子下沿画，火球却是四面扩散的，
     * 对准格子才会和真正的杀伤范围同心。
     *
     * 注意：这个方法会改动植物的坐标，凡是按坐标算范围的判定都要在调用它之前做完。
     *
     * 参数：plant 是要引爆的植物；explodeAnimation 是爆炸动画名。
     */
    private void explodeInPlace(Plant plant, String explodeAnimation) {
        double cellCenterX = Layout.columnCenter(plant.column);
        double cellCenterY = Layout.GRID_TOP + plant.row * Layout.CELL_HEIGHT
            + Layout.CELL_HEIGHT / 2.0;

        plant.change(explodeAnimation, assets, state.time);

        // 按换完之后火球偏了多少，把它整体挪回格子中央。
        Rectangle blast = plant.bounds(assets, state.time);
        plant.x = plant.x + (cellCenterX - blast.getCenterX());
        plant.y = plant.y + (cellCenterY - blast.getCenterY());
    }
}
