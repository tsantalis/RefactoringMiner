"""The API for marshalling higher-order Python objects into simple, encodable Python objects."""

from __future__ import annotations

import typing as tp

from typelib import ctx, graph, serdes
from typelib.marshals import routines
from typelib.py import compat, inspection, refs

T = tp.TypeVar("T")

__all__ = (
    "DelayedMarshaller",
    "marshal",
    "marshaller",
)


def marshal(
    value: tp.Any, *, t: type[T] | refs.ForwardRef | str | None = None
) -> serdes.MarshalledValueT:
    """Marshal `value` from `typ` into [`MarshalledValueT`][typelib.serdes.MarshalledValueT].

    Args:
        value: The value to reduce to a simple, encode-able type.
        t:
            The type to use for building the marshaller (optional).
            If not provided, we'll default to the type of the input value.
    """
    typ = value.__class__ if t is None else t
    routine: routines.AbstractMarshaller[T] = marshaller(typ)
    unmarshalled = routine(value)
    return unmarshalled


@compat.cache
def marshaller(
    t: type[T] | refs.ForwardRef | compat.TypeAliasType | str,
) -> routines.AbstractMarshaller[T]:
    """Get a marshaller routine for a given type.

    Args:
        t:
            The type annotation to generate a marshaller for. Can be a type, type alias,
            [`typing.ForwardRef`][], or string reference.
    """
    nodes = graph.static_order(t)
    context: ctx.TypeContext[routines.AbstractMarshaller] = ctx.TypeContext()
    if not nodes:
        return routines.NoOpMarshaller(t=t, context=context, var=None)  # type: ignore[arg-type]

    # "root" type will always be the final node in the sequence.
    root = nodes[-1]
    for node in nodes:
        context[node.type] = _get_unmarshaller(node, context=context)
        context[node.unwrapped] = context[node.type]

    return context[root.type]


def _get_unmarshaller(  # type: ignore[return]
    node: graph.TypeNode,
    context: routines.ContextT,
) -> routines.AbstractMarshaller[T]:
    if node.type in context:
        return context[node.type]

    for check, unmarshaller_cls in _HANDLERS.items():
        if check(node.unwrapped):  # type: ignore[arg-type]
            typ = tp.cast("type[T]", node.unwrapped)
            return unmarshaller_cls(typ, context=context, var=node.var)

    typ = tp.cast("type[T]", node.unwrapped)
    return routines.StructuredTypeMarshaller(typ, context=context, var=node.var)


class DelayedMarshaller(routines.AbstractMarshaller[T]):
    """Delayed proxy for a given type's marshaller, used when we encounter a [`typing.ForwardRef`][].

    Notes:
        This allows us to delay the resolution of the given type reference until
        call-time, enabling support for cyclic and recursive types.
    """

    def __init__(
        self, t: type[T], context: routines.ContextT, *, var: str | None = None
    ):
        super().__init__(t, context, var=var)
        self._resolved: routines.AbstractMarshaller[T] | None = None

    @property
    def resolved(self) -> routines.AbstractMarshaller[T]:
        """The resolved marshaller."""
        if self._resolved is None:
            self._resolved = marshaller(self.t)
            for attr in self._resolved.__slots__:
                setattr(self, attr, getattr(self._resolved, attr))
        return self._resolved

    def __call__(self, val: T) -> serdes.MarshalledValueT:
        unmarshalled = self.resolved(val)
        return unmarshalled


# Order is IMPORTANT! This is a FIFO queue.
_HANDLERS: tp.Mapping[
    tp.Callable[[type[T]], bool], type[routines.AbstractMarshaller]
] = {
    # Short-circuit forward refs
    inspection.isforwardref: DelayedMarshaller,
    inspection.isunresolvable: routines.NoOpMarshaller,
    inspection.isnonetype: routines.NoOpMarshaller,
    # Special handler for Literals
    inspection.isliteral: routines.LiteralMarshaller,
    # Special handler for Unions...
    inspection.isuniontype: routines.UnionMarshaller,
    # Special handling for Enums
    inspection.isenumtype: routines.EnumMarshaller,
    # Non-intersecting types (order doesn't matter here.
    inspection.isdatetimetype: routines.DateTimeMarshaller,
    inspection.isdatetype: routines.DateMarshaller,
    inspection.istimetype: routines.TimeMarshaller,
    inspection.istimedeltatype: routines.TimeDeltaMarshaller,
    inspection.isuuidtype: routines.UUIDMarshaller,
    inspection.ispatterntype: routines.PatternMarshaller,
    inspection.ispathtype: routines.PathMarshaller,
    inspection.isdecimaltype: routines.DecimalMarshaller,
    inspection.isfractiontype: routines.FractionMarshaller,
    inspection.isintegertype: routines.IntegerMarshaller,
    inspection.isfloattype: routines.FloatMarshaller,
    inspection.isstringtype: routines.StringMarshaller,
    inspection.isbytestype: routines.BytesMarshaller,
    # Psuedo-structured containers, should check before generics.
    inspection.istypeddict: routines.StructuredTypeMarshaller,
    inspection.istypedtuple: routines.StructuredTypeMarshaller,
    inspection.isnamedtuple: routines.StructuredTypeMarshaller,
    inspection.isfixedtupletype: routines.FixedTupleMarshaller,
    (
        lambda t: inspection.issubscriptedgeneric(t) and inspection.ismappingtype(t)
    ): routines.SubscriptedMappingMarshaller,
    (
        lambda t: inspection.issubscriptedgeneric(t) and inspection.isiterabletype(t)
    ): routines.SubscriptedIterableMarshaller,
    # A mapping is a collection so must come before that check.
    inspection.ismappingtype: routines.MappingMarshaller,
    # Generic Iterable handler
    inspection.isiterabletype: routines.IterableMarshaller,
}
