"""High-performance, Fine-grained runtime type inspections.

Examples: Typical Usage
    >>> from typelib.py import inspection
    >>> inspection.ismappingtype(dict)
    True
    >>> inspection.isfixedtupletype(tuple[int, str])
    True

"""

from __future__ import annotations

import abc
import builtins
import collections
import contextlib
import dataclasses
import datetime
import decimal
import enum
import fractions
import functools
import inspect
import ipaddress
import numbers
import pathlib
import re
import sqlite3
import types
import typing as tp
import typing_extensions as te
import uuid
from collections.abc import Callable as abc_Callable
from operator import attrgetter

from typelib import constants
from typelib.py import compat, contrib, refs

__all__ = (
    "BUILTIN_TYPES",
    "args",
    "isabstract",
    "isbuiltininstance",
    "isbuiltinsubtype",
    "isbuiltintype",
    "isbytestype",
    "isclassvartype",
    "iscollectiontype",
    "isdatetype",
    "isdecimaltype",
    "isdescriptor",
    "isenumtype",
    "isfinal",
    "isfixedtupletype",
    "isforwardref",
    "isfromdictclass",
    "isfrozendataclass",
    "isgeneric",
    "ishashable",
    "isiterabletype",
    "isliteral",
    "ismappingtype",
    "isnamedtuple",
    "isoptionaltype",
    "isproperty",
    "issequencetype",
    "issimpleattribute",
    "isstdlibinstance",
    "isstdlibsubtype",
    "isstdlibtype",
    "isstringtype",
    "istexttype",
    "istimedeltatype",
    "istimetype",
    "istupletype",
    "istypeddict",
    "istypedtuple",
    "isuniontype",
    "isunresolvable",
    "isuuidtype",
    "name",
    "normalize_typevar",
    "origin",
    "qualname",
    "should_unwrap",
)


@compat.cache
def origin(annotation: tp.Any) -> tp.Any:
    """Get the highest-order 'origin'-type for a given type definition.

    Tip:
        For the purposes of this library, if we can resolve to a builtin type, we will.

    Examples:
        >>> from typelib.py import inspection
        >>> from typing import Dict, Mapping, NewType, Optional
        >>> origin(Dict)
        <class 'dict'>
        >>> origin(Mapping)
        <class 'dict'>
        >>> Registry = NewType('Registry', Dict)
        >>> origin(Registry)
        <class 'dict'>
        >>> class Foo: ...
        ...
        >>> origin(Foo)
        <class 'typelib.Foo'>
    """
    # Resolve custom NewTypes.
    actual = resolve_supertype(annotation)

    # Unwrap optional/classvar
    if isclassvartype(actual):
        a = args(actual)
        actual = a[0] if a else actual

    if istypealiastype(actual):
        actual = actual.__value__

    actual = tp.get_origin(actual) or actual

    # provide defaults for generics
    if not isbuiltintype(actual):
        actual = _check_generics(actual)

    if iscallable(actual):
        actual = tp.Callable

    return actual


def _check_generics(hint: tp.Any):
    return GENERIC_TYPE_MAP.get(hint, hint)


GENERIC_TYPE_MAP: dict[type, type] = {
    tp.Sequence: list,
    tp.MutableSequence: list,
    collections.abc.Sequence: list,
    collections.abc.MutableSequence: list,
    tp.Collection: list,
    collections.abc.Collection: list,
    tp.Iterable: list,
    collections.abc.Iterable: list,
    tp.AbstractSet: set,
    tp.MutableSet: set,
    collections.abc.Set: set,
    collections.abc.MutableSet: set,
    tp.Mapping: dict,
    tp.MutableMapping: dict,
    collections.abc.Mapping: dict,
    collections.abc.MutableMapping: dict,
    tp.Hashable: str,
    collections.abc.Hashable: str,
}


def args(annotation: tp.Any, *, evaluate: bool = False) -> tuple[tp.Any, ...]:
    """Get the args supplied to an annotation, normalizing [`typing.TypeVar`][].

    Note:
        TypeVar normalization follows this strategy:

            -> If the TypeVar is bound
            -----> return the bound type
            -> Else If the TypeVar has constraints
            -----> return a Union of the constraints
            -> Else
            -----> return Any

    Examples:
        >>> from typelib.py import inspection
        >>> from typing import Dict, TypeVar, Any
        >>> T = TypeVar("T")
        >>> args(Dict)
        ()
        >>> args(Dict[str, int])
        (<class 'str'>, <class 'int'>)
        >>> args(Dict[str, T])
        (<class 'str'>, typing.Any)
    """
    a = tp.get_args(annotation)
    if not a:
        a = getattr(annotation, "__args__", a)

    if evaluate:
        a = (*(refs.evaluate(r) for r in a),)

    return (*_normalize_typevars(*a),)


def _normalize_typevars(*args: tp.Any) -> tp.Iterable:
    yield from (normalize_typevar(t) if type(t) is tp.TypeVar else t for t in args)


@compat.cache
def normalize_typevar(tvar: tp.TypeVar) -> type[tp.Any]:
    """Reduce a TypeVar to a simple type.

    Note:
        TypeVar normalization follows this strategy:

            -> If the TypeVar is bound
            -----> return the bound type
            -> Else If the TypeVar has constraints
            -----> return a Union of the constraints
            -> Else
            -----> return Any
    """
    if tvar.__bound__:
        return tvar.__bound__
    elif tvar.__constraints__:
        return tp.Union[tvar.__constraints__]  # noqa: UP007  # type: ignore[return-value]
    return tp.Any  # type: ignore[return-value]


