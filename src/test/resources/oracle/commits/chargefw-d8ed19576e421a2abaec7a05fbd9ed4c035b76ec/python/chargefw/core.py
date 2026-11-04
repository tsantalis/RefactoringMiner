"""Owned toolkit-neutral molecule values."""

from __future__ import annotations

from collections.abc import Iterable, Iterator, Sequence
from dataclasses import dataclass
from operator import index as as_index
from typing import Any, Literal, overload

import numpy as np
from numpy.typing import NDArray

from ._arrays import (
    as_coordinates,
    as_ids,
    as_integer_array,
    as_names,
    as_portable_id,
    immutable_array,
)
from ._chargefw import core as _native_core
from ._types import PortableId as _PortableId

type PortableId = _PortableId


@dataclass(frozen=True, slots=True)
class SourceIdentity:
    """Identity of the source record represented by a molecule."""

    source: str = ""
    record_index: int = 0
    record_id: PortableId | None = None

    def __post_init__(self) -> None:
        if not isinstance(self.source, str):
            raise TypeError("source must be a string")
        if isinstance(self.record_index, (bool, np.bool)):
            raise TypeError("record_index must be an integer")
        try:
            normalized_record_index = as_index(self.record_index)
        except TypeError as error:
            raise TypeError("record_index must be an integer") from error
        if normalized_record_index < 0:
            raise ValueError("record_index must be non-negative")
        normalized_record_id = (
            None if self.record_id is None else as_portable_id(self.record_id, "record_id")
        )
        object.__setattr__(self, "record_index", normalized_record_index)
        object.__setattr__(self, "record_id", normalized_record_id)

    @property
    def source_name(self) -> str:
        return self.source


type SourceFormat = Literal["mol", "sdf", "mol2", "molecule-json", "pdb", "mmcif"]
type SourceConnectivity = Literal["absent", "explicitly-empty", "present"]


@dataclass(frozen=True, slots=True)
class SourceHierarchyLabels:
    """Atom, residue, chain, and sequence labels in one structural namespace."""

    atom: str | None = None
    residue: str | None = None
    chain: str | None = None
    sequence: str | None = None


@dataclass(frozen=True, slots=True)
class SourceStructuralLabels:
    """Author and label identities retained for a structural atom site."""

    author: SourceHierarchyLabels
    label: SourceHierarchyLabels
    entity: str | None = None
    insertion_code: str | None = None
    alternate_location: str | None = None
    segment: str | None = None


@dataclass(frozen=True, slots=True)
class SourceAtomReference:
    """Location of one calculation atom in its source record."""

    position: int
    id: str | None = None
    structural_labels: SourceStructuralLabels | None = None


@dataclass(frozen=True, slots=True)
class SourceConformerReference:
    """Location and selected atom sites for one retained conformer."""

    position: int
    id: str | None
    sites: tuple[SourceAtomReference, ...]


@dataclass(frozen=True, slots=True)
class SourceComponentInstance:
    """Selected structural residue membership in calculation-atom order."""

    component_id: str
    atom_indices: tuple[int, ...]


@dataclass(frozen=True, slots=True)
class SourceMapping:
    """Read-only source correspondence retained for an imported molecule."""

    format: SourceFormat
    atoms: tuple[SourceAtomReference, ...]
    conformers: tuple[SourceConformerReference, ...]
    source_connectivity: SourceConnectivity
    record_selection: str | None = None
    alternate_location_selection: str | None = None
    conformer_selection: str | None = None
    bond_strategy: str | None = None
    components: tuple[SourceComponentInstance, ...] = ()


