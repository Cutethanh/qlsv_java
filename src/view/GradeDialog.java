package qlsv.view;

import qlsv.controller.StudentController;
import qlsv.model.Admin;
import qlsv.model.Student;
import qlsv.security.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;

public class GradeDialog extends WindowView {
    private static final long serialVersionUID = 1L;

    private final StudentController ctr;
    private final Admin admin;
    private JComboBox<String> chonSV;
    private JComboBox<String> chonMon;
    private JTextField nhapDiem;
    private boolean saved = false;

    public GradeDialog(Component parent, StudentController controller, Admin admin,
                       String studentId, String subject, Double score) {
        super("Nhập / Sửa điểm", 460, 240, parent, true);
        this.ctr = controller;
        this.admin = admin;
        initUI(studentId, subject, score);
    }

    private void initUI(String studentId, String subject, Double score) {
        JPanel p = new JPanel(new GridLayout(4, 2, 8, 12));
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 6, 16));
        p.add(new JLabel("Sinh viên:"));
        chonSV = new JComboBox<>();
        for (Student s : ctr.getStudents(admin)) {
            String item = s.getStudentId() + " - " + s.getFullName();
            chonSV.addItem(item);
            if (s.getStudentId().equals(studentId)) {
                chonSV.setSelectedItem(item);
            }
        }
        p.add(chonSV);
        p.add(new JLabel("Môn học:"));
        chonMon = new JComboBox<>(ctr.getSubjects().toArray(new String[0]));
        chonMon.setEditable(true);
        if (subject != null) {
            chonMon.setSelectedItem(subject);
        }
        p.add(chonMon);
        p.add(new JLabel("Điểm (0 – 10):"));
        nhapDiem = new JTextField(score == null ? "" : String.valueOf(score));
        p.add(nhapDiem);
        JButton btThoat = new JButton("Đóng lại");
        JButton btLuu = new JButton("Lưu điểm");
        p.add(btThoat);
        p.add(btLuu);
        root.add(p, BorderLayout.CENTER);
        JLabel note = UiUtils.hint("Sửa điểm sẽ được ghi vào nhật ký kèm giá trị cũ và mới.");
        note.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        root.add(note, BorderLayout.SOUTH);

        btThoat.addActionListener(e -> dispose());
        btLuu.addActionListener(e -> save());
    }

    private void save() {
        Object sel = chonSV.getSelectedItem();
        if (sel == null) {
            return;
        }
        String msv = sel.toString().split(" - ")[0];
        String mon = chonMon.getSelectedItem() == null ? "" : chonMon.getSelectedItem().toString();
        double diem;
        try {
            diem = Double.parseDouble(nhapDiem.getText().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            UiUtils.error(root, "Điểm phải là số, ví dụ 7.5");
            return;
        }
        try {
            ctr.saveGrade(admin, msv, mon, diem);
            saved = true;
            dispose();
        } catch (ValidationException ex) {
            UiUtils.error(root, ex.getMessage());
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
