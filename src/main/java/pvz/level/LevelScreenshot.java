package pvz.level;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import pvz.tools.AssetToolkit;

/** 负责关卡截图的导入、读取和等比例显示，编辑器与选关页共用以免路径规则不一致 */
public final class LevelScreenshot {
    private static final String DIRECTORY = "levels/screenshots";
    private static final Color BACKGROUND = new Color(45, 45, 45);

    /** 仅提供工具方法，不创建持有重复素材状态的对象 */
    private LevelScreenshot() {
    }

    /** 参数 root 是 assets，source 是用户选择的图片；返回导入后的相对路径，不覆盖已有截图 */
    public static String importImage(Path root, Path source) throws IOException {
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            throw new IOException("图片无法读取，请选择 PNG、JPG 或 GIF 图片");
        }
        Path directory = root.resolve(DIRECTORY);
        Files.createDirectories(directory);
        String identifier = UUID.randomUUID().toString();
        String relative = DIRECTORY + "/screenshot_" + identifier + ".png";
        Path target = root.resolve(relative);
        // 统一保存为 PNG，原文件保持不变，之后不再依赖外部路径
        if (!ImageIO.write(image, "png", target.toFile())) {
            throw new IOException("截图无法保存为 PNG");
        }
        return relative;
    }

    /** 参数 root 是素材目录，relative 是关卡保存的路径；返回图片，路径非法或文件损坏时抛出错误 */
    public static BufferedImage read(Path root, String relative) throws IOException {
        Path absoluteRoot = root.toAbsolutePath().normalize();
        Path file = absoluteRoot.resolve(relative).normalize();
        if (!file.startsWith(absoluteRoot) || !Files.isRegularFile(file)) {
            throw new IOException("找不到关卡截图：" + relative);
        }
        Path realRoot = absoluteRoot.toRealPath();
        Path realFile = file.toRealPath();
        if (!realFile.startsWith(realRoot)) {
            throw new IOException("关卡截图必须位于 assets 素材目录内");
        }
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new IOException("关卡截图不是可读取的图片：" + relative);
        }
        return image;
    }

    /** 参数 source 是截图，width 和 height 是显示尺寸；等比例缩放并居中留边，避免拉伸画面 */
    public static BufferedImage fit(BufferedImage source, int width, int height) {
        double scale = Math.min((double) width / source.getWidth(), (double) height / source.getHeight());
        int imageWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int imageHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage resized = AssetToolkit.resize(source, imageWidth, imageHeight);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D painter = result.createGraphics();
        painter.setColor(BACKGROUND);
        painter.fillRect(0, 0, width, height);
        painter.drawImage(resized, (width - imageWidth) / 2, (height - imageHeight) / 2, null);
        painter.dispose();
        return result;
    }
}
