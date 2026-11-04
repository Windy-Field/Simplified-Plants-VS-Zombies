package pvz.zombie;

import java.awt.Rectangle;
import pvz.game.GameState;
import pvz.plant.Plant;
import pvz.world.Assets;
import pvz.world.Layout;

/** 处理巨人的挥棒、投掷和小鬼飞行；普通僵尸继续使用原战斗规则 */
public class GargantuarActions {
    private final Assets assets;
    private final GameState state;

    /** 参数 assets 提供动画，state 保存当前一局的数据 */
    public GargantuarActions(Assets assets, GameState state) {
        this.assets = assets;
        this.state = state;
    }

    /**
     * 推进特殊动作。参数 zombie 是当前僵尸；返回真表示本帧无需再走路或啃食
     * 冻结时间从动作时钟中扣除，恢复后继续原动作
     */
    public boolean update(Zombie zombie) {
        if (zombie.airborne) {
            updateFlight(zombie);
            return true;
        }
        if (!zombie.isGargantuar()) {
            return false;
        }

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

    /** 参数 giant 是半血的巨人；从头播放投掷，期间停止走路和挥棒 */
    private void startThrow(Zombie giant) {
        giant.throwingImp = true;
        giant.attacking = false;
        giant.actionElapsed = 0;
        giant.change("GargantuarThrow", assets, state.time);
    }

    /** 参数 giant 是正在投掷的巨人；脱手时只生成一只小鬼，动画结束再恢复行走 */
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

    /** 参数 giant 是投掷者；小鬼沿同一行飞出三格，阵营与巨人一致 */
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

    /** 参数 imp 是空中的小鬼；使用已有行走素材沿抛物线移动，落地才恢复碰撞和攻击 */
    private void updateFlight(Zombie imp) {
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

    /** 参数 giant 是挥棒者，plant 和 opponent 是当前目标；在挥棒落下时只结算一次伤害 */
    public void smash(Zombie giant, Plant plant, Zombie opponent) {
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
