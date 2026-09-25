package pvz.game;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;
import pvz.level.Level;
import pvz.level.LevelLoader;
import pvz.plant.Card;
import pvz.plant.Cards;
import pvz.plant.Plant;
import pvz.plant.PlantActions;
import pvz.plant.PlantRules;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.Car;
import pvz.world.Layout;
import pvz.world.Sprite;
import pvz.world.Sun;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieSpawn;

/**
 * 游戏总控：处理鼠标输入，并按每一帧推进关卡里的所有东西。
 *
 * 这个类负责"游戏怎么运行"，不负责"画面长什么样"（那是 GameRenderer 的事），
 * 也不负责"数据存在哪"（那是 GameState 的事）。
 */
public class Game extends JPanel {
    /** 画面每秒重绘多少次对应的毫秒间隔，约 60 帧。 */
    private static final int FRAME_DELAY = 16;

    /** 提供图片素材。 */
    private final Assets assets;

    /** 这一局的所有数据。 */
    private final GameState state = new GameState();

    /** 负责把 state 画出来。 */
    private final GameRenderer renderer;

    /** 随机数，用于挑选天空阳光的落点和传送带的卡片。 */
    private final Random random = new Random();

    /** 负责把关卡 JSON 读成 Level 对象。 */
    private final LevelLoader levelLoader;

    /** 负责场上每一株植物每一帧的行为。 */
    private final PlantActions plantActions;

    /** 是否处于测试模式（测试时跳过选卡动画，立即更新状态）。 */
    private final boolean testMode;

    /**
     * 每 16 毫秒推进一帧的计时器。
     *
     * 它必须留成字段：Swing 的计时器不跟着窗口一起收摊，窗口关了它还会照常跑，
     * 只有手里攥着它才停得下来。试玩窗口关闭时就靠 stop 把这一局真正结束掉。
     */
    private final Timer timer;

    /**
     * 创建游戏画面，并自动开始计时刷新。
     *
     * 参数：originalAssets 是资源对象；firstLevel 是从第几关开始。
     */
    public Game(Assets originalAssets, int firstLevel) {
        this(originalAssets, firstLevel, true);
    }

