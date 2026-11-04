"""Vendored class decorators for dataclasses.

Notes:
    This module is unnecessary for Python versions >= 3.10.

Info: Typical Usage
    ```pycon
    >>> import dataclasses
    >>> from typelib.py import classes
    >>>
    >>> @classes.slotted
    >>> @dataclasses.dataclass
    >>> class Slotted:
    ...     attr: str
    ...
    >>> Slotted.__slots__
    ('attr',)
    ```
"""

from __future__ import annotations

import dataclasses
import sys
import warnings
from collections.abc import Callable, MutableSet
from typing import TypeVar, overload

from typelib import constants

_ClsT = TypeVar("_ClsT", bound=type)


@overload
def slotted(
    *, dict: bool = False, weakref: bool = True
) -> Callable[[_ClsT], _ClsT]: ...


@overload
def slotted(_cls: _ClsT, *, dict: bool = False, weakref: bool = True) -> _ClsT: ...


def slotted(  # noqa: C901
    _cls: _ClsT | None = None,
    *,
    dict: bool = False,
    weakref: bool = True,
) -> Callable[[_ClsT], _ClsT] | _ClsT:
    """Decorator to create a "slotted" version of the provided class.

    Args:
        _cls: The class to decorate.
        dict: Whether to add a slot for `__dict__`.
        weakref: Whether to add a slot for `__weakref__`.

    Warning:
        This function returns new class object as it's not possible to add `__slots__`
        after class creation.

    See Also:
        - [dataslots](https://github.com/starhel/dataslots/blob/master/src/dataslots/__init__.py)
    """

    def _slots_setstate(self, state):
        for param_dict in filter(None, state):
            for slot, value in param_dict.items():
                object.__setattr__(self, slot, value)

    def wrap(cls):
        key = repr(cls)
        if key in _stack:  # pragma: no cover
            raise TypeError(
                f"{cls!r} uses a custom metaclass {cls.__class__!r} "
                "which is not compatible with automatic slots. "
                "See Issue !typical#104 on GitHub for more information."
            ) from None

        _stack.add(key)

        if (
            sys.version_info >= (3, 10) and constants.PKG_NAME not in cls.__module__
        ):  # pragma: no cover
            warnings.warn(
                f"You are using Python {sys.version}. "
                "Python 3.10 introduced native support for slotted dataclasses. "
                "This is the preferred method for adding slots.",
                stacklevel=3,
            )

        cls_dict = {**cls.__dict__}
        # Create only missing slots
        inherited_slots = set().union(*(getattr(c, "__slots__", ()) for c in cls.mro()))

        field_names = {f.name: ... for f in dataclasses.fields(cls) if f.name}
        if dict:
            field_names["__dict__"] = ...
        if weakref:
            field_names["__weakref__"] = ...
        cls_dict["__slots__"] = (*(f for f in field_names if f not in inherited_slots),)

        # Erase filed names from class __dict__
        for f in field_names:
            cls_dict.pop(f, None)

        # Erase __dict__ and __weakref__
        cls_dict.pop("__dict__", None)
        cls_dict.pop("__weakref__", None)

        # Pickle fix for frozen dataclass as mentioned in https://bugs.python.org/issue36424
        # Use only if __getstate__ and __setstate__ are not declared and frozen=True
        if (
            all(param not in cls_dict for param in ["__getstate__", "__setstate__"])
            and cls.__dataclass_params__.frozen
        ):
            cls_dict["__setstate__"] = _slots_setstate

        # Prepare new class with slots
        new_cls = cls.__class__(cls.__name__, cls.__bases__, cls_dict)
        new_cls.__qualname__ = cls.__qualname__
        new_cls.__module__ = cls.__module__

        _stack.clear()
        return new_cls

    return wrap if _cls is None else wrap(_cls)


_stack: MutableSet[type] = set()
