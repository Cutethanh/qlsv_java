package qlsv.view;

import qlsv.model.LogLevel;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.regex.Pattern;

public final class UiUtils {
    public static final Color HEADER_BG = new Color(245, 245, 245);
    public static final Color WARNING_BG = new Color(255, 243, 205);
    public static final Color ALERT_BG = new Color(248, 215, 218);

    private UiUtils() {
    }

    public static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static void attachSearch(JTextField field, TableRowSorter<DefaultTableModel> sorter) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String text = field.getText().trim();
                sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
            }

            @Override
            public void insertUpdate(DocumentEvent e) { update(); }

            @Override
            public void removeUpdate(DocumentEvent e) { update(); }

            @Override
            public void changedUpdate(DocumentEvent e) { update(); }
        });
    }

    public static char[] askPassword(Component parent, String message) {
        JPasswordField field = new JPasswordField(18);
        Object[] content = {message, field};
        JOptionPane pane = new JOptionPane(content, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        javax.swing.JDialog dialog = pane.createDialog(parent, "Xác thực lại");
        dialog.addWindowFocusListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent e) {
                field.requestFocusInWindow();
            }
        });
        dialog.setVisible(true);
        dialog.dispose();
        Object value = pane.getValue();
        if (value instanceof Integer && (Integer) value == JOptionPane.OK_OPTION) {
            return field.getPassword();
        }
        return null;
    }

    public static JLabel note(String html, int width) {
        return hint("<html><body style='width:" + width + "px'>" + html + "</body></html>");
    }

    public static void localizeOptionPane() {
        javax.swing.UIManager.put("OptionPane.yesButtonText", "Có");
        javax.swing.UIManager.put("OptionPane.noButtonText", "Không");
        javax.swing.UIManager.put("OptionPane.cancelButtonText", "Huỷ");
        javax.swing.UIManager.put("OptionPane.okButtonText", "OK");
    }

    public static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setForeground(Color.GRAY);
        return label;
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static void colorRowsByLevel(JTable table, int levelColumn) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                if (!selected) {
                    Object level = t.getModel().getValueAt(t.convertRowIndexToModel(row), levelColumn);
                    if (level == LogLevel.ALERT || "Thất bại".equals(level)) {
                        c.setBackground(ALERT_BG);
                    } else if (level == LogLevel.WARNING) {
                        c.setBackground(WARNING_BG);
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
    }

    public static void columnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    public static void padding(JComponent c, int top, int left, int bottom, int right) {
        c.setBorder(javax.swing.BorderFactory.createEmptyBorder(top, left, bottom, right));
    }
}
