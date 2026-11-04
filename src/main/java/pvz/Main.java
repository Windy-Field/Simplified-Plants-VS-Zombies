package pvz;

import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import pvz.game.Game;
import pvz.world.Assets;

/**
 * 程序入口
 *
 * 它只做三件事：读命令行参数、加载资源、把游戏窗口显示出来
 * 真正的游戏逻辑都在 Game 里
 */
public class Main {
    /**
     * 启动植物大战僵尸
     *
     * 参数：arguments[0] 是素材目录（可以不传，默认是工作目录下的 assets）；
     *       arguments[1] 是起始关卡编号（可以不传，默认第 0 关）
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

        Assets assets = new Assets(project);

        // Swing 规定：创建窗口必须在事件线程里做，所以包一层 Runnable 交过去
        SwingUtilities.invokeLater(new OpenGameWindow(assets, level));
    }

    /** 用具名启动任务保存素材和关卡，让窗口创建始终在 Swing 事件线程执行 */
    private static class OpenGameWindow implements Runnable {
        private final Assets assets;
        private final int level;

        /** 参数 assets 是共用资源，level 是默认选中的关卡，避免再读取一份配置 */
        OpenGameWindow(Assets assets, int level) {
            this.assets = assets;
            this.level = level;
        }

        /** 创建固定尺寸窗口，窗口打开后显示菜单，关卡由选关页确定 */
        public void run() {
            JFrame window = new JFrame("植物大战僵尸（Java 版） - Windy-Field / Octorange");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setContentPane(new Game(assets, level, true, false));
            window.pack();
            window.setLocationRelativeTo(null);
            window.setResizable(false);
            window.setVisible(true);
        }
    }
}
