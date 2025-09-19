import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class DataVerificationTests :
    StringSpec({
        "uuid impl should match" {
            val uuid = "550e8400-e29b-41d4-a716-446655440000"
//            convertUuidToBytes(uuid) shouldBe Uuid.parse(uuid).toByteArray()
        }
    })
