package pvz.editor;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import pvz.game.Game;
import pvz.game.GameState;
import pvz.plant.Cards;
import pvz.world.Assets;
import pvz.world.Layout;

/**
 * 关卡编辑器的主窗口。
 *
 * 窗口分成三块：左边是可以挑的僵尸，中间是出怪网格，右边是关卡参数。
 * 这个类负责把三块拼起来，处理读盘存盘，并且居中协调一次拖动——
 * 因为拖动常常从一块面板开始、在另一块结束，只有窗口同时认识两边。
 */
public class LevelEditor extends JFrame implements EditorDragController {
    /** 拖动时跟着鼠标走的那个僵尸画多大。 */
    private static final int DRAG_ICON_HEIGHT = 56;

    /** 拖动时跟着鼠标走的僵尸画得稍微透一点，免得挡住底下的格子。 */
    private static final float DRAG_ICON_ALPHA = 0.8f;

    /** 界面文字的字号，和 Windows 自带程序差不多大。 */
    private static final int INTERFACE_FONT_SIZE = 12;

    /** 参数面板右边那一列控件的宽度，要装得下"0 - 白天草坪"这种最长的选项。 */
    private static final int FIELD_WIDTH = 132;

    /** 参数面板里控件的高度。 */
    private static final int FIELD_HEIGHT = 26;

    /** 提供图片素材，试玩时也要交给游戏。 */
    private final Assets assets;

    /** assets 目录，关卡文件就在它下面的 levels 里。 */
    private final Path assetRoot;

    /** 提供僵尸图标。 */
    private final EditorIcons icons;

    /** 正在编辑的关卡。打开别的关卡时会被整个换掉。 */
    private LevelDesign design = new LevelDesign();

    /** 中间的出怪网格。 */
    private final EditorGrid grid;

    /** 左边的僵尸列表。 */
    private final EditorPalette palette;

    /** 底部状态栏，显示统计和各种提示。 */
    private final JLabel statusLabel = new JLabel(" ");

    /** 要读写第几关。 */
    private final JSpinner levelSpinner;

    /** 一共几波，也就是网格有几列。 */
    private final JSpinner waveCountSpinner;

    /** 开局赠送多少阳光。 */
    private final JSpinner initialSunSpinner;

    /** 天空多久掉一颗阳光，单位是秒。 */
    private final JSpinner skySunSpinner;

    /** 第一波等多久才出，单位是秒。 */
    private final JSpinner firstWaveSpinner;

    /** 两波之间隔多久，单位是秒。 */
    private final JSpinner waveIntervalSpinner;

    /** 同一格里的僵尸错开多久，单位是毫秒。 */
    private final JSpinner spacingSpinner;

    /** 用第几张背景图。 */
    private final JComboBox<String> backgroundBox;

    /** 用哪种卡槽模式。 */
    private final JComboBox<String> barBox;

    /** 传送带和保龄球模式的可出卡池。 */
    private final CheckListPanel poolList;

    /** 本关禁用的植物。 */
    private final CheckListPanel bannedList;

    /** 本关强制携带的植物。 */
    private final CheckListPanel requiredList;

    /** 勾上表示当前选中的格子改成随机行出怪。 */
    private final JCheckBox randomRowBox = new JCheckBox("选中格子随机行出怪");

    /** 僵尸列表里当前选中的品种，在空格子上点左键就放它。 */
    private String selectedKind = LevelDesign.ZOMBIE_KINDS[0];

    /**
     * 是否正在把关卡数据写进各个控件。
     *
     * 给控件赋值会触发它们的监听器，若不加这个开关，
     * 赋值过程会被当成用户在改参数，再反过来去改关卡数据，绕成一个圈。
     */
    private boolean updatingFields;

    /** 此刻是不是正拖着一只僵尸。 */
    private boolean dragging;

    /** 正在拖的僵尸品种。 */
    private String dragKind;

    /** 正在拖的僵尸只数。 */
    private int dragCount;

    /** 拖动的内容来自哪个格子；从僵尸列表拖出来的就是 -1。 */
    private int dragFromRow = -1;

    private int dragFromWave = -1;

    /** 鼠标当前位置，换算成了玻璃面板的坐标，用来画跟手的那只僵尸。 */
    private Point dragPoint;

