# SysML v2 / KerML: specification issues and differences to the Pilot Implementation

Issues found while writing the AGL definitions of SysML v2.1 Beta 2 and KerML v1.1 Beta 2
(SysML v2 Release tag `2026-09.1`). The aim is to collect them so they can be reported to the OMG.

Each entry gives the specification text, what the Pilot Implementation
([SysML-v2-Pilot-Implementation](https://github.com/Systems-Modeling/SysML-v2-Pilot-Implementation)) does,
and what the AGL definitions do.

References to "KerML" are to *Kernel Modeling Language v1.1, Beta 2*.

## 1. Name conflicts from recursive imports (`::**`)

**Specification.** KerML 7.2.5.4 (Imports): "If the member name or member short name of any imported membership
conflicts with the name of any owned member, or with the name of any visible membership from any other imported
namespace, then the conflicting membership is hidden and is not included in the set of imported memberships of the
importing namespace." A recursive import (`::**`) is the same as importing the namespace and all its nested
namespaces. KerML 8.2.3.5.3: a well-formed namespace has at most one membership that is the local resolution of a name.

**Unclear in the specification.**
- When two imported memberships conflict, it does not say whether both are hidden, or only one (and which).
- For a recursive import, it does not say whether the nested namespaces count as "other imported namespaces",
  so that members with the same name at different depths hide each other.

**Pilot Implementation.** It does not hide conflicts. `KerMLScope` (`resolveIfUnvisited`, `resolveRecursive`)
searches the namespace's own members first, then its public nested namespaces in order, depth first;
the first membership found wins. Imports are tried in order, and the first one that finds the name wins.

**Example.** `examples/Vehicle Example/SysML v2 Spec Annex A SimpleVehicleModel.sysml` has
`public import VehicleConfigurations::VehicleConfiguration_b::**;`. The package `VehicleConfiguration_b` has
`PartsTree::vehicle_b` and also, nested deeper, `occurrence CruiseControl1 { part vehicle_b :> PartsTree::vehicle_b ... }`
and the same in `CruiseControl2`. With the Pilot behaviour `vehicle_b` resolves to `PartsTree::vehicle_b`
(found first). With the specification rule, the name conflicts, so it is hidden and does not resolve through that import.
The example model seems to rely on the Pilot behaviour.

**AGL.** Follows the specification (decided 2026-11-25): a conflicting name is not resolved through the import and is reported as ambiguous. Examples that rely on the Pilot behaviour are listed as known failing in the reference tests.

## 2. `new` is used as a keyword in KerML but is not reserved

**Specification.** KerML 1.1 uses `'new'` as a keyword in the expression grammar
(`bnf/KerML-textual-bnf.kebnf`: `'new' ownedRelationship += InstantiatedTypeMember`), but `new` is not in the list of
reserved words (KerML 8.2.2.6, and `RESERVED_KEYWORD` in `KerML-textual-bnf.kebnf`). The SysML 2.1 `RESERVED_KEYWORD`
list does include `'new'`.

**Issue.** In KerML, `new` can be used as a name, which makes `new T(...)` ambiguous with a name `new` followed by other
text. The KerML list seems to be missing `new`, as in SysML.

**AGL.** The KerML grammars use `#scanner: Classic`, which scans every literal of the grammar as a keyword, so `new` cannot be used as a name (as in SysML).

## 3. The OMG metamodel XMI does not mark composite properties

**Specification.** The machine-readable abstract syntax published by the OMG,
`https://www.omg.org/spec/KerML/20250201/KerML.xmi` and `https://www.omg.org/spec/SysML/20250201/SysML.xmi`
(KerML 1.0 / SysML 2.0; no 1.1 / 2.1 files are published yet), has no `aggregation="composite"` on any property.
The property descriptions in the specification documents (e.g. KerML 1.1 8.3.2.1, `ownedRelationship : Relationship [0..*] {subsets relationship, ordered}`)
do not say whether a property is composite either.

**Pilot Implementation.** Its UML model (`org.omg.sysml/model/SysML_xmi.uml`, at tags 2026-04 and 2026-09.1) marks 43 properties
as composite, for example `Element::ownedRelationship`, `Relationship::ownedRelatedElement`, `Namespace::ownedMembership`
and `OwningMembership::ownedMemberElement`.

**Issue.** Apart from this, the Pilot model and the OMG XMI give the same classes, generalizations and properties
(compared after converting both to agl types). Ownership (containment) is central to the KerML abstract syntax
(owned relationships and owned related elements), so the normative XMI should state it.

**AGL.** The metamodel types for agl are generated from the Pilot model, so these properties are composite.

## 4. `superClassifier` is not a property of Subclassification

**Specification.** SysML 2.1 8.2.2.6.5 (and `bnf/SysML-textual-bnf.kebnf`):
`OwnedSubclassification : Subclassification = superClassifier = [QualifiedName]`.
The metaclass `Subclassification` has the property `superclassifier` (lower case `c`), as in the KerML BNF.

**Pilot Implementation.** `superclassifier = [SysML::Classifier | QualifiedName]`.

## 5. `InitialNodeMember` assigns `memberFeature`, which `FeatureMembership` does not have

**Specification.** SysML 2.1 8.2.2.17.1 (and `bnf/SysML-textual-bnf.kebnf`):
`InitialNodeMember : FeatureMembership = MemberPrefix 'first' memberFeature = [QualifiedName] RelationshipBody`.
`FeatureMembership` has no property `memberFeature`, and it is an `OwningMembership`, whose member element is owned,
while `first x` refers to an existing feature `x`.

**Pilot Implementation.** `InitialNodeMember returns SysML::Membership : MemberPrefix 'first' memberElement = [SysML::Feature|QualifiedName] RelationshipBody`,
i.e. a (non-owning) `Membership` whose `memberElement` is the referenced feature.

## 6. FeatureInverting: the inverting feature chain is assigned to the wrong property

**Specification.** `bnf/KerML-textual-bnf.kebnf`, rule `FeatureInverting` (KerML 1.1 8.2.4.3):
`( invertingFeature = [QualifiedName] | ownedRelatedElement += OwnedFeatureChain { ownedRelatedElement += invertingFeature } )`.
The first alternative for `featureInverted` is written `featureInverted = OwnedFeatureChain { ownedRelatedElement += featureInverted }`,
so for consistency the second should be `invertingFeature = OwnedFeatureChain { ownedRelatedElement += invertingFeature }`.
As written, the chain is added to `ownedRelatedElement` and then `invertingFeature` (not set) is added again.

## 7. `OwnedExpressionMember` assigns `ownedFeatureMember`, which is not a property

**Specification.** `bnf/KerML-textual-bnf.kebnf`: `OwnedExpressionMember : FeatureMembership = ownedFeatureMember = OwnedExpression`.
`FeatureMembership` has `ownedMemberFeature`, not `ownedFeatureMember`.

## 8. `TypeResultMember : ResultParameterMembership`: no such metaclass

**Specification.** `bnf/KerML-textual-bnf.kebnf`: `TypeResultMember : ResultParameterMembership = ownedMemberFeature = TypeReference`.
The metamodel has no `ResultParameterMembership`; the metaclass for a result parameter is `ReturnParameterMembership`.

## 9. Rule `LiteralReal` creates a `LiteralReal`, but the metaclass is `LiteralRational`

**Specification.** `bnf/KerML-textual-bnf.kebnf`: `LiteralReal = value = RealValue`. A rule without `: Metaclass` creates an
element of the metaclass with the rule's name, but the metamodel has `LiteralRational` (value : Real), not `LiteralReal`.

(Entries 6 to 9 were found by the generator of the asm-transform from the BNF to the metamodel, in the SysML v2.0 work; they are the same in 2.1.)

## 10. `OwnedReferenceSubsetting` adds `referenceFeature`, which is not a property

**Specification.** `bnf/SysML-textual-bnf.kebnf` (SysML 2.1 8.2.2.6.5): `OwnedReferenceSubsetting : ReferenceSubsetting =
referencedFeature = [QualifiedName] | referencedFeature = OwnedFeatureChain { ownedRelatedElement += referenceFeature }`.
The property is `referencedFeature`; `referenceFeature` does not exist (the KerML rule has `referencedFeature`).

## 11. `ConjugatedPortTyping` assigns `originalPortDefinition`, which `ConjugatedPortTyping` does not have

**Specification.** `bnf/SysML-textual-bnf.kebnf` (SysML 2.1 8.2.2.12): `ConjugatedPortTyping : ConjugatedPortTyping = '~' originalPortDefinition = ~[QualifiedName]`.
`ConjugatedPortTyping` has `conjugatedPortDefinition` (stored) and `portDefinition` (derived); `originalPortDefinition`
is a property of `PortConjugation` and `ConjugatedPortDefinition`. Since `~[QualifiedName]` resolves to the conjugated port
definition (BNF Note 2), the assignment seems meant to be `conjugatedPortDefinition = ~[QualifiedName]`.
