package tasks

import contributors.GitHubService
import contributors.RequestData
import contributors.User
import contributors.log
import contributors.logRepos
import contributors.logUsers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .bodyList()

        // Each asynchronous "getRepoContributors()" coroutine will be a Producer
        // that sends its fetched contributors through this channel.
        val contributorsChannel = Channel<List<User>>()

        for (repo in repos) {
            launch {
                log("starting loading for ${repo.name}")
                val contributors: List<User> =
                    service.getRepoContributors(req.org, repo.name)
                        .also { logUsers(repo, it) }
                        .bodyList()
                contributorsChannel.send(contributors)
            }
        }

        // Consumer that collects the results and calls updateResults
        launch {
            var allUsers = emptyList<User>()
            repeat(repos.size) {
                val contributorsForRepo = contributorsChannel.receive()
                allUsers = (allUsers + contributorsForRepo).aggregate()
                updateResults(allUsers, it == repos.lastIndex)
            }
        }
    }
}
