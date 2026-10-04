package pvz.plant;

import java.awt.Rectangle;
import pvz.game.GameState;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.world.Sprite;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieEffects;

/**
 * CloseAttackActions 负责这一类植物每一帧的行为。
 *
 * 参数通过构造函数传入，处理结果直接写回这一局的游戏状态。
 */
public class CloseAttackActions {
    /** 图片和动画资源。 */
    private final Assets assets;
    /** 当前游戏的数据。 */
    private final GameState state;

    /**
     * 创建CloseAttackActions。
     *
     * 参数：assets 提供素材；gameState 是当前游戏的数据。
     */
    public CloseAttackActions(Assets assets, GameState gameState) {
        this.assets = assets;
        state = gameState;
    }

    /**
     * 近距离植物：土豆雷、窝瓜、食人花、地刺和保龄球。
     *
     * 它们的共同点是必须等僵尸走到身上才生效，所以先处理各自的"预备动作"，
     * 再统一遍历僵尸判断有没有挨上。
     *
     * 参数：plant 是那株植物。
     */
    public void update(Plant plant) {
        String name = plant.name;
        PlantDefinition definition = PlantCatalog.definitionOf(name);
        chargeUpPotatoMine(plant);
        if (PlantCatalog.isBowling(name)) {
            rollBowling(plant);
        }

        // 窝瓜检测左中右三格的僵尸，不需要碰撞判定。
        if (name.equals("Squash") && !plant.triggered) {
            Zombie target = findSquashTarget(plant);
            if (target != null) {
                triggerSquash(plant, target);
                return;
            }
        }
        if (name.equals("Squash") && plant.triggered) {
            resolveCloseAttackTimeout(plant);
            return;
        }

        for (Zombie zombie : state.zombies) {
            if (!zombie.alive || zombie.dying || zombie.hypno || zombie.row != plant.row) {
                continue;
            }
            boolean touching = Sprite.touches(plant, zombie, assets, state.time);
            boolean inForwardRange = inForwardRange(plant, zombie, definition);
            if (!touching && !inForwardRange) {
                continue;
            }
            if (handleCloseHit(plant, zombie)) {
                break;
            }
        }

        resolveCloseAttackTimeout(plant);
    }

    /**
     * 判断僵尸是否进入植物资料规定的前方索敌范围。
     *
     * 这个范围只向右计算，因为植物默认面向右边。
     * 这里直接使用僵尸躯干中心的实时横坐标，不把它先换算成列号，
     * 避免僵尸刚跨过格子边界时攻击判定突然跳变。
     * 参数：plant 是正在攻击的植物；zombie 是待检查的僵尸；
     * definition 是植物固定资料。
     * 返回：僵尸在前方指定格子内时返回真。
     */
    private boolean inForwardRange(Plant plant, Zombie zombie,
            PlantDefinition definition) {
        if (definition.forwardAttackRange <= 0) {
            return false;
        }
        return inPlantRange(plant, zombie, 0,
            definition.forwardAttackRange);
    }

    /**
     * 判断僵尸躯干中心是否落在植物周围的连续横坐标范围内。
     *
     * 参数：plant 是范围中心植物；zombie 是待检查的僵尸；
     * leftCells 是向左包含几格；rightCells 是向右包含几格。
     * 返回：僵尸在范围内时返回真。
     */
    private boolean inPlantRange(Plant plant, Zombie zombie,
            int leftCells, int rightCells) {
        double left = Layout.GRID_LEFT
            + (plant.column - leftCells) * Layout.CELL_WIDTH;
        double right = Layout.GRID_LEFT
            + (plant.column + rightCells + 1) * Layout.CELL_WIDTH;
        return inHorizontalRange(zombie, left, right);
    }

    /**
     * 判断僵尸的躯干中心是否落在指定横向范围内。
     *
     * 参数：zombie 是待检查的僵尸；left 和 right 是范围两端的横坐标。
     * 返回：在范围内返回真。
     */
    private boolean inHorizontalRange(Zombie zombie, double left, double right) {
        Rectangle zombieBody = zombie.collisionBox(assets, state.time);
        double zombieCenter = zombieBody.getCenterX();
        return zombieCenter >= left && zombieCenter <= right;
    }

    /**
     * 判断僵尸躯干是否碰到窝瓜的压扁范围。
     *
     * 参数：zombie 是待检查的僵尸；left 和 right 是压扁范围的两端。
     * 返回：躯干与范围相交时返回真。
     */
    private boolean touchesHorizontalRange(Zombie zombie, double left, double right) {
        Rectangle zombieBody = zombie.collisionBox(assets, state.time);
        if (zombieBody.getMaxX() < left) {
            return false;
        }
        return zombieBody.getMinX() <= right;
    }

