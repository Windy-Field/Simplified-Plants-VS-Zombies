package pvz.game;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import pvz.plant.Plant;
import pvz.plant.PlantCatalog;
import pvz.plant.PlantRules;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.Car;
import pvz.world.CombatValues;
import pvz.world.Layout;
import pvz.world.Sprite;
import pvz.world.Sun;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieEffects;

/**
 * 负责一局战斗中实体的更新。
 *
 * 这里处理僵尸、子弹、阳光、小推车和短时特效。
 * 关卡出怪时间表由 LevelSystem 负责，画面绘制由 GameRenderer 负责。
 */
public class CombatSystem {
    /** 当前游戏的所有运行数据。 */
    private final GameState state;

    /** 图片和动画资源。 */
    private final Assets assets;

    /**
     * 创建战斗系统。
     *
     * 参数：gameState 是当前游戏状态；assetManager 是素材管理器。
     */
    public CombatSystem(GameState gameState, Assets assetManager) {
        state = gameState;
        assets = assetManager;
    }

    /** 推进一帧战斗中的僵尸、子弹、阳光、小推车和特效。 */
    public void updateAll() {
        updateZombies();
        updateBullets();
        updateSunsAndCars();
        updateEffects();
        updateHeads();
    }

    /** 遍历所有僵尸，处理走路、啃植物、掉帽子、掉头和死亡。 */
    private void updateZombies() {
        for (Zombie zombie : state.zombies) {
            if (!zombie.alive) {
                continue;
            }
            if (ZombieEffects.updateDeath(zombie, assets, state, state.time)) {
                continue;
            }
            if (zombie.health <= 0) {
                ZombieEffects.die(zombie, assets, state, state.time, false);
                continue;
            }
            updateZombieDamageState(zombie);
            updateZombieBleeding(zombie);

            // 被冻住的僵尸这一帧什么也不做。
            if (state.time < zombie.frozenUntil) {
                continue;
            }
            updateZombieActions(zombie);
        }
    }

    /** 处理僵尸掉帽子、掉臂和掉头这几个血量节点。 */
    private void updateZombieDamageState(Zombie zombie) {
        if (zombie.hasOwnAnimation()) {
            return;
        }
        if (zombie.helmet
                && zombie.health <= CombatValues.ZOMBIE_HELMET_LOST_HEALTH) {
            zombie.helmet = false;
            zombie.speed = zombie.speedAfterHelmet;
            zombie.change(zombie.stateAnimation(zombie.attacking), assets, state.time);
        }

        if (!zombie.armLost && zombie.health <= Layout.ZOMBIE_ARM_LOST_HEALTH) {
            zombie.armLost = true;
            String next = zombie.stateAnimation(zombie.attacking);
            zombie.frameInterval = Zombie.animationIntervalFor(next);
            zombie.change(next, assets, state.time);
        }

        if (zombie.headLost || zombie.health > Layout.ZOMBIE_HEAD_LOST_HEALTH) {
            return;
        }
        zombie.headLost = true;
        zombie.lastBleed = state.time;
        Rectangle old = zombie.bounds(assets, state.time);
        int center = (int) old.getCenterX();
        int bottom = (int) old.getMaxY();
        Sprite head = new Sprite("ZombieHead", center, bottom, zombie.row, 0, assets);
        head.animationStart = state.time;
        state.heads.add(head);
        zombie.change(zombie.stateAnimation(zombie.attacking), assets, state.time);
    }

    /** 掉了头的僵尸会持续流血，直到血流干自己倒下。 */
    private void updateZombieBleeding(Zombie zombie) {
        if (!zombie.headLost) {
            return;
        }
        if (state.time - zombie.lastBleed <= Layout.ZOMBIE_BLEED_INTERVAL) {
            return;
        }
        zombie.health = zombie.health - CombatValues.ZOMBIE_BLEED_DAMAGE;
        zombie.lastBleed = state.time;
    }

    /** 僵尸这一帧的主动行为：找到目标，然后决定是攻击还是走路。 */
    private void updateZombieActions(Zombie zombie) {
        if (zombie.hasOwnAnimation()) {
            Plant target = findExplodingZombiePlant(zombie);
            if (target != null) {
                ZombieEffects.die(zombie, assets, state, state.time, false);
                return;
            }
            if (hasTouchingProtectedPlant(zombie)) {
                return;
            }
        }

        Plant prey = null;
        Zombie opponent = null;
        if (zombie.hypno) {
            opponent = findZombieOpponent(zombie);
        } else {
            prey = findPrey(zombie);
            if (prey == null) {
                opponent = findHypnotizedOpponent(zombie);
            }
        }

        boolean fighting = prey != null || opponent != null;
        updateZombieFightingState(zombie, fighting);
        if (fighting) {
            bite(zombie, prey, opponent);
        } else {
            walk(zombie);
        }
        checkZombieOutOfScreen(zombie);
    }