class Molecule:
    """Immutable, owned toolkit-neutral molecular data."""

    __slots__ = (
        "_atomic_numbers",
        "_formal_charges",
        "_bonds",
        "_coordinates",
        "_name",
        "_atom_names",
        "_conformer_names",
        "_source",
        "_source_mapping",
        "_atom_ids",
        "_caller_atom_ids",
        "_native",
    )

    _atomic_numbers: NDArray[np.int64]
    _formal_charges: NDArray[np.int64]
    _bonds: NDArray[np.int64]
    _coordinates: NDArray[np.float64]
    _name: str
    _atom_names: tuple[str, ...]
    _conformer_names: tuple[str, ...]
    _source: SourceIdentity
    _source_mapping: SourceMapping | None
    _atom_ids: tuple[PortableId, ...]
    _caller_atom_ids: tuple[PortableId, ...] | None
    _native: _native_core._NativeMolecule

    def __init__(
        self,
        atomic_numbers: Any,
        formal_charges: Any = None,
        bonds: Any = None,
        coordinates: Any = None,
        name: str | None = None,
        atom_names: Sequence[str] | None = None,
        conformer_names: Sequence[str] | None = None,
        source: SourceIdentity | None = None,
        source_name: str | None = None,
        record_index: int = 0,
        record_id: PortableId | None = None,
        atom_ids: Iterable[PortableId] | None = None,
    ) -> None:
        if name is not None and not isinstance(name, str):
            raise TypeError("name must be a string or None")
        atomic = as_integer_array(atomic_numbers, "atomic_numbers", 1)
        if np.any((atomic < 1) | (atomic > 100)):
            raise ValueError("atomic_numbers must contain values in range 1..100")
        atom_count = len(atomic)

        if formal_charges is None:
            formal = np.zeros(atom_count, dtype=np.int64)
        else:
            formal = as_integer_array(formal_charges, "formal_charges", 1)
            if len(formal) != atom_count:
                raise ValueError("formal_charges must have the same length as atomic_numbers")
        int_limits = np.iinfo(np.int32)
        if np.any((formal < int_limits.min) | (formal > int_limits.max)):
            raise ValueError("formal_charges contain values outside the native integer range")

        if bonds is None:
            bond_array = np.empty((0, 3), dtype=np.int64)
        else:
            bond_array = as_integer_array(bonds, "bonds", 2, empty_1d_shape=(0, 3))
            if bond_array.shape[1] != 3:
                raise ValueError("bonds must have shape (B, 3)")
        if len(bond_array) != 0:
            endpoints = bond_array[:, :2]
            if np.any((endpoints < 0) | (endpoints >= atom_count)):
                raise ValueError("bond atom indices must be within the molecule")
            if np.any(endpoints[:, 0] == endpoints[:, 1]):
                raise ValueError("bond endpoints must refer to different atoms")
            if np.any((bond_array[:, 2] < 1) | (bond_array[:, 2] > 3)):
                raise ValueError("bond orders must be 1, 2, or 3")

        coordinate_values = as_coordinates(coordinates, atom_count)
        atom_name_values = as_names(atom_names, atom_count, "atom_names")
        conformer_count = coordinate_values.shape[0]
        conformer_name_values = as_names(conformer_names, conformer_count, "conformer_names")

        if source is not None:
            if not isinstance(source, SourceIdentity):
                raise TypeError("source must be a SourceIdentity")
            if source_name is not None or record_index != 0 or record_id is not None:
                raise ValueError("source cannot be combined with source identity fields")
            source_identity = source
        else:
            if source_name is not None and not isinstance(source_name, str):
                raise TypeError("source_name must be a string or None")
            source_identity = SourceIdentity(source_name or "", record_index, record_id)

        atom_id_values = as_ids(atom_ids, atom_count, "atom_ids")

        atomic = immutable_array(atomic)
        formal = immutable_array(formal)
        bond_array = immutable_array(bond_array)
        coordinate_values = immutable_array(coordinate_values)

        object.__setattr__(self, "_atomic_numbers", atomic)
        object.__setattr__(self, "_formal_charges", formal)
        object.__setattr__(self, "_bonds", bond_array)
        object.__setattr__(self, "_coordinates", coordinate_values)
        object.__setattr__(self, "_name", name or "")
        object.__setattr__(self, "_atom_names", atom_name_values)
        object.__setattr__(self, "_conformer_names", conformer_name_values)
        object.__setattr__(self, "_source", source_identity)
        object.__setattr__(self, "_source_mapping", None)
        object.__setattr__(self, "_atom_ids", atom_id_values)
        object.__setattr__(self, "_caller_atom_ids", None if atom_ids is None else atom_id_values)
        object.__setattr__(
            self,
            "_native",
            _native_core._make_molecule(
                atomic,
                formal,
                bond_array,
                coordinate_values,
                atom_name_values,
                conformer_name_values,
                name or "",
            ),
        )

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError(f"{type(self).__name__} is immutable")

    @property
    def atomic_numbers(self) -> NDArray[np.int64]:
        """Read-only int64 array of shape (atom_count,) in molecule atom order."""

        return self._atomic_numbers

    @property
    def formal_charges(self) -> NDArray[np.int64]:
        """Read-only int64 array of formal charges, shape (atom_count,)."""

        return self._formal_charges

    @property
    def bonds(self) -> NDArray[np.int64]:
        """Read-only int64 array of shape (bond_count, 3).

        Rows contain (first_atom, second_atom, order), with zero-based atom indices
        and bond orders 1, 2, or 3.
        """

        return self._bonds

    @property
    def coordinates(self) -> NDArray[np.float64]:
        """Read-only float64 array in angstroms, shape (conformer_count, atom_count, 3).

        The first dimension is zero when no conformers are present.
        """

        return self._coordinates

    @property
    def name(self) -> str:
        return self._name

    @property
    def atom_names(self) -> tuple[str, ...]:
        return self._atom_names

    @property
    def conformer_names(self) -> tuple[str, ...]:
        return self._conformer_names

    @property
    def source(self) -> SourceIdentity:
        return self._source

    @property
    def source_name(self) -> str:
        return self._source.source

    @property
    def source_mapping(self) -> SourceMapping | None:
        return self._source_mapping

    @property
    def record_index(self) -> int:
        return self._source.record_index

    @property
    def record_id(self) -> PortableId | None:
        return self._source.record_id

    @property
    def atom_ids(self) -> tuple[PortableId, ...]:
        return self._atom_ids

    @property
    def atom_count(self) -> int:
        return len(self._atomic_numbers)

    @property
    def bond_count(self) -> int:
        return len(self._bonds)

    @property
    def conformer_count(self) -> int:
        return int(self._coordinates.shape[0])

    @property
    def has_coordinates(self) -> bool:
        return self.conformer_count != 0

    def __len__(self) -> int:
        """Return the number of atoms."""

        return self.atom_count

    def __repr__(self) -> str:
        return (
            f"{type(self).__name__}(atom_count={self.atom_count}, bond_count={self.bond_count}, "
            f"conformer_count={self.conformer_count}, name={self.name!r})"
        )


