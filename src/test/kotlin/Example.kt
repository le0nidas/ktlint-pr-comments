import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class Example {
    @Test fun `assertion works`() {
        assertThat(1 + 1, equalTo(2))
    }
}