    /** 找到爆炸僵尸接触到的第一株会触发爆炸的植物。 */
    private Plant findExplodingZombiePlant(Zombie zombie) {
        Rectangle zombieBody = zombie.collisionBox(assets, state.time);
        Plant firstPlant = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Plant plant : state.plants) {
            if (!plant.alive || plant.health <= 0 || plant.row != zombie.row) {
                continue;
            }
            if (PlantRules.isProtectedFromExplodingZombie(plant)) {
                continue;
            }
            if (!Sprite.touches(zombie, plant, assets, state.time)) {
                continue;
            }
            Rectangle plantBody = plant.collisionBox(assets, state.time);
            double distance = Math.abs(
                zombieBody.getCenterX() - plantBody.getCenterX());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                firstPlant = plant;
            }
        }
        return firstPlant;
    }

    /** 判断爆炸僵尸是否正接触攻击中的保护植物。 */
    private boolean hasTouchingProtectedPlant(Zombie zombie) {
        for (Plant plant : state.plants) {
            if (!plant.alive || plant.health <= 0 || plant.row != zombie.row) {
                continue;
            }
            if (!PlantRules.isProtectedFromExplodingZombie(plant)) {
                continue;
            }
            if (Sprite.touches(zombie, plant, assets, state.time)) {
                return true;
            }
        }
        return false;
    }

    /** 被魅惑的僵尸找同一行里的普通僵尸。 */
    private Zombie findZombieOpponent(Zombie zombie) {
        return findOpponent(zombie, false);
    }

    /** 普通僵尸找同一行里被魅惑的僵尸。 */
    private Zombie findHypnotizedOpponent(Zombie zombie) {
        return findOpponent(zombie, true);
    }

    /** 按目标阵营寻找同一行里接触到的僵尸。 */
    private Zombie findOpponent(Zombie zombie, boolean hypnotized) {
        for (Zombie other : state.zombies) {
            if (other == zombie || !other.alive || other.dying) {
                continue;
            }
            if (other.hypno != hypnotized || other.row != zombie.row) {
                continue;
            }
            if (Sprite.touches(zombie, other, assets, state.time)) {
                return other;
            }
        }
        return null;
    }

    /** 普通僵尸找同一行里可以吃掉的植物。 */
    private Plant findPrey(Zombie zombie) {
        for (Plant plant : state.plants) {
            if (!plant.alive || plant.health <= 0 || plant.row != zombie.row) {
                continue;
            }
            if (!PlantCatalog.canBeEaten(plant.name)) {
                continue;
            }
            if (Sprite.touches(zombie, plant, assets, state.time)) {
                return plant;
            }
        }
        return null;
    }

    /** 根据是否战斗切换僵尸动画。 */
    private void updateZombieFightingState(Zombie zombie, boolean fighting) {
        if (fighting == zombie.attacking) {
            return;
        }
        zombie.attacking = fighting;
        String next = zombie.stateAnimation(fighting);
        zombie.frameInterval = Zombie.animationIntervalFor(next);
        zombie.change(next, assets, state.time);
        zombie.lastAttack = state.time;
    }

    /** 僵尸攻击植物或另一只僵尸。 */
    private void bite(Zombie zombie, Plant prey, Zombie opponent) {
        long interval = Layout.ZOMBIE_ATTACK_INTERVAL;
        if (state.time < zombie.slowedUntil) {
            interval = interval * 2;
        }
        if (state.time - zombie.lastAttack <= interval) {
            return;
        }
        if (prey != null) {
            prey.health = prey.health - CombatValues.ZOMBIE_BITE_DAMAGE;
            boolean isHypnoShroom = prey.name.equals("HypnoShroom");
            if (prey.health <= 0 && isHypnoShroom && !prey.sleeping) {
                zombie.hypno = true;
            }
        }
        if (opponent != null) {
            opponent.health = opponent.health - CombatValues.ZOMBIE_BITE_DAMAGE;
        }
        zombie.lastAttack = state.time;
    }

    /** 僵尸向当前阵营的方向走一步。 */
    private void walk(Zombie zombie) {
        long interval = Layout.ZOMBIE_STEP_INTERVAL;
        if (state.time < zombie.slowedUntil) {
            interval = interval * 2;
        }
        if (state.time - zombie.lastStep <= interval) {
            return;
        }
        if (zombie.hypno) {
            zombie.x = zombie.x + zombie.speed;
        } else {
            zombie.x = zombie.x - zombie.speed;
        }
        zombie.lastStep = state.time;
    }

    /** 处理僵尸离开屏幕的结果。 */
    private void checkZombieOutOfScreen(Zombie zombie) {
        Rectangle bounds = zombie.bounds(assets, state.time);
        if (!zombie.hypno && bounds.getMaxX() < 0) {
            state.screen = GameScreen.LOSS;
            state.screenStart = state.time;
        }
        if (zombie.hypno && bounds.x > Layout.WINDOW_WIDTH) {
            zombie.alive = false;
        }
    }

    /** 更新子弹，并处理子弹命中僵尸。 */
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

    /** 按植物资料转换经过植物的子弹。 */
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

    /** 检查子弹是否命中僵尸。 */
    private void hitZombieWithBullet(Bullet bullet) {
        for (Zombie zombie : state.zombies) {
            if (zombie.row != bullet.row || !zombie.alive || zombie.dying || zombie.hypno) {
                continue;
            }
            if (!Sprite.touches(bullet, zombie, assets, state.time)) {
                continue;
            }
            zombie.health = zombie.health - bullet.damageAmount(CombatValues.BULLET_DAMAGE);
            if (bullet.ice) {
                zombie.slowedUntil = state.time + Layout.ZOMBIE_SLOW_DURATION;
            }
            bullet.explode(assets, state.time);
            return;
        }
    }

    /** 更新阳光、小推车、特效和掉落的僵尸头。 */
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

    /** 更新小推车，并处理小推车撞到的僵尸。 */
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
                if (zombie.row != car.row || !zombie.alive || zombie.dying || zombie.hypno) {
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

    /** 删除已经播完的爆炸和其他短时特效。 */
    private void updateEffects() {
        for (Sprite effect : state.effects) {
            long duration = assets.count(effect.animation) * effect.frameInterval;
            if (state.time - effect.animationStart > duration) {
                effect.alive = false;
            }
        }
    }

    /** 删除已经播完的掉落僵尸头动画。 */
    private void updateHeads() {
        for (Sprite head : state.heads) {
            long duration = assets.count(head.animation) * head.frameInterval;
            if (state.time - head.animationStart > duration) {
                head.alive = false;
            }
        }
    }
}
