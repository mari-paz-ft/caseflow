package com.caseflow.application.usecase

import com.caseflow.application.dto.CreateCaseDto
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CaseRulesTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `aceita somente tipo de caso documental`() {
        val valid = CreateCaseDto(
            title = "Caso documental válido",
            description = "Descrição com tamanho suficiente para validação de regra.",
        )
        val invalidType = valid.copy(type = "OUTRO_TIPO")

        assertTrue(validator.validate(valid).isEmpty())
        assertEquals(setOf("type"), validator.validate(invalidType).map { it.propertyPath.toString() }.toSet())
    }

    @Test
    fun `exige limites de titulo e descricao`() {
        val invalid = CreateCaseDto(title = "curt", description = "breve")

        assertEquals(
            setOf("title", "description"),
            validator.validate(invalid).map { it.propertyPath.toString() }.toSet()
        )
    }
}
