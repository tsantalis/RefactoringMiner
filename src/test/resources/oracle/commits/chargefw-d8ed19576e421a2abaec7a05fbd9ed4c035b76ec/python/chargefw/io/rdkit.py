"""Optional conversion and charge attachment for RDKit molecules."""

from __future__ import annotations

from importlib import import_module
from operator import index as as_index
from typing import Any, Literal

import numpy as np

from ..calculation import CalculationResult
from ..charges import ChargeAssignment
from ..core import Molecule

try:
    from rdkit import Chem as _rdkit  # type: ignore[import-not-found, unused-ignore]
except ModuleNotFoundError as error:
    if error.name not in ("rdkit", "rdkit.Chem"):
        raise
    _rdkit = None  # type: ignore[assignment, unused-ignore]

type BondConversion = Literal["none", "single"]


def _require_rdkit() -> Any:
    try:
        return import_module("rdkit.Chem")
    except ModuleNotFoundError as error:
        if error.name not in ("rdkit", "rdkit.Chem"):
            raise
        raise ImportError(
            "chargefw.io.rdkit requires the optional RDKit Python integration; "
            "install it with `pip install chargefw[rdkit]`"
        ) from error


def from_mol(
    molecule: _rdkit.Mol, *, source_name: str = "", bond_conversion: BondConversion = "none"
) -> Molecule:
    """Copy an RDKit molecule, optionally converting aromatic and dative bonds to single."""

    chemistry = _require_rdkit()
    if not isinstance(molecule, chemistry.Mol):
        raise TypeError("molecule must be an rdkit.Chem.Mol")
    if not isinstance(source_name, str):
        raise TypeError("source_name must be a string")
    if not isinstance(bond_conversion, str):
        raise TypeError("bond_conversion must be a string")
    if bond_conversion not in ("none", "single"):
        raise ValueError("bond_conversion must be 'none' or 'single'")

    # Inspect a copy because RDKit computes implicit hydrogen counts lazily. A quick copy omits
    # conformers and properties, which the hydrogen counts do not need.
    hydrogen_counts = chemistry.Mol(molecule, quickCopy=True)
    hydrogen_counts.UpdatePropertyCache(strict=False)
    if any(atom.GetTotalNumHs() for atom in hydrogen_counts.GetAtoms()):
        raise ValueError(
            "RDKit molecule has hydrogens that are not explicit atoms; add them with Chem.AddHs()"
        )

    atoms = tuple(molecule.GetAtoms())
    bonds: list[tuple[int, int, int]] = []
    direct_bond_orders = {
        chemistry.BondType.SINGLE: 1,
        chemistry.BondType.DOUBLE: 2,
        chemistry.BondType.TRIPLE: 3,
    }
    normalized_bond_types = {
        value
        for name in ("AROMATIC", "DATIVEONE", "DATIVE", "DATIVEL", "DATIVER")
        if (value := getattr(chemistry.BondType, name, None)) is not None
    }
    for bond in molecule.GetBonds():
        if bond.HasQuery():
            raise ValueError("RDKit query bonds cannot be imported as molecular input")
        bond_type = bond.GetBondType()
        is_aromatic = bond.GetIsAromatic()
        order = direct_bond_orders.get(bond_type) if not is_aromatic else None
        if (
            order is None
            and bond_conversion == "single"
            and (is_aromatic or bond_type in normalized_bond_types)
        ):
            order = 1
        if order is None:
            raise ValueError(
                f"RDKit molecule contains unsupported bond type {bond_type}; only explicit single, "
                "double, and triple bonds are accepted without bond conversion"
            )
        bonds.append((bond.GetBeginAtomIdx(), bond.GetEndAtomIdx(), order))

    conformers = tuple(molecule.GetConformers())
    coordinates = (
        [
            [
                (
                    conformer.GetAtomPosition(index).x,
                    conformer.GetAtomPosition(index).y,
                    conformer.GetAtomPosition(index).z,
                )
                for index in range(len(atoms))
            ]
            for conformer in conformers
        ]
        if conformers
        else None
    )
    atom_names = tuple(
        atom.GetProp("_TriposAtomName")
        if atom.HasProp("_TriposAtomName")
        else f"{atom.GetSymbol()}{atom.GetIdx() + 1}"
        for atom in atoms
    )
    name = molecule.GetProp("_Name") if molecule.HasProp("_Name") else ""
    return Molecule(
        [atom.GetAtomicNum() for atom in atoms],
        formal_charges=[atom.GetFormalCharge() for atom in atoms],
        bonds=bonds,
        coordinates=coordinates,
        name=name,
        atom_names=atom_names,
        conformer_names=[str(value.GetId()) for value in conformers],
        source_name=source_name,
        atom_ids=range(len(atoms)),
    )


