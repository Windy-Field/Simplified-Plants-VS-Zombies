package pvz.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import pvz.plant.Card;
import pvz.plant.Cards;
import pvz.plant.Plant;
import pvz.plant.PlantRules;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.Car;
import pvz.world.Layout;
import pvz.world.Sprite;
import pvz.world.Sun;
import pvz.zombie.Zombie;

/**
 * 负责把当前游戏画面画到屏幕上。
 *
 * 它不改变任何游戏状态，只读取数据然后画。
 * 拆出来的好处是：Game 只关心"游戏规则"，这个类只关心"长什么样"，
 * 想调整画面效果时不用在游戏逻辑里翻找。
 */
public class GameRenderer {
    /** 提供图片素材。 */
    private final Assets assets;

    /**
     * 创建绘图器。
     *
     * 参数：originalAssets 是已经加载好图片的资源对象。
     */
    public GameRenderer(Assets originalAssets) {
        assets = originalAssets;
    }

    /**
     * 按当前状态选择该画哪一个画面。
     *
     * 参数：painter 是画笔；screen 是当前画面编号；time 是当前时刻；
     *       state 里装着要画的所有数据。
     */
    public void draw(Graphics2D painter, int screen, long time, GameState state) {
        // 打开双线性插值，图片缩放之后边缘不会出现锯齿。
        painter.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (screen == GameScreen.MENU) {
            drawMenu(painter, time, state);
        } else if (screen == GameScreen.VICTORY || screen == GameScreen.LOSS) {
            drawEnding(painter, screen);
        } else {
            drawBackground(painter, state);
            if (screen == GameScreen.CHOOSE) {
                drawSelection(painter, time, state);
            } else {
                drawPlay(painter, time, state);
            }
        }
    }

    /**
     * 画主菜单。
     *
     * 菜单图比窗口宽，所以要只截取 x=80 往右的 800 像素，
     * 否则菜单会整体偏左。
     */
    private void drawMenu(Graphics2D painter, long time, GameState state) {
        BufferedImage menu = assets.image("MainMenu");
        painter.drawImage(menu, 0, 0, Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT,
            80, 0, 880, Layout.WINDOW_HEIGHT, null);

        // 点了冒险模式之后按钮会一闪一闪，两张图交替显示做成的。
        String option = "Adventure_0";
        if (state.startingMenu) {
            long elapsed = time - state.screenStart;
            long phase = elapsed / Layout.MENU_BLINK_INTERVAL;
            if (phase % 2 == 1) {
                option = "Adventure_1";
            }
        }
        BufferedImage button = assets.image(option);
        painter.drawImage(button, 435, 75, 715, 206, 0, 0, 165, 70, null);
    }

    /** 画胜利或失败那张整屏图片。 */
    private void drawEnding(Graphics2D painter, int screen) {
        String name = "GameLoose";
        if (screen == GameScreen.VICTORY) {
            name = "GameVictory";
        }
        BufferedImage image = assets.image(name);
        painter.drawImage(image, 0, 0, Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT,
            0, 0, Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT, null);
    }

    /** 画草坪背景；背景图比窗口宽，只截取房子右侧草坪所在的那一段。 */
    private void drawBackground(Graphics2D painter, GameState state) {
        int offset = 220;
        painter.drawImage(state.background, 0, 0, Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT,
            offset, 0, offset + Layout.WINDOW_WIDTH, Layout.WINDOW_HEIGHT, null);
    }

    /** 画选卡界面：底板、候选卡、已选的卡和开始按钮。 */
    private void drawSelection(Graphics2D painter, long time, GameState state) {
        painter.drawImage(assets.image("ChooserBackground"), 0, 0, null);
        painter.drawImage(assets.image("PanelBackground"), 0, 87, null);
        drawSunNumber(painter, state.sunValue, 21, 66);

        // 候选卡按 8 张一行往下排；现在是 17 张，所以第 17 张单独占第三行第一个。
        // 已选中或正在飞行（飞往卡槽、飞回候选区）的卡，在原位显示为灰色锁定状态。
        // 提示：选卡界面最多能放 24 张卡（3 行），超过需要改布局。
        for (int index = 0; index < Cards.CHOOSER_CARD_COUNT; index++) {
            int column = index % 8;
            int row = index / 8;
            int left = Layout.CHOOSER_LEFT + column * Layout.CHOOSER_COLUMN_SPACING;
            int top = Layout.CHOOSER_TOP + row * Layout.CHOOSER_ROW_SPACING;
            Card card = new Card(index, left, top);
            boolean available = !state.selected.contains(Integer.valueOf(index))
                && !state.isFlying(index)
                && !state.bannedPlants.contains(Integer.valueOf(index));
            // 选卡阶段不显示冷却，所以传一个很大的阳光数量。
            card.draw(painter, assets, time, Integer.MAX_VALUE, Layout.CHOOSER_CARD_SCALE, available);
        }

        // 卡槽里的卡：正在飞的已经由下面统一画了，这里跳过。
        for (int position = 0; position < state.selected.size(); position++) {
            int plantIndex = state.selected.get(position).intValue();
            if (state.isFlying(plantIndex)) {
                continue;
            }
            int left = 78 + position * Layout.CARD_BAR_SPACING;
            Card card = new Card(plantIndex, left, Layout.CARD_BAR_TOP);
            card.draw(painter, assets, time, Integer.MAX_VALUE, Layout.CARD_SCALE, true);
        }

        // 飞行中的卡片最后画，保证盖在其它卡上面。
        for (int position = 0; position < state.flyingCards.size(); position++) {
            Card card = state.flyingCards.get(position);
            card.draw(painter, assets, time, Integer.MAX_VALUE, Layout.CARD_SCALE, true);
        }

        if (state.selected.size() == state.maxCards) {
            painter.drawImage(assets.image("StartButton"), 155, 547, null);
        }
    }

