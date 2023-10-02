import org.gradle.api.tasks.Internal
import java.io.Serializable

class SerializableRegex(val regex: String, val regexOptions: Set<RegexOption>) : Serializable {

    constructor(regex: Regex) : this(regex.pattern, regex.options)

    @Internal
    @Transient
    private var compiledRegex: Regex? = null

    fun compiled(): Regex {
        if (compiledRegex == null) compiledRegex = Regex(pattern = regex, options = regexOptions)
        return compiledRegex!!
    }
}