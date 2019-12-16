package org.flaxo.rest

import org.flaxo.rest.manager.gitplag.GitplagClient
import org.flaxo.rest.manager.moss.GitplagMossManager
import org.flaxo.rest.manager.moss.MossManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Gitplag configuration.
 */
@Configuration
@ConditionalOnProperty(name = ["flaxo.plagiarism.analyser"], havingValue = "gitplag")
class GitplagConfiguration {

    /**
     * [GitplagMossManager] bean
     */
    @Bean
    fun gitplagPlagiarismAnalyser(
            gitplagClient: GitplagClient,
            @Value("\${REST_URL}") restUrl: String
    ): MossManager = GitplagMossManager(gitplagClient, restUrl)
}
