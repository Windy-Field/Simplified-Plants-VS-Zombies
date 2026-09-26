package pvz.plant;

import java.awt.Rectangle;
import pvz.game.GameState;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.Layout;
import pvz.zombie.Zombie;

/**
 * ShooterActions 负责这一类植物每一帧的行为。
 *
 * 参数通过构造函数传入，处理结果直接写回这一局的游戏状态。
 */
public class ShooterActions {
    /** 图片和动画资源。 */
    private final Assets assets;
    /** 当前游戏的数据。 */
    private final GameState state;

    /**
     * 创建ShooterActions。
     *
     * 参数：originalAssets 提供素材；gameState 是当前游戏的数据。
     */
    public ShooterActions(Assets originalAssets, GameState gameState) {
        assets = originalAssets;
        state = gameState;
    }

    /**
     * 射手每帧检查一次：身前同一行有没有僵尸，有就开火。
     *
     * 三线射手还会照顾上下各一行；胆小菇附近有僵尸时会吓得不出手。
     *
     * 参数：plant 是那株射手。
     */
    public void update(Plant plant) {
        boolean hasTarget = false;
        boolean inDanger = false;

        for (Zombie zombie : state.zombies) {
            if (!zombie.alive || zombie.dying || zombie.hypno) {
                continue;
            }
            Rectangle target = zombie.bounds(assets, state.time);
            // 僵尸已经走到植物身后就不用管了。
            if (target.getMaxX() < plant.x) {
                continue;
            }
            if (zombie.row == plant.row) {
                // 小喷菇射程短，只有三百二十像素内的僵尸才打。
                boolean inRange = true;
                if (plant.name.equals("PuffShroom") && target.x > plant.x + 320) {
                    inRange = false;
                }
                if (inRange) {
                    hasTarget = true;
                }
            } else if (plant.name.equals("Threepeater") && Math.abs(zombie.row - plant.row) == 1) {
                hasTarget = true;
            }
            if (zombie.row == plant.row && target.x < plant.x + 160) {
                inDanger = true;
            }
        }

        if (plant.name.equals("ScaredyShroom")) {
            if (!updateScaredyShroom(plant, inDanger)) {
                return;
            }
        }
        if (!hasTarget) {
            return;
        }

        long interval = Layout.SHOOT_INTERVAL;
        if (plant.name.equals("PuffShroom")) {
            interval = Layout.SHROOM_SHOOT_INTERVAL;
        }
        if (state.time - plant.lastAction <= interval) {
            return;
        }

        fireBullets(plant);
        plant.lastAction = state.time;
    }

    /**
     * 更新胆小菇的表情。
     *
     * 参数：plant 是那株胆小菇；inDanger 表示附近有没有僵尸。
     * 返回：现在可以开火就返回真；被吓住返回假。
     */
    private boolean updateScaredyShroom(Plant plant, boolean inDanger) {
        boolean crying = plant.animation.equals("ScaredyShroomCry");
        if (inDanger && !crying) {
            plant.change("ScaredyShroomCry", assets, state.time);
        } else if (!inDanger && crying) {
            plant.change("ScaredyShroom", assets, state.time);
        }
        return !inDanger;
    }

    /**
     * 真正生成子弹：三线射手一次射三行，双发射手一次射两颗。
     *
     * 参数：plant 是开火的射手。
     */
    private void fireBullets(Plant plant) {
        Rectangle source = plant.bounds(assets, state.time);
        String bulletName = bulletNameFor(plant);
        // 子弹从植物身体的右边缘射出。
        int muzzleX = source.x + source.width;

        // 子弹要长在嘴上，所以先算出嘴的中心高度，再把子弹的可见部分上下居中到这一点。
        int muzzleY = source.y + muzzleOffset(plant);
        int[] bulletVisible = assets.animationBounds(bulletName);
        int bulletHeight = bulletVisible[3] - bulletVisible[1];
        int top = muzzleY - bulletHeight / 2;

        if (plant.name.equals("Threepeater")) {
            for (int offset = -1; offset <= 1; offset++) {
                int lane = plant.row + offset;
                if (lane < 0 || lane >= Layout.ROW_COUNT) {
                    continue;
                }
                int targetY = top + offset * Layout.CELL_HEIGHT;
                state.bullets.add(new Bullet(bulletName, muzzleX, top, lane, targetY, assets));
            }
            return;
        }

        state.bullets.add(new Bullet(bulletName, muzzleX, top, plant.row, top, assets));
        // 双发射手的两颗子弹前后错开 40 像素，看起来就是连发两颗。
        if (plant.name.equals("RepeaterPea")) {
            int secondLeft = muzzleX + 40;
            state.bullets.add(new Bullet(bulletName, secondLeft, top, plant.row, top, assets));
        }
    }

    /**
     * 查一种射手的嘴比身体可见范围的上沿低多少像素。
     *
     * 嘴长在头的中下部，不是头顶。以前统一用身体上沿当子弹高度，
     * 看起来就像子弹从头顶冒出来，所以要按品种各挪一个量。
     *
     * 参数：plant 是开火的射手。
     * 返回：嘴中心相对身体上沿的纵向偏移。
     */
    // TODO：【选做-1】新增植物时可以调整子弹出射点
    private int muzzleOffset(Plant plant) {
        if (plant.name.equals("SnowPea")) {
            return Layout.SNOW_PEA_MUZZLE_OFFSET;
        }
        if (plant.name.equals("RepeaterPea")) {
            return Layout.REPEATER_MUZZLE_OFFSET;
        }
        if (plant.name.equals("Threepeater")) {
            return Layout.THREEPEATER_MUZZLE_OFFSET;
        }
        if (plant.name.equals("PuffShroom")) {
            return Layout.PUFF_SHROOM_MUZZLE_OFFSET;
        }
        if (plant.name.equals("ScaredyShroom")) {
            return Layout.SCAREDY_SHROOM_MUZZLE_OFFSET;
        }
        return Layout.PEA_SHOOTER_MUZZLE_OFFSET;
    }

    /**
     * 按植物品种决定子弹素材名。
     *
     * 参数：plant 是开火的射手。
     * 返回：子弹的素材名。
     */
    // TODO：【选做-2】新增植物时可以改变子弹类型
    private String bulletNameFor(Plant plant) {
        if (plant.name.equals("SnowPea")) {
            return "PeaIce";
        }
        if (plant.name.equals("PuffShroom") || plant.name.equals("ScaredyShroom")) {
            return "BulletMushRoom";
        }
        return "PeaNormal";
    }
}
