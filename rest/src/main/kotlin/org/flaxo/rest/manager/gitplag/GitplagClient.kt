package org.flaxo.rest.manager.gitplag

import io.gitplag.gitplagapi.model.input.AnalysisRequest
import io.gitplag.gitplagapi.model.input.RepositoryInput
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Gitplag retrofit client.
 */
interface GitplagClient {

    /**
     * Analyzes a task of a course.
     */
    @POST("/api/repositories/{vcsService}/{username}/{projectName}/analyze/detached")
    fun analyse(@Path("vcsService")
                vcsService: String,
                @Path("username")
                username: String,
                @Path("projectName")
                projectName: String,
                @Body
                analysisRequest: AnalysisRequest
    ): Call<ResponseBody>

    /**
     * Adds a repository to Gitplag.
     */
    @POST("/api/repositories")
    fun addRepository(@Body repository: RepositoryInput): Call<ResponseBody>

    /**
     * Updates files of a repository in Gitplag.
     */
    @GET("/api/repositories/{vcsService}/{username}/{projectName}/files/update/detached")
    fun updateRepository(@Path("vcsService")
                         vcsService: String,
                         @Path("username")
                         username: String,
                         @Path("projectName")
                         projectName: String): Call<ResponseBody>

}

