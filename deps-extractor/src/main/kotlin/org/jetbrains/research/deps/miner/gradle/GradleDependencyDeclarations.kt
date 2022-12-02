package org.jetbrains.research.deps.miner.gradle

enum class GradleDependencyDeclarations(
    val key: String
) {
    KAPT("kapt"),
    API("api"),
    CLASSPATH("classpath"),
    COMPILE_ONLY("compileOnly"),
    RUNTIME_ONLY("runtimeOnly"),
    COMPILE("compile"),
    IMPLEMENTATION("implementation"),
    ANNOTATION_PROCESSOR("annotationProcessor"),
    TEST_IMPLEMENTATION("testImplementation"),
    TEST_RUNTIME_ONLY("testRuntimeOnly"),
    TEST_RUNTIME("testRuntime"),
    TEST_COMPILE("testCompile"),
    ANDROID_TEST_IMPLEMENTATION("androidTestImplementation");

    companion object {
        fun fromKey(yamlKey: String) = values().firstOrNull { it.key.equals(yamlKey, ignoreCase = true) }
        fun availableKeys() = values().map { it.key }
    }
}