@compat.cache
def name(obj: type | refs.ForwardRef | tp.Callable) -> str:
    """Safely retrieve the name of either a standard object or a type annotation.

    Examples:
        >>> from typelib.py import inspection
        >>> from typing import Dict, Any, TypeVar
        >>> T = TypeVar("T")
        >>> name(Dict)
        'Dict'
        >>> name(Dict[str, str])
        'Dict'
        >>> name(Any)
        'Any'
        >>> name(dict)
        'dict'
    """
    strobj = qualname(obj)
    return strobj.rsplit(".")[-1]


@compat.cache
def qualname(obj: type | refs.ForwardRef | tp.Callable) -> str:
    """Safely retrieve the qualname of either a standard object or a type annotation.

    Examples:
        >>> from typelib.py import inspection
        >>> from typing import Dict, Any, TypeVar
        >>> T = TypeVar("T")
        >>> qualname(Dict)
        'typing.Dict'
        >>> qualname(Dict[str, str])
        'typing.Dict'
        >>> qualname(Any)
        'typing.Any'
        >>> qualname(dict)
        'dict'
    """
    strobj = str(obj)
    if isinstance(obj, refs.ForwardRef):
        strobj = str(obj.__forward_arg__)  # type: ignore[union-attr]
    is_generic = isgeneric(strobj)
    # We got a typing thing.
    if is_generic:
        # If this is a subscripted generic we should clean that up.
        return strobj.split("[", maxsplit=1)[0]
    # Easy-ish path, use name magix
    qname = getattr(obj, "__qualname__", None)
    nm = getattr(obj, "__name__", None)
    if qname is not None:
        return qname.replace("<locals>.", "")
    if nm is not None:  # pragma: no cover
        return nm
    return strobj


@compat.cache
def resolve_supertype(annotation: type[tp.Any] | types.FunctionType) -> tp.Any:
    """Get the highest-order supertype for a NewType.

    Examples:
        >>> from typelib.py import inspection
        >>> from typing import NewType
        >>> UserID = NewType("UserID", int)
        >>> AdminID = NewType("AdminID", UserID)
        >>> resolve_supertype(AdminID)
        <class 'int'>
    """
    while hasattr(annotation, "__supertype__"):
        annotation = annotation.__supertype__  # type: ignore[union-attr]
    return annotation


def signature(obj: tp.Callable[..., tp.Any] | type[tp.Any]) -> inspect.Signature:
    """Get the signature of a type or callable.

    Also supports TypedDict subclasses
    """
    if inspect.isclass(obj) or isgeneric(obj):
        if istypeddict(obj):
            return typed_dict_signature(obj)
        if istupletype(obj) and not isnamedtuple(obj):
            return tuple_signature(obj)
    return inspect.signature(obj)


cached_signature = compat.cache(signature)


def get_type_hints(
    obj: type | tp.Callable, exhaustive: bool = True
) -> dict[str, type[tp.Any] | tp.ForwardRef]:
    """Wrapper for [`typing.get_type_hints`][].

    If [`typing.get_type_hints`][] raises `([NameError][], [TypeError][])`, we will
    default to an empty dict.

    Args:
        obj: The object to inspect.
        exhaustive:
            Whether to pull type hints from the signature of the object if
            none can be found via [`typing.get_type_hints`][]. (defaults True)
    """
    tvar_to_type = {}
    try:
        t = obj
        if issubscriptedgeneric(t) and not isforwardref(t):
            t = obj.__origin__  # type: ignore[attr-defined,union-attr]
            tvar_to_type = dict(zip(obj.__parameters__, obj.__args__, strict=False))  # type: ignore[attr-defined,union-attr]
        hints = tp.get_type_hints(t)
    except (NameError, TypeError):
        hints = {}
    # KW_ONLY is a special sentinel to denote kw-only params in a dataclass.
    #  We don't want to do anything with this hint/field. It's not real.
    hints = {f: t for f, t in hints.items() if t is not compat.KW_ONLY}
    if not hints and exhaustive:
        hints = _hints_from_signature(obj)

    for name, annotation in hints.items():
        hints[name] = tvar_to_type.get(annotation, annotation)
    return hints


def _hints_from_signature(
    obj: type | tp.Callable,
) -> dict[str, type[tp.Any] | tp.ForwardRef]:
    try:
        params: dict[str, inspect.Parameter] = {**signature(obj).parameters}
    except (TypeError, ValueError):  # pragma: no cover
        return {}
    hints: dict[str, type[tp.Any] | tp.ForwardRef] = {}
    for name, param in params.items():
        annotation = param.annotation
        if annotation is param.empty:
            annotation = tp.Any
            hints[name] = annotation
            continue
        if annotation.__class__ is str:
            ref = refs.forwardref(
                annotation, is_argument=True, module=getattr(obj, "__module__", None)
            )
            hints[name] = ref
            continue
        hints[name] = annotation  # pragma: no cover
    return hints


cached_type_hints = compat.cache(get_type_hints)


def simple_attributes(t: type) -> tuple[str, ...]:
    """Extract all public, static data-attributes for a given type."""
    # If slots are defined, this is the best way to locate static attributes.
    if hasattr(t, "__slots__") and t.__slots__:
        return (
            *(
                f
                for f in t.__slots__
                if not f.startswith("_")
                # JIC - check if this is something fancy.
                and not isinstance(getattr(t, f, ...), _DYNAMIC_ATTRIBUTES)
            ),
        )
    # Otherwise we have to guess. This is inherently faulty, as attributes aren't
    #   always defined on a class before instantiation. The alternative is reverse
    #   engineering the constructor... yikes.
    return (
        *(
            x
            for x, y in inspect.getmembers(t, predicate=issimpleattribute)
            if not x.startswith("_") and not isinstance(y, _DYNAMIC_ATTRIBUTES)
        ),
    )


