package pvz;

import java.nio.file.Path;
import pvz.editor.LevelEditor;
import pvz.world.Assets;

/**
 * 关卡编辑器的入口。
 *
 * 游戏和编辑器各有各的入口：想玩就运行 Main，想改关卡就运行这个类。
 * 在 IDEA 里点 main 方法左边的绿色按钮就能直接打开编辑器，不用配命令行参数。
 */
public class EditorMain {
    /**
     * 打开关卡编辑器。
     *
     * 参数：arguments[0] 是素材目录（可以不传，默认是工作目录下的 assets）；
     *       arguments[1] 是一开始编辑第几关（可以不传，默认第 0 关）。
     * 异常：素材目录不存在或图片读不了时抛出异常。
     */
    public static void main(String[] arguments) throws Exception {
        Path project = Path.of("assets");
        int level = 0;
        if (arguments.length > 0) {
            project = Path.of(arguments[0]);
        }
        if (arguments.length > 1) {
            level = Integer.parseInt(arguments[1]);
        }

        // 编辑器要用僵尸图片画格子，所以和游戏一样得先把素材读进来。
        Assets assets = new Assets(project);
        LevelEditor.open(assets, project, level);
    }
}
