package pvz.world;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * 所有画在屏幕上的东西（植物、僵尸、子弹、阳光）共用的父类。
 *
 * 它负责三件事：记住位置、按时间播放动画、算出碰撞矩形。
 * 子类只要换不同的素材名，就能变成不同的东西。
 */
public class Sprite {
    /** 素材名，同时也是动画名的起点。 */
    public String name;

    /** 精灵左上角的横坐标，用小数是为了让移动更平滑。 */
    public double x;

    /** 精灵左上角的纵坐标。 */
    public double y;

    /** 所在的行号，0 是最上面一行。 */
    public int row;

    /** 剩余血量，掉到 0 或以下就死。 */
    public int health;

    /** 是否还活着；变成假之后不再更新也不再绘制。 */
    public boolean alive = true;

    /** 当前正在播放的动画名，通常和 name 一样，受伤或攻击时会换掉。 */
    public String animation;

    /** 当前动画的起始时刻，用来算播到第几帧了。 */
    public long animationStart;

    /** 每帧停留多少毫秒，数值越小播得越快。 */
    public int interval = 100;

    /** 绘制时的缩放倍数，阳光和寒冰菇会用到。 */
    public double scale = 1.0;

    /**
     * 创建一个精灵，位置按"底边中央"来定。
     *
     * 用底边中央而不是左上角，是因为植物要站在格子上、僵尸要站在地面上，
     * 按底边对齐比按左上角对齐更自然。
     *
     * 参数：kind 是素材名；center 是期望的中心横坐标；bottom 是期望的底边纵坐标；
     *       lane 是所在行；hitPoints 是最初的血量；assets 提供图片。
     */
    public Sprite(String kind, int center, int bottom, int lane, int hitPoints, Assets assets) {
        name = kind;
        animation = kind;
        animationStart = System.currentTimeMillis();
        row = lane;
        health = hitPoints;
        BufferedImage image = picture(assets, 0);
        x = center - image.getWidth() / 2.0;
        y = bottom - image.getHeight();
    }

    /**
     * 取得当前应该画哪一帧。
     *
     * 参数：assets 提供图片；time 是当前时刻。
     * 返回：缩放之后的图片。
     */
    public BufferedImage picture(Assets assets, long time) {
        return assets.sprite(animation, currentFrameIndex(time), scale);
    }

    /**
     * 算出身体在屏幕上的矩形范围，用于碰撞检测和对齐。
     *
     * 注意这里用的是"真正看得见"的那块区域，而不是整张图。
     * 动图每帧周围都留了大片透明边距，用整张图的话僵尸会隔着
     * 一段空气就咬到植物，子弹也会打空气命中。
     * 另外取的是整段动画所有帧的总范围，不随帧变化，
     * 否则僵尸啃食时碰撞盒一伸一缩，会反复在"啃"和"走"之间切换而抖动。
     *
     * 参数：assets 提供图片；time 是当前时刻（范围不随帧变化，保留参数方便调用）。
     * 返回：身体实际占用的矩形。
     */
    public Rectangle bounds(Assets assets, long time) {
        return placeBox(assets.animationBounds(animation));
    }

    /**
     * 把图片内的可见范围摆到精灵当前位置，得到屏幕上的矩形。
     *
     * 参数：visible 是未缩放的左、上、右、下边界。
     * 返回：屏幕坐标下的矩形。
     */
    protected Rectangle placeBox(int[] visible) {
        int visibleLeft = visible[0];
        int visibleTop = visible[1];
        int visibleRight = visible[2];
        int visibleBottom = visible[3];

        // visible 里存的是未缩放时的边界，所以有缩放时要按比例换算。
        double width = (visibleRight - visibleLeft) * scale;
        double height = (visibleBottom - visibleTop) * scale;
        // 图片内的边界加上精灵自身的位置，就是屏幕上的位置。
        double left = x + visibleLeft * scale;
        double top = y + visibleTop * scale;

        // 宽高至少留 1 像素，防止缩得太小时矩形变成空的，永远碰不到东西。
        int boxWidth = Math.max(1, (int) width);
        int boxHeight = Math.max(1, (int) height);
        return new Rectangle((int) left, (int) top, boxWidth, boxHeight);
    }

