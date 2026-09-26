package pvz.world;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import pvz.plant.Cards;
import pvz.zombie.Zombie;

/**
 * 负责读取项目 assets 目录里的图片和关卡。
 *
 * 素材大多是自带透明通道的 GIF 动图，一个文件就是一整段动画；
 * 少数是单张 PNG 或按编号排列的 PNG 序列。游戏代码只认"动画名"
 * （如 Peashooter、ZombieAttack），这里用一张对照表把动画名对应到具体文件，
 * 这样换素材时只需要改这一处。
 */
public class Assets {
    /** 动画名 → 帧列表。 */
    private final Map<String, List<BufferedImage>> frames = new HashMap<String, List<BufferedImage>>();

    /** 界面图片名 → 单张图片。 */
    private final Map<String, BufferedImage> images = new HashMap<String, BufferedImage>();

    /** 缓存缩放后的帧，避免每次绘制都重新缩放。 */
    private final Map<String, BufferedImage> scaledImages = new HashMap<String, BufferedImage>();

    /** 缓存每帧图上真正看得见的范围，避免每帧都重新扫描像素。 */
    private final Map<String, int[]> visibleBoundsCache = new HashMap<String, int[]>();

    /** 缓存每段动画所有帧可见范围的并集。 */
    private final Map<String, int[]> animationBoundsCache = new HashMap<String, int[]>();

    /** assets 目录。 */
    private final Path root;

    /**
     * 读取全部素材。
     *
     * 参数：assetRoot 是项目里的 assets 目录。
     * 异常：目录不存在或图片读取失败时抛出 IOException。
     */
    public Assets(Path assetRoot) throws IOException {
        root = assetRoot;
        if (!Files.isDirectory(root)) {
            throw new IOException("找不到素材目录：" + root.toAbsolutePath().normalize());
        }
        loadScreens();
        loadCards();
        loadBackgrounds();
        loadPlants();
        loadZombies();
        loadBullets();
    }

    /** 读取 JSON 文件，返回其根对象。 */
    public static JsonObject readObject(Path path) throws IOException {
        String text = Files.readString(path);
        JsonElement element = JsonParser.parseString(text);
        return element.getAsJsonObject();
    }

    /** 界面上用到的整张图片：菜单、选卡面板、按钮、胜负画面、小推车等。 */
    private void loadScreens() throws IOException {
        String[] names = {
            "MainMenu", "Adventure_0", "Adventure_1", "ChooserBackground", "MoveBackground",
            "PanelBackground", "StartButton", "GameVictory", "GameLoose", "car", "Boom",
            // 开局倒计时用的"准备-安放-开始"三张图，镜头移回草坪后按顺序显示。
            "ReadySetPlant1", "ReadySetPlant2", "ReadySetPlant3"
        };
        for (int index = 0; index < names.length; index++) {
            single(names[index], "Screen/" + names[index] + ".png");
        }
    }

    /**
     * 卡片图片：Cards 目录里的文件按文件名登记。
     *
     * 豌豆射手、向日葵、大嘴花、樱桃炸弹四张卡的文件名只是编号，
     * 这里按卡面上的植物补上对应的名字。
     */
    private void loadCards() throws IOException {
        List<Path> files = new ArrayList<Path>();
        try (Stream<Path> stream = Files.list(root.resolve("Cards"))) {
            Iterator<Path> iterator = stream.iterator();
            while (iterator.hasNext()) {
                files.add(iterator.next());
            }
        }
        for (Path file : files) {
            String filename = file.getFileName().toString();
            if (!filename.toLowerCase().endsWith(".png")) {
                continue;
            }
            String name = filename.substring(0, filename.length() - 4);
            images.put(name, readFrames(file).get(0));
        }
        alias("card_peashooter", "card_1");
        alias("card_sunflower", "card_2");
        alias("card_chomper", "card_3");
        alias("card_cherrybomb", "card_4");
    }

    /** 关卡背景，下标就是关卡文件里的 background_type。 */
    private void loadBackgrounds() throws IOException {
        sequence("Background",
            "Map/map0.jpg",           // 0：白天草坪
            "Map/Background_1.jpg",   // 1：夜晚草坪
            "Map/Background_2.jpg",   // 2：白天泳池
            "Map/Background_3.jpg",   // 3：夜晚泳池
            "Map/Background_4.jpg");  // 4：保龄球草坪（带红线）
    }

