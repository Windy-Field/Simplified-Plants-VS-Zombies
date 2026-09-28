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
        if (!zombie.alive || zombie.dying) {
            return;
        }
        if (zombie.hasOwnAnimation()) {
            // 有专属动画的僵尸用自己的开盒动画代替普通掉头动画。
            zombie.explosionOrigin = zombie.collisionBox(assets, time);
            zombie.pendingDeathExplosion = explosion;
            zombie.die(assets, time, explosion);
            return;
        }

        if (!zombie.headLost) {
            startHeadLoss(zombie, assets, state, time, explosion);
            return;
        }

        zombie.die(assets, time, explosion);
    }

    /**
     * 等死亡动画播完后才移除僵尸，并在此时执行小丑自爆。
     *
     * 参数：zombie 是待更新的僵尸；assets 提供动画帧数；
     * state 是当前游戏数据；time 是当前游戏时刻。
     * 返回：这只僵尸正在死亡或已经死亡时返回真。
     */
    public static boolean updateDeath(Zombie zombie, Assets assets,
            GameState state, long time) {
        if (!zombie.dying) {
            return false;
        }

        if (zombie.waitingForDeath) {
            long headLossDuration = assets.count(zombie.animation)
                * zombie.frameInterval;
            if (time - zombie.headLossTime >= headLossDuration) {
                zombie.waitingForDeath = false;
                zombie.die(assets, time, zombie.pendingDeathExplosion);
            }
            return true;
        }

        long duration = assets.count(zombie.animation) * zombie.frameInterval;
        if (time - zombie.deathTime >= duration) {
            finishDeath(zombie, assets, state, time);
            zombie.alive = false;
        }
        return true;
    }

    /**
     * 开始播放普通僵尸的掉头动画，动画结束后才进入死亡动画。
     *
     * 参数：zombie 是即将死亡的僵尸；assets 提供素材；state 保存掉下来的头；
     * time 是当前游戏时刻；explosion 表示之后是否使用爆炸死亡图。
     */
    private static void startHeadLoss(Zombie zombie, Assets assets,
            GameState state, long time, boolean explosion) {
        Rectangle oldBody = zombie.bounds(assets, time);
        int center = (int) oldBody.getCenterX();
        int bottom = (int) oldBody.getMaxY();
        String headAnimation = "ZombieHead";
        if (zombie.name.equals("NewspaperZombie")) {
            headAnimation = "NewspaperZombieHead";
        }

        Sprite head = new Sprite(headAnimation, center, bottom,
            zombie.row, 0, assets);
        head.animationStart = time;
        state.heads.add(head);

        zombie.helmet = false;
        zombie.headLost = true;
        zombie.attacking = false;
        zombie.dying = true;
        zombie.headLossTime = time;
        zombie.deathTime = time;
        zombie.pendingDeathExplosion = explosion;
        zombie.waitingForDeath = true;

        String headlessAnimation = zombie.stateAnimation(false);
        zombie.frameInterval = Zombie.animationIntervalFor(headlessAnimation);
        zombie.change(headlessAnimation, assets, time);
    }

    /**
     * 死亡动画播完后结算小丑爆炸，普通僵尸无需处理。
     *
     * 参数：zombie 是结束死亡动画的僵尸；assets 提供素材；
     * state 是当前游戏数据；time 是当前游戏时刻。
     */
    private static void finishDeath(Zombie zombie, Assets assets,
            GameState state, long time) {
        if (zombie.explosionOrigin == null) {
            return;
        }

        Rectangle origin = zombie.explosionOrigin;
        zombie.explosionOrigin = null;
        int centerX = (int) origin.getCenterX();
        int centerY = (int) origin.getMaxY();
        clearPlants(zombie.row, centerX, assets, state, time);
        damageHypnotizedZombies(zombie, centerX, assets, state, time);

        Sprite effect = new Sprite("JokerBoom", centerX, centerY,
            zombie.row, 0, assets);
        effect.animationStart = time;
        effect.frameInterval = (int) Layout.DEFAULT_ANIMATION_INTERVAL;
        state.effects.add(effect);
    }

    /**
     * 清除小丑 3×3 范围内的所有植物。
     *
     * 参数：centerRow 和 centerX 是爆炸中心；assets 提供碰撞盒；
     * state 是当前游戏数据；time 是当前时刻。
     */
    private static void clearPlants(int centerRow, int centerX, Assets assets,
            GameState state, long time) {
        for (Plant plant : state.plants) {
            if (!plant.alive) {
                continue;
            }
            if (Math.abs(plant.row - centerRow) > JOKER_EXPLOSION_RANGE) {
                continue;
            }
            Rectangle plantBody = plant.collisionBox(assets, time);
            double horizontalDistance = Math.abs(plantBody.getCenterX() - centerX);
            double horizontalRange = Layout.CELL_WIDTH * 1.5;
            if (horizontalDistance > horizontalRange) {
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
     * 参数：zombie 是爆炸中心的僵尸；centerX 是爆炸中心横坐标；assets 提供素材；
     * state 是当前游戏数据；time 是当前游戏时刻。
     */
    private static void damageHypnotizedZombies(Zombie zombie, int centerX,
            Assets assets, GameState state, long time) {
        for (Zombie other : state.zombies) {
            if (other == zombie || !other.alive || other.dying || !other.hypno) {
                continue;
            }
            if (Math.abs(other.row - zombie.row) > JOKER_EXPLOSION_RANGE) {
                continue;
            }
            Rectangle body = other.collisionBox(assets, time);
            double horizontalDistance = Math.abs(body.getCenterX() - centerX);
            double horizontalRange = Layout.CELL_WIDTH * 1.5;
            if (horizontalDistance > horizontalRange) {
                continue;
            }
            other.health = other.health - CombatValues.JOKER_EXPLOSION_DAMAGE;
        }
    }
}
