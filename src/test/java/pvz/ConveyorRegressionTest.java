package pvz;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;
import pvz.game.Game;
import pvz.game.GameRenderer;
import pvz.game.GameScreen;
import pvz.game.GameState;
import pvz.game.LevelSystem;
import pvz.plant.Card;
import pvz.plant.PlantCatalog;
import pvz.world.Assets;
import pvz.world.Layout;

/** 验证传送带和保龄球的入场时序、满队列与补位，逐步核对真实卡片边界 */
public class ConveyorRegressionTest {
    private static final long START_TIME = 10000;

    /** 参数 assets 和 root 提供素材与输出位置，检查后保留截图供人工核对 */
    public static void run(Assets assets, Path root) throws Exception {
        checkIntro(assets, root);
        checkQueue(assets, root, Layout.BAR_CONVEYOR);
        checkQueue(assets, root, Layout.BAR_BOWLING);
        System.out.println("PASS: Conveyor intro, capacity and spacing");
    }

    /** 直接用演出状态核对画面，并调用原演出更新入口验证倒计时后才开始出怪 */
    private static void checkIntro(Assets assets, Path root) throws Exception {
        // 读取游戏内部演出状态以检查时间边界，不为自检增加可写的公开游戏接口
        for (int mode = Layout.BAR_CONVEYOR; mode <= Layout.BAR_BOWLING; mode = mode + 1) {
            Game game = new Game(assets, 4, false, false);
            Field field = Game.class.getDeclaredField("state");
            field.setAccessible(true);
            GameState state = (GameState) field.get(game);
            state.screen = GameScreen.INTRO;
            state.barType = mode;
            state.background = assets.frame("Background", 0);
            state.introReturning = true;
            state.introReturnStart = START_TIME;
            Method update = Game.class.getDeclaredMethod("updateIntroReturn");
            update.setAccessible(true);
            state.time = START_TIME + Layout.INTRO_PAN_BACK_TIME - 1;
            update.invoke(game);
            BufferedImage before = render(assets, state);
            BufferedImage background = state.background;
            check(before.getRGB(100, 2) == background.getRGB(state.cameraOffset + 100, 2),
                "镜头停下前传送带已经出现");
            state.time = START_TIME + Layout.INTRO_PAN_BACK_TIME + Layout.INTRO_BAR_DROP_TIME / 2;
            update.invoke(game);
            check(state.cameraOffset == Layout.CAMERA_LEFT_OFFSET, "传送带下降时镜头仍未停止");
            BufferedImage halfway = render(assets, state);
            BufferedImage belt = assets.image("MoveBackground");
            int shift = (int) Math.round(-0.5 * belt.getHeight());
            check(halfway.getRGB(100, 2) == belt.getRGB(100 - Layout.CONVEYOR_LEFT, 2 - shift),
                "传送带没有从顶部向下滑入");
            long end = Layout.introCountdownDelay(mode) + Layout.INTRO_COUNTDOWN_TIME;
            state.time = START_TIME + end - 1;
            update.invoke(game);
            check(state.screen == GameScreen.INTRO && state.zombies.isEmpty(),
                "传送带或倒计时结束前提前开始战斗");
            state.time = START_TIME + end;
            update.invoke(game);
            check(state.screen == GameScreen.PLAY, "入场完成后没有开始战斗");
            if (mode == Layout.BAR_CONVEYOR) {
                ImageIO.write(halfway, "png", root.resolve("../build/conveyor-entering.png").toFile());
            }
        }
    }

    /** 参数 mode 是传送带或保龄球；连续补卡至满，再移除中间卡片，逐步检查实际矩形不重叠 */
    private static void checkQueue(Assets assets, Path root, int mode) throws Exception {
        GameState state = new GameState();
        state.barType = mode;
        state.background = assets.frame("Background", 0);
        state.screen = GameScreen.PLAY;
        // 测试卡池包含不同宽度的卡片，检查不能只依赖某一种卡片的固定宽度
        String[] plants = {"Peashooter", "SnowPea", "WallNut", "CherryBomb", "Torchwood", "TwinSunflower"};
        if (mode == Layout.BAR_BOWLING) {
            plants = new String[] {"WallNutBowling", "RedWallNutBowling"};
        }
        for (String name : plants) {
            state.pool.add(PlantCatalog.indexOf(name));
        }
        LevelSystem system = new LevelSystem(state, assets, new Random(42));
        advanceQueue(state, system, assets, 120000);
        int count = state.cards.size();
        check(count > 5, "排队测试没有补足卡片");
        advanceQueue(state, system, assets, 60000);
        check(state.cards.size() == count, "满传送带仍不断补卡");
        state.cards.remove(2);
        advanceQueue(state, system, assets, 60000);
        check(state.cards.size() >= count - 1, "使用中间卡片后队列没有正常补卡");
        if (mode == Layout.BAR_CONVEYOR) {
            ImageIO.write(render(assets, state), "png", root.resolve("../build/conveyor-queue.png").toFile());
        }
    }

    /** 参数 duration 是模拟时间，移动每一步都检查相邻卡片的真实边界以覆盖停靠和补位过程 */
    private static void advanceQueue(GameState state, LevelSystem system, Assets assets, long duration) {
        long end = state.time + duration;
        while (state.time < end) {
            state.time = state.time + Layout.FIXED_STEP_MS;
            system.updateSpawnsAndDrops();
            int right = Layout.CONVEYOR_FIRST_CARD_X - Layout.CONVEYOR_CARD_GAP;
            for (Card card : state.cards) {
                Rectangle bounds = card.bounds(assets, Layout.CARD_SCALE);
                check(bounds.x >= right + Layout.CONVEYOR_CARD_GAP, "传送带卡片移动或停靠时重叠");
                int beltRight = Layout.CONVEYOR_LEFT + assets.image("MoveBackground").getWidth();
                check(bounds.x + bounds.width <= beltRight - Layout.CONVEYOR_CARD_GAP,
                    "卡片伸出传送带右边界");
                right = bounds.x + bounds.width;
            }
        }
    }

    /** 参数 state 是演出或战斗状态，返回实际绘图器输出以检查位置和顺序 */
    private static BufferedImage render(Assets assets, GameState state) {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D painter = image.createGraphics();
        GameRenderer renderer = new GameRenderer(assets);
        renderer.draw(painter, state.screen, state.time, state, false);
        painter.dispose();
        return image;
    }

    /** 参数 condition 是验证结果，不满足时抛出 message 方便定位具体流程 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
