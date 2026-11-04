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
import pvz.plant.InstantPlantActions;
import pvz.plant.Plant;
import pvz.plant.PlantCatalog;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieEffects;

/** 验证爆炸死亡和传送带的完整时序、排队边界，防止只修正画面而遗漏实际行为 */
public class ConveyorRegressionTest {
    private static final long START_TIME = 10000;

    /** 参数 assets 和 root 提供素材与输出位置，检查后保留截图供人工核对 */
    public static void run(Assets assets, Path root) throws Exception {
        checkExplosion(assets);
        checkIntro(assets, root);
        checkQueue(assets, root, Layout.BAR_CONVEYOR);
        checkQueue(assets, root, Layout.BAR_BOWLING);
        System.out.println("PASS: Explosion skips head loss; conveyor intro, capacity and spacing");
    }

    /** 樱桃实际引爆后直接进入灰烬动画，不应生成头部，也不能重复启动死亡动画 */
    private static void checkExplosion(Assets assets) {
        String[] names = {"Zombie", "ConeheadZombie", "BucketheadZombie", "FlagZombie",
            "NewspaperZombie", "FootballZombie", "Imp"};
        for (String name : names) {
            GameState state = new GameState();
            state.time = START_TIME;
            int center = Layout.columnCenter(4);
            int bottom = Layout.rowBottom(2);
            Plant cherry = new Plant("CherryBomb", center, bottom, 2, 4, assets, state.time, false);
            Zombie zombie = new Zombie(name, 2, bottom, assets);
            Rectangle body = zombie.collisionBox(assets, state.time);
            zombie.x = zombie.x + center - body.getCenterX();
            state.zombies.add(zombie);
            state.time = state.time + assets.count(cherry.animation) * Layout.DEFAULT_ANIMATION_INTERVAL;
            InstantPlantActions actions = new InstantPlantActions(assets, state);
            actions.update(cherry);
            check(zombie.dying && zombie.deathAnimationStarted, "爆炸没有立即开始死亡动画：" + name);
            check(!zombie.waitingForDeath && !zombie.headLost && state.heads.isEmpty(),
                "樱桃炸弹仍触发掉头：" + name);
            check(zombie.animation.endsWith("BoomDie"), "爆炸没有使用灰烬图：" + name);
            long deathTime = zombie.deathTime;
            ZombieEffects.die(zombie, assets, state, state.time + 16, true);
            check(zombie.deathTime == deathTime, "重复爆炸重启了死亡动画");
            long duration = (long) assets.count(zombie.animation) * zombie.frameInterval;
            ZombieEffects.updateDeath(zombie, assets, state, state.time + duration);
            check(!zombie.alive, "爆炸动画结束后未清理僵尸：" + name);
        }
    }

    /** 直接用演出状态核对画面，并调用原演出更新入口验证倒计时后才开始出怪 */
    private static void checkIntro(Assets assets, Path root) throws Exception {
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
        // 少数植物不在传送带卡池中，使用项目已支持的不同宽度卡片测试
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
