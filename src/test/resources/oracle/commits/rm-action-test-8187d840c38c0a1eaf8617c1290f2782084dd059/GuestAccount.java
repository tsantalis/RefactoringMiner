public class GuestAccount {
    protected String username;

    public GuestAccount(String username) {
        this.username = username;
    }

    public String displayName() {
        return username.trim().toUpperCase();
    }

    public boolean canBrowseCatalog() {
        return username.length() > 0;
    }
}
