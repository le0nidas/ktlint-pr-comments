import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.File

fun makePrComments(
    args: Array<String>,
    httpUrl: HttpUrl = HttpUrl.get("https://api.github.com")
): Int {

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl(httpUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    return try {
        val relativePathsOfChangedFiles =
            loadRelativePathsOfChangedFiles(args[PrCommentsConstants.ARGS_INDEX_RELATIVE_PATHS_FILE_PATH])
        val report = createKtlintReport(args[PrCommentsConstants.ARGS_INDEX_KTLINT_REPORT_PATH], moshi)
        val event = createGithubEvent(args[PrCommentsConstants.ARGS_INDEX_EVENT_FILE_PATH], moshi)
        val comments = convertKtlintReportToGithubPrComments(report, event, relativePathsOfChangedFiles)
        val token = args[PrCommentsConstants.ARGS_INDEX_TOKEN]
        makeComments(comments, token, event, retrofit)

        0
    } catch (ex: Throwable) {
        -1
    }
}

private object PrCommentsConstants {
    const val ARGS_INDEX_RELATIVE_PATHS_FILE_PATH = 0
    const val ARGS_INDEX_KTLINT_REPORT_PATH = 1
    const val ARGS_INDEX_EVENT_FILE_PATH = 2
    const val ARGS_INDEX_TOKEN = 3
}

fun loadRelativePathsOfChangedFiles(pathToFileWithRelativePaths: String): List<String> {
    return File(pathToFileWithRelativePaths)
        .readText()
        .split(" ")
}

// extract ktlint errors:
fun createKtlintReport(pathToKtlintReport: String, moshi: Moshi): KtlintReport {
    val json = "{\"errors\": ${File(pathToKtlintReport).readText()}}"
    return moshi.adapter(KtlintReport::class.java)
        .fromJson(json)
        ?: throw Exception("")
}

class KtlintError(val line: Int, val message: String)
class KtlintFileErrors(val file: String, val errors: List<KtlintError>)
class KtlintReport(val errors: List<KtlintFileErrors>)
//------------------------

// make comment for the PR to github:
fun makeComments(comments: List<GithubPrComment>, token: String, event: GithubEvent, retrofit: Retrofit) {
    val githubPrCommentsService = retrofit.create(GithubPrCommentsService::class.java)
    comments.forEach { comment ->
        githubPrCommentsService
            .createComment(
                "token $token",
                event.pull_request.user.login,
                event.repository.name,
                event.pull_request.number,
                comment
            )
            .execute()
    }
}

interface GithubPrCommentsService {
    @POST("/repos/{owner}/{repo}/pulls/{pull_number}/comments")
    fun createComment(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") number: Int,
        @Body comment: GithubPrComment
    ): Call<GithubPrCommentResponse>
}

data class GithubPrComment(
    val body: String,
    val commit_id: String,
    val path: String,
    val line: Int,
    val side: String = "RIGHT"
)

class GithubPrCommentResponse(val url: String)
//------------------------------------

// helping functions:
fun convertKtlintReportToGithubPrComments(
    ktlintReport: KtlintReport,
    event: GithubEvent,
    relativePathsOfChangedFiles: List<String>
): List<GithubPrComment> {
    return ktlintReport
        .errors
        .flatMap { fileErrors ->
            fileErrors.errors.map { ktlintError ->
                val fileName =
                    relativePathsOfChangedFiles.first { relativePath -> fileErrors.file.endsWith(relativePath) }
                GithubPrComment(ktlintError.message, event.pull_request.head.sha, fileName, ktlintError.line)
            }
        }
}
//--------------------