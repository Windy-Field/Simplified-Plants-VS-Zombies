package pvz.world;

/**
 * 存放整个游戏的坐标和时间常量。
 *
 * 如果把 80、100、35 这样的数字直接写在逻辑里，读者看不出它们代表什么，
 * 改一处漏一处还会出难查的错误。所以都集中到这里，起个名字再用。
 */
public final class Layout {
    /** 窗口宽度。草坪背景图比窗口宽，绘制时只截取其中 800 像素宽的一段。 */
    public static final int WINDOW_WIDTH = 800;

    /** 窗口高度，和背景图的高度一致（都是 600 像素）。 */
    public static final int WINDOW_HEIGHT = 600;

    /** 草坪一共 5 行。 */
    public static final int ROW_COUNT = 5;

    /** 草坪一共 9 列。 */
    public static final int COLUMN_COUNT = 9;

    /** 草坪左边界：第 0 列的左沿。 */
    public static final int GRID_LEFT = 35;

    /** 草坪上边界：第 0 行的上沿。 */
    public static final int GRID_TOP = 100;

    /**
     * 背景图上草坪第 0 行真正的上沿。
     *
     * 背景里每一行草皮的实际范围是 80+100×行号 到 180+100×行号，
     * 比 GRID_TOP 高 20 像素。判断鼠标点在哪一行必须用这个值，
     * 否则点在某格上部时植物会种到上一行去。
     */
    public static final int LAWN_TOP = 80;

    /** 每个格子的宽度。 */
    public static final int CELL_WIDTH = 80;

    /** 每个格子的高度。 */
    public static final int CELL_HEIGHT = 100;

    /** 植物绘制时相对格子中心向右偏移的像素。 */
    public static final int PLANT_CENTER_OFFSET = 40;

    /** 植物绘制时相对格子上沿向下偏移的像素。 */
    public static final int PLANT_BOTTOM_OFFSET = 60;

    /**
     * 大嘴花的图要往右挪多少像素。
     * 它的图宽 130 像素，根部却在从左数第 37 像素左右，比正中（65）偏左 28 像素。
     */
    public static final int CHOMPER_ROOT_SHIFT = 28;

    /**
     * 窝瓜的图要往上挪多少像素。
     * 窝瓜的图比其他植物高，用默认的 PLANT_BOTTOM_OFFSET 会让它底部超出格子。
     */
    public static final int SQUASH_VERTICAL_SHIFT = -20;

    /** 僵尸碰撞盒从前面（脸朝的那一边）去掉可见宽度的百分之几，把探出去的脑袋和手臂排除掉。 */
    public static final int ZOMBIE_FRONT_TRIM_PERCENT = 30;

    /** 僵尸碰撞盒从后面去掉可见宽度的百分之几，把甩在身后的脚跟排除掉。 */
    public static final int ZOMBIE_BACK_TRIM_PERCENT = 20;

    // TODO：【选做-1】新增射手植物时，如果嘴的高度和豌豆射手不同，需要在这里加一个常量，
    //                再去 PlantActions.muzzleOffset() 里加对应的 if 分支返回这个常量
    /**
     * 各种射手的嘴（炮管开口）比身体可见范围的上沿低多少像素。
     *
     * 子弹要从嘴里射出来，而嘴长在头的中下部，不是头顶。
     * 以前直接用身体上沿当子弹高度，看起来就像从头顶冒出来，所以要按品种各量一个偏移。
     * 数值是量图片里炮管开口的中心位置得到的。
     */
    public static final int PEA_SHOOTER_MUZZLE_OFFSET = 14;

    /** 寒冰射手的嘴比身体上沿低多少像素。 */
    public static final int SNOW_PEA_MUZZLE_OFFSET = 16;

    /** 双发射手的嘴比身体上沿低多少像素。 */
    public static final int REPEATER_MUZZLE_OFFSET = 17;

    /** 三线射手的嘴比身体上沿低多少像素；它的三根炮管里只有右下那根最靠前。 */
    public static final int THREEPEATER_MUZZLE_OFFSET = 46;

