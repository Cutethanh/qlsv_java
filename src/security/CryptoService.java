package qlsv.security;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;

public class CryptoService {
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey aesKey;
    private final SecretKey hmacKey;
    private final SecureRandom random = new SecureRandom();
    private final boolean newKey;

    public CryptoService(File keyFile) throws IOException {
        Properties props = new Properties();
        if (keyFile.exists()) {
            try (InputStream in = new FileInputStream(keyFile)) {
                props.load(in);
            }
        }
        String aes = props.getProperty("aes");
        String hmac = props.getProperty("hmac");
        newKey = aes == null || hmac == null;
        if (newKey) {
            byte[] a = new byte[32];
            byte[] h = new byte[32];
            random.nextBytes(a);
            random.nextBytes(h);
            aes = Base64.getEncoder().encodeToString(a);
            hmac = Base64.getEncoder().encodeToString(h);
            props.setProperty("aes", aes);
            props.setProperty("hmac", hmac);
            File parent = keyFile.getAbsoluteFile().getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (OutputStream out = new FileOutputStream(keyFile)) {
                props.store(out, "Khoa bi mat cua he thong QLSV - khong chia se, khong dua len Git");
            }
            restrictToOwner(keyFile);
        }
        aesKey = new SecretKeySpec(Base64.getDecoder().decode(aes), "AES");
        hmacKey = new SecretKeySpec(Base64.getDecoder().decode(hmac), "HmacSHA256");
    }

    public boolean isNewKey() {
        return newKey;
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Lỗi mã hoá dữ liệu", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] in = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new SecurityException("Dữ liệu mã hoá đã bị sửa đổi hoặc sai khoá", e);
        }
    }

    public byte[] hmac(byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Lỗi tính HMAC", e);
        }
    }

    public boolean verifyHmac(byte[] data, byte[] signature) {
        return signature != null && MessageDigest.isEqual(hmac(data), signature);
    }

    private static void restrictToOwner(File f) {
        f.setReadable(false, false);
        f.setReadable(true, true);
        f.setWritable(false, false);
        f.setWritable(true, true);
    }
}
