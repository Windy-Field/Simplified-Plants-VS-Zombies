package pvz.plant;

import java.awt.Rectangle;
import pvz.game.GameState;
import pvz.world.Assets;
import pvz.world.CombatValues;
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
     * 参数：originalAssets 提供素材；gameState 是当前游戏的数据。
     */
    public CloseAttackActions(Assets originalAssets, GameState gameState) {
        assets = originalAssets;
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
        chargeUpPotatoMine(plant);
        if (PlantRules.isBowling(name)) {
            rollBowling(plant);
        }

        // 窝瓜检测左中右三格的僵尸，不需要碰撞判定。
        if (name.equals("Squash") && !plant.triggered) {
            Zombie target = findSquashTarget(plant);
            if (target != null) {
                plant.change("SquashAttack", assets, state.time);
                plant.triggered = true;
                plant.stateStart = state.time;
                plant.target = target;
                // 换完动画再挪位置：change 会因为画布大小不同而调整 x、y。
                placeSquashOnTarget(plant, target);
                return;
            }
        }

        for (Zombie zombie : state.zombies) {
            if (!zombie.alive || zombie.dying || zombie.hypno || zombie.row != plant.row) {
                continue;
            }
            if (!Sprite.touches(plant, zombie, assets, state.time)) {
                continue;
            }
            if (handleCloseHit(plant, zombie)) {
                break;
            }
        }

        resolveCloseAttackTimeout(plant);
    }

    /**
     * 算出某个横坐标落在第几列。
     *
     * 用 floorDiv 而不是普通除法：草坪左边外面一点点会用普通除法被算成第 0 列，
     * floorDiv 向下取整得到 -1，才能判成越界。
     *
     * 参数：x 是要换算的横坐标。
     * 返回：格子的列号，可能在草坪范围之外。
     */
    private static int columnOf(double x) {
        return Math.floorDiv((int) x - Layout.GRID_LEFT, Layout.CELL_WIDTH);
    }

    /**
     * 算出僵尸站在第几列。
     *
     * 不能拿 zombie.x 直接换算：那是整张图的左沿，而僵尸的图画布很宽（166 像素），
     * 身体只占靠右的一截，普通僵尸的身体中心比画布左沿靠右 106 像素，超过一整格。
     * 用画布左沿算出来的列号会比僵尸实际站的位置偏左一格多，
     * 窝瓜就会在僵尸离得还远的时候就扑上去。
     *
     * 这里改用碰撞盒（躯干）的中心：躯干中心就是僵尸"站在哪儿"最贴近的位置，
     * 而且和 Sprite.touches 用的是同一个盒子，两套判定不会各说各话。
     *
     * 参数：zombie 是要换算的僵尸。
     * 返回：僵尸所在的列号，可能在草坪范围之外。
     */
    private int columnOfZombie(Zombie zombie) {
        Rectangle body = zombie.collisionBox(assets, state.time);
        return columnOf(body.getCenterX());
    }

    /**
     * 窝瓜检测左中右三格内的僵尸。
     *
     * 参数：plant 是那株窝瓜。
     * 返回：找到的第一个目标僵尸，没找到返回 null。
     */
    private Zombie findSquashTarget(Plant plant) {
        for (Zombie zombie : state.zombies) {
            if (!zombie.alive || zombie.dying || zombie.hypno || zombie.row != plant.row) {
                continue;
            }
            // 窝瓜的格子用种下去时记下的列号，不用它的横坐标反推：
            // 触发后它的 x 会被挪到目标僵尸身上，那时候再反推就不准了。
            int zombieColumn = columnOfZombie(zombie);
            // 左中右三格都在范围内。
            if (Math.abs(zombieColumn - plant.column) <= 1) {
                return zombie;
            }
        }
        return null;
    }

    /**
     * 把窝瓜挪到目标僵尸身上，让它正好砸在僵尸站着的位置。
     *
     * 不能直接把 plant.x 赋成 zombie.x：那是两张图的画布左沿。
     * 窝瓜的画布 100 像素宽、僵尸的 166 像素宽，身体在各自画布里的位置也不一样
     * （僵尸的身体只占画布靠右的一截），按画布左沿对齐会让瓜身停在僵尸左边大半格，
     * 向左砸、向右砸都偏。
     *
     * 这里改成"窝瓜身体中心对准僵尸躯干中心"：
     * 躯干中心就是 columnOfZombie 用来判断僵尸在哪一列的那个点，
     * 索敌和落点用同一个参照，砸下去才不会偏。
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
            int lane = ((int) plant.y + 35 - Layout.GRID_TOP) / Layout.CELL_HEIGHT;
            if (Layout.insideGrid(lane, 0)) {
                plant.row = lane;
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
            plant.change("SquashAttack", assets, state.time);
            plant.triggered = true;
            plant.stateStart = state.time;
            plant.target = zombie;
            // 这条分支虽然少见（僵尸躯干边缘搭上来、中心还在隔壁列时才会走到），
            // 但触发后的落点必须和 findSquashTarget 那条路一致，否则会出现
            // "扑过去却砸在僵尸旁边"的情况。
            placeSquashOnTarget(plant, zombie);
        }
        if (name.equals("Chomper") && !plant.triggered) {
            plant.change("ChomperAttack", assets, state.time);
            plant.triggered = true;
            plant.stateStart = state.time;
            plant.target = zombie;
        }
        if (name.equals("Spikeweed") && state.time - plant.lastAction > Layout.SPIKEWEED_DAMAGE_INTERVAL) {
            zombie.health = zombie.health - CombatValues.SPIKEWEED_DAMAGE;
            plant.lastAction = state.time;
        }
        if (name.equals("WallNutBowling") && state.time - plant.stateStart > Layout.BOWLING_HIT_INTERVAL) {
            zombie.health = zombie.health - CombatValues.BOWLING_DAMAGE;
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
                // 窝瓜砸下时秒杀范围内所有僵尸（左中右三列都算在攻击范围内）。
                for (Zombie zombie : state.zombies) {
                    if (!zombie.alive || zombie.dying || zombie.row != plant.row) {
                        continue;
                    }
                    // 窝瓜的格子始终用种下去时记下的列号。
                    // 触发时 plant.x 已经被挪到目标僵尸身上，拿它反推列号会算出旁边一格，
                    // 砸下去的范围就会和当初索敌的范围对不上，忽大忽小。
                    int zombieColumn = columnOfZombie(zombie);
                    // 同一格、左一格、右一格都在窝瓜攻击范围内。
                    if (Math.abs(zombieColumn - plant.column) <= 1) {
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
