open class Account(protected val username: String) {
    open fun displayName(): String {
        return username.trim().uppercase()
    }
}
