package pvz;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import pvz.game.Game;
import pvz.game.GameRenderer;
import pvz.game.GameScreen;
import pvz.game.GameState;
import pvz.plant.Plant;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.Layout;
import pvz.world.Sun;
import pvz.zombie.Zombie;

/**
 * 开发者模式绘制的专项测试。
 *
 * 它不启动窗口，也不修改关卡，只检查同一场景在两种模式下的画面差别。
 */
public class DeveloperModeTest {
    /**
     * 运行开发者模式的渲染检查。
     *
     * 参数：arguments[0] 是素材目录；省略时使用当前目录下的 assets；
     * arguments[1] 可选，用来指定调试画面的截图路径。
     */
    public static void main(String[] arguments) throws Exception {
        Path root = Path.of("assets");
        if (arguments.length > 0) {
            root = Path.of(arguments[0]);
        }
        Assets assets = new Assets(root);
        GameState state = createScene(assets);
        GameRenderer renderer = new GameRenderer(assets);

        BufferedImage normal = render(renderer, state, false);
        BufferedImage normalAgain = render(renderer, state, false);
        BufferedImage developer = render(renderer, state, true);
        check(samePixels(normal, normalAgain), "普通模式绘制结果发生变化");

        Plant plant = state.plants.get(0);
        Rectangle box = plant.collisionBox(assets, state.time);
        int borderX = box.x;
        int borderY = box.y + box.height / 2;
        check(normal.getRGB(borderX, borderY)
            != developer.getRGB(borderX, borderY), "开发者模式没有绘制植物碰撞箱");
        Zombie zombie = state.zombies.get(0);
        Rectangle zombieBox = zombie.collisionBox(assets, state.time);
        int zombieBorderX = zombieBox.x;
        int zombieBorderY = zombieBox.y + zombieBox.height / 2;
        check(normal.getRGB(zombieBorderX, zombieBorderY)
            != developer.getRGB(zombieBorderX, zombieBorderY),
            "开发者模式没有绘制僵尸碰撞箱");
        check(normal.getRGB(460, 100) != developer.getRGB(460, 100),
            "开发者模式没有绘制战斗摘要");
        checkGameConstructors(assets);
        if (arguments.length > 1) {
            Path screenshot = Path.of(arguments[1]);
            Path parent = screenshot.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(developer, "png", screenshot.toFile());
        }
        System.out.println("PASS：开发者模式碰撞箱与普通画面检查通过");
    }

    /**
     * 检查普通游戏入口默认关闭开发者模式，显式开启的试玩才显示调试层。
     *
     * 参数：assets 提供游戏图片和关卡配置。
     */
    private static void checkGameConstructors(Assets assets) {
        Game normalGame = new Game(assets, 4, false, false);
        Game developerGame = new Game(assets, 4, false, true);
        normalGame.loadLevel();
        developerGame.loadLevel();

        BufferedImage normal = renderGame(normalGame);
        BufferedImage developer = renderGame(developerGame);
        check(normal.getRGB(460, 100) != developer.getRGB(460, 100),
            "普通游戏和开发者试玩没有正确区分");
    }

    /**
     * 把一个游戏窗口画到内存图片，用于比较入口设置。
     *
     * 参数：game 是要绘制的游戏。
     * 返回：绘制好的图片。
     */
    private static BufferedImage renderGame(Game game) {
        BufferedImage image = new BufferedImage(Layout.WINDOW_WIDTH,
            Layout.WINDOW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        game.setSize(Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT);
        Graphics2D painter = image.createGraphics();
        game.paint(painter);
        painter.dispose();
        return image;
    }

    /**
     * 建立含植物、僵尸、子弹和阳光的固定测试场景。
     *
     * 参数：assets 提供图片。
     * 返回：可直接交给绘制器的游戏状态。
     */
    private static GameState createScene(Assets assets) {
        GameState state = new GameState();
        state.screen = GameScreen.PLAY;
        state.time = 10000;
        state.playStart = 9000;
        state.backgroundIndex = 0;
        state.background = assets.frame("Background", 0);

        Plant plant = new Plant("WallNut", Layout.columnCenter(3),
            Layout.rowBottom(2), 2, 3, assets, state.time, true);
        state.plants.add(plant);

        Zombie zombie = new Zombie("Zombie", 2, Layout.rowBottom(2), assets);
        double zombieCenter = zombie.collisionBox(assets, state.time).getCenterX();
        zombie.x = zombie.x + Layout.columnCenter(7) - zombieCenter;
        state.zombies.add(zombie);

        Bullet bullet = new Bullet("PeaNormal", 260, 260, 2, 260, assets);
        state.bullets.add(bullet);

        Sun sun = new Sun(240, 170, 240, 300, true, assets);
        state.suns.add(sun);
        return state;
    }

    /**
     * 在内存图片里绘制一帧。
     *
     * 参数：renderer 是绘制器；state 是场景；developerMode 表示是否绘制调试信息。
     * 返回：绘制好的图片。
     */
    private static BufferedImage render(GameRenderer renderer, GameState state,
            boolean developerMode) {
        BufferedImage image = new BufferedImage(Layout.WINDOW_WIDTH,
            Layout.WINDOW_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = image.createGraphics();
        renderer.draw(painter, state.screen, state.time, state, developerMode);
        painter.dispose();
        return image;
    }

    /**
     * 逐像素比较两张画面。
     *
     * 参数：first 和 second 是待比较图片。
     * 返回：所有像素一致时返回真。
     */
    private static boolean samePixels(BufferedImage first, BufferedImage second) {
        for (int row = 0; row < first.getHeight(); row++) {
            for (int column = 0; column < first.getWidth(); column++) {
                if (first.getRGB(column, row) != second.getRGB(column, row)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 检查一个测试条件。
     *
     * 参数：condition 是检查结果；message 是不成立时的说明。
     */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