    /** 植物、阳光以及植物相关的特效。 */
    // TODO：【必做-1】新增植物时需要调用 animation() 登记植物动画（名字必须和 PlantCatalog 中的一致）
    private void loadPlants() throws IOException {
        animation("Sun", "Screen/Sun.gif");

        animation("SunFlower", "Plants/SunFlower/1.gif");
        animation("Peashooter", "Plants/Peashooter/1.gif");
        animation("SnowPea", "Plants/SnowPea/1.gif");
        animation("RepeaterPea", "Plants/Repeater/1.gif");
        animation("Threepeater", "Plants/Threepeater/Threepeater.gif");

        animation("WallNut", "Plants/WallNut/1.gif");
        animation("WallNut_cracked1", "Plants/WallNut/Wallnut_cracked1.gif");
        animation("WallNut_cracked2", "Plants/WallNut/Wallnut_cracked2.gif");
        // 保龄球用单张的坚果图，滚动时由绘制代码按距离旋转。
        animation("WallNutBowling", "Plants/WallNut/0.gif");
        animation("RedWallNutBowling", "Plants/WallNut/boom.gif");
        sequence("RedWallNutBowlingExplode", "Screen/Boom.png");

        animation("CherryBomb", "Plants/CherryBomb/1.gif");
        animation("CherryBombExplode", "Plants/CherryBomb/Boom.gif");

        animation("Chomper", "Plants/Chomper/1.gif");
        animation("ChomperAttack", "Plants/Chomper/ChomperAttack.gif");
        animation("ChomperDigest", "Plants/Chomper/ChomperDigest.gif");

        animation("PuffShroom", "Plants/PuffShroom/PuffShroom.gif");
        animation("PuffShroomSleep", "Plants/PuffShroom/PuffShroomSleep.gif");

        // 土豆雷：1.gif 是刚种下还埋在土里的样子，PotatoMine.gif 是出土后闪灯。
        animation("PotatoMineInit", "Plants/PotatoMine/1.gif");
        animation("PotatoMine", "Plants/PotatoMine/PotatoMine.gif");
        animation("PotatoMineExplode", "Plants/PotatoMine/PotatoMine_mashed.gif");

        animation("Squash", "Plants/Squash/1.gif");
        // 窝瓜跳起砸下这 4 帧停留时间差别很大，按原始时长展开，砸下去的节奏才对。
        stretchedAnimation("SquashAttack", "Plants/Squash/SquashAttack.gif");

        animation("Spikeweed", "Plants/Spikeweed/Spikeweed.gif");

        animation("Jalapeno", "Plants/Jalapeno/Jalapeno.gif");
        animation("JalapenoExplode", "Plants/Jalapeno/JalapenoAttack.gif");

        animation("ScaredyShroom", "Plants/ScaredyShroom/ScaredyShroom.gif");
        animation("ScaredyShroomCry", "Plants/ScaredyShroom/ScaredyShroomCry.gif");
        animation("ScaredyShroomSleep", "Plants/ScaredyShroom/ScaredyShroomSleep.gif");

        // 阳光菇：SunShroom2.gif 是刚种下的小个子，SunShroom.gif 是长大后的样子。
        animation("SunShroom", "Plants/SunShroom/SunShroom2.gif");
        animation("SunShroomBig", "Plants/SunShroom/SunShroom.gif");
        animation("SunShroomSleep", "Plants/SunShroom/SunShroomSleep.gif");

        animation("IceShroom", "Plants/IceShroom/IceShroom.gif");
        animation("IceShroomSleep", "Plants/IceShroom/IceShroomSleep.gif");
        animation("IceShroomSnow", "Plants/IceShroom/Snow.gif");
        animation("IceShroomTrap", "Plants/IceShroom/icetrap.gif");

        animation("HypnoShroom", "Plants/HypnoShroom/HypnoShroom.gif");
        animation("HypnoShroomSleep", "Plants/HypnoShroom/HypnoShroomSleep.gif");
    }

