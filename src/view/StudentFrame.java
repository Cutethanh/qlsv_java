package qlsv.view;

import qlsv.controller.StudentController;
import qlsv.model.Grade;
import qlsv.model.LogEntry;
import qlsv.model.Student;
import qlsv.security.AuthException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class StudentFrame extends WindowView {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StudentController ctr;
    private final Student sv;
    private final SessionGuard session;
    private JTabbedPane cacTab;
    private JTextField oCccd;
    private JTextField oSdt;
    private DefaultTableModel modelDiem;
    private DefaultTableModel modelLichSu;
    private JLabel lbTrungBinh;

    public StudentFrame(StudentController controller, Student student) {
        super("QLSV - Sinh viên: " + student.getUsername(), 960, 580, null, false);
        this.ctr = controller;
        this.sv = student;
        initUI();
        loadGrades();
        loadHistory();
        session = new SessionGuard(StudentController.SESSION_TIMEOUT_MINUTES, this::timeoutLogout);
        session.start();
    }

    private void initUI() {
        JPanel pHeader = new JPanel(new BorderLayout());
        pHeader.setBackground(UiUtils.HEADER_BG);
        String prev = sv.getPreviousLogin() == null ? "chưa có" : sv.getPreviousLogin().format(DT);
        pHeader.add(UiUtils.hint("  Lần đăng nhập trước: " + prev + "  ·  Tự đăng xuất sau "
                + StudentController.SESSION_TIMEOUT_MINUTES + " phút"), BorderLayout.WEST);
        JPanel pRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pRight.setOpaque(false);
        JButton btDoiPass = new JButton("Đổi mật khẩu");
        JButton btThoat = new JButton("Đăng xuất");
        pRight.add(new JLabel("Xin chào: " + sv.getFullName() + " (" + sv.getUsername() + ")"));
        pRight.add(btDoiPass);
        pRight.add(btThoat);
        pHeader.add(pRight, BorderLayout.EAST);
        root.add(pHeader, BorderLayout.NORTH);

        cacTab = new JTabbedPane();
        cacTab.addTab("Thông Tin Cá Nhân", createInfoPanel());
        cacTab.addTab("Bảng Điểm", createGradePanel());
        cacTab.addTab("Lịch Sử Đăng Nhập", createHistoryPanel());
        cacTab.addChangeListener(e -> loadHistory());
        root.add(cacTab, BorderLayout.CENTER);

        btThoat.addActionListener(e -> {
            int hoi = JOptionPane.showConfirmDialog(root, "Bạn muốn đăng xuất khỏi hệ thống ?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION);
            if (hoi == JOptionPane.YES_OPTION) {
                session.stop();
                ctr.logout(sv, false);
                new LoginFrame(ctr).setVisible(true);
                dispose();
            }
        });
        btDoiPass.addActionListener(e -> new ChangePasswordDialog(root, ctr, sv, false).setVisible(true));
    }

    public JTabbedPane getTabs() {
        return cacTab;
    }

    private void timeoutLogout() {
        ctr.logout(sv, true);
        for (Window w : Window.getWindows()) {
            if (w != getWindow() && w.isShowing() && w.getOwner() == getWindow()) {
                w.dispose();
            }
        }
        new LoginFrame(ctr, "Phiên đã hết hạn do không thao tác, hãy đăng nhập lại.").setVisible(true);
        dispose();
    }

    private JPanel createInfoPanel() {
        JPanel form = new JPanel(new GridLayout(8, 2, 10, 10));
        form.setPreferredSize(new Dimension(560, 300));
        addRow(form, "Mã sinh viên:", sv.getStudentId());
        addRow(form, "Họ và tên:", sv.getFullName());
        addRow(form, "Lớp:", sv.getClassName());
        addRow(form, "Địa chỉ:", sv.getAddress());
        oCccd = addRow(form, "CCCD:", ctr.maskedCccd(sv));
        oSdt = addRow(form, "Số điện thoại:", ctr.maskedPhone(sv));
        addRow(form, "Email:", sv.getEmail());
        addRow(form, "Trạng thái tài khoản:", ctr.accountStatus(sv));

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 16));
        wrap.add(form);

        JPanel pNut = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        JButton btHien = new JButton("Hiện CCCD/SĐT");
        pNut.add(btHien);
        pNut.add(UiUtils.hint("Dữ liệu nhạy cảm được mã hoá AES-256 và chỉ hiện sau khi nhập lại mật khẩu."));

        JPanel p = new JPanel(new BorderLayout());
        p.add(wrap, BorderLayout.NORTH);
        p.add(pNut, BorderLayout.CENTER);
        btHien.addActionListener(e -> reveal());
        return p;
    }

    private static JTextField addRow(JPanel form, String label, String value) {
        form.add(new JLabel(label));
        JTextField f = new JTextField(value == null ? "" : value);
        f.setEditable(false);
        form.add(f);
        return f;
    }

    private void reveal() {
        char[] pw = UiUtils.askPassword(root, "Nhập lại mật khẩu để xem CCCD/SĐT đầy đủ:");
        if (pw == null) {
            return;
        }
        try {
            String[] info = ctr.revealSensitive(sv, sv, pw);
            oCccd.setText(info[0]);
            oSdt.setText(info[1]);
        } catch (AuthException ex) {
            UiUtils.error(root, "Mật khẩu không đúng!");
        } finally {
            Arrays.fill(pw, '\0');
        }
    }

    private JPanel createGradePanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String[] cols = {"STT", "Môn học", "Điểm", "Xếp loại"};
        modelDiem = UiUtils.readOnlyModel(cols);
        JTable t = new JTable(modelDiem);
        UiUtils.columnWidths(t, 50, 300, 80, 200);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        lbTrungBinh = new JLabel();
        p.add(lbTrungBinh, BorderLayout.SOUTH);
        return p;
    }

    private void loadGrades() {
        modelDiem.setRowCount(0);
        List<Grade> list = ctr.getGrades(sv, sv.getStudentId());
        double sum = 0;
        int stt = 1;
        for (Grade g : list) {
            modelDiem.addRow(new Object[]{stt++, g.getSubject(), g.getScore(), g.getRank()});
            sum += g.getScore();
        }
        lbTrungBinh.setText(list.isEmpty() ? "Chưa có điểm."
                : String.format("Điểm trung bình: %.2f   ·   Số môn: %d", sum / list.size(), list.size()));
    }

    private JPanel createHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p.add(new JLabel("Các hoạt động bảo mật gần đây của tài khoản. Nếu thấy lần đăng nhập thất bại lạ, "
                + "hãy đổi mật khẩu ngay."), BorderLayout.NORTH);
        String[] cols = {"Thời gian", "Hoạt động", "Chi tiết", "Kết quả"};
        modelLichSu = UiUtils.readOnlyModel(cols);
        JTable t = new JTable(modelLichSu);
        UiUtils.colorRowsByLevel(t, 3);
        UiUtils.columnWidths(t, 140, 140, 420, 90);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private void loadHistory() {
        modelLichSu.setRowCount(0);
        for (LogEntry e : ctr.getOwnSecurityHistory(sv)) {
            modelLichSu.addRow(new Object[]{e.getFormattedTime(), e.getAction(), e.getDetail(),
                e.isSuccess() ? "Thành công" : "Thất bại"});
        }
    }
}
