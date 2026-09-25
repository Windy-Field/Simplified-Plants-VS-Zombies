package pvz.editor;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import pvz.world.Layout;

import static pvz.world.Layout.CELL_WIDTH;

/**
 * 编辑器中间那张出怪网格。
 *
 * 横向一列是一波，纵向一行对应草坪的一行，一个格子表示"这一波这一行"出的僵尸。
 * 格子里画着僵尸图标，右下角的红色标签是只数。
 * 鼠标左键加一只、右键减一只、滚轮快速增减，按住僵尸拖动可以换位置。
 */
public class EditorGrid extends JPanel {
    /** 左边写行号那一栏的宽度。 */
    public static final int ROW_HEADER_WIDTH = 76;

    /** 上边写波次和时刻那一栏的高度。 */
    public static final int COLUMN_HEADER_HEIGHT = 48;

    /** 每个格子的宽度。 */
    public static final int CELL_WIDTH = 80;

    /** 每个格子的高度。 */
    public static final int CELL_HEIGHT = 88;

    /** 格子里僵尸图标的高度。 */
    private static final int ICON_HEIGHT = 50;

    /** 鼠标按下后要移动超过这么多像素，才算是在拖动而不是点击。 */
    private static final int DRAG_THRESHOLD = 4;

    /** 拖动时源格子里的僵尸画得淡一些，表示它正被搬走。 */
    private static final float DRAG_SOURCE_ALPHA = 0.3f;

    /** 表头的底色。 */
    private static final Color HEADER_FILL = new Color(0xE4, 0xE9, 0xDF);

    /** 格子的底色。 */
    private static final Color CELL_FILL = new Color(0xF6, 0xFA, 0xF2);

    /** 相邻波次错开一点底色，方便一眼数清是第几波。 */
    private static final Color CELL_FILL_ALT = new Color(0xEC, 0xF4, 0xE4);

    /** 网格线的颜色。 */
    private static final Color LINE = new Color(0xC2, 0xCC, 0xB8);

    /** 表头上"第几波""第几行"的字色。 */
    private static final Color HEADER_TEXT = new Color(0x2E, 0x4A, 0x22);

    /** 表头上出场时刻那一行的字色，比标题淡一些。 */
    private static final Color HEADER_SUBTEXT = new Color(0x6B, 0x7A, 0x60);

    /** 只数标签的底色：多于一只时用醒目的红。 */
    private static final Color BADGE_MANY = new Color(0xC2, 0x43, 0x2E);

    /** 只数标签的底色：只有一只时用不抢眼的灰绿。 */
    private static final Color BADGE_ONE = new Color(0x7A, 0x87, 0x70);

    /** 拖动时落点格子的蒙色。 */
    private static final Color DROP_FILL = new Color(0x4C, 0x8C, 0xD8, 0x55);

    /** 拖动时落点格子的边框颜色。 */
    private static final Color DROP_LINE = new Color(0x2C, 0x6C, 0xB8);

    /** 选中格子的边框颜色。 */
    private static final Color SELECT_LINE = new Color(0xE0, 0x7A, 0x1A);

    /** 提供僵尸图标。 */
    private final EditorIcons icons;

    /** 拖动由主窗口统一处理，这里只负责报告。 */
    private final EditorDragController controller;

    /** 正在编辑的关卡，换关卡时会被整个替换掉。 */
    private LevelDesign design;

    /** 鼠标悬停的格子，没有就是 -1。 */
    private int hoverRow = -1;

    private int hoverWave = -1;

    /** 拖动时鼠标底下的格子，也就是松手会放到哪里，没有就是 -1。 */
    private int dropRow = -1;

    private int dropWave = -1;

    /** 拖动的内容来自哪个格子，从僵尸列表拖来的就是 -1。 */
    private int dragRow = -1;

    private int dragWave = -1;

    /** 当前选中的格子，键盘操作作用在它上面，没有就是 -1。 */
    private int selectedRow = -1;

    private int selectedWave = -1;

    /** 鼠标按下时按在哪个格子上，没有就是 -1。 */
    private int pressRow = -1;

    private int pressWave = -1;

    /** 鼠标按下时的位置，用来判断有没有移动够距离。 */
    private Point pressPoint;

    /** 这一次按下之后是否已经进入拖动状态。 */
    private boolean dragStarted;

