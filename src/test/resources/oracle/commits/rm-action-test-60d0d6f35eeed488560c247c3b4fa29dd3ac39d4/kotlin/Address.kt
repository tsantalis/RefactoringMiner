class Address(
    private val street: String,
    private val city: String,
    private val postalCode: String
) {
    fun format(): String {
        return "$street\n$city $postalCode"
    }
}