def _assignment(
    result: CalculationResult, molecule_index: int, conformer: int | None
) -> ChargeAssignment:
    assignments = result.assignments_by_molecule[molecule_index]
    if conformer is not None:
        matches = tuple(value for value in assignments if value.conformer_index == conformer)
        if len(matches) == 1:
            return matches[0]
        if len(assignments) == 1 and assignments[0].conformer_index is None:
            return assignments[0]
        raise ValueError("selected conformer has no charge assignment")
    if len(assignments) != 1:
        raise ValueError("conformer is required when the result has multiple assignments")
    return assignments[0]


def attach_charges(
    molecule: _rdkit.Mol,
    result: CalculationResult,
    *,
    molecule_index: int = 0,
    conformer: int | None = None,
    property_name: str = "ChargeFWPartialCharge",
    overwrite: bool = False,
) -> None:
    """Attach one calculated charge assignment to RDKit atom properties in place."""

    chemistry = _require_rdkit()
    if not isinstance(molecule, chemistry.Mol):
        raise TypeError("molecule must be an rdkit.Chem.Mol")
    if not isinstance(result, CalculationResult):
        raise TypeError("result must be a CalculationResult")
    if isinstance(molecule_index, bool):
        raise TypeError("molecule_index must be an integer")
    try:
        molecule_index = as_index(molecule_index)
    except TypeError as error:
        raise TypeError("molecule_index must be an integer") from error
    if molecule_index < 0 or molecule_index >= len(result.molecules):
        raise IndexError("molecule_index is outside the calculation input collection")
    if isinstance(conformer, bool):
        raise TypeError("conformer must be an integer or None")
    if conformer is not None:
        try:
            conformer = as_index(conformer)
        except TypeError as error:
            raise TypeError("conformer must be an integer or None") from error
        if conformer < 0:
            raise ValueError("conformer must be non-negative")
    if not isinstance(property_name, str):
        raise TypeError("property_name must be a string")
    if not property_name:
        raise ValueError("property_name must not be empty")
    if not isinstance(overwrite, bool):
        raise TypeError("overwrite must be a bool")

    assignment = _assignment(result, molecule_index, conformer)
    source = result.molecules[molecule_index]
    if molecule.GetNumAtoms() != source.atom_count:
        raise ValueError("RDKit target atom count does not match the calculation input")
    property_list_name = f"atom.dprop.{property_name}"
    if not overwrite and molecule.HasProp(property_list_name):
        raise ValueError(f"RDKit molecule already has property {property_list_name!r}")
    atom_indices: list[int] = []
    for atom_id in source.atom_ids:
        if isinstance(atom_id, (str, bool, np.bool)):
            raise ValueError("RDKit attachment requires integer atom IDs")
        try:
            atom_index = as_index(atom_id)
        except TypeError as error:
            raise ValueError("RDKit attachment requires integer atom IDs") from error
        if atom_index < 0 or atom_index >= molecule.GetNumAtoms():
            raise ValueError("RDKit atom ID is outside the target molecule")
        atom_indices.append(atom_index)
    if len(set(atom_indices)) != molecule.GetNumAtoms():
        raise ValueError("RDKit atom IDs must map each target atom exactly once")

    for source_index, atom_index in enumerate(atom_indices):
        atom = molecule.GetAtomWithIdx(atom_index)
        if atom.GetAtomicNum() != int(source.atomic_numbers[source_index]):
            raise ValueError("RDKit target atom mapping does not match atomic numbers")
        if atom.GetFormalCharge() != int(source.formal_charges[source_index]):
            raise ValueError("RDKit target atom mapping does not match formal charges")
        if not overwrite and atom.HasProp(property_name):
            raise ValueError(f"RDKit atom already has property {property_name!r}")

    for atom_index, charge in zip(atom_indices, assignment.values, strict=True):
        molecule.GetAtomWithIdx(atom_index).SetDoubleProp(property_name, float(charge))
    create_property_list = getattr(chemistry, "CreateAtomDoublePropertyList", None)
    if create_property_list is not None:
        create_property_list(molecule, property_name)


__all__ = ["BondConversion", "from_mol", "attach_charges"]