    /** 僵尸的走路、啃食、掉头、死亡等动画。 */
    // TODO：【必做-8】新增僵尸时需要调用 animation() 登记僵尸所有动画状态
    //                （基础、啃食、死亡、被炸死、掉头、掉头啃食、飞出去的头等）
    private void loadZombies() throws IOException {
        animation("Zombie", "Zombies/Zombie/Zombie.gif");
        animation("ZombieAttack", "Zombies/Zombie/ZombieAttack.gif");
        animation("ZombieLostHead", "Zombies/Zombie/ZombieLostHead.gif");
        animation("ZombieLostHeadAttack", "Zombies/Zombie/ZombieLostHeadAttack.gif");
        animation("ZombieDie", "Zombies/Zombie/ZombieDie.gif");
        animation("ZombieHead", "Zombies/Zombie/ZombieHead.gif");
        animation("BoomDie", "Zombies/Zombie/BoomDie.gif");

        // 独臂僵尸：普通僵尸、路障和铁桶的本体掉到一半血时换成这套图。
        // 素材画布比原版大得多，所以在登记时就缩放好，后面和普通僵尸走同一套代码。
        scaledAnimation("ZombieNoArm", "Zombies/Zombie/ZombieNoArm.gif",
            Layout.ZOMBIE_NO_ARM_SCALE);
        scaledAnimation("ZombieNoArmAttack", "Zombies/Zombie/ZombieNoArmAttack.gif",
            Layout.ZOMBIE_NO_ARM_SCALE);
        scaledAnimation("ZombieNoArmDie", "Zombies/Zombie/ZombieNoArmDie.gif",
            Layout.ZOMBIE_NO_ARM_SCALE);
        scaledAnimation("ZombieNoArmLostHead", "Zombies/Zombie/ZombieNoArmLostHead.gif",
            Layout.ZOMBIE_NO_ARM_SCALE);
        // 这张的导出分辨率只有别的四分之一，得用单独的倍数。
        scaledAnimation("ZombieNoArmLostHeadAttack",
            "Zombies/Zombie/ZombieNoArmLostHeadAttack.gif", Layout.ZOMBIE_NO_ARM_SMALL_SCALE);

        animation("ConeheadZombie", "Zombies/ConeheadZombie/ConeheadZombie.gif");
        animation("ConeheadZombieAttack", "Zombies/ConeheadZombie/ConeheadZombieAttack.gif");
        animation("BucketheadZombie", "Zombies/BucketheadZombie/BucketheadZombie.gif");
        animation("BucketheadZombieAttack", "Zombies/BucketheadZombie/BucketheadZombieAttack.gif");

        animation("FlagZombie", "Zombies/FlagZombie/FlagZombie.gif");
        animation("FlagZombieAttack", "Zombies/FlagZombie/FlagZombieAttack.gif");
        animation("FlagZombieLostHead", "Zombies/FlagZombie/FlagZombieLostHead.gif");
        animation("FlagZombieLostHeadAttack", "Zombies/FlagZombie/FlagZombieLostHeadAttack.gif");

        // 报纸僵尸的文件名里，1 表示还拿着报纸，0 表示报纸已经被打掉。
        animation("NewspaperZombie", "Zombies/NewspaperZombie/HeadWalk1.gif");
        animation("NewspaperZombieAttack", "Zombies/NewspaperZombie/HeadAttack1.gif");
        animation("NewspaperZombieNoPaper", "Zombies/NewspaperZombie/HeadWalk0.gif");
        animation("NewspaperZombieNoPaperAttack", "Zombies/NewspaperZombie/HeadAttack0.gif");
        animation("NewspaperZombieLostHead", "Zombies/NewspaperZombie/LostHeadWalk0.gif");
        animation("NewspaperZombieLostHeadAttack", "Zombies/NewspaperZombie/LostHeadAttack0.gif");
        animation("NewspaperZombieDie", "Zombies/NewspaperZombie/Die.gif");
        animation("NewspaperZombieHead", "Zombies/NewspaperZombie/Head.gif");
        animation("NewspaperZombieBoomDie", "Zombies/NewspaperZombie/BoomDie.gif");
    }

