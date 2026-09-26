package pvz.game;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import pvz.plant.Card;
import pvz.plant.Plant;
import pvz.world.Bullet;
import pvz.world.Car;
import pvz.world.Layout;
import pvz.world.Sprite;
import pvz.world.Sun;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieSpawn;

/**
 * 装着一局游戏里所有会变的东西。
 *
 * Game 只负责"怎么变"，GameRenderer 只负责"怎么画"，
 * 两边都从这个对象里取需要的数据，互相并不认识。
 */
public class GameState {
    /** 卡槽模式：正常选卡，左边有一排固定卡片。 */
    public static final int BAR_NORMAL = 0;

    /** 卡槽模式：传送带，卡片从右边慢慢送过来。 */
    public static final int BAR_CONVEYOR = 1;

    /** 卡槽模式：坚果保龄球，棋盘上直接摆着球。 */
    public static final int BAR_BOWLING = 2;

    /** 当前画面的编号，取值见 GameScreen。 */
    public int screen = GameScreen.MENU;

    /** 当前关卡编号，通关后会加一。 */
    public int levelNumber = 1;

    /** 使用第几张背景图。 */
    public int backgroundIndex;

    /** 本关的卡槽模式。 */
    public int barType;

    /** 当前拥有的阳光数量。 */
    public int sunValue;

    /** 本关天空掉阳光的间隔，由关卡文件决定。 */
    public long skySunInterval = Layout.SKY_SUN_INTERVAL;

    /** 僵尸出场表里下一条该出场的记录的下标。 */
    public int nextSpawnIndex;

    /** 鼠标当前横坐标，用于显示种植预览。 */
    public int mouseX;

    /** 鼠标当前纵坐标。 */
    public int mouseY;

    /** 玩家手里拿着的卡片；没拿就是 null。 */
    public Card heldCard;

    /** 是不是已经点了冒险模式、正在等进入关卡。 */
    public boolean startingMenu;

    /** 当前画面是什么时候开始的，用来计时切换。 */
    public long screenStart;

    /** 本关是什么时候开始的，僵尸出场时间从这里算起。 */
    public long playStart;

    /** 上次掉天空阳光的时刻。 */
    public long lastSkySun;

    /** 上次送出传送带卡片的时刻。 */
    public long lastCardTime;

    /** 当前时刻。战斗中按倍速推进，其他画面按真实时间推进。 */
    public long time;

    /**
     * 相机当前截取到背景图的哪一横坐标。
     *
     * 平时就是 CAMERA_LEFT_OFFSET（房子右侧那片草坪）；
     * 开局演出时会先推到最右再移回来，于是整个草坪跟着平移。
     */
    public int cameraOffset = Layout.CAMERA_LEFT_OFFSET;

    /** 开局演出的起始时刻，用来算镜头推到了哪儿。 */
    public long introStart;

    /**
     * 开局演出是不是已经进到第二段（镜头移回草坪 + 倒计时）。
     *
     * 演出分两段，中间隔着玩家的操作：
     * 正常选卡关卡第一段推完就切到选卡界面，等玩家点开始才进第二段；
     * 传送带和保龄球没有选卡环节，第一段推完立刻进第二段。
     */
    public boolean introReturning;

    /** 第二段的起始时刻，用来算镜头移回到哪儿、倒计时该显示第几张图。 */
    public long introReturnStart;

    /**
     * 当前的选卡界面是不是开局演出里的那一个。
     *
     * 是的话，选卡界面要演"背包从下方升起"，而且升降期间不接受点击；
     * 关卡载入就直接进选卡的老路子（比如自检）不走这个动画。
     */
    public boolean introChooser;

    /**
     * 演出里的选卡界面是不是正在收回去。
     *
     * 玩家点了"开始战斗"就把这个打开：背包先照来路沉回画面下方，
     * 沉完之后才轮到镜头移回草坪。判定为真的这段时间里不再接受点击。
     */
    public boolean introChooserExiting;

    /** 背包开始回收的时刻，用来算它沉到了哪儿。 */
    public long chooserExitStart;

    /**
     * 开局演出里摆出来给玩家看的僵尸。
     *
     * 它们只负责"让玩家知道这关有哪些僵尸"，不参与任何逻辑：
     * 既不移动、不啃植物，也不算通关判定。演出结束就整批清掉。
     * 所以特意和 state.zombies 分开存，免得被僵尸的更新逻辑碰到。
     */
    public final List<Zombie> introZombies = new ArrayList<Zombie>();

