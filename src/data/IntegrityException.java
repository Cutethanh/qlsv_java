package qlsv.data;

public class IntegrityException extends Exception {
    private static final long serialVersionUID = 1L;

    public IntegrityException(String fileName) {
        super("File " + fileName + " không khớp chữ ký toàn vẹn (HMAC)");
    }
}
