"""Private NumPy normalization helpers."""

from __future__ import annotations

import numbers
from collections.abc import Iterable, Sequence
from operator import index as as_index
from typing import Any, SupportsIndex

import numpy as np
from numpy.typing import NDArray

from ._types import PortableId


def as_integer_array(
    value: Any, field: str, ndim: int, empty_1d_shape: tuple[int, ...] | None = None
) -> NDArray[np.int64]:
    try:
        array = np.asarray(value)
    except Exception as error:
        raise TypeError(f"{field} must be an integer array") from error

    if (
        array.ndim == 0
        and array.dtype.kind == "O"
        and isinstance(value, Iterable)
        and not isinstance(value, (str, bytes))
    ):
        try:
            array = np.asarray(tuple(value))
        except Exception as error:
            raise TypeError(f"{field} must be an integer array") from error

    if array.ndim != ndim:
        if empty_1d_shape is not None and array.ndim == 1 and array.size == 0:
            return np.empty(empty_1d_shape, dtype=np.int64)
        raise ValueError(f"{field} must have rank {ndim}, got {array.ndim}")
    if array.dtype.kind == "b":
        raise TypeError(f"{field} must contain integers, not booleans")
    if array.size == 0 and array.dtype.kind == "f":
        return np.empty(array.shape, dtype=np.int64)
    if array.dtype.kind == "O":
        if not all(
            isinstance(item, numbers.Integral) and not isinstance(item, (bool, np.bool))
            for item in array.flat
        ):
            raise TypeError(f"{field} must contain integers")
        try:
            return np.array(array, dtype=np.int64, order="C", copy=True)
        except (OverflowError, TypeError, ValueError) as error:
            raise ValueError(
                f"{field} contains values outside the supported integer range"
            ) from error
    if array.dtype.kind not in "iu":
        raise TypeError(f"{field} must contain integers")

    if array.dtype.kind == "u" and np.any(array > np.iinfo(np.int64).max):
        raise ValueError(f"{field} contains values outside the supported integer range")
    try:
        return np.array(array, dtype=np.int64, order="C", copy=True)
    except (OverflowError, ValueError) as error:
        raise ValueError(f"{field} contains values outside the supported integer range") from error


def as_coordinates(value: Any, atom_count: int) -> NDArray[np.float64]:
    if value is None:
        return np.empty((0, atom_count, 3), dtype=np.float64)
    try:
        array = np.asarray(value)
    except Exception as error:
        raise TypeError("coordinates must be a numeric array") from error
    if array.ndim == 2:
        if array.shape != (atom_count, 3):
            raise ValueError(f"coordinates must have shape ({atom_count}, 3)")
        canonical_shape = (1, atom_count, 3)
    elif array.ndim == 3:
        if array.shape[1:] != (atom_count, 3):
            raise ValueError(f"coordinates must have shape (C, {atom_count}, 3)")
        canonical_shape = array.shape
    else:
        raise ValueError("coordinates must have rank 2 or 3")
    if array.dtype.kind not in "iuf" or array.dtype.kind == "b":
        raise TypeError("coordinates must contain real numbers")
    try:
        canonical = np.array(array, dtype=np.float64, order="C", copy=True).reshape(canonical_shape)
    except (OverflowError, ValueError) as error:
        raise ValueError("coordinates contain values outside the supported range") from error
    return canonical


def as_names(value: Sequence[str] | None, count: int, field: str) -> tuple[str, ...]:
    if value is None:
        return ("",) * count
    if isinstance(value, (str, bytes)):
        raise TypeError(f"{field} must be a sequence of strings")
    try:
        result = tuple(value)
    except TypeError as error:
        raise TypeError(f"{field} must be a sequence of strings") from error
    if len(result) != count:
        raise ValueError(f"{field} must contain {count} values")
    if not all(isinstance(item, str) for item in result):
        raise TypeError(f"{field} must contain only strings")
    return result


def as_portable_id(value: str | SupportsIndex, field: str) -> PortableId:
    if isinstance(value, str):
        return value
    if isinstance(value, (bool, np.bool)):
        raise TypeError(f"{field} must be a string or integer")
    try:
        result = as_index(value)
    except TypeError as error:
        raise TypeError(f"{field} must be a string or integer") from error
    if result < np.iinfo(np.int64).min or result > np.iinfo(np.int64).max:
        raise ValueError(f"{field} is outside the portable integer range")
    return int(result)


def as_ids(value: Iterable[PortableId] | None, count: int, field: str) -> tuple[PortableId, ...]:
    if value is None:
        return tuple(range(count))
    if isinstance(value, (str, bytes)):
        raise TypeError(f"{field} must be a sequence of string or integer values")
    try:
        result = tuple(value)
    except TypeError as error:
        raise TypeError(f"{field} must be a sequence of string or integer values") from error
    if len(result) != count:
        raise ValueError(f"{field} must contain {count} values")
    return tuple(as_portable_id(item, field) for item in result)


def immutable_array[ScalarT: np.generic](array: NDArray[ScalarT]) -> NDArray[ScalarT]:
    """Copy an array into C-contiguous storage that cannot be made writable."""
    return np.frombuffer(array.tobytes(), dtype=array.dtype).reshape(array.shape)