    /** 小喷菇的嘴比身体上沿低多少像素。 */
    public static final int PUFF_SHROOM_MUZZLE_OFFSET = 16;

    /** 胆小菇的嘴比身体上沿低多少像素。 */
    public static final int SCAREDY_SHROOM_MUZZLE_OFFSET = 37;

    /** 天空阳光统一降落在这一列的中心。 */
    public static final int SKY_SUN_COLUMN_CENTER = 75;

    /** 僵尸从屏幕右侧这个横坐标出现。 */
    public static final int ZOMBIE_START_X = 850;

    /** 静态卡槽里第一张卡的右沿横坐标。 */
    public static final int CARD_BAR_START = 32;

    /** 卡槽中相邻两张卡的水平间距。 */
    public static final int CARD_BAR_SPACING = 55;

    /** 卡槽中卡片的纵坐标。 */
    public static final int CARD_BAR_TOP = 8;

    /** 卡槽数量最少也得有 1 张，否则玩家一张卡都带不了。 */
    public static final int MIN_CARD_SLOTS = 1;

    /**
     * 卡槽数量最多 8 张，和原版一致。
     *
     * 卡槽底板的宽度就是窗口宽度，一排正好排得下 8 张缩放后的卡片，
     * 再多就会画到屏幕外面，所以卡槽数量只能往少里调。
     */
    public static final int MAX_CARD_SLOTS = 8;

    /** 关卡没写卡槽数量时用这个值，和原版一致。 */
    public static final int DEFAULT_CARD_SLOTS = MAX_CARD_SLOTS;

    /**
     * 把卡槽数量夹回允许范围。
     *
     * 关卡文件是给人手改的，写个 0 或者 999 都不奇怪，
     * 统一从这里过一道，后面的代码就不用再担心。
     *
     * 参数：value 是想设置的卡槽数量。
     * 返回：夹好之后的卡槽数量。
     */
    public static int clampCardSlots(int value) {
        if (value < MIN_CARD_SLOTS) {
            return MIN_CARD_SLOTS;
        }
        if (value > MAX_CARD_SLOTS) {
            return MAX_CARD_SLOTS;
        }
        return value;
    }

    /** 传送带区域的左边界。 */
    public static final int CONVEYOR_LEFT = 90;

    /** 传送带上卡片的初始横坐标。 */
    public static final int CONVEYOR_CARD_START_X = 601;

    /** 传送带上卡片的纵坐标。 */
    public static final int CONVEYOR_CARD_Y = 6;

    /** 传送带上相邻两张卡的水平间距。 */
    public static final int CONVEYOR_CARD_SPACING = 43;

    /** 传送带第一张卡的横坐标。 */
    public static final int CONVEYOR_FIRST_CARD_X = 98;

    /** 卡片绘制的默认缩放比例。 */
    public static final double CARD_SCALE = 0.78;

    /** 选卡界面里卡片的缩放比例。 */
    public static final double CHOOSER_CARD_SCALE = 0.75;

    /** 选卡界面里卡片区的左边界。 */
    public static final int CHOOSER_LEFT = 22;

    /** 选卡界面里卡片区的上边界。 */
    public static final int CHOOSER_TOP = 130;

    /** 选卡界面里卡片区的列间距。 */
    public static final int CHOOSER_COLUMN_SPACING = 53;

    /** 选卡界面里卡片区的行间距。 */
    public static final int CHOOSER_ROW_SPACING = 74;

    /** 点击种植时，横坐标小于它就算点到右上角卡槽了。 */
    public static final int PLAY_CARD_BAR_BOTTOM = 62;

    /** 一帧最多按多少毫秒推进游戏时钟，防止窗口卡顿后僵尸瞬间冲过去。 */
    public static final long MAX_FRAME_ELAPSED = 50;

    /** 加速按钮的左上角坐标和尺寸。 */
    public static final int SPEED_BUTTON_LEFT = 736;

