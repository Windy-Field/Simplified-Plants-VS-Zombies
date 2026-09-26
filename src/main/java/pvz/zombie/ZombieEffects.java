package pvz.zombie;

import java.awt.Rectangle;
import pvz.game.GameState;
import pvz.plant.Plant;
import pvz.world.Assets;
import pvz.world.CombatValues;
import pvz.world.Layout;
import pvz.world.Sprite;

/**
 * 处理僵尸死亡和小丑爆炸带来的公共效果。
 *
 * 小丑可能因为碰到植物、子弹、小推车或其他植物效果而死亡，
 * 所有入口都经过这里，才能保证它只爆炸一次。
 */
public final class ZombieEffects {
    /** 小丑爆炸的 3×3 范围，中心格上下左右各扩一格。 */
    private static final int JOKER_EXPLOSION_RANGE = 1;

    /** 这个类只提供静态效果处理，不允许创建对象。 */
    private ZombieEffects() {
    }

    /**
     * 让僵尸死亡，并在需要时执行小丑爆炸。
     *
     * 参数：zombie 是要死亡的僵尸；assets 提供素材；state 是当前游戏数据；
     * time 是当前游戏时刻；explosion 表示是否使用普通爆炸死亡效果。
     */
    public static void die(Zombie zombie, Assets assets, GameState state,
            long time, boolean explosion) {
        if (zombie.hasAbility(ZombieAbility.EXPLODES_ON_PLANT)
                && !zombie.explosionTriggered) {
            detonateExplodingZombie(zombie, assets, state, time);
        }
        zombie.die(assets, time, explosion);
    }

    /**
     * 执行小丑爆炸：清除范围内植物，伤害范围内魅惑僵尸，并添加爆炸提示图。
     *
     * 参数：zombie 是正在爆炸的僵尸；assets 提供素材；state 是当前游戏数据；
     * time 是当前游戏时刻。
     */
    public static void detonateExplodingZombie(Zombie zombie, Assets assets,
            GameState state, long time) {
        zombie.explosionTriggered = true;

        Rectangle body = zombie.collisionBox(assets, time);
        int centerX = (int) body.getCenterX();
        int centerY = (int) body.getMaxY();
        int centerColumn = Layout.columnAt(centerX);

        clearPlants(zombie.row, centerColumn, state);
        damageHypnotizedZombies(zombie, centerColumn, assets, state, time);

        Sprite effect = new Sprite("JokerBoom", centerX, centerY,
            zombie.row, 0, assets);
        effect.animationStart = time;
        effect.frameInterval = (int) Layout.DEFAULT_ANIMATION_INTERVAL;
        state.effects.add(effect);
    }

    /**
     * 清除小丑 3×3 范围内的所有植物。
     *
     * 参数：centerRow 和 centerColumn 是爆炸中心；state 是当前游戏数据。
     */
    private static void clearPlants(int centerRow, int centerColumn,
            GameState state) {
        for (Plant plant : state.plants) {
            if (!plant.alive) {
                continue;
            }
            if (Math.abs(plant.row - centerRow) > JOKER_EXPLOSION_RANGE) {
                continue;
            }
            if (Math.abs(plant.column - centerColumn) > JOKER_EXPLOSION_RANGE) {
                continue;
            }
            plant.health = 0;
            plant.alive = false;
            if (state.barType != GameState.BAR_BOWLING) {
                state.occupied[plant.row][plant.column] = false;
            }
        }
    }

    /**
     * 只伤害爆炸范围内的魅惑僵尸，普通僵尸不会受到小丑爆炸伤害。
     *
     * 参数：zombie 是爆炸中心的僵尸；centerColumn 是中心列；assets 提供素材；
     * state 是当前游戏数据；time 是当前游戏时刻。
     */
    private static void damageHypnotizedZombies(Zombie zombie, int centerColumn,
            Assets assets, GameState state, long time) {
        for (Zombie other : state.zombies) {
            if (other == zombie || !other.alive || other.dying || !other.hypno) {
                continue;
            }
            if (Math.abs(other.row - zombie.row) > JOKER_EXPLOSION_RANGE) {
                continue;
            }
            Rectangle body = other.collisionBox(assets, time);
            int otherColumn = Layout.columnAt((int) body.getCenterX());
            if (Math.abs(otherColumn - centerColumn) > JOKER_EXPLOSION_RANGE) {
                continue;
            }
            other.health = other.health - CombatValues.JOKER_EXPLOSION_DAMAGE;
        }
    }
}
