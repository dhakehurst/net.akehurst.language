# SysML v2.0 / KerML v1.0: specification issues and differences to the Pilot Implementation

Issues found while writing the AGL definitions of SysML v2.0 and KerML v1.0
(SysML v2 Release tag `2026-04`, the last release of SysML v2.0 and KerML v1.0). The aim is to collect them so they can be reported to the OMG.

Each entry gives the specification text, what the Pilot Implementation
([SysML-v2-Pilot-Implementation](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation)) does (where checked),
and what the AGL definitions do.

The BNF referred to is `bnf/KerML-textual-bnf.kebnf` and `bnf/SysML-textual-bnf.kebnf` of the release.
Issues of SysML v2.1 / KerML v1.1 are in [../v2.1_2026-09/SPEC_ISSUES.md](../v2.1_2026-09/SPEC_ISSUES.md).

## 1. Errors in the textual BNF

The following are errors in the BNF of release `2026-04`. Each one is corrected in the BNF of SysML v2.1 / KerML v1.1
(release `2026-09.1`). The AGL grammars (`languages/standard` and `languages/agl`) use the corrected rule,
marked `// AGL:` in the grammar.

| # | BNF | Rule | Specification text | Problem | Correction (as in 2.1) |
|---|-----|------|--------------------|---------|------------------------|
| 1.1 | KerML | `SuccessionDeclaration` | `( s.isSufficient ?= 'all' )?` | `s.` is not BNF notation | `( isSufficient ?= 'all' )?` |
| 1.2 | KerML | `PositionalArgumentList` | `e.ownedRelationship += ArgumentMember ( ',' e.ownedRelationship += ArgumentMember )*` | `e.` is not BNF notation | `ownedRelationship += ...` |
| 1.3 | KerML | `FunctionOperationExpression` | `ownedRelationship += InvocationTypeMember` | `InvocationTypeMember` is not defined | `InstantiatedTypeMember` |
| 1.4 | KerML | `MetaclassificationExpression` | `operator = ClassificationTestOperator` | the rule `MetaclassificationTestOperator` (`'@@'`) is defined but not used, so `x @@ T` cannot be parsed | `operator = MetaclassificationTestOperator` |
| 1.5 | SysML | `SuccessionAsUsage` | `'first' s.ownedRelationship += ConnectorEndMember 'then' s.ownedRelationship += ConnectorEndMember` | `s.` is not BNF notation | `ownedRelationship += ...` |
| 1.6 | SysML | `FilterPackage` | `ownedRelationship += FilterPackageImport` | `FilterPackageImport` is not defined | `FilterPackageImport : Import = ImportDeclaration { visibility = 'public' }` |
| 1.7 | SysML | `FramedConcernUsage` | `... FeatureSpecializationPart? CalculationBody \| ... CalculationUsageDeclaration CalculationBody` | `CalculationUsageDeclaration` is not defined. Also, a framed concern is a `ConcernUsage`, whose body is a `RequirementBody` (see `ConcernUsage`), but here it is a `CalculationBody` | `... FeatureSpecializationPart? RequirementBody \| ... ConstraintUsageDeclaration RequirementBody` |

## 2. `new` is used but not a reserved keyword

**Specification.** KerML 8.2.2.6 (Reserved Words) and the `RESERVED_KEYWORD` rule of both BNF files do not list `new`,
but `ConstructorExpression` (KerML 8.2.5.8.3) is `'new' ownedRelationship += InstantiatedTypeMember ...`,
and the examples use it (e.g. `training/21. Asynchronous Messaging/Messaging Example.sysml`: `send new Show(shoot.picture) to screen`).
SysML v2.1 adds `new` to the SysML `RESERVED_KEYWORD` (KerML v1.1 still does not list it).

**AGL.** The grammars use the Classic scanner, which reserves every literal of the grammar, so `new` is reserved
(as in SysML v2.1).
