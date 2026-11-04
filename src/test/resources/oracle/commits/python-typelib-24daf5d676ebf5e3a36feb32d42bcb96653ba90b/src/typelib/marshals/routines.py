"""Type-specific logic for marshalling Python objects in to simple types for representation over-the-wire."""

from __future__ import annotations

import abc
import contextlib
import datetime
import decimal
import enum
import fractions
import pathlib
import re
import typing as tp
import uuid
import warnings

from typelib import ctx, serdes
from typelib.py import compat, inspection, refs

__all__ = (
    "AbstractMarshaller",
    "BytesMarshaller",
    "ContextT",
    "DateMarshaller",
    "DateTimeMarshaller",
    "DecimalMarshaller",
    "EnumMarshaller",
    "FixedTupleMarshaller",
    "FloatMarshaller",
    "FractionMarshaller",
    "IntegerMarshaller",
    "IterableMarshaller",
    "LiteralMarshaller",
    "MappingMarshaller",
    "PathMarshaller",
    "PatternMarshaller",
    "StringMarshaller",
    "StructuredTypeMarshaller",
    "SubscriptedIterableMarshaller",
    "SubscriptedMappingMarshaller",
    "TimeDeltaMarshaller",
    "TimeMarshaller",
    "UUIDMarshaller",
    "UnionMarshaller",
)


class AbstractMarshaller[T](abc.ABC):
    """Abstract base class defining the common interface for marshallers.

    Marshallers are custom callables which maintain type-specific information. They
    use this information to provide robust, performant logic reducing Python objects
    into their primitive representations for over-the-wire encoding.

    Marshallers support contextual serialization, which enables the marshalling of
    nested types.

    Attributes:
        t: The root type of this marshaller.
        origin: If `t` is a generic, this will be an actionable runtime type
                related to `t`, otherwise it is the same as `t`.
        context: The complete type context for this unmarshaller.
        var: If this unmarshaller is used in a nested context, this will reference the
             field/parameter/index at which this unmarshaller should be used.
    """

    t: type[T]
    origin: type[T]
    context: ContextT
    var: str | None

    __slots__ = ("context", "origin", "t", "var")

    def __repr__(self):
        return f"<{self.__class__.__name__}(type={self.t!r}, origin={self.origin!r}, var={self.var!r})>"

    def __init__(self, t: type[T], context: ContextT, *, var: str | None = None):
        """Construct a marshaller instance.

        Args:
            t: The root type of this marshaller.
            context: The complete type context for this marshaller.
            var: The associated field or parameter name for this unmarshaller (optional).
        """
        self.t = t
        self.origin = inspection.origin(self.t)
        self.context = context
        self.var = var

    @abc.abstractmethod
    def __call__(self, val: T) -> serdes.MarshalledValueT: ...


type ContextT = "ctx.TypeContext[AbstractMarshaller]"


class NoOpMarshaller[T](AbstractMarshaller[T]):
    """A marshaller that does nothing."""

    def __call__(self, val: T) -> serdes.MarshalledValueT:
        """Run the marshaller.

        Args:
            val: The value to marshal.
        """
        return val  # type: ignore[return-value]


BytesMarshaller = NoOpMarshaller[bytes]


class CastMarshaller[T](AbstractMarshaller[T]):
    """A marshaller that casts a value to a specific type."""

    def __call__(self, val: T) -> serdes.MarshalledValueT:
        """Marshal a the value into bound type.

        Args:
            val: The value to marshal.
        """
        cast = tp.cast("serdes.MarshalledValueT", self.origin(val))  # type: ignore[call-arg]
        return cast


IntegerMarshaller = CastMarshaller[int]
FloatMarshaller = CastMarshaller[float]


class ToStringMarshaller[T](AbstractMarshaller[T]):
    """A marshaller that converts a value to a string."""

    def __call__(self, val: T) -> str:
        """Marshal a value of the bound type into a string.

        Args:
            val: The value to marshal.
        """
        return str(val)


StringMarshaller = ToStringMarshaller[str]

DecimalT = tp.TypeVar("DecimalT", bound=decimal.Decimal)
DecimalMarshaller = ToStringMarshaller[DecimalT]

FractionT = tp.TypeVar("FractionT", bound=fractions.Fraction)
FractionMarshaller = ToStringMarshaller[FractionT]

UUIDT = tp.TypeVar("UUIDT", bound=uuid.UUID)
UUIDMarshaller = ToStringMarshaller[UUIDT]

PathT = tp.TypeVar("PathT", bound=pathlib.Path)
PathMarshaller = ToStringMarshaller[PathT]


