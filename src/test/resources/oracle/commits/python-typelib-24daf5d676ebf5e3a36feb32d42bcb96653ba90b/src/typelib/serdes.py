"""Utilities for type translation, serialization, and deserialization.

Examples: Typical Usage
    >>> from typelib import serdes
    >>>
    >>> serdes.load("1")
    1

    >>> import datetime
    >>> from typelib import serdes
    >>>
    >>> serdes.unixtime(datetime.datetime(2020, 1, 1))
    1577854800.0
    >>> serdes.isoformat(datetime.timedelta(hours=1))
    'PT1H'

    >>> import dataclasses
    >>> @dataclasses.dataclass
    ... class Class:
    ...     attr: str
    ...
    >>> instance = Class(attr="value")
    >>> dict(serdes.iteritems(instance))
    {'attr': 'value'}
"""

from __future__ import annotations

import ast
import contextlib
import dataclasses
import datetime
import operator
import typing as t

import pendulum
from more_itertools import peekable

from typelib import constants
from typelib.py import compat, inspection

__all__ = (
    "dateparse",
    "decode",
    "get_items_iter",
    "isoformat",
    "iteritems",
    "itervalues",
    "load",
    "strload",
    "unixtime",
)


@t.overload
def decode(  # type: ignore[overload-overlap]
    val: bytes | bytearray | memoryview, *, encoding: str = constants.DEFAULT_ENCODING
) -> str: ...


@t.overload
def decode[T](val: T) -> T: ...  # type: ignore[overload-overlap]


def decode(val: t.Any, *, encoding: str = constants.DEFAULT_ENCODING) -> t.Any:
    """Decode a bytes-like object into a str.

    Note:
        If a non-bytes-like object is passed, it will be returned unchanged.

    Examples:
        >>> from typelib import serdes
        >>> serdes.decode(b"abc")
        'abc'
        >>> serdes.decode(memoryview(b"abc"))
        'abc'

    Args:
        val: The object to be decoded.
        encoding: The encoding to use when decoding the object (defaults "utf8").
    """
    val = val.tobytes() if isinstance(val, memoryview) else val
    if isinstance(val, (bytes, bytearray)):
        decoded = val.decode(encoding)
        return decoded
    return val


@compat.lru_cache(maxsize=100_000)
def isoformat(dt: datetime.date | datetime.time | datetime.timedelta) -> str:
    """Format any date/time object into an ISO-8601 string.

    Note:
        While the standard library includes `isoformat()` methods for
        [`datetime.date`][], [`datetime.time`][], &
        [`datetime.datetime`][], they do not include a method for serializing
        [`datetime.timedelta`][], even though durations are included in the
        ISO 8601 specification. This function fills that gap.

    Examples:
        >>> import datetime
        >>> from typelib import serdes
        >>> serdes.isoformat(datetime.date(1970, 1, 1))
        '1970-01-01'
        >>> serdes.isoformat(datetime.time())
        '00:00:00'
        >>> serdes.isoformat(datetime.datetime(1970, 1, 1))
        '1970-01-01T00:00:00'
        >>> serdes.isoformat(datetime.timedelta(hours=1))
        'PT1H'
    """
    if isinstance(dt, (datetime.date, datetime.time)):
        return dt.isoformat()
    dur: pendulum.Duration = (
        dt
        if isinstance(dt, pendulum.Duration)
        else pendulum.duration(
            days=dt.days,
            seconds=dt.seconds,
            microseconds=dt.microseconds,
        )
    )
    datepart = "".join(
        f"{p}{s}"
        for p, s in ((dur.years, "Y"), (dur.months, "M"), (dur.remaining_days, "D"))
        if p
    )
    timepart = "".join(
        f"{p}{s}"
        for p, s in (
            (dur.hours, "H"),
            (dur.minutes, "M"),
            (
                f"{dur.remaining_seconds}.{dur.microseconds:06}"
                if dur.microseconds
                else dur.remaining_seconds,
                "S",
            ),
        )
        if p
    )
    period = f"P{datepart}T{timepart}"
    return period


