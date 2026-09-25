package pvz.world;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 停在草坪最左边的小推车。
 *
 * 每一行开头都有一辆。僵尸走到头的时候它冲出去，把这一行的僵尸全碾死，
 * 相当于给玩家一次补救机会。
 */
public class Car {
    /** 车头当前的横坐标，一开始在屏幕左边外面。 */
    public int x;

    /** 车轮接触地面的纵坐标。 */
    public int bottom;

    /** 守在哪一行。 */
    public int row;

    /** 是否已经冲出去了。 */
    public boolean moving;

    /** 是否还在场上；冲出屏幕右边后就不再画了。 */
    public boolean alive = true;

    /** 启动的时刻；未启动时为 0。 */
    public long startTime;

    /**
     * 在指定行的最左边放一辆小推车。
     *
     * 参数：lane 是行号，0 是最上面一行。
     */
    public Car(int lane) {
        row = lane;
        x = -25;
        bottom = 180 + lane * Layout.CELL_HEIGHT;
    }

    /**
     * 推进一帧：没启动就原地不动，启动后按时间往右开。
     *
     * 参数：time 是当前游戏时刻。
     */
    public void update(long time) {
        if (moving) {
            if (startTime == 0) {
                startTime = time;
            }
            long elapsed = time - startTime;
            x = -25 + (int) (elapsed * Layout.CAR_SPEED / 16);
        }
        if (x > Layout.WINDOW_WIDTH) {
            alive = false;
        }
    }

    /**
     * 画出小推车。
     *
     * 参数：painter 是画笔；assets 提供 car.png。
     */
    public void draw(Graphics2D painter, Assets assets) {
        BufferedImage image = assets.image("car");
        painter.drawImage(image, x, bottom - image.getHeight(), null);
    }
}
