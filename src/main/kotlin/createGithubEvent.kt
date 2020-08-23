import com.squareup.moshi.Moshi
import java.io.File

fun createGithubEvent(
    eventFilePath: String,
    moshi: Moshi
): GithubEvent {

    val json = File(eventFilePath).readText()
    return moshi
        .adapter(GithubEvent::class.java)
        .fromJson(json)
        ?: throw Exception("Could not create json from file: $eventFilePath")
}

class GithubUser(val login: String)
class GithubRepository(val name: String)
class GithubPullRequestHead(val sha: String)
class GithubPullRequest(val number: Int, val user: GithubUser, val head: GithubPullRequestHead)
data class GithubEvent(val pull_request: GithubPullRequest, val repository: GithubRepository)