package qlsv.view;

import qlsv.controller.StudentController;
import qlsv.model.Admin;
import qlsv.security.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;

public class AddStudentDialog extends WindowView {
    private static final long serialVersionUID = 1L;

    private final StudentController ctr;
    private final Admin admin;
    private JTextField nhapMSV;
    private JTextField nhapTen;
    private JComboBox<String> chonLop;
    private JTextField nhapUser;
    private boolean trangThaiThem = false;

    public AddStudentDialog(Component parent, StudentController controller, Admin admin) {
        super("Form Thêm Sinh Viên", 420, 315, parent, true);
        this.ctr = controller;
        this.admin = admin;
        initUI();
    }

    private void initUI() {
        JPanel p = new JPanel(new GridLayout(5, 2, 8, 10));
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        p.add(new JLabel("Mã sinh viên (SVxxx):"));
        nhapMSV = new JTextField();
        p.add(nhapMSV);
        p.add(new JLabel("Họ và tên:"));
        nhapTen = new JTextField();
        p.add(nhapTen);
        p.add(new JLabel("Lớp:"));
        chonLop = new JComboBox<>(ctr.getClassNames().toArray(new String[0]));
        chonLop.setEditable(true);
        p.add(chonLop);
        p.add(new JLabel("Tên đăng nhập:"));
        nhapUser = new JTextField();
        p.add(nhapUser);
        JButton btThoat = new JButton("Đóng lại");
        JButton btLuu = new JButton("Lưu lại");
        p.add(btThoat);
        p.add(btLuu);
        root.add(p, BorderLayout.CENTER);

        JLabel note = UiUtils.note("Mật khẩu tạm do hệ thống sinh ngẫu nhiên; sinh viên phải đổi"
                + " và cập nhật thông tin ở lần đăng nhập đầu tiên.", 270);
        note.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        root.add(note, BorderLayout.SOUTH);

        btThoat.addActionListener(e -> dispose());
        btLuu.addActionListener(e -> save());
    }

    private void save() {
        String lop = chonLop.getSelectedItem() == null ? "" : chonLop.getSelectedItem().toString();
        try {
            String temp = ctr.addStudent(admin, nhapMSV.getText(), nhapTen.getText(), lop, nhapUser.getText());
            trangThaiThem = true;
            showTempPassword(root, nhapUser.getText().trim(), temp);
            dispose();
        } catch (ValidationException ex) {
            UiUtils.error(root, ex.getMessage());
        }
    }

    static void showTempPassword(Component parent, String username, String temp) {
        JOptionPane.showMessageDialog(parent, tempPasswordMessage(username, temp), "Cấp mật khẩu tạm",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static Object[] tempPasswordMessage(String username, String temp) {
        JTextField f = new JTextField(temp);
        f.setEditable(false);
        f.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        return new Object[]{
            "Cấp tài khoản " + username + " thành công!",
            "Mật khẩu tạm (chỉ hiển thị MỘT lần, hãy giao trực tiếp cho người dùng):",
            f,
            "Người dùng phải đổi mật khẩu ở lần đăng nhập đầu tiên."
        };
    }

    public boolean isAdded() {
        return trangThaiThem;
    }
}