    /**
     * 窝瓜检测左中右三格内距离种植格中心最近的僵尸。
     *
     * 参数：plant 是那株窝瓜。
     * 返回：最近的目标僵尸，没找到返回 null。
     */
    private Zombie findSquashTarget(Plant plant) {
        Zombie nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        double plantCenter = Layout.columnCenter(plant.column);
        for (Zombie zombie : state.zombies) {
            if (!zombie.alive || zombie.dying || zombie.hypno || zombie.row != plant.row) {
                continue;
            }
            if (!inPlantRange(plant, zombie, 1, 1)) {
                continue;
            }
            Rectangle zombieBody = zombie.collisionBox(assets, state.time);
            double zombieCenter = zombieBody.getCenterX();
            double distance = Math.abs(zombieCenter - plantCenter);
            if (distance < nearestDistance) {
                nearest = zombie;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /**
     * 让窝瓜开始攻击，并把它的身体对准选中的僵尸。
     *
     * 参数：plant 是窝瓜；target 是即将被砸的僵尸。
     */
    private void triggerSquash(Plant plant, Zombie target) {
        plant.change("SquashAttack", assets, state.time);
        plant.triggered = true;
        plant.stateStart = state.time;
        plant.target = target;
        // 换完动画再挪位置：change 会因为画布大小不同而调整坐标。
        placeSquashOnTarget(plant, target);
    }

    /**
     * 把窝瓜挪到目标僵尸身上，让它正好砸在僵尸站着的位置。
     *
     * 不能直接把 plant.x 赋成 zombie.x：那是两张图的画布左沿。
     * 窝瓜的画布 100 像素宽、僵尸的 166 像素宽，身体在各自画布里的位置也不一样
     * （僵尸的身体只占画布靠右的一截），按画布左沿对齐会让瓜身停在僵尸左边大半格，
     * 向左砸、向右砸都偏。
     *
     * 用"窝瓜身体中心对准僵尸躯干中心"来定位，索敌和落点使用同一个实时参照。
     *
     * 参数：plant 是那株窝瓜；zombie 是它盯上的僵尸。
     */
    private void placeSquashOnTarget(Plant plant, Zombie zombie) {
        Rectangle preyBody = zombie.collisionBox(assets, state.time);
        int[] ownBody = assets.animationBounds(plant.animation);
        // 窝瓜身体中心在它自己画布里的横坐标。
        double ownCenterInImage = (ownBody[0] + ownBody[2]) / 2.0;
        // 让"窝瓜身体中心"落在"僵尸躯干中心"上，反推出窝瓜画布左沿该放哪。
        plant.x = preyBody.getCenterX() - ownCenterInImage;
    }

    /**
     * 土豆雷埋够十五秒才出土，出土后才有效。
     *
     * 参数：plant 是那颗土豆雷。
     */
    private void chargeUpPotatoMine(Plant plant) {
        if (!plant.name.equals("PotatoMine")) {
            return;
        }
        if (state.time - plant.placed <= Layout.POTATO_MINE_ARM_TIME) {
            return;
        }
        if (plant.animation.equals("PotatoMineInit")) {
            plant.change("PotatoMine", assets, state.time);
        }
    }

    /**
     * 保龄球一边往右滚一边上下弹，撞到僵尸就掉血。
     *
     * 参数：plant 是那颗保龄球。
     */
    private void rollBowling(Plant plant) {
        if (state.time - plant.lastAction <= Layout.BOWLING_MOVE_INTERVAL) {
            return;
        }
        // 红坚果已经炸开，就停在原地放爆炸动画，不能再往前挪。
        if (plant.name.equals("RedWallNutBowling") && plant.triggered) {
            return;
        }
        plant.x = plant.x + Layout.BOWLING_MOVE_STEP;

        if (plant.name.equals("WallNutBowling") && plant.triggered) {
            if (plant.attacking) {
                plant.y = plant.y + Layout.BOWLING_MOVE_STEP;
            } else {
                plant.y = plant.y - Layout.BOWLING_MOVE_STEP;
            }
            // 在草坪上下边缘之间来回弹。
            if (plant.y < Layout.GRID_TOP) {
                plant.attacking = true;
            }
            if (plant.y > 490) {
                plant.attacking = false;
            }
            // 滚到哪一行就算在哪一行，这样才知道该撞谁。
            int row = ((int) plant.y + 35 - Layout.GRID_TOP) / Layout.CELL_HEIGHT;
            if (Layout.insideGrid(row, 0)) {
                plant.row = row;
            }
        }
        plant.lastAction = state.time;

        if (plant.x > Layout.WINDOW_WIDTH) {
            plant.health = 0;
        }
    }

    /**
     * 处理一次"僵尸挨到植物"的判定。
     *
     * 参数：plant 是那株植物；zombie 是挨上的僵尸。
     * 返回：真表示这颗植物已经用完（比如土豆雷炸了），调用方可以停止遍历僵尸。
     */
    private boolean handleCloseHit(Plant plant, Zombie zombie) {
        String name = plant.name;

        if (name.equals("PotatoMine") && !plant.animation.equals("PotatoMineInit")) {
            plant.change("PotatoMineExplode", assets, state.time);
            plant.health = 0;
            blast(plant, 0, 55);
            return true;
        }
        if (name.equals("Squash") && !plant.triggered) {
            // 躯干边缘先碰到窝瓜时，也要和主动索敌走同一套攻击动作。
            triggerSquash(plant, zombie);
        }
        if (name.equals("Chomper") && !plant.triggered) {
            plant.change("ChomperAttack", assets, state.time);
            plant.triggered = true;
            plant.stateStart = state.time;
            plant.target = zombie;
        }
        if (name.equals("Spikeweed") && state.time - plant.lastAction > Layout.SPIKEWEED_DAMAGE_INTERVAL) {
            zombie.health = zombie.health - Layout.SPIKEWEED_DAMAGE;
            plant.lastAction = state.time;
        }
        if (name.equals("WallNutBowling") && state.time - plant.stateStart > Layout.BOWLING_HIT_INTERVAL) {
            zombie.health = zombie.health - Layout.BOWLING_DAMAGE;
            plant.triggered = true;
            plant.stateStart = state.time;
        }
        if (name.equals("RedWallNutBowling") && !plant.triggered) {
            plant.triggered = true;
            plant.stateStart = state.time;
            plant.change("RedWallNutBowlingExplode", assets, state.time);
            blast(plant, 1, 120);
        }
        return false;
    }

    /**
     * 处理需要等一会儿才生效的后续动作：窝瓜压下去、食人花吞下去。
     *
     * 参数：plant 是那株植物。
     */
    private void resolveCloseAttackTimeout(Plant plant) {
        String name = plant.name;

        if (name.equals("Squash") && plant.triggered) {
            if (state.time - plant.stateStart > Layout.SQUASH_HIT_DELAY) {
                // 目标起跳后仍会移动，砸下时跟上它，让周围的僵尸一起受击。
                if (plant.target != null && plant.target.alive && !plant.target.dying) {
                    placeSquashOnTarget(plant, plant.target);
                }

                // 目标提前死亡时沿用起跳位置；其他情况以目标当前的位置为中心。
                Rectangle landingBody = plant.bounds(assets, state.time);
                double landingCenter = landingBody.getCenterX();
                double halfCell = Layout.CELL_WIDTH / 2.0;
                double left = landingCenter - halfCell;
                double right = landingCenter + halfCell;
                for (Zombie zombie : state.zombies) {
                    if (!zombie.alive || zombie.dying || zombie.row != plant.row) {
                        continue;
                    }
                    if (touchesHorizontalRange(zombie, left, right)) {
                        ZombieEffects.die(zombie, assets, state, state.time, false);
                    }
                }
                plant.health = 0;
            }
            return;
        }

        if (name.equals("Chomper")) {
            updateChomper(plant);
            return;
        }

        if (name.equals("RedWallNutBowling") && plant.triggered) {
            if (state.time - plant.stateStart > Layout.RED_BOWLING_EXPLODE_DELAY) {
                plant.health = 0;
            }
        }
    }

    /**
     * 食人花咬住僵尸后吞掉它，再消化十六秒才能咬下一只。
     *
     * 参数：plant 是那株食人花。
     */
    private void updateChomper(Plant plant) {
        boolean attacking = plant.animation.equals("ChomperAttack");
        if (plant.triggered && attacking) {
            if (state.time - plant.stateStart > Layout.CHOMPER_SWALLOW_DELAY) {
                if (plant.target != null) {
                    ZombieEffects.die(plant.target, assets, state, state.time, false);
                }
                plant.change("ChomperDigest", assets, state.time);
            }
            return;
        }
        boolean digesting = plant.animation.equals("ChomperDigest");
        if (digesting && state.time - plant.stateStart > Layout.CHOMPER_DIGEST_TIME) {
            plant.triggered = false;
            plant.change("Chomper", assets, state.time);
        }
    }

    /**
     * 清除爆炸范围内所有僵尸。
     *
     * 参数：plant 是爆炸的植物；rowRange 是上下波及几行；xRange 是左右波及多少像素。
     */
    private void blast(Plant plant, int rowRange, int xRange) {
        for (Zombie zombie : state.zombies) {
            if (Math.abs(zombie.row - plant.row) > rowRange) {
                continue;
            }
            if (Math.abs(zombie.x - plant.x) > xRange) {
                continue;
            }
            ZombieEffects.die(zombie, assets, state, state.time, true);
        }
    }
}