    /**
     * 创建出怪网格。
     *
     * 参数：initialDesign 是要编辑的关卡；editorIcons 提供图标；
     *       dragController 接收点击和拖动的报告。
     */
    public EditorGrid(LevelDesign initialDesign, EditorIcons editorIcons,
            EditorDragController dragController) {
        design = initialDesign;
        icons = editorIcons;
        controller = dragController;
        setBackground(Color.WHITE);
        // 要接收 Delete 这类按键，面板必须能拿到焦点。
        setFocusable(true);

        installMouseHandlers();
        installWheelHandler();
        installKeyHandler();
    }

    /**
     * 装上鼠标处理。
     *
     * 这里只把事件转交给下面几个有名字的方法，真正做事的代码都在那些方法里。
     * 匿名的监听器写多了会连成一大片，出了问题很难找。
     */
    private void installMouseHandlers() {
        addMouseListener(new MouseAdapter() {
            /** 按下鼠标。 */
            public void mousePressed(MouseEvent event) {
                handlePress(event);
            }

            /** 松开鼠标。 */
            public void mouseReleased(MouseEvent event) {
                handleRelease(event);
            }

            /** 鼠标离开面板。 */
            public void mouseExited(MouseEvent event) {
                clearHover();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            /** 鼠标在面板里移动。 */
            public void mouseMoved(MouseEvent event) {
                updateHover(event);
            }

            /** 按住鼠标拖动。 */
            public void mouseDragged(MouseEvent event) {
                handleDrag(event);
            }
        });
    }

    /**
     * 按下鼠标：先选中这个格子，按住 Ctrl 时才动只数。
     *
     * 左右键都是选中，不会误改数量；要增减就按住 Ctrl：
     * Ctrl + 左减一只，Ctrl + 右加一只（空格子上则放下当前选中的僵尸）。
     *
     * 此刻还看不出用户是想点一下还是想拖走，所以先把按下的位置记下来，
     * 等到松手或者移动够距离时再下结论。
     *
     * 参数：event 是鼠标事件。
     */
    private void handlePress(MouseEvent event) {
        requestFocusInWindow();
        pressPoint = event.getPoint();
        dragStarted = false;
        pressRow = -1;
        pressWave = -1;

        int[] cell = cellAt(event.getPoint());
        if (cell == null) {
            return;
        }
        pressRow = cell[0];
        pressWave = cell[1];
        selectedRow = cell[0];
        selectedWave = cell[1];
        // 选中格子变了，让主窗口把随机行复选框同步过来。
        controller.selectionChanged();
        repaint();
    }

    /**
     * 松开鼠标：拖起来了就交给主窗口放下；
     * 没拖起来时，按住 Ctrl 才增减只数，否则只是一次纯粹的选中。
     *
     * 参数：event 是鼠标事件。
     */
    private void handleRelease(MouseEvent event) {
        if (dragStarted) {
            controller.finishDrag(event.getLocationOnScreen());
            pressRow = -1;
            pressWave = -1;
            dragStarted = false;
            return;
        }
        if (pressRow >= 0 && event.isControlDown()) {
            if (SwingUtilities.isLeftMouseButton(event)) {
                changeCount(pressRow, pressWave, -1);
            } else if (SwingUtilities.isRightMouseButton(event)) {
                changeCount(pressRow, pressWave, 1);
            }
        }
        pressRow = -1;
        pressWave = -1;
    }

    /**
     * 按 Ctrl 点击格子时增减只数。
     *
     * 空格子上加一只，意思是放下当前选中的僵尸；减则什么都不做。
     *
     * 参数：row 是行号；wave 是波号；delta 是增减的只数。
     */
    private void changeCount(int row, int wave, int delta) {
        if (design.kindAt(row, wave) == null) {
            if (delta > 0 && controller.selectedKind() != null) {
                design.setCell(row, wave, controller.selectedKind(), 1);
                controller.designChanged();
            }
            return;
        }
        design.addCount(row, wave, delta);
        controller.designChanged();
    }

    /**
     * 鼠标移动：记住它悬在哪个格子上，用来画高亮。
     *
     * 参数：event 是鼠标事件。
     */
    private void updateHover(MouseEvent event) {
        int[] cell = cellAt(event.getPoint());
        int row = -1;
        int wave = -1;
        if (cell != null) {
            row = cell[0];
            wave = cell[1];
        }
        if (row != hoverRow || wave != hoverWave) {
            hoverRow = row;
            hoverWave = wave;
            repaint();
        }
    }

    /** 鼠标离开面板，取消悬停高亮。 */
    private void clearHover() {
        hoverRow = -1;
        hoverWave = -1;
        repaint();
    }

    /**
     * 拖动鼠标：按住有僵尸的格子并移动够距离，就算开始把它拖走了。
     *
     * 参数：event 是鼠标事件。
     */
    private void handleDrag(MouseEvent event) {
        if (pressPoint == null || !SwingUtilities.isLeftMouseButton(event)) {
            return;
        }
        if (dragStarted) {
            controller.updateDrag(event.getLocationOnScreen());
            return;
        }
        // 空格子没什么可拖的，手抖一下也不该有反应。
        if (pressRow < 0 || design.kindAt(pressRow, pressWave) == null) {
            return;
        }
        int movedX = Math.abs(event.getX() - pressPoint.x);
        int movedY = Math.abs(event.getY() - pressPoint.y);
        if (movedX + movedY < DRAG_THRESHOLD) {
            return;
        }

        dragStarted = true;
        String kind = design.kindAt(pressRow, pressWave);
        int count = design.countAt(pressRow, pressWave);
        controller.beginDrag(kind, count, pressRow, pressWave, event.getLocationOnScreen());
    }

    /** 装上滚轮处理：停在格子上就增减只数，其他地方照常滚动画面。 */
    private void installWheelHandler() {
        addMouseWheelListener(new MouseWheelListener() {
            /** 滚轮在格子上就增减只数，空格子上往上滚会放下当前选中的僵尸。 */
            public void mouseWheelMoved(MouseWheelEvent event) {
                int[] cell = cellAt(event.getPoint());
                if (cell == null) {
                    passWheelToScrollPane(event);
                    return;
                }
                int delta = -1;
                if (event.getWheelRotation() < 0) {
                    delta = 1;
                }
                // 先记住滚到哪一格，复选框才跟得上这次改动。
                selectedRow = cell[0];
                selectedWave = cell[1];
                controller.selectionChanged();
                changeCount(cell[0], cell[1], delta);
                repaint();
            }
        });
    }

    /** 装上键盘处理：Delete 清空选中的格子，加减号和上下键调只数，R 键切换随机行。 */
    private void installKeyHandler() {
        addKeyListener(new KeyAdapter() {
            /** Delete 清空选中的格子，加减号和上下键调整只数，R 键切换随机行。 */
            public void keyPressed(KeyEvent event) {
                if (selectedRow < 0) {
                    return;
                }
                int code = event.getKeyCode();
                if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE) {
                    design.clearCell(selectedRow, selectedWave);
                    controller.designChanged();
                    event.consume();
                } else if (isIncreaseKey(code)) {
                    design.addCount(selectedRow, selectedWave, 1);
                    controller.designChanged();
                    event.consume();
                } else if (isDecreaseKey(code)) {
                    design.addCount(selectedRow, selectedWave, -1);
                    controller.designChanged();
                    event.consume();
                } else if (code == KeyEvent.VK_R) {
                    if (design.kindAt(selectedRow, selectedWave) != null) {
                        design.toggleRandomRow(selectedRow, selectedWave);
                        controller.designChanged();
                        event.consume();
                    }
                }
                repaint();
            }
        });
    }

