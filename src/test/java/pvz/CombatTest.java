package pvz;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import pvz.editor.EditorIcons;
import pvz.editor.LevelDesign;
import pvz.game.CombatSystem;
import pvz.game.GameRenderer;
import pvz.game.GameScreen;
import pvz.game.GameState;
import pvz.game.LevelSystem;
import pvz.plant.CloseAttackActions;
import pvz.plant.InstantPlantActions;
import pvz.plant.Plant;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.Layout;
import pvz.zombie.Zombie;
import pvz.zombie.ZombieEffects;
import pvz.zombie.ZombieSpawn;

/** 按战斗职责验证血量、子弹、爆炸和僵尸特殊动作，集中保存场景用例而不按单个品种建类 */
public class CombatTest {
    private static final int TEST_ROW = 2;
    private static final int TEST_COLUMN = 5;
    private static final long START_TIME = 1000;
    /** 子弹命中场景使用草坪中间位置，避免僵尸提前离开画面 */
    private static final int TEST_CENTER = 450;

    /** 参数 assets 提供素材，project 是素材目录；失败抛异常，成功输出检查结果 */
    public static void run(Assets assets, Path project) throws Exception {
        checkCombatValues(assets);
        checkExplosion(assets);
        checkGargantuarSmash(assets);
        checkGargantuarSmashOpponent(assets);
        checkGargantuarThrow(assets, false);
        checkGargantuarThrow(assets, true);
        checkGargantuarDeath(assets);
        checkInstantAttacksOnGargantuar(assets);
        checkBulletsAndImpDeath(assets);
        checkTwoGiants(assets);
        checkGargantuarLevelAndEditor(assets, project);
        System.out.println("PASS: Combat values, explosion, zombie actions, freeze and editor");
    }

    /** 参数 assets 提供素材；用实际命中核对设计血量和子弹伤害，避免只验证常量自身 */
    private static void checkCombatValues(Assets assets) {
        String[] names = {
            "Zombie", "FlagZombie", "ConeheadZombie", "BucketheadZombie",
            "NewspaperZombie", "FootballZombie", "JokerZombie", "Gargantuar", "Imp"
        };
        // 预期值独立记录设计结果，不能直接读取游戏常量，否则常量误改时测试也会跟着通过
        int[] healthValues = {270, 270, 640, 1370, 420, 1670, 270, 3000, 150};
        for (int index = 0; index < names.length; index = index + 1) {
            checkZombieHealth(assets, names[index], healthValues[index]);
        }
        checkBullet(assets, "PeaNormal", 25, false);
        checkBullet(assets, "PeaIce", 25, true);
        checkBullet(assets, "BulletMushRoom", 25, true);
        checkBullet(assets, "PeaFire", 50, false);
        System.out.println("PASS: Zombie health, equipment thresholds and projectile damage");
    }

