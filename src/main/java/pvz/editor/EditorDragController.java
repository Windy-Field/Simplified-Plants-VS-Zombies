package pvz.editor;

import java.awt.Point;

/**
 * 编辑器里"拖动僵尸"这件事的约定。
 *
 * 僵尸列表和出怪网格是两块各自独立的面板，但一次拖动往往从一块开始、
 * 在另一块结束，两边都要知道当前拖的是什么。于是把拖动的处理交给主窗口，
 * 两块面板只管把鼠标动作按这个约定报上去，不必互相认识。
 */
public interface EditorDragController {
    /**
     * 报告一次拖动开始了。
     *
     * 参数：kind 是拖的僵尸品种；count 是拖的只数；
     *       fromRow、fromWave 是拖动起点所在的格子，从僵尸列表拖出来时传 -1；
     *       screenPoint 是鼠标在屏幕上的位置。
     */
    void beginDrag(String kind, int count, int fromRow, int fromWave, Point screenPoint);

    /**
     * 报告拖动中鼠标移动了。
     *
     * 参数：screenPoint 是鼠标在屏幕上的位置。
     */
    void updateDrag(Point screenPoint);

    /**
     * 报告拖动结束，在这个位置松手了。
     *
     * 参数：screenPoint 是鼠标在屏幕上的位置。
     */
    void finishDrag(Point screenPoint);

    /**
     * 查询此刻是不是正在拖动。
     *
     * 返回：正在拖动就返回真。面板靠它决定要不要画悬停高亮。
     */
    boolean isDragging();

    /**
     * 查询僵尸列表里当前选中的是哪一种。
     *
     * 返回：僵尸品种名；一种都没选时返回 null。
     */
    String selectedKind();

    /**
     * 报告用户在僵尸列表里选中了某一种僵尸。
     *
     * 参数：kind 是僵尸品种名。
     */
    void selectKind(String kind);

    /** 报告网格内容被改动了，主窗口据此刷新画面和底部的统计。 */
    void designChanged();

    /** 报告用户换了选中的格子，主窗口据此把随机行复选框同步过来。 */
    void selectionChanged();
}