    /**
     * 创建游戏画面。
     *
     * 参数：originalAssets 是资源对象；firstLevel 是从第几关开始；
     *       runTimer 表示要不要启动计时器。测试时传假，就能一帧一帧手动推进，
     *       不用真的等时间。
     */
    public Game(Assets originalAssets, int firstLevel, boolean runTimer) {
        assets = originalAssets;
        renderer = new GameRenderer(originalAssets);
        levelLoader = new LevelLoader(originalAssets);
        plantActions = new PlantActions(originalAssets, state);
        // 不启动计时器就说明是测试在手动推帧，这时跳过选卡的飞行动画更省事。
        testMode = !runTimer;
        state.levelNumber = firstLevel;
        // 游戏时钟和真实时钟从同一个点起步，之后游戏时钟按倍率累加。
        state.time = System.currentTimeMillis();
        state.lastRealTime = state.time;

        setPreferredSize(new Dimension(Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT));
        setFocusable(true);

        timer = new Timer(FRAME_DELAY, new ActionListener() {
            /** 每隔 16 毫秒推进一帧。 */
            public void actionPerformed(ActionEvent event) {
                tick();
            }
        });
        if (runTimer) {
            timer.start();
        }

        addMouseListener(new MouseAdapter() {
            /** 左键点击按当前画面处理，右键取消手里拿着的卡片。 */
            public void mousePressed(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON3) {
                    state.held = null;
                } else if (event.getButton() == MouseEvent.BUTTON1) {
                    click(event.getX(), event.getY());
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            /** 记住鼠标位置，用来显示种植预览。 */
            public void mouseMoved(MouseEvent event) {
                state.mouseX = event.getX();
                state.mouseY = event.getY();
            }

            /** 拖动时也更新预览位置。 */
            public void mouseDragged(MouseEvent event) {
                state.mouseX = event.getX();
                state.mouseY = event.getY();
            }
        });
    }

    /**
     * 推进一帧：先把游戏时钟往前拨，再按画面处理逻辑，最后请求重绘。
     *
     * 游戏时钟不是直接读墙上时间，而是"这一帧真实过了多久 × 加速倍率"累加上去。
     * 这样开加速时逻辑和动画一起变快，而画面刷新率保持不变，不会卡顿。
     */
    private void tick() {
        advanceClock();

        if (state.screen == GameScreen.MENU && state.startingMenu) {
            if (state.time - state.screenStart > Layout.MENU_START_DELAY) {
                state.startingMenu = false;
                loadLevel();
            }
        }
        if (state.screen == GameScreen.CHOOSE) {
            updateFlyingCards();
        }
        if (state.screen == GameScreen.PLAY) {
            updateLevel();
        }
        if (state.screen == GameScreen.VICTORY) {
            if (state.time - state.screenStart > Layout.ENDING_SCREEN_DURATION) {
                goToNextLevelOrMenu();
            }
        }
        if (state.screen == GameScreen.LOSS) {
            if (state.time - state.screenStart > Layout.ENDING_SCREEN_DURATION) {
                state.screen = GameScreen.MENU;
            }
        }
        repaint();
    }

    /**
     * 把游戏时钟往前拨一帧。
     *
     * 真实间隔要设个上限：窗口被拖动或最小化时计时器可能卡住很久，
     * 恢复后如果一次补上几秒，僵尸会瞬间窜过整个草坪。
     */
    private void advanceClock() {
        long realNow = System.currentTimeMillis();
        long realElapsed = realNow - state.lastRealTime;
        state.lastRealTime = realNow;

        if (realElapsed < 0) {
            realElapsed = 0;
        }
        if (realElapsed > Layout.MAX_FRAME_ELAPSED) {
            realElapsed = Layout.MAX_FRAME_ELAPSED;
        }
        state.time = state.time + realElapsed * state.speedMultiplier;
    }

    /** 通关时间到了：有下一关就接着打，没有了就回主菜单。 */
    private void goToNextLevelOrMenu() {
        if (Files.exists(assets.levelPath(state.levelNumber))) {
            loadLevel();
        } else {
            state.screen = GameScreen.MENU;
        }
    }

    /**
     * 更新选卡界面的飞行动画。
     *
     * 动画完成时才真正改动 selected 列表，这样卡片不会"人还没到、位置先占"。
     * 渲染时卡槽会跳过飞行中的卡片，避免出现一实一虚两张卡；候选区则保留原位并显示为灰色锁定。
     */
    private void updateFlyingCards() {
        for (int position = 0; position < state.flyingCards.size(); position++) {
            Card card = state.flyingCards.get(position);
            boolean wasFlying = card.flying;
            card.updateFly(state.time);

            // 只在"刚刚落地"的那一帧更新一次。
            if (!wasFlying || card.flying) {
                continue;
            }
            if (card.flyingToBar) {
                state.selected.add(Integer.valueOf(card.index));
            } else {
                state.selected.remove(Integer.valueOf(card.index));
            }
        }
        // 把已经落地的卡从飞行列表里删掉。从后往前删，删除后前面的下标不会错位。
        for (int position = state.flyingCards.size() - 1; position >= 0; position--) {
            Card card = state.flyingCards.get(position);
            if (!card.flying) {
                state.flyingCards.remove(position);
            }
        }
    }

    /** 读取当前关卡，把场地清空并重新摆好卡片和僵尸出场表。 */
    public void loadLevel() {
        Level level = levelLoader.load(state.levelNumber);

        state.backgroundIndex = level.backgroundIndex;
        state.background = assets.frame("Background", level.backgroundIndex);
        state.barType = level.barType;
        state.sunValue = level.initialSun;
        state.skySunInterval = level.skySunInterval;

        state.clearField();
        state.schedule.addAll(level.spawns);
        state.pool.addAll(level.cardPool);

        // 每行开头的推车先摆好；保龄球模式最右边三列被占掉，球从那里滚出来。
        for (int row = 0; row < Layout.ROW_COUNT; row++) {
            state.cars.add(new Car(row));
            for (int column = 0; column < Layout.COLUMN_COUNT; column++) {
                boolean reserved = state.barType == GameState.BAR_BOWLING && column >= 3;
                state.occupied[row][column] = reserved;
            }
        }

        if (level.isNormalMode()) {
            state.screen = GameScreen.CHOOSE;
        } else {
            startPlay();
        }
    }

    /** 真正开始打关卡：正常模式先按选好的卡摆出卡槽，然后切到游戏画面。 */
    private void startPlay() {
        state.cards.clear();
        if (state.barType == GameState.BAR_NORMAL) {
            state.cards.addAll(Cards.staticBar(state.selected));
        }
        state.screen = GameScreen.PLAY;
        state.playStart = state.time;
        state.lastSkySun = state.time;
        // 让第一张传送带卡片立即就能出，所以故意往前挪一点。
        state.lastCard = state.time - Layout.CONVEYOR_CARD_INTERVAL - 1;
    }

    /**
     * 处理一次左键点击。
     *
     * 参数：x 和 y 是点击位置。
     * 说明：设成公开的，是因为自检在别的包里，也要用它模拟点击。
     */
    public void click(int x, int y) {
        if (state.screen == GameScreen.MENU) {
            clickMenu(x, y);
            return;
        }
        if (state.screen == GameScreen.CHOOSE) {
            clickCardChooser(x, y);
            return;
        }
        if (state.screen == GameScreen.PLAY) {
            clickPlay(x, y);
        }
    }

    /** 判断有没有点中"冒险模式"按钮。 */
    private void clickMenu(int x, int y) {
        Rectangle option = new Rectangle(435, 75, 280, 131);
        if (option.contains(x, y)) {
            state.startingMenu = true;
            state.screenStart = state.time;
        }
    }

    /**
     * 推进一帧到指定的游戏时间，不依赖真实时钟。
     *
     * 参数：elapsed 是距离本关开始的毫秒数。
     * 说明：只给自检用，游戏本身靠计时器推进，不会调用它。
     */
    public void step(long elapsed) {
        state.time = state.playStart + elapsed;
        if (state.screen == GameScreen.PLAY) {
            updateLevel();
        }
    }

    /**
     * 处理选卡界面的点击：可以取消已选的卡、选中新卡，选满八张后按开始。
     */
    private void clickCardChooser(int x, int y) {
        if (state.selected.size() == 8) {
            BufferedImage button = assets.image("StartButton");
            Rectangle start = new Rectangle(155, 547, button.getWidth(), button.getHeight());
            if (start.contains(x, y)) {
                startPlay();
                return;
            }
        }

        // 先看是不是点在已经选中的卡上，是的话就取消选择。
        for (int position = 0; position < state.selected.size(); position++) {
            int plantIndex = state.selected.get(position).intValue();
            int left = 78 + position * Layout.CARD_BAR_SPACING;
            Card card = new Card(plantIndex, left, Layout.CARD_BAR_TOP);
            if (card.bounds(assets, Layout.CARD_SCALE).contains(x, y)) {
                if (testMode) {
                    state.selected.remove(position);
                    return;
                }
                // 这张卡已经在飞了，就别重复触发。
                if (state.isFlying(plantIndex)) {
                    return;
                }
                // 不立即从 selected 移除，等动画完成后由 updateFlyingCards 处理。
                int column = plantIndex % 8;
                int row = plantIndex / 8;
                int targetLeft = Layout.CHOOSER_LEFT + column * Layout.CHOOSER_COLUMN_SPACING;
                int targetTop = Layout.CHOOSER_TOP + row * Layout.CHOOSER_ROW_SPACING;
                card.startFly(targetLeft, targetTop, state.time, false);
                state.flyingCards.add(card);
                return;
            }
        }

        if (state.selected.size() >= 8) {
            return;
        }
        // 再看是不是点在候选卡上；已经选过的卡不能再选一次。
        for (int index = 0; index < Cards.CHOOSER_CARD_COUNT; index++) {
            int column = index % 8;
            int row = index / 8;
            int left = Layout.CHOOSER_LEFT + column * Layout.CHOOSER_COLUMN_SPACING;
            int top = Layout.CHOOSER_TOP + row * Layout.CHOOSER_ROW_SPACING;
            Card card = new Card(index, left, top);
            boolean alreadyChosen = state.selected.contains(Integer.valueOf(index));
            if (card.bounds(assets, Layout.CHOOSER_CARD_SCALE).contains(x, y) && !alreadyChosen) {
                if (testMode) {
                    // 测试模式：立即加到 selected，跳过动画。
                    state.selected.add(Integer.valueOf(index));
                    return;
                }
                // 这张卡已经在飞了，就别重复触发。
                if (state.isFlying(index)) {
                    return;
                }
                // 目标位置要空出正在飞过来的卡所占的槽位，否则两张卡会飞到同一格。
                int targetLeft = 78 + state.nextBarSlot() * Layout.CARD_BAR_SPACING;
                card.startFly(targetLeft, Layout.CARD_BAR_TOP, state.time, true);
                state.flyingCards.add(card);
                return;
            }
        }
    }

    /** 处理游戏中的点击：先看加速按钮，再收阳光，再选卡，手里有卡就是种植。 */
    private void clickPlay(int x, int y) {
        if (clickSpeedButton(x, y)) {
            return;
        }
        if (state.held == null) {
            if (collectSun(x, y)) {
                return;
            }
            selectCard(x, y);
            return;
        }
        plantHeldCard(x, y);
    }

    /**
     * 判断有没有点中右上角的加速按钮。
     *
     * 返回：点中了就返回真，表示这次点击已经被按钮用掉了。
     */
    private boolean clickSpeedButton(int x, int y) {
        Rectangle button = new Rectangle(Layout.SPEED_BUTTON_LEFT, Layout.SPEED_BUTTON_TOP,
            Layout.SPEED_BUTTON_WIDTH, Layout.SPEED_BUTTON_HEIGHT);
        if (!button.contains(x, y)) {
            return false;
        }
        state.speedMultiplier = nextSpeedMultiplier(state.speedMultiplier);
        return true;
    }

    /** 在可选倍率里找出当前倍率的下一个；已经是最后一个就绕回第一个。 */
    private int nextSpeedMultiplier(int current) {
        for (int index = 0; index < Layout.SPEED_CHOICES.length - 1; index++) {
            if (Layout.SPEED_CHOICES[index] == current) {
                return Layout.SPEED_CHOICES[index + 1];
            }
        }
        return Layout.SPEED_CHOICES[0];
    }

    /**
     * 试着点掉一颗阳光。
     *
     * 返回：收走了就返回真；否则返回假，让调用方继续判断是不是点卡。
     */
    private boolean collectSun(int x, int y) {
        for (Sun sun : state.suns) {
            if (!sun.alive) {
                continue;
            }
            Rectangle bounds = sun.bounds(assets, state.time);
            if (bounds.contains(x, y)) {
                sun.startCollect(assets, state.time);
                return true;
            }
        }
        return false;
    }

    /** 点卡槽：阳光够并且不在冷却中，才能把卡拿在手里。 */
    private void selectCard(int x, int y) {
        for (Card card : state.cards) {
            Rectangle bounds = card.bounds(assets, Layout.CARD_SCALE);
            if (!bounds.contains(x, y)) {
                continue;
            }
            // 传送带和保龄球模式不花阳光、也没有冷却，直接就能拿。
            if (state.barType != GameState.BAR_NORMAL) {
                state.held = card;
                return;
            }
            boolean enoughSun = state.sunValue >= Cards.COST[card.index];
            boolean cooledDown = state.time - card.lastUsed > Cards.COOLDOWN[card.index];
            if (enoughSun && cooledDown) {
                state.held = card;
            }
            return;
        }
    }

    /** 手里拿着卡时点击草坪：能种就种下去，点在卡槽区域则取消。 */
    private void plantHeldCard(int x, int y) {
        // 点回卡槽那一条就当作取消。
        if (y < Layout.PLAY_CARD_BAR_BOTTOM) {
            state.held = null;
            return;
        }

        int column = Layout.columnAt(x);
        int row = Layout.rowAt(y);
        if (x < Layout.GRID_LEFT || y < Layout.LAWN_TOP) {
            return;
        }
        if (!Layout.insideGrid(row, column)) {
            return;
        }
        if (state.occupied[row][column]) {
            return;
        }

        String name = Cards.PLANTS[state.held.index];
        int center = Layout.columnCenter(column);
        int bottom = Layout.rowBottom(row);
        boolean day = state.backgroundIndex == 0;
        Plant plant = new Plant(name, center, bottom, row, column, assets, state.time, day);
        state.plants.add(plant);

        // 保龄球模式不占格子，因为球是要滚走的。
        if (state.barType != GameState.BAR_BOWLING) {
            state.occupied[row][column] = true;
        }

        if (state.barType == GameState.BAR_NORMAL) {
            state.sunValue = state.sunValue - Cards.COST[state.held.index];
            state.held.lastUsed = state.time;
        } else {
            // 传送带上的卡用掉一张就少一张。
            state.cards.remove(state.held);
        }
        state.held = null;
    }

    /** 推进关卡一帧：出僵尸、出卡、掉阳光，然后更新所有物体。 */
    private void updateLevel() {
        spawnDueZombies();
        refillConveyor();
        slideConveyorCards();
        dropSkySun();

        plantActions.updateAll();
        updateZombies();
        updateBullets();
        updateSunsAndCars();
        updateHeads();

        checkVictory();
    }

    /** 按出场表放出到时间的僵尸，每次最多放一只。 */
    private void spawnDueZombies() {
        if (state.nextZombie >= state.schedule.size()) {
            return;
        }
        ZombieSpawn spawn = state.schedule.get(state.nextZombie);
        if (state.time - state.playStart < spawn.at) {
            return;
        }
        int bottom = 160 + spawn.row * Layout.CELL_HEIGHT;
        state.zombies.add(new Zombie(spawn.name, spawn.row, bottom, assets));
        state.nextZombie = state.nextZombie + 1;
    }

    /** 传送带和保龄球模式每六秒补一张卡，位置够放才补。 */
    private void refillConveyor() {
        if (state.barType == GameState.BAR_NORMAL || state.pool.isEmpty()) {
            return;
        }
        if (state.time - state.lastCard <= Layout.CONVEYOR_CARD_INTERVAL) {
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
        state.lastCard = state.time;
    }

    /** 传送带上的卡片慢慢往左挪，挪到自己的位置上。 */
    private void slideConveyorCards() {
        if (state.barType == GameState.BAR_NORMAL) {
            return;
        }
        for (int index = 0; index < state.cards.size(); index++) {
            Card card = state.cards.get(index);
            int targetLeft = Layout.CONVEYOR_FIRST_CARD_X + index * Layout.CONVEYOR_CARD_SPACING;
            if (card.x > targetLeft && state.time - card.created > Layout.CONVEYOR_CARD_SHIFT_INTERVAL) {
                card.x = card.x - 1;
                card.created = state.time;
            }
        }
    }

    /** 白天的选卡关卡每隔一段时间从天上掉一颗阳光，间隔由关卡文件的 sky_sun_interval 决定。 */
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
        int bottom = 160 + row * Layout.CELL_HEIGHT;
        state.suns.add(new Sun(center, 0, center, bottom, true, assets));
        state.lastSkySun = state.time;
    }

    /** 僵尸全部出完并且场上没有活僵尸，就算过关。 */
    private void checkVictory() {
        if (state.nextZombie != state.schedule.size()) {
            return;
        }
        if (!noActiveZombies()) {
            return;
        }
        state.levelNumber = state.levelNumber + 1;
        state.screen = GameScreen.VICTORY;
        state.screenStart = state.time;
    }

    /** 判断场上还有没有会攻击玩家的僵尸；被魅惑的僵尸不算威胁。 */
    private boolean noActiveZombies() {
        for (Zombie zombie : state.zombies) {
            if (zombie.alive && !zombie.hypno) {
                return false;
            }
        }
        return true;
    }

    /** 遍历所有僵尸，处理走路、啃植物、掉帽子、掉头和死亡。 */
    private void updateZombies() {
        for (Zombie zombie : state.zombies) {
            if (!zombie.alive) {
                continue;
            }
            if (updateZombieDying(zombie)) {
                continue;
            }
            if (zombie.health <= 0) {
                zombie.die(assets, state.time, false);
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

    /**
     * 处理僵尸的死亡动画。
     *
     * 返回：真表示这只僵尸这一帧已经处理完了。
     */
    private boolean updateZombieDying(Zombie zombie) {
        if (!zombie.dying) {
            return false;
        }
        long duration = assets.count(zombie.animation) * zombie.interval;
        if (state.time - zombie.deathTime >= duration) {
            zombie.alive = false;
        }
        return true;
    }

    /** 处理僵尸掉帽子和掉头这两个血量节点。 */
    private void updateZombieDamageState(Zombie zombie) {
        if (zombie.helmet && zombie.health <= 10) {
            zombie.helmet = false;
            // 报纸僵尸掉了报纸之后会加快脚步。
            if (zombie.name.equals("NewspaperZombie")) {
                zombie.speed = 2;
            }
            zombie.change(zombie.stateAnimation(zombie.attacking), assets, state.time);
        }

        if (zombie.headLost || zombie.health > 5) {
            return;
        }
        zombie.headLost = true;
        // 头刚被打掉，从这一刻开始算流血的时间。
        zombie.lastBleed = state.time;
        Rectangle old = zombie.bounds(assets, state.time);
        int center = (int) old.getCenterX();
        int bottom = (int) old.getMaxY();
        Sprite head = new Sprite("ZombieHead", center, bottom, zombie.row, 0, assets);
        head.animationStart = state.time;
        state.heads.add(head);
        zombie.change(zombie.stateAnimation(zombie.attacking), assets, state.time);
    }

    /**
     * 掉了头的僵尸会持续流血，直到血流干自己倒下。
     *
     * 被冻住时也照样流血，否则用寒冰菇冻住反而是帮僵尸回血。
     */
    private void updateZombieBleeding(Zombie zombie) {
        if (!zombie.headLost) {
            return;
        }
        if (state.time - zombie.lastBleed <= Layout.ZOMBIE_BLEED_INTERVAL) {
            return;
        }
        zombie.health = zombie.health - 1;
        zombie.lastBleed = state.time;
    }

    /** 僵尸这一帧的主动行为：找到要咬的东西，然后决定是咬还是走。 */
    private void updateZombieActions(Zombie zombie) {
        Plant prey = null;
        Zombie opponent = null;
        if (zombie.hypno) {
            opponent = findZombieOpponent(zombie);
        } else {
            prey = findPrey(zombie);
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

    /** 被魅惑的僵尸找同一行里的敌方僵尸下手。 */
    private Zombie findZombieOpponent(Zombie zombie) {
        for (Zombie other : state.zombies) {
            if (other == zombie || !other.alive || other.dying || other.hypno) {
                continue;
            }
            if (other.row != zombie.row) {
                continue;
            }
            if (Sprite.touches(zombie, other, assets, state.time)) {
                return other;
            }
        }
        return null;
    }

    /** 找同一行里挨着的第一株能吃的植物。 */
    private Plant findPrey(Zombie zombie) {
        for (Plant plant : state.plants) {
            if (!plant.alive || plant.health <= 0) {
                continue;
            }
            if (plant.row != zombie.row) {
                continue;
            }
            if (!PlantRules.canBeEaten(plant.name)) {
                continue;
            }
            if (Sprite.touches(zombie, plant, assets, state.time)) {
                return plant;
            }
        }
        return null;
    }

    /** 啃东西和走路的动画、速度不一样，状态切换时要换图。 */
    private void updateZombieFightingState(Zombie zombie, boolean fighting) {
        if (fighting == zombie.attacking) {
            return;
        }
        zombie.attacking = fighting;
        if (fighting) {
            zombie.interval = (int) Layout.ZOMBIE_ATTACK_ANIMATION_INTERVAL;
        } else {
            zombie.interval = (int) Layout.ZOMBIE_ANIMATION_INTERVAL;
        }
        zombie.change(zombie.stateAnimation(fighting), assets, state.time);
        zombie.lastAttack = state.time;
    }

    /** 咬一口：植物掉血；如果是魅惑菇，僵尸反被魅惑。 */
    private void bite(Zombie zombie, Plant prey, Zombie opponent) {
        // 被寒冰射手打中后，咬得也慢一半。
        long interval = Layout.ZOMBIE_ATTACK_INTERVAL;
        if (state.time < zombie.slowedUntil) {
            interval = interval * 2;
        }
        if (state.time - zombie.lastAttack <= interval) {
            return;
        }

        if (prey != null) {
            prey.health = prey.health - 1;
            boolean isHypnoShroom = prey.name.equals("HypnoShroom");
            if (prey.health <= 0 && isHypnoShroom && !prey.sleeping) {
                zombie.hypno = true;
            }
        }
        if (opponent != null) {
            opponent.health = opponent.health - 1;
        }
        zombie.lastAttack = state.time;
    }

    /** 往前走一步；被魅惑的僵尸往右走，也就是往玩家这边走。 */
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

    /** 僵尸走出屏幕：普通僵尸走出去就算玩家输了，被魅惑的走出去就消失。 */
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

    /** 子弹往前飞，撞上同一行的僵尸就扣血并爆炸。 */
    private void updateBullets() {
        for (Bullet bullet : state.bullets) {
            if (!bullet.alive) {
                continue;
            }
            bullet.update(state.time);
            if (bullet.exploded) {
                continue;
            }
            hitZombieWithBullet(bullet);
        }
    }

    /** 检查这颗子弹有没有打中僵尸。 */
    private void hitZombieWithBullet(Bullet bullet) {
        for (Zombie zombie : state.zombies) {
            if (zombie.row != bullet.row || !zombie.alive || zombie.dying || zombie.hypno) {
                continue;
            }
            if (!Sprite.touches(bullet, zombie, assets, state.time)) {
                continue;
            }
            zombie.health = zombie.health - 1;
            if (bullet.ice) {
                zombie.slowedUntil = state.time + Layout.ZOMBIE_SLOW_DURATION;
            }
            bullet.explode(assets, state.time);
            return;
        }
    }

    /** 掉下来的僵尸头播完动画就消失。 */
    private void updateHeads() {
        for (Sprite head : state.heads) {
            long duration = assets.count(head.animation) * head.interval;
            if (state.time - head.animationStart > duration) {
                head.alive = false;
            }
        }
    }

    /** 更新阳光位置，并检查小推车有没有撞到僵尸。 */
    private void updateSunsAndCars() {
        for (Sun sun : state.suns) {
            if (!sun.alive) {
                continue;
            }
            boolean wasAlive = sun.alive;
            sun.update(assets, state.time, state.speedMultiplier);
            // 收集动画飞到终点时，阳光会标记自己为死。这时加阳光值。
            if (wasAlive && !sun.alive && sun.flyingToCounter) {
                state.sunValue = state.sunValue + sun.value;
            }
        }
        updateCars();
    }

    /**
     * 僵尸走到推车跟前就把推车撞出去，推车碾过所在行所有僵尸。
     *
     * 判断碰没碰用僵尸的躯干碰撞盒，不用整个可见范围。
     * 僵尸走路时脑袋和手臂探在身体前面，用整个可见范围的话，
     * 手还离车老远车就被撞飞了，看起来就是"没碰到就跑了"。
     */
    private void updateCars() {
        BufferedImage carImage = assets.image("car");
        for (Car car : state.cars) {
            if (!car.alive) {
                continue;
            }
            car.update(state.time);
            int carTop = car.bottom - carImage.getHeight();
            Rectangle carRect = new Rectangle(car.x, carTop, carImage.getWidth(), carImage.getHeight());

            for (Zombie zombie : state.zombies) {
                if (zombie.row != car.row || !zombie.alive || zombie.dying || zombie.hypno) {
                    continue;
                }
                if (!carRect.intersects(zombie.collisionBox(assets, state.time))) {
                    continue;
                }
                car.moving = true;
                zombie.die(assets, state.time, false);
            }
        }
    }

    /**
     * 把画面画出来。
     *
     * 这里只把活儿交给 GameRenderer，自己不掺和画图的细节。
     */
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D painter = (Graphics2D) graphics.create();
        renderer.draw(painter, state.screen, state.time, state);
        painter.dispose();
    }

    /** 返回本关的卡槽模式，供无需窗口的测试核对。 */
    public int getBarType() {
        return state.barType;
    }

    /**
     * 停掉这一局，不再推进画面和逻辑。
     *
     * 关掉游戏窗口并不会自动停下计时器，这一局会在看不见的地方接着跑，
     * 白白占着处理器。所以关窗口的时候要记得叫一声 stop。
     */
    public void stop() {
        timer.stop();
    }

    /**
     * 查询这一局是不是还在跑。
     *
     * 返回：计时器还在走就返回真。
     */
    public boolean isRunning() {
        return timer.isRunning();
    }

    /** 返回已经种下的植物数量，供测试核对。 */
    public int getPlantCount() {
        return state.plants.size();
    }

    /** 返回已经出场的僵尸数量，供测试核对。 */
    public int getZombieCount() {
        return state.zombies.size();
    }

    /** 返回当前卡槽里的卡片数量，供测试核对。 */
    public int getCardCount() {
        return state.cards.size();
    }
}
