class CustomerProfile(
    fullName: String,
    street: String,
    city: String,
    postalCode: String,
    private var loyaltyPoints: Int
) {
    private val displayName: String = fullName
    private val address: Address = Address(street, city, postalCode)

    fun mailingLabel(): String {
        return "$displayName\n${address.format()}"
    }

    fun canReceivePromotion(emailOptedIn: Boolean, bouncedEmail: Boolean, purchaseCount: Int): Boolean {
        if (!emailOptedIn || bouncedEmail || purchaseCount <= 0) {
            return false
        }
        return true
    }

    fun addPurchase(amountCents: Int) {
        val earnedPoints = amountCents / 100
        loyaltyPoints += earnedPoints
    }

    fun getLoyaltyPoints(): Int {
        return loyaltyPoints
    }
}
