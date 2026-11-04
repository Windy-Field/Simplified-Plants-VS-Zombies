package pvz;

import java.awt.Rectangle;
import pvz.game.CombatSystem;
import pvz.game.GameState;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.Layout;
import pvz.zombie.Zombie;

/** 用实际子弹命中验证总血量、掉装备、掉头节点以及各种子弹的直接伤害 */
public class CombatValuesTest {
    private static final int TEST_ROW = 2;
    private static final int TEST_CENTER = 450;

    /** 参数 assets 提供素材；所有检查通过后返回，失败抛出异常 */
    public static void run(Assets assets) {
        String[] names = {
            "Zombie", "FlagZombie", "ConeheadZombie", "BucketheadZombie",
            "NewspaperZombie", "FootballZombie", "JokerZombie", "Gargantuar", "Imp"
        };
        int[] healthValues = {270, 270, 640, 1370, 420, 1670, 270, 3000, 150};
        for (int index = 0; index < names.length; index = index + 1) {
            checkZombie(assets, names[index], healthValues[index]);
        }
        checkBullet(assets, "PeaNormal", 25, false);
        checkBullet(assets, "PeaIce", 25, true);
        checkBullet(assets, "BulletMushRoom", 25, true);
        checkBullet(assets, "PeaFire", 50, false);
        System.out.println("PASS: Zombie health, equipment thresholds and projectile damage");
    }

    /** 参数 name 是僵尸名，expectedHealth 是设计血量；逐发命中检查实际扣血和状态节点 */
    private static void checkZombie(Assets assets, String name, int expectedHealth) {
        GameState state = new GameState();
        Zombie zombie = new Zombie(name, TEST_ROW, Layout.rowBottom(TEST_ROW), assets);
        Rectangle body = zombie.collisionBox(assets, state.time);
        zombie.x = zombie.x + TEST_CENTER - body.getCenterX();
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
        Rectangle body = zombie.collisionBox(assets, state.time);
        zombie.x = zombie.x + TEST_CENTER - body.getCenterX();
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

    /** 参数 condition 是判断结果；失败时用 message 说明原因 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
