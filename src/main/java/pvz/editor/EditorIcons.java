package pvz.editor;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import pvz.tools.AssetToolkit;
import pvz.world.Assets;

/**
 * 给编辑器准备僵尸小图标。
 *
 * 游戏的僵尸动图四周留了大片透明边距，直接缩小放进格子里，
 * 僵尸会显得又小又偏。这里先把真正看得见的那块裁出来再缩放，
 * 图标就能填满格子。裁好的图会存起来，同一个尺寸只做一次。
 */
public class EditorIcons {
    /** 提供僵尸动画的原始图片。 */
    private final Assets assets;

    /** 已经做好的图标，键是"品种名:高度"。 */
    private final Map<String, BufferedImage> cache = new HashMap<String, BufferedImage>();

    /**
     * 创建图标工具。
     *
     * 参数：assets 用来取僵尸动画的第一帧。
     */
    public EditorIcons(Assets assets) {
        this.assets = assets;
    }

    /**
     * 取某种僵尸的图标。
     *
     * 参数：kind 是僵尸品种名；height 是想要的图标高度，宽度按原比例算。
     * 返回：裁掉透明边距并缩放好的图标。
     */
    public BufferedImage icon(String kind, int height) {
        String cacheKey = kind + ":" + height;
        BufferedImage cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 只取动画第一帧，并按可见范围把身体那块裁下来。
        BufferedImage source = assets.frame(kind, 0);
        int[] visible = assets.visibleBounds(kind, 0);
        int bodyWidth = visible[2] - visible[0];
        int bodyHeight = visible[3] - visible[1];
        BufferedImage body = AssetToolkit.crop(source, visible[0], visible[1],
            bodyWidth, bodyHeight);

        // 按高度算出缩放比例，宽度跟着等比缩，图标才不会变形。
        double scale = (double) height / bodyHeight;
        int scaledWidth = Math.max(1, (int) Math.round(bodyWidth * scale));
        BufferedImage result = AssetToolkit.resize(body, scaledWidth, height);

        cache.put(cacheKey, result);
        return result;
    }
}