class EnumMarshaller[EnumT: enum.Enum](AbstractMarshaller[EnumT]):
    """A marshaller that converts an [`enum.Enum`][] instance to its assigned value."""

    def __call__(self, val: EnumT) -> serdes.MarshalledValueT:
        """Marshal an [`enum.Enum`][] instance into a [`serdes.MarshalledValueT`][].

        Args:
            val: The enum instance to marshal.
        """
        return val.value


class PatternMarshaller[PatternT: re.Pattern](AbstractMarshaller[PatternT]):
    """A marshaller that converts a [`re.Pattern`][] to a string."""

    def __call__(self, val: PatternT) -> str:
        """Marshal a compiled regex pattern into a string.

        Args:
            val: The pattern to marshal.
        """
        return val.pattern


class ToISOTimeMarshaller[
    DateOrTimeT: (datetime.date, datetime.time, datetime.timedelta)
](AbstractMarshaller[DateOrTimeT]):
    """A marshaller that converts any date/time object to a ISO time string.

    See Also:
        - [`typelib.serdes.isoformat`]
    """

    def __call__(self, val: DateOrTimeT) -> str:
        """Marshal a date/time object into a ISO time string.

        Args:
            val: The date/time object to marshal.
        """
        return serdes.isoformat(val)


DateT = tp.TypeVar("DateT", bound=datetime.date)
DateMarshaller = ToISOTimeMarshaller[DateT]

DateTimeT = tp.TypeVar("DateTimeT", bound=datetime.datetime)
DateTimeMarshaller = ToISOTimeMarshaller[DateTimeT]

TimeT = tp.TypeVar("TimeT", bound=datetime.time)
TimeMarshaller = ToISOTimeMarshaller[TimeT]

TimeDeltaT = tp.TypeVar("TimeDeltaT", bound=datetime.timedelta)
TimeDeltaMarshaller = ToISOTimeMarshaller[TimeDeltaT]


class LiteralMarshaller[LiteralT](AbstractMarshaller[LiteralT]):
    """A marshaller that enforces the given value be one of the values in the defined [`typing.Literal`][]"""

    __slots__ = ("values",)

    def __init__(self, t: type[LiteralT], context: ContextT, *, var: str | None = None):
        """Constructor.

        Args:
            t: The Literal type to enforce.
            context: Nested type context (unused).
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        self.values = inspection.args(t)

    def __call__(self, val: LiteralT) -> serdes.MarshalledValueT:
        """Enforce the given value is a member of the bound `Literal` type.

        Args:
            val: The value to enforce.

        Raises:
            ValueError: If `val` is not a member of the bound `Literal` type.
        """
        if val in self.values:
            return val  # type: ignore[return-value]

        raise ValueError(f"{val!r} is not one of {self.values!r}")


class UnionMarshaller[UnionT](AbstractMarshaller[UnionT]):
    """A marshaller for dumping a given value via one of the types in the defined bound union.

    See Also:
        - [`UnionUnmarshaller`][typelib.unmarshals.routines.UnionUnmarshaller]
    """

    __slots__ = ("nullable", "ordered_routines", "stack")

    def __init__(self, t: type[UnionT], context: ContextT, *, var: str | None = None):
        """Constructor.

        Args:
            t: The type to unmarshal into.
            context: Any nested type context. Used to resolve the member marshallers.
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        self.stack = inspection.args(t, evaluate=True)
        self.nullable = inspection.isoptionaltype(t)
        self.ordered_routines = [self.context[typ] for typ in self.stack]

    def __call__(self, val: UnionT) -> serdes.MarshalledValueT:
        """Marshal a value into the bound `UnionT`.

        Args:
            val: The input value to unmarshal.

        Raises:
            ValueError: If `val` cannot be marshalled via any member type.
        """
        if self.nullable and val is None:
            return val

        for routine in self.ordered_routines:
            with contextlib.suppress(
                ValueError, TypeError, SyntaxError, AttributeError
            ):
                unmarshalled = routine(val)
                return unmarshalled

        raise ValueError(f"{val!r} is not one of types {self.stack!r}")


class MappingMarshaller[MappingT: tp.Mapping](AbstractMarshaller[MappingT]):
    """A marshaller for dumping any mapping into a simple [`dict`][]."""

    def __call__(self, val: MappingT) -> MarshalledMappingT:
        """Marshal a mapping into a simple [`dict`][].

        Args:
            val: The mapping object to marshal.
        """
        return {**val}


class IterableMarshaller[IterableT: tp.Iterable](AbstractMarshaller[IterableT]):
    """A marshaller for dumping any iterable into a simple [`list`][]."""

    def __call__(self, val: IterableT) -> MarshalledIterableT:
        """Marshal an iterable into a simple [`list`][].

        Args:
            val: The iterable to marshal.
        """
        return [*val]