    /**
     * 创建并布置好编辑器窗口。
     *
     * 参数：originalAssets 提供图片素材；root 是 assets 目录；
     *       levelNumber 是一开始要编辑第几关。
     */
    public LevelEditor(Assets originalAssets, Path root, int levelNumber) {
        super("植物大战僵尸 - 关卡编辑器 - Windy-Field / Octorange");
        assets = originalAssets;
        assetRoot = root;
        icons = new EditorIcons(originalAssets);

        grid = new EditorGrid(design, icons, this);
        palette = new EditorPalette(icons, this);

        levelSpinner = new JSpinner(new SpinnerNumberModel(levelNumber, 0, 99, 1));
        waveCountSpinner = new JSpinner(new SpinnerNumberModel(design.waveCount(),
            LevelDesign.MIN_WAVE_COUNT, LevelDesign.MAX_WAVE_COUNT, 1));
        initialSunSpinner = new JSpinner(new SpinnerNumberModel(design.initialSun, 0, 9990, 25));
        // 时间类的参数按秒显示，用户不用心算毫秒。
        skySunSpinner = new JSpinner(new SpinnerNumberModel(
            design.skySunInterval / 1000.0, 0.5, 120.0, 0.5));
        firstWaveSpinner = new JSpinner(new SpinnerNumberModel(
            design.firstWaveDelay / 1000.0, 0.0, 600.0, 1.0));
        waveIntervalSpinner = new JSpinner(new SpinnerNumberModel(
            design.waveInterval / 1000.0, 1.0, 300.0, 1.0));
        spacingSpinner = new JSpinner(new SpinnerNumberModel(
            (int) design.spawnSpacing, (int) LevelDesign.MIN_SPAWN_SPACING, 5000, 50));
        backgroundBox = new JComboBox<String>(LevelDesign.BACKGROUND_LABELS);
        barBox = new JComboBox<String>(LevelDesign.BAR_LABELS);
        poolList = new CheckListPanel(plantChoices());
        bannedList = new CheckListPanel(chooserPlantChoices());
        requiredList = new CheckListPanel(chooserPlantChoices());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildPaletteSide(), BorderLayout.WEST);
        add(buildGridArea(), BorderLayout.CENTER);
        add(buildSettingsSide(), BorderLayout.EAST);
        add(buildStatusBar(), BorderLayout.SOUTH);
        installGlassPane();

        hookListeners();
        writeFields();
        updateStatus();

        // 编辑器多半是拿来改现成关卡的，开窗时就把这一关读进来，省一次点击。
        Path initial = assetRoot.resolve("levels/level_" + levelNumber + ".json");
        if (Files.isRegularFile(initial)) {
            openPath(initial);
        }

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * 列出卡池里可选的植物，顺带标上各自要花多少阳光。
     *
     * 返回：每种植物一行的文字。
     */
    private static String[] plantChoices() {
        return plantChoices(Cards.PLANTS.length);
    }

    /**
     * 列出能进选卡界面的植物，给禁用/必选清单用。
     *
     * 保龄球那两种只在保龄球关里出现，本来就不进选卡界面，
     * 把它们列进禁用/必选清单没有意义，所以不列。
     *
     * 返回：每种植物一行的文字。
     */
    private static String[] chooserPlantChoices() {
        return plantChoices(Cards.CHOOSER_CARD_COUNT);
    }

    /**
     * 列出前若干种植物，每行带上中文名和阳光数。
     *
     * 参数：count 是要列几种。
     * 返回：每种植物一行的文字。
     */
    private static String[] plantChoices(int count) {
        String[] result = new String[count];
        for (int index = 0; index < count; index++) {
            result[index] = Cards.PLANTS[index] + "（" + Cards.COST[index] + "）";
        }
        return result;
    }

