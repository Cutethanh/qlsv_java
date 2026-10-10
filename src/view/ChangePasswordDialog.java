package qlsv.view;

import qlsv.controller.StudentController;
import qlsv.model.User;
import qlsv.security.AuthException;
import qlsv.security.InputValidator;
import qlsv.security.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Arrays;

public class ChangePasswordDialog extends WindowView {
    private static final long serialVersionUID = 1L;

    private final StudentController ctr;
    private final User user;
    private JPasswordField oCu;
    private JPasswordField oMoi;
    private JPasswordField oMoi2;
    private boolean changed = false;

    public ChangePasswordDialog(Component parent, StudentController controller, User user, boolean forced) {
        super(forced ? "Bắt buộc đổi mật khẩu" : "Đổi mật khẩu", 440, 285, parent, true);
        this.ctr = controller;
        this.user = user;
        initUI();
    }

    private void initUI() {
        JPanel p = new JPanel(new GridLayout(4, 2, 10, 12));
        p.setBorder(BorderFactory.createEmptyBorder(18, 20, 6, 20));
        p.add(new JLabel("Mật khẩu hiện tại:"));
        oCu = new JPasswordField();
        p.add(oCu);
        p.add(new JLabel("Mật khẩu mới:"));
        oMoi = new JPasswordField();
        p.add(oMoi);
        p.add(new JLabel("Nhập lại mật khẩu mới:"));
        oMoi2 = new JPasswordField();
        p.add(oMoi2);
        JButton btHuy = new JButton("Huỷ");
        JButton btDoi = new JButton("Đổi mật khẩu");
        p.add(btHuy);
        p.add(btDoi);
        root.add(p, BorderLayout.CENTER);

        JLabel note = UiUtils.note(InputValidator.PASSWORD_RULE, 280);
        note.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        root.add(note, BorderLayout.SOUTH);

        setDefaultButton(btDoi);
        btHuy.addActionListener(e -> dispose());
        btDoi.addActionListener(e -> submit());
    }

    private void submit() {
        char[] cu = oCu.getPassword();
        char[] moi = oMoi.getPassword();
        char[] moi2 = oMoi2.getPassword();
        try {
            ctr.changePassword(user, cu, moi, moi2);
            changed = true;
            JOptionPane.showMessageDialog(root, "Đổi mật khẩu thành công.");
            dispose();
        } catch (AuthException ex) {
            UiUtils.error(root, "Mật khẩu hiện tại không đúng!");
        } catch (ValidationException ex) {
            UiUtils.error(root, ex.getMessage());
        } finally {
            Arrays.fill(cu, '\0');
            Arrays.fill(moi, '\0');
            Arrays.fill(moi2, '\0');
        }
    }

    public boolean isChanged() {
        return changed;
    }
}
