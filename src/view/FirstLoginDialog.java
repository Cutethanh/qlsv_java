package qlsv.view;

import qlsv.controller.StudentController;
import qlsv.model.Student;
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

public class FirstLoginDialog extends WindowView {
    private static final long serialVersionUID = 1L;

    private final StudentController ctr;
    private final Student ngDung;
    private JTextField oHoTen;
    private JTextField oDiaChi;
    private JTextField oCccd;
    private JTextField oSdt;
    private JTextField oMail;
    private JPasswordField oPass;
    private JPasswordField oPass2;
    private boolean checkSua = false;

    public FirstLoginDialog(Component parent, StudentController controller, Student student) {
        super("Cập nhật lần đầu", 450, 510, parent, true);
        this.ctr = controller;
        this.ngDung = student;
        disableCloseButton();
        initUI();
    }

    private void initUI() {
        JLabel title = new JLabel("<html><body style='width:290px'><b>Kích hoạt tài khoản " + ngDung.getUsername()
                + "</b><br>Cập nhật thông tin và thay mật khẩu tạm do phòng đào tạo cấp.</body></html>");
        title.setBorder(BorderFactory.createEmptyBorder(14, 20, 0, 20));
        root.add(title, BorderLayout.NORTH);

        JPanel p = new JPanel(new GridLayout(8, 2, 10, 12));
        p.setBorder(BorderFactory.createEmptyBorder(14, 20, 6, 20));
        p.add(new JLabel("Họ và tên:"));
        oHoTen = new JTextField(ngDung.getFullName());
        p.add(oHoTen);
        p.add(new JLabel("Địa chỉ:"));
        oDiaChi = new JTextField();
        p.add(oDiaChi);
        p.add(new JLabel("CCCD (12 số):"));
        oCccd = new JTextField();
        p.add(oCccd);
        p.add(new JLabel("Số điện thoại:"));
        oSdt = new JTextField();
        p.add(oSdt);
        p.add(new JLabel("Email:"));
        oMail = new JTextField();
        p.add(oMail);
        p.add(new JLabel("Mật khẩu mới:"));
        oPass = new JPasswordField();
        p.add(oPass);
        p.add(new JLabel("Nhập lại mật khẩu:"));
        oPass2 = new JPasswordField();
        p.add(oPass2);
        JButton btHuy = new JButton("Huỷ");
        JButton btLuu = new JButton("Lưu lại & Vào app");
        p.add(btHuy);
        p.add(btLuu);
        root.add(p, BorderLayout.CENTER);

        JLabel note = UiUtils.note(InputValidator.PASSWORD_RULE
                + "<br>CCCD được mã hoá AES-256 trước khi lưu.", 290);
        note.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));
        root.add(note, BorderLayout.SOUTH);

        btHuy.addActionListener(e -> dispose());
        btLuu.addActionListener(e -> save());
    }

    private void save() {
        char[] p1 = oPass.getPassword();
        char[] p2 = oPass2.getPassword();
        try {
            ctr.completeFirstLogin(ngDung, oHoTen.getText(), oDiaChi.getText(), oCccd.getText(), oSdt.getText(),
                    oMail.getText(), p1, p2);
            checkSua = true;
            JOptionPane.showMessageDialog(root, "Đã kích hoạt tài khoản. Chào mừng " + ngDung.getFullName() + "!");
            dispose();
        } catch (ValidationException ex) {
            UiUtils.error(root, ex.getMessage());
        } finally {
            Arrays.fill(p1, '\0');
            Arrays.fill(p2, '\0');
        }
    }

    public boolean isUpdated() {
        return checkSua;
    }
}
