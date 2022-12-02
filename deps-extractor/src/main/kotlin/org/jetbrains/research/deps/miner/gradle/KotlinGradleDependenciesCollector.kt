package org.jetbrains.research.deps.miner.gradle

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.idea.debugger.sequence.psi.callName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.research.deps.miner.DependencyExtractorParseError
import org.jetbrains.research.deps.miner.models.DependencyInfo

class KotlinGradleDependenciesCollector : PsiRecursiveElementVisitor() {
    private val log = Logger.getInstance(KotlinGradleDependenciesCollector::class.java)

    val dependencyInfos: MutableSet<DependencyInfo> = HashSet()

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (element is KtCallExpression &&
            GradleDependencyDeclarations.availableKeys().any { it == element.callName() }
        ) {
            val args = element.valueArguments
            when (args.size) {
                1 -> {
                    try {
                        val valueArg = args.first().children.first()
                        when {
                            valueArg is KtStringTemplateExpression -> {
                                val literal = valueArg.entries.firstOrNull()?.text
                                    ?: throw SingleArgumentParseError(element.text)
                                val parts = literal.split(":")
                                val groupId = parts.getOrNull(0)
                                    ?: throw ThreeUnnamedArgumentsParseError(element.text)
                                val artifactId = parts.getOrNull(1)
                                    ?: throw ThreeUnnamedArgumentsParseError(element.text)
                                dependencyInfos.add(DependencyInfo(groupId, artifactId))
                            }

                            valueArg is KtCallExpression && valueArg.callName() == "kotlin" -> {
                                val kotlinFuncValueArgs = valueArg.valueArguments
                                val kotlinLibName = (kotlinFuncValueArgs.firstOrNull()
                                    ?.children?.firstOrNull() as? KtStringTemplateExpression)
                                    ?.entries?.firstOrNull()?.text
                                    ?: throw KotlinArgumentParseError(element.text)
                                dependencyInfos.add(DependencyInfo("org.jetbrains.kotlin", "kotlin-$kotlinLibName"))
                            }

                            else -> {
                                throw SingleArgumentParseError(element.text)
                            }
                        }
                    } catch (e: DependencyExtractorParseError) {
                        log.info("Failed to parse single argument of ${e.elementText}")
                    }
                }

                3 -> {
                    val namedArgs = element.valueArguments
                    try {
                        val groupId = (namedArgs.getOrNull(0)
                            ?.getArgumentExpression() as? KtStringTemplateExpression)
                            ?.entries?.firstOrNull()?.text
                            ?: throw KotlinNamedArgumentsParseError(element.text)
                        val artifactId = (namedArgs.getOrNull(1)
                            ?.getArgumentExpression() as? KtStringTemplateExpression)
                            ?.entries?.firstOrNull()?.text
                            ?: throw KotlinNamedArgumentsParseError(element.text)
                        dependencyInfos.add(DependencyInfo(groupId, artifactId))
                    } catch (e: DependencyExtractorParseError) {
                        log.info("Failed to parse single argument of ${e.elementText}")
                    }
                }
            }
        }
    }
}

class KotlinArgumentParseError(reason: String) : DependencyExtractorParseError(reason)
class KotlinNamedArgumentsParseError(reason: String) : DependencyExtractorParseError(reason)