    /** 豌豆、冰豆、蘑菇孢子以及它们打中后的效果。 */
    // TODO：【选做-2.5】新增植物时需要调用 animation()/sequence() 登记子弹材质，
    //                 并在 ShooterActions.bulletNameFor() 中返回对应名字。
    private void loadBullets() throws IOException {
        sequence("PeaNormal", "bullets/PeaNormal/PeaNormal_0.png");
        sequence("PeaIce", "bullets/PeaIce/PeaIce_0.png");
        sequence("PeaNormalExplode", "bullets/PeaNormalExplode/PeaNormalExplode_0.png");
        sequence("BulletMushRoom",
            "bullets/BulletMushRoom/BulletMushRoom_0.png",
            "bullets/BulletMushRoom/BulletMushRoom_1.png",
            "bullets/BulletMushRoom/BulletMushRoom_2.png",
            "bullets/BulletMushRoom/BulletMushRoom_3.png",
            "bullets/BulletMushRoom/BulletMushRoom_4.png");
        // 这组孢子图是从大图上切下来的，边上残留着一圈紫色底，要先擦掉。
        for (BufferedImage image : frames.get("BulletMushRoom")) {
            clearEdgeColor(image, 0xAA71BA);
        }
        animation("BulletMushRoomExplode", "bullets/BulletMushRoomExplode/BulletMushRoomExplode_0.gif");
    }

    /** 登记一张界面图片（取文件第一帧）。 */
    private void single(String name, String file) throws IOException {
        images.put(name, readFrames(root.resolve(file)).get(0));
    }

    /** 给已登记的界面图片再起一个名字。 */
    private void alias(String name, String existing) throws IOException {
        BufferedImage image = images.get(existing);
        if (image == null) {
            throw new IOException("缺少卡片图片：" + existing);
        }
        images.put(name, image);
    }

    /** 登记一段动画：一个 GIF 文件里的所有帧。 */
    private void animation(String name, String file) throws IOException {
        List<BufferedImage> list = new ArrayList<BufferedImage>();
        for (GifFrame frame : readGif(root.resolve(file))) {
            list.add(frame.image);
        }
        frames.put(name, list);
    }

    /** 登记一段由若干单独图片按顺序组成的动画。 */
    private void sequence(String name, String... files) throws IOException {
        List<BufferedImage> list = new ArrayList<BufferedImage>();
        for (int index = 0; index < files.length; index++) {
            list.add(readFrames(root.resolve(files[index])).get(0));
        }
        frames.put(name, list);
    }

    /**
     * 登记一段动画，并按 GIF 里每帧的停留时间把帧复制展开。
     *
     * 游戏里所有帧都按同一个间隔播放，而有些 GIF 各帧停留时间差别很大。
     * 展开之后，停得久的帧会重复出现几次，按统一间隔播放也能还原原来的节奏。
     */
    private void stretchedAnimation(String name, String file) throws IOException {
        long unit = Layout.DEFAULT_ANIMATION_INTERVAL;
        List<BufferedImage> list = new ArrayList<BufferedImage>();
        for (GifFrame frame : readGif(root.resolve(file))) {
            long repeat = Math.max(1, Math.round((double) frame.delay / unit));
            for (long count = 0; count < repeat; count++) {
                list.add(frame.image);
            }
        }
        frames.put(name, list);
    }

    /**
     * 登记一段动画，并把每一帧都缩放到指定倍数。
     *
     * 有些素材的画布比原版大得多（比如独臂僵尸那几组，站立高度是原版的六倍多），
     * 直接放进来会是个巨人。在这里一次缩好，后面算可见范围、算碰撞盒、
     * 换动画时对齐底边就都不用再管缩放，和别的僵尸走同一套代码。
     *
     * 缩小时分几步走：一次缩到六分之一会丢很多像素，分几次减半再缩更清楚。
     *
     * 参数：name 是动画名；file 是文件路径；scale 是目标倍数（小于 1 表示缩小）。
     */
    private void scaledAnimation(String name, String file, double scale) throws IOException {
        List<BufferedImage> list = new ArrayList<BufferedImage>();
        for (GifFrame frame : readGif(root.resolve(file))) {
            list.add(resize(frame.image, scale));
        }
        frames.put(name, list);
    }

