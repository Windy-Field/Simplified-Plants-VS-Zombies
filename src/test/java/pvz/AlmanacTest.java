package pvz;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import pvz.game.Almanac;
import pvz.game.AlmanacDescriptions;
import pvz.game.Game;
import pvz.game.GameScreen;
import pvz.plant.PlantCatalog;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.zombie.ZombieCatalog;

/** 检查图鉴入口、全部条目、GIF 实际帧变化和返回路径，避免新增界面影响选关与战斗 */
public class AlmanacTest {
    private static final long START_TIME = 1000;

    /** 参数 assets 是共用素材，root 是素材目录；保存代表性截图供人工核对 */
    public static void run(Assets assets, Path root) throws Exception {
        checkMenu(assets, root);
        Almanac almanac = new Almanac(assets);
        int explosiveNut = PlantCatalog.indexOf("RedWallNutBowling");
        check(AlmanacDescriptions.plantLabel(explosiveNut).equals("爆炸坚果保龄球"),
            "爆炸坚果保龄球的图鉴名称不正确");
        almanac.open(START_TIME);
        check(almanac.count() == PlantCatalog.DEFINITIONS.length, "图鉴缺少植物品种");
        for (int index = 0; index < almanac.count(); index = index + 1) {
            check(!AlmanacDescriptions.plantLabel(index).isBlank(), "植物缺少中文名");
            check(!AlmanacDescriptions.plantDescription(index).isBlank(), "植物缺少能力介绍");
            check(assets.hasAnimation(almanac.animation()), "植物图鉴缺少动画");
            BufferedImage plantImage = render(almanac, START_TIME);
            int background = 0;
            if (PlantCatalog.definitionAt(index).sleepsAtDay) {
                background = 1;
            }
            checkLawn(assets, plantImage, 522, 117, background);
            if (PlantCatalog.nameAt(index).equals("PuffShroom")) {
                ImageIO.write(plantImage, "png", root.resolve("../build/almanac-mushrooms.png").toFile());
            }
            if (index == almanac.count() - 1) {
                ImageIO.write(plantImage, "png", root.resolve("../build/almanac-last-plant.png").toFile());
            }
            almanac.keyPressed(KeyEvent.VK_RIGHT, START_TIME);
        }
        check(almanac.selectedIndex() == almanac.count() - 1, "植物页不能选择到最后一项");
        almanac.keyPressed(KeyEvent.VK_PAGE_UP, START_TIME);
        check(almanac.selectedIndex() == 0, "植物图鉴上一页无效");
        almanac.keyPressed(KeyEvent.VK_PAGE_DOWN, START_TIME);
        check(almanac.selectedIndex() == 20, "植物图鉴下一页无效");
        almanac.keyPressed(KeyEvent.VK_PAGE_DOWN, START_TIME);
        check(almanac.selectedIndex() == 20, "图鉴可以越界翻页");
        almanac.open(START_TIME);
        checkAnimation(almanac, root, "almanac-plants.png");
        checkPause(almanac);

        almanac.click(300, 100, START_TIME);
        check(!almanac.showingPlants(), "僵尸标签没有切换类别");
        check(almanac.count() == ZombieCatalog.DEFINITIONS.length, "图鉴缺少僵尸品种");
        for (int index = 0; index < almanac.count(); index = index + 1) {
            check(!AlmanacDescriptions.zombieLabel(index).isBlank(), "僵尸缺少中文名");
            check(!AlmanacDescriptions.zombieDescription(index).isBlank(), "僵尸缺少能力介绍");
            check(assets.count(almanac.animation()) > 1, "僵尸图鉴没有使用多帧动画");
            BufferedImage zombieImage = render(almanac, START_TIME);
            checkLawn(assets, zombieImage, 497, 142, 0);
            if (ZombieCatalog.DEFINITIONS[index].name.equals("Gargantuar")) {
                ImageIO.write(zombieImage, "png", root.resolve("../build/almanac-gargantuar.png").toFile());
            } else if (ZombieCatalog.DEFINITIONS[index].name.equals("Imp")) {
                ImageIO.write(zombieImage, "png", root.resolve("../build/almanac-imp.png").toFile());
            }
            almanac.keyPressed(KeyEvent.VK_RIGHT, START_TIME);
        }
        almanac.click(60, 175, START_TIME);
        check(almanac.selectedIndex() == 0, "点击图鉴条目没有切换");
        checkAnimation(almanac, root, "almanac-zombies.png");
        almanac.keyPressed(KeyEvent.VK_TAB, START_TIME);
        check(almanac.showingPlants(), "键盘不能切换图鉴类别");
        check(almanac.click(70, 552, START_TIME), "图鉴返回按钮无效");
        check(almanac.keyPressed(KeyEvent.VK_ESCAPE, START_TIME), "图鉴退出键无效");
        System.out.println("PASS: Almanac navigation, all entries, moving GIF frames and pause");
    }