    /**
     * 判断某个键是不是"增加一只"。
     *
     * 参数：code 是按键编号。
     * 返回：是加号或者上方向键就返回真。
     */
    private static boolean isIncreaseKey(int code) {
        if (code == KeyEvent.VK_ADD || code == KeyEvent.VK_EQUALS) {
            return true;
        }
        if (code == KeyEvent.VK_PLUS || code == KeyEvent.VK_UP) {
            return true;
        }
        return false;
    }

    /**
     * 判断某个键是不是"减少一只"。
     *
     * 参数：code 是按键编号。
     * 返回：是减号或者下方向键就返回真。
     */
    private static boolean isDecreaseKey(int code) {
        if (code == KeyEvent.VK_SUBTRACT || code == KeyEvent.VK_MINUS) {
            return true;
        }
        if (code == KeyEvent.VK_DOWN) {
            return true;
        }
        return false;
    }

    /**
     * 把滚轮事件转交给外面的滚动面板。
     *
     * 网格自己装了滚轮监听之后，事件就不会自动往上传了，
     * 所以在不需要增减只数的时候要手动转交，否则画面滚不动。
     *
     * 参数：event 是收到的滚轮事件。
     */
    private void passWheelToScrollPane(MouseWheelEvent event) {
        if (getParent() == null) {
            return;
        }
        MouseEvent converted = SwingUtilities.convertMouseEvent(this, event, getParent());
        getParent().dispatchEvent(converted);
    }

