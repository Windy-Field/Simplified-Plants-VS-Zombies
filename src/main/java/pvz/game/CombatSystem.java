package pvz.game;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import pvz.plant.Plant;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.Car;
import pvz.world.Layout;
import pvz.world.Sprite;
import pvz.world.Sun;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieEffects;
import pvz.zombie.ZombieActions;

/**
 * 负责一局战斗中实体的更新
 *
 * 这里按固定顺序调用僵尸行为，再处理子弹、阳光、小推车和短时特效
 * 僵尸的索敌与动作统一放在 ZombieActions，避免本类同时管理全部品种的细节
 * 关卡出怪时间表由 LevelSystem 负责，画面绘制由 GameRenderer 负责
 */
public class CombatSystem {
    /** 当前游戏的所有运行数据 */
    private final GameState state;

    /** 图片和动画资源 */
    private final Assets assets;

    /** 所有僵尸共用的行为入口，按统一顺序处理普通动作与特殊动作 */
    private final ZombieActions zombieActions;

    /**
     * 创建战斗系统
     *
     * 参数：gameState 是当前游戏状态；assetManager 是素材管理器
     */
    public CombatSystem(GameState gameState, Assets assetManager) {
        state = gameState;
        assets = assetManager;
        zombieActions = new ZombieActions(assets, state);
    }

    /** 推进一帧战斗中的僵尸、子弹、阳光、小推车和特效 */
    public void updateAll() {
        zombieActions.updateAll();
        updateBullets();
        updateSunsAndCars();
        updateEffects();
        updateHeads();
    }

    /** 更新子弹，并处理子弹命中僵尸 */
    private void updateBullets() {
        for (Bullet bullet : state.bullets) {
            if (!bullet.alive) {
                continue;
            }
            bullet.update(state.time);
            if (bullet.exploded) {
                continue;
            }
            transformBullet(bullet);
            if (bullet.alive) {
                hitZombieWithBullet(bullet);
            }
        }
    }

    /** 按植物资料转换经过植物的子弹 */
    private void transformBullet(Bullet bullet) {
        for (Plant plant : state.plants) {
            if (!plant.alive || plant.row != bullet.row) {
                continue;
            }
            if (plant.tryTransformBullet(bullet, assets, state.time)) {
                return;
            }
        }
    }

    /** 检查子弹是否命中僵尸 */
    private void hitZombieWithBullet(Bullet bullet) {
        for (Zombie zombie : state.zombies) {
            if (zombie.row != bullet.row || !zombie.alive || zombie.dying
                    || zombie.hypno || zombie.airborne) {
                continue;
            }
            if (!Sprite.touches(bullet, zombie, assets, state.time)) {
                continue;
            }
            zombie.health = zombie.health - bullet.damage;
            if (bullet.ice) {
                zombie.slowedUntil = state.time + Layout.ZOMBIE_SLOW_DURATION;
            }
            bullet.explode(assets, state.time);
            return;
        }
    }

    /** 推进阳光和小推车；阳光收集动画完成后才计入余额，避免点击时提前到账 */
    private void updateSunsAndCars() {
        for (Sun sun : state.suns) {
            if (!sun.alive) {
                continue;
            }
            boolean wasAlive = sun.alive;
            sun.update(assets, state.time);
            if (wasAlive && !sun.alive && sun.flyingToCounter) {
                state.sunValue = state.sunValue + sun.value;
            }
        }
        updateCars();
    }

    /** 更新小推车，并处理小推车撞到的僵尸 */
    private void updateCars() {
        BufferedImage carImage = assets.image("car");
        for (Car car : state.cars) {
            if (!car.alive) {
                continue;
            }
            car.update(state.time);
            int carTop = car.bottom - carImage.getHeight();
            Rectangle carRect = new Rectangle(car.x, carTop,
                carImage.getWidth(), carImage.getHeight());
            for (Zombie zombie : state.zombies) {
                if (zombie.row != car.row || !zombie.alive || zombie.dying
                        || zombie.hypno || zombie.airborne) {
                    continue;
                }
                if (!carRect.intersects(zombie.collisionBox(assets, state.time))) {
                    continue;
                }
                car.moving = true;
                ZombieEffects.die(zombie, assets, state, state.time, false);
            }
        }
    }

    /** 删除已经播完的爆炸和其他短时特效 */
    private void updateEffects() {
        for (Sprite effect : state.effects) {
            long duration = assets.count(effect.animation) * effect.frameInterval;
            if (state.time - effect.animationStart > duration) {
                effect.alive = false;
            }
        }
    }

    /** 删除已经播完的掉落僵尸头动画 */
    private void updateHeads() {
        for (Sprite head : state.heads) {
            long duration = assets.count(head.animation) * head.frameInterval;
            if (state.time - head.animationStart > duration) {
                head.alive = false;
            }
        }
    }
}
