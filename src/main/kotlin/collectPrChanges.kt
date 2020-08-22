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
): CollectionResult {

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val json = File(args[0]).readText()
    val event: GithubEvent = moshi
        .adapter(GithubEvent::class.java)
        .fromJson(json)
        ?: throw Exception("")

    val token = args[1]
    val changedFiles: List<GithubChangedFile> = Retrofit.Builder()
        .baseUrl(httpUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GithubService::class.java)
        .getPullRequestFiles("token $token", event.pull_request.user.login, event.repository.name, event.pull_request.number, 1)
        .execute()
        .body()
        ?: emptyList()

    return CollectedChanges(
        changedFiles.joinToString(" ") { file -> file.filename }
    )
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