package pvz.game;

/**
 * 游戏一共有几种画面。
 *
 * 如果把编号（0、1、2……）直接写在代码里，读者看到 screen == 3
 * 根本不知道是哪个画面；写成 screen == GameScreen.PLAY 就一目了然了。
 */
public final class GameScreen {
    /** 主菜单。 */
    public static final int MENU = 0;

    /** 选卡界面。 */
    public static final int CHOOSE = 1;

    /**
     * 开局演出：镜头往右扫一遍僵尸、移回来，再倒数三秒。
     *
     * 这个阶段不推进关卡逻辑，僵尸不会提前走、阳光也不会掉，
     * 演出走完才切到 PLAY，玩家正好从"准备"状态开始打。
     */
    public static final int INTRO = 2;

    /** 正在打关卡。 */
    public static final int PLAY = 3;

    /** 过关画面。 */
    public static final int VICTORY = 4;

    /** 失败画面。 */
    public static final int LOSS = 5;

    /** 这个类只提供常量，不允许创建对象。 */
    private GameScreen() {
    }
}
