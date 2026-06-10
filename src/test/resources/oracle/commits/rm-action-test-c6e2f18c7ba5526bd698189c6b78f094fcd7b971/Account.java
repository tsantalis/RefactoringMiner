public class Account {
    protected String username;

    public Account(String username) {
        this.username = username;
    }

    public String displayName() {
        return username.trim().toUpperCase();
    }
}
