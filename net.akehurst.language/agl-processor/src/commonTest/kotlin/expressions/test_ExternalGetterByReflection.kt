package net.akehurst.language.expressions.processor

import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.api.PropertyValueName
import net.akehurst.language.asm.simple.AsmStructureSimple
import net.akehurst.language.asm.simple.raw
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class test_ExternalGetterByReflection {

    private companion object {
        // 'Thing' has no implementation class, so it cannot be constructed by reflection
        val types = typesDomain("Test", true) {
            namespace("test.nonexistent") {
                data("Thing") {
                    propertyOf(setOf(REF, VAL), "name", "String")
                }
            }
        }
    }

    @Test
    fun createStructure_falls_back_to_AsmStructure() {
        val sut = ExternalGetterByReflection(types, IssueHolder(LanguageProcessorPhase.INTERPRET))

        val actual = sut.createStructure("test.nonexistent.Thing".asQualifiedName, mapOf("name" to "fred"))

        assertIs<AsmStructure>(actual)
        assertEquals("test.nonexistent.Thing", actual.qualifiedTypeName.value)
        assertEquals("fred", sut.getProperty(actual, "name"))
    }

    @Test
    fun typeFor_AsmStructure() {
        val sut = ExternalGetterByReflection(types, IssueHolder(LanguageProcessorPhase.INTERPRET))
        val obj = AsmStructureSimple("test.nonexistent.Thing".asQualifiedName)

        val actual = sut.typeFor(obj, StdLibDefault.AnyType)

        assertEquals("test.nonexistent.Thing", actual.qualifiedTypeName.value)
    }

    @Test
    fun setProperty_on_empty_AsmStructure() {
        val sut = ExternalGetterByReflection(types, IssueHolder(LanguageProcessorPhase.INTERPRET))
        val obj = AsmStructureSimple("test.nonexistent.Thing".asQualifiedName)

        sut.setProperty(obj, "name", false, "fred")

        assertEquals("fred", obj.getPropertyOrNull(PropertyValueName("name"))?.raw)
    }

}
