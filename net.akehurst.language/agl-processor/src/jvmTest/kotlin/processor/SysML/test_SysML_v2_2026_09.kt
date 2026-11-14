/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.agl.processor.SysML

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.KerML.KerML_v2_2026_09
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.StyleString
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.regex.api.RegexEngineKind
import net.akehurst.language.scanner.api.ScannerKind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the SysML v2 2026-09 grammars with the example models of the SysML v2 Release (in 'valid')
 * and with some invalid sentences (in 'invalid').
 */
abstract class test_SysML_v2_2026_09(
    grammarName: String,
    /** relative paths (from 'valid') of example files that are known not to parse */
    private val knownFailing: Set<String>,
    /** null: the scanner given by the grammar option '#scanner' (Classic) */
    private val scannerKind: ScannerKind? = null
) {

    private val languageDir = File("../language-repository/languages/SysML/v2_2026-09")
    private val grammarDir = File(languageDir, "grammars/$grammarName")
    private val grammarStr by lazy { File(grammarDir, "grammar.agl").readText() }
    private val styleStr by lazy { File(grammarDir, "style.agl").readText() }

    private val processor: LanguageProcessor<Asm, SentenceContext> by lazy {
        val res = Agl.processorFromStringSimple(
            GrammarString(grammarStr),
            styleStr = StyleString(styleStr),
            configurationBase = when (scannerKind) {
                null -> Agl.configurationSimple()
                else -> Agl.configuration(Agl.configurationSimple()) { scanner(scannerKind, RegexEngineKind.PLATFORM) }
            }
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        res.processor!!
    }

    private fun sentenceFiles(kind: String) = File(languageDir, kind).walkTopDown()
        .filter { it.isFile && it.name.endsWith(".sysml") }
        .sortedBy { it.path }
        .toList()

    private fun parses(text: String): Boolean = processor.parse(text).let { it.issues.errors.isEmpty() && null != it.sppt }
    private fun parses(file: File): Boolean = parses(file.readText())

    /** sentences that fail only when keywords are reserved, i.e. with the Classic scanner */
    protected fun assertKeywordsReserved() {
        val keywordAsName = listOf(
            "package P {\n    import ;\n}\n",
            "package P {\n    part null;\n}\n",
            "package P {\n    part def part;\n}\n",
        )
        assertEquals(emptyList(), keywordAsName.filter { parses(it) }, "keywords used as names that parse")
        // an unrestricted name may be a keyword
        assertTrue(parses("package P {\n    part 'part';\n    part def 'import';\n}\n"))
    }

    @Test
    fun parse_grammar() {
        val res = Agl.registry.agl.grammar.processor!!.parse(grammarStr)
        assertTrue(res.issues.isEmpty(), res.issues.toString())
    }

    @Test
    fun process_grammar() {
        val res = Agl.registry.agl.grammar.processor!!.process(grammarStr, Agl.options { semanticAnalysis { sentenceContext(contextFromGrammarRegistry(Agl.registry)) } })
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
    }

    @Test
    fun processor_with_style() {
        assertTrue(processor.typesDomain.allDefinitions.isNotEmpty())
        assertEquals(scannerKind ?: ScannerKind.Classic, processor.scanner!!.kind)
    }

    @Test
    fun parse_valid() {
        val files = sentenceFiles("valid")
        assertTrue(files.size > 200, "example files not found in '${languageDir.absolutePath}'")
        val failed = files.filterNot { parses(it) }.map { it.relativeTo(File(languageDir, "valid")).invariantSeparatorsPath }.toSet()
        val unexpectedFailures = failed - knownFailing
        val unexpectedPasses = knownFailing - failed
        assertTrue(unexpectedFailures.isEmpty(), "${unexpectedFailures.size} examples do not parse:\n${unexpectedFailures.joinToString("\n")}")
        assertTrue(unexpectedPasses.isEmpty(), "examples now parse, remove them from 'knownFailing':\n${unexpectedPasses.joinToString("\n")}")
    }

    @Test
    fun parse_invalid() {
        val files = sentenceFiles("invalid")
        assertTrue(files.isNotEmpty())
        val parsed = files.filter { parses(it) }.map { it.name }
        assertEquals(emptyList(), parsed, "invalid sentences that parse")
    }

    @Test
    fun process_valid_single() {
        val sentence = """
            package 'Part Definition Example' {
                private import ScalarValues::*;
                part def Vehicle {
                    attribute mass : Real;
                    part eng : Engine;
                    ref part driver : Person;
                }
                part def Engine;
                part def Person;
            }
        """.trimIndent()
        val res = processor.process(sentence)
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        assertTrue(null != res.asm)
    }
}

// the grammars set '#scanner: Classic', because SysML reserves its keywords (KerML 8.2.2.6):
// with the Classic scanner a literal beats a name of the same length
class test_SysML_v2_2026_09_standard : test_SysML_v2_2026_09("standard", emptySet()) {
    @Test
    fun keywords_reserved() = assertKeywordsReserved()
}

class test_SysML_v2_2026_09_agl : test_SysML_v2_2026_09("agl", emptySet()) {
    @Test
    fun keywords_reserved() = assertKeywordsReserved()
}

// the grammars also work with the OnDemand scanner, but then keywords can be used as names
class test_SysML_v2_2026_09_standard_OnDemand : test_SysML_v2_2026_09("standard", emptySet(), ScannerKind.OnDemand)

class test_SysML_v2_2026_09_agl_OnDemand : test_SysML_v2_2026_09("agl", emptySet(), ScannerKind.OnDemand)

/**
 * Tests the references of the 'agl' grammar.
 *
 * SysML is built on KerML: the SysML processor uses the types domain of the KerML processor (KerML/v2_2026-09, grammar 'agl') as its base types,
 * and its references are merged with the KerML references, so that the KerML model libraries (.kerml), processed by the KerML processor,
 * are found from SysML.
 *
 * TODO(David): (q29b) an agl feature for a language definition to import the types and references of another language, instead of this configuration code.
 */
class test_SysML_v2_2026_09_agl_references {

    private companion object {
        val languageDir = File("../language-repository/languages/SysML/v2_2026-09")
        val grammarDir = File(languageDir, "grammars/agl")

        /** relative paths (from 'valid') of example files that are known to have reference errors (mostly implicit specializations, not supported yet) */
        val knownFailing = setOf(
            "examples/Analysis Examples/AnalysisAnnotation.sysml",
            "examples/Analysis Examples/Turbojet Stage Analysis.sysml",
            "examples/Analysis Examples/Vehicle Analysis Demo.sysml",
            "examples/Arrowhead Framework Example/AHFCoreLib.sysml",
            "examples/Arrowhead Framework Example/AHFNorwayTopics.sysml",
            "examples/Arrowhead Framework Example/AHFSequences.sysml",
            "examples/Cause and Effect Examples/CauseAndEffectExample.sysml",
            "examples/Comment Examples/Comments.sysml",
            "examples/Flashlight Example/Flashlight Example.sysml",
            "examples/Geometry Examples/CarWithEnvelopingShape.sysml",
            "examples/Geometry Examples/CarWithShapeAndCSG.sysml",
            "examples/Geometry Examples/ExternalShapeRefExample.sysml",
            "examples/Geometry Examples/SimpleQuadcopter.sysml",
            "examples/Geometry Examples/VehicleGeometryAndCoordinateFrames.sysml",
            "examples/Import Tests/AliasImport.sysml",
            "examples/Import Tests/PrivateImportTest.sysml",
            "examples/Individuals Examples/AnalysisIndividualExample.sysml",
            "examples/Interaction Sequencing Examples/ServerSequenceModelOutside.sysml",
            "examples/Interaction Sequencing Examples/ServerSequenceOutsideRealization-2.sysml",
            "examples/Interaction Sequencing Examples/ServerSequenceOutsideRealization-3.sysml",
            "examples/Interaction Sequencing Examples/ServerSequenceRealization-2.sysml",
            "examples/Interaction Sequencing Examples/ServerSequenceRealization-3.sysml",
            "examples/Mass Roll-up Example/Vehicles.sysml",
            "examples/Metadata Examples/IssueMetadataExample.sysml",
            "examples/Metadata Examples/RationaleMetadataExample.sysml",
            "examples/Metadata Examples/RequirementMetadataExample.sysml",
            "examples/Metadata Examples/RiskMetadataExample.sysml",
            "examples/Metadata Examples/VerificationMetadataExample.sysml",
            "examples/Requirements Examples/RequirementDerivationExample.sysml",
            "examples/Room Model/RoomModel.sysml",
            "examples/Simple Tests/ActionTest.sysml",
            "examples/Simple Tests/AliasTest.sysml",
            "examples/Simple Tests/AllocationTest.sysml",
            "examples/Simple Tests/AssignmentTest.sysml",
            "examples/Simple Tests/CalculationTest.sysml",
            "examples/Simple Tests/CommentTest.sysml",
            "examples/Simple Tests/ConstraintTest.sysml",
            "examples/Simple Tests/FeaturePathTest.sysml",
            "examples/Simple Tests/ImportTest.sysml",
            "examples/Simple Tests/InterfaceTest.sysml",
            "examples/Simple Tests/MetadataTest.sysml",
            "examples/Simple Tests/ParameterTest.sysml",
            "examples/Simple Tests/PartTest.sysml",
            "examples/Simple Tests/RootPackageTest.sysml",
            "examples/Simple Tests/StateTest.sysml",
            "examples/Simple Tests/VariabilityTest.sysml",
            "examples/Simple Tests/ViewTest.sysml",
            "examples/State Space Representation Examples/EVSample.sysml",
            "examples/Timeslice and Snapshot Examples/TimeVaryingAttribute.sysml",
            "examples/Variability Examples/VehicleVariabilityModel.sysml",
            "examples/Vehicle Example/SysML v2 Spec Annex A SimpleVehicleModel.sysml",
            "examples/Vehicle Example/VehicleIndividuals.sysml",
            "examples/Vehicle Example/VehicleUsages.sysml",
            "examples/v1 Spec Examples/8.4.5 Constraining Decomposition/Vehicle Decomposition - Updated.sysml",
            "examples/v1 Spec Examples/8.4.5 Constraining Decomposition/Vehicle Decomposition.sysml",
            "examples/v1 Spec Examples/D.4.7.8 Dynamics/HSUVDynamics.sysml",
            "training/07. Parts/Parts Example-1.sysml",
            "training/07. Parts/Parts Example-2.sysml",
            "training/11. Interfaces/Interface Example.sysml",
            "training/12. Binding Connectors/Binding Connectors Example-1.sysml",
            "training/12. Binding Connectors/Binding Connectors Example-2.sysml",
            "training/13. Flows/Flow Definition Example.sysml",
            "training/13. Flows/Flow Interface Example.sysml",
            "training/13. Flows/Flow Usage Example.sysml",
            "training/16. Conditional Succession/Conditional Succession Example-1.sysml",
            "training/16. Conditional Succession/Conditional Succession Example-2.sysml",
            "training/17. Control/Camera.sysml",
            "training/17. Control/Control Structures Example.sysml",
            "training/18. Action Performance/Action Performance Example.sysml",
            "training/21. Asynchronous Messaging/Messaging Example.sysml",
            "training/21. Asynchronous Messaging/Messaging with Ports.sysml",
            "training/25. Transitions/Change and Time Triggers.sysml",
            "training/26. State Exhibition/State Exhibition Example.sysml",
            "training/27. Occurrences/Interaction Example-1.sysml",
            "training/27. Occurrences/Interaction Realization-1.sysml",
            "training/27. Occurrences/Interaction Realization-2.sysml",
            "training/27. Occurrences/Message Payload Example.sysml",
            "training/28. Individuals/Individuals and Roles-1.sysml",
            "training/28. Individuals/Individuals and Snapshots Example.sysml",
            "training/28. Individuals/Individuals and Time Slices.sysml",
            "training/29. Expressions/Car Mass Rollup Example 1.sysml",
            "training/29. Expressions/Car Mass Rollup Example 2.sysml",
            "training/31. Constraints/Analytical Constraints.sysml",
            "training/31. Constraints/Time Constraints.sysml",
            "training/32. Requirements/Requirement Satisfaction.sysml",
            "training/33. Analysis/Analysis Case Definition Example.sysml",
            "training/33. Analysis/Trade Study Analysis Example.sysml",
            "training/34. Verification/Verification Case Definition Example.sysml",
            "training/34. Verification/Verification Case Usage Example.sysml",
            "training/36. Variability/Variation Configuration.sysml",
            "training/36. Variability/Variation Definitions.sysml",
            "training/38. Allocation/Allocation Usage Example.sysml",
            "training/39. Metadata/Metadata Example-2.sysml",
            "training/40. Filtering/Filtering Example-1.sysml",
            "training/40. Filtering/Filtering Example-2.sysml",
            "training/41. Language Extension/User Keyword Example.sysml",
            "training/42. Views/Views Example.sysml",
            "validation/01-Parts Tree/1a-Parts Tree.sysml",
            "validation/01-Parts Tree/1c-Parts Tree Redefinition.sysml",
            "validation/02-Parts Interconnection/2a-Parts Interconnection.sysml",
            "validation/03-Function-based Behavior/3d-Function-based Behavior-item.sysml",
            "validation/04-Functional Allocation/4a-Functional Allocation.sysml",
            "validation/05-State-based Behavior/5-State-based Behavior-1.sysml",
            "validation/05-State-based Behavior/5-State-based Behavior-1a.sysml",
            "validation/05-State-based Behavior/5-State-based Behavior-2.sysml",
            "validation/07-Variant Configuration/7a1-Variant Configuration - General Concept-a.sysml",
            "validation/07-Variant Configuration/7b-Variant Configurations.sysml",
            "validation/08-Requirements/8-Requirements.sysml",
            "validation/09-Verification/9-Verification-simplified.sysml",
            "validation/10-Analysis and Trades/10b-Trade-off Among Alternative Configurations.sysml",
            "validation/10-Analysis and Trades/10c-Fuel Economy Analysis.sysml",
            "validation/11-View and Viewpoint/11a-View-Viewpoint.sysml",
            "validation/11-View and Viewpoint/11b-Safety and Security Feature Views.sysml",
            "validation/12-Dependency Relationships/12b-Allocation-1.sysml",
            "validation/12-Dependency Relationships/12b-Allocation.sysml",
            "validation/13-Model Containment/13a-Model Containment.sysml",
            "validation/13-Model Containment/13b-Safety and Security Features Element Group-1.sysml",
            "validation/13-Model Containment/13b-Safety and Security Features Element Group-2.sysml",
            "validation/14-Language Extensions/14a-Language Extensions.sysml",
            "validation/14-Language Extensions/14c-Language Extensions.sysml",
            "validation/15-Properties-Values-Expressions/15_12-Compound Value Type.sysml",
            "validation/15-Properties-Values-Expressions/15_13-Discretely Sampled Function Value.sysml",
            "validation/15-Properties-Values-Expressions/15_19-Materials with Properties.sysml",
            "validation/15-Properties-Values-Expressions/15_19a-Materials with Properties.sysml",
            "validation/18-Use Case/18-Use Case.sysml",
        )
    }

    private val kermlProcessor = KerML_v2_2026_09.processorWithReferences()

    private val processor: LanguageProcessor<Asm, SentenceContext> by lazy {
        val referenceStr = CrossReferenceString(File(grammarDir, "references.agl").readText())
        val kermlCrossReferenceDomain = kermlProcessor.crossReferenceDomain // created before the SysML types are added to the KerML types domain
        val res = Agl.processorFromStringSimple(
            GrammarString(File(grammarDir, "grammar.agl").readText()),
            styleStr = StyleString(File(grammarDir, "style.agl").readText()),
            configurationBase = Agl.configuration(Agl.configurationSimple()) {
                typesResolver { ProcessResultDefault(kermlProcessor.typesDomain) }
                crossReferenceResolver { p ->
                    val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(p.typesDomain), referenceStr)
                    res.asm?.merge(kermlCrossReferenceDomain)
                    res
                }
            }
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        res.processor!!
    }

    /** the KerML model libraries processed by the KerML processor, then the SysML model libraries ('valid/library') */
    private fun libraryContext(): SentenceContext {
        val ctx = contextAsmSimple()
        assertEquals(emptyList(), KerML_v2_2026_09.processLibrary(kermlProcessor, ctx))
        val sysmlLib = KerML_v2_2026_09.sentenceFiles(File(languageDir, "valid/library"), "sysml")
        assertTrue(sysmlLib.isNotEmpty(), "library files not found in '${languageDir.absolutePath}'")
        assertEquals(emptyList(), KerML_v2_2026_09.processFiles(processor, sysmlLib, ctx, false))
        return ctx
    }

    @Test
    fun process_library() {
        libraryContext()
    }

    @Test
    fun references_examples() {
        val libCtx = libraryContext()
        val validDir = File(languageDir, "valid")
        val files = listOf("examples", "training", "validation").flatMap { KerML_v2_2026_09.sentenceFiles(File(validDir, it), "sysml") }
        assertTrue(files.size > 200, "example files not found in '${validDir.absolutePath}'")
        val failed = KerML_v2_2026_09.filesWithReferenceErrors(processor, files, libCtx, validDir)
        val unexpectedFailures = failed - knownFailing
        val unexpectedPasses = knownFailing - failed
        assertTrue(unexpectedFailures.isEmpty(), "${unexpectedFailures.size} examples have reference errors:\n${unexpectedFailures.joinToString("\n") { "\"$it\"," }}")
        assertTrue(unexpectedPasses.isEmpty(), "examples now have no reference errors, remove them from 'knownFailing':\n${unexpectedPasses.joinToString("\n")}")
    }
}
