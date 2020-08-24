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
    httpUrl: HttpUrl = HttpUrl.get(Common.URL_GITHUB)
): Int {

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl(httpUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    var failedOnEvent = true
    return try {
        val event = createGithubEvent(args[Common.ARGS_INDEX_EVENT_FILE_PATH], moshi)
            .also { failedOnEvent = false }
        val changes = collectChanges(args[Common.ARGS_INDEX_TOKEN], retrofit, event)
            .filterNot { file -> file.status == Constants.STATUS_REMOVED }
            .filter { file -> file.filename.endsWith(".kt") }
        saveChanges(changes)
        Common.EXIT_CODE_SUCCESS
    } catch (ex: Throwable) {
        val prefix = if (failedOnEvent)
            "Error while getting the event" else
            "Error while getting the changes"
        val errorMessage = if (ex.message.isNullOrBlank())
            "Unknown error: ${ex.javaClass.name}" else
            ex.message
        Common.EXIT_CODE_FAILURE
    }
}

fun saveChanges(changes: List<GithubChangedFile>) {
    val changesConcatenated = changes.joinToString(" ") { file -> file.filename }
    File(Common.COLLECTION_REPORT).writeText(changesConcatenated)
}

private object Constants {
    const val STATUS_REMOVED = "removed"
}

// collect changes from github:
fun collectChanges(
    token: String,
    retrofit: Retrofit,
    event: GithubEvent
): List<GithubChangedFile> {

    val startingPage = 1
    return retrofit
        .create(GithubService::class.java)
        .collectAllPrChanges(token, event, startingPage)
}

fun GithubService.collectAllPrChanges(
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

fun GithubService.executeGetPullRequestFiles(
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

interface GithubService {
    @GET("/repos/{owner}/{repo}/pulls/{pull_number}/files")
    fun getPullRequestFiles(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") number: Int,
        @Query("page") page: Int
    ): Call<List<GithubChangedFile>>
}

class GithubChangedFile(val filename: String, val status: String)
//------------------------------