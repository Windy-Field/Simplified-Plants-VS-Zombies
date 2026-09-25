package pvz.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * 一列可以打勾的植物清单。
 *
 * 参数面板里要摆卡池、禁用清单、必选清单三份名单，用 JList 要多选得按住 Ctrl，
 * 而且勾没勾上不如一个方框直观，所以改用一排复选框：点一下就切换，
 * 不用记快捷键，一眼也能看出哪些已经勾上。
 */
public class CheckListPanel extends JPanel {
    /** 每个复选框占的高度。 */
    private static final int ROW_HEIGHT = 22;

    /** 一列复选框装进滚动区时默认显示多高。 */
    private static final int VIEW_HEIGHT = 118;

    /** 每一项的文字和它对应的编号。 */
    private final List<JCheckBox> boxes = new ArrayList<JCheckBox>();

    /** 勾选状态变了要通知谁。 */
    private Runnable onChange;

    /** 有没有在回填勾选状态，回填时不要触发 onChange。 */
    private boolean notifyOnChange = true;

    /**
     * 创建一列复选框。
     *
     * 参数：labels 是每一项的文字，下标就是这一项的编号。
     */
    public CheckListPanel(String[] labels) {
        setLayout(new BorderLayout());

        // 复选框竖着排；Box 上下的胶水把它们顶到顶部，项少时不会散开。
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBackground(Color.WHITE);
        column.add(Box.createVerticalGlue());

        Font font = getFont().deriveFont(Font.PLAIN, 12f);
        for (int index = 0; index < labels.length; index++) {
            JCheckBox box = new JCheckBox(labels[index]);
            box.setFont(font);
            box.setBackground(Color.WHITE);
            box.setPreferredSize(new Dimension(0, ROW_HEIGHT));
            box.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
            box.setAlignmentX(Component.LEFT_ALIGNMENT);
            box.addActionListener(new ActionListener() {
                /** 用户点了某一项，把整份勾选结果报上去。 */
                public void actionPerformed(ActionEvent event) {
                    fireChange();
                }
            });
            boxes.add(box);
            column.add(box);
        }
        column.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(column);
        scroll.setPreferredSize(new Dimension(230, VIEW_HEIGHT));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0xB0, 0xB0, 0xB0)));
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * 把一份清单勾到复选框上。
     *
     * 参数：chosen 是要勾上的编号。
     */
    public void setChecked(List<Integer> chosen) {
        for (int index = 0; index < boxes.size(); index++) {
            boolean on = chosen.contains(Integer.valueOf(index));
            // 赋值会触发监听器，用标志挡住，免得又被当成用户在点。
            if (boxes.get(index).isSelected() != on) {
                boolean keep = notifyOnChange;
                notifyOnChange = false;
                boxes.get(index).setSelected(on);
                notifyOnChange = keep;
            }
        }
    }

    /**
     * 把当前勾上的项收成一份清单。
     *
     * 返回：按编号从小到大排好的清单。
     */
    public List<Integer> checkedIndices() {
        List<Integer> result = new ArrayList<Integer>();
        for (int index = 0; index < boxes.size(); index++) {
            if (boxes.get(index).isSelected()) {
                result.add(Integer.valueOf(index));
            }
        }
        return result;
    }

    /**
     * 设置勾选变化时的回调。
     *
     * 参数：listener 是回调；点一项就调一次。
     */
    public void setOnChange(Runnable listener) {
        onChange = listener;
    }

    /** 通知外面勾选变了。 */
    private void fireChange() {
        if (onChange != null && notifyOnChange) {
            onChange.run();
        }
    }

    /**
     * 整块面板一起启用或禁用。
     *
     * 参数：enabled 为真表示可以改。
     */
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (int index = 0; index < boxes.size(); index++) {
            boxes.get(index).setEnabled(enabled);
        }
    }
}
