package pvz.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import pvz.world.Assets;
import pvz.level.LevelLoader;
import pvz.tools.TextDrawing;

/** 管理选关页布局和绘制，关卡启动仍交给 Game，避免选关界面改变战斗数据 */
public class LevelSelection {
    private static final int COLUMNS = 3;
    private static final int ROWS = 2;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int FIRST_LEFT = 70;
    private static final int FIRST_TOP = 110;
    private static final int COLUMN_GAP = 230;
    private static final int ROW_GAP = 192;
    private static final int TILE_WIDTH = 200;
    private static final int TILE_HEIGHT = 176;
    private static final Rectangle TITLE_BOUNDS = new Rectangle(100, 20, 600, 56);
    private static final Rectangle BACK = new Rectangle(20, 534, 120, 44);
    private static final Rectangle PREVIOUS = new Rectangle(194, 534, 110, 44);
    private static final Rectangle NEXT = new Rectangle(496, 534, 110, 44);
    private static final Rectangle START = new Rectangle(660, 534, 120, 44);
    private static final Color INK = new Color(51, 36, 21);
    private static final Color TITLE = new Color(255, 239, 163);
    private static final Color SELECTED = new Color(43, 105, 35);
    private final Assets assets;
    private final List<Integer> levels = new ArrayList<Integer>();
    private final List<BufferedImage> covers = new ArrayList<BufferedImage>();
    private int selected;
    private int page;

    /** 参数 assets 用于查找关卡和原版素材，封面只在打开选关页时生成 */
    public LevelSelection(Assets assets) {
        this.assets = assets;
    }

    /** 参数 preferred 是希望选中的关卡；重新扫描文件，以便编辑器保存后立即看到新关卡 */
    public void open(int preferred) {
        levels.clear();
        covers.clear();
        selected = 0;
        LevelPreview preview = new LevelPreview(assets);
        LevelLoader loader = new LevelLoader(assets);
        List<Integer> numbers = loader.levelNumbers();
        for (int number : numbers) {
            if (number == preferred) {
                selected = levels.size();
            }
            levels.add(number);
            covers.add(preview.create(number));
        }
        page = selected / PAGE_SIZE;
    }

    /** 返回当前选中的关卡；没有关卡时返回 -1，防止启动不存在的文件 */
    public int selectedLevel() {
        if (levels.isEmpty()) {
            return -1;
        }
        return levels.get(selected);
    }

    /** 参数 position 是关卡列表下标；返回对应位置，供点击与绘制共用同一套边界 */
    public Rectangle tileBounds(int position) {
        int slot = position % PAGE_SIZE;
        int left = FIRST_LEFT + slot % COLUMNS * COLUMN_GAP;
        int top = FIRST_TOP + slot / COLUMNS * ROW_GAP;
        return new Rectangle(left, top, TILE_WIDTH, TILE_HEIGHT);
    }

    /** 参数 horizontal、vertical 是点击坐标；返回 start、back 或空字符串，由 Game 执行操作 */
    public String click(int horizontal, int vertical) {
        return click(horizontal, vertical, 1);
    }

    /** 参数 clickCount 是系统报告的连击次数，只在双击同一关卡时开始，避免误触空白或翻页按钮 */
    public String click(int horizontal, int vertical, int clickCount) {
        if (BACK.contains(horizontal, vertical)) {
            return "back";
        }
        if (START.contains(horizontal, vertical) && selectedLevel() >= 0) {
            return "start";
        }
        if (PREVIOUS.contains(horizontal, vertical)) {
            changePage(-1);
        } else if (NEXT.contains(horizontal, vertical)) {
            changePage(1);
        } else {
            int end = Math.min(levels.size(), (page + 1) * PAGE_SIZE);
            for (int index = page * PAGE_SIZE; index < end; index = index + 1) {
                if (tileBounds(index).contains(horizontal, vertical)) {
                    boolean sameLevel = selected == index;
                    selected = index;
                    if (clickCount == 2 && sameLevel) {
                        return "start";
                    }
                    break;
                }
            }
        }
        return "";
    }

