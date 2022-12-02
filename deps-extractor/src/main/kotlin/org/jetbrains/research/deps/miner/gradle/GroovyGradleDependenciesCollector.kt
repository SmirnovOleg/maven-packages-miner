package org.jetbrains.research.deps.miner.gradle

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl
import org.jetbrains.plugins.groovy.lang.psi.impl.stringValue
import org.jetbrains.research.deps.miner.DependencyExtractorParseError
import org.jetbrains.research.deps.miner.models.DependencyInfo

class GroovyGradleDependenciesCollector : PsiRecursiveElementVisitor() {
    private val log = Logger.getInstance(GroovyGradleDependenciesCollector::class.java)

    val dependencyInfos: MutableSet<DependencyInfo> = HashSet()

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (element is GrApplicationStatement &&
            GradleDependencyDeclarations.availableKeys().any { it == element.callReference?.methodName }
        ) {
            val args = element.argumentList.allArguments
            when (args.size) {
                1 -> {
                    try {
                        when {
                            args.first() is GrString -> {
                                val literal = (args.first() as GrString).textParts.firstOrNull()
                                    ?: throw SingleArgumentParseError(element.text)
                                val parts = literal.split(":")
                                val groupId = parts.getOrNull(0)
                                    ?: throw ThreeUnnamedArgumentsParseError(element.text)
                                val artifactId = parts.getOrNull(1)
                                    ?: throw ThreeUnnamedArgumentsParseError(element.text)
                                dependencyInfos.add(DependencyInfo(groupId, artifactId))
                            }

                            args.first() is GrLiteralImpl -> {
                                val literal = (args.first() as GrLiteral).value as? String
                                    ?: throw SingleArgumentParseError(element.text)
                                val parts = literal.split(":")
                                val groupId = parts.getOrNull(0)
                                    ?: throw ThreeUnnamedArgumentsParseError(element.text)
                                val artifactId = parts.getOrNull(1)
                                    ?: throw ThreeUnnamedArgumentsParseError(element.text)
                                dependencyInfos.add(DependencyInfo(groupId, artifactId))
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
                    val namedArgs = element.argumentList.namedArguments
                    try {
                        val dependencyInfo = if (namedArgs.size == 3) {
                            val groupId = (namedArgs[0].expression as? GrLiteralImpl)?.value as? String
                                ?: throw ThreeNamedArgumentsParseError(element.text)
                            val artifactId = (namedArgs[1].expression as? GrLiteralImpl)?.value as? String
                                ?: throw ThreeNamedArgumentsParseError(element.text)
                            DependencyInfo(groupId, artifactId)
                        } else {
                            val unnamedArgs = element.argumentList.expressionArguments
                            val groupId = unnamedArgs.getOrNull(0)?.stringValue()
                                ?: throw ThreeUnnamedArgumentsParseError(element.text)
                            val artifactId = unnamedArgs.getOrNull(1)?.stringValue()
                                ?: throw ThreeUnnamedArgumentsParseError(element.text)
                            DependencyInfo(groupId, artifactId)
                        }
                        dependencyInfos.add(dependencyInfo)
                    } catch (e: DependencyExtractorParseError) {
                        log.info("Failed to parse 3 named arguments of ${e.elementText}")
                    }
                }
            }
        }
    }
}

class SingleArgumentParseError(reason: String) : DependencyExtractorParseError(reason)
class ThreeNamedArgumentsParseError(reason: String) : DependencyExtractorParseError(reason)
class ThreeUnnamedArgumentsParseError(reason: String) : DependencyExtractorParseError(reason)
