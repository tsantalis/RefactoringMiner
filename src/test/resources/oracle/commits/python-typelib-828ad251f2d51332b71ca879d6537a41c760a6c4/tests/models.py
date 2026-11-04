from __future__ import annotations

import dataclasses
import datetime
import enum
import typing

from typelib.py import compat


@dataclasses.dataclass
class RecursiveType:
    cycle: RecursiveType | None = None


@dataclasses.dataclass
class IndirectCycleType:
    indirect: CycleType


@dataclasses.dataclass
class CycleType:
    cycle: IndirectCycleType | None = None


@dataclasses.dataclass
class Data:
    field: str
    value: int


class Vanilla:
    def __init__(self, field: str, value: int):
        self.field = field
        self.value = value

    def __eq__(self, other):
        return self.field == getattr(other, "field", ...) and self.value == getattr(
            other, "value", ...
        )


class VanillaWithHints(Vanilla):
    field: str
    value: int


class NTuple(typing.NamedTuple):
    field: str
    value: int


class TDict(typing.TypedDict):
    field: str
    value: int


IntVar = typing.TypeVar("IntVar", bound=int)
StrVar = typing.TypeVar("StrVar", bound=str)


@dataclasses.dataclass
class DataGeneric(typing.Generic[StrVar, IntVar]):
    field: StrVar
    value: IntVar


class GivenEnum(enum.Enum):
    one = "one"


@dataclasses.dataclass
class UnionSTDLib:
    timestamp: datetime.datetime | None = None
    date_time: datetime.datetime | None = None
    intstr: int | str = 0


@dataclasses.dataclass
class Parent:
    intersection: ParentIntersect
    child: Child


@dataclasses.dataclass
class Child:
    intersection: ChildIntersect


@dataclasses.dataclass
class ParentIntersect:
    a: int


@dataclasses.dataclass
class ChildIntersect:
    b: int


ListAlias = compat.TypeAliasType("ListAlias", list[int])


@dataclasses.dataclass
class NestedTypeAliasType:
    alias: ListAlias


ValueAlias = compat.TypeAliasType("ValueAlias", int)
RecursiveAlias = compat.TypeAliasType(
    "RecursiveAlias", "dict[str, RecursiveAlias | ValueAlias]"
)

ScalarValue = compat.TypeAliasType("ScalarValue", "int | float | str | bool | None")
Record = compat.TypeAliasType(
    "Record", "dict[str, list[Record] | list[ScalarValue] | Record | ScalarValue]"
)


TVar = typing.TypeVar("TVar", bound=int)


@dataclasses.dataclass
class SimpleGeneric(typing.Generic[TVar]):
    field: TVar


@dataclasses.dataclass
class NestedGeneric(typing.Generic[TVar]):
    gen: SimpleGeneric[TVar]
