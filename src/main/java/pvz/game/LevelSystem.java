package pvz.game;

import java.util.Random;
import pvz.plant.Card;
import pvz.plant.Cards;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.world.Sun;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieSpawn;

/**
 * 负责关卡时间表相关的运行逻辑。
 *
 * 这里处理出怪、传送带、天空阳光和通关判断。
 * 植物、僵尸和子弹的具体战斗规则仍由 Game 当前的战斗流程负责。
 */
public class LevelSystem {
    /** 当前关卡的所有运行数据。 */
    private final GameState state;

    /** 提供关卡和图片资源。 */
    private final Assets assets;

    /** 用于随机行、天空阳光位置和传送带卡片。 */
    private final Random random;

    /**
     * 创建关卡系统。
     *
     * 参数：gameState 是当前游戏状态；originalAssets 是素材管理器；
     * randomGenerator 是当前游戏使用的随机数对象。
     */
    public LevelSystem(GameState gameState, Assets originalAssets,
            Random randomGenerator) {
        state = gameState;
        assets = originalAssets;
        random = randomGenerator;
    }

    /**
     * 生成当前时刻应该出现的僵尸、传送带卡片和天空阳光。
     */
    public void updateSpawnsAndDrops() {
        spawnDueZombies();
        refillConveyor();
        slideConveyorCards();
        dropSkySun();
    }

    /** 按出场表放出所有到时间的僵尸。 */
    private void spawnDueZombies() {
        while (state.nextSpawnIndex < state.schedule.size()) {
            ZombieSpawn spawn = state.schedule.get(state.nextSpawnIndex);
            if (state.time - state.playStart < spawn.spawnTime) {
                return;
            }
            int targetRow = spawn.row;
            if (spawn.row == ZombieSpawn.RANDOM_ROW) {
                targetRow = random.nextInt(Layout.ROW_COUNT);
            }
            int bottom = Layout.ZOMBIE_FOOT_BASE + targetRow * Layout.CELL_HEIGHT;
            state.zombies.add(new Zombie(spawn.name, targetRow, bottom, assets));
            state.nextSpawnIndex = state.nextSpawnIndex + 1;
        }
    }

    /** 传送带和保龄球模式每六秒补一张卡，位置够放才补。 */
    private void refillConveyor() {
        if (state.barType == GameState.BAR_NORMAL || state.pool.isEmpty()) {
            return;
        }
        if (state.time - state.lastCardTime <= Layout.CONVEYOR_CARD_INTERVAL) {
            return;
        }
        boolean roomLeft = true;
        if (!state.cards.isEmpty()) {
            Card last = state.cards.get(state.cards.size() - 1);
            if (last.x + 42 >= Layout.CONVEYOR_CARD_START_X) {
                roomLeft = false;
            }
        }
        if (!roomLeft) {
            return;
        }
        state.cards.add(Cards.newMovingCard(state.pool, random, state.time));
        state.lastCardTime = state.time;
    }

    /** 传送带上的卡片慢慢往左挪，挪到自己的位置上。 */
    private void slideConveyorCards() {
        if (state.barType == GameState.BAR_NORMAL) {
            return;
        }
        for (int index = 0; index < state.cards.size(); index++) {
            Card card = state.cards.get(index);
            int targetLeft = Layout.CONVEYOR_FIRST_CARD_X
                + index * Layout.CONVEYOR_CARD_SPACING;
            if (card.x > targetLeft
                    && state.time - card.created > Layout.CONVEYOR_CARD_SHIFT_INTERVAL) {
                card.x = card.x - 1;
                card.created = state.time;
            }
        }
    }

    /** 白天的正常选卡关卡每隔一段时间从天上掉一颗阳光。 */
    private void dropSkySun() {
        if (state.backgroundIndex != 0 || state.barType != GameState.BAR_NORMAL) {
            return;
        }
        if (state.time - state.lastSkySun <= state.skySunInterval) {
            return;
        }
        int column = random.nextInt(Layout.COLUMN_COUNT);
        int row = random.nextInt(Layout.ROW_COUNT);
        int center = Layout.SKY_SUN_COLUMN_CENTER + column * Layout.CELL_WIDTH;
        int bottom = Layout.ZOMBIE_FOOT_BASE + row * Layout.CELL_HEIGHT;
        state.suns.add(new Sun(center, 0, center, bottom, true, assets));
        state.lastSkySun = state.time;
    }

    /**
     * 判断所有僵尸是否已经处理完毕。
     *
     * 返回：没有待出场的僵尸、没有敌方僵尸、没有活动特效时返回真。
     */
    public boolean isVictoryReady() {
        if (state.nextSpawnIndex != state.schedule.size()) {
            return false;
        }
        for (Zombie zombie : state.zombies) {
            if (zombie.alive && !zombie.hypno) {
                return false;
            }
        }
        return !hasActiveEffects();
    }

    /** 判断是否还有正在播放的场上特效。 */
    private boolean hasActiveEffects() {
        for (pvz.world.Sprite effect : state.effects) {
            if (effect.alive) {
                return true;
            }
        }
        return false;
    }
}