    /**
     * 把一张图缩放到指定倍数。
     *
     * 参数：source 是原图；scale 是倍数。
     * 返回：缩放后的新图，宽高至少留 1 像素。
     */
    private static BufferedImage resize(BufferedImage source, double scale) {
        BufferedImage current = source;
        double remaining = scale;
        // 先反复对折到接近目标，最后再走一次带插值的缩放。
        while (remaining <= 0.5) {
            current = half(current);
            remaining = remaining * 2;
        }

        int width = Math.max(1, (int) Math.round(current.getWidth() * remaining));
        int height = Math.max(1, (int) Math.round(current.getHeight() * remaining));
        if (width == current.getWidth() && height == current.getHeight()) {
            return current;
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = result.createGraphics();
        painter.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        painter.drawImage(current, 0, 0, width, height, null);
        painter.dispose();
        return result;
    }

    /** 把一张图宽高各取一半。 */
    private static BufferedImage half(BufferedImage source) {
        int width = Math.max(1, source.getWidth() / 2);
        int height = Math.max(1, source.getHeight() / 2);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = result.createGraphics();
        painter.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        painter.drawImage(source, 0, 0, width, height, null);
        painter.dispose();
        return result;
    }

    /** GIF 里的一帧：合成后的完整画面和它的停留时间（毫秒）。 */
    private static class GifFrame {
        /** 合成后的完整画面。 */
        final BufferedImage image;

        /** 这一帧停留多少毫秒。 */
        final long delay;

        /** 保存一帧的画面和停留时间。 */
        GifFrame(BufferedImage frameImage, long frameDelay) {
            image = frameImage;
            delay = frameDelay;
        }
    }

    /** 读取一个图片文件的所有帧；非 GIF 图片只有一帧。 */
    private static List<BufferedImage> readFrames(Path path) throws IOException {
        List<BufferedImage> list = new ArrayList<BufferedImage>();
        if (path.getFileName().toString().toLowerCase().endsWith(".gif")) {
            for (GifFrame frame : readGif(path)) {
                list.add(frame.image);
            }
            return list;
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException("缺少素材文件：" + path);
        }
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("无法解码图片：" + path);
        }
        list.add(toArgb(image));
        return list;
    }

    /**
     * 解码 GIF 动图的全部帧。
     *
     * GIF 的每一帧往往只记录"和上一帧不同的那一小块"，
     * 所以要按帧的位置和处置方式，一层层叠到同一块画布上，
     * 每叠完一帧就拷贝一份画布，才能得到完整的画面。
     */
    private static List<GifFrame> readGif(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("缺少素材文件：" + path);
        }
        ImageReader reader = ImageIO.getImageReadersByFormatName("gif").next();
        List<GifFrame> result = new ArrayList<GifFrame>();
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            reader.setInput(input, false);
            int count = reader.getNumImages(true);