class SubscriptedMappingMarshaller[MappingT: tp.Mapping](AbstractMarshaller[MappingT]):
    """A marshaller for dumping a subscripted mapping into a simple [`dict`][].

    Keys are marshalled according to the defined key-type, values according to the defined value-type.

    See Also:
        - [`SubscriptedMappingUnmarshaller`][typelib.unmarshals.routines.SubscriptedMappingUnmarshaller]
    """

    __slots__ = (
        "keys",
        "values",
    )

    def __init__(self, t: type[MappingT], context: ContextT, *, var: str | None = None):
        """Constructor.

        Args:
            t: The type to unmarshals from.
            context: Any nested type context. Used to resolve the member marshallers.
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        key_t, value_t = inspection.args(t, evaluate=True)
        self.keys = context[key_t]
        self.values = context[value_t]

    def __call__(self, val: MappingT) -> MarshalledMappingT:
        keys = self.keys
        values = self.values
        return {keys(k): values(v) for k, v in serdes.iteritems(val)}  # type: ignore[misc]


class SubscriptedIterableMarshaller[IterableT: tp.Iterable](
    AbstractMarshaller[IterableT]
):
    """A marshaller for dumping a subscripted iterable into a simple [`list`][].

    Values are marshalled according to the defined value-type.

    See Also:
        - [`SubscriptedIterableUnmarshaller`][typelib.unmarshals.routines.SubscriptedIterableUnmarshaller]
    """

    __slots__ = ("values",)

    def __init__(
        self, t: type[IterableT], context: ContextT, *, var: str | None = None
    ):
        """Constructor.

        Args:
            t: The type to unmarshals from.
            context: Any nested type context. Used to resolve the member marshallers.
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t=t, context=context, var=var)
        # supporting tuple[str, ...]
        (value_t, *_) = inspection.args(t, evaluate=True)
        self.values = context[value_t]

    def __call__(self, val: IterableT) -> MarshalledIterableT:
        """Marshal an iterable into a simple [`list`][].

        Args:
            val: The iterable to marshal.
        """
        # Always decode bytes.
        values = self.values
        return [values(v) for v in serdes.itervalues(val)]


class FixedTupleMarshaller(AbstractMarshaller[compat.TupleT]):
    """A marshaller for dumping a "fixed" tuple to a simple [`list`][].

    Values are marshalled according to the value-type in the order they are defined.

    See Also:
        - [`FixedTupleUnmarshaller`][typelib.unmarshals.routines.FixedTupleUnmarshaller]
    """

    __slots__ = ("ordered_routines", "stack")

    def __init__(
        self, t: type[compat.TupleT], context: ContextT, *, var: str | None = None
    ):
        """Constructor.

        Args:
            t: The type to unmarshal from.
            context: Any nested type context. Used to resolve the member marshallers.
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        self.stack = inspection.args(t, evaluate=True)
        self.ordered_routines = [self.context[vt] for vt in self.stack]

    def __call__(self, val: compat.TupleT) -> MarshalledIterableT:
        """Marshal a tuple into a simple [`list`][].

        Args:
            val: The tuple to marshal.
        """
        return [
            routine(v)
            for routine, v in zip(
                self.ordered_routines, serdes.itervalues(val), strict=False
            )
        ]


class StructuredTypeMarshaller[ST](AbstractMarshaller[ST]):
    """A marshaller for dumping a structured (user-defined) type to a simple [`dict`][].

    See Also:
        - [`StructuredTypeUnmarshaller`][typelib.unmarshals.routines.StructuredTypeUnmarshaller]
    """

    __slots__ = ("fields_by_var",)

    def __init__(self, t: type[ST], context: ContextT, *, var: str | None = None):
        """Constructor.

        Args:
            t: The type to unmarshals from.
            context: Any nested type context. Used to resolve the member marshallers.
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        self.fields_by_var = self._fields_by_var()

    def _fields_by_var(self):
        fields_by_var = {}
        hints = inspection.cached_type_hints(self.t)
        for name, hint in hints.items():
            resolved = refs.evaluate(hint)
            m = self.context.get(hint) or self.context.get(resolved)
            if m is None:
                warnings.warn(
                    "Failed to identify a marshaller for the associated type-variable pair: "
                    f"Original ref: {hint}, Resolved ref: {resolved}. Will default to no-op.",
                    stacklevel=5,
                )
                fields_by_var[name] = NoOpMarshaller(hint, self.context, var=name)
                continue

            fields_by_var[name] = m

        return fields_by_var

    def __call__(self, val: ST) -> MarshalledMappingT:
        """Marshal a structured type into a simple [`dict`][].

        Args:
            val: The structured type to marshal.
        """
        fields = self.fields_by_var
        return {f: fields[f](v) for f, v in serdes.iteritems(val) if f in fields}


type MarshalledMappingT = dict[serdes.PythonPrimitiveT, serdes.MarshalledValueT]
type MarshalledIterableT = list[serdes.MarshalledValueT]
