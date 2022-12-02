package org.jetbrains.research.deps.miner.models

import kotlinx.serialization.Serializable

@Serializable
data class RepositoryInfo(
    val owner: String,
    val name: String,
    val stars: Int,
    val dependencies: MutableSet<DependencyInfo> = HashSet()
)