package pvz.zombie;

import java.awt.Rectangle;
import pvz.game.GameScreen;
import pvz.game.GameState;
import pvz.plant.Plant;
import pvz.plant.PlantCatalog;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.world.Sprite;

/** 统一更新所有僵尸的受伤、索敌、移动和攻击，巨人与小鬼的特殊动作也放在同一流程中 */
public class ZombieActions {
    /** 所有僵尸共用的图片和动画 */
    private final Assets assets;

    /** 当前一局的对象、时钟和画面状态 */
    private final GameState state;

    /** 参数 assets 提供素材，state 提供本局数据；创建共用行为入口，不按品种创建处理器 */
    public ZombieActions(Assets assets, GameState state) {
        this.assets = assets;
        this.state = state;
    }

    /** 按原有顺序更新死亡、受伤和主动动作，飞行或投掷中的僵尸不再执行普通走路 */
    public void updateAll() {
        // 小鬼可能在遍历中生成，本帧只更新开始时已有的僵尸，避免边遍历边添加出错
        int zombieCount = state.zombies.size();
        for (int index = 0; index < zombieCount; index = index + 1) {
            Zombie zombie = state.zombies.get(index);
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
            updateDamageState(zombie);
            updateBleeding(zombie);

            // TODO【17-僵尸特殊能力】：少量特殊动作写成本类的具名方法，在普通走路和攻击之前调用
            // 动作按时钟推进，伤害只结算一次，并检查冻结和死亡能否正确中断，不按每个品种新增文件
            if (zombie.airborne) {
                updateImpFlight(zombie);
                continue;
            }
            if (zombie.isGargantuar() && updateGargantuar(zombie)) {
                continue;
            }
            if (state.time < zombie.frozenUntil) {
                continue;
            }
            updateActions(zombie);
        }
    }

    /** 参数 zombie 是当前僵尸；按血量切换装备、独臂和掉头状态，特殊品种跳过普通规则 */
    private void updateDamageState(Zombie zombie) {
        if (!zombie.usesStandardDamageStates()) {
            return;
        }
        if (zombie.helmet && zombie.health <= Layout.ZOMBIE_HELMET_LOST_HEALTH) {
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
        Rectangle body = zombie.bounds(assets, state.time);
        int center = (int) body.getCenterX();
        int bottom = (int) body.getMaxY();
        Sprite head = new Sprite("ZombieHead", center, bottom, zombie.row, 0, assets);
        head.animationStart = state.time;
        state.heads.add(head);
        zombie.change(zombie.stateAnimation(zombie.attacking), assets, state.time);
    }

    /** 参数 zombie 是当前僵尸；掉头后按固定间隔扣血，保持原来的死亡过渡 */
    private void updateBleeding(Zombie zombie) {
        if (!zombie.headLost) {
            return;
        }
        if (state.time - zombie.lastBleed <= Layout.ZOMBIE_BLEED_INTERVAL) {
            return;
        }
        zombie.health = zombie.health - Layout.ZOMBIE_BLEED_DAMAGE;
        zombie.lastBleed = state.time;
    }

    /** 参数 zombie 是当前僵尸；先处理开盒，再按阵营寻找目标，决定攻击或走路 */
    private void updateActions(Zombie zombie) {
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
            opponent = findOpponent(zombie, false);
        } else {
            prey = findPrey(zombie);
            if (prey == null) {
                opponent = findOpponent(zombie, true);
            }
        }
        boolean fighting = prey != null || opponent != null;
        updateFightingState(zombie, fighting);
        if (fighting) {
            attack(zombie, prey, opponent);
        } else {
            walk(zombie);
        }
        checkOutOfScreen(zombie);
    }