    /**
     * 算出当前应该播到第几帧。
     *
     * 参数：time 是当前时刻。
     * 返回：帧号，从 0 开始。
     */
    protected int currentFrameIndex(long time) {
        if (interval > 0 && time >= animationStart) {
            return (int) ((time - animationStart) / interval);
        }
        return 0;
    }

    /**
     * 换一个动画，同时保持身体的底边和中心不动。
     *
     * 同一角色的动图一般画在同样大小的画布上，人物位置已经对好，
     * 这种情况直接沿用原来的坐标就不会跳动；画布大小不同时，
     * 就把新图"看得见的部分"的底边中央对齐到旧身体的底边中央。
     *
     * 参数：next 是新动画名；assets 提供图片；time 是当前时刻。
     */
    public void change(String next, Assets assets, long time) {
        // 第一步：换动画之前，先记下旧身体在屏幕上的位置和旧图的大小。
        Rectangle oldBody = bounds(assets, time);
        BufferedImage oldImage = picture(assets, time);

        // 第二步：换成新动画，并让它从第 0 帧开始播。
        animation = next;
        animationStart = time;
        BufferedImage newImage = picture(assets, time);

        // 第三步：新旧画布一样大，说明人物位置本来就对好了，坐标不用动。
        boolean sameWidth = newImage.getWidth() == oldImage.getWidth();
        boolean sameHeight = newImage.getHeight() == oldImage.getHeight();
        if (sameWidth && sameHeight) {
            return;
        }

        // 第四步：画布大小不同，就让新图看得见部分的底边中央，对准旧身体的底边中央。
        int[] visible = assets.animationBounds(animation);
        int visibleLeft = visible[0];
        int visibleRight = visible[2];
        int visibleBottom = visible[3];
        // 新图里身体中线离图片左边有多远（已按缩放换算）。
        double centerInImage = (visibleLeft + visibleRight) / 2.0 * scale;
        // 新图里身体底边离图片上边有多远（已按缩放换算）。
        double bottomInImage = visibleBottom * scale;
        x = oldBody.getCenterX() - centerInImage;
        y = oldBody.getMaxY() - bottomInImage;
    }

    /** 把当前帧画到界面上。子类可以覆盖它来改变画法。 */
    public void draw(Graphics2D painter, Assets assets, long time) {
        BufferedImage image = picture(assets, time);
        painter.drawImage(image, (int) x, (int) y, null);
    }

    /**
     * 算出用于碰撞检测的矩形，和 bounds 相同：整段动画看得见的总范围。
     *
     * 参数：assets 提供图片；time 是当前时刻。
     * 返回：把这个精灵的身体范围摆到它所在位置之后得到的矩形。
     */
    public Rectangle collisionBox(Assets assets, long time) {
        return bounds(assets, time);
    }

    /**
     * 判断两个精灵有没有碰上。
     *
     * 判定方式是"两个碰撞盒有没有重叠"。没有用圆形近似（半径取对角线的一半），
     * 是因为僵尸又高又窄，那个圆会被身高撑大，僵尸还没走到植物跟前就判定啃上了。
     * 用矩形相交，判定范围就等于身体实际占的地方。
     *
     * 参数：first 和 second 是要比较的两个精灵；assets 提供图片；time 是当前时刻。
     * 返回：碰上了返回真。
     */
    public static boolean touches(Sprite first, Sprite second, Assets assets, long time) {
        Rectangle firstBox = first.collisionBox(assets, time);
        Rectangle secondBox = second.collisionBox(assets, time);
        return firstBox.intersects(secondBox);
    }
}
