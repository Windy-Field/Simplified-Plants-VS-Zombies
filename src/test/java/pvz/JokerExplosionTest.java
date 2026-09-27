package pvz;

import java.nio.file.Path;
import pvz.game.GameState;
import pvz.plant.Plant;
import pvz.world.Assets;
import pvz.world.CombatValues;
import pvz.world.Layout;
import pvz.world.Sprite;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieEffects;

/**
 * 小丑爆炸的专项回归测试。
 *
 * 主自检只检查通用流程，这里单独验证九格范围、死亡爆炸和阵营伤害。
 */
public class JokerExplosionTest {
    /**
     * 运行小丑爆炸测试。
     *
     * 参数：arguments[0] 是素材目录；省略时使用当前目录下的 assets。
     */
    public static void main(String[] arguments) throws Exception {
        Path root = Path.of("assets");
        if (arguments.length > 0) {
            root = Path.of(arguments[0]);
        }
        Assets assets = new Assets(root);
        checkContactExplosion(assets);
        checkDeathExplosion(assets);
        System.out.println("PASS：小丑爆炸范围和死亡效果检查通过");
    }

    /**
     * 检查刚碰到植物边缘时，爆炸以被碰植物为中心。
     *
     * 参数：assets 提供植物和小丑的动画。
     */
    private static void checkContactExplosion(Assets assets) {
        GameState state = createField(assets);
        Plant touchedPlant = findPlant(state, 2, 4);
        Zombie joker = new Zombie("JokerZombie", 2, Layout.rowBottom(2), assets);
        placeZombieAtColumnLeft(joker, 5, assets, state.time);
        state.zombies.add(joker);

        int zombieColumn = columnOfZombie(joker, assets, state.time);
        check(zombieColumn == 5, "小丑未处于目标植物右侧一列");
        check(Sprite.touches(joker, touchedPlant, assets, state.time),
            "小丑未碰到目标植物：植物 "
                + touchedPlant.collisionBox(assets, state.time)
                + "，小丑 " + joker.collisionBox(assets, state.time));

        ZombieEffects.die(joker, assets, state, state.time, false, touchedPlant);
        checkPlants(state, 2, 4);
        checkZombies(state);
        check("JokerZombieExplode".equals(joker.animation),
            "小丑未播放专用死亡动画");
        int effectCount = state.effects.size();
        ZombieEffects.die(joker, assets, state, state.time, false, touchedPlant);
        check(state.effects.size() == effectCount, "同一只小丑重复爆炸");
    }

    /**
     * 检查小丑不接触植物而死亡时，以自己的所在格爆炸。
     *
     * 参数：assets 提供植物和小丑的动画。
     */
    private static void checkDeathExplosion(Assets assets) {
        GameState state = createField(assets);
        Zombie joker = new Zombie("JokerZombie", 2, Layout.rowBottom(2), assets);
        double currentCenter = joker.collisionBox(assets, state.time).getCenterX();
        joker.x = joker.x + Layout.columnCenter(4) - currentCenter;
        state.zombies.add(joker);

        ZombieEffects.die(joker, assets, state, state.time, false);
        checkPlants(state, 2, 4);
        checkZombies(state);
    }

    /**
     * 摆出五行五列植物，并在爆炸范围内外各放置不同阵营僵尸。
     *
     * 参数：assets 提供实体图片。
     * 返回：布置好的游戏状态。
     */
    private static GameState createField(Assets assets) {
        GameState state = new GameState();
        state.time = 1000;

        for (int row = 0; row < Layout.ROW_COUNT; row++) {
            for (int column = 2; column <= 6; column++) {
                String kind = "SunFlower";
                if (row == 2 && column == 4) {
                    kind = "WallNut";
                }
                Plant plant = new Plant(kind, Layout.columnCenter(column),
                    Layout.rowBottom(row), row, column, assets, state.time, false);
                state.plants.add(plant);
                state.occupied[row][column] = true;
            }
        }

        addZombie(state, assets, 1, 3, true);
        addZombie(state, assets, 3, 5, true);
        addZombie(state, assets, 2, 4, false);
        addZombie(state, assets, 0, 2, true);
        return state;
    }