_DYNAMIC_ATTRIBUTES = (contrib.SQLAMetaData, contrib.sqla_registry)
cached_simple_attributes = compat.cache(simple_attributes)


def typed_dict_signature(obj: tp.Callable) -> inspect.Signature:
    """A little faker for getting the "signature" of a [`typing.TypedDict`][].

    Note:
        Technically, these are dicts at runtime, but we are enforcing a static shape,
        so we should be able to declare a matching signature for it.
    """
    hints = cached_type_hints(obj)
    total = getattr(obj, "__total__", True)
    default = inspect.Parameter.empty if total else ...
    return inspect.Signature(
        parameters=tuple(
            inspect.Parameter(
                name=x,
                kind=inspect.Parameter.KEYWORD_ONLY,
                annotation=y,
                default=getattr(obj, x, default),
            )
            for x, y in hints.items()
        )
    )


def tuple_signature(t: type[compat.TupleT]) -> inspect.Signature:
    """A little faker for getting the "signature" of a [`tuple`][].

    Note:
        At runtime, tuples are just tuples, but we can make use of their type hints to
        define a predictable signature.
    """
    a = args(t)
    if not a or a[-1] is ...:
        argt = tp.Any if not a else a[0]
        param = inspect.Parameter(
            name="args", kind=inspect.Parameter.VAR_POSITIONAL, annotation=argt
        )
        sig = inspect.Signature(parameters=(param,))
        return sig
    kind = inspect.Parameter.POSITIONAL_ONLY
    params = tuple(
        inspect.Parameter(name=f"arg{i!s}", kind=kind, annotation=at)
        for i, at in enumerate(a)
    )
    sig = inspect.Signature(parameters=params)
    return sig


@compat.cache
def safe_get_params(obj: type) -> tp.Mapping[str, inspect.Parameter]:
    """Try to extract the parameters of the given object.

    Return an empty mapping if we encounter an error.
    """
    params: tp.Mapping[str, inspect.Parameter]
    try:
        if ismappingtype(obj) and not istypeddict(obj):
            return {}
        params = cached_signature(obj).parameters
    except (ValueError, TypeError):  # pragma: nocover
        params = {}
    return params


@compat.cache
def isbuiltintype(
    obj: type | types.FunctionType,
) -> compat.TypeIs[type[BuiltIntypeT]]:
    """Check whether the provided object is a builtin-type.

    Note:
        Python stdlib and Python documentation have no "definitive list" of
        builtin-**types**, despite the fact that they are well-known. The closest we have
        is https://docs.python.org/3.7/library/functions.html, which clumps the
        builtin-types with builtin-functions. Despite clumping these types with functions
        in the documentation, these types eval as False when compared to
        [`types.BuiltinFunctionType`][], which is meant to be an alias for the
        builtin-functions listed in the documentation.

        All this to say, here we are with a custom check to determine whether a type is a
        builtin.

    Examples:
        >>> from typing import NewType, Mapping
        >>> isbuiltintype(str)
        True
        >>> isbuiltintype(NewType("MyStr", str))
        True
        >>> class Foo: ...
        ...
        >>> isbuiltintype(Foo)
        False
        >>> isbuiltintype(Mapping)
        False
    """
    return (
        resolve_supertype(obj) in BUILTIN_TYPES
        or resolve_supertype(type(obj)) in BUILTIN_TYPES
    )


@compat.cache
def isstdlibtype(obj: type) -> compat.TypeIs[type[STDLibtypeT]]:
    if isoptionaltype(obj):
        nargs = tp.get_args(obj)[:-1]
        return all(isstdlibtype(a) for a in nargs)
    if isuniontype(obj):
        args = tp.get_args(obj)
        return all(isstdlibtype(a) for a in args)

    return (
        resolve_supertype(obj) in STDLIB_TYPES
        or resolve_supertype(type(obj)) in STDLIB_TYPES
    )


@compat.cache
def isbuiltinsubtype(t: type) -> compat.TypeIs[type[BuiltIntypeT]]:
    """Check whether the provided type is a subclass of a builtin-type.

    Examples:
        >>> from typing import NewType, Mapping
        >>> class SuperStr(str): ...
        ...
        >>> isbuiltinsubtype(SuperStr)
        True
        >>> isbuiltinsubtype(NewType("MyStr", SuperStr))
        True
        >>> class Foo: ...
        ...
        >>> isbuiltintype(Foo)
        False
        >>> isbuiltintype(Mapping)
        False
    """
    return issubclass(resolve_supertype(t), BUILTIN_TYPES_TUPLE)


@compat.cache
def isstdlibsubtype(t: type) -> compat.TypeIs[type[STDLibtypeT]]:
    """Test whether the given type is a subclass of a standard-lib type.

    Examples:
        >>> import datetime

        >>> class MyDate(datetime.date): ...
        ...
        >>> isstdlibsubtype(MyDate)
        True
    """
    return _safe_issubclass(resolve_supertype(t), STDLIB_TYPES_TUPLE)


def isbuiltininstance(o: tp.Any) -> compat.TypeIs[BuiltIntypeT]:
    """Test whether an object is an instance of a builtin type.

    Examples:
        >>> isbuiltininstance("")
        True
    """
    return builtins.isinstance(o, BUILTIN_TYPES_TUPLE)


def isstdlibinstance(o: tp.Any) -> compat.TypeIs[STDLibtypeT]:
    """Test whether an object is an instance of a type in the standard-lib."""
    return builtins.isinstance(o, STDLIB_TYPES_TUPLE)


