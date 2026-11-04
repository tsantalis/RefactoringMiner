"""Utilities for working with [`typing.ForwardRef`][].

This module allows the developer to create and evaluate [`typing.ForwardRef`][] instances
with additional logic to support forwards compatibility.

Examples: Typical Usage
    >>> from typelib.py import refs
    >>> ref = refs.forwardref("str")
    >>> cls = refs.evaluate(ref)
    >>> cls is str
    True
"""

from __future__ import annotations

import functools
import inspect
import typing

from typelib.py import frames, inspection

__all__ = ("ForwardRef", "evaluate", "forwardref")

ForwardRef: typing.TypeAlias = typing.ForwardRef


def forwardref(
    ref: str | type,
    *,
    is_argument: bool = False,
    module: typing.Any | None = None,
    is_class: bool = True,
) -> ForwardRef:
    """Create a [`typing.ForwardRef`][] instance from a `ref` string.

    This wrapper function will attempt to determine the module name ahead of instantiation
    if not provided. This is important when resolving the reference to an actual type.

    Args:
        ref: The type reference string.
        is_argument: Whether the reference string was an argument to a function (default False).
        module: The python module in which the reference string is defined (optional)
        is_class: Whether the reference string is a class (default True).
    """
    if not isinstance(ref, str):
        name = inspection.qualname(ref)
        module = module or getattr(ref, "__module__", None)
    else:
        name = typing.cast(str, ref)

    module = _resolve_module_name(ref, module)
    if module is not None:
        name = name.replace(f"{module}.", "")

    return ForwardRef(
        name,
        is_argument=is_argument,
        module=module,
        is_class=is_class,
    )


if typing.TYPE_CHECKING:

    def evaluate(
        ref: ForwardRef,
        *,
        globalns: typing.Mapping[str, typing.Any] | None = None,
        localns: typing.Mapping[str, typing.Any] | None = None,
        recursive_guard: set | None = None,
    ) -> typing.Any: ...


else:

    def evaluate(
        ref: ForwardRef,
        *,
        globalns: typing.Mapping[str, typing.Any] | None = None,
        localns: typing.Mapping[str, typing.Any] | None = None,
        recursive_guard: set | None = None,
    ) -> typing.Any:
        """Evaluate the [`typing.ForwardRef`][] instance into a proper type.

        Note:
            Most of the time you will not need to provide anything but `ref`.

            In Python 3.9, we will automatically transform "new-style" type hints into
            runtime-compatible type hints:
              - Union-operator (`str | int` -> `typing.Union[str, int]`)
              - builtin generics (`dict[str, str]` -> `typing.Dict[str, str]`)

        Args:
            ref: The [`typing.ForwardRef`][] instance to evaluate.
            globalns: A mapping of global variable names to values (optional).
            localns: A mapping of local variable names to values (optional).
            recursive_guard: A set instance to prevent recursion during evaluation (optional).
        """
        if type(ref) is not ForwardRef:
            return ref
        if ref.__forward_evaluated__:
            return ref.__forward_value__

        recursive_guard = recursive_guard or set()
        return ref._evaluate(globalns, localns, recursive_guard=recursive_guard)


@functools.cache
def _resolve_module_name(ref: str, module: str | None) -> str | None:
    if module is not None:
        return module

    # Easy path, use the qualname if it's provided.
    module = ref.split(".", maxsplit=1)[0]
    if module != ref:
        return module
    # Harder path, find the actual object in the stack frame, if possible.
    obj = frames.extract(ref)
    module = getattr(obj, "__module__", None)
    if module:
        return module
    # Tricky path, get the caller and get the module name of the caller.
    caller = frames.getcaller()
    module = getattr(inspect.getmodule(caller), "__name__", None)
    return module
