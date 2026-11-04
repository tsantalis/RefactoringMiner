"""Type-specific logic for unmarshalling simple Python objects into higher-order Python types."""

from __future__ import annotations

import abc
import contextlib
import datetime
import decimal
import enum
import fractions
import numbers
import pathlib
import re
import types
import typing as tp
import uuid
import warnings

from typelib import constants, ctx, serdes
from typelib.py import compat, inspection, refs

T = tp.TypeVar("T")

__all__ = (
    "AbstractUnmarshaller",
    "BytesUnmarshaller",
    "CastUnmarshaller",
    "ContextT",
    "DateTimeUnmarshaller",
    "DateUnmarshaller",
    "DecimalUnmarshaller",
    "EnumUnmarshaller",
    "FixedTupleUnmarshaller",
    "FractionUnmarshaller",
    "IterableUnmarshaller",
    "LiteralUnmarshaller",
    "MappingUnmarshaller",
    "NoOpUnmarshaller",
    "NoneTypeUnmarshaller",
    "NumberUnmarshaller",
    "PathUnmarshaller",
    "PatternUnmarshaller",
    "StringUnmarshaller",
    "StructuredTypeUnmarshaller",
    "SubscriptedIterableUnmarshaller",
    "SubscriptedIteratorUnmarshaller",
    "SubscriptedMappingUnmarshaller",
    "TimeDeltaUnmarshaller",
    "TimeUnmarshaller",
    "UUIDUnmarshaller",
    "UnionUnmarshaller",
    "noop",
)


