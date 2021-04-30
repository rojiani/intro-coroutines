package tasks

import contributors.*
import kotlinx.coroutines.*

suspend fun loadContributorsConcurrent(service: GitHubService, req: RequestData): List<User> =
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .bodyList()

        val deferredContributors: List<Deferred<List<User>>> = repos.map { repo ->
            async(Dispatchers.Default) {
                log("starting loading for ${repo.name}")
                delay(3000)
                service.getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                    .bodyList()
            }
        }
        deferredContributors.awaitAll() // List<List<User>>
            .flatten()
            .aggregate()
    }
