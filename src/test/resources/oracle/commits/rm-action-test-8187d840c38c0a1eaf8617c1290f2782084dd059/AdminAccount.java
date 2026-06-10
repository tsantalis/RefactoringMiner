public class AdminAccount {
    protected String username;
    private final String accessLevel = "admin";

    public AdminAccount(String username) {
        this.username = username;
    }

    public String displayName() {
        return username.trim().toUpperCase();
    }

    public boolean canManageUsers() {
        return accessLevel.equals("admin");
    }
}
