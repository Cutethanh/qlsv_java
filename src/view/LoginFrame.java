package qlsv.view;

import qlsv.controller.StudentController;
import qlsv.model.Admin;
import qlsv.model.Student;
import qlsv.model.User;
import qlsv.security.AuthException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;

public class LoginFrame extends WindowView {
    private static final long serialVersionUID = 1L;

    private final StudentController ctr;
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnVao;
    private JButton btnQuenPass;

    public LoginFrame(StudentController c) {
        this(c, null);
    }

    public LoginFrame(StudentController c, String notice) {
        super("QLSV", 400, 230, null, false);
        this.ctr = c;
        initUI(notice);
    }

    private void initUI(String notice) {
        JPanel p = new JPanel(new GridLayout(3, 2, 10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 8, 20));

        p.add(new JLabel("Tên đăng nhập:"));
        txtUser = new JTextField();
        p.add(txtUser);

        p.add(new JLabel("Mật khẩu:"));
        txtPass = new JPasswordField();
        p.add(txtPass);

        btnQuenPass = new JButton("<html><u>Quên mật khẩu?</u></html>");
        btnQuenPass.setBorderPainted(false);
        btnQuenPass.setContentAreaFilled(false);
        btnQuenPass.setFocusPainted(false);
        btnQuenPass.setForeground(Color.DARK_GRAY);
        btnQuenPass.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel panelQuenPass = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelQuenPass.add(btnQuenPass);
        p.add(panelQuenPass);

        btnVao = new JButton("Đăng nhập ngay");
        p.add(btnVao);
        root.add(p, BorderLayout.CENTER);

        JLabel status = new JLabel(notice != null ? notice
                : "Tài khoản bị khoá " + StudentController.LOCK_MINUTES + " phút nếu sai mật khẩu "
                + StudentController.MAX_FAILED_ATTEMPTS + " lần liên tiếp.", SwingConstants.CENTER);
        status.setFont(status.getFont().deriveFont(Font.PLAIN, 11f));
        status.setForeground(notice != null ? new Color(170, 40, 40) : Color.GRAY);
        status.setBorder(BorderFactory.createEmptyBorder(0, 10, 12, 10));
        root.add(status, BorderLayout.SOUTH);

        setDefaultButton(btnVao);
        btnVao.addActionListener(e -> handleLogin());
        btnQuenPass.addActionListener(e -> new ForgotPasswordDialog(root, ctr).setVisible(true));
    }

    private void handleLogin() {
        String u = txtUser.getText().trim();
        char[] p = txtPass.getPassword();
        try {
            if (u.isEmpty() || p.length == 0) {
                JOptionPane.showMessageDialog(root, "Vui lòng nhập đủ thông tin!");
                return;
            }
            User user = ctr.authenticate(u, p);
            openAfterLogin(user);
        } catch (AuthException ex) {
            if (ex.getReason() == AuthException.Reason.LOCKED) {
                JOptionPane.showMessageDialog(root, lockedMessage(ex.getMinutesLeft()),
                        "Tài khoản bị khoá", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(root, "Tài khoản hoặc mật khẩu không đúng!", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
            }
        } finally {
            Arrays.fill(p, '\0');
            txtPass.setText("");
        }
    }

    public static String lockedMessage(long minutes) {
        return "Tài khoản đang bị tạm khoá do nhập sai nhiều lần.\n"
                + "Vui lòng thử lại sau " + minutes + " phút hoặc liên hệ quản trị viên.";
    }

    void openAfterLogin(User user) {
        if (user instanceof Student && ((Student) user).isFirstLogin()) {
            FirstLoginDialog dl = new FirstLoginDialog(root, ctr, (Student) user);
            dl.setVisible(true);
            if (!dl.isUpdated()) {
                ctr.logout(user, false);
                return;
            }
        } else if (user.isMustChangePassword()) {
            JOptionPane.showMessageDialog(root, "Mật khẩu của bạn vừa được quản trị viên đặt lại.\n"
                    + "Vui lòng đổi mật khẩu mới trước khi tiếp tục.");
            ChangePasswordDialog d = new ChangePasswordDialog(root, ctr, user, true);
            d.setVisible(true);
            if (!d.isChanged()) {
                ctr.logout(user, false);
                return;
            }
        }
        WindowView next = user.isAdmin() ? new AdminFrame(ctr, (Admin) user) : new StudentFrame(ctr, (Student) user);
        next.setVisible(true);
        dispose();
    }
}