def unixtime(dt: datetime.date | datetime.time | datetime.timedelta) -> float:
    """Convert a date/time object to a unix timestamp.

    Note:
        Time is messy. Here is how we've decided to make this work:

        - `datetime.datetime` instances will preserve the current tzinfo (even if naive).
        - `datetime.time` instances will default to today, preserving the tzinfo (even if naive).
        - `datetime.date` instances will assume UTC.
        - `datetime.timedelta` instances be reflected as total seconds since epoch (January 1, 1970).

        If you find yourself in a situation where this does not work for you, your best
        bet is to stop using tz-naive date/time objects. *It's always best to keep your time
        explicit!*

    Args:
        dt: The object to be converted.

    Examples:
        >>> import datetime
        >>> from typelib import serdes
        >>>
        >>> serdes.unixtime(datetime.datetime(1970, 1, 1, tzinfo=datetime.timezone.utc))
        0.0
        >>> serdes.unixtime(datetime.date(1970, 1, 1))
        0.0
    """
    if isinstance(dt, datetime.timedelta):
        return dt.total_seconds()

    if isinstance(dt, datetime.time):
        dt = datetime.datetime.now(tz=dt.tzinfo).replace(
            hour=dt.hour,
            minute=dt.minute,
            second=dt.second,
            microsecond=dt.microsecond,
        )
    if isinstance(dt, datetime.date) and not isinstance(dt, datetime.datetime):
        dt = datetime.datetime(
            year=dt.year,
            month=dt.month,
            day=dt.day,
            tzinfo=datetime.UTC,
        )

    return dt.timestamp()


@compat.lru_cache(maxsize=100_000)
def dateparse[DateTimeT: (datetime.date, datetime.time, datetime.timedelta)](
    val: str, t: type[DateTimeT]
) -> DateTimeT:
    """Parse a date string into a datetime object.

    Examples:
        >>> import datetime
        >>> from typelib import serdes
        >>> serdes.dateparse("1970-01-01",t=datetime.datetime)
        DateTime(1970, 1, 1, 0, 0, 0, tzinfo=Timezone('UTC'))

    Args:
        val: The date string to parse.
        t: The target datetime type.

    Returns:
        The parsed datetime object.

    Raises:
        ValueError:
            If `val` is not a date string or does not resolve to an instance of
            the target datetime type.
    """
    try:
        # When `exact=False`, the only two possibilities are DateTime and Duration.
        parsed: pendulum.DateTime | pendulum.Duration = pendulum.parse(val)  # type: ignore[assignment]
        normalized = _nomalize_dt(val=val, parsed=parsed, td=t)
        return normalized
    except ValueError:
        if val.isdigit() or val.isdecimal():
            return _normalize_number(numval=float(val), td=t)
        raise


def _nomalize_dt[DateTimeT: (datetime.date, datetime.time, datetime.timedelta)](
    *, val: str, parsed: pendulum.DateTime | pendulum.Duration, td: type[DateTimeT]
) -> DateTimeT:
    if isinstance(parsed, pendulum.DateTime):
        if issubclass(td, datetime.time):
            return parsed.time().replace(tzinfo=parsed.tzinfo)
        if issubclass(td, datetime.datetime):
            return parsed
        if issubclass(td, datetime.date):
            return parsed.date()
    if not isinstance(parsed, td):
        raise ValueError(f"Cannot parse {val!r} as {td.__qualname__!r}")  # noqa: TRY004
    return parsed


