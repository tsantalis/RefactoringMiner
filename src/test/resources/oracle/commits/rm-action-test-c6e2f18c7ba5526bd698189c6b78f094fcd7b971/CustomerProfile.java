public class CustomerProfile {
    private String displayName;
    private Address address;
    private int loyaltyPoints;

    public CustomerProfile(String fullName, String street, String city, String postalCode, int loyaltyPoints) {
        this.displayName = fullName;
        this.address = new Address(street, city, postalCode);
        this.loyaltyPoints = loyaltyPoints;
    }

    public String mailingLabel() {
        return displayName + "\n" + address.format();
    }

    public boolean canReceivePromotion(boolean emailOptedIn, boolean bouncedEmail, int purchaseCount) {
        if (!emailOptedIn || bouncedEmail || purchaseCount <= 0) {
            return false;
        }
        return true;
    }

    public void addPurchase(int amountCents) {
        int earnedPoints = amountCents / 100;
        loyaltyPoints += earnedPoints;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }
}
