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

**AGL.** Not decided yet (open question q47a).
