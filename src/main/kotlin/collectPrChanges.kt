import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File

fun collectPrChanges(
    args: Array<String>,
    httpUrl: HttpUrl = HttpUrl.get("https://api.github.com")
): Pair<Int, String> {

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl(httpUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    var failedOnEvent = true
    return try {
        val event = createGithubEvent(args[ARGS_INDEX_EVENT_FILE_PATH], moshi)
            .also { failedOnEvent = false }
        val changes = collectChanges(args[ARGS_INDEX_TOKEN], retrofit, event)
            .filterNot { file -> file.status == STATUS_REMOVED }
            .filter { file -> file.filename.endsWith(".kt") }
        Pair(EXIT_CODE_SUCCESS, changes.joinToString(" ") { file -> file.filename })
    } catch (ex: Throwable) {
        val prefix = if (failedOnEvent)
            "Error while getting the event" else
            "Error while getting the changes"
        val errorMessage = if (ex.message.isNullOrBlank())
            "Unknown error: ${ex.javaClass.name}" else
            ex.message
        Pair(EXIT_CODE_FAILURE, "$prefix: $errorMessage")
    }
}

private const val EXIT_CODE_SUCCESS = 0
private const val EXIT_CODE_FAILURE = -1
private const val ARGS_INDEX_EVENT_FILE_PATH = 0
private const val ARGS_INDEX_TOKEN = 1
private const val STATUS_REMOVED = "removed"

private fun createGithubEvent(
    eventFilePath: String,
    moshi: Moshi
): GithubEvent {

    val json = File(eventFilePath).readText()
    return moshi
        .adapter(GithubEvent::class.java)
        .fromJson(json)
        ?: throw Exception("Could not create json from file: $eventFilePath")
}

private fun collectChanges(
    token: String,
    retrofit: Retrofit,
    event: GithubEvent
): List<GithubChangedFile> {

    val startingPage = 1
    return retrofit
        .create(GithubService::class.java)
        .collectAllPrChanges(token, event, startingPage)
}

private fun GithubService.collectAllPrChanges(
    token: String,
    event: GithubEvent,
    startingPage: Int
): List<GithubChangedFile> {

    val changesFromCurrentPage = executeGetPullRequestFiles(token, event, startingPage)
    val changesFromNextPage = if (changesFromCurrentPage.isEmpty())
        emptyList() else
        collectAllPrChanges(token, event, startingPage + 1)
    return changesFromCurrentPage + changesFromNextPage
}

private fun GithubService.executeGetPullRequestFiles(
    token: String,
    event: GithubEvent,
    startingPage: Int
): List<GithubChangedFile> {

    val requestFiles = getPullRequestFiles(
        "token $token",
        event.pull_request.user.login,
        event.repository.name,
        event.pull_request.number,
        startingPage
    )
    return requestFiles
        .execute()
        .body()
        ?: emptyList()
}

// event
private class GithubUser(val login: String)
private class GithubRepository(val name: String)
private class GithubPullRequest(val number: Int, val user: GithubUser)
private data class GithubEvent(val pull_request: GithubPullRequest, val repository: GithubRepository)

// changes
private interface GithubService {
    @GET("/repos/{owner}/{repo}/pulls/{pull_number}/files")
    fun getPullRequestFiles(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") number: Int,
        @Query("page") page: Int
    ): Call<List<GithubChangedFile>>
}

private class GithubChangedFile(val filename: String, val status: String)