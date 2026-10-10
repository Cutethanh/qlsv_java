package qlsv;

import qlsv.controller.StudentController;
import qlsv.view.LoginFrame;
import qlsv.view.UiUtils;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UiUtils.localizeOptionPane();
            StudentController ctr;
            try {
                ctr = new StudentController(new File("data"));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Không thể khởi động hệ thống:\n" + ex.getMessage()
                        + "\n\nHãy khôi phục thư mục data/ từ bản sao lưu.\n"
                        + "Muốn khởi tạo lại dữ liệu mẫu: xoá toàn bộ thư mục data/.",
                        "Lỗi toàn vẹn dữ liệu", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }
            LoginFrame frame = new LoginFrame(ctr);
            frame.setVisible(true);
            List<String> warnings = ctr.getStartupWarnings();
            if (!warnings.isEmpty()) {
                JOptionPane.showMessageDialog(frame.getContent(), String.join("\n", warnings)
                        + "\n\nSự việc đã được ghi vào nhật ký hệ thống.", "Cảnh báo bảo mật",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    }
}