    public static final int SPEED_BUTTON_TOP = 4;

    public static final int SPEED_BUTTON_WIDTH = 60;

    public static final int SPEED_BUTTON_HEIGHT = 22;

    /** 加速按钮能切换的倍率，按一下就在这几个值之间轮换。 */
    public static final int[] SPEED_CHOICES = {1, 2, 3};

    /**
     * 游戏画面右下角作者水印的位置。
     *
     * 水印画在草坪下方的石路条上，那里本来没有别的东西，不挡视线；
     * 而且每局游戏都看得见，别人截图或录屏时会跟着带上。
     */
    public static final int WATERMARK_RIGHT = 790;

    public static final int WATERMARK_BOTTOM = 592;

    /** 水印文字。 */
    public static final String WATERMARK_TEXT = "Windy-Field / Octorange";

    /** 水印字号。 */
    public static final int WATERMARK_FONT_SIZE = 12;

    /** 通关或失败画面停留的毫秒数。 */
    public static final long ENDING_SCREEN_DURATION = 3000;

    /** 主菜单按钮闪光切换的毫秒数。 */
    public static final long MENU_BLINK_INTERVAL = 200;

    /** 点击冒险模式后等多久进入关卡，用于播放切换动画。 */
    public static final long MENU_START_DELAY = 1300;

    /** 传送带每隔多久送出一张新卡。 */
    public static final long CONVEYOR_CARD_INTERVAL = 6000;

    /** 传送带上卡片逐帧左移一格所需的时间。 */
    public static final long CONVEYOR_CARD_SHIFT_INTERVAL = 60;

    /** 天空每隔多久掉一颗阳光。 */
    public static final long SKY_SUN_INTERVAL = 7000;

    /** 天空掉阳光的最短间隔。太小会变成几乎每帧掉一颗，所以游戏和编辑器都按它兜底。 */
    public static final long MIN_SKY_SUN_INTERVAL = 500;

    /** 阳光落地后停留多久消失。 */
    public static final long SUN_STAY_DURATION = 7000;

    /** 子弹爆炸图播放多久后移除。 */
    public static final long BULLET_EXPLODE_DURATION = 500;

    /** 向日葵和阳光菇第一次产阳光前的等待时间。 */
    public static final long SUN_PRODUCE_FIRST_DELAY = 6000;

    /** 向日葵和阳光菇两次产阳光之间的间隔。 */
    public static final long SUN_PRODUCE_INTERVAL = 22000;

    /** 阳光菇长成大阳光菇所需的毫秒数。 */
    public static final long SUN_SHROOM_GROW_TIME = 25000;

    /** 普通射手两次射击之间的间隔。 */
    public static final long SHOOT_INTERVAL = 2000;

    /** 小喷菇和小喷菇类射手两次射击之间的间隔。 */
    public static final long SHROOM_SHOOT_INTERVAL = 3000;

    /** 地刺每次扎伤僵尸之后的间隔。 */
    public static final long SPIKEWEED_DAMAGE_INTERVAL = 2000;

    /** 土豆雷埋好后需要多久才能出土。 */
    public static final long POTATO_MINE_ARM_TIME = 15000;

    /** 僵尸吃植物的间隔倍率：减速状态下乘 2。 */
    public static final long ZOMBIE_ATTACK_INTERVAL = 1000;

    /** 僵尸走一步的间隔倍率：减速状态下乘 2。 */
    public static final long ZOMBIE_STEP_INTERVAL = 70;

    /** 僵尸掉了头之后，每隔多久流掉一点血。 */
    public static final long ZOMBIE_BLEED_INTERVAL = 1000;

    /** 寒冰射手让僵尸减速持续的毫秒数。 */
    public static final long ZOMBIE_SLOW_DURATION = 2000;

    /** 寒冰菇让全场僵尸冻结的毫秒数。 */
    public static final long ZOMBIE_FREEZE_DURATION = 7500;

    /** 定格动画帧的默认真实间隔，单位毫秒。 */
    public static final long DEFAULT_ANIMATION_INTERVAL = 100;