    /**
     * 查询当前选中的是哪一行。
     *
     * 返回：行号；没有选中任何格子就返回 -1。
     */
    public int selectedRow() {
        return selectedRow;
    }

    /**
     * 查询当前选中的是哪一波。
     *
     * 返回：波号；没有选中任何格子就返回 -1。
     */
    public int selectedWave() {
        return selectedWave;
    }

    /**
     * 判断某个格子里有没有僵尸。
     *
     * 参数：row 是行号；wave 是波号。
     * 返回：有僵尸就返回真；空格子或者坐标越界都返回假。
     */
    public boolean hasCell(int row, int wave) {
        return design.kindAt(row, wave) != null;
    }

    /**
     * 换一个关卡来编辑。
     *
     * 参数：newDesign 是新的关卡数据。
     */
    public void setDesign(LevelDesign newDesign) {
        design = newDesign;
        selectedRow = -1;
        selectedWave = -1;
        refresh();
    }

    /** 关卡内容变了之后重新算尺寸并重画，波数变化时靠它撑开或收窄网格。 */
    public void refresh() {
        revalidate();
        repaint();
    }

    /**
     * 把面板里的一个点换算成格子坐标。
     *
     * 参数：point 是这块面板里的坐标。
     * 返回：两个元素的数组，依次是行号和波号；点在表头或网格外面时返回 null。
     */
    public int[] cellAt(Point point) {
        if (point.x < ROW_HEADER_WIDTH || point.y < COLUMN_HEADER_HEIGHT) {
            return null;
        }
        int wave = (point.x - ROW_HEADER_WIDTH) / CELL_WIDTH;
        int row = (point.y - COLUMN_HEADER_HEIGHT) / CELL_HEIGHT;
        if (!design.insideGrid(row, wave)) {
            return null;
        }
        return new int[] {row, wave};
    }

    /**
     * 设置拖动时高亮显示的落点格子。
     *
     * 参数：row 是行号；wave 是波号；都传 -1 表示不高亮任何格子。
     */
    public void setDropTarget(int row, int wave) {
        if (row == dropRow && wave == dropWave) {
            return;
        }
        dropRow = row;
        dropWave = wave;
        repaint();
    }

    /**
     * 设置拖动的内容来自哪个格子，那个格子会画得淡一些。
     *
     * 参数：row 是行号；wave 是波号；都传 -1 表示没有源格子。
     */
    public void setDragSource(int row, int wave) {
        dragRow = row;
        dragWave = wave;
        repaint();
    }

    /**
     * 算出整张网格要占多大地方，波数越多越宽。
     *
     * 返回：网格的期望尺寸。
     */
    public Dimension getPreferredSize() {
        int width = ROW_HEADER_WIDTH + design.waveCount() * CELL_WIDTH;
        int height = COLUMN_HEADER_HEIGHT + Layout.ROW_COUNT * CELL_HEIGHT;
        return new Dimension(width, height);
    }

