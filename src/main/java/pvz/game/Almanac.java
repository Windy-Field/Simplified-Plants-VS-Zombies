package pvz.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import pvz.tools.TextDrawing;
import pvz.plant.PlantCatalog;
import pvz.plant.PlantDefinition;
import pvz.world.Assets;
import pvz.world.Layout;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieCatalog;
import pvz.zombie.ZombieDefinition;

/** 管理只读图鉴，复用 GIF 帧播放，不生成战斗对象或改变当前关卡 */
public class Almanac {
    private static final int COLUMNS = 5;
    private static final int PAGE_SIZE = 20;
    private static final int GRID_LEFT = 28;
    private static final int GRID_TOP = 137;
    private static final int CELL_WIDTH = 82;
    private static final int CELL_HEIGHT = 91;
    /** 小鬼素材会被预览区域自动放大，额外缩小以体现它比普通僵尸矮小 */
    private static final double IMP_PREVIEW_SCALE = 0.8;
    /** 只截取草坪内部，避免详情背景混入房屋、卡槽或道路 */
    private static final Rectangle LAWN_SAMPLE = new Rectangle(300, 180, 480, 360);
    private static final Rectangle PLANTS_TAB = new Rectangle(45, 82, 160, 40);
    private static final Rectangle ZOMBIES_TAB = new Rectangle(235, 82, 160, 40);
    private static final Rectangle BACK = new Rectangle(25, 536, 100, 44);
    private static final Rectangle PREVIOUS = new Rectangle(145, 536, 78, 44);
    private static final Rectangle NEXT = new Rectangle(267, 536, 78, 44);
    private static final Rectangle PAUSE = new Rectangle(355, 536, 92, 44);
    private static final Color INK = new Color(55, 35, 24);
    private static final Color LIGHT = new Color(255, 239, 163);
    private static final Color SELECTED = new Color(37, 102, 35);
    private final Assets assets;
    private boolean plants = true;
    private int selected;
    private long animationStart;
    private boolean paused;
    private long pausedAt;

    /** 参数 assets 是游戏已经加载的资源，图鉴共享 GIF 数据避免重复读取 */
    public Almanac(Assets assets) {
        this.assets = assets;
    }

    /** 参数 time 是当前游戏时刻，进入图鉴时从第一种植物重新开始播放 */
    public void open(long time) {
        plants = true;
        paused = false;
        select(0, time);
    }

    /** 返回当前类别的条目数量，直接读取资料表以保证全部已实现品种都能展示 */
    public int count() {
        if (plants) {
            return PlantCatalog.DEFINITIONS.length;
        }
        return ZombieCatalog.DEFINITIONS.length;
    }

    /** 返回当前是否为植物页，供界面流程检查而不暴露可写状态 */
    public boolean showingPlants() {
        return plants;
    }

    /** 返回当前条目下标，供选择与翻页验证 */
    public int selectedIndex() {
        return selected;
    }

    /** 返回选中条目的 GIF 动画名，保龄球也使用同品种素材 */
    public String animation() {
        if (plants) {
            String name = PlantCatalog.nameAt(selected);
            if (name.equals("WallNutBowling")) {
                return "WallNut";
            }
            return name;
        }
        return ZombieCatalog.DEFINITIONS[selected].name;
    }

    /** 参数 time 是游戏时刻，返回当前动画帧号；暂停时固定时钟，普通浏览按原游戏节奏循环 */
    public int frameIndex(long time) {
        long current = time;
        if (paused) {
            current = pausedAt;
        }
        int interval = (int) Layout.DEFAULT_ANIMATION_INTERVAL;
        if (!plants) {
            interval = Zombie.animationIntervalFor(animation());
        }
        long elapsed = Math.max(0, current - animationStart);
        int index = (int) ((elapsed / interval) % assets.count(animation()));
        return index;
    }

    /** 参数 index 是要选中的条目，time 是当前时刻；重新计时使新条目总从第一帧开始 */
    private void select(int index, long time) {
        selected = Math.max(0, Math.min(count() - 1, index));
        animationStart = time;
        pausedAt = time;
    }

