package pvz;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import pvz.game.Game;
import pvz.game.GameRenderer;
import pvz.game.GameScreen;
import pvz.game.GameState;
import pvz.game.LevelSelection;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.level.LevelLoader;

/** 验证阳光数字恢复旧版样式，以及菜单、选关、返回和进入指定关卡的连接 */
public class GameInterfaceTest {
    private static final int[] SUN_VALUES = {0, 50, 100, 1000, 99999, Integer.MAX_VALUE};
    private static final double[] DISPLAY_SCALES = {1.0, 1.25, 1.5, 2.0};

    /** 参数 assets 和 project 提供资源与输出位置，验证后保存界面截图用于人工检查 */
    public static void run(Assets assets, Path project) throws Exception {
        checkSunNumbers(assets, project);
        checkLevelSelection(assets, project);
        checkScaledSelection(assets, project);
        checkDoubleClick(assets);
        checkReturnToSelection(assets, project);
        checkPaging(assets);
        System.out.println("PASS: Original sun text, level selection, keyboard and paging");
    }

    /** 验证选卡、战斗和演出都能返回选关，重新开始保持原关卡且不残留本局对象 */
    private static void checkReturnToSelection(Assets assets, Path project) throws Exception {
        int left = Layout.RETURN_SELECTION_LEFT + Layout.RETURN_SELECTION_WIDTH / 2;
        int top = Layout.RETURN_SELECTION_TOP + Layout.RETURN_SELECTION_HEIGHT / 2;
        Rectangle returnButton = new Rectangle(Layout.RETURN_SELECTION_LEFT,
            Layout.RETURN_SELECTION_TOP, Layout.RETURN_SELECTION_WIDTH,
            Layout.RETURN_SELECTION_HEIGHT);
        Rectangle speedButton = new Rectangle(Layout.SPEED_BUTTON_LEFT, Layout.SPEED_BUTTON_TOP,
            Layout.SPEED_BUTTON_WIDTH, Layout.SPEED_BUTTON_HEIGHT);
        check(!returnButton.intersects(speedButton), "返回按钮与倍速按钮重叠");
        check(returnButton.x > speedButton.x
            && returnButton.x + returnButton.width == Layout.WINDOW_WIDTH - 4,
            "返回选关按钮应位于最右上角");

        Game choosing = new Game(assets, 3, false, false);
        choosing.loadLevel();
        check(choosing.getScreen() == GameScreen.CHOOSE, "未进入选卡画面");
        choosing.click(left, top);
        check(choosing.getScreen() == GameScreen.LEVEL_SELECT, "选卡时不能返回选关");
        choosing.click(700, 552);
        check(choosing.getLevelNumber() == 3, "返回后没有默认选中原关卡");
        check(choosing.getScreen() == GameScreen.CHOOSE, "重新开始原关卡失败");

        // 传送带关卡可以直接进入战斗，推进到出怪后验证退出确实清空场地
        Game playing = new Game(assets, 4, false, false);
        playing.loadLevel();
        playing.step(5000);
        check(playing.getZombieCount() > 0, "退出测试没有生成敌人");
        playing.setSize(Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT);
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D painter = image.createGraphics();
        playing.paint(painter);
        painter.dispose();
        ImageIO.write(image, "png", project.resolve("../build/return-to-selection.png").toFile());
        playing.click(left, top);
        check(playing.getScreen() == GameScreen.LEVEL_SELECT, "战斗时不能返回选关");
        check(playing.getZombieCount() == 0 && playing.getPlantCount() == 0
            && playing.getCardCount() == 0, "返回选关后仍残留战斗对象");
        playing.step(10000);
        check(playing.getZombieCount() == 0, "选关页仍在推进旧战斗");
        playing.click(700, 552);
        check(playing.getLevelNumber() == 4 && playing.getScreen() == GameScreen.PLAY,
            "返回后没有重新开始原传送带关卡");
        check(playing.getZombieCount() == 0, "重新进入关卡时保留了旧敌人");

        Game intro = new Game(assets, 0, true, false);
        try {
            intro.loadLevel();
            check(intro.getScreen() == GameScreen.INTRO, "未进入开场演出");
            KeyEvent escape = new KeyEvent(intro, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
            for (java.awt.event.KeyListener listener : intro.getKeyListeners()) {
                listener.keyPressed(escape);
            }
            check(intro.getScreen() == GameScreen.LEVEL_SELECT, "演出期间 Esc 不能返回选关");
        } finally {
            intro.stop();
        }
    }

    /** 分别检查战斗与选卡画面，逐像素比较旧版固定字号和右对齐，防止只修改其中一处 */
    private static void checkSunNumbers(Assets assets, Path project) throws Exception {
        BufferedImage samples = new BufferedImage(360, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D samplePainter = samples.createGraphics();
        for (int screen = GameScreen.CHOOSE; screen <= GameScreen.PLAY; screen = screen + 1) {
            if (screen == GameScreen.INTRO) {
                continue;
            }
            for (int index = 0; index < SUN_VALUES.length; index = index + 1) {
                GameState state = new GameState();
                state.screen = screen;
                state.background = assets.frame("Background", 0);
                state.sunValue = SUN_VALUES[index];
                state.cameraOffset = Layout.CAMERA_LEFT_OFFSET;
                BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
                Graphics2D painter = image.createGraphics();
                GameRenderer renderer = new GameRenderer(assets);
                renderer.draw(painter, screen, 1000, state, false);
                painter.dispose();
                checkOriginalSunNumber(image, state.sunValue);
                int top = 0;
                if (screen == GameScreen.PLAY) {
                    top = 50;
                }
                samplePainter.drawImage(image, index * 60, top, index * 60 + 56, top + 17,
                    Layout.CARD_BAR_SUN_LEFT, Layout.CARD_BAR_SUN_TOP,
                    Layout.CARD_BAR_SUN_LEFT + 56, Layout.CARD_BAR_SUN_TOP + 17, null);
            }
        }
        samplePainter.dispose();
        ImageIO.write(samples, "png", project.resolve("../build/sun-numbers.png").toFile());
    }

    /** 参数 image 是渲染实图，amount 是阳光数；构造旧版绘制结果核对数字框内每个像素 */
    private static void checkOriginalSunNumber(BufferedImage image, int amount) {
        BufferedImage expected = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D painter = expected.createGraphics();
        painter.setColor(new Color(234, 233, 171));
        painter.fillRect(31, 66, 32, 17);
        painter.setColor(new Color(60, 60, 100));
        painter.setFont(new Font("SansSerif", Font.PLAIN, 19));
        String text = Integer.toString(amount);
        int width = painter.getFontMetrics().stringWidth(text);
        painter.drawString(text, 63 - width, 81);
        painter.dispose();
        for (int row = Layout.CARD_BAR_SUN_TOP;
                row < Layout.CARD_BAR_SUN_TOP + Layout.CARD_BAR_SUN_HEIGHT; row = row + 1) {
            for (int column = Layout.CARD_BAR_SUN_LEFT;
                    column < Layout.CARD_BAR_SUN_LEFT + Layout.CARD_BAR_SUN_WIDTH;
                    column = column + 1) {
                check(image.getRGB(column, row) == expected.getRGB(column, row),
                    "阳光数字与修改前样式不一致：" + amount);
            }
        }
    }

    /** 验证点击封面仅选中关卡，点击开始后才进入实际关卡，并支持返回菜单 */
    private static void checkLevelSelection(Assets assets, Path project) throws Exception {
        Game game = new Game(assets, 0, false, false);
        game.setSize(Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT);
        game.click(Layout.MENU_BUTTON_LEFT + 30, Layout.MENU_BUTTON_TOP + 30);
        check(game.getScreen() == GameScreen.LEVEL_SELECT, "冒险模式没有进入选关");
        LevelSelection selection = new LevelSelection(assets);
        selection.open(0);
        Rectangle tile = selection.tileBounds(3);
        game.click(tile.x + 50, tile.y + 50);
        check(game.getScreen() == GameScreen.LEVEL_SELECT, "浏览封面时提前启动关卡");
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D painter = image.createGraphics();
        game.paint(painter);
        painter.dispose();
        checkTitleCenter(image);
        ImageIO.write(image, "png", project.resolve("../build/level-selection.png").toFile());
        game.click(700, 552);
        check(game.getLevelNumber() == 3, "没有载入选中的第 4 关");
        check(game.getScreen() == GameScreen.CHOOSE, "选关后没有进入正常选卡");

        game = new Game(assets, 0, false, false);
        game.click(Layout.MENU_BUTTON_LEFT + 30, Layout.MENU_BUTTON_TOP + 30);
        game.click(50, 552);
        check(game.getScreen() == GameScreen.MENU, "返回按钮没有回到主菜单");
        selection.open(0);
        selection.keyPressed(KeyEvent.VK_RIGHT);
        check(selection.selectedLevel() == 1, "键盘没有选中下一关");
        check(selection.keyPressed(KeyEvent.VK_ENTER).equals("start"), "回车没有开始选中的关卡");
        check(selection.keyPressed(KeyEvent.VK_ESCAPE).equals("back"), "退出键没有返回");
    }

    /** 参数 image 是原尺寸选关图，复用带缩放的检查，避免只验证垂直方向 */
    private static void checkTitleCenter(BufferedImage image) {
        checkTitleCenter(image, 1.0);
    }

    /** 参数 image 和 scale 是实际渲染图及缩放比例，核对字形中心与石牌中心一致 */
    private static void checkTitleCenter(BufferedImage image, double scale) {
        int top = image.getHeight();
        int bottom = 0;
        int left = image.getWidth();
        int right = 0;
        for (int row = (int) (15 * scale); row < (int) (80 * scale); row = row + 1) {
            for (int column = (int) (100 * scale); column < (int) (700 * scale);
                    column = column + 1) {
                int color = image.getRGB(column, row);
                int red = (color >> 16) & 255;
                int green = (color >> 8) & 255;
                int blue = color & 255;
                if (red == 255 && green == 239 && blue == 163) {
                    top = Math.min(top, row);
                    bottom = Math.max(bottom, row + 1);
                    left = Math.min(left, column);
                    right = Math.max(right, column + 1);
                }
            }
        }
        check(bottom > top, "没有找到选关标题");
        double centerY = (top + bottom) / 2.0;
        double centerX = (left + right) / 2.0;
        check(Math.abs(centerY - 48 * scale) <= 2, "选择关卡标题未上下居中：" + scale);
        check(Math.abs(centerX - 400 * scale) <= 2, "选择关卡标题未左右居中：" + scale);
    }

    /** 使用模拟系统缩放的画笔绘制选关，防止只在百分之百缩放时看起来居中 */
    private static void checkScaledSelection(Assets assets, Path project) throws Exception {
        LevelSelection selection = new LevelSelection(assets);
        selection.open(1);
        for (double scale : DISPLAY_SCALES) {
            int width = (int) (Layout.WINDOW_WIDTH * scale);
            int height = (int) (Layout.WINDOW_HEIGHT * scale);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D painter = image.createGraphics();
            painter.scale(scale, scale);
            selection.draw(painter);
            painter.dispose();
            checkTitleCenter(image, scale);
            if (scale == 1.5) {
                Path output = project.resolve("../build/level-selection-scaled.png");
                ImageIO.write(image, "png", output.toFile());
            }
        }
    }

    /** 验证左键单击仅选择，左键双击才进入，右键和空白区不会开始关卡 */
    private static void checkDoubleClick(Assets assets) {
        Game game = new Game(assets, 0, false, false);
        game.click(Layout.MENU_BUTTON_LEFT + 30, Layout.MENU_BUTTON_TOP + 30);
        LevelSelection selection = new LevelSelection(assets);
        selection.open(0);
        Rectangle tile = selection.tileBounds(2);
        sendMousePress(game, tile.x + 50, tile.y + 50, 1, MouseEvent.BUTTON1);
        check(game.getScreen() == GameScreen.LEVEL_SELECT, "单击封面不应立即进入关卡");
        sendMousePress(game, tile.x + 50, tile.y + 50, 2, MouseEvent.BUTTON3);
        check(game.getScreen() == GameScreen.LEVEL_SELECT, "右键双击不应开始关卡");
        sendMousePress(game, tile.x + 50, tile.y + 50, 2, MouseEvent.BUTTON1);
        check(game.getLevelNumber() == 2 && game.getScreen() == GameScreen.CHOOSE,
            "双击未进入封面对应的关卡");

        Rectangle first = selection.tileBounds(0);
        Rectangle second = selection.tileBounds(1);
        selection.click(first.x + 50, first.y + 50, 1);
        String action = selection.click(second.x + 50, second.y + 50, 2);
        check(action.isEmpty(), "连续点击不同封面不应当成同一关卡双击");
        check(selection.click(400, 510, 2).isEmpty(), "双击空白处不应开始关卡");
        check(selection.click(second.x + 50, second.y + 50, 2).equals("start"),
            "双击选中封面没有请求进入关卡");
    }

    /** 参数 game 是目标界面，坐标、次数和按键组成系统鼠标事件，验证实际监听器的分派 */
    private static void sendMousePress(Game game, int horizontal, int vertical, int count, int button) {
        MouseEvent event = new MouseEvent(game, MouseEvent.MOUSE_PRESSED,
            System.currentTimeMillis(), 0, horizontal, vertical, count, false, button);
        for (MouseListener listener : game.getMouseListeners()) {
            listener.mousePressed(event);
        }
    }

    /** 临时增加下一页关卡，验证翻页边界；结束删除文件，不修改现有关卡 */
    private static void checkPaging(Assets assets) throws Exception {
        LevelLoader loader = new LevelLoader(assets);
        List<Integer> originalLevels = loader.levelNumbers();
        int count = originalLevels.size();
        int lastNumber = originalLevels.get(count - 1);
        int next = lastNumber + 2;
        Path temporary = assets.levelPath(next);
        boolean created = false;
        try {
            Files.copy(assets.levelPath(0), temporary);
            created = true;
            LevelSelection selection = new LevelSelection(assets);
            selection.open(0);
            // 先定位末页再点下一页，关卡数量变化时这项检查仍然有效
            for (int index = 0; index < count; index = index + 1) {
                selection.keyPressed(KeyEvent.VK_RIGHT);
            }
            check(selection.selectedLevel() == next, "方向键没有选中最后一关");
            selection.click(550, 552);
            check(selection.selectedLevel() == next, "最后一页仍然可以向后翻页");
            for (int index = 0; index <= count; index = index + 1) {
                selection.keyPressed(KeyEvent.VK_PAGE_UP);
            }
            if (count >= 6) {
                check(selection.selectedLevel() == 0, "上一页没有返回第一页");
                selection.click(550, 552);
                List<Integer> currentLevels = loader.levelNumbers();
                check(selection.selectedLevel() == currentLevels.get(6), "下一页没有选中第 7 个关卡");
                selection.click(240, 552);
            }
            selection.keyPressed(KeyEvent.VK_LEFT);
            check(selection.selectedLevel() == 0, "方向键越过了第一关");
        } finally {
            if (created) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    /** 参数 condition 是检查结果，失败抛出 message 以便定位界面问题 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