    /**
     * 画出整张网格：先画表头，再一个个画格子。
     *
     * 参数：graphics 是画笔。
     */
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D painter = (Graphics2D) graphics;
        painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        painter.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawHeaders(painter);
        for (int row = 0; row < Layout.ROW_COUNT; row++) {
            for (int wave = 0; wave < design.waveCount(); wave++) {
                drawCell(painter, row, wave);
            }
        }
    }

    /**
     * 画上方的波次表头和左边的行号表头，顺带把网格线也画了。
     *
     * 参数：painter 是画笔。
     */
    private void drawHeaders(Graphics2D painter) {
        int width = ROW_HEADER_WIDTH + design.waveCount() * CELL_WIDTH;
        int height = COLUMN_HEADER_HEIGHT + Layout.ROW_COUNT * CELL_HEIGHT;

        painter.setColor(HEADER_FILL);
        painter.fillRect(0, 0, width, COLUMN_HEADER_HEIGHT);
        painter.fillRect(0, 0, ROW_HEADER_WIDTH, height);
        painter.setColor(LINE);
        painter.drawLine(0, COLUMN_HEADER_HEIGHT, width, COLUMN_HEADER_HEIGHT);
        painter.drawLine(ROW_HEADER_WIDTH, 0, ROW_HEADER_WIDTH, height);

        drawWaveHeaders(painter, height);
        drawRowHeaders(painter, width);

        // 补上最右边和最下边的收口线。
        painter.setColor(LINE);
        painter.drawLine(0, height - 1, width, height - 1);
        painter.drawLine(width - 1, 0, width - 1, height);
    }

    /**
     * 画上方的波次表头：每一波写两行，上面是第几波，下面是这一波的出场时刻。
     *
     * 参数：painter 是画笔；height 是整张网格的高度，用来画竖的分隔线。
     */
    private void drawWaveHeaders(Graphics2D painter, int height) {
        for (int wave = 0; wave < design.waveCount(); wave++) {
            int left = ROW_HEADER_WIDTH + wave * CELL_WIDTH;

            painter.setColor(HEADER_TEXT);
            painter.setFont(getFont().deriveFont(Font.BOLD, 13f));
            String title = "第 " + (wave + 1) + " 波";
            painter.drawString(title, left + centerOffset(painter, title, CELL_WIDTH), 20);

            painter.setColor(HEADER_SUBTEXT);
            painter.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            String moment = LevelDesign.formatTime(design.waveTime(wave));
            painter.drawString(moment, left + centerOffset(painter, moment, CELL_WIDTH), 38);

            painter.setColor(LINE);
            painter.drawLine(left, 0, left, height);
        }
    }

    /**
     * 画左边的行号表头。
     *
     * 参数：painter 是画笔；width 是整张网格的宽度，用来画横的分隔线。
     */
    private void drawRowHeaders(Graphics2D painter, int width) {
        for (int row = 0; row < Layout.ROW_COUNT; row++) {
            int top = COLUMN_HEADER_HEIGHT + row * CELL_HEIGHT;

            painter.setColor(HEADER_TEXT);
            painter.setFont(getFont().deriveFont(Font.BOLD, 13f));
            String title = "第 " + (row + 1) + " 行";
            painter.drawString(title, centerOffset(painter, title, ROW_HEADER_WIDTH),
                top + CELL_HEIGHT / 2 + 5);

            painter.setColor(LINE);
            painter.drawLine(0, top, width, top);
        }
    }

    /**
     * 画一个格子：底色、僵尸图标、只数标签，最后再补上高亮边框。
     *
     * 参数：painter 是画笔；row 是行号；wave 是波号。
     */
    private void drawCell(Graphics2D painter, int row, int wave) {
        int left = ROW_HEADER_WIDTH + wave * CELL_WIDTH;
        int top = COLUMN_HEADER_HEIGHT + row * CELL_HEIGHT;

        if (wave % 2 == 0) {
            painter.setColor(CELL_FILL);
        } else {
            painter.setColor(CELL_FILL_ALT);
        }
        painter.fillRect(left + 1, top + 1, CELL_WIDTH - 1, CELL_HEIGHT - 1);

        // 拖动过程中满屏高亮会很花，所以只在不拖动时提示鼠标停在哪一格。
        if (row == hoverRow && wave == hoverWave && !controller.isDragging()) {
            painter.setColor(new Color(0xFF, 0xFF, 0xFF, 0x90));
            painter.fillRect(left + 1, top + 1, CELL_WIDTH - 1, CELL_HEIGHT - 1);
        }

        String kind = design.kindAt(row, wave);
        if (kind != null) {
            drawZombie(painter, kind, row, wave, left, top);
            drawCountBadge(painter, left, top, design.countAt(row, wave));
            if (design.randomRowAt(row, wave)) {
                drawRandomRowMark(painter, left, top);
            }
        }

        drawCellBorder(painter, row, wave, left, top);
    }

    /**
     * 画格子里的僵尸图标；正被拖走的那一格画得半透明。
     *
     * 参数：painter 是画笔；kind 是僵尸品种；row 是行号；wave 是波号；
     *       left、top 是格子的左上角。
     */
    private void drawZombie(Graphics2D painter, String kind, int row, int wave, int left, int top) {
        BufferedImage image = icons.icon(kind, ICON_HEIGHT);
        int imageLeft = left + (CELL_WIDTH - image.getWidth()) / 2;
        int imageTop = top + 8;

        boolean beingDragged = controller.isDragging() && row == dragRow && wave == dragWave;
        if (!beingDragged) {
            painter.drawImage(image, imageLeft, imageTop, null);
            return;
        }
        // 把画笔的混合方式临时调成半透明，画完要还原，否则后面全都变透明了。
        Composite original = painter.getComposite();
        painter.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, DRAG_SOURCE_ALPHA));
        painter.drawImage(image, imageLeft, imageTop, null);
        painter.setComposite(original);
    }

    /**
     * 在格子右下角画只数标签，多于一只时用醒目的红底。
     *
     * 参数：painter 是画笔；left、top 是格子的左上角；count 是只数。
     */
    private void drawCountBadge(Graphics2D painter, int left, int top, int count) {
        String text = "× " + count;
        if (count > 1) {
            painter.setFont(getFont().deriveFont(Font.BOLD, 14f));
        } else {
            painter.setFont(getFont().deriveFont(Font.PLAIN, 14f));
        }

        int badgeWidth = painter.getFontMetrics().stringWidth(text) + 12;
        int badgeLeft = left + CELL_WIDTH - badgeWidth - 5;
        int badgeTop = top + CELL_HEIGHT - 24;

        if (count > 1) {
            painter.setColor(BADGE_MANY);
        } else {
            painter.setColor(BADGE_ONE);
        }
        painter.fillRoundRect(badgeLeft, badgeTop, badgeWidth, 19, 9, 9);
        painter.setColor(Color.WHITE);
        painter.drawString(text, badgeLeft + 6, badgeTop + 14);
    }

    /**
     * 在格子左上角画随机行标记。
     *
     * 参数：painter 是画笔；left、top 是格子的左上角。
     */
    private void drawRandomRowMark(Graphics2D painter, int left, int top) {
        painter.setFont(getFont().deriveFont(Font.BOLD, 13f));
        String text = "随机";
        int markWidth = painter.getFontMetrics().stringWidth(text) + 10;
        int markLeft = left + 5;
        int markTop = top + 5;

        painter.setColor(new Color(255, 140, 0));
        painter.fillRoundRect(markLeft, markTop, markWidth, 18, 8, 8);
        painter.setColor(Color.WHITE);
        painter.drawString(text, markLeft + 5, markTop + 13);
    }

    /**
     * 给格子补上高亮边框：拖动落点画蓝框，选中的格子画橙框。
     *
     * 参数：painter 是画笔；row 是行号；wave 是波号；left、top 是格子的左上角。
     */
    private void drawCellBorder(Graphics2D painter, int row, int wave, int left, int top) {
        boolean isDropTarget = row == dropRow && wave == dropWave;
        boolean isSelected = row == selectedRow && wave == selectedWave;
        if (!isDropTarget && !isSelected) {
            return;
        }

        if (isDropTarget) {
            painter.setColor(DROP_FILL);
            painter.fillRect(left + 1, top + 1, CELL_WIDTH - 1, CELL_HEIGHT - 1);
        }

        Stroke original = painter.getStroke();
        painter.setStroke(new BasicStroke(2f));
        if (isDropTarget) {
            painter.setColor(DROP_LINE);
        } else {
            painter.setColor(SELECT_LINE);
        }
        painter.drawRect(left + 2, top + 2, CELL_WIDTH - 4, CELL_HEIGHT - 4);
        painter.setStroke(original);
    }

    /**
     * 算出一段文字要往右挪多少才能在给定宽度里居中。
     *
     * 参数：painter 是画笔，用来量字宽；text 是文字；available 是可用宽度。
     * 返回：文字左端相对可用区域左沿的偏移。
     */
    private static int centerOffset(Graphics2D painter, String text, int available) {
        int textWidth = painter.getFontMetrics().stringWidth(text);
        return (available - textWidth) / 2;
    }
}
