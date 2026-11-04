package pvz.game;

import pvz.plant.PlantActionType;
import pvz.plant.PlantCatalog;
import pvz.plant.PlantDefinition;
import pvz.world.Layout;
import pvz.zombie.ZombieCatalog;
import pvz.zombie.ZombieDefinition;

/** 只保存图鉴的中文名称与能力介绍，数值从游戏资料读取，避免调整平衡后介绍仍显示旧数据 */
public final class AlmanacDescriptions {
    private static final String[][] PLANTS = {
        {"SunFlower", "向日葵", "持续生产阳光，为后续种植提供资源"},
        {"TwinSunflower", "双子向日葵", "每次同时生产两颗阳光，适合需要更多资源的防线"},
        {"Peashooter", "豌豆射手", "向同一行的前方僵尸发射豌豆，是基础远程攻击植物"},
        {"SnowPea", "寒冰射手", "发射寒冰豌豆，命中后降低僵尸移动和攻击速度"},
        {"WallNut", "坚果", "用较高血量阻挡僵尸，受伤后会出现裂纹"},
        {"CherryBomb", "樱桃炸弹", "准备后炸伤周围区域的敌人，使用一次后消失"},
        {"Torchwood", "火炬树桩", "经过的普通豌豆变为火焰豌豆，冰豌豆变回普通豌豆并失去减速"},
        {"Threepeater", "三线射手", "向所在行及相邻两行各发射一颗豌豆，可以照顾多条防线"},
        {"RepeaterPea", "双发射手", "每次向同一行连续发射两颗普通豌豆"},
        {"Chomper", "大嘴花", "吞下前方近处的僵尸后消化，目标直接消失，但不能吞掉巨人"},
        {"PuffShroom", "小喷菇", "免费种植，向较近的敌人发射蘑菇子弹，白天会睡觉"},
        {"PotatoMine", "土豆雷", "种下后需要准备时间，出土后接触僵尸引爆"},
        {"Squash", "窝瓜", "发现近处敌人后跳起砸下，优先锁定距离最近的目标"},
        {"Spikeweed", "地刺", "伤害踩在上面的僵尸，普通僵尸无法啃食它"},
        {"Jalapeno", "火爆辣椒", "准备后清除一整行的普通敌人，对巨人造成一次性攻击伤害"},
        {"ScaredyShroom", "胆小菇", "远程发射蘑菇子弹，僵尸靠近时会害怕而停止射击"},
        {"SunShroom", "阳光菇", "先生产小阳光，长大后生产大阳光，白天会睡觉"},
        {"IceShroom", "寒冰菇", "发动后冻结场上的敌方僵尸，白天会睡觉"},
        {"HypnoShroom", "魅惑菇", "清醒时被僵尸吃掉会改变对方阵营，被巨人砸毁不会触发魅惑"},
        {"WallNutBowling", "坚果保龄球", "保龄球关卡专用，向前滚动撞伤僵尸，命中后在行间弹跳"},
        {"RedWallNutBowling", "爆炸坚果保龄球", "保龄球关卡专用，接触僵尸后爆炸，波及附近敌人"}
    };
    private static final String[][] ZOMBIES = {
        {"Zombie", "没有防具，沿所在行前进，接触植物后啃食"},
        {"ConeheadZombie", "路障提供额外血量，路障被打掉后露出普通僵尸本体"},
        {"BucketheadZombie", "铁桶提供很高的防护，需要集中火力才能击倒"},
        {"FlagZombie", "手持旗帜前进，血量与普通僵尸相同"},
        {"NewspaperZombie", "报纸提供额外保护，失去报纸后移动速度加快"},
        {"JokerZombie", "接触植物或被击倒时先开盒，再爆炸，被大嘴花吞下则不会爆炸"},
        {"FootballZombie", "防具坚固，移动速度较快，失去防具后继续攻击"},
        {"Gargantuar", "挥棒一击砸毁植物，大嘴花无法吞下，小推车可以直接击倒"},
        {"Imp", "体型较小、移动较快，可以独立出场，也可以被巨人投到防线后方"}
    };

    /** 此类只提供资料查询，避免为每一局重复创建介绍对象 */
    private AlmanacDescriptions() {
    }

    /** 参数 index 是植物编号，返回按内部名称查到的中文名，顺序变化时也能对应正确 */
    public static String plantLabel(int index) {
        String name = PlantCatalog.nameAt(index);
        return findText(PLANTS, name, 1);
    }

    /** 参数 index 是植物编号，返回能力说明；伤害和时长直接取当前游戏常量 */
    public static String plantDescription(int index) {
        PlantDefinition plant = PlantCatalog.definitionAt(index);
        String text = findText(PLANTS, plant.name, 2);
        if (plant.actionType == PlantActionType.SHOOTER) {
            text = text + "。每颗子弹伤害 " + Layout.BULLET_DAMAGE;
        }
        if (plant.name.equals("Torchwood")) {
            text = text + "。火焰豌豆伤害 " + Layout.FIRE_PEA_DAMAGE;
        } else if (plant.name.equals("Chomper")) {
            text = text + "。消化等待 " + Layout.CHOMPER_DIGEST_TIME / 1000 + " 秒";
        } else if (plant.name.equals("PotatoMine")) {
            text = text + "。准备时间 " + Layout.POTATO_MINE_ARM_TIME / 1000 + " 秒";
        } else if (plant.name.equals("Spikeweed")) {
            text = text + "。每次伤害 " + Layout.SPIKEWEED_DAMAGE;
        } else if (plant.name.equals("IceShroom")) {
            text = text + "。冻结持续 " + Layout.ZOMBIE_FREEZE_DURATION / 1000.0 + " 秒";
        } else if (plant.name.equals("WallNutBowling")) {
            text = text + "。每次撞击伤害 " + Layout.BOWLING_DAMAGE;
        }
        return text;
    }

    /** 参数 entries 是资料表，name 是内部名，column 是资料列；按名称查找，避免调整顺序后介绍错位 */
    private static String findText(String[][] entries, String name, int column) {
        for (String[] entry : entries) {
            if (entry[0].equals(name)) {
                return entry[column];
            }
        }
        throw new IllegalArgumentException("图鉴缺少说明：" + name);
    }

    /** 参数 index 是僵尸编号，返回中文名；与编辑器共用同一份名称资料 */
    public static String zombieLabel(int index) {
        return ZombieCatalog.LABELS[index];
    }

    /** 参数 index 是僵尸编号，返回能力说明，巨人投掷条件跟随血量常量更新 */
    public static String zombieDescription(int index) {
        ZombieDefinition zombie = ZombieCatalog.DEFINITIONS[index];
        String text = findText(ZOMBIES, zombie.name, 1);
        if (zombie.name.equals("Gargantuar")) {
            text = text + "。剩余 " + zombie.maxHealth / 2 + " 血时投一次小鬼";
        }
        return text;
    }
}
