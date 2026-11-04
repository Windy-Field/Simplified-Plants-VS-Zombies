package pvz.game;

/**
 * 游戏一共有几种画面
 *
 * 如果把编号（0、1、2……）直接写在代码里，读者看到 screen == 3
 * 根本不知道是哪个画面；写成 screen == GameScreen.PLAY 就一目了然了
 */
public final class GameScreen {
    /** 主菜单 */
    public static final int MENU = 0;

    /** 选卡界面 */
    public static final int CHOOSE = 1;

    /**
     * 开局演出：镜头往右扫一遍僵尸、移回来，再倒数三秒
     *
     * 这个阶段不推进关卡逻辑，僵尸不会提前走、阳光也不会掉，
     * 演出走完才切到 PLAY，玩家正好从"准备"状态开始打
     */
    public static final int INTRO = 2;

    /** 正在打关卡 */
    public static final int PLAY = 3;

    /** 过关画面 */
    public static final int VICTORY = 4;

    /** 失败画面 */
    public static final int LOSS = 5;

    /** 关卡选择使用独立画面编号，避免与战斗和选卡流程混在一起 */
    public static final int LEVEL_SELECT = 6;

    /** 图鉴独立于关卡，浏览介绍时只推进动画而不运行战斗 */
    public static final int ALMANAC = 7;

    /** 这个类只提供常量，不允许创建对象 */
    private GameScreen() {
    }

    /** 参数 screen 是画面编号，只有关卡内显示返回按钮并接收退出输入，避免误触主菜单 */
    public static boolean canReturnToSelection(int screen) {
        return screen == CHOOSE || screen == INTRO || screen == PLAY;
    }
}