    /** 参数 name 是僵尸名，expectedHealth 是设计血量；逐发命中检查实际扣血和状态节点 */
    private static void checkZombieHealth(Assets assets, String name, int expectedHealth) {
        GameState state = new GameState();
        Zombie zombie = new Zombie(name, TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        placeAt(zombie, TEST_CENTER, assets, state.time);
        state.zombies.add(zombie);
        CombatSystem combat = new CombatSystem(state, assets);
        check(zombie.health == expectedHealth, "初始血量不符合设计：" + name);
        boolean startedWithHelmet = zombie.helmet;
        int shots = 0;
        while (zombie.health > 0) {
            int previousHealth = zombie.health;
            fireAt(assets, state, combat, zombie, "PeaNormal");
            shots = shots + 1;
            check(previousHealth - zombie.health == 25, "普通豌豆没有造成 25 伤害：" + name);

            // 命中后的下一次战斗更新负责切换掉装备和掉头动画
            combat.updateAll();
            if (startedWithHelmet && zombie.health > 0) {
                boolean shouldHaveHelmet = zombie.health > 270;
                check(zombie.helmet == shouldHaveHelmet, "掉装备血量节点错误：" + name);
            }
            if (zombie.usesStandardDamageStates() && zombie.health > 0) {
                boolean shouldLoseHead = zombie.health <= 70;
                check(zombie.headLost == shouldLoseHead, "掉头血量节点错误：" + name);
                boolean shouldLoseArm = zombie.health <= 170;
                check(zombie.armLost == shouldLoseArm, "掉臂血量节点错误：" + name);
            }
        }
        int expectedShots = (expectedHealth + 24) / 25;
        check(shots == expectedShots, "击倒所需豌豆数量错误：" + name);
        check(zombie.dying, "血量耗尽后没有进入死亡流程：" + name);
    }

    /** 参数 kind、expectedDamage 和 expectedSlow 描述子弹设计，核对创建和实际命中结果 */
    private static void checkBullet(Assets assets, String kind, int expectedDamage,
            boolean expectedSlow) {
        GameState state = new GameState();
        Zombie zombie = new Zombie("BucketheadZombie", TEST_ROW,
            Layout.rowBottom(TEST_ROW), assets);
        placeAt(zombie, TEST_CENTER, assets, state.time);
        state.zombies.add(zombie);
        CombatSystem combat = new CombatSystem(state, assets);
        int initialHealth = zombie.health;
        Bullet bullet = fireAt(assets, state, combat, zombie, kind);
        check(bullet.damage == expectedDamage, "子弹基础伤害错误：" + kind);
        check(initialHealth - zombie.health == expectedDamage, "子弹实际伤害错误：" + kind);
        check(bullet.ice == expectedSlow, "子弹减速属性错误：" + kind);
    }

    /** 参数 target 是命中目标，kind 是子弹名；返回实际发射并命中的子弹 */
    private static Bullet fireAt(Assets assets, GameState state, CombatSystem combat,
            Zombie target, String kind) {
        state.bullets.clear();
        Rectangle body = target.collisionBox(assets, state.time);
        int top = (int) body.getMaxY() - 30;
        Bullet bullet = new Bullet(kind, body.x, top, TEST_ROW, top, assets);
        state.bullets.add(bullet);
        state.time = state.time + Layout.FIXED_STEP_MS;
        combat.updateAll();
        check(bullet.exploded, "测试子弹没有命中：" + kind);
        return bullet;
    }

    /** 樱桃实际引爆后直接进入灰烬动画，不应生成头部，也不能重复启动死亡动画 */
    private static void checkExplosion(Assets assets) {
        String[] names = {"Zombie", "ConeheadZombie", "BucketheadZombie", "FlagZombie",
            "NewspaperZombie", "FootballZombie", "Imp"};
        for (String name : names) {
            GameState state = new GameState();
            state.time = START_TIME;
            int center = Layout.columnCenter(4);
            int bottom = Layout.rowBottom(2);
            Plant cherry = new Plant("CherryBomb", center, bottom, 2, 4, assets, state.time, false);
            Zombie zombie = new Zombie(name, 2, bottom, assets);
            placeAt(zombie, center, assets, state.time);
            state.zombies.add(zombie);
            state.time = state.time + assets.count(cherry.animation) * Layout.DEFAULT_ANIMATION_INTERVAL;
            InstantPlantActions actions = new InstantPlantActions(assets, state);
            actions.update(cherry);
            check(zombie.dying && zombie.deathAnimationStarted, "爆炸没有立即开始死亡动画：" + name);
            check(!zombie.waitingForDeath && !zombie.headLost && state.heads.isEmpty(),
                "樱桃炸弹仍触发掉头：" + name);
            check(zombie.animation.endsWith("BoomDie"), "爆炸没有使用灰烬图：" + name);
            long deathTime = zombie.deathTime;
            ZombieEffects.die(zombie, assets, state, state.time + 16, true);
            check(zombie.deathTime == deathTime, "重复爆炸重启了死亡动画");
            long duration = (long) assets.count(zombie.animation) * zombie.frameInterval;
            ZombieEffects.updateDeath(zombie, assets, state, state.time + duration);
            check(!zombie.alive, "爆炸动画结束后未清理僵尸：" + name);
        }
    }

    /** 创建草坪中间的巨人；参数 state 是所在游戏，返回已加入场地的巨人 */
    private static Zombie createGiant(Assets assets, GameState state) {
        state.time = START_TIME;
        state.screen = GameScreen.PLAY;
        Zombie giant = new Zombie("Gargantuar", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        placeAt(giant, Layout.columnCenter(TEST_COLUMN), assets, state.time);
        state.zombies.add(giant);
        return giant;
    }

    /** 参数 center 是身体中心；按碰撞盒位置放置，避免把图片透明留白当作身体 */
    private static void placeAt(Zombie zombie, double center, Assets assets, long time) {
        Rectangle body = zombie.collisionBox(assets, time);
        zombie.x = zombie.x + center - body.getCenterX();
    }

    /** 参数 duration 是需推进的游戏毫秒数；每次用原游戏固定步长更新 */
    private static void advance(GameState state, CombatSystem combat, long duration) {
        long endTime = state.time + duration;
        while (state.time < endTime) {
            state.time = Math.min(endTime, state.time + Layout.FIXED_STEP_MS);
            combat.updateAll();
        }
    }

    /** 验证砸击不是普通啃咬：落棒前不扣血，落棒时清掉坚果并释放格子 */
    private static void checkGargantuarSmash(Assets assets) {
        GameState state = new GameState();
        Zombie giant = createGiant(assets, state);
        Plant nut = new Plant("WallNut", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, true);
        state.plants.add(nut);
        state.occupied[TEST_ROW][TEST_COLUMN] = true;
        CombatSystem combat = new CombatSystem(state, assets);
        combat.updateAll();
        check(giant.attacking, "巨人接触坚果后没有挥棒");
        advance(state, combat, Layout.GARGANTUAR_SMASH_DELAY - 100);
        check(nut.health == Layout.WALL_NUT_HEALTH, "巨人落棒前提前造成了伤害");

        giant.frozenUntil = state.time + 500;
        advance(state, combat, 500);
        check(nut.alive, "巨人冻结期间仍完成了砸击");
        advance(state, combat, 200);
        check(!nut.alive && nut.health == 0, "巨人没有一击砸掉坚果");
        check(!state.occupied[TEST_ROW][TEST_COLUMN], "巨人砸掉坚果后没有释放格子");
        check(state.heads.isEmpty(), "巨人错误进入了掉头流程");
    }

    /** 参数 hypnotized 决定阵营；验证半血只投一次、飞行无碰撞、解冻落地与阵营一致 */
    private static void checkGargantuarThrow(Assets assets, boolean hypnotized) {
        GameState state = new GameState();
        Zombie giant = createGiant(assets, state);
        giant.hypno = hypnotized;
        CombatSystem combat = new CombatSystem(state, assets);
        giant.health = Layout.GARGANTUAR_HEALTH / 2 + 1;
        combat.updateAll();
        check(!giant.throwingImp, "巨人高于半血就开始投掷");
        giant.health = Layout.GARGANTUAR_HEALTH / 2;
        combat.updateAll();
        check(giant.throwingImp, "巨人半血没有开始投掷");
        check(giant.animation.equals("GargantuarThrow"), "巨人未播放投掷动画");
        giant.frozenUntil = state.time + 500;
        advance(state, combat, 500);
        check(giant.actionElapsed == 0, "巨人冻结期间投掷动作仍在推进");
        long releaseDelay = assets.gargantuarReleaseDelay();
        advance(state, combat, releaseDelay - 100);
        check(state.zombies.size() == 1, "小鬼还没脱手就提前生成");
        advance(state, combat, 150);
        check(state.zombies.size() == 2, "巨人没有生成小鬼");
        Zombie imp = state.zombies.get(1);
        check(imp.isImp() && imp.airborne, "生成的小鬼没有进入飞行状态");
        check(imp.hypno == hypnotized, "小鬼阵营与巨人不一致");
        check(imp.collisionBox(assets, state.time).isEmpty(), "空中小鬼参与了地面碰撞");
        if (hypnotized) {
            check(imp.flightTargetX > imp.flightStartX, "魅惑巨人投掷方向错误");
        } else {
            check(imp.flightTargetX < imp.flightStartX, "巨人没有向草坪左侧投掷");
        }

        double frozenX = imp.x;
        double frozenY = imp.y;
        imp.frozenUntil = state.time + 500;
        advance(state, combat, 500);
        check(imp.x == frozenX && imp.y == frozenY, "被冻结的小鬼仍在飞行");
        advance(state, combat, Layout.IMP_FLIGHT_DURATION);
        check(!imp.airborne, "小鬼没有落地");
        check(!imp.collisionBox(assets, state.time).isEmpty(), "小鬼落地后仍没有碰撞盒");
        Rectangle impBody = imp.bounds(assets, state.time);
        check(Math.abs(impBody.getMaxY() - Layout.rowBottom(TEST_ROW)) < 2,
            "小鬼脚底没有对齐原来的行");

        advance(state, combat, 5000);
        check(!giant.throwingImp && giant.impThrown, "投掷动画结束后状态不正确");
        check(giant.animation.equals("GargantuarNoImp"), "投掷后没有切换无小鬼动画");
        check(state.zombies.size() == 2, "同一只巨人重复投掷了小鬼");
    }

    /** 验证巨人对敌方僵尸每次挥棒只造成一次伤害，减速时推迟落棒 */
    private static void checkGargantuarSmashOpponent(Assets assets) {
        GameState state = new GameState();
        Zombie giant = createGiant(assets, state);
        giant.hypno = true;
        Zombie opponent = new Zombie("BucketheadZombie", TEST_ROW,
            Layout.rowBottom(TEST_ROW), assets);
        placeAt(opponent, Layout.columnCenter(TEST_COLUMN), assets, state.time);
        state.zombies.add(opponent);
        CombatSystem combat = new CombatSystem(state, assets);
        combat.updateAll();
        int initialHealth = opponent.health;
        giant.slowedUntil = state.time + 10000;
        advance(state, combat, Layout.GARGANTUAR_SMASH_DELAY);
        check(opponent.health == initialHealth, "减速中的巨人提前落棒");
        advance(state, combat, Layout.GARGANTUAR_SMASH_DELAY + 100);
        check(opponent.health == initialHealth - Layout.GARGANTUAR_SMASH_DAMAGE,
            "巨人砸僵尸时伤害错误");
        advance(state, combat, 300);
        check(opponent.health == initialHealth - Layout.GARGANTUAR_SMASH_DAMAGE,
            "巨人一次挥棒重复造成伤害");
    }

    /** 验证投掷前后使用对应死亡图，投掷中死亡不再生成小鬼，也不会触发小丑爆炸 */
    private static void checkGargantuarDeath(Assets assets) {
        for (int scenario = 0; scenario < 2; scenario = scenario + 1) {
            GameState state = new GameState();
            Zombie giant = createGiant(assets, state);
            giant.impThrown = scenario == 1;
            giant.throwingImp = true;
            ZombieEffects.die(giant, assets, state, state.time, true);
            String expected = "GargantuarDie";
            if (giant.impThrown) {
                expected = "GargantuarNoImpDie";
            }
            check(giant.animation.equals(expected), "巨人死亡时使用了错误的素材");
            check(!giant.throwingImp && !giant.waitingForDeath, "死亡没有中断投掷");
            check(state.heads.isEmpty(), "巨人死亡时生成了普通僵尸头");
            CombatSystem combat = new CombatSystem(state, assets);
            advance(state, combat, 6000);
            check(!giant.alive, "巨人死亡动画结束后仍然存活");
            check(state.zombies.size() == 1 && state.effects.isEmpty(),
                "巨人死亡时错误生成了小鬼或爆炸");
        }
    }

    /** 验证炸弹与辣椒需两次击倒巨人，大嘴花不能吞下巨人 */
    private static void checkInstantAttacksOnGargantuar(Assets assets) {
        GameState state = new GameState();
        Zombie giant = createGiant(assets, state);
        Plant cherry = new Plant("CherryBomb", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, true);
        InstantPlantActions actions = new InstantPlantActions(assets, state);
        long growDuration = assets.count(cherry.animation) * Layout.DEFAULT_ANIMATION_INTERVAL;
        state.time = state.time + growDuration;
        actions.update(cherry);
        int remaining = Layout.GARGANTUAR_HEALTH - Layout.GARGANTUAR_INSTANT_DAMAGE;
        check(!giant.dying && giant.health == remaining, "一颗樱桃没有按约定伤害巨人");

        Plant chomper = new Plant("Chomper", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, true);
        CloseAttackActions closeActions = new CloseAttackActions(assets, state);
        closeActions.update(chomper);
        check(!chomper.triggered, "大嘴花不应该吞下巨人");

        Plant pepper = new Plant("Jalapeno", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, state.time, true);
        growDuration = assets.count(pepper.animation) * Layout.DEFAULT_ANIMATION_INTERVAL;
        state.time = state.time + growDuration;
        actions.update(pepper);
        check(giant.dying, "第二次爆炸没有击倒巨人");

        GameState mineState = new GameState();
        Zombie mineTarget = createGiant(assets, mineState);
        Plant mine = new Plant("PotatoMine", Layout.columnCenter(TEST_COLUMN),
            Layout.rowBottom(TEST_ROW), TEST_ROW, TEST_COLUMN, assets, 0, true);
        mineState.time = Layout.POTATO_MINE_ARM_TIME + 1;
        CloseAttackActions mineActions = new CloseAttackActions(assets, mineState);
        mineActions.update(mine);
        check(mineTarget.health == remaining && !mineTarget.dying,
            "土豆雷未按巨人身体位置结算伤害");
    }

    /** 验证冰豌豆能命中巨人，以及小鬼使用自己的死亡动画而不生成普通僵尸头 */
    private static void checkBulletsAndImpDeath(Assets assets) {
        GameState state = new GameState();
        Zombie giant = createGiant(assets, state);
        Rectangle body = giant.collisionBox(assets, state.time);
        int bulletTop = (int) body.getMaxY() - 30;
        Bullet bullet = new Bullet("PeaIce", body.x, bulletTop,
            TEST_ROW, bulletTop, assets);
        state.bullets.add(bullet);
        CombatSystem combat = new CombatSystem(state, assets);
        combat.updateAll();
        check(bullet.exploded, "冰豌豆没有命中巨人");
        check(giant.health == Layout.GARGANTUAR_HEALTH - bullet.damage,
            "巨人没有受到子弹伤害");
        check(giant.slowedUntil > state.time, "巨人没有受到寒冰减速");

        Zombie imp = new Zombie("Imp", TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        state.zombies.add(imp);
        ZombieEffects.die(imp, assets, state, state.time, false);
        check(imp.animation.equals("ImpDie"), "小鬼没有使用专属死亡图");
        advance(state, combat, 3000);
        check(!imp.alive && state.heads.isEmpty(), "小鬼死亡流程错误");
    }

    /** 验证同时投掷不会修改遍历中的集合，并且空中的小鬼不会导致提前胜利 */
    private static void checkTwoGiants(Assets assets) {
        GameState state = new GameState();
        Zombie first = createGiant(assets, state);
        Zombie second = createGiant(assets, state);
        first.health = Layout.GARGANTUAR_HEALTH / 2;
        second.health = Layout.GARGANTUAR_HEALTH / 2;
        CombatSystem combat = new CombatSystem(state, assets);
        combat.updateAll();
        advance(state, combat, assets.gargantuarReleaseDelay() + 16);
        check(state.zombies.size() == 4, "两只巨人没有各投掷一只小鬼");
        first.alive = false;
        second.alive = false;
        Random random = new Random(1);
        LevelSystem levels = new LevelSystem(state, assets, random);
        check(!levels.isVictoryReady(), "小鬼尚未落地时提前胜利");
    }

    /** 验证编辑器和关卡出怪读取同一个巨人名字，并输出游戏渲染图供检查 */
    private static void checkGargantuarLevelAndEditor(Assets assets, Path project) throws Exception {
        LevelDesign design = new LevelDesign();
        design.setCell(TEST_ROW, 0, "Gargantuar", 1);
        List<ZombieSpawn> spawns = design.buildSpawns();
        ZombieSpawn firstSpawn = spawns.get(0);
        check(firstSpawn.name.equals("Gargantuar"), "编辑器没有保留巨人品种");
        EditorIcons icons = new EditorIcons(assets);
        BufferedImage icon = icons.icon("Gargantuar", 56);
        check(icon.getHeight() == 56, "编辑器不能显示巨人图标");

        GameState spawnState = new GameState();
        spawnState.time = firstSpawn.spawnTime;
        spawnState.schedule.add(firstSpawn);
        Random random = new Random(1);
        LevelSystem spawnSystem = new LevelSystem(spawnState, assets, random);
        spawnSystem.updateSpawnsAndDrops();
        Zombie spawned = spawnState.zombies.get(0);
        check(spawned.isGargantuar(), "出场表没有生成巨人");

        GameState state = new GameState();
        Zombie giant = createGiant(assets, state);
        state.background = assets.frame("Background", 0);
        state.sunValue = 500;
        Zombie ordinary = new Zombie("Zombie", 3, Layout.rowBottom(3), assets);
        placeAt(ordinary, Layout.columnCenter(TEST_COLUMN), assets, state.time);
        state.zombies.add(ordinary);
        giant.animationStart = state.time;
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D painter = image.createGraphics();
        GameRenderer renderer = new GameRenderer(assets);
        renderer.draw(painter, GameScreen.PLAY, state.time, state, true);
        painter.dispose();
        Path output = project.resolve("../build/gargantuar-play.png");
        ImageIO.write(image, "png", output.toFile());

        LevelSystem levels = new LevelSystem(state, assets, random);
        check(!levels.isVictoryReady(), "巨人仍在场上时提前胜利");
    }

    /** 参数 condition 是测试结果；失败时抛出 message，停止自检 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
