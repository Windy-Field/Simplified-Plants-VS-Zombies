package pvz.tools;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

/** 共用界面文字的定位方法，让选关和图鉴在系统缩放后仍使用相同的居中规则 */
public final class TextDrawing {
    /** 只提供绘制方法，不需要创建工具对象 */
    private TextDrawing() {
    }

    /** 参数 painter 提供字体和颜色，text 是非空文字，bounds 是区域；按字形的可见范围居中 */
    public static void drawCentered(Graphics2D painter, String text, Rectangle bounds) {
        TextLayout layout = new TextLayout(text, painter.getFont(), painter.getFontRenderContext());
        Rectangle2D ink = layout.getBounds();
        float left = (float) (bounds.getCenterX() - ink.getCenterX());
        float baseline = (float) (bounds.getCenterY() - ink.getCenterY());
        layout.draw(painter, left, baseline);
    }
}
