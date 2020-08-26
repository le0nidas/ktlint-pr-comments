import com.squareup.moshi.Moshi
import java.io.File

fun createGithubEvent(
    eventFilePath: String,
    moshi: Moshi
): GithubEvent {

    debug("fun createGithubEvent: $eventFilePath")

    val json = File(eventFilePath).readText()
    return moshi
        .adapter(GithubEvent::class.java)
        .fromJson(json)
        ?: throw Exception("Could not create json from file: $eventFilePath")
}


data class GithubUser(val login: String)
data class GithubRepository(val name: String)
data class GithubPullRequestHead(val sha: String)
data class GithubPullRequest(val number: Int, val user: GithubUser, val head: GithubPullRequestHead)
data class GithubEvent(val pull_request: GithubPullRequest, val repository: GithubRepository)