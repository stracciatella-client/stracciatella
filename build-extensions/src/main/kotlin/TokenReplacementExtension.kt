import java.util.function.Supplier

class TokenReplacementExtension {
    val rules: MutableCollection<TokenReplacementRule> = ArrayList()
    val excludeFiles: MutableCollection<SerializableRegex> = ArrayList()

    fun replace(token: String, value: Any) {
        replace(token) { value }
    }

    fun replace(token: String, value: Supplier<Any>) {
        replaceRegex(Regex.fromLiteral(token), value)
    }

    fun replaceRegex(regex: String, value: Any) {
        replaceRegex(regex) { value }
    }

    fun replaceRegex(regex: String, value: Supplier<Any>) {
        replaceRegex(Regex(regex), value)
    }

    fun replaceRegex(regex: Regex, value: Supplier<Any>) {
        rules.add(TokenReplacementRule(SerializableRegex(regex.pattern, regex.options), value.get().toString()))
    }

    fun excludeFile(file: String) {
        excludeFileRegex(Regex.fromLiteral(file))
    }

    fun excludeFileRegex(regex: String) {
        excludeFileRegex(Regex(regex))
    }

    fun excludeFileRegex(regex: Regex) {
        excludeFiles.add(SerializableRegex(regex))
    }
}