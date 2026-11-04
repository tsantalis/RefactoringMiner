"""A simple hashmap for working with types in a contextual manner."""

from __future__ import annotations

import contextlib
import typing as tp
import typing_extensions as te

from typelib.py import inspection, refs

ValueT = tp.TypeVar("ValueT")
DefaultT = tp.TypeVar("DefaultT")
KeyT = te.TypeAliasType("KeyT", "type | refs.ForwardRef")


class TypeContext(dict[KeyT, ValueT], tp.Generic[ValueT]):
    """A key-value mapping which can map between forward references and real types."""

    def get(self, key: KeyT, default: ValueT | DefaultT = None) -> ValueT | DefaultT:  # type: ignore[assignment]
        with contextlib.suppress(KeyError):
            return self[key]

        return default

    def __missing__(self, key: type | refs.ForwardRef):
        """Hook to handle missing type references.

        Allows for sharing lookup results between forward references, type aliases, real types.

        Args:
            key: The type or reference.
        """
        # If we missed a ForwardRef, we've already tried this, bail out.
        if isinstance(key, refs.ForwardRef):
            raise KeyError(key)

        unwrapped = inspection.unwrap(key)
        if unwrapped in self:
            val = self[unwrapped]
            # Store the value at the original key to short-circuit in future
            self[key] = val
            return val

        ref = refs.forwardref(key)
        return self[ref]
