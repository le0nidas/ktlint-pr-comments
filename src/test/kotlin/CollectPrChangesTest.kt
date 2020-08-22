@file:Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CollectPrChangesTest {
    @Test fun `the event triggers the collection of all kt-related changes`() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=1" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main.kt\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Dockerfile\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/action.yml\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main2.kt\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  }\n" +
                                    "]"
                        )
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=2" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "]"
                        )
                    else ->
                        throw IllegalArgumentException("Unknown path: ${request.path}")
                }
            }
        }

        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("event.json").path
        val userToken = "abc1234"

        val actual = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(
            actual,
            equalTo(Pair(0, "src/main/kotlin/Main.kt src/main/kotlin/Main2.kt"))
        )
    }

    @Test fun `the provided token is being sent in the request's header`() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=1" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main.kt\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  }\n" +
                                    "]"
                        )
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=2" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "]"
                        )
                    else ->
                        throw IllegalArgumentException("Unknown path: ${request.path}")
                }
            }
        }

        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("event.json").path
        val userToken = "abc1234"

        collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(mockWebServer.takeRequest().headers["Authorization"], equalTo("token abc1234"))
    }

    @Test fun `collect changes from all pages`() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=1" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main.kt\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  }\n" +
                                    "]"
                        )
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=2" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main2.kt\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  }\n" +
                                    "]"
                        )
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=3" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main3.kt\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  }\n" +
                                    "]"
                        )
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=4" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "]"
                        )
                    else ->
                        throw IllegalArgumentException("Unknown path: ${request.path}")
                }
            }
        }
        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("event.json").path
        val userToken = "abc1234"

        val actual = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(
            actual,
            equalTo(Pair(0, "src/main/kotlin/Main.kt src/main/kotlin/Main2.kt src/main/kotlin/Main3.kt"))
        )
    }

    @Test fun `keep only additions and modifications`() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=1" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main.kt\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main2.kt\",\n" +
                                    "    \"status\": \"removed\"\n" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main3.kt\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main4.kt\",\n" +
                                    "    \"status\": \"modified\"\n" +
                                    "  }\n" +
                                    "]"
                        )
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=2" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "]"
                        )
                    else ->
                        throw IllegalArgumentException("Unknown path: ${request.path}")
                }
            }
        }
        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("event.json").path
        val userToken = "abc1234"

        val actual = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(
            actual,
            equalTo(Pair(0, "src/main/kotlin/Main.kt src/main/kotlin/Main3.kt src/main/kotlin/Main4.kt"))
        )
    }

    @Test fun `when there are no changed files it returns an empty string`() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=1" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "]"
                        )
                    else ->
                        throw IllegalArgumentException("Unknown path: ${request.path}")
                }
            }
        }
        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("event.json").path
        val userToken = "abc1234"

        val actual = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(
            actual,
            equalTo(Pair(0, ""))
        )
    }

    @Test fun `when there are no kt files changed it returns an empty string`() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=1" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Dockerfile\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/action.yml\",\n" +
                                    "    \"status\": \"added\"\n" +
                                    "  }\n" +
                                    "]"
                        )
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=2" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "]"
                        )
                    else ->
                        throw IllegalArgumentException("Unknown path: ${request.path}")
                }
            }
        }
        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("event.json").path
        val userToken = "abc1234"

        val actual = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(
            actual,
            equalTo(Pair(0, ""))
        )
    }

    @Test fun `when there is an error while getting the github event it returns the error's message`() {
        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("not-correct-event.json").path
        val userToken = "abc1234"

        val actual = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(
            actual,
            equalTo(Pair(-1, "Error while getting the event: Required value 'pull_request' missing at \$"))
        )
    }

    @Test fun `when there is an error while getting the changes from github it returns the error's message`() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=1" ->
                        MockResponse().setBody(
                            "{\n" +
                                    "  \"message\": \"Bad credentials\",\n" +
                                    "  \"documentation_url\": \"https://docs.github.com/rest\"\n" +
                                    "}\n"
                        )
                    else ->
                        throw IllegalArgumentException("Unknown path: ${request.path}")
                }
            }
        }
        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("event.json").path
        val userToken = "abc1234"

        val actual = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(
            actual,
            equalTo(Pair(-1, "Error while getting the changes: Expected BEGIN_ARRAY but was BEGIN_OBJECT at path \$"))
        )
    }

    lateinit var mockWebServer: MockWebServer

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }
}