def _normalize_number[DateTimeT: (datetime.date, datetime.time, datetime.timedelta)](
    *, numval: float, td: type[DateTimeT]
) -> DateTimeT:
    # Assume the number value is seconds - same logic as time-since-epoch
    if issubclass(td, datetime.timedelta):
        return datetime.timedelta(seconds=numval)
    # Parse a datetime from the time-since-epoch as indicated by the value.
    dt = datetime.datetime.fromtimestamp(numval, tz=datetime.UTC)
    # Return the datetime if the target type is a datetime
    if issubclass(td, datetime.datetime):
        return dt
    # If the target type is a time object, just return the time.
    if issubclass(td, datetime.time):
        return dt.time().replace(tzinfo=dt.tzinfo)
    # If the target type is a date object, just return the date.
    return dt.date()


def iteritems(val: t.Any) -> t.Iterable[tuple[t.Any, t.Any]]:
    """Iterate over (field, value) pairs for any object.

    Examples:
        >>> import dataclasses
        >>> from typelib import serdes
        >>>
        >>> @dataclasses.dataclass
        ... class Class:
        ...     attr: str
        ...
        >>> instance = Class(attr="value")
        >>> [*serdes.iteritems(instance)]
        [('attr', 'value')]
        >>> [*serdes.iteritems("string")]
        [(0, 's'), (1, 't'), (2, 'r'), (3, 'i'), (4, 'n'), (5, 'g')]
        >>> [*serdes.iteritems(serdes.iteritems(instance))]
        [('attr', 'value')]

    Note:
        If the given item is detected to be an iterable of pairs (e.g., `[('a', 1), ('b', 2)]`),
        we will iterate directly over that.

        Otherwise, we will create an iterator over (field, value) pairs with the following
        strategy:

        - For mappings -> `((key, value), ...)`
        - For structured objects (user-defined classes) -> `((field, value), ...)`
        - For all other iterables -> `((index, value), ...)`.

    Args:
        val: The object to iterate over.
    """
    # If the given value is an iterable, we will create a `peekable` so we can inspect it.
    #   In that case, we want to continue using the peekable, since the act of peeking
    #   is destructive to the original iterable in the event it is an iterator.
    is_pairs, it = _is_iterable_of_pairs(val)
    if is_pairs:
        return iter(it)

    iterate = get_items_iter(val.__class__)
    return iterate(it)


def _is_iterable_of_pairs(val: t.Any) -> tuple[bool, t.Any]:
    cls = val.__class__
    if not inspection.isiterabletype(cls) or inspection.ismappingtype(cls):
        return False, val

    if inspection.issequencetype(cls):
        peek = next(iter(val), ())
        is_pairs = inspection.iscollectiontype(peek.__class__) and len(peek) == 2
        return is_pairs, val

    it = peekable(val)
    peek = it.peek()
    is_pairs = inspection.iscollectiontype(peek.__class__) and len(peek) == 2
    return is_pairs, it


def itervalues(val: t.Any) -> t.Iterator[t.Any]:
    """Iterate over the contained values for any object.

    Examples:
        >>> import dataclasses
        >>> from typelib import serdes
        >>>
        >>> @dataclasses.dataclass
        ... class Class:
        ...     attr: str
        ...
        >>> instance = Class(attr="value")
        >>> [*serdes.itervalues(instance)]
        ['value']

    Args:
        val: The object to iterate over.
    """
    iterate = get_items_iter(val.__class__)
    return (v for k, v in iterate(val))


@compat.cache
def get_items_iter(tp: type) -> t.Callable[[t.Any], t.Iterable[tuple[t.Any, t.Any]]]:
    """Given a type, return a callable which will produce an iterator over (field, value) pairs.

    Examples:
        >>> import dataclasses
        >>> from typelib import serdes
        >>>
        >>> @dataclasses.dataclass
        ... class Class:
        ...     attr: str
        ...
        >>> instance = Class(attr="value")
        >>> iteritems = get_items_iter(Class)
        >>> next(iteritems(instance))
        ('attr', 'value')

    Args:
        tp: The type to create an iterator for.
    """
    ismapping, isnamedtuple, isiterable = (
        inspection.ismappingtype(tp),
        inspection.isnamedtuple(tp),
        inspection.isiterabletype(tp),
    )
    if ismapping:
        return _itemscaller
    if isnamedtuple:
        return _namedtupleitems
    if isiterable:
        return enumerate
    return _make_fields_iterator(tp)