@compat.cache
def isoptionaltype[OT](obj: type[OT]) -> compat.TypeGuard[type[OT] | None]:
    """Test whether an annotation is [`typing.Optional`][], or can be treated as.

    [`typing.Optional`][] is an alias for `typing.Union[<T>, None]`, so both are
    "optional".

    Examples:
        >>> from typing import Optional, Union, Dict, Literal
        >>> isoptionaltype(Optional[str])
        True
        >>> isoptionaltype(Union[str, None])
        True
        >>> isoptionaltype(Literal["", None])
        True
        >>> isoptionaltype(Dict[str, None])
    False
    """
    args = getattr(obj, "__args__", ())
    tname = name(origin(obj))
    nullarg = next((a for a in args if a in (type(None), None)), ...)
    isoptional = tname == "Optional" or (
        nullarg is not ... and tname in ("Union", "UnionType", "Literal")
    )
    return isoptional


@compat.cache
def isuniontype(obj: type) -> compat.TypeIs[tp.Union]:
    n = name(origin(obj))
    return n in ("Union", "UnionType")


@compat.cache
def isfinal(obj: type) -> bool:
    """Test whether an annotation is [`typing.Final`][].

    Examples:
        >>> from typing import NewType
        >>> from typelib.py.compat import Final
        >>> isfinal(Final[str])
        True
        >>> isfinal(NewType("Foo", Final[str]))
        True
    """
    return origin(obj) is compat.Final


@compat.cache
def isliteral(obj) -> bool:
    """Test whether an annotation is [`typing.Literal`][].

    Examples:
        >>>
    """
    return origin(obj) is tp.Literal or (
        obj.__class__ is refs.ForwardRef and obj.__forward_arg__.startswith("Literal")
    )


@compat.cache
def isdatetype(
    obj: type,
) -> compat.TypeIs[type[datetime.datetime | datetime.date]]:
    """Test whether this annotation is a a date/datetime object.

    Examples:
        >>> import datetime
        >>> from typing import NewType
        >>> isdatetype(datetime.datetime)
        True
        >>> isdatetype(datetime.date)
        True
        >>> isdatetype(NewType("Foo", datetime.datetime))
        True
    """
    return builtins.issubclass(origin(obj), datetime.date)


@compat.cache
def isdatetimetype(
    obj: type,
) -> compat.TypeIs[type[datetime.datetime | datetime.date]]:
    """Test whether this annotation is a a date/datetime object.

    Examples:
        >>> import datetime
        >>> from typing import NewType
        >>> isdatetype(datetime.datetime)
        True
        >>> isdatetype(datetime.date)
        True
        >>> isdatetype(NewType("Foo", datetime.datetime))
        True
    """
    return builtins.issubclass(origin(obj), datetime.datetime)


@compat.cache
def istimetype(obj: type) -> compat.TypeIs[type[datetime.time]]:
    """Test whether this annotation is a a date/datetime object.

    Examples:
        >>> import datetime
        >>> from typing import NewType
        >>> istimetype(datetime.time)
        True
        >>> istimetype(NewType("Foo", datetime.time))
        True
    """
    return builtins.issubclass(origin(obj), datetime.time)


@compat.cache
def istimedeltatype(obj: type) -> compat.TypeIs[type[datetime.timedelta]]:
    """Test whether this annotation is a a date/datetime object.

    Examples:
        >>> import datetime
        >>> from typing import NewType
        >>> istimedeltatype(datetime.timedelta)
        True
        >>> istimedeltatype(NewType("Foo", datetime.timedelta))
        True
    """
    return builtins.issubclass(origin(obj), datetime.timedelta)


@compat.cache
def isdecimaltype(obj: type) -> compat.TypeIs[type[decimal.Decimal]]:
    """Test whether this annotation is a Decimal object.

    Examples:
        >>> import decimal
        >>> from typing import NewType
        >>> isdecimaltype(decimal.Decimal)
        True
        >>> isdecimaltype(NewType("Foo", decimal.Decimal))
        True
    """
    return builtins.issubclass(origin(obj), decimal.Decimal)


@compat.cache
def isfractiontype(obj: type) -> compat.TypeIs[type[fractions.Fraction]]:
    """Test whether this annotation is a Decimal object.

    Examples:
        >>> import fractions
        >>> from typing import NewType
        >>> isdecimaltype(fractions.Fraction)
        True
        >>> isdecimaltype(NewType("Foo", fractions.Fraction))
        True
    """
    return builtins.issubclass(origin(obj), fractions.Fraction)


@compat.cache
def isuuidtype(obj: type) -> compat.TypeIs[type[uuid.UUID]]:
    """Test whether this annotation is a a date/datetime object.

    Examples:
        >>> import uuid
        >>> from typing import NewType
        >>> isuuidtype(uuid.UUID)
        True
        >>> class MyUUID(uuid.UUID): ...
        ...
        >>> isuuidtype(MyUUID)
        True
        >>> isuuidtype(NewType("Foo", uuid.UUID))
        True
    """
    return builtins.issubclass(origin(obj), uuid.UUID)


@compat.cache
def isiterabletype(obj: type) -> compat.TypeIs[type[tp.Iterable]]:
    """Test whether the given type is iterable.

    Examples:
        >>> from typing import Sequence, Collection
        >>> isiterabletype(Sequence[str])
        True
        >>> isiterabletype(Collection)
        True
        >>> isiterabletype(str)
        True
        >>> isiterabletype(tuple)
        True
        >>> isiterabletype(int)
        False
    """
    obj = origin(obj)
    return builtins.issubclass(obj, tp.Iterable)


