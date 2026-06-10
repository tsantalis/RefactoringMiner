public class AdminAccount extends Account {
    private final String accessLevel = "admin";

    public AdminAccount(String username) {
        super(username);
    }

    public boolean canManageUsers() {
        return accessLevel.equals("admin");
    }
}
