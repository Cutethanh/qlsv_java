package qlsv.view;

import qlsv.controller.StudentController;
import qlsv.model.Admin;
import qlsv.model.Grade;
import qlsv.model.LogEntry;
import qlsv.model.LogLevel;
import qlsv.model.Student;
import qlsv.model.User;
import qlsv.security.AuthException;
import qlsv.security.PasswordHasher;
import qlsv.security.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AdminFrame extends WindowView {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StudentController ctr;
    private final Admin admin;
    private final SessionGuard session;
    private JTabbedPane tabs;
    private JTable bangSV;
    private JTable bangDiem;
    private JTable bangTK;
    private JTable bangLog;
    private DefaultTableModel modelSV;
    private DefaultTableModel modelDiem;
    private DefaultTableModel modelTK;
    private DefaultTableModel modelThongKe;
    private DefaultTableModel modelLog;
    private JComboBox<String> cbMucDo;
    private JTextField txtTimLog;
    private TableRowSorter<DefaultTableModel> sorterLog;

    public AdminFrame(StudentController controller, Admin admin) {
        super("QLSV - Quản trị viên", 1000, 680, null, false);
        this.ctr = controller;
        this.admin = admin;
        initUI();
        loadStudentData();
        loadGradeData();
        loadAccountData();
        runStats();
        loadLogData();
        session = new SessionGuard(StudentController.SESSION_TIMEOUT_MINUTES, this::timeoutLogout);
        session.start();
    }

    private void initUI() {
        JPanel pHeader = new JPanel(new BorderLayout());
        pHeader.setBackground(UiUtils.HEADER_BG);
        String prev = admin.getPreviousLogin() == null ? "chưa có" : admin.getPreviousLogin().format(DT);
        JLabel lbPhien = UiUtils.hint("  Lần đăng nhập trước: " + prev + "   ·   Tự đăng xuất sau "
                + StudentController.SESSION_TIMEOUT_MINUTES + " phút không thao tác");
        pHeader.add(lbPhien, BorderLayout.WEST);
        JPanel pRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pRight.setOpaque(false);
        JButton btDoiPass = new JButton("Đổi mật khẩu");
        JButton btOut = new JButton("Đăng xuất");
        pRight.add(new JLabel("Quyền hạn: " + admin.getRoleName() + " (" + admin.getUsername() + ")"));
        pRight.add(btDoiPass);
        pRight.add(btOut);
        pHeader.add(pRight, BorderLayout.EAST);
        root.add(pHeader, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.addTab("Quản Lý Sinh Viên", createStudentPanel());
        tabs.addTab("Quản Lý Điểm", createGradePanel());
        tabs.addTab("Quản Lý Tài Khoản", createAccountPanel());
        tabs.addTab("Thống Kê Bảo Mật", createStatsPanel());
        tabs.addTab("Nhật Ký Hệ Thống", createLogPanel());
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 4) {
                loadLogData();
            }
        });
        root.add(tabs, BorderLayout.CENTER);

        btDoiPass.addActionListener(e -> new ChangePasswordDialog(root, ctr, admin, false).setVisible(true));
        btOut.addActionListener(e -> {
            int hoi = JOptionPane.showConfirmDialog(root, "Bạn muốn đăng xuất khỏi hệ thống ?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION);
            if (hoi == JOptionPane.YES_OPTION) {
                session.stop();
                ctr.logout(admin, false);
                new LoginFrame(ctr).setVisible(true);
                dispose();
            }
        });
    }

    public JTabbedPane getTabs() {
        return tabs;
    }

    private void timeoutLogout() {
        ctr.logout(admin, true);
        for (Window w : Window.getWindows()) {
            if (w != getWindow() && w.isShowing() && w.getOwner() == getWindow()) {
                w.dispose();
            }
        }
        new LoginFrame(ctr, "Phiên đã hết hạn do không thao tác, hãy đăng nhập lại.").setVisible(true);
        dispose();
    }

    private static JPanel bottomBar(String hint, JPanel buttons) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.add(UiUtils.hint("  " + hint), BorderLayout.WEST);
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    private JPanel createStudentPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel pSearch = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pSearch.add(new JLabel("Tìm kiếm sinh viên (Tên/MSV/Lớp/User): "));
        JTextField txtTim = new JTextField(30);
        pSearch.add(txtTim);
        p.add(pSearch, BorderLayout.NORTH);

        String[] cols = {"MSV", "Tài khoản", "Họ tên", "Lớp", "CCCD", "Số điện thoại", "Email", "Trạng thái"};
        modelSV = UiUtils.readOnlyModel(cols);
        bangSV = new JTable(modelSV);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelSV);
        bangSV.setRowSorter(sorter);
        UiUtils.attachSearch(txtTim, sorter);
        UiUtils.columnWidths(bangSV, 60, 70, 140, 90, 110, 100, 180, 140);
        p.add(new JScrollPane(bangSV), BorderLayout.CENTER);

        JPanel pNut = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btThem = new JButton("Thêm mới");
        JButton btXem = new JButton("Xem CCCD/SĐT");
        JButton btXoa = new JButton("Xóa sinh viên");
        JButton btReset = new JButton("Làm mới");
        pNut.add(btThem);
        pNut.add(btXem);
        pNut.add(btXoa);
        pNut.add(btReset);
        p.add(bottomBar("CCCD/SĐT luôn được che; xem đầy đủ cần nhập lại mật khẩu.", pNut), BorderLayout.SOUTH);

        btReset.addActionListener(e -> loadStudentData());
        btThem.addActionListener(e -> {
            AddStudentDialog d = new AddStudentDialog(root, ctr, admin);
            d.setVisible(true);
            if (d.isAdded()) {
                loadStudentData();
                loadAccountData();
            }
        });
        btXem.addActionListener(e -> revealSelected());
        btXoa.addActionListener(e -> deleteSelected());
        return p;
    }

    private void revealSelected() {
        int row = bangSV.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(root, "Hãy chọn một sinh viên trong bảng!");
            return;
        }
        String msv = (String) modelSV.getValueAt(bangSV.convertRowIndexToModel(row), 0);
        Student s = ctr.findStudent(msv);
        char[] pw = UiUtils.askPassword(root, "Nhập lại mật khẩu quản trị để xem CCCD/SĐT của " + msv + ":");
        if (pw == null) {
            return;
        }
        try {
            String[] info = ctr.revealSensitive(admin, s, pw);
            JOptionPane.showMessageDialog(root, revealMessage(s, info), "Thông tin nhạy cảm",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (AuthException ex) {
            UiUtils.error(root, "Mật khẩu không đúng. Lượt thử đã được ghi nhật ký.");
        } finally {
            Arrays.fill(pw, '\0');
        }
    }

    private void deleteSelected() {
        int[] rows = bangSV.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(root, "Chưa chọn sinh viên nào để xóa!");
            return;
        }
        int check = JOptionPane.showConfirmDialog(root, "Xóa " + rows.length
                + " sinh viên đã chọn cùng toàn bộ điểm của họ?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (check == JOptionPane.YES_OPTION) {
            List<String> ids = new ArrayList<>();
            for (int r : rows) {
                ids.add((String) modelSV.getValueAt(bangSV.convertRowIndexToModel(r), 0));
            }
            ctr.removeStudents(admin, ids);
            loadStudentData();
            loadGradeData();
            loadAccountData();
        }
    }

    private void loadStudentData() {
        modelSV.setRowCount(0);
        for (Student s : ctr.getStudents(admin)) {
            modelSV.addRow(new Object[]{s.getStudentId(), s.getUsername(), s.getFullName(), s.getClassName(),
                ctr.maskedCccd(s), ctr.maskedPhone(s), s.getEmail() == null ? "" : s.getEmail(),
                ctr.accountStatus(s)});
        }
    }

    private JPanel createGradePanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel pSearch = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pSearch.add(new JLabel("Tìm kiếm điểm (MSV/Tên/Môn): "));
        JTextField txtTim = new JTextField(30);
        pSearch.add(txtTim);
        p.add(pSearch, BorderLayout.NORTH);

        String[] cols = {"MSV", "Họ tên", "Lớp", "Môn học", "Điểm", "Xếp loại"};
        modelDiem = UiUtils.readOnlyModel(cols);
        bangDiem = new JTable(modelDiem);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelDiem);
        bangDiem.setRowSorter(sorter);
        UiUtils.attachSearch(txtTim, sorter);
        UiUtils.columnWidths(bangDiem, 60, 150, 90, 170, 60, 140);
        p.add(new JScrollPane(bangDiem), BorderLayout.CENTER);

        JPanel pNut = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btNhap = new JButton("Nhập / Sửa điểm");
        JButton btXoa = new JButton("Xóa điểm");
        JButton btReset = new JButton("Làm mới");
        pNut.add(btNhap);
        pNut.add(btXoa);
        pNut.add(btReset);
        p.add(bottomBar("Mọi lần sửa / xoá điểm đều được ghi vào nhật ký (giá trị cũ → mới).", pNut),
                BorderLayout.SOUTH);

        btReset.addActionListener(e -> loadGradeData());
        btNhap.addActionListener(e -> {
            String msv = null;
            String mon = null;
            Double diem = null;
            int row = bangDiem.getSelectedRow();
            if (row >= 0) {
                int m = bangDiem.convertRowIndexToModel(row);
                msv = (String) modelDiem.getValueAt(m, 0);
                mon = (String) modelDiem.getValueAt(m, 3);
                diem = (Double) modelDiem.getValueAt(m, 4);
            }
            GradeDialog d = new GradeDialog(root, ctr, admin, msv, mon, diem);
            d.setVisible(true);
            if (d.isSaved()) {
                loadGradeData();
            }
        });
        btXoa.addActionListener(e -> {
            int row = bangDiem.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(root, "Hãy chọn dòng điểm muốn xóa!");
                return;
            }
            int m = bangDiem.convertRowIndexToModel(row);
            String msv = (String) modelDiem.getValueAt(m, 0);
            String mon = (String) modelDiem.getValueAt(m, 3);
            int c = JOptionPane.showConfirmDialog(root, "Xóa điểm môn " + mon + " của " + msv + "?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                ctr.deleteGrade(admin, msv, mon);
                loadGradeData();
            }
        });
        return p;
    }

    private void loadGradeData() {
        modelDiem.setRowCount(0);
        for (Grade g : ctr.getAllGrades(admin)) {
            Student s = ctr.findStudent(g.getStudentId());
            modelDiem.addRow(new Object[]{g.getStudentId(), s == null ? "" : s.getFullName(),
                s == null ? "" : s.getClassName(), g.getSubject(), g.getScore(), g.getRank()});
        }
    }

    private JPanel createAccountPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel pSearch = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pSearch.add(new JLabel("Tìm kiếm tài khoản: "));
        JTextField txtTim = new JTextField(30);
        pSearch.add(txtTim);
        p.add(pSearch, BorderLayout.NORTH);

        String[] cols = {"Tài khoản", "Vai trò", "Họ tên", "Sai liên tiếp", "Trạng thái", "Đăng nhập gần nhất",
            "Mật khẩu lưu trữ (băm PBKDF2)"};
        modelTK = UiUtils.readOnlyModel(cols);
        bangTK = new JTable(modelTK);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelTK);
        bangTK.setRowSorter(sorter);
        UiUtils.attachSearch(txtTim, sorter);
        UiUtils.columnWidths(bangTK, 70, 90, 140, 80, 190, 120, 260);
        p.add(new JScrollPane(bangTK), BorderLayout.CENTER);

        JPanel pNut = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btMoKhoa = new JButton("Mở khoá");
        JButton btDatLai = new JButton("Đặt lại mật khẩu");
        JButton btReset = new JButton("Làm mới");
        pNut.add(btMoKhoa);
        pNut.add(btDatLai);
        pNut.add(btReset);
        p.add(bottomBar("Chỉ lưu băm PBKDF2 + salt, không ai đọc được mật khẩu gốc.", pNut), BorderLayout.SOUTH);

        btReset.addActionListener(e -> loadAccountData());
        btMoKhoa.addActionListener(e -> {
            String u = selectedAccount();
            if (u != null) {
                ctr.unlockAccount(admin, u);
                JOptionPane.showMessageDialog(root, "Đã mở khoá tài khoản " + u + ".");
                loadAccountData();
                loadStudentData();
            }
        });
        btDatLai.addActionListener(e -> {
            String u = selectedAccount();
            if (u == null) {
                return;
            }
            int c = JOptionPane.showConfirmDialog(root, "Cấp mật khẩu tạm mới cho " + u + "?\n"
                    + "Người dùng sẽ phải đổi mật khẩu ở lần đăng nhập tới.", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) {
                return;
            }
            try {
                String temp = ctr.resetPassword(admin, u);
                AddStudentDialog.showTempPassword(root, u, temp);
                loadAccountData();
                loadStudentData();
            } catch (ValidationException ex) {
                UiUtils.error(root, ex.getMessage());
            }
        });
        return p;
    }

    private String selectedAccount() {
        int row = bangTK.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(root, "Hãy chọn một tài khoản trong bảng!");
            return null;
        }
        return (String) modelTK.getValueAt(bangTK.convertRowIndexToModel(row), 0);
    }

    private void loadAccountData() {
        modelTK.setRowCount(0);
        for (User u : ctr.getAllUsers(admin)) {
            modelTK.addRow(new Object[]{u.getUsername(), u.getRoleName(), u.getDisplayName(), u.getFailedAttempts(),
                ctr.accountStatus(u), u.getLastLogin() == null ? "" : u.getLastLogin().format(DT),
                PasswordHasher.preview(u.getPasswordHash())});
        }
    }

    private JPanel createStatsPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        String[] cols = {"STT", "Chỉ số bảo mật", "Giá trị"};
        modelThongKe = UiUtils.readOnlyModel(cols);
        JTable t = new JTable(modelThongKe);
        UiUtils.columnWidths(t, 50, 500, 150);
        p.add(new JScrollPane(t), BorderLayout.CENTER);

        JPanel pNut = new JPanel();
        JButton btUpdate = new JButton("Chạy thống kê");
        JButton btToanVen = new JButton("Kiểm tra toàn vẹn dữ liệu");
        pNut.add(btUpdate);
        pNut.add(btToanVen);
        p.add(pNut, BorderLayout.SOUTH);

        btUpdate.addActionListener(e -> runStats());
        btToanVen.addActionListener(e -> showIntegrity());
        return p;
    }

    private void runStats() {
        modelThongKe.setRowCount(0);
        int stt = 1;
        for (String[] row : ctr.computeSecurityStats(admin)) {
            modelThongKe.addRow(new Object[]{stt++, row[0], row[1]});
        }
    }

    void showIntegrity() {
        Map<String, Boolean> result = ctr.checkIntegrity(admin);
        boolean all = !result.containsValue(false);
        JOptionPane.showMessageDialog(root, integrityReport(result), "Kiểm tra toàn vẹn dữ liệu",
                all ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        runStats();
    }

    public static String integrityReport(Map<String, Boolean> result) {
        StringBuilder sb = new StringBuilder("Kết quả kiểm tra chữ ký HMAC-SHA256:\n\n");
        boolean all = true;
        for (Map.Entry<String, Boolean> e : result.entrySet()) {
            sb.append("•  ").append(e.getKey()).append(":  ")
                    .append(e.getValue() ? "Hợp lệ" : "KHÔNG KHỚP — có thể đã bị sửa").append('\n');
            all &= e.getValue();
        }
        sb.append(all ? "\nDữ liệu nguyên vẹn, không phát hiện sửa đổi trái phép."
                : "\nCẢNH BÁO: phát hiện file bị sửa ngoài ứng dụng!");
        return sb.toString();
    }

    public static String revealMessage(Student s, String[] info) {
        return "Sinh viên: " + s.getFullName() + " (" + s.getStudentId() + ")\n"
                + "CCCD: " + info[0] + "\nSố điện thoại: " + info[1]
                + "\n\nLượt xem này đã được ghi vào nhật ký hệ thống.";
    }

    private JPanel createLogPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel pTop = new JPanel(new BorderLayout());
        JLabel lbl = new JLabel("NHẬT KÝ BẢO MẬT HỆ THỐNG", JLabel.CENTER);
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        pTop.add(lbl, BorderLayout.NORTH);
        JPanel pLoc = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pLoc.add(new JLabel("Mức độ: "));
        cbMucDo = new JComboBox<>(new String[]{"Tất cả", LogLevel.INFO.getLabel(), LogLevel.WARNING.getLabel(),
            LogLevel.ALERT.getLabel()});
        pLoc.add(cbMucDo);
        pLoc.add(new JLabel("   Tìm kiếm: "));
        txtTimLog = new JTextField(24);
        pLoc.add(txtTimLog);
        pTop.add(pLoc, BorderLayout.SOUTH);
        p.add(pTop, BorderLayout.NORTH);

        String[] cols = {"Thời gian", "Tài khoản", "Hành động", "Chi tiết", "Kết quả", "Mức độ"};
        modelLog = UiUtils.readOnlyModel(cols);
        bangLog = new JTable(modelLog);
        sorterLog = new TableRowSorter<>(modelLog);
        bangLog.setRowSorter(sorterLog);
        UiUtils.colorRowsByLevel(bangLog, 5);
        UiUtils.columnWidths(bangLog, 130, 70, 120, 420, 70, 90);
        p.add(new JScrollPane(bangLog), BorderLayout.CENTER);

        JButton btReset = new JButton("Đọc lại nhật ký");
        p.add(btReset, BorderLayout.SOUTH);

        btReset.addActionListener(e -> loadLogData());
        cbMucDo.addActionListener(e -> applyLogFilter());
        txtTimLog.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyLogFilter(); }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyLogFilter(); }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyLogFilter(); }
        });
        return p;
    }

    private void applyLogFilter() {
        List<RowFilter<DefaultTableModel, Integer>> filters = new ArrayList<>();
        String level = (String) cbMucDo.getSelectedItem();
        if (level != null && !"Tất cả".equals(level)) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(level) + "$", 5));
        }
        String text = txtTimLog.getText().trim();
        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
        sorterLog.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private void loadLogData() {
        modelLog.setRowCount(0);
        for (LogEntry e : ctr.getLogs(admin)) {
            modelLog.addRow(new Object[]{e.getFormattedTime(), e.getUsername(), e.getAction(), e.getDetail(),
                e.isSuccess() ? "Thành công" : "Thất bại", e.getLevel()});
        }
    }
}