def _namedtupleitems(val: t.NamedTuple) -> t.Iterable[tuple[str, t.Any]]:
    return zip(val._fields, val, strict=False)


def _make_fields_iterator(
    tp: type,
) -> t.Callable[[t.Any], t.Iterator[tuple[t.Any, t.Any]]]:
    # If we have a dataclass, use the defined public fields.
    if dataclasses.is_dataclass(tp):
        public_attribs = [
            f.name for f in dataclasses.fields(tp) if not f.name.startswith("_")
        ]
    # Otherwise, try using the public type-hints.
    else:
        attribs = inspection.get_type_hints(tp)
        public_attribs = [k for k in attribs if not k.startswith("_")]
    # If that didn't work, look for `__slots__`.
    if not public_attribs and hasattr(tp, "__slots__"):
        public_attribs = [s for s in tp.__slots__ if not s.startswith("_")]
    # If we located all public attributes, create a factory function for iterating over
    #   these fields and fetching the value from an instance.
    if public_attribs:

        def _iterfields(val: t.Any) -> t.Iterator[tuple[str, t.Any]]:
            return ((a, getattr(val, a)) for a in public_attribs)

        return _iterfields

    # Finally, if all else fails, just use `vars` on the instance.
    def _itervars(val: t.Any) -> t.Iterator[tuple[str, t.Any]]:
        return ((k, v) for k, v in vars(val).items() if not k.startswith("_"))

    return _itervars


def load[T](val: T) -> PythonValueT | T:
    """Attempt to decode `val` if it is a text-like object.

    Otherwise, return `val` unchanged.

    Examples:
        >>> from typelib import serdes
        >>> serdes.load(1)
        1
        >>> serdes.load("1")
        1
        >>> serdes.load("1,2")
        (1, 2)
        >>> serdes.load(b'{"a": 1, "b": 2}')
        {'a': 1, 'b': 2}

    Args:
        val: The value to decode.
    """
    return strload(val) if inspection.istexttype(val.__class__) else val  # type: ignore[arg-type]


@compat.lru_cache(maxsize=100_000)
def strload(val: str | bytes | bytearray | memoryview) -> PythonValueT:
    """Attempt to decode a string-like input into a Python value.

    Examples:
        >>> from typelib import serdes
        >>> serdes.strload("1")
        1
        >>> serdes.strload("1,2")
        (1, 2)
        >>> serdes.strload(b'{"a": 1, "b": 2}')
        {'a': 1, 'b': 2}


    Tip:
        This function is memoized and only safe for text-type inputs.

    See Also:
         - [`load`][typelib.serdes.load]

    Args:
        val: The string-like input to be decoded.
    """
    with contextlib.suppress(ValueError):
        return compat.json.loads(val)  # type: ignore[arg-type]

    decoded = decode(val)
    with contextlib.suppress(ValueError, TypeError, SyntaxError):
        return ast.literal_eval(decoded)

    return decoded


type PythonPrimitiveT = bool | int | float | str | None
"""Type alias for serializable, non-container Python types."""
type PythonValueT = (
    PythonPrimitiveT
    | dict[PythonPrimitiveT, PythonValueT]
    | list[PythonValueT]
    | tuple[PythonValueT, ...]
    | set[PythonValueT]
)
"""Type alias for any Python builtin type."""
type MarshalledValueT = (
    PythonPrimitiveT | dict[PythonPrimitiveT, MarshalledValueT] | list[MarshalledValueT]
)
"""Type alias for a Python value which is ready for over-the-wire serialization."""


_itemscaller = operator.methodcaller("items")
_valuescaller = operator.methodcaller("values")
