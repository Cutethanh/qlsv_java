package qlsv.controller;

import qlsv.data.DataHandler;
import qlsv.data.DataManager;
import qlsv.data.DataSeeder;
import qlsv.data.IntegrityException;
import qlsv.model.Admin;
import qlsv.model.Grade;
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
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudentController {
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCK_MINUTES = 15;
    public static final int SESSION_TIMEOUT_MINUTES = 5;

    public static final String USERS_FILE = "users.dat";
    public static final String GRADES_FILE = "grades.dat";
    public static final String LOGS_FILE = "logs.dat";
    private static final String[] DATA_FILES = {USERS_FILE, GRADES_FILE, LOGS_FILE};

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm dd/MM");
    private static final String DUMMY_HASH = PasswordHasher.hash("Dummy#Password1".toCharArray());

    private final DataHandler data;
    private final CryptoService crypto;
    private final Clock clock;
    private final List<User> users;
    private final List<Grade> grades;
    private final List<LogEntry> logs;
    private final List<String> startupWarnings = new ArrayList<>();

    public StudentController(File dataDir) throws IOException {
        this(dataDir, Clock.systemDefaultZone());
    }

    public StudentController(File dataDir, Clock clock) throws IOException {
        this.clock = clock;
        this.crypto = new CryptoService(new File(dataDir, "secret.key"));
        this.data = new DataManager(dataDir, crypto);
        boolean freshInstall = crypto.isNewKey();
        this.users = loadVerified(USERS_FILE, User.class, freshInstall);
        this.grades = loadVerified(GRADES_FILE, Grade.class, freshInstall);
        this.logs = loadVerified(LOGS_FILE, LogEntry.class, freshInstall);
        if (freshInstall) {
            DataSeeder.seed(users, grades, crypto);
            log("system", "Khởi tạo dữ liệu", "Tạo " + users.size() + " tài khoản mẫu", true, LogLevel.INFO);
            saveUsers();
            saveGrades();
        }
        for (String warning : startupWarnings) {
            log("system", "Kiểm tra toàn vẹn", warning, false, LogLevel.ALERT);
        }
    }

    private <T extends Serializable> List<T> loadVerified(String file, Class<T> type, boolean fresh)
            throws IOException {
        if (!data.exists(file)) {
            if (fresh) {
                return new ArrayList<>();
            }
            if (data.restoreBackup(file)) {
                startupWarnings.add("Phát hiện thiếu file " + file + " — đã khôi phục từ bản sao lưu.");
            } else {
                throw new IOException("Thiếu file dữ liệu " + file + " (có thể đã bị xoá) và không có bản sao lưu hợp lệ.");
            }
        }
        try {
            return data.load(file, type);
        } catch (IntegrityException e) {
            if (data.restoreBackup(file)) {
                startupWarnings.add("Phát hiện " + file + " bị sửa trái phép — đã khôi phục từ bản sao lưu gần nhất.");
                try {
                    return data.load(file, type);
                } catch (IntegrityException e2) {
                    throw new IOException(e2.getMessage());
                }
            }
            throw new IOException(e.getMessage() + " và không có bản sao lưu hợp lệ.");
        }
    }

    public User authenticate(String username, char[] password) throws AuthException {
        String name = username == null ? "" : username.trim();
        LocalDateTime now = now();
        User user = findUser(name);
        if (user == null) {
            PasswordHasher.verify(password, DUMMY_HASH);
            log(name, "Đăng nhập", "Tài khoản không tồn tại", false, LogLevel.WARNING);
            throw new AuthException(AuthException.Reason.INVALID_CREDENTIALS, 0);
        }
        if (user.isLocked(now)) {
            long left = minutesLeft(user, now);
            log(name, "Đăng nhập", "Bị từ chối: tài khoản đang khoá, còn " + left + " phút", false, LogLevel.WARNING);
            throw new AuthException(AuthException.Reason.LOCKED, left);
        }
        if (PasswordHasher.verify(password, user.getPasswordHash())) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            user.recordLogin(now);
            log(name, "Đăng nhập", "Thành công (" + user.getRoleName() + ")", true, LogLevel.INFO);
            saveUsers();
            return user;
        }
        registerFailure(user, now, "Đăng nhập", "Sai mật khẩu");
        if (user.isLocked(now)) {
            throw new AuthException(AuthException.Reason.LOCKED, LOCK_MINUTES);
        }
        throw new AuthException(AuthException.Reason.INVALID_CREDENTIALS, 0);
    }

    private void registerFailure(User user, LocalDateTime now, String action, String reason) {
        int n = user.getFailedAttempts() + 1;
        if (n >= MAX_FAILED_ATTEMPTS) {
            user.setFailedAttempts(0);
            user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
            log(user.getUsername(), "Khoá tài khoản",
                    reason + " " + MAX_FAILED_ATTEMPTS + " lần liên tiếp (" + action.toLowerCase() + "), khoá "
                            + LOCK_MINUTES + " phút", false, LogLevel.ALERT);
        } else {
            user.setFailedAttempts(n);
            log(user.getUsername(), action, reason + " (lần " + n + "/" + MAX_FAILED_ATTEMPTS + ")", false,
                    LogLevel.WARNING);
        }
        saveUsers();
    }

    public void logout(User user, boolean timeout) {
        if (timeout) {
            log(user.getUsername(), "Hết phiên", "Tự đăng xuất sau " + SESSION_TIMEOUT_MINUTES
                    + " phút không thao tác", true, LogLevel.INFO);
        } else {
            log(user.getUsername(), "Đăng xuất", "", true, LogLevel.INFO);
        }
    }

    public void completeFirstLogin(Student s, String name, String address, String cccd, String phone,
                                   String email, char[] newPassword, char[] confirm) throws ValidationException {
        requireActive(s);
        name = trim(name);
        address = trim(address);
        cccd = trim(cccd);
        phone = trim(phone);
        email = trim(email);
        if (name.isEmpty() || address.isEmpty() || cccd.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            throw new ValidationException("Phải điền hết các ô!");
        }
        if (!InputValidator.isName(name)) {
            throw new ValidationException("Họ tên chỉ gồm chữ cái và khoảng trắng (2–50 ký tự).");
        }
        if (address.length() > 100) {
            throw new ValidationException("Địa chỉ tối đa 100 ký tự.");
        }
        if (!InputValidator.isCccd(cccd)) {
            throw new ValidationException("CCCD phải đúng 12 chữ số!");
        }
        if (!InputValidator.isPhone(phone)) {
            throw new ValidationException("Số điện thoại phải có 10 số và bắt đầu bằng 0!");
        }
        if (!InputValidator.isEmail(email)) {
            throw new ValidationException("Email nhập sai định dạng!");
        }
        if (isCccdUsedByOther(cccd, s)) {
            throw new ValidationException("Số CCCD này đã được đăng ký cho tài khoản khác!");
        }
        validateNewPassword(s, newPassword, confirm);
        s.setFullName(name);
        s.setAddress(address);
        s.setEncryptedCccd(crypto.encrypt(cccd));
        s.setPhone(phone);
        s.setEmail(email);
        s.setPasswordHash(PasswordHasher.hash(newPassword));
        s.setFirstLogin(false);
        s.setMustChangePassword(false);
        log(s.getUsername(), "Kích hoạt tài khoản", "Cập nhật thông tin lần đầu và thay mật khẩu tạm", true,
                LogLevel.INFO);
        saveUsers();
    }

    public void changePassword(User user, char[] current, char[] newPassword, char[] confirm)
            throws AuthException, ValidationException {
        requireActive(user);
        if (!PasswordHasher.verify(current, user.getPasswordHash())) {
            log(user.getUsername(), "Đổi mật khẩu", "Sai mật khẩu hiện tại", false, LogLevel.WARNING);
            throw new AuthException(AuthException.Reason.REAUTH_FAILED, 0);
        }
        validateNewPassword(user, newPassword, confirm);
        user.setPasswordHash(PasswordHasher.hash(newPassword));
        user.setMustChangePassword(false);
        log(user.getUsername(), "Đổi mật khẩu", "Thành công", true, LogLevel.INFO);
        saveUsers();
    }

    public void resetForgottenPassword(String username, String email, String cccdLast4, char[] newPassword,
                                       char[] confirm) throws AuthException, ValidationException {
        LocalDateTime now = now();
        User user = findUser(trim(username));
        if (user != null && user.isLocked(now)) {
            log(user.getUsername(), "Quên mật khẩu", "Bị từ chối: tài khoản đang khoá", false, LogLevel.WARNING);
            throw new AuthException(AuthException.Reason.LOCKED, minutesLeft(user, now));
        }
        boolean verified = false;
        if (user instanceof Student) {
            Student s = (Student) user;
            String last4 = trim(cccdLast4);
            String cccd = s.getEncryptedCccd() == null ? null : crypto.decrypt(s.getEncryptedCccd());
            verified = s.getEmail() != null && s.getEmail().equalsIgnoreCase(trim(email))
                    && last4.matches("\\d{4}") && cccd != null && cccd.endsWith(last4);
        }
        if (!verified) {
            if (user == null) {
                log(trim(username), "Quên mật khẩu", "Tài khoản không tồn tại", false, LogLevel.WARNING);
            } else {
                registerFailure(user, now, "Quên mật khẩu", "Thông tin xác minh không khớp");
                if (user.isLocked(now)) {
                    throw new AuthException(AuthException.Reason.LOCKED, LOCK_MINUTES);
                }
            }
            throw new AuthException(AuthException.Reason.INVALID_CREDENTIALS, 0);
        }
        validateNewPassword(user, newPassword, confirm);
        user.setPasswordHash(PasswordHasher.hash(newPassword));
        user.setFailedAttempts(0);
        user.setMustChangePassword(false);
        log(user.getUsername(), "Quên mật khẩu", "Đặt lại mật khẩu sau khi xác minh email + CCCD", true,
                LogLevel.WARNING);
        saveUsers();
    }

    private void validateNewPassword(User user, char[] password, char[] confirm) throws ValidationException {
        if (password == null || !Arrays.equals(password, confirm)) {
            throw new ValidationException("Mật khẩu nhập lại không khớp!");
        }
        String error = InputValidator.checkPasswordPolicy(password, user.getUsername());
        if (error != null) {
            throw new ValidationException(error);
        }
        if (PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new ValidationException("Mật khẩu mới phải khác mật khẩu hiện tại!");
        }
    }

    public String maskedCccd(Student s) {
        return InputValidator.maskCccd(s.getEncryptedCccd() == null ? null : crypto.decrypt(s.getEncryptedCccd()));
    }

    public String maskedPhone(Student s) {
        return InputValidator.maskPhone(s.getPhone());
    }

    public String[] revealSensitive(User actor, Student target, char[] actorPassword) throws AuthException {
        requireActive(actor);
        if (actor != target) {
            requireAdmin(actor, "Xem CCCD của " + target.getStudentId());
        }
        if (!PasswordHasher.verify(actorPassword, actor.getPasswordHash())) {
            log(actor.getUsername(), "Xem CCCD", "Xác thực lại thất bại", false, LogLevel.WARNING);
            throw new AuthException(AuthException.Reason.REAUTH_FAILED, 0);
        }
        String cccd = target.getEncryptedCccd() == null ? "(chưa cập nhật)" : crypto.decrypt(target.getEncryptedCccd());
        log(actor.getUsername(), "Xem CCCD", "Xem CCCD/SĐT đầy đủ của " + target.getStudentId(), true, LogLevel.INFO);
        return new String[]{cccd, target.getPhone() == null ? "(chưa cập nhật)" : target.getPhone()};
    }

    private boolean isCccdUsedByOther(String cccd, Student self) {
        for (User u : users) {
            if (u instanceof Student && u != self) {
                String enc = ((Student) u).getEncryptedCccd();
                if (enc != null && cccd.equals(crypto.decrypt(enc))) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Student> getStudents(User actor) {
        requireAdmin(actor, "Xem danh sách sinh viên");
        List<Student> list = new ArrayList<>();
        for (User u : users) {
            if (u instanceof Student) {
                list.add((Student) u);
            }
        }
        return list;
    }

    public String addStudent(User actor, String studentId, String fullName, String className, String username)
            throws ValidationException {
        requireAdmin(actor, "Thêm sinh viên");
        studentId = trim(studentId).toUpperCase();
        fullName = trim(fullName);
        className = trim(className);
        username = trim(username);
        if (!InputValidator.isStudentId(studentId)) {
            throw new ValidationException("Mã sinh viên dạng SV + 3–8 chữ số, ví dụ SV026.");
        }
        if (!InputValidator.isName(fullName)) {
            throw new ValidationException("Họ tên chỉ gồm chữ cái và khoảng trắng (2–50 ký tự).");
        }
        if (!InputValidator.isClassName(className)) {
            throw new ValidationException("Tên lớp chỉ gồm chữ, số và dấu gạch ngang, ví dụ CNTT-K18A.");
        }
        if (!InputValidator.isUsername(username)) {
            throw new ValidationException("Tên đăng nhập 3–20 ký tự: chữ thường, số, dấu chấm hoặc gạch dưới.");
        }
        if (findUser(username) != null) {
            throw new ValidationException("Tên đăng nhập này đã có người dùng!");
        }
        if (findStudent(studentId) != null) {
            throw new ValidationException("Mã sinh viên đã tồn tại!");
        }
        String temp = newTemporaryPassword(username);
        Student s = new Student(studentId, username, PasswordHasher.hash(temp.toCharArray()), fullName, className);
        s.setMustChangePassword(true);
        users.add(s);
        log(actor.getUsername(), "Thêm sinh viên", studentId + " · tài khoản " + username + " · cấp mật khẩu tạm",
                true, LogLevel.INFO);
        saveUsers();
        return temp;
    }

    public void removeStudents(User actor, List<String> studentIds) {
        requireAdmin(actor, "Xoá sinh viên");
        for (String id : studentIds) {
            Student s = findStudent(id);
            if (s != null) {
                users.remove(s);
                grades.removeIf(g -> g.getStudentId().equals(id));
                log(actor.getUsername(), "Xoá sinh viên", id + " (" + s.getFullName() + ") và toàn bộ điểm", true,
                        LogLevel.WARNING);
            }
        }
        saveUsers();
        saveGrades();
    }

    public List<Grade> getGrades(User actor, String studentId) {
        requireActive(actor);
        if (!actor.isAdmin()) {
            boolean own = actor instanceof Student && ((Student) actor).getStudentId().equals(studentId);
            if (!own) {
                log(actor.getUsername(), "Truy cập trái phép", "Cố xem điểm của " + studentId, false, LogLevel.ALERT);
                throw new SecurityException("Bạn chỉ được xem điểm của chính mình.");
            }
        }
        List<Grade> list = new ArrayList<>();
        for (Grade g : grades) {
            if (g.getStudentId().equals(studentId)) {
                list.add(g);
            }
        }
        return list;
    }

    public List<Grade> getAllGrades(User actor) {
        requireAdmin(actor, "Xem bảng điểm toàn trường");
        return new ArrayList<>(grades);
    }

    public void saveGrade(User actor, String studentId, String subject, double score) throws ValidationException {
        requireAdmin(actor, "Nhập/sửa điểm");
        subject = trim(subject);
        if (findStudent(studentId) == null) {
            throw new ValidationException("Không tìm thấy sinh viên " + studentId + ".");
        }
        if (!InputValidator.isSubject(subject)) {
            throw new ValidationException("Tên môn học không hợp lệ.");
        }
        if (!InputValidator.isScore(score)) {
            throw new ValidationException("Điểm phải nằm trong khoảng 0 – 10.");
        }
        for (Grade g : grades) {
            if (g.getStudentId().equals(studentId) && g.getSubject().equalsIgnoreCase(subject)) {
                double old = g.getScore();
                g.setScore(score);
                log(actor.getUsername(), "Sửa điểm", studentId + " · " + g.getSubject() + ": " + old + " → " + score,
                        true, LogLevel.WARNING);
                saveGrades();
                return;
            }
        }
        grades.add(new Grade(studentId, subject, score));
        log(actor.getUsername(), "Nhập điểm", studentId + " · " + subject + ": " + score, true, LogLevel.INFO);
        saveGrades();
    }

    public void deleteGrade(User actor, String studentId, String subject) {
        requireAdmin(actor, "Xoá điểm");
        Grade found = null;
        for (Grade g : grades) {
            if (g.getStudentId().equals(studentId) && g.getSubject().equals(subject)) {
                found = g;
            }
        }
        if (found != null) {
            grades.remove(found);
            log(actor.getUsername(), "Xoá điểm", studentId + " · " + subject + " (" + found.getScore() + ")", true,
                    LogLevel.WARNING);
            saveGrades();
        }
    }

    public List<User> getAllUsers(User actor) {
        requireAdmin(actor, "Xem danh sách tài khoản");
        return new ArrayList<>(users);
    }

    public void unlockAccount(User actor, String username) {
        requireAdmin(actor, "Mở khoá tài khoản");
        User u = findUser(username);
        if (u != null) {
            u.setLockedUntil(null);
            u.setFailedAttempts(0);
            log(actor.getUsername(), "Mở khoá tài khoản", username, true, LogLevel.INFO);
            saveUsers();
        }
    }

    public String resetPassword(User actor, String username) throws ValidationException {
        requireAdmin(actor, "Đặt lại mật khẩu");
        User u = findUser(username);
        if (u == null) {
            throw new ValidationException("Không tìm thấy tài khoản.");
        }
        if (u == actor) {
            throw new ValidationException("Hãy dùng chức năng Đổi mật khẩu cho tài khoản của chính bạn.");
        }
        String temp = newTemporaryPassword(username);
        u.setPasswordHash(PasswordHasher.hash(temp.toCharArray()));
        u.setMustChangePassword(true);
        u.setLockedUntil(null);
        u.setFailedAttempts(0);
        log(actor.getUsername(), "Đặt lại mật khẩu", "Cấp mật khẩu tạm cho " + username, true, LogLevel.WARNING);
        saveUsers();
        return temp;
    }

    private static String newTemporaryPassword(String username) {
        String temp;
        do {
            temp = PasswordHasher.generateTemporaryPassword();
        } while (InputValidator.checkPasswordPolicy(temp.toCharArray(), username) != null);
        return temp;
    }

    public String accountStatus(User u) {
        LocalDateTime now = now();
        if (u.isLocked(now)) {
            return "Đang khoá đến " + u.getLockedUntil().format(HM);
        }
        if (u instanceof Student && ((Student) u).isFirstLogin()) {
            return "Chờ kích hoạt";
        }
        if (u.isMustChangePassword()) {
            return "Phải đổi mật khẩu";
        }
        return "Hoạt động";
    }

    public List<LogEntry> getLogs(User actor) {
        requireAdmin(actor, "Xem nhật ký hệ thống");
        List<LogEntry> list = new ArrayList<>(logs);
        Collections.reverse(list);
        return list;
    }

    public List<LogEntry> getOwnSecurityHistory(User actor) {
        requireActive(actor);
        Set<String> actions = new LinkedHashSet<>(Arrays.asList("Đăng nhập", "Đăng xuất", "Hết phiên",
                "Khoá tài khoản", "Đổi mật khẩu", "Quên mật khẩu", "Kích hoạt tài khoản", "Xem CCCD"));
        List<LogEntry> list = new ArrayList<>();
        for (LogEntry e : logs) {
            if (e.getUsername().equals(actor.getUsername()) && actions.contains(e.getAction())) {
                list.add(e);
            }
        }
        Collections.reverse(list);
        return list;
    }

    public List<String[]> computeSecurityStats(User actor) {
        requireAdmin(actor, "Thống kê bảo mật");
        int ok = 0;
        int fail = 0;
        int locks = 0;
        int denied = 0;
        int views = 0;
        int gradeChanges = 0;
        Map<String, Integer> failedBy = new HashMap<>();
        for (LogEntry e : logs) {
            switch (e.getAction()) {
                case "Đăng nhập":
                    if (e.isSuccess()) {
                        ok++;
                    } else {
                        fail++;
                        failedBy.merge(e.getUsername(), 1, Integer::sum);
                    }
                    break;
                case "Khoá tài khoản":
                    locks++;
                    if (e.getDetail().contains("(đăng nhập)")) {
                        fail++;
                    }
                    failedBy.merge(e.getUsername(), 1, Integer::sum);
                    break;
                case "Truy cập trái phép":
                    denied++;
                    break;
                case "Xem CCCD":
                    if (e.isSuccess()) {
                        views++;
                    }
                    break;
                case "Sửa điểm":
                case "Xoá điểm":
                    gradeChanges++;
                    break;
                default:
                    break;
            }
        }
        int lockedNow = 0;
        for (User u : users) {
            if (u.isLocked(now())) {
                lockedNow++;
            }
        }
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Đăng nhập thành công", String.valueOf(ok)});
        rows.add(new String[]{"Đăng nhập thất bại", String.valueOf(fail)});
        rows.add(new String[]{"Số lần khoá tài khoản tự động", String.valueOf(locks)});
        rows.add(new String[]{"Tài khoản đang bị khoá", String.valueOf(lockedNow)});
        rows.add(new String[]{"Truy cập trái phép bị chặn", String.valueOf(denied)});
        rows.add(new String[]{"Lượt xem CCCD/SĐT đầy đủ", String.valueOf(views)});
        rows.add(new String[]{"Lượt sửa / xoá điểm", String.valueOf(gradeChanges)});
        List<Map.Entry<String, Integer>> top = new ArrayList<>(failedBy.entrySet());
        top.sort((a, b) -> b.getValue().equals(a.getValue())
                ? a.getKey().compareTo(b.getKey()) : b.getValue() - a.getValue());
        for (int i = 0; i < Math.min(3, top.size()); i++) {
            Map.Entry<String, Integer> e = top.get(i);
            rows.add(new String[]{"Top " + (i + 1) + " tài khoản bị thử sai: " + e.getKey(), e.getValue() + " lần"});
        }
        return rows;
    }

    public Map<String, Boolean> checkIntegrity(User actor) {
        requireAdmin(actor, "Kiểm tra toàn vẹn dữ liệu");
        Map<String, Boolean> result = new LinkedHashMap<>();
        int okCount = 0;
        for (String f : DATA_FILES) {
            boolean ok = data.verify(f);
            result.put(f, ok);
            if (ok) {
                okCount++;
            }
        }
        boolean allOk = okCount == DATA_FILES.length;
        log(actor.getUsername(), "Kiểm tra toàn vẹn", okCount + "/" + DATA_FILES.length + " file hợp lệ", allOk,
                allOk ? LogLevel.INFO : LogLevel.ALERT);
        return result;
    }

    public List<String> getStartupWarnings() {
        return new ArrayList<>(startupWarnings);
    }

    public User findUser(String username) {
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }

    public Student findStudent(String studentId) {
        for (User u : users) {
            if (u instanceof Student && ((Student) u).getStudentId().equals(studentId)) {
                return (Student) u;
            }
        }
        return null;
    }

    public List<String> getSubjects() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList(DataSeeder.SUBJECTS));
        for (Grade g : grades) {
            set.add(g.getSubject());
        }
        return new ArrayList<>(set);
    }

    public List<String> getClassNames() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList(DataSeeder.CLASSES));
        for (User u : users) {
            if (u instanceof Student) {
                set.add(((Student) u).getClassName());
            }
        }
        return new ArrayList<>(set);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private void requireAdmin(User actor, String action) {
        if (actor == null || !users.contains(actor) || !actor.isAdmin() || !(actor instanceof Admin)) {
            log(actor == null ? "?" : actor.getUsername(), "Truy cập trái phép", action, false, LogLevel.ALERT);
            throw new SecurityException("Bạn không có quyền thực hiện thao tác này.");
        }
    }

    private void requireActive(User actor) {
        if (actor == null || !users.contains(actor)) {
            throw new SecurityException("Phiên làm việc không hợp lệ.");
        }
    }

    private long minutesLeft(User u, LocalDateTime now) {
        long seconds = Duration.between(now, u.getLockedUntil()).getSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }

    private void log(String username, String action, String detail, boolean success, LogLevel level) {
        logs.add(new LogEntry(now(), InputValidator.sanitizeForLog(username), action, detail, success, level));
        persist(LOGS_FILE, logs);
    }

    private void saveUsers() {
        persist(USERS_FILE, users);
    }

    private void saveGrades() {
        persist(GRADES_FILE, grades);
    }

    private <T extends Serializable> void persist(String file, List<T> list) {
        try {
            data.save(list, file);
        } catch (IOException e) {
            throw new UncheckedIOException("Không ghi được " + file, e);
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
