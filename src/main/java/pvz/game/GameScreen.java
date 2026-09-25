package pvz.game;

/**
 * 游戏一共有几种画面。
 *
 * 如果把编号（0、1、2、3、4）直接写在代码里，读者看到 screen == 2
 * 根本不知道是哪个画面；写成 screen == GameScreen.PLAY 就一目了然了。
 */
public final class GameScreen {
    /** 主菜单。 */
    public static final int MENU = 0;

    /** 选卡界面。 */
    public static final int CHOOSE = 1;

    /** 正在打关卡。 */
    public static final int PLAY = 2;

    /** 过关画面。 */
    public static final int VICTORY = 3;

    /** 失败画面。 */
    public static final int LOSS = 4;

    /** 这个类只提供常量，不允许创建对象。 */
    private GameScreen() {
    }
}
