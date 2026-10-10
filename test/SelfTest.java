package qlsv;

import qlsv.controller.StudentController;
import qlsv.data.DataManager;
import qlsv.data.IntegrityException;
import qlsv.model.Admin;
import qlsv.model.LogEntry;
import qlsv.model.LogLevel;
import qlsv.model.Student;
import qlsv.model.User;
import qlsv.security.AuthException;
import qlsv.security.CryptoService;
import qlsv.security.InputValidator;
import qlsv.security.PasswordHasher;
import qlsv.security.ValidationException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SelfTest {
    private static final char[] ADMIN_PW = "Admin@2026".toCharArray();
    private static final char[] SV_PW = "Sinhvien@2026".toCharArray();

    private static int passed = 0;
    private static int failed = 0;

    static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-10-08T02:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneId getZone() { return ZoneId.of("Asia/Ho_Chi_Minh"); }

        @Override public Clock withZone(ZoneId zone) { return this; }

        @Override public Instant instant() { return now; }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("qlsv-test");
        MutableClock clock = new MutableClock();
        StudentController c = new StudentController(dir.toFile(), clock);
        Admin admin = (Admin) c.authenticate("admin", ADMIN_PW);
        Student sv1 = c.findStudent("SV001");
        Student sv2 = c.findStudent("SV002");
        byte[] usersFile = Files.readAllBytes(dir.resolve("users.dat"));

        System.out.println("== 1. Lưu trữ mật khẩu & dữ liệu nhạy cảm");
        check("Mật khẩu không xuất hiện dạng rõ trong users.dat",
                !contains(usersFile, "Sinhvien@2026") && !contains(usersFile, "Admin@2026"));
        check("Cùng mật khẩu nhưng chuỗi băm khác nhau nhờ salt riêng",
                !sv1.getPasswordHash().equals(sv2.getPasswordHash()));
        String cccd1 = c.revealSensitive(admin, sv1, ADMIN_PW)[0];
        check("CCCD không xuất hiện dạng rõ trong users.dat", !contains(usersFile, cccd1));
        CryptoService crypto = new CryptoService(dir.resolve("secret.key").toFile());
        check("Cùng một CCCD mã hoá 2 lần cho 2 bản mã khác nhau (IV ngẫu nhiên)",
                !crypto.encrypt(cccd1).equals(crypto.encrypt(cccd1)));
        String enc = crypto.encrypt(cccd1);
        char[] bad = enc.toCharArray();
        bad[20] = bad[20] == 'A' ? 'B' : 'A';
        check("AES-GCM phát hiện bản mã bị sửa 1 ký tự", throwsSecurity(() -> crypto.decrypt(new String(bad))));

        System.out.println("== 2. Xác thực & chống dò mật khẩu");
        check("Đăng nhập đúng mật khẩu thành công", c.authenticate("sv001", SV_PW) == sv1);
        AuthException wrong = authError(c, "sv001", "SaiMatKhau@1");
        AuthException ghost = authError(c, "khongtontai", "SaiMatKhau@1");
        check("Sai mật khẩu và sai tên đăng nhập trả về cùng một lỗi (không lộ tài khoản)",
                wrong != null && ghost != null && wrong.getReason() == ghost.getReason()
                        && wrong.getReason() == AuthException.Reason.INVALID_CREDENTIALS);
        c.authenticate("sv001", SV_PW);
        AuthException last = null;
        for (int i = 0; i < 5; i++) {
            last = authError(c, "sv003", "Doan@" + i + "xyz");
        }
        check("Sai 5 lần liên tiếp thì tài khoản bị khoá", last != null && last.getReason() == AuthException.Reason.LOCKED);
        AuthException whileLocked = authError(c, "sv003", "Sinhvien@2026");
        check("Đang khoá thì nhập ĐÚNG mật khẩu cũng bị từ chối",
                whileLocked != null && whileLocked.getReason() == AuthException.Reason.LOCKED);
        clock.advance(Duration.ofMinutes(16));
        check("Sau 15 phút tài khoản tự mở khoá", c.authenticate("sv003", SV_PW) != null);
        for (int i = 0; i < 5; i++) {
            authError(c, "sv004", "Doan@" + i + "xyz");
        }
        c.unlockAccount(admin, "sv004");
        check("Quản trị viên mở khoá thủ công được", c.authenticate("sv004", SV_PW) != null);

        System.out.println("== 3. Chính sách mật khẩu");
        check("Từ chối 'abc12345' (thiếu chữ hoa, ký tự đặc biệt)",
                InputValidator.checkPasswordPolicy("abc12345".toCharArray(), "sv001") != null);
        check("Từ chối mật khẩu chứa tên đăng nhập 'Sv001@abcd'",
                InputValidator.checkPasswordPolicy("Sv001@abcd".toCharArray(), "sv001") != null);
        check("Chấp nhận 'Hoc#Ky2026'", InputValidator.checkPasswordPolicy("Hoc#Ky2026".toCharArray(), "sv001") == null);
        boolean allTempOk = true;
        for (int i = 0; i < 200; i++) {
            allTempOk &= InputValidator.checkPasswordPolicy(PasswordHasher.generateTemporaryPassword().toCharArray(), "sv001") == null;
        }
        check("200 mật khẩu tạm sinh ngẫu nhiên đều đạt chính sách", allTempOk);

        System.out.println("== 4. Phân quyền (RBAC) & chống IDOR");
        check("Sinh viên xem được điểm của chính mình", c.getGrades(sv1, "SV001").size() == 5);
        check("Sinh viên KHÔNG xem được điểm của người khác", throwsSecurity(() -> c.getGrades(sv1, "SV002")));
        check("Sinh viên KHÔNG gọi được chức năng quản trị", throwsSecurity(() -> c.getStudents(sv1)));
        check("Sinh viên KHÔNG xem được CCCD của người khác",
                throwsSecurity(() -> { try { c.revealSensitive(sv1, sv2, SV_PW); } catch (AuthException e) { throw new RuntimeException(e); } }));
        check("Xem CCCD cần nhập lại mật khẩu: sai mật khẩu bị từ chối",
                reauthFails(c, admin, sv2, "SaiMatKhau@1"));

        System.out.println("== 5. Quên mật khẩu & kích hoạt lần đầu");
        String last4 = c.revealSensitive(admin, sv2, ADMIN_PW)[0].substring(8);
        check("Sai email bị từ chối", forgotFails(c, "sv002", "sai@email.vn", last4));
        boolean lockedByGuessing = false;
        for (int i = 0; i < 6 && !lockedByGuessing; i++) {
            try {
                c.resetForgottenPassword("sv002", "sv002@sinhvien.edu.vn", String.format("%04d", i),
                        "Moi#Pass2026".toCharArray(), "Moi#Pass2026".toCharArray());
            } catch (AuthException e) {
                lockedByGuessing = e.getReason() == AuthException.Reason.LOCKED;
            }
        }
        check("Dò 4 số cuối CCCD bị khoá sau 5 lần sai", lockedByGuessing);
        clock.advance(Duration.ofMinutes(16));
        c.resetForgottenPassword("sv002", "sv002@sinhvien.edu.vn", last4, "Moi#Pass2026".toCharArray(),
                "Moi#Pass2026".toCharArray());
        check("Xác minh đúng email + 4 số cuối CCCD thì đặt lại được mật khẩu",
                c.authenticate("sv002", "Moi#Pass2026".toCharArray()) == sv2);
        Student sv24 = (Student) c.authenticate("sv024", SV_PW);
        check("Tài khoản mới ở trạng thái chờ kích hoạt", sv24.isFirstLogin());
        check("Kích hoạt với CCCD 11 số bị từ chối", validationFails(() -> c.completeFirstLogin(sv24, "Lâm Yến Nhi",
                "Hà Nội", "00120600001", "0912345678", "nhi@gmail.com", "Nhi#2026abc".toCharArray(), "Nhi#2026abc".toCharArray())));
        check("Kích hoạt trùng CCCD của người khác bị từ chối", validationFails(() -> c.completeFirstLogin(sv24,
                "Lâm Yến Nhi", "Hà Nội", cccd1, "0912345678", "nhi@gmail.com", "Nhi#2026abc".toCharArray(), "Nhi#2026abc".toCharArray())));
        c.completeFirstLogin(sv24, "Lâm Yến Nhi", "Hà Nội", "001306012345", "0912345678", "nhi@gmail.com",
                "Nhi#2026abc".toCharArray(), "Nhi#2026abc".toCharArray());
        check("Kích hoạt hợp lệ: thông tin được lưu, mật khẩu tạm hết hiệu lực",
                !sv24.isFirstLogin() && authError(c, "sv024", "Sinhvien@2026") != null);
        String temp = c.addStudent(admin, "SV026", "Nguyễn Hải Yến", "CNTT-K18A", "sv026");
        Student sv26 = c.findStudent("SV026");
        check("Thêm sinh viên: hệ thống cấp mật khẩu tạm và buộc kích hoạt",
                sv26.isFirstLogin() && c.authenticate("sv026", temp.toCharArray()) == sv26);

        System.out.println("== 6. Toàn vẹn dữ liệu & nhật ký");
        c.saveGrade(admin, "SV005", "Mạng máy tính", 9.5);
        boolean oldNewLogged = false;
        for (LogEntry e : c.getLogs(admin)) {
            oldNewLogged |= e.getAction().equals("Sửa điểm") && e.getDetail().contains("→ 9.5");
        }
        check("Sửa điểm được ghi nhật ký kèm giá trị cũ → mới", oldNewLogged);
        Map<String, Integer> byAction = new HashMap<>();
        for (LogEntry e : c.getLogs(admin)) {
            byAction.merge(e.getAction() + (e.getLevel() == LogLevel.ALERT ? "!" : ""), 1, Integer::sum);
        }
        check("Nhật ký có sự kiện khoá tài khoản (mức Nghiêm trọng)", byAction.containsKey("Khoá tài khoản!"));
        check("Nhật ký có sự kiện truy cập trái phép (mức Nghiêm trọng)", byAction.containsKey("Truy cập trái phép!"));
        check("Kiểm tra toàn vẹn: cả 3 file đều hợp lệ", !c.checkIntegrity(admin).containsValue(false));
        Path grades = dir.resolve("grades.dat");
        byte[] g = Files.readAllBytes(grades);
        g[g.length - 30] ^= 1;
        Files.write(grades, g);
        check("Sửa 1 bit trong grades.dat bị phát hiện ngay", c.checkIntegrity(admin).get("grades.dat") == Boolean.FALSE);
        StudentController restarted = new StudentController(dir.toFile(), clock);
        check("Khởi động lại: tự khôi phục grades.dat từ bản sao lưu và cảnh báo",
                !restarted.getStartupWarnings().isEmpty()
                        && restarted.checkIntegrity((Admin) restarted.findUser("admin")).get("grades.dat"));
        Files.delete(dir.resolve("logs.dat"));
        StudentController afterDelete = new StudentController(dir.toFile(), clock);
        check("Xoá file nhật ký để xoá dấu vết bị phát hiện khi khởi động",
                afterDelete.getStartupWarnings().get(0).contains("logs.dat"));
        Files.delete(dir.resolve("users.dat"));
        Files.delete(dir.resolve("users.dat.bak"));
        boolean refused = false;
        try {
            new StudentController(dir.toFile(), clock);
        } catch (IOException e) {
            refused = true;
        }
        check("Mất users.dat và bản sao lưu: từ chối chạy, KHÔNG tự tạo lại tài khoản mặc định", refused);
        Path other = Files.createTempDirectory("qlsv-filter");
        CryptoService crypto2 = new CryptoService(other.resolve("secret.key").toFile());
        DataManager dm = new DataManager(other.toFile(), crypto2);
        List<HashMap<String, String>> evil = new ArrayList<>(Collections.singletonList(new HashMap<>()));
        dm.save(evil, "evil.dat");
        boolean blocked = false;
        try {
            dm.load("evil.dat", HashMap.class);
        } catch (IOException | IntegrityException e) {
            blocked = true;
        }
        check("Bộ lọc giải tuần tự hoá chặn lớp ngoài danh sách cho phép", blocked);

        System.out.println();
        System.out.println("Kết quả: " + passed + "/" + (passed + failed) + " ca kiểm thử đạt");
        System.exit(failed == 0 ? 0 : 1);
    }

    interface Action {
        void run() throws Exception;
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
        }
        System.out.println((ok ? "  ✓ " : "  ✗ ") + name);
    }

    private static boolean contains(byte[] haystack, String needle) {
        return new String(haystack, StandardCharsets.ISO_8859_1).contains(needle)
                || new String(haystack, StandardCharsets.UTF_8).contains(needle);
    }

    private static AuthException authError(StudentController c, String u, String p) {
        try {
            c.authenticate(u, p.toCharArray());
            return null;
        } catch (AuthException e) {
            return e;
        }
    }

    private static boolean throwsSecurity(Runnable r) {
        try {
            r.run();
            return false;
        } catch (SecurityException e) {
            return true;
        }
    }

    private static boolean reauthFails(StudentController c, User actor, Student target, String pw) {
        try {
            c.revealSensitive(actor, target, pw.toCharArray());
            return false;
        } catch (AuthException e) {
            return e.getReason() == AuthException.Reason.REAUTH_FAILED;
        }
    }

    private static boolean forgotFails(StudentController c, String u, String email, String last4) {
        try {
            c.resetForgottenPassword(u, email, last4, "Moi#Pass2026".toCharArray(), "Moi#Pass2026".toCharArray());
            return false;
        } catch (AuthException e) {
            return true;
        } catch (ValidationException e) {
            return false;
        }
    }

    private static boolean validationFails(Action a) {
        try {
            a.run();
            return false;
        } catch (ValidationException e) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
