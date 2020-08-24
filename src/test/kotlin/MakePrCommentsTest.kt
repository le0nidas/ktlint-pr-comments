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
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class MakePrCommentsTest {
    @Test fun `all ktlint errors are being used to make comments in the pr`() {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/repos/le0nidas/ktlint-playground/pulls/7/comments" ->
                        MockResponse().setBody("{\"url\": \"https://api.github.com/repos/le0nidas/ktlint-playground/pulls/comments/475175865\"}")
                    else ->
                        throw IllegalArgumentException("Unknown path: ${request.path}")
                }
            }
        }
        val pathToRelativePaths = MakePrCommentsTest::class.java.classLoader.getResource("relative-paths.txt").path
        val pathToKtlintReport = MakePrCommentsTest::class.java.classLoader.getResource("ktlint-report.json").path
        val pathToEventFile = MakePrCommentsTest::class.java.classLoader.getResource("event.json").path
        val token = "abc1234"

        makePrComments(arrayOf(pathToEventFile, token), mockWebServer.url("/"), pathToRelativePaths, pathToKtlintReport)

        assertAll(
            assertComment("{\"body\":\"Incorrect modifier order (should be \\\"public abstract\\\")\",\"commit_id\":\"31017d4c19c5a69ac8d5327748cdde2514dba220\",\"path\":\"src/main/kotlin/ConsinstentOrder.kt\",\"line\":5,\"side\":\"RIGHT\"}"),
            assertComment("{\"body\":\"Incorrect modifier order (should be \\\"internal open suspend\\\")\",\"commit_id\":\"31017d4c19c5a69ac8d5327748cdde2514dba220\",\"path\":\"src/main/kotlin/ConsinstentOrder.kt\",\"line\":7,\"side\":\"RIGHT\"}"),
            assertComment("{\"body\":\"Incorrect modifier order (should be \\\"public override\\\")\",\"commit_id\":\"31017d4c19c5a69ac8d5327748cdde2514dba220\",\"path\":\"src/main/kotlin/ConsinstentOrder.kt\",\"line\":13,\"side\":\"RIGHT\"}"),
            assertComment("{\"body\":\"Unnecessary block (\\\"{}\\\")\",\"commit_id\":\"31017d4c19c5a69ac8d5327748cdde2514dba220\",\"path\":\"src/main/kotlin/ConsistentSpacing.kt\",\"line\":15,\"side\":\"RIGHT\"}")
        )
    }

    @Test fun `a ktlint report is being converted to one comment per error`() {
        val event = GithubEvent(
            GithubPullRequest(
                7,
                GithubUser("le0nidas"),
                GithubPullRequestHead("31017d4c19c5a69ac8d5327748cdde2514dba220")
            ),
            GithubRepository("ktlint-playground")
        )
        val report = KtlintReport(
            listOf(
                KtlintFileErrors("/work/github/fileA", listOf(KtlintError(7, "message A 7"), KtlintError(13, "message A 13"))),
                KtlintFileErrors("/work/github/fileB", listOf(KtlintError(17, "message B 17"))),
            )
        )

        val comments = convertKtlintReportToGithubPrComments(report, event, listOf("fileA", "fileB"))

        assertThat(
            comments,
            equalTo(
                listOf(
                    GithubPrComment("message A 7", "31017d4c19c5a69ac8d5327748cdde2514dba220", "fileA", 7),
                    GithubPrComment("message A 13", "31017d4c19c5a69ac8d5327748cdde2514dba220", "fileA", 13),
                    GithubPrComment("message B 17", "31017d4c19c5a69ac8d5327748cdde2514dba220", "fileB", 17)
                )
            )
        )
    }

    private fun assertComment(json: String): () -> Unit = {
        val request = mockWebServer.takeRequest(1, TimeUnit.SECONDS) ?: throw TimeoutException()
        assertThat(request.path, equalTo("/repos/le0nidas/ktlint-playground/pulls/7/comments"))
        assertThat(request.headers["Authorization"], equalTo("token abc1234"))
        assertThat(request.body.readUtf8(), equalTo(json))
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