    /**
     * 拼出顶部那一排按钮。
     *
     * 返回：摆好按钮的面板。
     */
    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xCC, 0xCC, 0xCC)));

        addLevelNumberButtons(toolbar);
        toolbar.add(Box.createHorizontalStrut(10));
        addFileButtons(toolbar);
        toolbar.add(Box.createHorizontalStrut(10));
        addEditButtons(toolbar);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(button("保存并试玩", new ActionListener() {
            /** 先存盘，再开一个游戏窗口跑这一关。 */
            public void actionPerformed(ActionEvent event) {
                playCurrentLevel();
            }
        }));
        return toolbar;
    }

    /**
     * 往工具栏上摆"关卡编号 + 读取 + 保存"这一组。
     *
     * 参数：toolbar 是要摆按钮的工具栏。
     */
    private void addLevelNumberButtons(JPanel toolbar) {
        toolbar.add(new JLabel("关卡编号"));
        levelSpinner.setPreferredSize(new Dimension(58, 26));
        toolbar.add(levelSpinner);

        JButton loadButton = button("读取关卡", new ActionListener() {
            /** 把关卡编号对应的文件读进来。 */
            public void actionPerformed(ActionEvent event) {
                openLevelByNumber();
            }
        });
        toolbar.add(loadButton);

        JButton saveButton = button("保存关卡", new ActionListener() {
            /** 写回关卡编号对应的文件。 */
            public void actionPerformed(ActionEvent event) {
                saveLevelByNumber();
            }
        });
        toolbar.add(saveButton);

        // 监听关卡编号变化，超出现有关卡范围就禁用保存按钮
        levelSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent event) {
                int number = levelNumber();
                boolean canSave = canEditLevel(number);
                saveButton.setEnabled(canSave);
                if (!canSave) {
                    statusLabel.setText("关卡 " + number + " 超出现有范围，只能读取和另存为，不能直接保存");
                } else {
                    updateStatus();
                }
            }
        });

        // 初始状态也要检查一次
        saveButton.setEnabled(canEditLevel(levelNumber()));
    }

    /**
     * 往工具栏上摆"自己挑文件打开 / 另存为"这一组。
     *
     * 参数：toolbar 是要摆按钮的工具栏。
     */
    private void addFileButtons(JPanel toolbar) {
        toolbar.add(button("打开文件…", new ActionListener() {
            /** 自己挑一个关卡文件打开。 */
            public void actionPerformed(ActionEvent event) {
                openFromChooser();
            }
        }));
        toolbar.add(button("另存为…", new ActionListener() {
            /** 自己挑一个位置存盘。 */
            public void actionPerformed(ActionEvent event) {
                saveWithChooser();
            }
        }));
    }

    /**
     * 往工具栏上摆"新建 / 清空"这一组。
     *
     * 参数：toolbar 是要摆按钮的工具栏。
     */
    private void addEditButtons(JPanel toolbar) {
        toolbar.add(button("新建空关卡", new ActionListener() {
            /** 把参数和网格都恢复成默认值，从头开始编。 */
            public void actionPerformed(ActionEvent event) {
                resetDesign();
            }
        }));
        toolbar.add(button("清空网格", new ActionListener() {
            /** 只清掉网格里的僵尸，参数保留。 */
            public void actionPerformed(ActionEvent event) {
                design.clearAllCells();
                designChanged();
            }
        }));
    }

    /**
     * 造一个按钮并挂上点击响应。
     *
     * 参数：text 是按钮上的字；listener 是点击后要做的事。
     * 返回：做好的按钮。
     */
    private static JButton button(String text, ActionListener listener) {
        JButton result = new JButton(text);
        result.addActionListener(listener);
        return result;
    }

    /**
     * 拼出左边那一栏：上面是僵尸列表，下面是操作提示。
     *
     * 返回：摆好的面板。
     */
    private JPanel buildPaletteSide() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBorder(BorderFactory.createTitledBorder("僵尸（拖到网格里）"));
        side.add(palette, BorderLayout.NORTH);

        JLabel hint = new JLabel("<html><body style='width:130px;padding:6px;color:#555'>"
            + "拖动僵尸放进格子<br>左右键点格子：选中<br>"
            + "Ctrl + 左键：数量 -1<br>Ctrl + 右键：数量 +1<br>"
            + "滚轮：快速增减<br>拖出网格：删除<br>Delete：清空该格<br>"
            + "R：切换随机行</body></html>");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));

        // 复选框和提示文字一起放在下面，上面留给僵尸列表。
        JPanel bottom = new JPanel(new BorderLayout());
        randomRowBox.setFont(randomRowBox.getFont().deriveFont(Font.PLAIN, 11f));
        randomRowBox.setBorder(BorderFactory.createEmptyBorder(4, 6, 0, 6));
        bottom.add(randomRowBox, BorderLayout.NORTH);
        bottom.add(hint, BorderLayout.CENTER);
        side.add(bottom, BorderLayout.CENTER);
        return side;
    }

    /**
     * 把出怪网格装进可滚动的区域，波数多了也能横着看。
     *
     * 返回：装好网格的滚动面板。
     */
    private JScrollPane buildGridArea() {
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(BorderFactory.createTitledBorder("出怪网格（横向是波次，纵向是草坪行）"));
        scroll.setPreferredSize(new Dimension(744, 548));
        scroll.getHorizontalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    /**
     * 拼出右边的参数面板。
     *
     * 返回：摆好各项参数的面板。
     */
    private JPanel buildSettingsSide() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("关卡参数"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 6, 4, 6);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        int gridRow = 0;
        gridRow = addField(panel, constraints, gridRow, "初始阳光", initialSunSpinner);
        gridRow = addField(panel, constraints, gridRow, "天空阳光间隔（秒）", skySunSpinner);
        gridRow = addField(panel, constraints, gridRow, "第一波延迟（秒）", firstWaveSpinner);
        gridRow = addField(panel, constraints, gridRow, "出怪间隔（秒）", waveIntervalSpinner);
        gridRow = addField(panel, constraints, gridRow, "同格僵尸间隔（毫秒）", spacingSpinner);
        gridRow = addField(panel, constraints, gridRow, "波数", waveCountSpinner);
        gridRow = addField(panel, constraints, gridRow, "背景", backgroundBox);
        gridRow = addField(panel, constraints, gridRow, "卡槽模式", barBox);

        gridRow = addTallField(panel, constraints, gridRow, "传送带 / 保龄球卡池", poolList);
        gridRow = addTallField(panel, constraints, gridRow, "禁用植物（正常选卡）", bannedList);
        gridRow = addTallField(panel, constraints, gridRow, "必选植物（正常选卡）", requiredList);
        return panel;
    }

    /**
     * 在参数面板上加一行"标签 + 清单"。
     *
     * 清单比普通控件高，所以单独一个方法，不和 addField 混在一起。
     *
     * 参数：panel 是参数面板；constraints 是摆放规则，会被改动后复用；
     *       gridRow 是摆在第几行；label 是左边的说明文字；list 是要摆的清单。
     * 返回：下一行的行号。
     */
    private static int addTallField(JPanel panel, GridBagConstraints constraints, int gridRow,
            String label, CheckListPanel list) {
        constraints.gridx = 0;
        constraints.gridy = gridRow;
        constraints.gridwidth = 2;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), constraints);

        constraints.gridy = gridRow + 1;
        constraints.fill = GridBagConstraints.BOTH;
        panel.add(list, constraints);
        return gridRow + 2;
    }

    /**
     * 在参数面板上加一行"标签 + 控件"。
     *
     * 参数：panel 是参数面板；constraints 是摆放规则，会被改动后复用；
     *       gridRow 是摆在第几行；label 是左边的说明文字；field 是右边的控件。
     * 返回：下一行的行号。
     */
    private static int addField(JPanel panel, GridBagConstraints constraints, int gridRow,
            String label, JComponent field) {
        constraints.gridx = 0;
        constraints.gridy = gridRow;
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        panel.add(new JLabel(label), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        field.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        panel.add(field, constraints);
        return gridRow + 1;
    }

    /**
     * 拼出底部状态栏。
     *
     * 返回：摆好状态文字的面板。
     */
    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
        statusBar.add(statusLabel, BorderLayout.WEST);
        return statusBar;
    }

    /**
     * 铺一层透明的玻璃面板，专门用来画拖动时跟着鼠标走的那只僵尸。
     *
     * 这只僵尸要能盖在所有面板上面，只有铺在最顶层才画得出来。
     * 但玻璃面板默认会把鼠标事件全挡下，所以让它的 contains 一律回答"点不在我身上"，
     * 事件就会照常落到底下的网格和列表上。
     */
    private void installGlassPane() {
        JComponent glass = new JComponent() {
            /** 一律回答"点不在我身上"，好让鼠标事件穿过去。 */
            public boolean contains(int x, int y) {
                return false;
            }

            /** 画出跟着鼠标走的那只僵尸。 */
            protected void paintComponent(Graphics graphics) {
                if (!dragging || dragPoint == null || dragKind == null) {
                    return;
                }
                Graphics2D painter = (Graphics2D) graphics.create();
                painter.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                painter.setComposite(
                    AlphaComposite.getInstance(AlphaComposite.SRC_OVER, DRAG_ICON_ALPHA));

                BufferedImage image = icons.icon(dragKind, DRAG_ICON_HEIGHT);
                int left = dragPoint.x - image.getWidth() / 2;
                int top = dragPoint.y - image.getHeight() / 2;
                painter.drawImage(image, left, top, null);

                if (dragCount > 1) {
                    drawDragCount(painter, image, left, top);
                }
                painter.dispose();
            }
        };
        glass.setOpaque(false);
        setGlassPane(glass);
        glass.setVisible(true);
    }

    /**
     * 在跟手的僵尸旁边标出正在搬几只。
     *
     * 参数：painter 是画笔；image 是僵尸图标；left、top 是图标的左上角。
     */
    private void drawDragCount(Graphics2D painter, BufferedImage image, int left, int top) {
        // 数字要看得清楚，所以把前面调低的透明度改回不透明。
        painter.setComposite(AlphaComposite.SrcOver);
        int badgeLeft = left + image.getWidth() - 8;
        int badgeTop = top + image.getHeight() - 16;
        painter.setColor(new Color(0xC2, 0x43, 0x2E));
        painter.fillRoundRect(badgeLeft, badgeTop, 34, 18, 8, 8);
        painter.setColor(Color.WHITE);
        painter.setFont(getFont().deriveFont(Font.BOLD, 12f));
        painter.drawString("× " + dragCount, badgeLeft + 5, badgeTop + 13);
    }

    /** 给所有参数控件挂上监听：用户一改，就把新值收进关卡数据。 */
    private void hookListeners() {
        ChangeListener onChange = new ChangeListener() {
            /** 数字框的值变了。 */
            public void stateChanged(ChangeEvent event) {
                readFields();
            }
        };
        initialSunSpinner.addChangeListener(onChange);
        skySunSpinner.addChangeListener(onChange);
        firstWaveSpinner.addChangeListener(onChange);
        waveIntervalSpinner.addChangeListener(onChange);
        spacingSpinner.addChangeListener(onChange);
        waveCountSpinner.addChangeListener(onChange);

        ActionListener onSelect = new ActionListener() {
            /** 下拉框选了别的项。 */
            public void actionPerformed(ActionEvent event) {
                readFields();
            }
        };
        backgroundBox.addActionListener(onSelect);
        barBox.addActionListener(onSelect);

        poolList.setOnChange(this::readFields);
        bannedList.setOnChange(this::readFields);
        requiredList.setOnChange(this::readFields);

        randomRowBox.addActionListener(new ActionListener() {
            /** 勾选框状态变了：改当前选中格子的随机行标记。 */
            public void actionPerformed(ActionEvent event) {
                if (updatingFields) {
                    return;
                }
                int row = grid.selectedRow();
                int wave = grid.selectedWave();
                if (row < 0) {
                    return;
                }
                design.setRandomRow(row, wave, randomRowBox.isSelected());
                designChanged();
                // 复选框会抢走焦点，交还给网格，否则 R 键等快捷键就失效了。
                grid.requestFocusInWindow();
            }
        });
    }

    /** 把各个控件里的值收进关卡数据，然后刷新画面。 */
    private void readFields() {
        if (updatingFields) {
            return;
        }
        design.initialSun = ((Number) initialSunSpinner.getValue()).intValue();
        design.skySunInterval = Math.max(Layout.MIN_SKY_SUN_INTERVAL,
            secondsToMilliseconds(skySunSpinner));
        design.firstWaveDelay = secondsToMilliseconds(firstWaveSpinner);
        design.waveInterval = Math.max(LevelDesign.MIN_WAVE_INTERVAL,
            secondsToMilliseconds(waveIntervalSpinner));
        design.spawnSpacing = Math.max(LevelDesign.MIN_SPAWN_SPACING,
            ((Number) spacingSpinner.getValue()).longValue());
        design.backgroundIndex = backgroundBox.getSelectedIndex();
        design.barType = barBox.getSelectedIndex();
        design.setWaveCount(((Number) waveCountSpinner.getValue()).intValue());

        design.cardPool.clear();
        design.cardPool.addAll(poolList.checkedIndices());
        design.bannedPlants.clear();
        design.bannedPlants.addAll(bannedList.checkedIndices());
        design.requiredPlants.clear();
        design.requiredPlants.addAll(requiredList.checkedIndices());
        updateListAvailability();

        grid.refresh();
        updateRandomRowBox();
        updateStatus();
    }

    /**
     * 按卡槽模式决定三份清单哪个能改。
     *
     * 卡池只有传送带和保龄球模式才看，禁用/必选清单反过来，只有正常选卡模式才看。
     * 不该看的就变灰，免得用户以为选了有用。
     */
    private void updateListAvailability() {
        boolean normal = design.barType == GameState.BAR_NORMAL;
        poolList.setEnabled(!normal);
        bannedList.setEnabled(normal);
        requiredList.setEnabled(normal);
    }

    /**
     * 让随机行复选框跟上当前选中的格子。
     *
     * 没选中格子、或者格子是空的，复选框就置灰——空格子谈不上随机行。
     */
    private void updateRandomRowBox() {
        int row = grid.selectedRow();
        int wave = grid.selectedWave();
        boolean usable = grid.hasCell(row, wave);
        updatingFields = true;
        randomRowBox.setEnabled(usable);
        randomRowBox.setSelected(usable && design.randomRowAt(row, wave));
        if (usable) {
            randomRowBox.setText("随机行出怪：" + design.cellSummary(row, wave));
        } else {
            randomRowBox.setText("选中格子随机行出怪");
        }
        updatingFields = false;
    }

    /**
     * 读一个按秒显示的数字框，换算成毫秒。
     *
     * 参数：spinner 是那个数字框。
     * 返回：对应的毫秒数。
     */
    private static long secondsToMilliseconds(JSpinner spinner) {
        double seconds = ((Number) spinner.getValue()).doubleValue();
        return Math.round(seconds * 1000);
    }

    /** 把关卡数据写进各个控件，打开别的关卡之后靠它把界面刷新成新关卡的样子。 */
    private void writeFields() {
        // 赋值会触发控件的监听器，先立起这个牌子，免得又被当成用户在改参数。
        updatingFields = true;

        initialSunSpinner.setValue(Integer.valueOf(design.initialSun));
        skySunSpinner.setValue(Double.valueOf(design.skySunInterval / 1000.0));
        firstWaveSpinner.setValue(Double.valueOf(design.firstWaveDelay / 1000.0));
        waveIntervalSpinner.setValue(Double.valueOf(design.waveInterval / 1000.0));
        spacingSpinner.setValue(Integer.valueOf((int) design.spawnSpacing));
        waveCountSpinner.setValue(Integer.valueOf(design.waveCount()));
        backgroundBox.setSelectedIndex(
            clampIndex(design.backgroundIndex, LevelDesign.BACKGROUND_LABELS.length));
        barBox.setSelectedIndex(clampIndex(design.barType, LevelDesign.BAR_LABELS.length));

        poolList.setChecked(design.cardPool);
        bannedList.setChecked(design.bannedPlants);
        requiredList.setChecked(design.requiredPlants);
        updateListAvailability();

        updatingFields = false;
        updateRandomRowBox();
    }

    /**
     * 把下标夹回合法范围，免得关卡文件里写了越界的数字导致下拉框报错。
     *
     * 参数：value 是文件里读到的下标；size 是下拉框一共几项。
     * 返回：夹好的下标。
     */
    private static int clampIndex(int value, int size) {
        if (value < 0) {
            return 0;
        }
        if (value >= size) {
            return size - 1;
        }
        return value;
    }

    /** 刷新底部状态栏：统计数字加上需要提醒的话。 */
    private void updateStatus() {
        String text = "共 " + design.totalZombies() + " 只僵尸";
        text = text + " · " + design.waveCount() + " 波";
        long lastWaveTime = design.waveTime(design.waveCount() - 1);
        text = text + " · 最后一波 " + LevelDesign.formatTime(lastWaveTime);

        if (!design.importNote.isEmpty()) {
            text = text + " · 导入提示：" + design.importNote;
        }
        String warning = design.validate();
        if (!warning.isEmpty()) {
            text = text + " · 注意：" + warning;
        }
        statusLabel.setText(text);
    }

    /** 网格内容被改动了，重画一遍并更新统计。 */
    public void designChanged() {
        grid.refresh();
        updateRandomRowBox();
        updateStatus();
    }

    /** 用户换了选中的格子，把随机行复选框同步成那个格子的状态。 */
    public void selectionChanged() {
        updateRandomRowBox();
    }

    /**
     * 查询僵尸列表里选中的品种。
     *
     * 返回：僵尸品种名。
     */
    public String selectedKind() {
        return selectedKind;
    }

    /**
     * 记下用户在僵尸列表里选了哪一种。
     *
     * 参数：kind 是僵尸品种名。
     */
    public void selectKind(String kind) {
        selectedKind = kind;
    }

    /**
     * 查询此刻是不是正在拖动。
     *
     * 返回：正在拖动就返回真。
     */
    public boolean isDragging() {
        return dragging;
    }

    /**
     * 一次拖动开始了：记下拖的是什么、从哪来。
     *
     * 参数：kind 是僵尸品种；count 是只数；fromRow、fromWave 是源格子（来自僵尸列表时是 -1）；
     *       screenPoint 是鼠标在屏幕上的位置。
     */
    public void beginDrag(String kind, int count, int fromRow, int fromWave, Point screenPoint) {
        dragging = true;
        dragKind = kind;
        dragCount = Math.max(1, count);
        dragFromRow = fromRow;
        dragFromWave = fromWave;
        grid.setDragSource(fromRow, fromWave);
        updateDrag(screenPoint);
    }

    /**
     * 拖动中鼠标动了：挪动跟手的僵尸，并高亮此刻会落进的格子。
     *
     * 参数：screenPoint 是鼠标在屏幕上的位置。
     */
    public void updateDrag(Point screenPoint) {
        dragPoint = toGlassPoint(screenPoint);

        int[] cell = cellAtScreen(screenPoint);
        if (cell == null) {
            grid.setDropTarget(-1, -1);
        } else {
            grid.setDropTarget(cell[0], cell[1]);
        }
        getGlassPane().repaint();
    }

    /**
     * 拖动结束：落在格子上就放进去，落在网格外面就当成把它扔掉。
     *
     * 参数：screenPoint 是松手时鼠标在屏幕上的位置。
     */
    public void finishDrag(Point screenPoint) {
        int[] cell = cellAtScreen(screenPoint);
        if (cell != null) {
            dropInto(cell[0], cell[1]);
        } else if (dragFromRow >= 0) {
            design.clearCell(dragFromRow, dragFromWave);
        }
        clearDragState();
        designChanged();
    }

    /**
     * 把拖着的东西放进某个格子。
     *
     * 从网格里拖来的：目标空着就搬过去，目标有东西就两格对调，这样谁都不会丢。
     * 从僵尸列表拖来的：同品种就加一只，不同品种就换成新的。
     *
     * 参数：row 是行号；wave 是波号。
     */
    private void dropInto(int row, int wave) {
        if (dragFromRow >= 0) {
            if (design.kindAt(row, wave) == null) {
                design.moveCell(dragFromRow, dragFromWave, row, wave);
            } else {
                design.swapCells(dragFromRow, dragFromWave, row, wave);
            }
            return;
        }
        if (dragKind.equals(design.kindAt(row, wave))) {
            design.addCount(row, wave, 1);
        } else {
            design.setCell(row, wave, dragKind, 1);
        }
    }

    /** 收拾掉这一次拖动留下的所有痕迹。 */
    private void clearDragState() {
        dragging = false;
        dragKind = null;
        dragCount = 0;
        dragFromRow = -1;
        dragFromWave = -1;
        dragPoint = null;
        grid.setDragSource(-1, -1);
        grid.setDropTarget(-1, -1);
        getGlassPane().repaint();
    }

    /**
     * 把屏幕坐标换算成玻璃面板里的坐标。
     *
     * 参数：screenPoint 是鼠标在屏幕上的位置。
     * 返回：玻璃面板里的对应位置。
     */
    private Point toGlassPoint(Point screenPoint) {
        // convertPointFromScreen 会直接改传进去的那个点，所以先复制一份再交给它。
        Point local = new Point(screenPoint);
        SwingUtilities.convertPointFromScreen(local, getGlassPane());
        return local;
    }

    /**
     * 看看屏幕上的某个点落在网格的哪个格子里。
     *
     * 参数：screenPoint 是鼠标在屏幕上的位置。
     * 返回：两个元素的数组，依次是行号和波号；没落在格子上就返回 null。
     */
    private int[] cellAtScreen(Point screenPoint) {
        // 网格还没显示出来时谈不上屏幕坐标，直接当成没落在格子上。
        if (!grid.isShowing()) {
            return null;
        }
        Point local = new Point(screenPoint);
        SwingUtilities.convertPointFromScreen(local, grid);
        return grid.cellAt(local);
    }

    /** 丢掉当前内容，换成一个各项都是默认值的空关卡。 */
    private void resetDesign() {
        applyDesign(new LevelDesign());
        statusLabel.setText("已新建空关卡。");
    }

    /**
     * 换一个关卡来编辑，网格和各个控件都跟着换。
     *
     * 参数：newDesign 是新的关卡数据。
     */
    private void applyDesign(LevelDesign newDesign) {
        design = newDesign;
        grid.setDesign(design);
        writeFields();
        updateStatus();
    }

    /**
     * 读出当前选的关卡编号。
     *
     * 返回：关卡编号。
     */
    private int levelNumber() {
        return ((Number) levelSpinner.getValue()).intValue();
    }

    /** 按关卡编号找到文件并读进来。 */
    private void openLevelByNumber() {
        openPath(levelPath(levelNumber()));
    }

    /**
     * 拼出某一关的文件位置。
     *
     * 参数：number 是关卡编号。
     * 返回：该关卡文件的路径。
     */
    private Path levelPath(int number) {
        return assetRoot.resolve("levels/level_" + number + ".json");
    }

    /**
     * 判断某一关是否可以直接保存。
     *
     * 只有当关卡编号在现有关卡范围内（即文件已存在，或者是紧接着最后一关的下一关）时才能直接保存。
     * 如果用户只有 0~5 关，那么可以编辑 0~6 关，但不能直接保存到第 7 关及以后。
     *
     * 参数：number 是关卡编号。
     * 返回：可以直接保存就返回真。
     */
    private boolean canEditLevel(int number) {
        // 找出现有关卡的最大编号
        int maxExisting = -1;
        try {
            Path levelsDir = assetRoot.resolve("levels");
            if (Files.isDirectory(levelsDir)) {
                for (int i = 0; i <= 99; i++) {
                    if (Files.exists(levelPath(i))) {
                        maxExisting = i;
                    }
                }
            }
        } catch (Exception exception) {
            // 出错就允许保存，不挡住用户
            return true;
        }
        // 允许编辑现有关卡和紧接着的下一关
        return number <= maxExisting + 1;
    }

    /** 弹出文件对话框，让用户自己挑一个关卡文件打开。 */
    private void openFromChooser() {
        JFileChooser chooser = new JFileChooser(assetRoot.resolve("levels").toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("关卡文件 (*.json)", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        openPath(chooser.getSelectedFile().toPath());
    }

    /**
     * 读入指定位置的关卡文件。
     *
     * 参数：path 是关卡文件位置。
     */
    private void openPath(Path path) {
        if (!Files.isRegularFile(path)) {
            JOptionPane.showMessageDialog(this, "找不到关卡文件：\n" + path,
                "打开失败", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            applyDesign(LevelDesign.load(path));
            statusLabel.setText("已读取 " + path.getFileName() + " · " + statusLabel.getText());
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "读取失败：\n" + exception.getMessage(),
                "打开失败", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException exception) {
            // 文件能读但内容不对（缺字段、类型不符）时 Gson 抛的是运行时异常，一并接住。
            JOptionPane.showMessageDialog(this, "关卡文件格式不对：\n" + exception.getMessage(),
                "打开失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 按关卡编号把当前内容写回文件。
     *
     * 返回：真的存下去了就返回真；用户中途反悔或出错就返回假。
     */
    private boolean saveLevelByNumber() {
        return saveTo(levelPath(levelNumber()));
    }

    /** 弹出文件对话框，让用户自己挑一个位置存盘。 */
    private void saveWithChooser() {
        JFileChooser chooser = new JFileChooser(assetRoot.resolve("levels").toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("关卡文件 (*.json)", "json"));
        chooser.setSelectedFile(levelPath(levelNumber()).toFile());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        saveTo(chooser.getSelectedFile().toPath());
    }

    /**
     * 把当前内容写到指定文件，写之前先提醒两件事。
     *
     * 一是关卡本身有没有毛病，二是会不会覆盖掉已有的文件，两处都要用户点头才继续。
     *
     * 参数：path 是要写到哪里。
     * 返回：真的存下去了就返回真。
     */
    private boolean saveTo(Path path) {
        String warning = design.validate();
        if (!warning.isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(this, warning + "\n\n仍然保存吗？",
                "关卡检查", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        if (Files.exists(path)) {
            int answer = JOptionPane.showConfirmDialog(this,
                "文件已存在，保存会覆盖它：\n" + path + "\n\n继续吗？",
                "覆盖确认", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        try {
            design.save(path);
            statusLabel.setText("已保存到 " + path + "（共 " + design.totalZombies() + " 只僵尸）");
            return true;
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "保存失败：\n" + exception.getMessage(),
                "保存失败", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** 先存盘，再开一个窗口直接跑这一关，方便边改边试。 */
    private void playCurrentLevel() {
        if (!saveLevelByNumber()) {
            return;
        }
        final Game game = new Game(assets, levelNumber());
        JFrame window = new JFrame("试玩：第 " + levelNumber() + " 关");
        // 试玩窗口关掉只是收起这一局，编辑器还要继续用，所以不能设成退出程序。
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setContentPane(game);
        window.pack();
        window.setResizable(false);
        window.setLocationRelativeTo(this);

        // 光把窗口关掉，这一局还会在背地里接着跑，所以要亲手把它停下来。
        // 不然试玩几次之后，就有好几局同时占着处理器。
        window.addWindowListener(new WindowAdapter() {
            /** 窗口关闭时结束这一局。 */
            public void windowClosed(WindowEvent event) {
                game.stop();
            }
        });

        window.setVisible(true);

        // 直接载入关卡，跳过主菜单那一步。
        game.loadLevel();
        game.requestFocusInWindow();
    }

    /**
     * 打开编辑器窗口。
     *
     * 参数：assets 提供图片素材；root 是 assets 目录；levelNumber 是一开始编辑第几关。
     */
    public static void open(final Assets assets, final Path root, final int levelNumber) {
        // Swing 规定：创建窗口必须在事件线程里做，所以包一层 Runnable 交过去。
        SwingUtilities.invokeLater(new Runnable() {
            /** 在 Swing 事件线程里创建并显示编辑器窗口。 */
            public void run() {
                useSystemLookAndFeel();
                LevelEditor editor = new LevelEditor(assets, root, levelNumber);
                editor.setVisible(true);
            }
        });
    }

    /**
     * 让按钮、输入框这些控件用上当前系统的外观。
     *
     * Swing 默认用的是自带的那一套跨平台外观，摆在 Windows 上一眼就看得出不是本地程序。
     * 换成系统外观之后，控件和文件对话框都会跟系统保持一致。
     * 必须赶在创建任何控件之前换，换晚了已经造好的控件不会跟着变。
     */
    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            useInterfaceFont();
        } catch (Exception failure) {
            // 换不成就照默认外观用，样子丑一点而已，不值得为此打不开编辑器。
            System.out.println("套用系统外观失败，改用默认外观：" + failure.getMessage());
        }
    }

    /**
     * 把界面文字换成系统界面字体。
     *
     * 系统外观在中文 Windows 上默认用宋体，字形偏旧，和现在的窗口凑在一起不协调。
     * 换成微软雅黑就跟系统自带的程序一致了。万一这台机器没装，就保持原样不动。
     */
    private static void useInterfaceFont() {
        String preferred = "Microsoft YaHei UI";
        if (!isFontInstalled(preferred)) {
            return;
        }

        // 外观的设置表里有几十项字体（按钮的、标签的、菜单的……），逐项换掉才不会漏。
        Font font = new Font(preferred, Font.PLAIN, INTERFACE_FONT_SIZE);
        List<Object> keys = new ArrayList<Object>(UIManager.getLookAndFeelDefaults().keySet());
        for (int index = 0; index < keys.size(); index++) {
            Object key = keys.get(index);
            if (UIManager.get(key) instanceof Font) {
                UIManager.put(key, font);
            }
        }
    }

    /**
     * 查这台机器上装没装某个字体。
     *
     * 参数：name 是字体名。
     * 返回：装了就返回真。
     */
    private static boolean isFontInstalled(String name) {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] installed = environment.getAvailableFontFamilyNames();
        for (int index = 0; index < installed.length; index++) {
            if (installed[index].equals(name)) {
                return true;
            }
        }
        return false;
    }
}