class AbstractUnmarshaller(abc.ABC, tp.Generic[T]):
    """Abstract base class defining the common interface for unmarshallers.

    Unmarshallers are custom callables which maintain type-specific information. They
    use this information to provide robust, performant logic for decoding and converting
    primtive Python objects or JSON-endcoded data into their target type.

    Unmarshallers support contextual deserialization, which enables the unmarshalling of
    nested types.

    Attributes:
        t: The root type of this unmarshaller.
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
        """Construct an unmarshaller instance.

        Args:
            t: The root type of this unmarshaller.
            context: The complete type context for this unmarshaller.
            var: The associated field or parameter name for this unmarshaller (optional).
        """
        self.t = t
        self.origin = inspection.origin(self.t)
        self.context = context
        self.var = var

    @abc.abstractmethod
    def __call__(self, val: tp.Any) -> T:
        """Unmarshall a Python object into its target type.

        Not implemented for the abstract base class.
        """


class NoOpUnmarshaller(AbstractUnmarshaller[T]):
    """Unmarshaller that does nothing."""

    def __call__(self, val: tp.Any) -> T:
        return tp.cast(T, val)


def noop() -> NoOpUnmarshaller:
    return NoOpUnmarshaller(type, ctx.TypeContext())


class NoneTypeUnmarshaller(AbstractUnmarshaller[None]):
    """Unmarshaller for null values.

    Note:
        We will attempt to decode any string/bytes input before evaluating for `None`.

    See Also:
        - [`typelib.serdes.decode`][]
    """

    def __call__(self, val: tp.Any) -> None:
        """Unmarshal the given input into a `None` value.

        Args:
            val: The value to unmarshal.

        Raises:
            ValueError: If `val` is not `None` after decoding.
        """
        decoded = serdes.decode(val)
        if decoded is not None:
            raise ValueError(f"{val!r} is not of {types.NoneType!r}")


ContextT: tp.TypeAlias = "ctx.TypeContext[AbstractUnmarshaller]"
BytesT = tp.TypeVar("BytesT", bound=bytes)


class BytesUnmarshaller(AbstractUnmarshaller[BytesT], tp.Generic[BytesT]):
    """Unmarshaller that encodes an input to bytes.

    Note:
        We will format a member of the `datetime` module into ISO format before converting to bytes.

    See Also:
        - [`typelib.serdes.isoformat`][]
    """

    def __call__(self, val: tp.Any) -> BytesT:
        if isinstance(val, self.t):
            return val
        # Always encode date/time as ISO strings.
        if isinstance(val, (datetime.date, datetime.time, datetime.timedelta)):
            val = serdes.isoformat(val)
        return self.t(str(val).encode(constants.DEFAULT_ENCODING))


StringT = tp.TypeVar("StringT", bound=str)


class StringUnmarshaller(AbstractUnmarshaller[StringT], tp.Generic[StringT]):
    """Unmarshaller that converts an input to a string.

    Note:
        We will format a member of the `datetime` module into ISO format.

    See Also:
        - [`typelib.serdes.isoformat`][]
    """

    def __call__(self, val: tp.Any) -> StringT:
        # Always decode bytes.
        decoded = serdes.decode(val)
        if isinstance(decoded, self.t):
            return decoded
        # Always encode date/time as ISO strings.
        if isinstance(val, (datetime.date, datetime.time, datetime.timedelta)):
            decoded = serdes.isoformat(val)
        return self.t(decoded)


NumberT = tp.TypeVar("NumberT", bound=numbers.Number)


class NumberUnmarshaller(AbstractUnmarshaller[NumberT], tp.Generic[NumberT]):
    """Unmarshaller that converts an input to a number.

    Note:
        Number unmarshalling follows a best-effort strategy. We may extend type resolution
        to support more advanced type unmarshalling strategies in the future.

        As of now:
            1. Attempt to decode any bytes/string input into a real Python value.
            2. If the input is a member of the `datetime` module, convert it to a number.
            3. If the input is a mapping, unpack it into the number constructor.
            4. If the input is an iterable, unpack it into the number constructor.
            5. Otherwise, call the number constructor with the input.

    See Also:
        - [`typelib.serdes.unixtime`][]
    """

    def __call__(self, val: tp.Any) -> NumberT:
        """Unmarshall a value into the bound Number type.

        Args:
            val: The input value to unmarshal.
        """
        # Always decode bytes.
        decoded = serdes.decode(val)
        if isinstance(decoded, self.t):
            return decoded
        # Represent date/time objects as time since unix epoch.
        if isinstance(val, (datetime.date, datetime.time, datetime.timedelta)):
            decoded = serdes.unixtime(val)
        # Treat containers as constructor args.
        if inspection.ismappingtype(decoded.__class__):
            return self.t(**decoded)
        if inspection.isiterabletype(decoded.__class__) and not inspection.istexttype(
            decoded.__class__
        ):
            return self.t(*decoded)
        # Simple cast for non-containers.
        return self.t(decoded)  # type: ignore[call-arg]


DecimalT = tp.TypeVar("DecimalT", bound=decimal.Decimal)
DecimalUnmarshaller = NumberUnmarshaller[DecimalT]

FractionT = tp.TypeVar("FractionT", bound=fractions.Fraction)
FractionUnmarshaller = NumberUnmarshaller[FractionT]


DateT = tp.TypeVar("DateT", bound=datetime.date)


class DateUnmarshaller(AbstractUnmarshaller[DateT], tp.Generic[DateT]):
    """Unmarshaller that converts an input to a [`datetime.date`][] (or subclasses).

    Notes:
        This class tries to handle the 90% case:

        1. If we are already a [`datetime.date`][] instance, return it.
        2. If we are a `float` or `int` instance, treat it as a unix timestamp, at UTC.
        3. Attempt to decode any bytes/string input into a real Python value.
        4. If we have a string value, parse it into either a [`datetime.date`][]
        5. If the parsed result is a [`datetime.time`][] instance, then return
           the result of [`datetime.datetime.now`][], at UTC, as a [`datetime.date`][].

    Tip: TL;DR
        There are many ways to represent a date object over-the-wire. Your most
        fool-proof method is to rely upon [ISO 8601][iso] or [RFC 3339][rfc].

        [iso]: https://en.wikipedia.org/wiki/ISO_8601
        [rfc]: https://tools.ietf.org/html/rfc3339

    See Also:
        - [`typelib.serdes.decode`][]
        - [`typelib.serdes.dateparse`][]

    """

    def __call__(self, val: tp.Any) -> DateT:
        """Unmarshal a value into the bound `DateT` type.

        Args:
            val: The input value to unmarshal.
        """
        if isinstance(val, self.t) and not isinstance(val, datetime.datetime):
            return val

        # Numbers can be treated as time since epoch.
        if isinstance(val, (int, float)):
            val = datetime.datetime.fromtimestamp(val, tz=datetime.UTC)
        # Always decode bytes.
        decoded = serdes.decode(val)
        # Parse strings.
        date: datetime.date | datetime.time = (
            serdes.dateparse(decoded, self.t) if isinstance(decoded, str) else decoded
        )
        # Time-only construct is treated as today.
        if isinstance(date, datetime.time):
            date = datetime.datetime.now(tz=datetime.UTC).today()
        # Exact class matching - the parser returns subclasses.
        if date.__class__ is self.t:
            return date  # type: ignore[return-value]
        # Reconstruct as the exact type.
        return self.t(year=date.year, month=date.month, day=date.day)


DateTimeT = tp.TypeVar("DateTimeT", bound=datetime.datetime)


class DateTimeUnmarshaller(
    AbstractUnmarshaller[datetime.datetime], tp.Generic[DateTimeT]
):
    """Unmarshaller that converts an input to a [`datetime.datetime`][] (or subclasses).

    Notes:
        This class tries to handle the 90% case:

        1. If we are already a [`datetime.datetime`][] instance, return it.
        2. If we are a `float` or `int` instance, treat it as a unix timestamp, at UTC.
        3. Attempt to decode any bytes/string input into a real Python value.
        4. If we have a string value, parse it into either a [`datetime.date`][]
           instance, a [`datetime.time`][] instance or a [`datetime.datetime`][].
        5. If the parsed result is a [`datetime.time`][] instance, then merge
           the parsed time with today, at the timezone specified in the time instance.
        6. If the parsed result is a [`datetime.date`][] instance, create a
           [`datetime.datetime`][] instance at midnight of the indicated date, UTC.

    Tip: TL;DR
        There are many ways to represent a datetime object over-the-wire. Your most
        fool-proof method is to rely upon [ISO 8601][iso] or [RFC 3339][rfc].

        [iso]: https://en.wikipedia.org/wiki/ISO_8601
        [rfc]: https://tools.ietf.org/html/rfc3339


    See Also:
        - [`typelib.serdes.decode`][]
        - [`typelib.serdes.dateparse`][]


    """

    def __call__(self, val: tp.Any) -> datetime.datetime:
        """Unmarshal a value into the bound `DateTimeT` type.

        Args:
            val: The input value to unmarshal.
        """
        if isinstance(val, self.t):
            return val

        # Numbers can be treated as time since epoch.
        if isinstance(val, (int, float)):
            val = datetime.datetime.fromtimestamp(val, tz=datetime.UTC)
        # Always decode bytes.
        decoded = serdes.decode(val)
        # Parse strings.
        dt: datetime.datetime | datetime.date | datetime.time = (
            serdes.dateparse(decoded, self.t) if isinstance(decoded, str) else decoded
        )
        # If we have a time object, default to today.
        if isinstance(dt, datetime.time):
            return self.t.now(tz=dt.tzinfo).replace(
                hour=dt.hour,
                minute=dt.minute,
                second=dt.second,
                microsecond=dt.microsecond,
                tzinfo=dt.tzinfo,
            )
        # Exact class matching.
        if dt.__class__ is self.t:
            return dt  # type: ignore[return-value]
        # Subclass check for datetimes.
        if isinstance(dt, datetime.datetime):
            return self.t(
                year=dt.year,
                month=dt.month,
                day=dt.day,
                hour=dt.hour,
                minute=dt.minute,
                second=dt.second,
                microsecond=dt.microsecond,
                tzinfo=dt.tzinfo,
                fold=dt.fold,
            )
        # Implicit: we have a date object.
        return self.t(year=dt.year, month=dt.month, day=dt.day, tzinfo=datetime.UTC)


TimeT = tp.TypeVar("TimeT", bound=datetime.time)


class TimeUnmarshaller(AbstractUnmarshaller[TimeT], tp.Generic[TimeT]):
    """Unmarshaller that converts an input to a[`datetime.time`][] (or subclasses).

    Notes:
        This class tries to handle the 90% case:

        1. If we are already a [`datetime.time`][] instance, return it.
        2. If we are a `float` or `int` instance, treat it as a unix timestamp, at UTC.
        3. Attempt to decode any bytes/string input into a real Python value.
        4. If we have a string value, parse it into either a [`datetime.date`][]
           instance, a [`datetime.time`][] instance or a [`datetime.datetime`][].
        5. If the parsed result is a [`datetime.datetime`][] instance, then
           extract the time portion, preserving the associated timezone.
        6. If the parsed result is a [`datetime.date`][] instance, create
           a time instance at midnight, UTC.

    Tip: TL;DR
        There are many ways to represent a time object over-the-wire. Your most
        fool-proof method is to rely upon [ISO 8601][iso] or [RFC 3339][rfc].

        [iso]: https://en.wikipedia.org/wiki/ISO_8601
        [rfc]: https://tools.ietf.org/html/rfc3339

    See Also:
        - [`typelib.serdes.decode`][]
        - [`typelib.serdes.dateparse`][]

    """

    def __call__(self, val: tp.Any) -> TimeT:
        """Unmarshal a value into the bound `TimeT` type.

        Args:
            val: The input value to unmarshal.
        """
        if isinstance(val, self.t):
            return val

        decoded = serdes.decode(val)
        if isinstance(decoded, (int, float)):
            decoded = (
                datetime.datetime.fromtimestamp(val, tz=datetime.UTC)
                .time()
                # datetime.time() strips tzinfo...
                .replace(tzinfo=datetime.UTC)
            )
        dt: datetime.datetime | datetime.date | datetime.time = (
            serdes.dateparse(decoded, self.t) if isinstance(decoded, str) else decoded
        )

        if isinstance(dt, datetime.datetime):
            # datetime.time() strips tzinfo...
            dt = dt.time().replace(tzinfo=dt.tzinfo)
        elif isinstance(dt, datetime.date):
            dt = self.t(tzinfo=datetime.UTC)

        if dt.__class__ is self.t:
            return dt  # type: ignore[return-value]

        return self.t(
            hour=dt.hour,
            minute=dt.minute,
            second=dt.second,
            microsecond=dt.microsecond,
            tzinfo=dt.tzinfo,
            fold=dt.fold,
        )


TimeDeltaT = tp.TypeVar("TimeDeltaT", bound=datetime.timedelta)


class TimeDeltaUnmarshaller(AbstractUnmarshaller[TimeDeltaT], tp.Generic[TimeDeltaT]):
    """Unmarshaller that converts an input to a [`datetime.timedelta`][] (or subclasses).

    Notes:
        This class tries to handle the 90% case:

        1. If we are already a [`datetime.timedelta`][] instance, return it.
        2. If we are a `float` or `int` instance, treat it as total seconds for a delta.
        3. Attempt to decode any bytes/string input into a real Python value.
        4. If we have a string value, parse it into a [`datetime.timedelta`][] instance.
        5. If the parsed result is not *exactly* the bound `TimeDeltaT` type, convert it.


    Tips: TL;DR
        There are many ways to represent a time object over-the-wire. Your most
        fool-proof method is to rely upon [ISO 8601][iso] or [RFC 3339][rfc].

        [iso]: https://en.wikipedia.org/wiki/ISO_8601
        [rfc]: https://tools.ietf.org/html/rfc3339


    See Also:
        - [`typelib.serdes.decode`][]
        - [`typelib.serdes.dateparse`][]

    """

    def __call__(self, val: tp.Any) -> TimeDeltaT:
        """Unmarshal a value into the bound `TimeDeltaT` type.

        Args:
            val: The input value to unmarshal.
        """
        if isinstance(val, (int, float)):
            return self.t(seconds=int(val))

        decoded = serdes.decode(val)
        td: datetime.timedelta = (
            serdes.dateparse(decoded, t=datetime.timedelta)
            if isinstance(decoded, str)
            else decoded
        )

        if td.__class__ is self.t:
            return td  # type: ignore[return-value]

        return self.t(seconds=td.total_seconds())


UUIDT = tp.TypeVar("UUIDT", bound=uuid.UUID)


class UUIDUnmarshaller(AbstractUnmarshaller[UUIDT], tp.Generic[UUIDT]):
    """Unmarshaller that converts an input to a [`uuid.UUID`][] (or subclasses).

    Note:
        The resolution algorithm is intentionally simple:

        1. Attempt to decode any bytes/string input into a real Python object.
        2. If the value is an integer, pass it into the constructor via the `int=` param.
        3. Otherwise, pass into the constructor directly.

    Tip:
        While the [`uuid.UUID`][] constructor supports many different keyword
        inputs for different types of UUID formats/encodings, we don't have a great
        method for detecting the correct input. We have moved with the assumption that
        the two most common formats are a standard string encoding, or an integer encoding.
    """

    def __call__(self, val: tp.Any) -> UUIDT:
        """Unmarshal a value into the bound `UUIDT` type.

        Args:
            val: The input value to unmarshal.

        See Also:
            - [`typelib.serdes.load`][]
        """
        decoded = serdes.load(val)
        if isinstance(decoded, int):
            return self.t(int=decoded)
        if isinstance(decoded, self.t):
            return decoded
        return self.t(decoded)  # type: ignore[arg-type]


PatternT = tp.TypeVar("PatternT", bound=re.Pattern)


class PatternUnmarshaller(AbstractUnmarshaller[PatternT], tp.Generic[PatternT]):
    """Unmarshaller that converts an input to a [`re.Pattern`][].

    Note:
        You can't instantiate a [`re.Pattern`][] directly, so we don't have a good
        method for handling patterns from a different library out-of-the-box. We simply call
        `re.compile()` on the decoded input.

    See Also:
        - [`typelib.serdes.decode`][]
    """

    def __call__(self, val: tp.Any) -> PatternT:
        decoded = serdes.decode(val)
        return re.compile(decoded)  # type: ignore[return-value]


class CastUnmarshaller(AbstractUnmarshaller[T]):
    """Unmarshaller that converts an input to an instance of `T` with a direct cast.

    Note:
        Before casting to the bound type, we will attempt to decode the value into a
        real Python object.

    See Also:
        - [`typelib.serdes.load`][]
    """

    __slots__ = ("caster",)

    def __init__(self, t: type[T], context: ContextT, *, var: str | None = None):
        """Constructor.

        Args:
            t: The type to unmarshal into.
            context: Any nested type context (unused).
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        self.caster: tp.Callable[[tp.Any], T] = self.origin  # type: ignore[assignment]

    def __call__(self, val: tp.Any) -> T:
        """Unmarshal a value into the bound `T` type.

        Args:
            val: The input value to unmarshal.
        """
        # Try to load the string, if this is JSON or a literal expression.
        decoded = serdes.load(val)
        # Short-circuit cast if we have the type we want.
        if isinstance(decoded, self.t):
            return decoded
        # Cast the decoded value to the type.
        return self.caster(decoded)


