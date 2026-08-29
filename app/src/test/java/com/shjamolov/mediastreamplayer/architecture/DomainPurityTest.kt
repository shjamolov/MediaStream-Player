package com.shjamolov.mediastreamplayer.architecture

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readLines
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainPurityTest {
    @Test
    fun domain_hasNoFrameworkImports() {
        val domainRoot = locateDomainRoot()
        val forbiddenImports = listOf(
            "android.",
            "androidx.",
            "org.koin.",
            "retrofit2.",
            "okhttp3.",
            "androidx.room.",
            "androidx.media3.",
            "androidx.compose.",
        )

        val violations = buildList {
            Files.walk(domainRoot).use { paths ->
                paths
                    .filter { it.extension == "kt" }
                    .forEach { file ->
                        file.readLines()
                            .filter { line ->
                                val trimmedLine = line.trim()
                                val importedType = trimmedLine.removePrefix("import ")
                                trimmedLine.startsWith("import ") &&
                                    forbiddenImports.any(importedType::startsWith)
                            }
                            .forEach { line ->
                                add("${domainRoot.relativize(file)}: ${line.trim()}")
                            }
                    }
            }
        }

        assertTrue(
            "Domain layer contains forbidden framework imports:\n${violations.joinToString("\n")}",
            violations.isEmpty(),
        )
    }

    private fun locateDomainRoot(): Path {
        val moduleRelative = Path.of(
            "src/main/java/com/shjamolov/mediastreamplayer/domain",
        )
        if (Files.isDirectory(moduleRelative)) return moduleRelative

        val rootRelative = Path.of(
            "app/src/main/java/com/shjamolov/mediastreamplayer/domain",
        )
        require(Files.isDirectory(rootRelative)) { "Domain source directory is missing" }
        return rootRelative
    }
}
