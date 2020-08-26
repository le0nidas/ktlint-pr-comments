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
import java.lang.Thread.sleep


fun makePrComments(
    args: Array<String>,
    httpUrl: HttpUrl = HttpUrl.get(Common.URL_GITHUB),
    collectionReportPath: String = Common.COLLECTION_REPORT,
    ktlintReportPath: String = Common.KTLINT_REPORT
): Int {

    debug("fun makePrComments: args=${args[Common.ARGS_INDEX_EVENT_FILE_PATH]}|collectionReportPath=$collectionReportPath|ktlintReportPath=$ktlintReportPath")

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl(httpUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    return try {
        val allChanges = loadChanges(collectionReportPath)
        val report = createKtlintReport(ktlintReportPath, moshi)
        val event = createGithubEvent(args[Common.ARGS_INDEX_EVENT_FILE_PATH], moshi)
        val comments = convertKtlintReportToGithubPrComments(report, event, allChanges.map { it.name })
        val commentsInDiff = comments
            .filter { comment ->
                val fileChanges = allChanges.first { change -> change.name == comment.path }
                comment.line in fileChanges
            }
        val token = args[Common.ARGS_INDEX_TOKEN]
        makeComments(commentsInDiff, token, event, retrofit)

        Common.EXIT_CODE_SUCCESS
    } catch (ex: Throwable) {
        val errorMessage = if (ex.message.isNullOrBlank())
            "Unknown error: ${ex.javaClass.name}" else
            ex.message
        error("while making PR comments: $errorMessage")
        Common.EXIT_CODE_FAILURE
    }
}

// changes in files:
fun loadChanges(
    pathToFileWithRelativePaths: String
): List<FileChanges> {

    debug("fun loadRelativePathsOfChangedFiles: pathToFileWithRelativePaths=$pathToFileWithRelativePaths")

    return File(pathToFileWithRelativePaths)
        .readText()
        .split("\n")
        .map { line ->
            val lineParts = line.split(" ")
            val fileName = lineParts.first()
            val patchedAreas = lineParts.subList(1, lineParts.size)
                .map { patch ->
                    val patchAsInt = patch.split(",").map { it.toInt() }
                    patchAsInt[0] until patchAsInt[0] + patchAsInt[1]
                }
            Pair(fileName, patchedAreas)
        }
        .map { FileChanges(it.first, it.second) }
}

data class FileChanges(
    val name: String,
    private val patchedAreas: List<IntRange>
) {

    operator fun contains(line: Int): Boolean {
        return patchedAreas.any { area -> line in area }
    }
}
// -----------------

// extract ktlint errors:
fun createKtlintReport(
    pathToKtlintReport: String, moshi: Moshi
): KtlintReport {

    debug("fun createKtlintReport: pathToKtlintReport=$pathToKtlintReport")

    val json = "{\"errors\": ${File(pathToKtlintReport).readText()}}"
    return moshi.adapter(KtlintReport::class.java)
        .fromJson(json)
        ?: throw Exception("")
}

class KtlintError(val line: Int, val message: String)
class KtlintFileErrors(val file: String, val errors: List<KtlintError>)
class KtlintReport(val errors: List<KtlintFileErrors>)
// ----------------------

// make comment for the PR to github:
fun makeComments(
    comments: List<GithubPrComment>,
    token: String,
    event: GithubEvent,
    retrofit: Retrofit
) {

    debug("fun makeComments: comments=$comments|event=$event")

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
        sleep(250) // wait 1/4 of a second to avoid abusing the rate limit
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
// ----------------------------------

// helping functions:
fun convertKtlintReportToGithubPrComments(
    ktlintReport: KtlintReport,
    event: GithubEvent,
    relativePathsOfChangedFiles: List<String>
): List<GithubPrComment> {

    debug("fun convertKtlintReportToGithubPrComments: event=$event|relativePathsOfChangedFiles=$relativePathsOfChangedFiles")

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
// -------------------