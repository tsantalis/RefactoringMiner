"""Private immutable ordered catalog support."""

from __future__ import annotations

from collections.abc import Iterator, Mapping
from types import MappingProxyType
from typing import Generic, Protocol, TypeVar


class _Identified(Protocol):
    @property
    def id(self) -> str: ...


T = TypeVar("T", bound=_Identified)


class _Catalog(Mapping[str, T], Generic[T]):
    """An immutable insertion-ordered mapping keyed by stable string ID."""

    __slots__ = ("_by_id", "_kind", "_values")

    _by_id: Mapping[str, T]
    _kind: str
    _values: tuple[T, ...]

    def __init__(self, values: tuple[T, ...], kind: str) -> None:
        ordered = tuple(values)
        by_id = {value.id: value for value in ordered}
        if len(by_id) != len(ordered):
            raise ValueError(f"{kind} IDs must be unique")
        object.__setattr__(self, "_values", ordered)
        object.__setattr__(self, "_by_id", MappingProxyType(by_id))
        object.__setattr__(self, "_kind", kind)

    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError(f"{type(self).__name__} is immutable")

    def __len__(self) -> int:
        return len(self._values)

    def __iter__(self) -> Iterator[str]:
        return iter(self._by_id)

    def __getitem__(self, key: str) -> T:
        if not isinstance(key, str):
            raise TypeError("catalog IDs must be strings")
        try:
            return self._by_id[key]
        except KeyError:
            raise KeyError(f"unknown {self._kind} ID: {key!r}") from None

    def __repr__(self) -> str:
        ids = tuple(self._by_id)
        preview = ids if len(ids) <= 5 else (*ids[:5], "...")
        return f"{type(self).__name__}(size={len(self)}, ids={preview!r})"