    /** 参数 key 是键码；方向键选择，回车开始，退出键返回，便于不用鼠标也能选关 */
    public String keyPressed(int key) {
        if (key == KeyEvent.VK_ESCAPE) {
            return "back";
        }
        if (levels.isEmpty()) {
            return "";
        }
        if (key == KeyEvent.VK_ENTER) {
            return "start";
        }
        int movement = 0;
        if (key == KeyEvent.VK_LEFT) {
            movement = -1;
        } else if (key == KeyEvent.VK_RIGHT) {
            movement = 1;
        } else if (key == KeyEvent.VK_UP) {
            movement = -COLUMNS;
        } else if (key == KeyEvent.VK_DOWN) {
            movement = COLUMNS;
        } else if (key == KeyEvent.VK_PAGE_UP) {
            changePage(-1);
            return "";
        } else if (key == KeyEvent.VK_PAGE_DOWN) {
            changePage(1);
            return "";
        }
        selected = Math.max(0, Math.min(levels.size() - 1, selected + movement));
        page = selected / PAGE_SIZE;
        return "";
    }

    /** 参数 movement 是翻页方向；无效翻页忽略，避免空页或数组越界 */
    private void changePage(int movement) {
        int nextPage = page + movement;
        if (nextPage < 0 || nextPage * PAGE_SIZE >= levels.size()) {
            return;
        }
        page = nextPage;
        selected = page * PAGE_SIZE;
    }

    /** 参数 painter 是游戏画笔；引用原版底板和边框，封面保持战场截图比例 */
    public void draw(Graphics2D painter) {
        painter.drawImage(assets.image("LevelSelector"), 0, 0, null);
        painter.setFont(new Font("SansSerif", Font.BOLD, 28));
        painter.setColor(TITLE);
        TextDrawing.drawCentered(painter, "选择关卡", TITLE_BOUNDS);
        int end = Math.min(levels.size(), (page + 1) * PAGE_SIZE);
        for (int index = page * PAGE_SIZE; index < end; index = index + 1) {
            drawTile(painter, index);
        }
        drawButton(painter, BACK, "返回", true);
        drawButton(painter, PREVIOUS, "上一页", page > 0);
        drawButton(painter, NEXT, "下一页", end < levels.size());
        drawButton(painter, START, "开始", selectedLevel() >= 0);
        painter.setColor(INK);
        painter.setFont(new Font("SansSerif", Font.BOLD, 17));
        int pageCount = Math.max(1, (levels.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        TextDrawing.drawCentered(painter, (page + 1) + " / " + pageCount,
            new Rectangle(310, 534, 180, 44));
        if (levels.isEmpty()) {
            TextDrawing.drawCentered(painter, "暂无关卡", new Rectangle(100, 220, 600, 80));
        }
    }

    /** 参数 index 是关卡列表下标；先画截图再画原版边框，选中态增加描边和文字 */
    private void drawTile(Graphics2D painter, int index) {
        Rectangle tile = tileBounds(index);
        painter.drawImage(covers.get(index), tile.x + 14, tile.y + 8,
            tile.width - 28, 129, null);
        BufferedImage frame = assets.image("LevelPanel");
        // 只使用边框区域，略过原素材中央的占位文字
        painter.drawImage(frame, tile.x, tile.y, tile.x + tile.width, tile.y + 8,
            0, 0, frame.getWidth(), 8, null);
        painter.drawImage(frame, tile.x, tile.y + 137, tile.x + tile.width, tile.y + 149,
            0, 100, frame.getWidth(), 112, null);
        painter.drawImage(frame, tile.x, tile.y + 8, tile.x + 14, tile.y + 137,
            0, 8, 14, 100, null);
        painter.drawImage(frame, tile.x + tile.width - 14, tile.y + 8,
            tile.x + tile.width, tile.y + 137, 104, 8, 118, 100, null);
        painter.setColor(INK);
        painter.setFont(new Font("SansSerif", Font.BOLD, 18));
        String label = "第 " + (levels.get(index) + 1) + " 关";
        if (index == selected) {
            painter.setStroke(new BasicStroke(3));
            painter.setColor(SELECTED);
            painter.drawRect(tile.x - 4, tile.y - 4, tile.width + 8, 155);
            label = label + " · 已选";
        }
        TextDrawing.drawCentered(painter, label, new Rectangle(tile.x, tile.y + 150, tile.width, 26));
    }

    /** 参数 bounds、text 和 enabled 描述按钮；禁用态有灰色遮罩且不会改变布局 */
    private void drawButton(Graphics2D painter, Rectangle bounds, String text, boolean enabled) {
        painter.drawImage(assets.image("LevelButton"), bounds.x, bounds.y,
            bounds.width, bounds.height, null);
        if (!enabled) {
            painter.setColor(new Color(220, 220, 220, 170));
            painter.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        painter.setFont(new Font("SansSerif", Font.BOLD, 18));
        painter.setColor(INK);
        TextDrawing.drawCentered(painter, text, bounds);
    }
}