@compat.cache
def isiteratortype(obj: type) -> compat.TypeIs[type[tp.Iterator]]:
    """Check whether the given object is a subclass of an Iterator.

    Examples:
        >>> def mygen(): yield 1
        ...
        >>> isiteratortype(mygen().__class__)
        True
        >>> isiteratortype(iter([]).__class__)
        True
        >>> isiteratortype(mygen)
        False
        >>> isiteratortype(list)
        False
    """
    obj = origin(obj)
    return builtins.issubclass(obj, tp.Iterator)


@compat.cache
def istupletype(
    obj: tp.Callable[..., tp.Any] | type[tp.Any],
) -> compat.TypeIs[type[tuple]]:
    """Tests whether the given type is a subclass of [`tuple`][].

    Examples:
        >>> from typing import NamedTuple, Tuple
        >>> class MyTup(NamedTuple):
        ...     field: int
        ...
        >>> istupletype(tuple)
        True
        >>> istupletype(Tuple[str])
        True
        >>> istupletype(MyTup)
        True
    """
    obj = origin(obj)
    return obj is tuple or issubclass(obj, tuple)  # type: ignore[arg-type]


@compat.cache
def issequencetype(obj: type) -> compat.TypeIs[type[tp.Collection]]:
    """Test whether this annotation is a subclass of [`typing.Collection`][].

    Includes builtins.

    Examples:
        >>> from typing import Collection, Mapping, NewType, Sequence
        >>> issequencetype(Sequence)
        True
        >>> issequencetype(Mapping[str, str])
        True
        >>> issequencetype(str)
        True
        >>> issequencetype(list)
        True
        >>> issequencetype(NewType("Foo", dict))
        True
        >>> issequencetype(int)
        False
    """
    obj = origin(obj)
    return obj in _COLLECTIONS or builtins.issubclass(obj, tp.Sequence)


@compat.cache
def iscollectiontype(obj: type) -> compat.TypeIs[type[tp.Collection]]:
    """Test whether this annotation is a subclass of [`typing.Collection`][].

    Includes builtins.

    Examples:
        >>> from typing import Collection, Mapping, NewType
        >>> iscollectiontype(Collection)
        True
        >>> iscollectiontype(Mapping[str, str])
        True
        >>> iscollectiontype(str)
        True
        >>> iscollectiontype(list)
        True
        >>> iscollectiontype(NewType("Foo", dict))
        True
        >>> iscollectiontype(int)
        False
    """
    obj = origin(obj)
    return obj in _COLLECTIONS or builtins.issubclass(obj, tp.Collection)


_COLLECTIONS = {list, set, tuple, frozenset, dict, str, bytes}


@compat.cache
def issubscriptedcollectiontype[ArgsT](
    obj: type[tp.Generic[ArgsT]],  # type: ignore[valid-type]
) -> compat.TypeIs[type[tp.Collection[ArgsT]]]:
    """Test whether this annotation is a collection type and is subscripted.

    Examples:
        >>> from typing import Collection, Mapping, NewType
        >>> issubscriptedcollectiontype(Collection)
        False
        >>> issubscriptedcollectiontype(Mapping[str, str])
        True
        >>> issubscriptedcollectiontype(str)
        False
        >>> issubscriptedcollectiontype(NewType("Foo", Collection[int]))
        True
    """
    return iscollectiontype(obj) and issubscriptedgeneric(obj)


@compat.cache
def ismappingtype(obj: type) -> compat.TypeIs[type[tp.Mapping]]:
    """Test whether this annotation is a subtype of [`typing.Mapping`][].

    Examples:
        >>> from typing import Mapping, Dict, DefaultDict, NewType
        >>> ismappingtype(Mapping)
        True
        >>> ismappingtype(Dict[str, str])
        True
        >>> ismappingtype(DefaultDict)
        True
        >>> ismappingtype(dict)
        True
        >>> class MyDict(dict): ...
        ...
        >>> ismappingtype(MyDict)
        True
        >>> class MyMapping(Mapping): ...
        ...
        >>> ismappingtype(MyMapping)
        True
        >>> ismappingtype(NewType("Foo", dict))
        True
    """
    obj = origin(obj)
    return builtins.issubclass(obj, _MAPPING_TYPES) or builtins.issubclass(
        obj, tp.Mapping
    )


_MAPPING_TYPES = (dict, sqlite3.Row, types.MappingProxyType)


@compat.cache
def isenumtype(obj: type) -> compat.TypeIs[type[enum.Enum]]:
    """Test whether this annotation is a subclass of [`enum.Enum`][]

    Examples:
        >>> import enum
        >>>
        >>> class FooNum(enum.Enum): ...
        ...
        >>> isenumtype(FooNum)
        True
    """
    return _safe_issubclass(obj, enum.Enum)


@compat.cache
def isclassvartype(obj: type) -> bool:
    """Test whether an annotation is a ClassVar annotation.

    Examples:
        >>> from typing import ClassVar, NewType
        >>> isclassvartype(ClassVar[str])
        True
        >>> isclassvartype(NewType("Foo", ClassVar[str]))
        True
    """
    obj = resolve_supertype(obj)
    return getattr(obj, "__origin__", obj) is tp.ClassVar


_UNWRAPPABLE = (
    isclassvartype,
    isfinal,
)


@compat.cache
def should_unwrap(obj: type) -> bool:
    """Test whether we should use the __args__ attr for resolving the type.

    This is useful for determining what type to use at run-time for coercion.
    """
    return (not isliteral(obj)) and any(x(obj) for x in _UNWRAPPABLE)


@compat.cache
def isfromdictclass(obj: type) -> compat.TypeIs[type[_FromDict]]:
    """Test whether this annotation is a class with a `from_dict()` method."""
    return inspect.isclass(obj) and hasattr(obj, "from_dict")


