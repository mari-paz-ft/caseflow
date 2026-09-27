package com.caseflow.infrastructure.storage

import com.caseflow.application.port.DocumentStoragePort
import com.caseflow.domain.exception.TechnicalFailureException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

@Component
class LocalFileSystemDocumentStorage(
    @Value("\${caseflow.upload-dir:./data/documents}") uploadDirectory: String
) : DocumentStoragePort {
    private val root = Paths.get(uploadDirectory).toAbsolutePath().normalize()

    init {
        try {
            Files.createDirectories(root)
        } catch (exception: IOException) {
            throw TechnicalFailureException("Armazenamento documental indisponível", exception)
        }
    }

    override fun store(storageKey: String, content: ByteArray) {
        val path = resolve(storageKey)
        try {
            Files.write(path, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        } catch (exception: IOException) {
            throw TechnicalFailureException("Não foi possível armazenar o documento", exception)
        }
    }

    override fun read(storageKey: String): ByteArray {
        val path = resolve(storageKey)
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw TechnicalFailureException("Documento indisponível no armazenamento")
        }
        return try {
            Files.readAllBytes(path)
        } catch (exception: IOException) {
            throw TechnicalFailureException("Documento indisponível no armazenamento", exception)
        }
    }

    override fun delete(storageKey: String) {
        val path = resolve(storageKey)
        try {
            Files.deleteIfExists(path)
        } catch (exception: IOException) {
            throw TechnicalFailureException("Não foi possível remover o documento do armazenamento", exception)
        }
    }

    private fun resolve(storageKey: String): Path {
        val key = runCatching { Paths.get(storageKey) }
            .getOrElse { throw TechnicalFailureException("Chave de armazenamento inválida", it) }
        if (storageKey.isBlank() || key.isAbsolute || key.nameCount != 1) {
            throw TechnicalFailureException("Chave de armazenamento inválida")
        }
        val path = root.resolve(key).normalize()
        if (!path.startsWith(root) || path == root) {
            throw TechnicalFailureException("Chave de armazenamento inválida")
        }
        return path
    }
}