    /** 参数 image 和坐标指定详情窗口，background 是预期草坪；检查未被 GIF 覆盖的背景像素 */
    private static void checkLawn(Assets assets, BufferedImage image, int left, int top, int background) {
        BufferedImage lawn = assets.frame("Background", background);
        int width = 190;
        int height = 145;
        if (left == 497) {
            width = 244;
            height = 190;
        }
        // 缩放取样可能偏向相邻像素，先按显示尺寸绘制预期背景再比较
        BufferedImage expected = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D painter = expected.createGraphics();
        painter.drawImage(lawn, 0, 0, width, height, 300, 180, 780, 540, null);
        painter.dispose();
        check(image.getRGB(left, top) == expected.getRGB(0, 0),
            "图鉴详情没有使用对应的白天或黑夜草坪");
    }

    /** 参数 assets 和 root 用于创建菜单和保存截图，实际经过 Game 的点击入口而非直接修改画面编号 */
    private static void checkMenu(Assets assets, Path root) throws Exception {
        Game game = new Game(assets, 0, false, false);
        game.setSize(800, 600);
        BufferedImage menu = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D painter = menu.createGraphics();
        game.paint(painter);
        painter.dispose();
        ImageIO.write(menu, "png", root.resolve("../build/menu-almanac.png").toFile());
        game.click(Layout.ALMANAC_BUTTON_LEFT + 40, Layout.ALMANAC_BUTTON_TOP + 40);
        check(game.getScreen() == GameScreen.ALMANAC, "主菜单图鉴入口无效");
        check(game.getPlantCount() == 0 && game.getZombieCount() == 0,
            "图鉴创建了战斗对象");
        KeyEvent event = new KeyEvent(game, KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
        for (KeyListener listener : game.getKeyListeners()) {
            listener.keyPressed(event);
        }
        check(game.getScreen() == GameScreen.MENU, "图鉴退出后没有回到主页");
        game.click(Layout.MENU_BUTTON_LEFT + 30, Layout.MENU_BUTTON_TOP + 30);
        check(game.getScreen() == GameScreen.LEVEL_SELECT, "图鉴返回后影响了冒险模式");
    }

    /** 参数 almanac 是图鉴，name 是截图名；比较详情动画区域的真实像素而不是只判断帧号 */
    private static void checkAnimation(Almanac almanac, Path root, String name) throws Exception {
        BufferedImage first = render(almanac, START_TIME);
        boolean changed = false;
        for (int step = 1; step <= 8; step = step + 1) {
            BufferedImage next = render(almanac, START_TIME + step * 150);
            for (int row = 110; row < 335 && !changed; row = row + 1) {
                for (int column = 490; column < 745; column = column + 1) {
                    if (first.getRGB(column, row) != next.getRGB(column, row)) {
                        changed = true;
                        break;
                    }
                }
            }
        }
        check(changed, "图鉴 GIF 详情没有实际动画变化");
        ImageIO.write(first, "png", root.resolve("../build/" + name).toFile());
    }

    /** 参数 almanac 是植物图鉴，验证暂停和继续不会把停止的时间算进动画 */
    private static void checkPause(Almanac almanac) {
        almanac.keyPressed(KeyEvent.VK_SPACE, 1300);
        int frame = almanac.frameIndex(1300);
        check(almanac.frameIndex(2300) == frame, "图鉴暂停后 GIF 仍然改变帧号");
        almanac.keyPressed(KeyEvent.VK_SPACE, 2300);
        check(almanac.frameIndex(2300) == frame, "继续动画时突然跳帧");
    }

    /** 参数 almanac 和 time 指定图鉴画面，返回完整渲染用于动画和布局验证 */
    private static BufferedImage render(Almanac almanac, long time) {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D painter = image.createGraphics();
        almanac.draw(painter, time);
        painter.dispose();
        return image;
    }

    /** 参数 condition 是检查结果，失败时报告 message 便于找到图鉴遗漏 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