class _FromDict(tp.Protocol):
    @classmethod
    def from_dict(cls: type[compat.Self], *args, **kwargs) -> compat.Self: ...


@compat.cache
def isfrozendataclass(obj: type) -> compat.TypeIs[type[_FrozenDataclass]]:
    """Test whether this is a dataclass and whether it's frozen."""
    return getattr(getattr(obj, "__dataclass_params__", None), "frozen", False)


class _FrozenDataclass(tp.Protocol):
    __dataclass_params__: dataclasses._DataclassParams  # type: ignore


cached_issubclass = compat.cache(issubclass)


__hashgetter = attrgetter("__hash__")


def ishashable(obj: tp.Any) -> compat.TypeIs[tp.Hashable]:
    """Check whether an object is hashable.

    An order of magnitude faster than [`isinstance`][] with
    [`typing.Hashable`][]

    Examples:
        >>> ishashable(str())
        True
        >>> ishashable(frozenset())
        True
        >>> ishashable(list())
        False
    """
    return __hashgetter(obj) is not None


@compat.cache
def istypeddict(obj: tp.Any) -> bool:
    """Check whether an object is a [`typing.TypedDict`][].

    Examples:
        >>> from typing import TypedDict
        >>>
        >>> class FooMap(TypedDict):
        ...     bar: str
        ...
        >>> istypeddict(FooMap)
        True
    """
    return (
        inspect.isclass(obj)
        and dict in {*inspect.getmro(obj)}
        and hasattr(obj, "__total__")
    )


@compat.cache
def istypedtuple(obj: type) -> compat.TypeIs[type[tp.NamedTuple]]:
    """Check whether an object is a "typed" tuple ([`typing.NamedTuple`][]).

    Examples:
        >>> from typing import NamedTuple
        >>>
        >>> class FooTup(NamedTuple):
        ...     bar: str
        ...
        >>> istypedtuple(FooTup)
        True
    """
    return (
        inspect.isclass(obj)
        and issubclass(obj, tuple)
        and bool(getattr(obj, "__annotations__", False))
    )


@compat.cache
def isnamedtuple(obj: type) -> compat.TypeIs[type[tp.NamedTuple]]:
    """Check whether an object is a "named" tuple ([`collections.namedtuple`][]).

    Examples:
        >>> from collections import namedtuple
        >>>
        >>> FooTup = namedtuple("FooTup", ["bar"])
        >>> isnamedtuple(FooTup)
        True
    """
    return inspect.isclass(obj) and issubclass(obj, tuple) and hasattr(obj, "_fields")


@compat.cache
def isfixedtupletype(obj: type) -> compat.TypeIs[type[tuple]]:
    """Check whether an object is a "fixed" tuple, e.g., `tuple[int, int]`.

    Examples:
        >>> from typing import Tuple
        >>>
        >>>
        >>> isfixedtupletype(Tuple[str, int])
        True
        >>> isfixedtupletype(Tuple[str, ...])
        False
    """
    a = args(obj)
    origin = tp.get_origin(obj)
    if not a or a[-1] is ...:
        return False
    return _safe_issubclass(origin, tuple)


def isforwardref(obj: tp.Any) -> compat.TypeIs[refs.ForwardRef]:
    """Tests whether the given object is a [`typing.ForwardRef`][]."""
    return obj.__class__ is refs.ForwardRef


def isproperty(obj) -> compat.TypeIs[types.DynamicClassAttribute]:
    """Test whether the given object is an instance of [`property`] or [`functools.cached_property`][].

    Examples:
        >>> import functools

        >>> class Foo:
        ...     @property
        ...     def prop(self) -> int:
        ...         return 1
        ...
        ...     @functools.cached_property
        ...     def cached(self) -> str:
        ...         return "foo"
        ...
        >>> isproperty(Foo.prop)
        True
        >>> isproperty(Foo.cached)
        True
    """

    return builtins.issubclass(obj.__class__, (property, functools.cached_property))


def isdescriptor(obj) -> compat.TypeIs[DescriptorT]:
    """Test whether the given object is a [`types.GetSetDescriptorType`][]

    Examples:
        >>> class StringDescriptor:
        ...     __slots__ = ("value",)
        ...
        ...     def __init__(self, default: str = "value"):
        ...         self.value = default
        ...
        ...     def __get__(self, instance: Any, value: str) -> str:
        ...         return self.value
        ...
        >>> isdescriptor(StringDescriptor)
        True
    """
    intersection = {*dir(obj)} & _DESCRIPTOR_METHODS
    return bool(intersection)


_DESCRIPTOR_METHODS = frozenset(("__get__", "__set__", "__delete__", "__set_name__"))


class GetDescriptor[VT](tp.Protocol):
    @tp.overload
    def __get__(self, instance: None, owner: tp.Any) -> GetDescriptor: ...

    @tp.overload
    def __get__(self, instance: object, owner: tp.Any) -> VT: ...

    def __get__(self, instance: tp.Any, owner: tp.Any) -> GetDescriptor | VT: ...


class SetDescriptor[VT](tp.Protocol):
    def __set__(self, instance: tp.Any, value: VT): ...


class DeleteDescriptor[VT](tp.Protocol):
    def __delete__(self, instance: tp.Any): ...


class SetNameDescriptor(tp.Protocol):
    def __set_name__(self, owner: tp.Any, name: str): ...


DescriptorT = tp.Union[  # noqa: UP007
    GetDescriptor, SetDescriptor, DeleteDescriptor, SetNameDescriptor
]


