public class CustomerProfile {
    private String fullName;
    private String street;
    private String city;
    private String postalCode;
    public int loyaltyPoints;

    public CustomerProfile(String fullName, String street, String city, String postalCode, int loyaltyPoints) {
        this.fullName = fullName;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.loyaltyPoints = loyaltyPoints;
    }

    public String mailingLabel() {
        return fullName + "\n" + street + "\n" + city + " " + postalCode;
    }

    public boolean canReceivePromotion(boolean optedIn, boolean bouncedEmail, int purchaseCount) {
        if (optedIn && !bouncedEmail && purchaseCount > 0) {
            return true;
        }
        return false;
    }

    public void addPurchase(int amountCents) {
        int points = amountCents / 100;
        loyaltyPoints += points;
    }
}