    /** 参数 horizontal、vertical 是点击位置，time 用于动画计时；返回真表示请求返回主菜单 */
    public boolean click(int horizontal, int vertical, long time) {
        if (BACK.contains(horizontal, vertical)) {
            return true;
        }
        if (PLANTS_TAB.contains(horizontal, vertical) || ZOMBIES_TAB.contains(horizontal, vertical)) {
            plants = PLANTS_TAB.contains(horizontal, vertical);
            select(0, time);
        } else if (PREVIOUS.contains(horizontal, vertical)) {
            changePage(-1, time);
        } else if (NEXT.contains(horizontal, vertical)) {
            changePage(1, time);
        } else if (PAUSE.contains(horizontal, vertical)) {
            togglePause(time);
        } else {
            int first = selected / PAGE_SIZE * PAGE_SIZE;
            int end = Math.min(count(), first + PAGE_SIZE);
            for (int index = first; index < end; index = index + 1) {
                if (entryBounds(index).contains(horizontal, vertical)) {
                    select(index, time);
                    break;
                }
            }
        }
        return false;
    }

    /** 参数 key 是键码；方向键选择、翻页键翻页、Tab 切类别、空格暂停、Esc 返回 */
    public boolean keyPressed(int key, long time) {
        if (key == KeyEvent.VK_ESCAPE) {
            return true;
        }
        if (key == KeyEvent.VK_TAB) {
            plants = !plants;
            select(0, time);
        } else if (key == KeyEvent.VK_SPACE) {
            togglePause(time);
        } else if (key == KeyEvent.VK_PAGE_UP) {
            changePage(-1, time);
        } else if (key == KeyEvent.VK_PAGE_DOWN) {
            changePage(1, time);
        } else {
            int movement = 0;
            if (key == KeyEvent.VK_LEFT) {
                movement = -1;
            } else if (key == KeyEvent.VK_RIGHT) {
                movement = 1;
            } else if (key == KeyEvent.VK_UP) {
                movement = -COLUMNS;
            } else if (key == KeyEvent.VK_DOWN) {
                movement = COLUMNS;
            }
            if (movement != 0) {
                select(selected + movement, time);
            }
        }
        return false;
    }

    /** 参数 movement 是翻页方向，time 是动画时刻；越界翻页不改变条目 */
    private void changePage(int movement, long time) {
        int page = selected / PAGE_SIZE + movement;
        if (page >= 0 && page * PAGE_SIZE < count()) {
            select(page * PAGE_SIZE, time);
        }
    }

    /** 参数 time 是当前时刻；恢复时扣除暂停时间，防止动画突然跳到其他帧 */
    private void togglePause(long time) {
        if (paused) {
            animationStart = animationStart + time - pausedAt;
        } else {
            pausedAt = time;
        }
        paused = !paused;
    }

    /** 参数 index 是条目下标，返回固定格子边界供鼠标与绘图共用 */
    private Rectangle entryBounds(int index) {
        int slot = index % PAGE_SIZE;
        return new Rectangle(GRID_LEFT + slot % COLUMNS * CELL_WIDTH,
            GRID_TOP + slot / COLUMNS * CELL_HEIGHT, CELL_WIDTH - 4, CELL_HEIGHT - 5);
    }

    /** 参数 painter 和 time 是画笔与时刻，绘制两类条目和详情，GIF 随原计时器持续重绘 */
    public void draw(Graphics2D painter, long time) {
        String background = "AlmanacPlantBack";
        String card = "AlmanacPlantCard";
        String title = "植物图鉴";
        if (!plants) {
            background = "AlmanacZombieBack";
            card = "AlmanacZombieCard";
            title = "僵尸图鉴";
        }
        painter.drawImage(assets.image(background), 0, 0, null);
        painter.setFont(new Font("SansSerif", Font.BOLD, 27));
        painter.setColor(LIGHT);
        TextDrawing.drawCentered(painter, title, new Rectangle(100, 20, 600, 47));
        drawButton(painter, PLANTS_TAB, "植物", plants, true);
        drawButton(painter, ZOMBIES_TAB, "僵尸", !plants, true);
        int first = selected / PAGE_SIZE * PAGE_SIZE;
        int end = Math.min(count(), first + PAGE_SIZE);
        for (int index = first; index < end; index = index + 1) {
            drawEntry(painter, index);
        }
        painter.drawImage(assets.image(card), 460, 95, 316, 473, null);
        drawDetail(painter, time);
        drawButton(painter, BACK, "返回", false, true);
        drawButton(painter, PREVIOUS, "上一页", false, first > 0);
        drawButton(painter, NEXT, "下一页", false, end < count());
        String pauseText = "暂停动画";
        if (paused) {
            pauseText = "继续动画";
        }
        drawButton(painter, PAUSE, pauseText, paused, true);
        painter.setFont(new Font("SansSerif", Font.BOLD, 14));
        painter.setColor(INK);
        TextDrawing.drawCentered(painter, (selected / PAGE_SIZE + 1) + "/" + (count() + PAGE_SIZE - 1) / PAGE_SIZE,
            new Rectangle(223, 536, 44, 44));
    }