    /** 参数 zombie 是开盒僵尸；返回接触到的最近可引爆植物，攻击中受保护的植物不触发开盒 */
    private Plant findExplodingZombiePlant(Zombie zombie) {
        Rectangle zombieBody = zombie.collisionBox(assets, state.time);
        Plant firstPlant = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Plant plant : state.plants) {
            if (!plant.alive || plant.health <= 0 || plant.row != zombie.row) {
                continue;
            }
            if (plant.isProtectedFromExplodingZombie()) {
                continue;
            }
            if (!Sprite.touches(zombie, plant, assets, state.time)) {
                continue;
            }
            Rectangle plantBody = plant.collisionBox(assets, state.time);
            double distance = Math.abs(zombieBody.getCenterX() - plantBody.getCenterX());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                firstPlant = plant;
            }
        }
        return firstPlant;
    }

    /** 参数 zombie 是开盒僵尸；返回是否碰到受保护植物，避免穿过正在攻击的大嘴花或窝瓜 */
    private boolean hasTouchingProtectedPlant(Zombie zombie) {
        for (Plant plant : state.plants) {
            if (!plant.alive || plant.health <= 0 || plant.row != zombie.row) {
                continue;
            }
            if (!plant.isProtectedFromExplodingZombie()) {
                continue;
            }
            if (Sprite.touches(zombie, plant, assets, state.time)) {
                return true;
            }
        }
        return false;
    }

    /** 参数 zombie 是索敌者，hypnotized 是目标阵营；返回同一行接触到的第一只敌方僵尸 */
    private Zombie findOpponent(Zombie zombie, boolean hypnotized) {
        for (Zombie other : state.zombies) {
            if (other == zombie || !other.alive || other.dying || other.airborne) {
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

    /** 参数 zombie 是索敌者；返回同一行接触到的可食用植物，没有目标时返回 null */
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

    /** 参数 zombie 是当前僵尸，fighting 表示有攻击目标；只在状态改变时重启动画和攻击计时 */
    private void updateFightingState(Zombie zombie, boolean fighting) {
        if (fighting == zombie.attacking) {
            return;
        }
        zombie.attacking = fighting;
        String next = zombie.stateAnimation(fighting);
        zombie.frameInterval = Zombie.animationIntervalFor(next);
        zombie.change(next, assets, state.time);
        zombie.lastAttack = state.time;
        zombie.actionElapsed = 0;
        zombie.lastActionUpdate = state.time;
        zombie.smashResolved = false;
    }

    /** 参数 prey 和 opponent 是植物或僵尸目标；巨人挥棒，其他僵尸按啃咬间隔扣血 */
    private void attack(Zombie zombie, Plant prey, Zombie opponent) {
        if (zombie.isGargantuar()) {
            smash(zombie, prey, opponent);
            return;
        }
        long interval = Layout.ZOMBIE_ATTACK_INTERVAL;
        if (state.time < zombie.slowedUntil) {
            interval = interval * 2;
        }
        if (state.time - zombie.lastAttack <= interval) {
            return;
        }
        if (prey != null) {
            prey.health = prey.health - Layout.ZOMBIE_BITE_DAMAGE;
            boolean isHypnoShroom = prey.name.equals("HypnoShroom");
            if (prey.health <= 0 && isHypnoShroom && !prey.sleeping) {
                zombie.hypno = true;
            }
        }
        if (opponent != null) {
            opponent.health = opponent.health - Layout.ZOMBIE_BITE_DAMAGE;
        }
        zombie.lastAttack = state.time;
    }

    /** 参数 zombie 是当前僵尸；按阵营方向走一步，寒冰减速时延长两步之间的等待 */
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

    /** 参数 zombie 是当前僵尸；敌人穿过左边界判负，魅惑僵尸走出右边界后移除 */
    private void checkOutOfScreen(Zombie zombie) {
        Rectangle bounds = zombie.bounds(assets, state.time);
        if (!zombie.hypno && bounds.getMaxX() < 0) {
            state.screen = GameScreen.LOSS;
            state.screenStart = state.time;
        }
        if (zombie.hypno && bounds.x > Layout.WINDOW_WIDTH) {
            zombie.alive = false;
        }
    }

    /** 参数 zombie 是巨人；返回真表示冻结或投掷占用了本帧，普通攻击继续使用共用入口 */
    private boolean updateGargantuar(Zombie zombie) {
        long elapsed = activeElapsed(zombie.lastActionUpdate, zombie.frozenUntil);
        zombie.lastActionUpdate = state.time;
        if (state.time < zombie.slowedUntil) {
            elapsed = elapsed / 2;
        }
        if (zombie.attacking || zombie.throwingImp) {
            zombie.actionElapsed = zombie.actionElapsed + elapsed;
            zombie.animationStart = state.time - zombie.actionElapsed;
        }
        if (state.time < zombie.frozenUntil) {
            return true;
        }
        if (!zombie.throwingImp && !zombie.impThrown
                && zombie.health <= Layout.GARGANTUAR_HEALTH / 2) {
            startThrow(zombie);
        }
        if (zombie.throwingImp) {
            updateThrow(zombie);
            return true;
        }
        return false;
    }

    /** 参数 previous 是上次更新时间，frozenUntil 是解冻时刻；返回期间未被冻结的毫秒数 */
    private long activeElapsed(long previous, long frozenUntil) {
        long start = Math.max(previous, frozenUntil);
        return Math.max(0, state.time - start);
    }

    /** 参数 giant 是半血巨人；从头播放投掷，期间停止走路和挥棒 */
    private void startThrow(Zombie giant) {
        giant.throwingImp = true;
        giant.attacking = false;
        giant.actionElapsed = 0;
        giant.change("GargantuarThrow", assets, state.time);
    }

    /** 参数 giant 是投掷中的巨人；脱手时只生成一只小鬼，动画结束后恢复行走 */
    private void updateThrow(Zombie giant) {
        if (!giant.impThrown && giant.actionElapsed >= assets.gargantuarReleaseDelay()) {
            giant.impThrown = true;
            releaseImp(giant);
        }
        long duration = (long) assets.count(giant.animation) * giant.frameInterval;
        if (giant.actionElapsed >= duration) {
            giant.throwingImp = false;
            giant.change(giant.stateAnimation(false), assets, state.time);
            giant.lastStep = state.time;
        }
    }

    /** 参数 giant 是投掷者；小鬼沿同一行飞出三格，落点限制在草坪内，阵营与巨人一致 */
    private void releaseImp(Zombie giant) {
        Rectangle body = giant.collisionBox(assets, state.time);
        int bottom = Layout.rowBottom(giant.row);
        Zombie imp = new Zombie("Imp", giant.row, bottom, assets);
        imp.hypno = giant.hypno;
        Rectangle impBody = imp.bounds(assets, state.time);
        imp.x = imp.x + body.getCenterX() - impBody.getCenterX();
        imp.y = imp.y + bottom - impBody.getMaxY();
        imp.flightTargetY = imp.y;
        imp.flightStartY = imp.y - Layout.IMP_RELEASE_HEIGHT;
        imp.flightStartX = imp.x;
        double distance = -Layout.IMP_THROW_DISTANCE;
        if (giant.hypno) {
            distance = Layout.IMP_THROW_DISTANCE;
        }
        double targetCenter = body.getCenterX() + distance;
        targetCenter = Math.max(Layout.columnCenter(0), targetCenter);
        targetCenter = Math.min(Layout.columnCenter(Layout.COLUMN_COUNT - 1), targetCenter);
        imp.flightTargetX = imp.x + targetCenter - body.getCenterX();
        imp.y = imp.flightStartY;
        imp.airborne = true;
        imp.lastFlightUpdate = state.time;
        imp.animationStart = state.time;
        state.zombies.add(imp);
    }

    /** 参数 imp 是空中的小鬼；按抛物线移动，落地后才恢复地面碰撞和攻击 */
    private void updateImpFlight(Zombie imp) {
        long elapsed = activeElapsed(imp.lastFlightUpdate, imp.frozenUntil);
        imp.lastFlightUpdate = state.time;
        imp.flightElapsed = imp.flightElapsed + elapsed;
        double progress = (double) imp.flightElapsed / Layout.IMP_FLIGHT_DURATION;
        progress = Math.min(1, progress);
        imp.x = imp.flightStartX + (imp.flightTargetX - imp.flightStartX) * progress;
        double height = Layout.IMP_FLIGHT_HEIGHT * 4 * progress * (1 - progress);
        imp.y = imp.flightStartY + (imp.flightTargetY - imp.flightStartY) * progress - height;
        if (progress >= 1) {
            imp.airborne = false;
            imp.lastStep = state.time;
            imp.lastAttack = state.time;
        }
    }

    /** 参数 giant 是挥棒者，plant 和 opponent 是当前目标；落棒时只结算一次伤害 */
    private void smash(Zombie giant, Plant plant, Zombie opponent) {
        if (!giant.smashResolved && giant.actionElapsed >= Layout.GARGANTUAR_SMASH_DELAY) {
            giant.smashResolved = true;
            if (plant != null) {
                plant.health = 0;
                plant.alive = false;
                state.occupied[plant.row][plant.column] = false;
            }
            if (opponent != null) {
                opponent.health = opponent.health - Layout.GARGANTUAR_SMASH_DAMAGE;
            }
        }
        long duration = (long) assets.count(giant.animation) * giant.frameInterval;
        if (giant.actionElapsed >= duration) {
            giant.actionElapsed = 0;
            giant.smashResolved = false;
            giant.change(giant.stateAnimation(true), assets, state.time);
        }
    }
}
