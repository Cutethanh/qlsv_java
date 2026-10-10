package qlsv.model;

public class Student extends User {
    private static final long serialVersionUID = 1L;

    private final String studentId;
    private String fullName;
    private String className;
    private String address;
    private String encryptedCccd;
    private String phone;
    private String email;
    private boolean firstLogin = true;

    public Student(String studentId, String username, String passwordHash, String fullName, String className) {
        super(username, passwordHash);
        this.studentId = studentId;
        this.fullName = fullName;
        this.className = className;
    }

    @Override
    public String getRoleName() { return "Sinh viên"; }

    @Override
    public String getDisplayName() { return fullName; }

    @Override
    public boolean isAdmin() { return false; }

    public String getStudentId() { return studentId; }

    public String getFullName() { return fullName; }

    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getClassName() { return className; }

    public void setClassName(String className) { this.className = className; }

    public String getAddress() { return address; }

    public void setAddress(String address) { this.address = address; }

    public String getEncryptedCccd() { return encryptedCccd; }

    public void setEncryptedCccd(String encryptedCccd) { this.encryptedCccd = encryptedCccd; }

    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public boolean isFirstLogin() { return firstLogin; }

    public void setFirstLogin(boolean firstLogin) { this.firstLogin = firstLogin; }
}
