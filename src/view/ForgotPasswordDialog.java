package qlsv.view;

import qlsv.controller.StudentController;
import qlsv.security.AuthException;
import qlsv.security.InputValidator;
import qlsv.security.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Arrays;

public class ForgotPasswordDialog extends WindowView {
    private static final long serialVersionUID = 1L;

    private final StudentController ctr;
    private JTextField oUser;
    private JTextField oEmail;
    private JTextField oCccd4;
    private JPasswordField oPass;
    private JPasswordField oPass2;

    public ForgotPasswordDialog(Component parent, StudentController controller) {
        super("Quên mật khẩu", 440, 360, parent, true);
        this.ctr = controller;
        initUI();
    }

    private void initUI() {
        JPanel p = new JPanel(new GridLayout(6, 2, 10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 6, 20));
        p.add(new JLabel("Tên đăng nhập:"));
        oUser = new JTextField();
        p.add(oUser);
        p.add(new JLabel("Email đã đăng ký:"));
        oEmail = new JTextField();
        p.add(oEmail);
        p.add(new JLabel("4 số cuối CCCD:"));
        oCccd4 = new JTextField();
        p.add(oCccd4);
        p.add(new JLabel("Mật khẩu mới:"));
        oPass = new JPasswordField();
        p.add(oPass);
        p.add(new JLabel("Nhập lại mật khẩu:"));
        oPass2 = new JPasswordField();
        p.add(oPass2);
        JButton btHuy = new JButton("Huỷ");
        JButton btXacMinh = new JButton("Xác minh & Đặt lại");
        p.add(btHuy);
        p.add(btXacMinh);
        root.add(p, BorderLayout.CENTER);

        JLabel note = UiUtils.note(InputValidator.PASSWORD_RULE
                + "<br>Xác minh sai 5 lần sẽ khoá tài khoản 15 phút.", 280);
        note.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        root.add(note, BorderLayout.SOUTH);

        setDefaultButton(btXacMinh);
        btHuy.addActionListener(e -> dispose());
        btXacMinh.addActionListener(e -> submit());
    }

    private void submit() {
        char[] p1 = oPass.getPassword();
        char[] p2 = oPass2.getPassword();
        try {
            ctr.resetForgottenPassword(oUser.getText(), oEmail.getText(), oCccd4.getText(), p1, p2);
            JOptionPane.showMessageDialog(root, "Khôi phục mật khẩu thành công! Vui lòng đăng nhập lại.");
            dispose();
        } catch (AuthException ex) {
            if (ex.getReason() == AuthException.Reason.LOCKED) {
                JOptionPane.showMessageDialog(root, LoginFrame.lockedMessage(ex.getMinutesLeft()),
                        "Tài khoản bị khoá", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(root, "Thông tin xác minh không khớp!", "Lỗi xác minh",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (ValidationException ex) {
            UiUtils.error(root, ex.getMessage());
        } finally {
            Arrays.fill(p1, '\0');
            Arrays.fill(p2, '\0');
        }
    }
}
