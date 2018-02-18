package com.tcibinan.flaxo.rest.service.travis

import io.vavr.control.Either
import okhttp3.ResponseBody

class Travis(private val travisClient: TravisClient,
             private val travisToken: String) {

    fun getUser(): Either<ResponseBody, TravisUser> =
            travisClient.getUser(authorization()).execute()
                    .run {
                        if (isSuccessful) Either.right(body())
                        else Either.left(errorBody())
                    }

    fun activate(userName: String, repositoryName: String): Either<ResponseBody, TravisRepository> =
            travisClient.activate(authorization(), repositorySlug(userName, repositoryName)).execute()
                    .run {
                        if (isSuccessful) Either.right(body())
                        else Either.left(errorBody())
                    }

    fun deactivate(userName: String, repositoryName: String): Either<ResponseBody, TravisRepository> =
            travisClient.deactivate(authorization(), repositorySlug(userName, repositoryName)).execute()
                    .run {
                        if (isSuccessful) Either.right(body())
                        else Either.left(errorBody())
                    }

    private fun authorization() = "token $travisToken"

    private fun repositorySlug(userName: String, repositoryName: String) =
            "$userName/$repositoryName"

}