def issimpleattribute(v) -> bool:
    """Test whether the given object is a static value

    (e.g., not a function, class, or descriptor).

    Examples:
        >>> class MyOperator:
        ...     type = str
        ...
        ...     def operate(self, v) -> type:
        ...         return self.type(v)
        ...
        ...     @property
        ...     def default(self) -> type:
        ...         return self.type()
        ...
        >>> issimpleattribute(MyOperator.type)
        False
        >>> issimpleattribute(MyOperator.operate)
        False
        >>> issimpleattribute(MyOperator.default)
        False
    """
    return not any(c(v) for c in _ATTR_CHECKS)


_ATTR_CHECKS = (inspect.isclass, inspect.isroutine, isproperty, isdescriptor)


def isabstract(o) -> compat.TypeIs[abc.ABC]:
    """Test whether the given object is an abstract type.

    Examples:
        >>> import abc
        >>> import numbers

        >>>
        >>> isabstract(numbers.Number)
        True
        >>>
        >>> class MyABC(abc.ABC): ...
        ...
        >>> isabstract(MyABC)
        True

    """
    return inspect.isabstract(o) or o in _ABCS


# Custom list of ABCs which incorrectly evaluate to false
_ABCS = frozenset({numbers.Number})


@compat.cache
def istexttype(t: type[tp.Any]) -> compat.TypeIs[type[str | bytes | bytearray]]:
    """Test whether the given type is a subclass of text or bytes.

    Examples:
        >>> class MyStr(str): ...
        ...
        >>> istexttype(MyStr)
        True
    """
    return _safe_issubclass(t, (str, bytes, bytearray, memoryview))


@compat.cache
def isstringtype(t: type[tp.Any]) -> compat.TypeIs[type[str | bytes | bytearray]]:
    """Test whether the given type is a subclass of text or bytes.

    Examples:
        >>> class MyStr(str): ...
        ...
        >>> istexttype(MyStr)
        True
    """
    return _safe_issubclass(t, str)


@compat.cache
def isbytestype(t: type[tp.Any]) -> compat.TypeIs[type[str | bytes | bytearray]]:
    """Test whether the given type is a subclass of text or bytes.

    Examples:
        >>> class MyStr(str): ...
        ...
        >>> istexttype(MyStr)
        True
    """
    return _safe_issubclass(t, (bytes, bytearray, memoryview))


@compat.cache
def isnumbertype(t: type[tp.Any]) -> compat.TypeIs[type[numbers.Number]]:
    """Test whether `t` is a subclass of the [`numbers.Number`][] protocol.

    Examples:
        >>> import decimal

        >>> isnumbertype(int)
        True
        >>> isnumbertype(float)
        True
        >>> isnumbertype(decimal.Decimal)
        True
    """
    return _safe_issubclass(t, numbers.Number)


@compat.cache
def isintegertype(t: type[tp.Any]) -> compat.TypeIs[type[int]]:
    """Test whether `t` is a subclass of the [`numbers.Number`][] protocol.

    Examples:
        >>> import decimal

        >>> isnumbertype(int)
        True
        >>> isnumbertype(float)
        False
        >>> isnumbertype(decimal.Decimal)
        False
    """
    return _safe_issubclass(t, int)


@compat.cache
def isfloattype(t: type[tp.Any]) -> compat.TypeIs[type[float]]:
    """Test whether `t` is a subclass of the [`numbers.Number`][] protocol.

    Examples:
        >>> import decimal

        >>> isnumbertype(int)
        False
        >>> isnumbertype(float)
        True
        >>> isnumbertype(decimal.Decimal)
        False
    """
    return _safe_issubclass(t, float)


@compat.cache
def isstructuredtype(t: type[tp.Any]) -> bool:
    """Test whether the given type has a fixed set of fields.

    Examples:
        >>> import dataclasses
        >>> from typing import Tuple, NamedTuple, TypedDict, Union, Literal, Collection

        >>>
        >>> isstructuredtype(Tuple[str, int])
        True
        >>> isstructuredtype(class MyDict(TypedDict): ...)
        True
        >>> isstructuredtype(class MyTup(NamedTuple): ...)
        True
        >>> isstructuredtype(class MyClass: ...)
        True
        >>> isstructuredtype(Union[str, int])
        False
        >>> isstructuredtype(Literal[1, 2])
        False
        >>> isstructuredtype(tuple)
        False
        >>> isstructuredtype(Collection[str])
        False
    """
    return (
        isfixedtupletype(t)
        or isnamedtuple(t)
        or istypeddict(t)
        or (not isstdlibsubtype(origin(t)) and not isuniontype(t) and not isliteral(t))
    )


@compat.cache
def isgeneric(t: tp.Any) -> bool:
    """Test whether the given type is a typing generic.

    Examples:
        >>> from typing import Tuple, Generic, TypeVar

        >>>
        >>> isgeneric(Tuple)
        True
        >>> isgeneric(tuple)
        False
        >>> T = TypeVar("T")
        >>> class MyGeneric(Generic[T]): ...
        >>> isgeneric(MyGeneric[int])
        True
    """
    strobj = str(t)
    is_generic = (
        strobj.startswith(("typing.", "typing_extensions."))
        or "[" in strobj
        or _safe_issubclass(t, tp.Generic)  # type: ignore[arg-type]
    )
    return is_generic


@compat.cache
def issubscriptedgeneric(t: tp.Any) -> bool:
    """Test whether the given type is a typing generic.

    Examples:
        >>> from typing import Tuple, Generic, TypeVar

        >>>
        >>> isgeneric(Tuple)
        True
        >>> isgeneric(tuple)
        False
        >>> T = TypeVar("T")
        >>> class MyGeneric(Generic[T]): ...
        >>> isgeneric(MyGeneric[int])
        True
    """
    strobj = str(t)
    og = tp.get_origin(t) or t
    is_generic = isgeneric(og) or isgeneric(t)
    is_subscripted = "[" in strobj
    return is_generic and is_subscripted


