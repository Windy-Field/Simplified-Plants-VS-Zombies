package pvz;

import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import pvz.game.Game;
import pvz.plant.Plant;
import pvz.world.Assets;

/**
 * 程序入口。
 *
 * 它只做三件事：读命令行参数、加载资源、把游戏窗口显示出来。
 * 真正的游戏逻辑都在 Game 里。
 */
public class Main {
    /**
     * 启动植物大战僵尸。
     *
     * 参数：arguments[0] 是素材目录（可以不传，默认是工作目录下的 assets）；
     *       arguments[1] 是起始关卡编号（可以不传，默认第 0 关）。
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

        // Swing 规定：创建窗口必须在事件线程里做，所以包一层 Runnable 交过去。
        final int startLevel = level;
        SwingUtilities.invokeLater(new Runnable() {
            /** 在 Swing 事件线程里创建并显示窗口。 */
            public void run() {
                JFrame window = new JFrame("植物大战僵尸（Java 版） - Windy-Field / Octorange");
                window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                window.setContentPane(new Game(assets, startLevel));
                window.pack();
                window.setLocationRelativeTo(null);
                window.setResizable(false);
                window.setVisible(true);
            }
        });
    }
}