class MoleculeCollection(Sequence[Molecule]):
    """Immutable source-ordered collection of molecules."""

    __slots__ = ("_molecules", "_name", "_native_molecules")

    _molecules: tuple[Molecule, ...]
    _name: str
    _native_molecules: tuple[_native_core._NativeMolecule, ...]

    def __init__(self, molecules: Iterable[Molecule], name: str | None = None) -> None:
        if isinstance(molecules, (str, bytes, Molecule)):
            raise TypeError("molecules must be an iterable of Molecule values")
        if name is not None and not isinstance(name, str):
            raise TypeError("name must be a string or None")
        try:
            values = tuple(molecules)
        except TypeError as error:
            raise TypeError("molecules must be an iterable of Molecule values") from error
        if not all(isinstance(molecule, Molecule) for molecule in values):
            raise TypeError("molecules must contain only Molecule values")
        object.__setattr__(self, "_molecules", values)
        object.__setattr__(self, "_name", name or "")
        object.__setattr__(
            self,
            "_native_molecules",
            tuple(value._native for value in values),
        )

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError(f"{type(self).__name__} is immutable")

    def __len__(self) -> int:
        return len(self._molecules)

    @overload
    def __getitem__(self, item: int) -> Molecule: ...

    @overload
    def __getitem__(self, item: slice) -> tuple[Molecule, ...]: ...

    def __getitem__(self, item: int | slice) -> Molecule | tuple[Molecule, ...]:
        return self._molecules[item]

    def __iter__(self) -> Iterator[Molecule]:
        return iter(self._molecules)

    @property
    def molecules(self) -> tuple[Molecule, ...]:
        return self._molecules

    @property
    def name(self) -> str:
        return self._name

    def __repr__(self) -> str:
        return f"{type(self).__name__}(molecules={len(self)}, name={self.name!r})"
