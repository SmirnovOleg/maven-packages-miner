package org.jetbrains.research.deps.miner.maven

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.xml.XmlTag
import org.jetbrains.research.deps.miner.gradle.KotlinGradleDependenciesCollector
import org.jetbrains.research.deps.miner.models.DependencyInfo

class MavenDependenciesCollector : PsiRecursiveElementVisitor() {
    private val log = Logger.getInstance(KotlinGradleDependenciesCollector::class.java)

    val dependencyInfos: MutableSet<DependencyInfo> = HashSet()

    override fun visitElement(element: PsiElement) {
        super.visitElement(element)

        if (element is XmlTag && element.name == "dependency") {
            val groupId = element.subTags.find { it.name == "groupId" }?.value?.text ?: return
            val artifactId = element.subTags.find { it.name == "artifactId" }?.value?.text ?: return
            dependencyInfos.add(DependencyInfo(groupId, artifactId))
        }
    }
}
