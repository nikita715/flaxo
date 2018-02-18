package com.tcibinan.flaxo.git

interface GitInstance : Git {
    fun createRepository(repositoryName: String, private: Boolean = false): RepositoryInstance
    fun createBranch(repository: Repository, branchName: String): BranchInstance
    fun load(repository: Repository, branch: Branch, path: String, content: String)
    fun load(repository: Repository, branch: Branch, path: String, bytes: ByteArray)
    fun createSubBranch(repository: Repository, branch: Branch, subBranchName: String)
    fun deleteRepository(repositoryName: String)
    fun addWebHook(repositoryName: String)
}