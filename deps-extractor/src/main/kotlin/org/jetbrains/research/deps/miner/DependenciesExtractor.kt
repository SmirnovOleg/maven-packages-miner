package org.jetbrains.research.deps.miner

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.parsers.TomlParser
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.intellij.ide.impl.ProjectUtil
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.plugins.groovy.GroovyLanguage
import org.jetbrains.research.deps.miner.gradle.GroovyGradleDependenciesCollector
import org.jetbrains.research.deps.miner.gradle.KotlinGradleDependenciesCollector
import org.jetbrains.research.deps.miner.gradle.TomlParseError
import org.jetbrains.research.deps.miner.gradle.extractDependenciesFromTOML
import org.jetbrains.research.deps.miner.maven.MavenDependenciesCollector
import org.jetbrains.research.deps.miner.models.RepositoryInfo
import java.io.File
import java.nio.file.Path
import java.util.*

typealias RepoID = String

class DependenciesExtractor {
    private val log = Logger.getInstance(DependenciesExtractor::class.java)
    private val repos = HashMap<RepoID, RepositoryInfo>()

    fun run() {
        val project = ProjectUtil.openOrImport(Path.of("."))
        val factory = PsiFileFactory.getInstance(project)

        csvReader().open(File(Config.PATH_TO_DATASET)) {
            readAllWithHeaderAsSequence().forEachIndexed { idx: Int, row: Map<String, String> ->
                try {
                    if (idx % 100 == 0 && idx != 0) {
                        println("Processing row #${idx}...")
                    }

                    val repoId = row["repo_id"]!!
                    val owner = row["repo_owner"]!!
                    val name = row["repo_name"]!!
                    val stars = row["stars"]!!.toInt()
                    val code = row["content"]!!
                    val filePath = row["file_link"]!!

                    when {
                        filePath.endsWith("build.gradle") -> {
                            log.info("Found build.gradle (Groovy) file: $filePath")

                            val file = factory.createFileFromText(UUID.randomUUID().toString(), GroovyLanguage, code)
                            val visitor = GroovyGradleDependenciesCollector()
                            file.accept(visitor)

                            repos.getOrPut(repoId) { RepositoryInfo(owner, name, stars) }
                                .dependencies.addAll(visitor.dependencyInfos)
                        }

                        filePath.endsWith("build.gradle.kts") -> {
                            log.info("Found build.gradle (Kotlin) file: $filePath")

                            val filename = UUID.randomUUID().toString()
                            val file = factory.createFileFromText(filename, KotlinLanguage.INSTANCE, code)
                            val visitor = KotlinGradleDependenciesCollector()
                            file.accept(visitor)

                            repos.getOrPut(repoId) { RepositoryInfo(owner, name, stars) }
                                .dependencies.addAll(visitor.dependencyInfos)
                        }

                        filePath.contains("pom.xml") -> {
                            log.info("Found pom.xml (Maven) file: $filePath")

                            val filename = UUID.randomUUID().toString()
                            val file = factory.createFileFromText(filename, XMLLanguage.INSTANCE, code)
                            val visitor = MavenDependenciesCollector()
                            file.accept(visitor)

                            repos.getOrPut(repoId) { RepositoryInfo(owner, name, stars) }
                                .dependencies.addAll(visitor.dependencyInfos)
                        }

                        filePath.endsWith("toml") -> {
                            log.info("Found TOML file: $filePath")

                            try {
                                val tomlDocument = TomlParser(TomlInputConfig()).parseString(code)
                                val dependencyInfos = extractDependenciesFromTOML(tomlDocument)

                                repos.getOrPut(repoId) { RepositoryInfo(owner, name, stars) }
                                    .dependencies.addAll(dependencyInfos)
                            } catch (e: TomlParseError) {
                                log.info(e.message)
                            }
                        }

                        else -> log.info("Unknown file extension: $filePath")
                    }
                } catch (e: Throwable) {
                    log.warn("FAILED TO PROCESS ROW: $idx, ${e.message}")
                }
            }
        }

        println("Saving...")
        csvWriter().writeAll(
            rows = repos.map {
                listOf(
                    it.key,
                    it.value.owner,
                    it.value.name,
                    it.value.stars,
                    it.value.dependencies.joinToString(";") { dep -> "${dep.groupId}:${dep.artifactId}" }
                )
            },
            targetFile = File(Config.PATH_TO_OUTPUT)
        )

        println("Finished!")
    }
}


open class DependencyExtractorParseError(val elementText: String) : Exception(elementText)