    /** 画游戏中的画面：卡槽、植物、僵尸、子弹、小推车、僵尸头和阳光。 */
    private void drawPlay(Graphics2D painter, long time, GameState state) {
        if (state.barType == GameState.BAR_NORMAL) {
            painter.drawImage(assets.image("ChooserBackground"), 10, 0, null);
            drawSunNumber(painter, state.sunValue, 31, 66);
        } else {
            painter.drawImage(assets.image("MoveBackground"), Layout.CONVEYOR_LEFT, 0, null);
        }

        for (Card card : state.cards) {
            card.draw(painter, assets, time, state.sunValue, Layout.CARD_SCALE, true);
        }

        // 按行绘制，这样同一行的东西前后顺序才正确（植物在僵尸后面）。
        for (int row = 0; row < Layout.ROW_COUNT; row++) {
            drawRowPlants(painter, time, state, row);
            drawRowZombies(painter, time, state, row);
            drawRowBullets(painter, time, state, row);
        }

        for (Car car : state.cars) {
            if (car.alive) {
                car.draw(painter, assets);
            }
        }
        for (Sprite head : state.heads) {
            if (head.alive) {
                head.draw(painter, assets, time);
            }
        }
        for (Sun sun : state.suns) {
            if (sun.alive) {
                sun.draw(painter, assets, time);
            }
        }
        drawPreview(painter, time, state);
        drawSpeedButton(painter, state);
        drawWatermark(painter);
    }

    /**
     * 在右下角画作者水印。
     *
     * 画在草坪下方的石路条上，那里本来没有别的东西，所以不挡视线。
     * 用半透明的浅色，位置和文字都写在 Layout 里，改起来只有一处。
     *
     * 参数：painter 是画笔。
     */
    private void drawWatermark(Graphics2D painter) {
        painter.setFont(new Font("SansSerif", Font.BOLD, Layout.WATERMARK_FONT_SIZE));
        // 半透明的浅灰：看得见，但不会抢走游戏画面的注意力。
        painter.setColor(new Color(255, 255, 255, 130));

        // 右对齐：先量出文字宽度，再从右边界往左退这么多。
        int textWidth = painter.getFontMetrics().stringWidth(Layout.WATERMARK_TEXT);
        int textX = Layout.WATERMARK_RIGHT - textWidth;
        painter.drawString(Layout.WATERMARK_TEXT, textX, Layout.WATERMARK_BOTTOM);
    }

    /**
     * 画右上角的加速按钮。
     *
     * 按钮用纯色矩形加文字，不依赖外部素材，所以不会因为缺图而出错。
     * 非 1 倍速时按钮变色，让玩家一眼看出现在是加速状态。
     */
    private void drawSpeedButton(Graphics2D painter, GameState state) {
        int left = Layout.SPEED_BUTTON_LEFT;
        int top = Layout.SPEED_BUTTON_TOP;
        int width = Layout.SPEED_BUTTON_WIDTH;
        int height = Layout.SPEED_BUTTON_HEIGHT;

        Color face = new Color(60, 60, 100);
        if (state.speedMultiplier > 1) {
            face = new Color(190, 60, 45);
        }
        painter.setColor(face);
        painter.fillRect(left, top, width, height);
        painter.setColor(new Color(234, 233, 171));
        painter.drawRect(left, top, width - 1, height - 1);

        String text = state.speedMultiplier + "x";
        painter.setFont(new Font("SansSerif", Font.BOLD, 14));
        int textWidth = painter.getFontMetrics().stringWidth(text);
        int textX = left + (width - textWidth) / 2;
        int textY = top + height - 6;
        painter.drawString(text, textX, textY);
    }

    /** 画某一行的植物。 */
    private void drawRowPlants(Graphics2D painter, long time, GameState state, int row) {
        for (Plant plant : state.plants) {
            if (plant.alive && plant.row == row) {
                drawPlant(painter, time, plant);
            }
        }
    }

