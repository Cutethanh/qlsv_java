package qlsv.security;

import java.util.regex.Pattern;

public final class InputValidator {
    private static final Pattern PHONE = Pattern.compile("0\\d{9}");
    private static final Pattern CCCD = Pattern.compile("\\d{12}");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,10}$");
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9_.]{3,20}");
    private static final Pattern STUDENT_ID = Pattern.compile("SV\\d{3,8}");
    private static final Pattern NAME = Pattern.compile("[\\p{L} ]{2,50}");
    private static final Pattern CLASS_NAME = Pattern.compile("[A-Za-z0-9-]{2,20}");
    private static final Pattern SUBJECT = Pattern.compile("[\\p{L}\\p{N} .,+#()-]{2,60}");

    public static final String PASSWORD_RULE =
            "Mật khẩu ≥ 8 ký tự, gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt; không chứa tên đăng nhập.";

    private InputValidator() {
    }

    public static boolean isPhone(String s) { return s != null && PHONE.matcher(s).matches(); }

    public static boolean isCccd(String s) { return s != null && CCCD.matcher(s).matches(); }

    public static boolean isEmail(String s) { return s != null && s.length() <= 80 && EMAIL.matcher(s).matches(); }

    public static boolean isUsername(String s) { return s != null && USERNAME.matcher(s).matches(); }

    public static boolean isStudentId(String s) { return s != null && STUDENT_ID.matcher(s).matches(); }

    public static boolean isName(String s) { return s != null && NAME.matcher(s).matches() && !s.trim().isEmpty(); }

    public static boolean isClassName(String s) { return s != null && CLASS_NAME.matcher(s).matches(); }

    public static boolean isSubject(String s) { return s != null && SUBJECT.matcher(s).matches(); }

    public static boolean isScore(double d) { return d >= 0 && d <= 10; }

    public static String checkPasswordPolicy(char[] pw, String username) {
        if (pw == null || pw.length < 8) {
            return "Mật khẩu phải có ít nhất 8 ký tự.";
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (char c : pw) {
            if (Character.isUpperCase(c)) {
                upper = true;
            } else if (Character.isLowerCase(c)) {
                lower = true;
            } else if (Character.isDigit(c)) {
                digit = true;
            } else if (!Character.isWhitespace(c)) {
                special = true;
            }
        }
        if (!(upper && lower && digit && special)) {
            return "Mật khẩu phải gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt.";
        }
        if (username != null && !username.isEmpty()
                && new String(pw).toLowerCase().contains(username.toLowerCase())) {
            return "Mật khẩu không được chứa tên đăng nhập.";
        }
        return null;
    }

    public static String maskCccd(String cccd) {
        if (cccd == null || cccd.length() < 4) {
            return "(chưa cập nhật)";
        }
        return "********" + cccd.substring(cccd.length() - 4);
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "(chưa cập nhật)";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    public static String sanitizeForLog(String s) {
        if (s == null) {
            return "";
        }
        String clean = s.replaceAll("\\p{Cntrl}", "?");
        return clean.length() > 40 ? clean.substring(0, 40) + "…" : clean;
    }
}
