public class GuestAccount extends Account {
    public GuestAccount(String username) {
        super(username);
    }

    public boolean canBrowseCatalog() {
        return username.length() > 0;
    }
}
