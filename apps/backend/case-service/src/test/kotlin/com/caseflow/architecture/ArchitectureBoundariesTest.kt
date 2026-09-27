package com.caseflow.architecture

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class ArchitectureBoundariesTest {
    @Test
    fun `domain and persistence ports do not import framework or outward layers`() {
        val sourceRoot = Path.of(System.getProperty("user.dir"), "src/main/kotlin/com/caseflow")
        val forbiddenImports = listOf(
            "import jakarta.persistence",
            "import org.hibernate",
            "import org.springframework",
            "import com.caseflow.infrastructure",
            "import com.caseflow.interfaces"
        )
        val boundaryRoots = listOf(sourceRoot.resolve("domain"), sourceRoot.resolve("application/port"))

        boundaryRoots.forEach { root ->
            Files.walk(root).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .forEach { source ->
                        val forbidden = Files.readAllLines(source).firstOrNull { line ->
                            val import = line.trim()
                            forbiddenImports.any(import::startsWith)
                        }
                        assertFalse(forbidden != null, "$source imports an outward/framework dependency: $forbidden")
                    }
            }
        }
    }
}
