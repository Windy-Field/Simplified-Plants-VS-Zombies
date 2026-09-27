package pvz;

import java.nio.file.Path;
import pvz.plant.BulletTransformation;
import pvz.plant.Plant;
import pvz.plant.PlantCatalog;
import pvz.plant.PlantDefinition;
import pvz.world.Assets;
import pvz.world.Bullet;
import pvz.world.CombatValues;
import pvz.world.Layout;

/**
 * 检查火炬树桩的子弹转换资料和通用转换流程。
 */
public class TorchwoodTest {
    /**
     * 运行火炬树桩专项检查。
     *
     * 参数：arguments[0] 是素材目录；省略时使用当前目录下的 assets。
     */
    public static void main(String[] arguments) throws Exception {
        Path root = Path.of("assets");
        if (arguments.length > 0) {
            root = Path.of(arguments[0]);
        }

        Assets assets = new Assets(root);
        Plant torchwood = new Plant("Torchwood", Layout.columnCenter(3),
            Layout.rowBottom(2), 2, 3, assets, 1000, false);
        checkTransformationData();
        checkNormalPea(assets, torchwood);
        checkIcePea(assets, torchwood);
        checkIcePeaTravelsThroughTorchwood(assets, torchwood);
        System.out.println("PASS：火炬树桩子弹转换检查通过");
    }

    /** 检查火炬树桩资料里登记了普通豌豆和冰豌豆两条规则。 */
    private static void checkTransformationData() {
        PlantDefinition definition = PlantCatalog.definitionOf("Torchwood");
        BulletTransformation normal = definition.transformationFor("PeaNormal");
        BulletTransformation ice = definition.transformationFor("PeaIce");
        check(normal != null, "火炬树桩没有登记普通豌豆转换规则");
        check(ice != null, "火炬树桩没有登记冰豌豆转换规则");
        check("PeaFire".equals(normal.targetBullet), "普通豌豆没有转换成火焰豌豆");
        check(normal.damageMultiplier == 2, "火焰豌豆伤害倍率不是 2");
        check("PeaNormal".equals(ice.targetBullet), "冰豌豆没有转换成普通豌豆");
    }

    /** 检查普通豌豆经过火焰区域后会变成火焰豌豆并翻倍伤害。 */
    private static void checkNormalPea(Assets assets, Plant torchwood) {
        Bullet bullet = bulletAtTorchwood(torchwood, "PeaNormal", assets);
        boolean transformed = torchwood.tryTransformBullet(bullet, assets, 1000);
        check(transformed, "普通豌豆没有经过火炬树桩的转换区域");
        check("PeaFire".equals(bullet.name), "普通豌豆没有变成火焰豌豆");
        check(bullet.damageMultiplier == 2, "火焰豌豆伤害倍率不正确");
        check(!bullet.ice, "火焰豌豆不应该带有冰冻效果");
        check(bullet.damageAmount(CombatValues.BULLET_DAMAGE)
                == CombatValues.BULLET_DAMAGE * 2,
            "火焰豌豆的实际伤害没有翻倍");
        check(!torchwood.tryTransformBullet(bullet, assets, 1000),
            "同一颗子弹在同一株火炬树桩上重复转换");
    }

    /** 检查冰豌豆经过火焰区域后会变回普通豌豆并失去减速效果。 */
    private static void checkIcePea(Assets assets, Plant torchwood) {
        Bullet bullet = bulletAtTorchwood(torchwood, "PeaIce", assets);
        boolean transformed = torchwood.tryTransformBullet(bullet, assets, 1000);
        check(transformed, "冰豌豆没有经过火炬树桩的转换区域");
        check("PeaNormal".equals(bullet.name), "冰豌豆没有变成普通豌豆");
        check(!bullet.ice, "变回普通豌豆后仍然保留冰冻效果");
        check(bullet.damageMultiplier == 1, "普通豌豆的伤害倍率不正确");
    }

    /** 检查冰豌豆从火炬树桩左侧飞过来时也能完成转换。 */
    private static void checkIcePeaTravelsThroughTorchwood(Assets assets,
            Plant torchwood) {
        BulletTransformation transformation =
            PlantCatalog.definitionOf("Torchwood").transformationFor("PeaIce");
        int startLeft = (int) torchwood.x - 90;
        int top = (int) torchwood.y + transformation.zoneTop;
        Bullet bullet = new Bullet("PeaIce", startLeft, top, torchwood.row,
            top, assets);

        boolean transformed = false;
        for (int step = 0; step < 40; step++) {
            bullet.update(1000 + step * 16);
            if (torchwood.tryTransformBullet(bullet, assets,
                    1000 + step * 16)) {
                transformed = true;
                break;
            }
        }

        check(transformed, "飞过火炬树桩的冰豌豆没有完成转换");
        check("PeaNormal".equals(bullet.name),
            "飞过火炬树桩的冰豌豆没有变回普通豌豆");
        check(!bullet.ice, "飞过火炬树桩的冰豌豆仍然带有减速效果");
    }

    /** 把子弹放到火炬树桩左侧的转换区域，模拟子弹刚碰到火焰。 */
    private static Bullet bulletAtTorchwood(Plant torchwood, String kind,
            Assets assets) {
        BulletTransformation transformation =
            PlantCatalog.definitionOf("Torchwood").transformationFor(kind);
        int[] visible = assets.animationBounds(kind);
        int flameX = (int) torchwood.x + transformation.zoneLeft + 1;
        int left = flameX - visible[0];
        int top = (int) torchwood.y + transformation.zoneTop;
        return new Bullet(kind, left, top, torchwood.row, top, assets);
    }

    /** 检查一个测试条件。 */
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
