package qlsv.data;

import qlsv.model.Admin;
import qlsv.model.Grade;
import qlsv.model.Student;
import qlsv.model.User;
import qlsv.security.CryptoService;
import qlsv.security.PasswordHasher;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DataSeeder {
    public static final String DEMO_ADMIN_PASSWORD = "Admin@2026";
    public static final String DEMO_STUDENT_PASSWORD = "Sinhvien@2026";
    public static final String[] CLASSES = {"CNTT-K18A", "CNTT-K18B", "ATTT-K18"};
    public static final String[] SUBJECTS = {
        "Toán cao cấp", "Lập trình Java", "Cơ sở dữ liệu", "Mạng máy tính", "An toàn thông tin"
    };
    private static final String[] NAMES = {
        "Trần Bình", "Lê Hoa", "Phạm Dũng", "Nguyễn Thị Mai", "Hoàng Minh Tuấn", "Vũ Thu Trang",
        "Đặng Quốc Huy", "Bùi Ngọc Anh", "Đỗ Văn Nam", "Ngô Thanh Hương", "Dương Đức Long",
        "Lý Khánh Linh", "Phan Hải Đăng", "Võ Thị Lan", "Trịnh Công Sơn", "Hồ Bảo Ngọc",
        "Mai Xuân Phúc", "Tạ Diệu Linh", "Cao Văn Khoa", "Lương Thùy Dương", "Đinh Gia Bảo",
        "Kiều Phương Thảo", "Châu Minh Khôi", "Lâm Yến Nhi", "Quách Tiến Đạt"
    };
    private static final String[] ADDRESSES = {
        "Hà Đông, Hà Nội", "Cầu Giấy, Hà Nội", "Thanh Xuân, Hà Nội", "Nam Từ Liêm, Hà Nội", "Long Biên, Hà Nội"
    };
    private static final int PENDING_ACTIVATION = 2;

    private DataSeeder() {
    }

    public static void seed(List<User> users, List<Grade> grades, CryptoService crypto) {
        Random r = new Random(2026);
        users.add(new Admin("admin", PasswordHasher.hash(DEMO_ADMIN_PASSWORD.toCharArray()), "Phòng Đào tạo"));
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < NAMES.length; i++) {
            String id = String.format("SV%03d", i + 1);
            String username = String.format("sv%03d", i + 1);
            Student s = new Student(id, username, null, NAMES[i], CLASSES[i % CLASSES.length]);
            if (i < NAMES.length - PENDING_ACTIVATION) {
                String cccd = String.format("001%d%02d%06d", 2 + (i % 2), 4 + r.nextInt(3), r.nextInt(1_000_000));
                s.setAddress(ADDRESSES[i % ADDRESSES.length]);
                s.setEncryptedCccd(crypto.encrypt(cccd));
                s.setPhone(String.format("09%08d", r.nextInt(100_000_000)));
                s.setEmail(username + "@sinhvien.edu.vn");
                s.setFirstLogin(false);
            } else {
                s.setMustChangePassword(true);
            }
            students.add(s);
            for (String subject : SUBJECTS) {
                double score = Math.round((4.5 + r.nextDouble() * 5.5) * 2) / 2.0;
                grades.add(new Grade(id, subject, score));
            }
        }
        students.parallelStream().forEach(s ->
                s.setPasswordHash(PasswordHasher.hash(DEMO_STUDENT_PASSWORD.toCharArray())));
        users.addAll(students);
    }
}