    /** 僵尸动画帧的默认真实间隔，比植物慢一些。 */
    public static final long ZOMBIE_ANIMATION_INTERVAL = 150;

    /** 僵尸攻击动画帧的间隔。 */
    public static final long ZOMBIE_ATTACK_ANIMATION_INTERVAL = 100;

    /** 僵尸死亡动画帧的间隔。 */
    public static final long ZOMBIE_DIE_ANIMATION_INTERVAL = 200;

    /** 窝瓜压下去之后多久砸中目标。 */
    public static final long SQUASH_HIT_DELAY = 1300;

    /** 食人花咬住僵尸之后多久吞下去。 */
    public static final long CHOMPER_SWALLOW_DELAY = 1000;

    /** 食人花消化一只要多久，之后才能再咬。 */
    public static final long CHOMPER_DIGEST_TIME = 16000;

    /** 坚果保龄球滚动时多久移动一次。 */
    public static final long BOWLING_MOVE_INTERVAL = 70;

    /** 坚果保龄球每次移动的像素。 */
    public static final int BOWLING_MOVE_STEP = 13;

    /** 坚果保龄球撞到僵尸后造成伤害的间隔。 */
    public static final long BOWLING_HIT_INTERVAL = 700;

    /** 坚果保龄球每次撞击造成的伤害。 */
    public static final int BOWLING_DAMAGE = 10;

    /** 红坚果保龄球撞到僵尸后多久炸开。 */
    public static final long RED_BOWLING_EXPLODE_DELAY = 500;

    /** 黄油爆米花之类特效图的播放时长。 */
    public static final long SHORT_EFFECT_DURATION = 500;

    /** 这个类只提供常量，不允许创建对象。 */
    private Layout() {
    }

    /** 把鼠标横坐标换算成草坪列号；可能返回负数或越界值，调用方需自行判断。 */
    public static int columnAt(int x) {
        int distanceFromLawn = x - GRID_LEFT;
        // 这里不用普通的 "/"：Java 的整数除法会把 -10 / 80 算成 0，
        // 草坪左边外面一点点也会被当成第 0 列；floorDiv 向下取整，得到 -1，才能被判为越界。
        return Math.floorDiv(distanceFromLawn, CELL_WIDTH);
    }

    /** 把鼠标纵坐标换算成草坪行号；可能返回负数或越界值，调用方需自行判断。 */
    public static int rowAt(int y) {
        int distanceFromLawn = y - LAWN_TOP;
        // 和 columnAt 一样用 floorDiv，草坪上方一点点的位置会得到 -1 而不是 0。
        return Math.floorDiv(distanceFromLawn, CELL_HEIGHT);
    }

    /** 由列号算出该列格子的中心横坐标，用于放置植物。 */
    public static int columnCenter(int column) {
        return GRID_LEFT + column * CELL_WIDTH + PLANT_CENTER_OFFSET;
    }

    /** 由行号算出该行格子的底部纵坐标，用于放置植物。 */
    public static int rowBottom(int row) {
        return GRID_TOP + row * CELL_HEIGHT + PLANT_BOTTOM_OFFSET;
    }

    /** 判断行号和列号是否都落在草坪范围内。 */
    public static boolean insideGrid(int row, int column) {
        if (row < 0 || row >= ROW_COUNT) {
            return false;
        }
        if (column < 0 || column >= COLUMN_COUNT) {
            return false;
        }
        return true;
    }

    /** 原版子弹的横向飞行速度，单位是像素每帧。 */
    public static final int BULLET_SPEED = 4;

    /** 小推车的横向行驶速度，单位是像素每帧。 */
    public static final int CAR_SPEED = 4;

    /** 阳光每帧移动的像素，走得比较慢。 */
    public static final int SUN_SPEED = 1;

    /** 阳光被点击收集后飞向左上角的速度，单位是像素每帧。 */
    public static final double SUN_COLLECT_SPEED = 8.0;
}
