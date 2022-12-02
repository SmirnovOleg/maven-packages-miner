package org.jetbrains.research.deps.miner.models

import kotlinx.serialization.Serializable

@Serializable
data class DependencyInfo(
    val groupId: String,
    val artifactId: String
)