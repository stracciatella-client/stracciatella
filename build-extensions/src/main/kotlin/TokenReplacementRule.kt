import org.gradle.api.tasks.Nested
import java.io.Serializable

class TokenReplacementRule(@Nested val regex: SerializableRegex, val value: String) : Serializable {
}