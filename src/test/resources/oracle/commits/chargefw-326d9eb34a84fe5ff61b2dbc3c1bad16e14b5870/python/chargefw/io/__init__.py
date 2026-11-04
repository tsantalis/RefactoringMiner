"""Native molecular input and generated calculation output."""

from __future__ import annotations

from os import PathLike
from pathlib import Path
from typing import TYPE_CHECKING, Literal, TypeAlias

from .._chargefw import adapters as _native_adapters
from ..core import (
    Molecule,
    MoleculeCollection,
    SourceAtomReference,
    SourceComponentInstance,
    SourceConformerReference,
    SourceHierarchyLabels,
    SourceMapping,
    SourceStructuralLabels,
)

if TYPE_CHECKING:
    from ..calculation import CalculationResult

InputFormat: TypeAlias = Literal["mol", "sdf", "mol2", "molecule-json", "pdb", "mmcif"]
OutputFormat: TypeAlias = Literal["mol2", "mmcif", "result-json"]
RecordSelection: TypeAlias = Literal["all", "polymers-and-ligands", "polymers"]
BondStrategy: TypeAlias = Literal["none", "explicit", "templates", "hybrid"]
ConformerSelection: TypeAlias = Literal["first", "all"]

INPUT_FORMATS: tuple[InputFormat, ...] = ("mol", "sdf", "mol2", "molecule-json", "pdb", "mmcif")
OUTPUT_FORMATS: tuple[OutputFormat, ...] = ("mol2", "mmcif", "result-json")
_STRUCTURAL_FORMATS = frozenset(("pdb", "mmcif"))
_MULTI_CONFORMER_FORMATS = frozenset(("molecule-json", "pdb", "mmcif"))


class _ImportedMolecule(Molecule):
    __slots__ = ("_native_input_metadata",)

    _native_input_metadata: _native_adapters._NativeInputMetadata


def _molecule(payload: _native_adapters.MoleculePayload) -> Molecule:
    coordinates = payload["coordinates"] or None
    import_metadata = payload["import_metadata"]

    def atom_reference(
        value: _native_adapters.AtomReferencePayload,
    ) -> SourceAtomReference:
        position, source_id, labels = value
        return SourceAtomReference(
            position,
            source_id,
            None
            if labels is None
            else SourceStructuralLabels(
                author=SourceHierarchyLabels(**labels["author"]),
                label=SourceHierarchyLabels(**labels["label"]),
                entity=labels["entity"],
                insertion_code=labels["insertion_code"],
                alternate_location=labels["alternate_location"],
                segment=labels["segment"],
            ),
        )

    source_mapping = (
        None
        if import_metadata is None
        else SourceMapping(
            format=import_metadata["format"],
            atoms=tuple(atom_reference(value) for value in import_metadata["atoms"]),
            conformers=tuple(
                SourceConformerReference(
                    position,
                    source_id,
                    tuple(atom_reference(value) for value in sites),
                )
                for position, source_id, sites in import_metadata["conformers"]
            ),
            source_connectivity=import_metadata["source_connectivity"],
            record_selection=import_metadata["record_selection"],
            alternate_location_selection=import_metadata["alternate_location_selection"],
            conformer_selection=import_metadata["conformer_selection"],
            bond_strategy=import_metadata["bond_strategy"],
            components=tuple(
                SourceComponentInstance(component_id, tuple(atom_indices))
                for component_id, atom_indices in import_metadata["components"]
            ),
        )
    )
    result = _ImportedMolecule(
        atomic_numbers=payload["atomic_numbers"],
        formal_charges=payload["formal_charges"],
        bonds=payload["bonds"],
        coordinates=coordinates,
        name=payload["name"],
        atom_names=payload["atom_names"],
        conformer_names=payload["conformer_names"],
        source_name=payload["source"],
        record_index=payload["record_index"],
        record_id=payload["record_id"],
        atom_ids=(
            None
            if source_mapping is None
            else tuple(
                reference.position if reference.id is None else reference.id
                for reference in source_mapping.atoms
            )
        ),
    )
    object.__setattr__(result, "_source_mapping", source_mapping)
    object.__setattr__(result, "_native_input_metadata", payload["native_input_metadata"])
    return result


def _collection(
    payloads: list[_native_adapters.MoleculePayload], source_name: str
) -> MoleculeCollection:
    molecules = tuple(_molecule(payload) for payload in payloads)
    return MoleculeCollection(molecules, source_name)


def _validate_options(
    format: InputFormat,
    selection: RecordSelection,
    bonds: BondStrategy | None,
    conformers: ConformerSelection,
) -> None:
    if not isinstance(format, str):
        raise TypeError("format must be a string")
    if format not in INPUT_FORMATS:
        raise ValueError(f"unsupported molecular input format: {format}")
    if format not in _STRUCTURAL_FORMATS:
        if selection != "all":
            raise ValueError("selection is only supported for PDB and mmCIF input")
        if bonds is not None:
            raise ValueError("bonds is only supported for PDB and mmCIF input")
    if format not in _MULTI_CONFORMER_FORMATS and conformers != "all":
        raise ValueError("conformers is only supported for molecule JSON, PDB, and mmCIF input")


def parse(
    contents: str,
    *,
    format: InputFormat,
    source_name: str = "",
    selection: RecordSelection = "all",
    bonds: BondStrategy | None = None,
    conformers: ConformerSelection = "all",
) -> MoleculeCollection:
    """Parse molecular text using an explicitly selected native reader."""

    if not isinstance(contents, str):
        raise TypeError("contents must be a string")
    if not isinstance(source_name, str):
        raise TypeError("source_name must be a string")
    _validate_options(format, selection, bonds, conformers)
    payloads = _native_adapters._parse(
        contents, source_name, format, selection, "hybrid" if bonds is None else bonds, conformers
    )
    return _collection(payloads, source_name)


def read(
    path: str | PathLike[str],
    *,
    format: InputFormat,
    selection: RecordSelection = "all",
    bonds: BondStrategy | None = None,
    conformers: ConformerSelection = "all",
) -> MoleculeCollection:
    """Read UTF-8 molecular text using an explicitly selected native reader."""

    if not isinstance(path, (str, PathLike)):
        raise TypeError("path must be a string or path-like value")
    source_path = Path(path)
    return parse(
        source_path.read_text(encoding="utf-8"),
        format=format,
        source_name=str(source_path),
        selection=selection,
        bonds=bonds,
        conformers=conformers,
    )


def dumps(
    result: CalculationResult,
    *,
    format: OutputFormat,
) -> str:
    """Serialize a calculation result through a native output writer."""

    from ..calculation import CalculationResult

    if not isinstance(result, CalculationResult):
        raise TypeError("result must be a CalculationResult")
    if not isinstance(format, str):
        raise TypeError("format must be a string")
    if format not in OUTPUT_FORMATS:
        raise ValueError(f"unsupported calculation output format: {format}")
    return _native_adapters._dumps(result._native, format)


def write(
    path: str | PathLike[str],
    result: CalculationResult,
    *,
    format: OutputFormat,
) -> None:
    """Serialize a calculation result as UTF-8 text."""

    if not isinstance(path, (str, PathLike)):
        raise TypeError("path must be a string or path-like value")
    contents = dumps(result, format=format)
    Path(path).write_text(contents, encoding="utf-8")


__all__ = [
    "InputFormat",
    "OutputFormat",
    "INPUT_FORMATS",
    "OUTPUT_FORMATS",
    "RecordSelection",
    "BondStrategy",
    "ConformerSelection",
    "parse",
    "read",
    "dumps",
    "write",
]
