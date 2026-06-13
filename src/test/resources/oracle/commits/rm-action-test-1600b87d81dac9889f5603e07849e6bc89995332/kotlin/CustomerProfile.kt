class CustomerProfile(
    private val fullName: String,
    private val street: String,
    private val city: String,
    private val postalCode: String,
    var loyaltyPoints: Int
) {
    fun mailingLabel(): String {
        return "$fullName\n$street\n$city $postalCode"
    }

    fun canReceivePromotion(optedIn: Boolean, bouncedEmail: Boolean, purchaseCount: Int): Boolean {
        if (optedIn && !bouncedEmail && purchaseCount > 0) {
            return true
        }
        return false
    }

    fun addPurchase(amountCents: Int) {
        val points = amountCents / 100
        loyaltyPoints += points
    }
}
