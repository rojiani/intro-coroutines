package tasks

import contributors.*
import kotlinx.coroutines.*

// https://play.kotlinlang.org/hands-on/Introduction%20to%20Coroutines%20and%20Channels/06_StructuredConcurrency
suspend fun loadContributorsNotCancellable(service: GitHubService, req: RequestData): List<User> {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    val deferredContributors: List<Deferred<List<User>>> = repos.map { repo ->
        GlobalScope.async(Dispatchers.Default) {
            log("starting loading for ${repo.name}")
            delay(3000)
            service.getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }
    val result = deferredContributors.awaitAll() // List<List<User>>
        .flatten()
        .aggregate()

    return result
}