    /** 画某一行的僵尸；被冻住的僵尸脚下要加一层冰。 */
    private void drawRowZombies(Graphics2D painter, long time, GameState state, int row) {
        for (Zombie zombie : state.zombies) {
            if (!zombie.alive || zombie.row != row) {
                continue;
            }
            zombie.draw(painter, assets, time);
            if (time < zombie.frozenUntil) {
                BufferedImage trap = assets.sprite("IceShroomTrap", 0, 1);
                Rectangle bounds = zombie.bounds(assets, time);
                int left = (int) bounds.getCenterX() - trap.getWidth() / 2;
                int top = (int) bounds.getMaxY() - trap.getHeight();
                painter.drawImage(trap, left, top, null);
            }
        }
    }

    /** 画某一行的子弹。 */
    private void drawRowBullets(Graphics2D painter, long time, GameState state, int row) {
        for (Bullet bullet : state.bullets) {
            if (bullet.alive && bullet.row == row) {
                bullet.draw(painter, assets, time);
            }
        }
    }

    /**
     * 画单株植物。
     *
     * 有两种植物不用普通画法：樱桃炸弹用一张专门的爆炸图；
     * 保龄球要一边滚一边转，得先旋转画布。
     */
    // TODO：【选做-4】新增植物时需要使用特殊画法
    private void drawPlant(Graphics2D painter, long time, Plant plant) {
        if (plant.name.equals("CherryBomb") && plant.triggered) {
            drawCherryBoom(painter, time, plant);
            return;
        }
        if (isRollingBowling(plant)) {
            drawRollingBowling(painter, time, plant);
            return;
        }
        plant.draw(painter, assets, time);
    }

    /** 画樱桃炸弹爆炸：把 Boom 图对准植物中心。 */
    private void drawCherryBoom(Graphics2D painter, long time, Plant plant) {
        BufferedImage image = assets.image("Boom");
        Rectangle bounds = plant.bounds(assets, time);
        int left = (int) bounds.getCenterX() - image.getWidth() / 2;
        int top = (int) bounds.getCenterY() - image.getHeight() / 2;
        painter.drawImage(image, left, top, null);
    }

    /** 判断这株保龄球是不是正处于滚动状态（红坚果炸之前也在地上滚）。 */
    private boolean isRollingBowling(Plant plant) {
        if (!PlantRules.isBowling(plant.name)) {
            return false;
        }
        if (plant.name.equals("RedWallNutBowling") && plant.triggered) {
            return false;
        }
        return true;
    }

    /** 画滚动的坚果保龄球：按已经滚过的帧数算出旋转角度。 */
    private void drawRollingBowling(Graphics2D painter, long time, Plant plant) {
        BufferedImage image = plant.picture(assets, time);
        double centerX = plant.x + image.getWidth() / 2.0;
        double centerY = plant.y + image.getHeight() / 2.0;

        // 每滚一帧转 30 度，看起来就像在地上滚。
        // Java 的纵坐标向下，所以正角度在屏幕上看是顺时针；球往右滚就该顺时针转。
        long frames = (time - plant.placed) / Layout.BOWLING_MOVE_INTERVAL;
        double angle = Math.toRadians(30 * frames);

        Graphics2D rotated = (Graphics2D) painter.create();
        rotated.rotate(angle, centerX, centerY);
        rotated.drawImage(image, (int) plant.x, (int) plant.y, null);
        rotated.dispose();
    }

    /** 画阳光数量：先铺一块浅色底，再把数字靠右写上去。 */
    private void drawSunNumber(Graphics2D painter, int amount, int x, int y) {
        painter.setColor(new Color(234, 233, 171));
        painter.fillRect(x, y, 32, 17);
        painter.setColor(new Color(60, 60, 100));
        painter.setFont(new Font("SansSerif", Font.PLAIN, 19));
        String text = Integer.toString(amount);
        int width = painter.getFontMetrics().stringWidth(text);
        painter.drawString(text, x + 32 - width, y + 15);
    }

    /** 手里拿着卡片时，同时画出半透明的落点提示和跟着鼠标的图。 */
    private void drawPreview(Graphics2D painter, long time, GameState state) {
        Card held = state.held;
        if (held == null) {
            return;
        }

        String name = Cards.PLANTS[held.index];
        BufferedImage preview = assets.sprite(name, 0, 1);

        int column = Layout.columnAt(state.mouseX);
        int row = Layout.rowAt(state.mouseY);
        boolean landable = Layout.insideGrid(row, column);
        if (state.mouseX < Layout.GRID_LEFT || state.mouseY < Layout.LAWN_TOP) {
            landable = false;
        }
        if (landable && state.occupied[row][column]) {
            landable = false;
        }

        if (landable) {
            // 落点预览和真正种下去的位置必须用同一套算法，否则预览和实物会错开。
            int center = Layout.columnCenter(column);
            int bottom = Layout.rowBottom(row);
            int left = center - preview.getWidth() / 2 + Plant.rootShift(name);
            int top = bottom - preview.getHeight();
            painter.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            painter.drawImage(preview, left, top, null);
            painter.setComposite(AlphaComposite.SrcOver);
        }

        painter.drawImage(preview, state.mouseX - preview.getWidth() / 2,
            state.mouseY - preview.getHeight() / 2, null);
    }
}
