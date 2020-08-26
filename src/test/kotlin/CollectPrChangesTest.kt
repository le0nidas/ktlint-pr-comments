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
import org.junit.jupiter.api.assertAll
import java.io.File


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
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -0,0 +1,8 @@\nblah blah\"" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Dockerfile\",\n" +
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -0,0 +2,5 @@\nblah blah\"" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/action.yml\",\n" +
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -0,0 +22,8 @@\nblah blah\"" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main2.kt\",\n" +
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -15,8 +15,6 @@ blah blah\n  @@ -98,6 +96,7 @@ blah blah\n@@ -206,8 +205,6 @@ blah blah\n  @JvmStatic\"" +
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

        val result = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertAll(
            { assertThat(result, equalTo(0))},
            {
                assertThat(
                    File("collection-report.txt").readText(),
                    equalTo("src/main/kotlin/Main.kt 1,8\nsrc/main/kotlin/Main2.kt 15,6 96,7 205,6")
                )
            }
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
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -0,0 +1,8 @@\nblah blah\"" +
                                    "  }\n" +
                                    "]"
                        )
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=2" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main2.kt\",\n" +
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -0,0 +1,8 @@\nblah blah\"" +
                                    "  }\n" +
                                    "]"
                        )
                    "/repos/le0nidas/ktlint-playground/pulls/7/files?page=3" ->
                        MockResponse().setBody(
                            "[\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main3.kt\",\n" +
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -0,0 +1,8 @@\nblah blah\"" +
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

        val result = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertAll(
            { assertThat(result, equalTo(0))},
            {
                assertThat(
                    File("collection-report.txt").readText(),
                    equalTo("src/main/kotlin/Main.kt 1,8\nsrc/main/kotlin/Main2.kt 1,8\nsrc/main/kotlin/Main3.kt 1,8")
                )
            }
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
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -0,0 +1,8 @@\nblah blah\"" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main2.kt\",\n" +
                                    "    \"status\": \"removed\",\n" +
                                    "    \"patch\": \"@@ -0,0 +1,8 @@\nblah blah\"" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main3.kt\",\n" +
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -0,0 +1,8 @@\nblah blah\"" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/Main4.kt\",\n" +
                                    "    \"status\": \"modified\",\n" +
                                    "    \"patch\": \"@@ -0,0 +1,8 @@\nblah blah\"" +
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

        val result = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertAll(
            { assertThat(result, equalTo(0))},
            {
                assertThat(
                    File("collection-report.txt").readText(),
                    equalTo("src/main/kotlin/Main.kt 1,8\nsrc/main/kotlin/Main3.kt 1,8\nsrc/main/kotlin/Main4.kt 1,8")
                )
            }
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

        val result = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertAll(
            { assertThat(result, equalTo(0))},
            { assertThat(File("collection-report.txt").readText(), equalTo(""))}
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
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -0,0 +1,8 @@\nblah blah\"" +
                                    "  },\n" +
                                    "  {\n" +
                                    "    \"filename\": \"src/main/kotlin/action.yml\",\n" +
                                    "    \"status\": \"added\",\n" +
                                    "    \"patch\": \"@@ -0,0 +1,8 @@\nblah blah\"" +
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

        val result = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertAll(
            { assertThat(result, equalTo(0))},
            { assertThat(File("collection-report.txt").readText(), equalTo(""))}
        )
    }

    @Test fun `when there is an error while getting the github event it returns the error's message`() {
        val pathToEventFile = CollectPrChangesTest::class.java.classLoader.getResource("not-correct-event.json").path
        val userToken = "abc1234"

        val result = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(result, equalTo(-1))
    }

    @Test fun `when there is an error while getting the changes from github it exits with code -1`() {
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

        val result = collectPrChanges(arrayOf(pathToEventFile, userToken), mockWebServer.url("/"))

        assertThat(result, equalTo(-1))
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
        with(File("collection-report.txt")) {
            if (exists()) delete()
        }
    }
}