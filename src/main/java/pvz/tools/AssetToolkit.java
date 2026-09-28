package pvz.tools;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * 素材预处理工具。
 *
 * 这个类提供统一的图片缩放、裁剪、复制、图片序列转 GIF，
 * 以及调用本机 FFmpeg 把视频转 GIF 的方法。它既能被游戏代码调用，
 * 也能直接从命令行运行。
 */
public final class AssetToolkit {
    /** 这个类只提供静态方法，不允许创建对象。 */
    private AssetToolkit() {
    }

    /**
     * 把图片缩放到指定的宽和高。
     *
     * 参数：source 是原图；width 和 height 是目标尺寸。
     * 返回：带透明通道的目标图片。
     */
    public static BufferedImage resize(BufferedImage source, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("目标图片尺寸必须大于 0");
        }

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
     * 按比例缩放图片，缩小时分几次减半以减少细节损失。
     *
     * 参数：source 是原图；scale 是缩放比例，必须大于 0。
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
            int halfWidth = Math.max(1, current.getWidth() / 2);
            int halfHeight = Math.max(1, current.getHeight() / 2);
            current = resize(current, halfWidth, halfHeight);
            remaining = remaining * 2;
        }

        int width = Math.max(1, (int) Math.round(current.getWidth() * remaining));
        int height = Math.max(1, (int) Math.round(current.getHeight() * remaining));
        return resize(current, width, height);
    }

    /**
     * 裁剪图片的一块区域，并返回独立的新图片。
     *
     * 参数：source 是原图；left、top 是裁剪起点；width、height 是裁剪尺寸。
     * 返回：裁剪后的图片。
     */
    public static BufferedImage crop(BufferedImage source, int left, int top,
            int width, int height) {
        if (left < 0 || top < 0 || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("裁剪区域不合法");
        }
        if (left + width > source.getWidth() || top + height > source.getHeight()) {
            throw new IllegalArgumentException("裁剪区域超出图片范围");
        }

        BufferedImage result = new BufferedImage(width, height,
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = result.createGraphics();
        painter.drawImage(source, 0, 0, width, height,
            left, top, left + width, top + height, null);
        painter.dispose();
        return result;
    }

    /**
     * 复制图片，避免后续绘制修改原始素材。
     *
     * 参数：source 是要复制的图片。
     * 返回：独立的新图片。
     */
    public static BufferedImage copyImage(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = result.createGraphics();
        painter.drawImage(source, 0, 0, null);
        painter.dispose();
        return result;
    }

    /**
     * 把图片转换成带透明通道的格式。
     *
     * 参数：source 是原始图片。
     * 返回：ARGB 格式的图片；原图已经是 ARGB 时直接返回原图。
     */
    public static BufferedImage toArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        return copyImage(source);
    }

    /**
     * 读取单张图片并规范化后写成 PNG。
     *
     * 参数：input 是输入图片；output 是输出 PNG；width 和 height 是目标尺寸。
     */
    public static void normalizeImage(Path input, Path output, int width, int height)
            throws IOException {
        BufferedImage source = ImageIO.read(input.toFile());
        if (source == null) {
            throw new IOException("无法读取图片：" + input);
        }
        BufferedImage result = resize(source, width, height);
        createParentDirectory(output);
        ImageIO.write(result, "png", output.toFile());
    }

    /**
     * 把一组图片写成 GIF 动画。
     *
     * 参数：frames 是按播放顺序排列的图片；output 是 GIF 文件；
     * delayMillis 是每帧停留时间，单位是毫秒。
     */
    public static void writeGif(List<BufferedImage> frames, Path output,
            int delayMillis) throws IOException {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("GIF 至少需要一张图片");
        }
        if (delayMillis <= 0) {
            throw new IllegalArgumentException("GIF 帧间隔必须大于 0");
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        ImageWriteParam parameters = writer.getDefaultWriteParam();
        createParentDirectory(output);
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output.toFile())) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            BufferedImage firstFrame = frames.get(0);
            int frameWidth = firstFrame.getWidth();
            int frameHeight = firstFrame.getHeight();
            for (int index = 0; index < frames.size(); index++) {
                BufferedImage frame = toArgb(frames.get(index));
                if (frame.getWidth() != frameWidth || frame.getHeight() != frameHeight) {
                    frame = resize(frame, frameWidth, frameHeight);
                }
                ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(frame);
                IIOMetadata metadata = writer.getDefaultImageMetadata(type, parameters);
                configureGifMetadata(metadata, delayMillis, index == 0);
                IIOImage image = new IIOImage(frame, null, metadata);
                writer.writeToSequence(image, parameters);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    /**
     * 调用本机 FFmpeg，把视频转成指定尺寸和帧率的 GIF。
     *
     * 参数：input 是视频文件；output 是 GIF 文件；width 和 height 是目标尺寸；
     * fps 是每秒帧数。
     * 异常：找不到 FFmpeg 或 FFmpeg 转换失败时抛出 IOException。
     */
    public static void convertVideoToGif(Path input, Path output, int width, int height,
            int fps) throws IOException, InterruptedException {
        if (width <= 0 || height <= 0 || fps <= 0) {
            throw new IllegalArgumentException("视频输出尺寸和帧率必须大于 0");
        }

        createParentDirectory(output);
        List<String> command = new ArrayList<String>();
        command.add("ffmpeg");
        command.add("-y");
        command.add("-i");
        command.add(input.toString());
        command.add("-vf");
        command.add("fps=" + fps + ",scale=" + width + ":" + height
            + ":flags=lanczos");
        command.add(output.toString());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String outputText = readProcessOutput(process);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg 转换失败：\n" + outputText);
        }
    }

    /**
     * 读取外部工具的输出，方便失败时给出原因。
     *
     * 参数：process 是正在运行的外部进程。
     * 返回：进程输出文字。
     */
    private static String readProcessOutput(Process process) throws IOException {
        StringBuilder text = new StringBuilder();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            text.append(line);
            text.append(System.lineSeparator());
            line = reader.readLine();
        }
        return text.toString();
    }

    /** 为输出文件创建父目录。 */
    private static void createParentDirectory(Path output) throws IOException {
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /** 设置 GIF 的帧间隔和无限循环播放属性。 */
    private static void configureGifMetadata(IIOMetadata metadata, int delayMillis,
            boolean firstFrame) throws IOException {
        String format = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
        IIOMetadataNode control = findMetadataNode(root, "GraphicControlExtension");
        control.setAttribute("disposalMethod", "none");
        control.setAttribute("userInputFlag", "FALSE");
        control.setAttribute("transparentColorFlag", "FALSE");
        control.setAttribute("delayTime", Integer.toString(Math.max(1, delayMillis / 10)));

        if (firstFrame) {
            IIOMetadataNode applications = findMetadataNode(root, "ApplicationExtensions");
            IIOMetadataNode application = new IIOMetadataNode("ApplicationExtension");
            application.setAttribute("applicationID", "NETSCAPE");
            application.setAttribute("authenticationCode", "2.0");
            application.setUserObject(new byte[] {1, 0, 0});
            applications.appendChild(application);
        }
        metadata.setFromTree(format, root);
    }

    /** 在 GIF 元数据树中查找指定节点。 */
    private static IIOMetadataNode findMetadataNode(IIOMetadataNode root, String name) {
        for (int index = 0; index < root.getLength(); index++) {
            IIOMetadataNode child = (IIOMetadataNode) root.item(index);
            if (name.equals(child.getNodeName())) {
                return child;
            }
        }
        IIOMetadataNode created = new IIOMetadataNode(name);
        root.appendChild(created);
        return created;
    }

    /**
     * 命令行入口。
     *
     * 支持三个命令：normalize、images-to-gif、video-to-gif。
     * 参数不足或命令不认识时会打印用法说明。
     */
    public static void main(String[] arguments) throws Exception {
        if (arguments.length == 0) {
            printUsage();
            return;
        }

        String command = arguments[0];
        if (command.equals("normalize")) {
            runNormalizeCommand(arguments);
            return;
        }
        if (command.equals("images-to-gif")) {
            runImagesToGifCommand(arguments);
            return;
        }
        if (command.equals("video-to-gif")) {
            runVideoToGifCommand(arguments);
            return;
        }
        printUsage();
    }

    /** 执行图片尺寸规范化命令。 */
    private static void runNormalizeCommand(String[] arguments) throws IOException {
        if (arguments.length != 5) {
            printUsage();
            return;
        }
        Path input = Path.of(arguments[1]);
        Path output = Path.of(arguments[2]);
        int width = Integer.parseInt(arguments[3]);
        int height = Integer.parseInt(arguments[4]);
        normalizeImage(input, output, width, height);
    }

    /** 执行图片序列转 GIF 命令。 */
    private static void runImagesToGifCommand(String[] arguments) throws IOException {
        if (arguments.length < 4) {
            printUsage();
            return;
        }
        Path output = Path.of(arguments[1]);
        int delay = Integer.parseInt(arguments[2]);
        List<BufferedImage> frames = new ArrayList<BufferedImage>();
        for (int index = 3; index < arguments.length; index++) {
            BufferedImage frame = ImageIO.read(Path.of(arguments[index]).toFile());
            if (frame == null) {
                throw new IOException("无法读取图片：" + arguments[index]);
            }
            frames.add(frame);
        }
        writeGif(frames, output, delay);
    }

    /** 执行视频转 GIF 命令。 */
    private static void runVideoToGifCommand(String[] arguments)
            throws IOException, InterruptedException {
        if (arguments.length != 6) {
            printUsage();
            return;
        }
        Path input = Path.of(arguments[1]);
        Path output = Path.of(arguments[2]);
        int width = Integer.parseInt(arguments[3]);
        int height = Integer.parseInt(arguments[4]);
        int fps = Integer.parseInt(arguments[5]);
        convertVideoToGif(input, output, width, height, fps);
    }

    /** 打印命令行用法。 */
    private static void printUsage() {
        System.out.println("素材工具用法：");
        System.out.println("  normalize 输入图片 输出.png 宽 高");
        System.out.println("  images-to-gif 输出.gif 帧间隔毫秒 图片1 图片2 ...");
        System.out.println("  video-to-gif 输入视频 输出.gif 宽 高 帧率");
        System.out.println("视频转 GIF 需要系统中可以直接调用 ffmpeg。");
    }
}
