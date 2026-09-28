package pvz.tools;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 提供通用的图片处理方法。
 *
 * 这里不负责登记游戏素材，也不负责读取 GIF 动画。
 * 它只处理可以独立复用的图片复制、格式转换、裁剪和缩放。
 */
public final class AssetToolkit {
    /** 这个类只提供静态方法，不允许创建对象。 */
    private AssetToolkit() {
    }

    /**
     * 把图片缩放到指定大小。
     *
     * 参数：source 是原图；width 和 height 是目标大小。
     * 返回：缩放后的 ARGB 图片。
     */
    public static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = result.createGraphics();
        painter.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        painter.drawImage(source, 0, 0, width, height, null);
        painter.dispose();
        return result;
    }

    /**
     * 按比例缩放图片。
     *
     * 缩小时先分几次缩小，再做最后一次插值缩放，尽量保留细节。
     * 参数：source 是原图；scale 是缩放比例。
     * 返回：缩放后的图片。
     */
    public static BufferedImage resizeByScale(BufferedImage source, double scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("缩放比例必须大于 0");
        }
        if (scale == 1.0) {
            return source;
        }

        BufferedImage current = source;
        double remaining = scale;
        while (remaining <= 0.5) {
            int width = Math.max(1, current.getWidth() / 2);
            int height = Math.max(1, current.getHeight() / 2);
            current = resize(current, width, height);
            remaining = remaining * 2;
        }

        int width = Math.max(1, (int) Math.round(current.getWidth() * remaining));
        int height = Math.max(1, (int) Math.round(current.getHeight() * remaining));
        return resize(current, width, height);
    }

    /**
     * 裁剪图片的一块区域。
     *
     * 参数：source 是原图；left 和 top 是裁剪起点；width 和 height 是裁剪大小。
     * 返回：裁剪后的独立图片。
     */
    public static BufferedImage crop(BufferedImage source, int left, int top,
            int width, int height) {
        BufferedImage result = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = result.createGraphics();
        painter.drawImage(source, 0, 0, width, height,
            left, top, left + width, top + height, null);
        painter.dispose();
        return result;
    }

    /**
     * 复制一张图片。
     *
     * 参数：source 是要复制的图片。
     * 返回：独立的新图片。
     */
    public static BufferedImage copyImage(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = result.createGraphics();
        painter.setComposite(AlphaComposite.Src);
        painter.drawImage(source, 0, 0, null);
        painter.dispose();
        return result;
    }

    /**
     * 把图片转换成带透明通道的格式。
     *
     * 参数：source 是原图片。
     * 返回：ARGB 格式图片。
     */
    public static BufferedImage toArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        return copyImage(source);
    }
}