    /**
     * 向测试场地添加一个指定阵营的僵尸，并将其身体中心对准格子中心。
     *
     * 参数：state 是测试场地；assets 提供图片；row、column 是格子；
     * hypnotized 表示是否被魅惑。
     */
    private static void addZombie(GameState state, Assets assets, int row,
            int column, boolean hypnotized) {
        Zombie zombie = new Zombie("Zombie", row, Layout.rowBottom(row), assets);
        double currentCenter = zombie.collisionBox(assets, state.time).getCenterX();
        zombie.x = zombie.x + Layout.columnCenter(column) - currentCenter;
        zombie.hypno = hypnotized;
        state.zombies.add(zombie);
    }

    /**
     * 按格子寻找测试植物。
     *
     * 参数：state 是测试场地；row、column 是目标格子。
     * 返回：对应的植物，缺失时抛出测试错误。
     */
    private static Plant findPlant(GameState state, int row, int column) {
        for (Plant plant : state.plants) {
            if (plant.row == row && plant.column == column) {
                return plant;
            }
        }
        throw new AssertionError("测试场地缺少目标植物");
    }

    /**
     * 将僵尸身体中心放到指定列的左边界，用来模拟边缘接触。
     *
     * 参数：zombie 是要移动的僵尸；column 是目标列；assets 提供图片；
     * time 是当前测试时刻。
     */
    private static void placeZombieAtColumnLeft(Zombie zombie, int column,
            Assets assets, long time) {
        double currentCenter = zombie.collisionBox(assets, time).getCenterX();
        int targetCenter = Layout.GRID_LEFT + column * Layout.CELL_WIDTH + 2;
        zombie.x = zombie.x + targetCenter - currentCenter;
    }

    /**
     * 取得僵尸身体中心所在列。
     *
     * 参数：zombie 是目标僵尸；assets 提供图片；time 是测试时刻。
     * 返回：列号。
     */
    private static int columnOfZombie(Zombie zombie, Assets assets, long time) {
        double center = zombie.collisionBox(assets, time).getCenterX();
        return Layout.columnAt((int) center);
    }

    /**
     * 检查爆炸中心九格清除、外圈保留，且种植格同步释放。
     *
     * 参数：state 是爆炸后的场地；centerRow、centerColumn 是中心格。
     */
    private static void checkPlants(GameState state, int centerRow,
            int centerColumn) {
        for (Plant plant : state.plants) {
            boolean insideRows = Math.abs(plant.row - centerRow) <= 1;
            boolean insideColumns = Math.abs(plant.column - centerColumn) <= 1;
            if (insideRows && insideColumns) {
                check(!plant.alive, "九格内有植物没有被清除");
                check(!state.occupied[plant.row][plant.column],
                    "被清除植物的种植格仍被占用");
            } else {
                check(plant.alive, "九格外的植物被误清除");
                check(state.occupied[plant.row][plant.column],
                    "九格外的种植格被误释放");
            }
        }
    }

    /**
     * 检查范围内魅惑僵尸受伤，普通僵尸和范围外魅惑僵尸不受伤。
     *
     * 参数：state 是爆炸后的场地。
     */
    private static void checkZombies(GameState state) {
        for (Zombie zombie : state.zombies) {
            if (zombie.name.equals("JokerZombie")) {
                continue;
            }
            if (zombie.hypno && zombie.row == 0) {
                check(zombie.health == CombatValues.NORMAL_ZOMBIE_HEALTH,
                    "九格外的魅惑僵尸被误伤");
            } else if (zombie.hypno) {
                check(zombie.health == 0, "九格内的魅惑僵尸没有受伤");
            } else {
                check(zombie.health == CombatValues.NORMAL_ZOMBIE_HEALTH,
                    "普通僵尸被爆炸误伤");
            }
        }
    }

    /**
     * 断言测试条件成立。
     *
     * 参数：condition 是检查结果；message 是失败时显示的说明。
     */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
