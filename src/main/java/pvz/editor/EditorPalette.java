package pvz.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * 编辑器左边那一列可供选用的僵尸。
 *
 * 每种僵尸占一行，显示图标和中文名。点一下表示选中它，
 * 之后在网格的空格子上点左键就会放下这种僵尸；也可以直接把它拖进格子里。
 */
public class EditorPalette extends JPanel {
    /** 每种僵尸占的高度。 */
    public static final int ITEM_HEIGHT = 74;

    /** 整列的宽度。 */
    public static final int PANEL_WIDTH = 152;

    /** 列表里僵尸图标的高度。 */
    private static final int ICON_HEIGHT = 56;

    /** 鼠标按下后要移动超过这么多像素，才算是在拖动而不是点击。 */
    private static final int DRAG_THRESHOLD = 4;

    /** 提供僵尸图标。 */
    private final EditorIcons icons;

    /** 拖动由主窗口统一处理，这里只负责报告。 */
    private final EditorDragController controller;

    /** 鼠标正悬在第几种僵尸上，没有就是 -1。 */
    private int hovered = -1;

    /** 鼠标按下时按在第几种僵尸上，没有就是 -1。 */
    private int pressedItem = -1;

    /** 鼠标按下时的位置，用来判断有没有移动够距离。 */
    private Point pressPoint;

    /** 这一次按下之后是否已经进入拖动状态。 */
    private boolean dragStarted;

    /**
     * 创建僵尸列表。
     *
     * 参数：editorIcons 提供图标；dragController 接收点击和拖动的报告。
     */
    public EditorPalette(EditorIcons editorIcons, EditorDragController dragController) {
        icons = editorIcons;
        controller = dragController;
        setBackground(new Color(0xF2, 0xF5, 0xF0));
        int panelHeight = ITEM_HEIGHT * LevelDesign.ZOMBIE_KINDS.length;
        setPreferredSize(new Dimension(PANEL_WIDTH, panelHeight));

        installMouseHandlers();
    }

    /**
     * 装上鼠标处理。
     *
     * 这里只把事件转交给下面几个有名字的方法，真正做事的代码都在那些方法里。
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
                hovered = -1;
                repaint();
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
     * 按下鼠标：先把这种僵尸设为选中。
     *
     * 到底是点一下还是要拖出去，等松手或者移动够距离才知道。
     *
     * 参数：event 是鼠标事件。
     */
    private void handlePress(MouseEvent event) {
        pressedItem = itemAt(event.getY());
        pressPoint = event.getPoint();
        dragStarted = false;
        if (pressedItem >= 0) {
            controller.selectKind(LevelDesign.ZOMBIE_KINDS[pressedItem]);
            repaint();
        }
    }

    /**
     * 松开鼠标：如果之前已经拖起来了，就告诉主窗口在这里放下。
     *
     * 参数：event 是鼠标事件。
     */
    private void handleRelease(MouseEvent event) {
        if (dragStarted) {
            controller.finishDrag(event.getLocationOnScreen());
        }
        pressedItem = -1;
        pressPoint = null;
        dragStarted = false;
        repaint();
    }

    /**
     * 鼠标移动：记住它悬在哪一行，用来画高亮。
     *
     * 参数：event 是鼠标事件。
     */
    private void updateHover(MouseEvent event) {
        int item = itemAt(event.getY());
        if (item != hovered) {
            hovered = item;
            repaint();
        }
    }

    /**
     * 拖动鼠标：按住不放并移动够距离，就算开始把这种僵尸拖出去了。
     *
     * 参数：event 是鼠标事件。
     */
    private void handleDrag(MouseEvent event) {
        if (pressedItem < 0 || pressPoint == null) {
            return;
        }
        if (dragStarted) {
            controller.updateDrag(event.getLocationOnScreen());
            return;
        }
        int movedX = Math.abs(event.getX() - pressPoint.x);
        int movedY = Math.abs(event.getY() - pressPoint.y);
        if (movedX + movedY < DRAG_THRESHOLD) {
            return;
        }

        dragStarted = true;
        String kind = LevelDesign.ZOMBIE_KINDS[pressedItem];
        controller.beginDrag(kind, 1, -1, -1, event.getLocationOnScreen());
    }

    /**
     * 把鼠标纵坐标换算成第几种僵尸。
     *
     * 参数：y 是鼠标在这块面板里的纵坐标。
     * 返回：僵尸下标；点在空白处返回 -1。
     */
    private int itemAt(int y) {
        int item = y / ITEM_HEIGHT;
        if (item < 0 || item >= LevelDesign.ZOMBIE_KINDS.length) {
            return -1;
        }
        return item;
    }

    /**
     * 画出整列僵尸。
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

        String selected = controller.selectedKind();
        for (int item = 0; item < LevelDesign.ZOMBIE_KINDS.length; item++) {
            String kind = LevelDesign.ZOMBIE_KINDS[item];
            int top = item * ITEM_HEIGHT;
            drawItemBackground(painter, kind, selected, item, top);

            BufferedImage image = icons.icon(kind, ICON_HEIGHT);
            int imageLeft = 14 + (48 - image.getWidth()) / 2;
            int imageTop = top + (ITEM_HEIGHT - ICON_HEIGHT) / 2;
            painter.drawImage(image, imageLeft, imageTop, null);

            painter.setColor(new Color(0x33, 0x33, 0x33));
            painter.setFont(getFont().deriveFont(Font.BOLD, 13f));
            painter.drawString(LevelDesign.ZOMBIE_LABELS[item], 72, top + ITEM_HEIGHT / 2 + 5);

            painter.setColor(new Color(0xDD, 0xDD, 0xDD));
            painter.drawLine(0, top + ITEM_HEIGHT - 1, getWidth(), top + ITEM_HEIGHT - 1);
        }
    }

    /**
     * 画一行的底色：选中的画绿底加边框，鼠标悬着的画浅底。
     *
     * 参数：painter 是画笔；kind 是这一行的僵尸品种；selected 是当前选中的品种；
     *       item 是这一行的下标；top 是这一行的上沿。
     */
    private void drawItemBackground(Graphics2D painter, String kind, String selected,
            int item, int top) {
        if (kind.equals(selected)) {
            painter.setColor(new Color(0xD3, 0xE8, 0xC6));
            painter.fillRect(0, top, getWidth(), ITEM_HEIGHT);
            painter.setColor(new Color(0x4C, 0x8C, 0x2B));
            painter.drawRect(1, top + 1, getWidth() - 3, ITEM_HEIGHT - 3);
            return;
        }
        if (item == hovered) {
            painter.setColor(new Color(0xE6, 0xEE, 0xE1));
            painter.fillRect(0, top, getWidth(), ITEM_HEIGHT);
        }
    }
}
