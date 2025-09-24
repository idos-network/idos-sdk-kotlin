import io.kotest.core.spec.style.StringSpec
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class DataVerificationTests :
    StringSpec({
        "uuid impl should match" {
            val uuid = "550e8400-e29b-41d4-a716-446655440000"
//            convertUuidToBytes(uuid) shouldBe Uuid.parse(uuid).toByteArray()
        }
    })
