package org.jetbrains.research.deps.miner.gradle

import com.akuleshov7.ktoml.tree.TomlBasicString
import com.akuleshov7.ktoml.tree.TomlFile
import com.akuleshov7.ktoml.tree.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.TomlTablePrimitive
import org.jetbrains.research.deps.miner.DependencyExtractorParseError
import org.jetbrains.research.deps.miner.models.DependencyInfo

fun extractDependenciesFromTOML(tomlDocument: TomlFile): List<DependencyInfo> {
    val libs = tomlDocument.children.find { it.name == "libraries" }
        ?: throw TomlParseError("TOML libs sections not found")

    val dependencyInfos =
        libs.children.filterIsInstance<TomlTablePrimitive>().mapNotNull { tomlDep ->
            val parts =
                (((tomlDep.children.find { it.name == "module" } as? TomlKeyValuePrimitive)
                    ?.value as? TomlBasicString)
                    ?.content as? String)
                    ?.split(":")
            val groupId = parts?.getOrNull(0) ?: return@mapNotNull null
            val artifactId = parts.getOrNull(1) ?: return@mapNotNull null
            DependencyInfo(groupId, artifactId)
        } + libs.children.filterIsInstance<TomlKeyValuePrimitive>().mapNotNull { tomlDep ->
            val parts = (tomlDep.value.content as? String)?.split(":")
            val groupId = parts?.getOrNull(0) ?: return@mapNotNull null
            val artifactId = parts.getOrNull(1) ?: return@mapNotNull null
            DependencyInfo(groupId, artifactId)
        }

    return dependencyInfos
}

class TomlParseError(reason: String) : DependencyExtractorParseError(reason)