PathUnmarshaller = CastUnmarshaller[pathlib.Path]
MappingUnmarshaller = CastUnmarshaller[tp.Mapping]
IterableUnmarshaller = CastUnmarshaller[tp.Iterable]

EnumT = tp.TypeVar("EnumT", bound=enum.Enum)
EnumUnmarshaller = CastUnmarshaller[EnumT]


LiteralT = tp.TypeVar("LiteralT")


class LiteralUnmarshaller(AbstractUnmarshaller[LiteralT], tp.Generic[LiteralT]):
    """Unmarshaller that will enforce an input conform to a defined [`typing.Literal`][].

    Note:
        We will attempt to decode the value into a real Python object if the input
        fails initial membership evaluation.

    See Also:
        - [`typelib.serdes.load`][]
    """

    __slots__ = ("values",)

    def __init__(self, t: type[LiteralT], context: ContextT, *, var: str | None = None):
        """Constructor.

        Args:
            t: The type to unmarshal into.
            context: Any nested type context (unused).
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        self.values = inspection.args(t, evaluate=True)

    def __call__(self, val: tp.Any) -> LiteralT:
        if val in self.values:
            return val
        decoded = serdes.load(val)
        if decoded in self.values:
            return decoded  # type: ignore[return-value]

        raise ValueError(f"{decoded!r} is not one of {self.values!r}")


UnionT = tp.TypeVar("UnionT")


class UnionUnmarshaller(AbstractUnmarshaller[UnionT], tp.Generic[UnionT]):
    """Unmarshaller that will convert an input to one of the types defined in a [`typing.Union`][].

    Note:
        Union deserialization is messy and violates a static type-checking mechanism -
        for static type-checkers, `str | int` is equivalent to `int | str`. This breaks
        down during unmarshalling for the simple fact that casting something to `str`
        will always succeed, so we would never actually unmarshal the input it an `int`,
        even if that is the "correct" result.

        Our algorithm is intentionally simple:

        1. We iterate through each union member from top to bottom and call the
           resolved unmarshaller, returning the result.
        2. If any of `(ValueError, TypeError, SyntaxError)`, try again with the
           next unmarshaller.
        3. If all unmarshallers fail, then we have an invalid input, raise an error.

    Tip: TL;DR
        In order to ensure correctness, you should treat your union members as a stack,
        sorted from most-strict initialization to least-strict.
    """

    __slots__ = ("ordered_routines", "stack")

    def __init__(self, t: type[UnionT], context: ContextT, *, var: str | None = None):
        """Constructor.

        Args:
            t: The type to unmarshal into.
            context: Any nested type context. Used to resolve the member unmarshallers.
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        self.stack = inspection.args(t, evaluate=True)
        if inspection.isoptionaltype(t):
            self.stack = (self.stack[-1], *self.stack[:-1])

        self.ordered_routines = [self.context[typ] for typ in self.stack]

    def __call__(self, val: tp.Any) -> UnionT:
        """Unmarshal a value into the bound `UnionT`.

        Args:
            val: The input value to unmarshal.

        Raises:
            ValueError: If `val` cannot be unmarshalled into any member type.
        """
        for routine in self.ordered_routines:
            with contextlib.suppress(
                ValueError, TypeError, SyntaxError, AttributeError
            ):
                unmarshalled = routine(val)
                return unmarshalled

        raise ValueError(f"{val!r} is not one of types {self.stack!r}")