@compat.cache  # type: ignore[arg-type]
def iscallable(t: tp.Any) -> compat.TypeIs[tp.Callable]:
    """Test whether the given type is a callable.

    Examples:
        >>> import typing
        >>> import collections.abc
        >>> iscallable(lambda: None)
        True
        >>> iscallable(typing.Callable)
        True
        >>> iscallable(collections.abc.Callable)
        True
        >>> iscallable(1)
        False
    """
    return inspect.isroutine(t) or t is tp.Callable or _safe_issubclass(t, abc_Callable)  # type: ignore[arg-type]


@compat.cache
def isunresolvable(t: tp.Any) -> bool:
    """Test whether the given type is unresolvable.

    Examples:
        >>> import typing
        >>> isunresolvable(int)
        False
        >>> isunresolvable(typ.Any)
        True
        >>> isunresolvable(...)
        True
    """
    return t in _UNRESOLVABLE


_UNRESOLVABLE = (
    object,
    tp.Any,
    re.Match,
    constants.empty,
    tp.Callable,
    abc_Callable,
    inspect.Parameter.empty,
    type(Ellipsis),
    Ellipsis,
)


@compat.cache
def isnonetype(t: tp.Any) -> compat.TypeIs[None]:
    """Detect if the given type is a [`types.NoneType`][].

    Examples:
        >>> isnonetype(None)
        True
        >>> isnonetype(type(None))
        True
        >>> isnonetype(1)
        False
    """
    return t in (None, type(None))


@compat.cache
def ispatterntype(t: tp.Any) -> compat.TypeIs[re.Pattern]:
    """Detect if the given type is a [`re.Pattern`][].

    Examples:
        >>> import re
        >>> ispatterntype(re.compile(r"^[a-z]+$"))
        True
        >>> ispatterntype(r"^[a-z]+$")
        False
    """
    return _safe_issubclass(t, re.Pattern)


@compat.cache
def ispathtype(t: tp.Any) -> compat.TypeIs[pathlib.Path]:
    """Detect if the given type is a [`pathlib.Path`][].

    Examples:
        >>> import pathlib
        >>> ispathtype(pathlib.Path.cwd())
        True
        >>> ispathtype(".")
        False
    """
    return _safe_issubclass(t, pathlib.PurePath)


@compat.cache
def istypealiastype(t: tp.Any) -> compat.TypeIs[compat.TypeAliasType]:
    """Detect if the given object is a [`typing.TypeAliasType`][].

    Examples:
        >>> type IntList = list[int]
        >>> istypealiastype(IntList)
        True
        >>> IntList = compat.TypeAliasType("IntList", list[int])
        >>> istypealiastype(IntList)
        True

    """
    return isinstance(t, _TYPE_ALIAS_TYPES)


# typing_extensions ships its own TypeAliasType before Python 3.14.
_TYPE_ALIAS_TYPES = (tp.TypeAliasType, te.TypeAliasType)


@compat.cache
def istypevartype(t: tp.Any) -> compat.TypeIs[compat.TypeVar]:
    """Detect if the given object is a [`typing.TypeAliasType`][].

    Examples:
        >>> type IntList = list[int]
        >>> istypealiastype(IntList)
        True
        >>> IntList = compat.TypeAliasType("IntList", list[int])
        >>> istypealiastype(IntList)
        True

    """
    return isinstance(t, tp.TypeVar)


@compat.cache
def unwrap(t: tp.Any) -> tp.Any:
    lt = None
    while lt is not t:
        if should_unwrap(t):
            lt = t
            t = t.__args__[0]
            continue
        if istypealiastype(t):
            tv = t.__value__
            if issubclass(type(tv), str):
                return refs.forwardref(tv, module=t.__module__)
            lt = t
            t = tv
            continue

        if istypevartype(t):
            lt = t
            if t.__bound__:
                t = t.__bound__
            elif t.__constraints__:
                t = tp.Union[t.__constraints__]  # noqa: UP007
            else:
                t = tp.Any
            continue

        if hasattr(t, "__supertype__"):
            lt = t
            t = t.__supertype__
            continue

        return t
    return t


def _safe_issubclass(
    cls: type | None, class_or_tuple: type | tuple[type, ...], /
) -> bool:
    with contextlib.suppress(TypeError):
        return issubclass(cls, class_or_tuple)  # type: ignore[arg-type]
    return False


# Here we are with a manually-defined set of builtin-types.
# This probably won't break anytime soon, but we shall see...
BuiltIntypeT = tp.Union[  # noqa: UP007
    int, bool, float, str, bytes, bytearray, list, set, frozenset, tuple, dict, None
]
BUILTIN_TYPES = frozenset(
    (type(None), *(t for t in BuiltIntypeT.__args__ if t is not None))  # type: ignore
)
BUILTIN_TYPES_TUPLE = tuple(BUILTIN_TYPES)
STDLibtypeT = tp.Union[  # noqa: UP007
    BuiltIntypeT,
    datetime.datetime,
    datetime.date,
    datetime.timedelta,
    datetime.time,
    decimal.Decimal,
    ipaddress.IPv4Address,
    ipaddress.IPv6Address,
    pathlib.Path,
    uuid.UUID,
    collections.defaultdict,
    collections.deque,
    types.MappingProxyType,
]
STDLIB_TYPES = frozenset(
    (type(None), *(t for t in STDLibtypeT.__args__ if t is not None))  # type: ignore
)
STDLIB_TYPES_TUPLE = tuple(STDLIB_TYPES)