    /** 游戏内部速度倍率，2 是新的 1 倍速，3 是新的 1.5 倍速。 */
    public int speedMultiplier = 2;

    /** 上一帧的真实时刻，用来算出这一帧过了多久。 */
    public long lastRealTime;

    /** 本关的背景图。 */
    public BufferedImage background;

    /** 场上所有植物、僵尸、子弹、特效、掉落的僵尸头、阳光、小推车和卡片。 */
    public final List<Plant> plants = new ArrayList<Plant>();
    public final List<Zombie> zombies = new ArrayList<Zombie>();
    public final List<Bullet> bullets = new ArrayList<Bullet>();
    public final List<Sprite> effects = new ArrayList<Sprite>();
    public final List<Sprite> heads = new ArrayList<Sprite>();
    public final List<Sun> suns = new ArrayList<Sun>();
    public final List<Car> cars = new ArrayList<Car>();
    public final List<Card> cards = new ArrayList<Card>();

    /** 本关的僵尸出场表。 */
    public final List<ZombieSpawn> schedule = new ArrayList<ZombieSpawn>();

    /** 选卡界面里已经选中的卡片编号。 */
    public final List<Integer> selected = new ArrayList<Integer>();

    /** 选卡界面正在飞行的卡片动画。 */
    public final List<Card> flyingCards = new ArrayList<Card>();

    /** 传送带和保龄球模式可出的卡片编号。 */
    public final List<Integer> pool = new ArrayList<Integer>();

    /** 本关禁用的植物编号，选卡界面里画成灰色锁定。 */
    public final List<Integer> bannedPlants = new ArrayList<Integer>();

    /** 本关必选的植物编号，进选卡界面时自动飞入卡槽。 */
    public final List<Integer> requiredPlants = new ArrayList<Integer>();

    /**
     * 本关的卡槽数量，也就是选卡界面上最多能带几张卡。
     *
     * 这是个上限：玩家可以少带，但不能超过它。只对正常选卡模式有影响。
     */
    public int maxCards = Layout.DEFAULT_CARD_SLOTS;

    /** 记录草坪上哪些格子已经种了东西。true 表示被占用。 */
    public final boolean[][] occupied = new boolean[Layout.ROW_COUNT][Layout.COLUMN_COUNT];

    /** 清空上一关留下的所有物体和记录，准备重新开始。 */
    public void clearField() {
        plants.clear();
        zombies.clear();
        bullets.clear();
        effects.clear();
        heads.clear();
        suns.clear();
        cars.clear();
        cards.clear();
        schedule.clear();
        selected.clear();
        pool.clear();
        bannedPlants.clear();
        requiredPlants.clear();
        flyingCards.clear();
        // 卡槽数量是每关自己的设置，换关时先退回默认值，免得上一关的小卡槽跟着带过来。
        maxCards = Layout.DEFAULT_CARD_SLOTS;
        nextSpawnIndex = 0;
        heldCard = null;
    }

    /**
     * 判断某张卡片是不是正在飞。
     *
     * 渲染时用它来决定要不要画静态卡片，逻辑判断时用它来防止重复点击。
     *
     * 参数：plantIndex 是植物下标。
     * 返回：只要有一张同种卡片在飞就返回真。
     */
    public boolean isFlying(int plantIndex) {
        for (int position = 0; position < flyingCards.size(); position++) {
            Card card = flyingCards.get(position);
            if (card.index == plantIndex && card.flying) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断某张卡片是不是正在飞往卡槽。
     *
     * 参数：plantIndex 是植物下标。
     * 返回：正在飞向卡槽就返回真。
     */
    public boolean isFlyingToBar(int plantIndex) {
        for (int position = 0; position < flyingCards.size(); position++) {
            Card card = flyingCards.get(position);
            if (card.index == plantIndex && card.flying && card.flyingToBar) {
                return true;
            }
        }
        return false;
    }

    /**
     * 算出一张新选的卡片应该飞到卡槽的第几格。
     *
     * 已经在飞向卡槽的卡片也要占一格，否则两张卡会飞到同一个位置上。
     *
     * 返回：槽位下标，从 0 开始。
     */
    public int nextBarSlot() {
        int slot = selected.size();
        for (int position = 0; position < flyingCards.size(); position++) {
            if (flyingCards.get(position).flyingToBar) {
                slot = slot + 1;
            }
        }
        return slot;
    }
}