_KT = tp.TypeVar("_KT")
_VT = tp.TypeVar("_VT")


MappingT = tp.TypeVar("MappingT", bound=tp.Mapping)


class SubscriptedMappingUnmarshaller(
    AbstractUnmarshaller[MappingT], tp.Generic[MappingT]
):
    """Unmarshaller for a subscripted mapping type.

    Note:
        This unmarshaller handles standard key->value mappings. We leverage our own
        generic `iteritems` to allow for translating other collections or structured
        objects into the target mapping.

        The algorithm is as follows:

        1. We attempt to decode the input into a real Python object.
        2. We iterate over key->value pairs.
        3. We call the key-type's unmarshaller on the `key` members.
        4. We call the value-type's unmarshaller on the `value` members.
        5. We pass the unmarshalling iterator in to the type's constructor.

    See Also:
        - [`typelib.serdes.load`][]
        - [`typelib.serdes.iteritems`][]
    """

    __slots__ = (
        "keys",
        "values",
    )

    def __init__(self, t: type[MappingT], context: ContextT, *, var: str | None = None):
        """Constructor.

        Args:
            t: The type to unmarshal into.
            context: Any nested type context. Used to resolve the member unmarshallers.
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        key_t, value_t = inspection.args(t, evaluate=True)
        self.keys = context[key_t]
        self.values = context[value_t]

    def __call__(self, val: tp.Any) -> MappingT:
        """Unmarshal a value into the bound `MappingT`.

        Args:
            val: The input value to unmarshal.
        """
        # Always decode bytes.
        decoded = serdes.load(val)
        keys = self.keys
        values = self.values
        return self.origin(  # type: ignore[call-arg]
            ((keys(k), values(v)) for k, v in serdes.iteritems(decoded))
        )


IterableT = tp.TypeVar("IterableT", bound=tp.Iterable)


class SubscriptedIterableUnmarshaller(
    AbstractUnmarshaller[IterableT], tp.Generic[IterableT]
):
    """Unmarshaller for a subscripted iterable type.

    Note:
        This unmarshaller handles standard simple iterable types. We leverage our own
        generic `itervalues` to allow for translating other collections or structured
        objects into the target iterable.

        The algorithm is as follows:

        1. We attempt to decode the input into a real Python object.
        2. We iterate over values in the decoded input.
        3. We call the value-type's unmarshaller on the `value` members.
        5. We pass the unmarshalling iterator in to the type's constructor.

    See Also:
        - [`typelib.serdes.load`][]
        - [`typelib.serdes.itervalues`][]
    """

    __slots__ = ("values",)

    def __init__(
        self, t: type[IterableT], context: ContextT, *, var: str | None = None
    ):
        """Constructor.

        Args:
            t: The type to unmarshal into.
            context: Any nested type context. Used to resolve the member unmarshaller.
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t=t, context=context, var=var)
        # supporting tuple[str, ...]
        (value_t, *_) = inspection.args(t, evaluate=True)
        self.values = context[value_t]

    def __call__(self, val: tp.Any) -> IterableT:
        """Unmarshal a value into the bound `IterableT`.

        Args:
            val: The input value to unmarshal.
        """
        # Always decode bytes.
        decoded = serdes.load(val)
        values = self.values
        return self.origin(values(v) for v in serdes.itervalues(decoded))  # type: ignore[call-arg]


