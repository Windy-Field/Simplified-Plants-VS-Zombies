package pvz.plant;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import pvz.game.GameState;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.Layout;
import pvz.world.Sprite;
import pvz.world.Sun;
import pvz.zombie.Zombie;

/**
 * 每一帧里，场上的植物各自该做什么。
 *
 * 向日葵产阳光、射手开火、樱桃炸弹爆炸、食人花吞僵尸……植物的行为全在这个类里。
 * 加一种植物或者改某株植物的脾气，从 dispatchPlant 看起就行，不用去翻 Game。
 *
 * 它只需要两样东西：素材（取图和帧数）和这一局的数据（场上有哪些僵尸、子弹加到哪去）。
 */
public class PlantActions {
    /** 提供图片和动画帧数。 */
    private final Assets assets;

    /** 这一局的数据：植物、僵尸、子弹、阳光都在里面。 */
    private final GameState state;

    /**
     * 创建植物行为处理器。
     *
     * 参数：originalAssets 提供图片；gameState 是这一局的数据。
     */
    public PlantActions(Assets originalAssets, GameState gameState) {
        assets = originalAssets;
        state = gameState;
    }

    /** 遍历所有植物，按品种分派给各自的处理函数。 */
    public void updateAll() {
        for (Plant plant : state.plants) {
            if (!plant.alive) {
                continue;
            }
            if (plant.health <= 0) {
                killPlant(plant);
                continue;
            }
            if (plant.sleeping) {
                continue;
            }
            dispatchPlant(plant);
        }
    }

    /**
     * 按植物品种决定这一帧要做什么。
     *
     * 加一种新植物时，如果它的行为能归到现有的某一类（产阳光、射手、一次性、近距离），
     * 就不用动这里；只有全新的行为才需要在这里添一个分支。
     *
     * 参数：plant 是要处理的植物。
     */
    private void dispatchPlant(Plant plant) {
        String name = plant.name;
        if (name.equals("SunFlower") || name.equals("SunShroom")) {
            produceSun(plant);
            return;
        }
        if (PlantRules.isShooter(name)) {
            updateShooter(plant);
            return;
        }
        if (name.equals("WallNut")) {
            updateWallNut(plant);
            return;
        }
        if (PlantRules.isInstant(name)) {
            updateInstant(plant);
            return;
        }
        if (PlantRules.isCloseAttack(name)) {
            updateCloseAttack(plant);
            return;
        }
    }

    /**
     * 坚果被咬坏之后会换成有裂纹的图，分两个阶段。
     *
     * 参数：plant 是那株坚果。
     */
    private void updateWallNut(Plant plant) {
        boolean cracked2 = plant.animation.equals("WallNut_cracked2");
        if (plant.health <= 10 && !cracked2) {
            plant.change("WallNut_cracked2", assets, state.time);
            return;
        }
        boolean fresh = plant.animation.equals("WallNut");
        if (plant.health <= 20 && fresh) {
            plant.change("WallNut_cracked1", assets, state.time);
        }
    }

    /**
     * 向日葵和阳光菇产阳光。
     *
     * 刚种下的六秒内不产，之后每隔二十二秒产一颗。
     *
     * 参数：plant 是那株植物。
     */
    private void produceSun(Plant plant) {
        long alive = state.time - plant.placed;

        // 阳光菇长够时间会变大，变大之后产的是大阳光。
        boolean grownUp = plant.animation.equals("SunShroomBig");
        if (plant.name.equals("SunShroom") && alive > Layout.SUN_SHROOM_GROW_TIME && !grownUp) {
            plant.change("SunShroomBig", assets, state.time);
        }

        if (alive < Layout.SUN_PRODUCE_FIRST_DELAY) {
            return;
        }
        boolean firstTime = plant.lastAction == 0;
        if (!firstTime && state.time - plant.lastAction < Layout.SUN_PRODUCE_INTERVAL) {
            return;
        }

        Rectangle bounds = plant.bounds(assets, state.time);
        boolean big = !plant.name.equals("SunShroom") || plant.animation.equals("SunShroomBig");
        int center = (int) bounds.getCenterX();
        int bottom = (int) bounds.getMaxY();
        int sunTargetX = bounds.x + bounds.width;
        int sunTargetY = bottom + bounds.height / 2;
        state.suns.add(new Sun(center, bottom, sunTargetX, sunTargetY, big, assets));
        plant.lastAction = state.time;
    }

    /**
     * 射手每帧检查一次：身前同一行有没有僵尸，有就开火。
     *
     * 三线射手还会照顾上下各一行；胆小菇附近有僵尸时会吓得不出手。
     *
     * 参数：plant 是那株射手。
     */
    private void updateShooter(Plant plant) {
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

    /**
     * 一次性植物：种下后先播一段动画，播完触发效果，效果演完就消失。
     *
     * 参数：plant 是那株一次性植物。
     */
    private void updateInstant(Plant plant) {
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
            for (Zombie zombie : state.zombies) {
                if (zombie.hypno) {
                    continue;
                }
                if (Math.abs(zombie.row - plant.row) <= 1 && Math.abs(zombie.x - plant.x) <= 120) {
                    zombie.die(assets, state.time, true);
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
                    zombie.die(assets, state.time, true);
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

    /**
     * 近距离植物：土豆雷、窝瓜、食人花、地刺和保龄球。
     *
     * 它们的共同点是必须等僵尸走到身上才生效，所以先处理各自的"预备动作"，
     * 再统一遍历僵尸判断有没有挨上。
     *
     * 参数：plant 是那株植物。
     */
    private void updateCloseAttack(Plant plant) {
        String name = plant.name;
        chargeUpPotatoMine(plant);
        if (PlantRules.isBowling(name)) {
            rollBowling(plant);
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
        }
        if (name.equals("Chomper") && !plant.triggered) {
            plant.change("ChomperAttack", assets, state.time);
            plant.triggered = true;
            plant.stateStart = state.time;
            plant.target = zombie;
        }
        if (name.equals("Spikeweed") && state.time - plant.lastAction > Layout.SPIKEWEED_DAMAGE_INTERVAL) {
            zombie.health = zombie.health - 1;
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
                if (plant.target != null) {
                    plant.target.die(assets, state.time, false);
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
                    plant.target.alive = false;
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
            zombie.die(assets, state.time, true);
        }
    }

    /**
     * 植物被打死了：标记死亡并把它占的格子空出来。
     *
     * 参数：plant 是死掉的植物。
     */
    private void killPlant(Plant plant) {
        plant.alive = false;
        if (state.barType == GameState.BAR_BOWLING) {
            return;
        }
        state.occupied[plant.row][plant.column] = false;
    }
}
