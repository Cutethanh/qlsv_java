package qlsv.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    private final String fullName;

    public Admin(String username, String passwordHash, String fullName) {
        super(username, passwordHash);
        this.fullName = fullName;
    }

    @Override
    public String getRoleName() { return "Quản trị viên"; }

    @Override
    public String getDisplayName() { return fullName; }

    @Override
    public boolean isAdmin() { return true; }
}