IteratorT = tp.TypeVar("IteratorT", bound=tp.Iterator)


class SubscriptedIteratorUnmarshaller(
    AbstractUnmarshaller[IteratorT], tp.Generic[IteratorT]
):
    """Unmarshaller for a subscripted iterator type.

    Note:
        This unmarshaller handles standard simple iterable types. We leverage our own
        generic `itervalues` to allow for translating other collections or structured
        objects into the target iterator.

        The algorithm is as follows:

        1. We attempt to decode the input into a real Python object.
        2. We iterate over values in the decoded input.
        3. We call the value-type's unmarshaller on the `value` members.
        5. We return a new, unmarshalling iterator.

    See Also:
        - [`typelib.serdes.load`][]
        - [`typelib.serdes.itervalues`][]
    """

    __slots__ = ("values",)

    def __init__(
        self, t: type[IteratorT], context: ContextT, *, var: str | None = None
    ):
        """Constructor.

        Args:
            t: The type to unmarshal into.
            context: Any nested type context. Used to resolve the member unmarshaller.
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        (value_t,) = inspection.args(t, evaluate=True)
        self.values = context[value_t]

    def __call__(self, val: tp.Any) -> IteratorT:
        """Unmarshal a value into the bound `IteratorT`.

        Args:
            val: The input value to unmarshal.
        """
        # Always decode bytes.
        decoded = serdes.load(val)
        values = self.values
        it: IteratorT = (values(v) for v in serdes.itervalues(decoded))  # type: ignore[assignment]
        return it


class FixedTupleUnmarshaller(AbstractUnmarshaller[compat.TupleT]):
    """Unmarshaller for a "fixed" tuple (e.g., `tuple[int, str, float]`).

    Note:
        Python supports two distinct uses for tuples, unlike in other languages:

        1. Tuples with a fixed number of members.
        2. Tuples of variable length (an immutable sequence).

        "Fixed" tuples may have a distinct type for each member, while variable-length
        tuples may only have a single type (or union of types) for all members.

        Variable-length tuples are handled by our generic iterable unmarshaller.

        For "fixed" tuples, the algorithm is:

        1. Attempt to decode the input into a real Python object.
        2. zip the stack of member unmarshallers and the values in the decoded object.
        3. Unmarshal each value using the associated unmarshaller for that position.
        4. Pass the unmarshalling iterator in to the type's constructor.

    Tip:
        If the input has more members than the type definition allows, those members
        will be dropped by nature of our unmarshalling algorithm.

    See Also:
        - [`typelib.serdes.load`][]
        - [`typelib.serdes.itervalues`][]
    """

    __slots__ = ("ordered_routines", "stack")

    def __init__(
        self, t: type[compat.TupleT], context: ContextT, *, var: str | None = None
    ):
        """Constructor.

        Args:
            t: The type to unmarshal into.
            context: Any nested type context. Used to resolve the value unmarshaller stack.
            var: A variable name for the indicated type annotation (unused, optional).
        """
        super().__init__(t, context, var=var)
        self.stack = inspection.args(t, evaluate=True)
        self.ordered_routines = [self.context[vt] for vt in self.stack]

    def __call__(self, val: tp.Any) -> compat.TupleT:
        """Unmarshal a value into the bound [`tuple`][] structure.

        Args:
            val: The input value to unmarshal.
        """
        decoded = serdes.load(val)
        return self.origin(
            routine(v)
            for routine, v in zip(
                self.ordered_routines, serdes.itervalues(decoded), strict=False
            )
        )


_ST = tp.TypeVar("_ST")


class StructuredTypeUnmarshaller(AbstractUnmarshaller[_ST]):
    """Unmarshaller for a "structured" (user-defined) type.

    Note:
        This unmarshaller supports the unmarshalling of any mapping or structured
        type into the targeted structured type. There are limitations.

        The algorithm is:

        1. Attempt to decode the input into a real Python object.
        2. Using a mapping of the structured types "field" to the field-type's unmarshaller,
           iterate over the field->value pairs of the input, skipping fields in the
           input which are not present in the field mapping.
        3. Store each unmarshalled value in a keyword-argument mapping.
        4. Unpack the keyword argument mapping into the bound type's constructor.

    Tip:
        While we don't currently support arbitrary collections, we may add this
        functionality at a later date. Doing so requires more advanced introspection
        and parameter-binding that would lead to a significant loss in performance if
        not done carefully.

    See Also:
        - [`typelib.serdes.load`][]
        - [`typelib.serdes.itervalues`][]
    """

    __slots__ = ("fields_by_var",)

    def __init__(self, t: type[_ST], context: ContextT, *, var: str | None = None):
        """Constructor.

        Args:
            t: The type to unmarshal into.
            context: Any nested type context. Used to resolve the value field-to-unmarshaller mapping.
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
                    "Failed to identify an unmarshaller for the associated type-variable pair: "
                    f"Original ref: {hint}, Resolved ref: {resolved}. Will default to no-op.",
                    stacklevel=6,
                )
                fields_by_var[name] = NoOpUnmarshaller(hint, self.context, var=name)
                continue

            fields_by_var[name] = m

        return fields_by_var

    def __call__(self, val: tp.Any) -> _ST:
        """Unmarshal a value into the bound type.

        Args:
            val: The input value to unmarshal.
        """
        decoded = serdes.load(val)
        fields = self.fields_by_var
        kwargs = {f: fields[f](v) for f, v in serdes.iteritems(decoded) if f in fields}
        return self.t(**kwargs)