            // 先读出所有帧和它们的位置，才能确定画布需要多大。
            List<BufferedImage> pieces = new ArrayList<BufferedImage>();
            List<IIOMetadataNode> metas = new ArrayList<IIOMetadataNode>();
            int width = 0;
            int height = 0;
            IIOMetadata streamData = reader.getStreamMetadata();
            if (streamData != null) {
                IIOMetadataNode tree = (IIOMetadataNode) streamData.getAsTree("javax_imageio_gif_stream_1.0");
                IIOMetadataNode screen = child(tree, "LogicalScreenDescriptor");
                if (screen != null) {
                    width = intAttribute(screen, "logicalScreenWidth", 0);
                    height = intAttribute(screen, "logicalScreenHeight", 0);
                }
            }
            for (int index = 0; index < count; index++) {
                BufferedImage piece = reader.read(index);
                IIOMetadataNode tree = (IIOMetadataNode) reader.getImageMetadata(index)
                    .getAsTree("javax_imageio_gif_image_1.0");
                IIOMetadataNode descriptor = child(tree, "ImageDescriptor");
                int left = intAttribute(descriptor, "imageLeftPosition", 0);
                int top = intAttribute(descriptor, "imageTopPosition", 0);
                width = Math.max(width, left + piece.getWidth());
                height = Math.max(height, top + piece.getHeight());
                pieces.add(piece);
                metas.add(tree);
            }

            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int index = 0; index < count; index++) {
                BufferedImage piece = pieces.get(index);
                IIOMetadataNode tree = metas.get(index);
                IIOMetadataNode descriptor = child(tree, "ImageDescriptor");
                int left = intAttribute(descriptor, "imageLeftPosition", 0);
                int top = intAttribute(descriptor, "imageTopPosition", 0);
                String disposal = "none";
                long delay = Layout.DEFAULT_ANIMATION_INTERVAL;
                IIOMetadataNode control = child(tree, "GraphicControlExtension");
                if (control != null) {
                    disposal = control.getAttribute("disposalMethod");
                    delay = intAttribute(control, "delayTime", 10) * 10L;
                }

                // "恢复到上一帧"要先把叠加前的画布存起来。
                BufferedImage before = null;
                if (disposal.equals("restoreToPrevious")) {
                    before = copy(canvas);
                }

                Graphics2D painter = canvas.createGraphics();
                painter.drawImage(piece, left, top, null);
                painter.dispose();
                result.add(new GifFrame(copy(canvas), delay));

                // 按处置方式为下一帧准备画布。
                if (disposal.equals("restoreToBackgroundColor")) {
                    Graphics2D eraser = canvas.createGraphics();
                    eraser.setComposite(AlphaComposite.Clear);
                    eraser.fillRect(left, top, piece.getWidth(), piece.getHeight());
                    eraser.dispose();
                } else if (before != null) {
                    canvas = before;
                }
            }
        } finally {
            reader.dispose();
        }
        if (result.isEmpty()) {
            throw new IOException("GIF 中没有任何帧：" + path);
        }
        return result;
    }

    /** 在元数据树里找指定名字的直接子节点，找不到返回 null。 */
    private static IIOMetadataNode child(IIOMetadataNode parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node.getNodeName().equals(name)) {
                return (IIOMetadataNode) node;
            }
        }
        return null;
    }

    /** 读取元数据节点里的整数属性，缺失时返回默认值。 */
    private static int intAttribute(IIOMetadataNode node, String name, int fallback) {
        if (node == null) {
            return fallback;
        }
        String value = node.getAttribute(name);
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        return Integer.parseInt(value);
    }

    /**
     * 把贴着图片边缘、颜色恰好为 rgb 的那一片像素改成透明。
     *
     * 从四条边上的同色像素出发向内扩散，只擦掉和边缘连成一片的底色，
     * 图案内部偶尔用到的同一种颜色不会被误擦。
     */
    private static void clearEdgeColor(BufferedImage image, int rgb) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[] visited = new boolean[width * height];
        ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
        for (int column = 0; column < width; column++) {
            queue.add(new int[] {column, 0});
            queue.add(new int[] {column, height - 1});
        }
        for (int row = 0; row < height; row++) {
            queue.add(new int[] {0, row});
            queue.add(new int[] {width - 1, row});
        }
        while (!queue.isEmpty()) {
            int[] point = queue.poll();
            int column = point[0];
            int row = point[1];
            if (column < 0 || row < 0 || column >= width || row >= height) {
                continue;
            }
            int position = row * width + column;
            if (visited[position]) {
                continue;
            }
            visited[position] = true;
            int argb = image.getRGB(column, row);
            if (((argb >>> 24) & 0xFF) == 0 || (argb & 0xFFFFFF) != rgb) {
                continue;
            }
            image.setRGB(column, row, 0);
            queue.add(new int[] {column + 1, row});
            queue.add(new int[] {column - 1, row});
            queue.add(new int[] {column, row + 1});
            queue.add(new int[] {column, row - 1});
        }
    }

    /** 复制一张图片。 */
    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = result.createGraphics();
        painter.setComposite(AlphaComposite.Src);
        painter.drawImage(source, 0, 0, null);
        painter.dispose();
        return result;
    }

    /** 统一转成带透明通道的格式，方便后面逐像素判断透明度。 */
    private static BufferedImage toArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        return copy(source);
    }

    /** 取得单张界面素材；不存在时明确报错。 */
    public BufferedImage image(String name) {
        BufferedImage image = images.get(name);
        if (image == null) {
            throw new IllegalArgumentException("缺少图片：" + name);
        }
        return image;
    }

    /** 取得动画的指定帧，索引循环回到第一帧。 */
    public BufferedImage frame(String name, int index) {
        List<BufferedImage> list = animationFrames(name);
        return list.get(wrap(index, list.size()));
    }

    /** 返回动画长度，供实体控制播放速度。 */
    public int count(String name) {
        return animationFrames(name).size();
    }

    /** 判断某个动画是否存在。 */
    public boolean hasAnimation(String name) {
        return frames.containsKey(name);
    }

    /**
     * 取得动画某一帧，并按需要缩放。
     *
     * 参数：name 是动画名；index 是第几帧（超出会循环）；scale 是缩放倍数。
     */
    public BufferedImage sprite(String name, int index, double scale) {
        List<BufferedImage> animation = animationFrames(name);
        int frameIndex = wrap(index, animation.size());
        BufferedImage source = animation.get(frameIndex);
        if (scale == 1.0) {
            return source;
        }
        String cacheKey = name + ":" + frameIndex + ":" + scale;
        BufferedImage cached = scaledImages.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        int scaledWidth = Math.max(1, (int) (source.getWidth() * scale));
        int scaledHeight = Math.max(1, (int) (source.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = scaled.createGraphics();
        painter.drawImage(source, 0, 0, scaledWidth, scaledHeight, null);
        painter.dispose();
        scaledImages.put(cacheKey, scaled);
        return scaled;
    }

    /**
     * 算出某帧图上"真正看得见"的那块区域。
     *
     * 动图每帧周围都留了大片透明边距，如果直接用整张图做碰撞，
     * 僵尸会隔着一段空气就"咬到"植物。这里逐像素找出不透明的范围，
     * 让碰撞盒刚好等于视觉上的身体。
     *
     * 参数：name 是动画名；index 是第几帧。
     * 返回：四个整数组成的数组，依次是左、上、右、下边界（右下是开区间），未缩放。
     */
    public int[] visibleBounds(String name, int index) {
        List<BufferedImage> animation = animationFrames(name);
        int frameIndex = wrap(index, animation.size());
        String cacheKey = name + ":" + frameIndex;
        int[] cached = visibleBoundsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BufferedImage image = animation.get(frameIndex);
        int width = image.getWidth();
        int height = image.getHeight();
        int left = width;
        int top = height;
        int right = 0;
        int bottom = 0;

        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int alpha = (image.getRGB(column, row) >>> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }
                if (column < left) {
                    left = column;
                }
                if (column + 1 > right) {
                    right = column + 1;
                }
                if (row < top) {
                    top = row;
                }
                if (row + 1 > bottom) {
                    bottom = row + 1;
                }
            }
        }

        // 整张图都是透明的（比如动画里的空白帧），就退回整块区域，免得算出负数宽高。
        if (right <= left || bottom <= top) {
            left = 0;
            top = 0;
            right = width;
            bottom = height;
        }

        int[] result = new int[] {left, top, right, bottom};
        visibleBoundsCache.put(cacheKey, result);
        return result;
    }

    /**
     * 算出整段动画"看得见的部分"的总范围，即所有帧可见区域的并集。
     *
     * 动图每一帧的轮廓都在变（僵尸走路时手臂前后摆），如果碰撞盒逐帧跟着变，
     * 僵尸会在"碰到植物"和"没碰到"之间来回切换，啃食动画不停从头播放，看起来就在颤抖。
     * 用整段动画的总范围做碰撞盒，大小固定不变，就不会出现这种抖动。
     *
     * 参数：name 是动画名。
     * 返回：四个整数组成的数组，依次是左、上、右、下边界（右下是开区间），未缩放。
     */
    public int[] animationBounds(String name) {
        int[] cached = animationBoundsCache.get(name);
        if (cached != null) {
            return cached;
        }
        // 先以第 0 帧的范围作为起点。取出来放进单独的变量里，不直接改缓存里的数组。
        int[] firstFrame = visibleBounds(name, 0);
        int left = firstFrame[0];
        int top = firstFrame[1];
        int right = firstFrame[2];
        int bottom = firstFrame[3];

        // 再逐帧把范围往外扩：左、上取最小，右、下取最大，就能包住所有帧。
        int frameCount = count(name);
        for (int index = 1; index < frameCount; index++) {
            int[] visible = visibleBounds(name, index);
            left = Math.min(left, visible[0]);
            top = Math.min(top, visible[1]);
            right = Math.max(right, visible[2]);
            bottom = Math.max(bottom, visible[3]);
        }

        int[] result = new int[] {left, top, right, bottom};
        animationBoundsCache.put(name, result);
        return result;
    }

    /** 返回某个关卡的 JSON 文件位置。 */
    public Path levelPath(int number) {
        return root.resolve("levels/level_" + number + ".json");
    }

    /** 取出动画的帧列表，不存在时明确报错。 */
    private List<BufferedImage> animationFrames(String name) {
        List<BufferedImage> list = frames.get(name);
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("缺少动画：" + name);
        }
        return list;
    }

    /** 把任意帧号折回到 0 到 size-1 之间。 */
    private static int wrap(int index, int size) {
        int result = index % size;
        if (result < 0) {
            result += size;
        }
        return result;
    }
}
