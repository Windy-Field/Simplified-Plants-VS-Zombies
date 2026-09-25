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
    public int nextZombie;

    /** 鼠标当前横坐标，用于显示种植预览。 */
    public int mouseX;

    /** 鼠标当前纵坐标。 */
    public int mouseY;

    /** 玩家手里拿着的卡片；没拿就是 null。 */
    public Card held;

    /** 是不是已经点了冒险模式、正在等进入关卡。 */
    public boolean startingMenu;

    /** 当前画面是什么时候开始的，用来计时切换。 */
    public long screenStart;

    /** 本关是什么时候开始的，僵尸出场时间从这里算起。 */
    public long playStart;

    /** 上次掉天空阳光的时刻。 */
    public long lastSkySun;

    /** 上次送出传送带卡片的时刻。 */
    public long lastCard;

    /** 当前时刻。它不是墙上时间，而是按加速倍率累加出来的游戏时间。 */
    public long time;

    /** 游戏速度倍率，1 是正常速度，2 是两倍速。 */
    public int speedMultiplier = 1;

    /** 上一帧的真实时刻，用来算出这一帧过了多久。 */
    public long lastRealTime;

    /** 本关的背景图。 */
    public BufferedImage background;

    /** 场上所有植物、僵尸、子弹、掉落的僵尸头、阳光、小推车和卡片。 */
    public final List<Plant> plants = new ArrayList<Plant>();
    public final List<Zombie> zombies = new ArrayList<Zombie>();
    public final List<Bullet> bullets = new ArrayList<Bullet>();
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
        nextZombie = 0;
        held = null;
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