    /** 参数 index 是条目编号；缩略图只取 GIF 第一帧，动画集中在详情处避免所有条目同时晃动 */
    private void drawEntry(Graphics2D painter, int index) {
        Rectangle bounds = entryBounds(index);
        painter.setColor(new Color(255, 226, 178, 215));
        painter.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        String name;
        String label;
        if (plants) {
            name = PlantCatalog.nameAt(index);
            label = AlmanacDescriptions.plantLabel(index);
        } else {
            name = ZombieCatalog.DEFINITIONS[index].name;
            label = AlmanacDescriptions.zombieLabel(index);
        }
        drawAnimation(painter, name, 0,
            new Rectangle(bounds.x + 8, bounds.y + 4, bounds.width - 16, 57));
        painter.setColor(INK);
        int size = 12;
        Font font = new Font("SansSerif", Font.PLAIN, size);
        while (painter.getFontMetrics(font).stringWidth(label) > bounds.width - 4) {
            size = size - 1;
            font = font.deriveFont((float) size);
        }
        painter.setFont(font);
        TextDrawing.drawCentered(painter, label, new Rectangle(bounds.x, bounds.y + 62, bounds.width, 20));
        if (index == selected) {
            painter.setColor(SELECTED);
            painter.setStroke(new BasicStroke(3));
            painter.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    /** 参数 time 是动画时刻，植物与僵尸采用各自原版卡片的留白位置，确保图像和说明互不覆盖 */
    private void drawDetail(Graphics2D painter, long time) {
        Rectangle area = new Rectangle(522, 117, 190, 145);
        String label;
        String description;
        int textTop = 330;
        if (plants) {
            PlantDefinition plant = PlantCatalog.definitionAt(selected);
            label = AlmanacDescriptions.plantLabel(selected);
            description = AlmanacDescriptions.plantDescription(selected);
            drawLawn(painter, area, plant.sleepsAtDay);
            painter.setColor(INK);
            painter.setFont(new Font("SansSerif", Font.PLAIN, 15));
            painter.drawString("阳光 " + plant.cost + "     血量 " + plant.maxHealth, 488, textTop + 15);
            painter.drawString("冷却 " + plant.cooldown / 1000.0 + " 秒", 488, textTop + 39);
            if (plant.bowling) {
                painter.drawString("专用：坚果保龄球关卡", 488, textTop + 63);
            } else if (plant.sleepsAtDay) {
                painter.drawString("特性：白天睡觉", 488, textTop + 63);
            }
        } else {
            area = new Rectangle(497, 142, 244, 190);
            ZombieDefinition zombie = ZombieCatalog.DEFINITIONS[selected];
            label = AlmanacDescriptions.zombieLabel(selected);
            description = AlmanacDescriptions.zombieDescription(selected);
            textTop = 403;
            drawLawn(painter, area, false);
            painter.setColor(INK);
            painter.setFont(new Font("SansSerif", Font.PLAIN, 15));
            painter.drawString("血量 " + zombie.maxHealth, 486, textTop + 15);
            painter.drawString("移动：每步 " + zombie.speed + " 像素", 486, textTop + 39);
        }
        drawAnimation(painter, animation(), frameIndex(time), area);
        painter.setFont(new Font("SansSerif", Font.BOLD, 23));
        painter.setColor(LIGHT);
        Rectangle titleArea = new Rectangle(475, 276, 284, 36);
        if (!plants) {
            titleArea.y = 350;
        }
        TextDrawing.drawCentered(painter, label, titleArea);
        painter.setColor(INK);
        painter.setFont(new Font("SansSerif", Font.PLAIN, 15));
        int descriptionTop = textTop + 92;
        if (!plants) {
            descriptionTop = textTop + 68;
        }
        wrapText(painter, description, new Rectangle(486, descriptionTop - 15,
            262, 540 - descriptionTop));
    }

    /** 参数 area 是动画窗口，night 表示夜间植物；共用游戏草坪素材，不重复保存背景图片 */
    private void drawLawn(Graphics2D painter, Rectangle area, boolean night) {
        int background = 0;
        if (night) {
            background = 1;
        }
        BufferedImage lawn = assets.frame("Background", background);
        painter.drawImage(lawn, area.x, area.y, area.x + area.width, area.y + area.height,
            LAWN_SAMPLE.x, LAWN_SAMPLE.y, LAWN_SAMPLE.x + LAWN_SAMPLE.width,
            LAWN_SAMPLE.y + LAWN_SAMPLE.height, null);
    }

    /** 参数 name、frame 和 area 描述 GIF 与显示区域；按整段可见边界缩放，切帧时不会尺寸跳动 */
    private void drawAnimation(Graphics2D painter, String name, int frame, Rectangle area) {
        BufferedImage image = assets.frame(name, frame);
        int[] body = assets.animationBounds(name);
        int sourceWidth = body[2] - body[0];
        int sourceHeight = body[3] - body[1];
        double scale = Math.min((area.width - 12.0) / sourceWidth,
            (area.height - 12.0) / sourceHeight);
        if (name.equals("Imp")) {
            scale = scale * IMP_PREVIEW_SCALE;
        }
        int width = Math.max(1, (int) (sourceWidth * scale));
        int height = Math.max(1, (int) (sourceHeight * scale));
        int left = area.x + (area.width - width) / 2;
        int top = area.y + (area.height - height) / 2;
        painter.drawImage(image, left, top, left + width, top + height,
            body[0], body[1], body[2], body[3], null);
    }

    /** 参数 text 与 area 指定说明区域，长介绍缩小字号以完整显示，不让文字越出原版卡片 */
    private void wrapText(Graphics2D painter, String text, Rectangle area) {
        int size = 15;
        Font font = new Font("SansSerif", Font.PLAIN, size);
        painter.setFont(font);
        List<String> lines = textLines(painter, text, area.width);
        FontMetrics metrics = painter.getFontMetrics();
        while (lines.size() * metrics.getHeight() > area.height && size > 10) {
            size = size - 1;
            painter.setFont(font.deriveFont((float) size));
            metrics = painter.getFontMetrics();
            lines = textLines(painter, text, area.width);
        }
        int baseline = area.y + metrics.getAscent();
        for (String line : lines) {
            painter.drawString(line, area.x, baseline);
            baseline = baseline + metrics.getHeight();
        }
    }

    /** 参数 text 是说明，width 是可用宽度；返回逐字分行结果，中文无需依赖空格 */
    private List<String> textLines(Graphics2D painter, String text, int width) {
        List<String> lines = new ArrayList<String>();
        FontMetrics metrics = painter.getFontMetrics();
        String line = "";
        for (int index = 0; index < text.length(); index = index + 1) {
            String next = line + text.charAt(index);
            if (!line.isEmpty() && metrics.stringWidth(next) > width) {
                lines.add(line);
                line = "";
            }
            line = line + text.charAt(index);
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    /** 参数 bounds、text、selectedTab 和 enabled 描述按钮，复用素材并显示选中边框与禁用遮罩 */
    private void drawButton(Graphics2D painter, Rectangle bounds, String text,
            boolean selectedTab, boolean enabled) {
        painter.drawImage(assets.image("LevelButton"), bounds.x, bounds.y,
            bounds.width, bounds.height, null);
        if (!enabled) {
            painter.setColor(new Color(220, 220, 220, 170));
            painter.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        if (selectedTab) {
            painter.setColor(SELECTED);
            painter.setStroke(new BasicStroke(3));
            painter.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        painter.setColor(LIGHT);
        if (!enabled) {
            painter.setColor(INK);
        }
        painter.setFont(new Font("SansSerif", Font.BOLD, 16));
        TextDrawing.drawCentered(painter, text, bounds);
    }
}
