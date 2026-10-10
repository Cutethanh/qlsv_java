package qlsv.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {
    public static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        return ITERATIONS + ":" + b64(salt) + ":" + b64(derived);
    }

    public static boolean verify(char[] password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split(":");
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không khởi tạo được PBKDF2", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static String generateTemporaryPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnpqrstuvwxyz";
        String digits = "23456789";
        String special = "@#$%&*!?";
        String all = upper + lower + digits + special;
        char[] pw = new char[12];
        pw[0] = pick(upper);
        pw[1] = pick(lower);
        pw[2] = pick(digits);
        pw[3] = pick(special);
        for (int i = 4; i < pw.length; i++) {
            pw[i] = pick(all);
        }
        for (int i = pw.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char t = pw[i];
            pw[i] = pw[j];
            pw[j] = t;
        }
        return new String(pw);
    }

    public static String preview(String stored) {
        if (stored == null) {
            return "";
        }
        String[] parts = stored.split(":");
        if (parts.length != 3) {
            return "(không hợp lệ)";
        }
        return parts[0] + " vòng · salt " + parts[1].substring(0, 6) + "… · " + parts[2].substring(0, 10) + "…";
    }

    private static char pick(String s) {
        return s.charAt(RANDOM.nextInt(s.length()));
    }

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
