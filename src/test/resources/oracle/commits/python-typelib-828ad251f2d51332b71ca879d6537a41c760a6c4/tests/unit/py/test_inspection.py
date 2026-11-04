from __future__ import annotations

import abc
import collections
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
import sqlite3
import sys
import types
import typing
import typing as t
import typing_extensions as te
import uuid
from unittest import mock

import pytest

from typelib.py import inspection, refs


class MyClass: ...


@pytest.mark.suite(
    dict=dict(annotation=dict, expected=dict),
    list=dict(annotation=list, expected=list),
    tuple=dict(annotation=tuple, expected=tuple),
    set=dict(annotation=set, expected=set),
    frozenset=dict(annotation=frozenset, expected=frozenset),
    generic_dict=dict(annotation=t.Dict, expected=dict),
    generic_list=dict(annotation=t.List, expected=list),
    generic_tuple=dict(annotation=t.Tuple, expected=tuple),
    generic_set=dict(annotation=t.Set, expected=set),
    generic_frozenset=dict(annotation=t.FrozenSet, expected=frozenset),
    function=dict(annotation=lambda x: x, expected=t.Callable),
    newtype=dict(annotation=t.NewType("new", dict), expected=dict),
    subscripted_generic=dict(annotation=t.Dict[str, str], expected=dict),
    user_type=dict(annotation=MyClass, expected=MyClass),
    class_var_subscripted=dict(annotation=t.ClassVar[str], expected=str),
    class_var_unsubsripted=dict(annotation=t.ClassVar, expected=t.ClassVar),
    type_alias_type=dict(annotation=te.TypeAliasType("T", dict), expected=dict),
)
def test_origin(annotation, expected):
    # When
    actual = inspection.origin(annotation)
    # Then
    assert actual == expected


UnBoundT = t.TypeVar("UnBoundT")
BoundT = t.TypeVar("BoundT", bound=int)
ConstrainedT = t.TypeVar("ConstrainedT", str, int)


@pytest.mark.suite(
    dict=dict(annotation=dict, expected=()),
    subscripted_dict=dict(annotation=t.Dict[str, str], expected=(str, str)),
    dict_unbound_tvar=dict(annotation=t.Dict[str, UnBoundT], expected=(str, t.Any)),
    dict_bound_tvar=dict(annotation=t.Dict[str, BoundT], expected=(str, int)),
    dict_constrained_tvar=dict(
        annotation=t.Dict[str, ConstrainedT], expected=(str, t.Union[str, int])
    ),
)
def test_args(annotation, expected):
    # When
    actual = inspection.args(annotation)
    # Then
    assert actual == expected


@pytest.mark.suite(
    builtin_dict=dict(annotation=dict, expected="dict"),
    generic_dict=dict(annotation=t.Dict, expected="Dict"),
    subscripted_dict=dict(annotation=t.Dict[str, str], expected="Dict"),
    user_class=dict(annotation=MyClass, expected=MyClass.__name__),
)
def test_name(annotation, expected):
    # When
    actual = inspection.name(annotation)
    # Then
    assert actual == expected


def outer():
    def closure(): ...

    return closure


@pytest.mark.suite(
    builtin_dict=dict(annotation=dict, expected="dict"),
    generic_dict=dict(annotation=t.Dict, expected="typing.Dict"),
    subscripted_dict=dict(annotation=t.Dict[str, str], expected="typing.Dict"),
    user_class=dict(annotation=MyClass, expected=MyClass.__qualname__),
    forwardref=dict(annotation=t.ForwardRef("foo"), expected="foo"),
    sanitize_local=dict(annotation=outer(), expected="outer.closure"),
)
def test_qualname(annotation, expected):
    # When
    actual = inspection.qualname(annotation)
    # Then
    assert actual == expected


def test_resolve_supertype():
    # Given
    supertype = int
    UserID = t.NewType("UserID", int)
    AdminID = t.NewType("AdminID", UserID)
    # When
    resolved = inspection.resolve_supertype(AdminID)
    # Then
    assert resolved is supertype


class NoHints:
    def __init__(self, foo: str, bar): ...


class BadHints:
    hint: impossible  # noqa: F821


@pytest.mark.suite(
    no_hints=dict(given_obj=NoHints, given_exhaustive=False, expected_type_hints={}),
    bad_hints=dict(given_obj=BadHints, given_exhaustive=False, expected_type_hints={}),
    no_hints_exhaustive=dict(
        given_obj=NoHints,
        given_exhaustive=True,
        expected_type_hints={
            "foo": t.ForwardRef("str", module=__name__),
            "bar": t.Any,
        },
    ),
    bad_hints_exhaustive=dict(
        given_obj=BadHints, given_exhaustive=True, expected_type_hints={}
    ),
)
def test_get_type_hints(given_obj, given_exhaustive, expected_type_hints):
    # When
    type_hints = inspection.get_type_hints(given_obj, given_exhaustive)
    # Then
    assert type_hints == expected_type_hints


@dataclasses.dataclass
class FieldClass:
    field: str = None


class TotalFieldDict(t.TypedDict):
    field: str


class FieldDict(t.TypedDict, total=False):
    field: str


class FieldTuple(t.NamedTuple):
    field: str = None


StructuredTuple = t.Tuple[str]
VarTuple = t.Tuple[str, ...]


@pytest.mark.skipif(sys.version_info < (3, 9), reason="3.8 doesn't handle ForwardRef.")
@pytest.mark.suite(
    user_class=dict(
        annotation=FieldClass,
        expected=inspect.Signature(
            parameters=(
                inspect.Parameter(
                    name="field",
                    kind=inspect.Parameter.POSITIONAL_OR_KEYWORD,
                    default=None,
                    annotation="str",
                ),
            ),
            return_annotation=None,
        ),
    ),
    total_typed_dict=dict(
        annotation=TotalFieldDict,
        expected=inspect.Signature(
            parameters=(
                inspect.Parameter(
                    name="field",
                    kind=inspect.Parameter.KEYWORD_ONLY,
                    annotation=str,
                ),
            )
        ),
    ),
    typed_dict=dict(
        annotation=FieldDict,
        expected=inspect.Signature(
            parameters=(
                inspect.Parameter(
                    name="field",
                    kind=inspect.Parameter.KEYWORD_ONLY,
                    default=...,
                    annotation=str,
                ),
            )
        ),
    ),
    named_tuple=dict(
        annotation=FieldTuple,
        expected=inspect.Signature(
            parameters=(
                inspect.Parameter(
                    name="field",
                    kind=inspect.Parameter.POSITIONAL_OR_KEYWORD,
                    default=None,
                    annotation=typing.ForwardRef("str"),
                ),
            )
        ),
    ),
    structured_tuple=dict(
        annotation=StructuredTuple,
        expected=inspect.Signature(
            parameters=(
                inspect.Parameter(
                    name="arg0",
                    kind=inspect.Parameter.POSITIONAL_ONLY,
                    annotation=str,
                ),
            )
        ),
    ),
    var_tuple=dict(
        annotation=VarTuple,
        expected=inspect.Signature(
            parameters=(
                inspect.Parameter(
                    name="args",
                    kind=inspect.Parameter.VAR_POSITIONAL,
                    annotation=str,
                ),
            )
        ),
    ),
)
def test_signature(annotation, expected):
    # When
    actual = inspection.signature(annotation)
    # Then
    assert actual == expected


@pytest.mark.suite(
    int=dict(given_type=int),
    bool=dict(given_type=bool),
    float=dict(given_type=float),
    str=dict(given_type=str),
    bytes=dict(given_type=bytes),
    bytearray=dict(given_type=bytearray),
    list=dict(given_type=list),
    set=dict(given_type=set),
    frozenset=dict(given_type=frozenset),
    tuple=dict(given_type=tuple),
    dict=dict(given_type=dict),
    none=dict(given_type=type(None)),
    new_type=dict(given_type=t.NewType("foo", int)),
)
def test_isbuiltintype(given_type):
    # When
    is_valid = inspection.isbuiltintype(given_type)
    # Then
    assert is_valid


@pytest.mark.suite(
    int=dict(given_type=int),
    bool=dict(given_type=bool),
    float=dict(given_type=float),
    str=dict(given_type=str),
    bytes=dict(given_type=bytes),
    bytearray=dict(given_type=bytearray),
    list=dict(given_type=list),
    set=dict(given_type=set),
    frozenset=dict(given_type=frozenset),
    tuple=dict(given_type=tuple),
    dict=dict(given_type=dict),
    none=dict(given_type=type(None)),
    new_type=dict(given_type=t.NewType("foo", int)),
    datetime=dict(given_type=datetime.datetime),
    date=dict(given_type=datetime.datetime),
    timedelta=dict(given_type=datetime.timedelta),
    time=dict(given_type=datetime.time),
    decimal=dict(given_type=decimal.Decimal),
    ipv4=dict(given_type=ipaddress.IPv4Address),
    ipv6=dict(given_type=ipaddress.IPv6Address),
    path=dict(given_type=pathlib.Path),
    uuid=dict(given_type=uuid.UUID),
    defaultdict=dict(given_type=collections.defaultdict),
    deque=dict(given_type=collections.deque),
    mapping_proxy=dict(given_type=types.MappingProxyType),
)
def test_isstdlibtype(given_type):
    # When
    is_valid = inspection.isstdlibtype(given_type)
    # Then
    assert is_valid


def test_isstdlibsubtype():
    # Given

    class SuperStr(str): ...

    # When
    is_valid = inspection.isstdlibsubtype(SuperStr)
    # Then
    assert is_valid


def test_isbuiltinsubtype():
    # Given

    class SuperStr(str): ...

    # When
    is_valid = inspection.isbuiltinsubtype(SuperStr)
    # Then
    assert is_valid


@pytest.mark.suite(
    int=dict(given_type=int),
    bool=dict(given_type=bool),
    float=dict(given_type=float),
    str=dict(given_type=str),
    bytes=dict(given_type=bytes),
    bytearray=dict(given_type=bytearray),
    list=dict(given_type=list),
    set=dict(given_type=set),
    frozenset=dict(given_type=frozenset),
    tuple=dict(given_type=tuple),
    dict=dict(given_type=dict),
    none=dict(given_type=type(None)),
)
def test_isbuiltinstance(given_type):
    # Given
    instance = given_type()
    # When
    is_valid = inspection.isbuiltininstance(instance)
    # Then
    assert is_valid


@pytest.mark.suite(
    int=dict(instance=1),
    bool=dict(instance=True),
    float=dict(instance=1.0),
    str=dict(instance=""),
    bytes=dict(instance=b""),
    bytearray=dict(instance=bytearray()),
    list=dict(instance=[]),
    set=dict(instance=set()),
    frozenset=dict(instance=frozenset([])),
    tuple=dict(instance=()),
    dict=dict(instance={}),
    none=dict(instance=None),
    datetime=dict(instance=datetime.datetime(1970, 1, 1)),
    date=dict(instance=datetime.date(1970, 1, 1)),
    timedelta=dict(instance=datetime.timedelta()),
    time=dict(instance=datetime.time()),
    decimal=dict(instance=decimal.Decimal(1)),
    ipv4=dict(instance=ipaddress.IPv4Address("0.0.0.0")),
    ipv6=dict(instance=ipaddress.IPv6Address("2001:db8::")),
    path=dict(instance=pathlib.Path()),
    uuid=dict(instance=uuid.UUID(int=0)),
    defaultdict=dict(instance=collections.defaultdict()),
    deque=dict(instance=collections.deque()),
    mapping_proxy=dict(instance=types.MappingProxyType({})),
)
def test_isstdlibinstance(instance):
    # When
    is_valid = inspection.isstdlibinstance(instance)
    # Then
    assert is_valid


@pytest.mark.suite(
    optional=dict(given_type=t.Optional[str], expected_is_optional=True),
    union=dict(given_type=t.Union[str, None], expected_is_optional=True),
    literal=dict(given_type=t.Literal["str", None], expected_is_optional=True),
    not_optional=dict(given_type=t.Literal[1, 2], expected_is_optional=False),
)
def test_isoptionaltype(given_type, expected_is_optional):
    # When
    is_optional = inspection.isoptionaltype(given_type)
    # Then
    assert is_optional == expected_is_optional


@pytest.mark.suite(
    union=dict(given_type=t.Union[str, int], expected_is_union=True),
)
def test_isuniontype(given_type, expected_is_union):
    # When
    is_union = inspection.isuniontype(given_type)
    # THen
    assert is_union == expected_is_union


@pytest.mark.suite(
    final=dict(given_type=t.Final[str], expected_is_final=True),
)
def test_isfinal(given_type, expected_is_final):
    # When
    is_final = inspection.isfinal(given_type)
    # Then
    assert is_final == expected_is_final


@pytest.mark.suite(
    literal=dict(given_type=t.Literal["str", None], expected_is_literal=True),
)
def test_isliteral(given_type, expected_is_literal):
    # When
    is_literal = inspection.isliteral(given_type)
    # Then
    assert is_literal == expected_is_literal


@pytest.mark.suite(
    date=dict(given_type=datetime.date, expected_is_date_type=True),
    datetime=dict(given_type=datetime.datetime, expected_is_date_type=True),
    time=dict(given_type=datetime.time, expected_is_date_type=False),
    timedelta=dict(given_type=datetime.timedelta, expected_is_date_type=False),
)
def test_isdatetype(given_type, expected_is_date_type):
    # When
    is_date_type = inspection.isdatetype(given_type)
    # Then
    assert is_date_type == expected_is_date_type


@pytest.mark.suite(
    date=dict(given_type=datetime.date, expected_is_datetime_type=False),
    datetime=dict(given_type=datetime.datetime, expected_is_datetime_type=True),
    time=dict(given_type=datetime.time, expected_is_datetime_type=False),
    timedelta=dict(given_type=datetime.timedelta, expected_is_datetime_type=False),
)
def test_isdatetimetype(given_type, expected_is_datetime_type):
    # When
    is_datetime_type = inspection.isdatetimetype(given_type)
    # Then
    assert is_datetime_type == expected_is_datetime_type


@pytest.mark.suite(
    date=dict(given_type=datetime.date, expected_is_time_type=False),
    datetime=dict(given_type=datetime.datetime, expected_is_time_type=False),
    time=dict(given_type=datetime.time, expected_is_time_type=True),
    timedelta=dict(given_type=datetime.timedelta, expected_is_time_type=False),
)
def test_istimetype(given_type, expected_is_time_type):
    # When
    is_time_type = inspection.istimetype(given_type)
    # Then
    assert is_time_type == expected_is_time_type


@pytest.mark.suite(
    date=dict(given_type=datetime.date, expected_is_timedelta_type=False),
    datetime=dict(given_type=datetime.datetime, expected_is_timedelta_type=False),
    time=dict(given_type=datetime.time, expected_is_timedelta_type=False),
    timedelta=dict(given_type=datetime.timedelta, expected_is_timedelta_type=True),
)
def test_istimedeltatype(given_type, expected_is_timedelta_type):
    # When
    is_timedelta_type = inspection.istimedeltatype(given_type)
    # Then
    assert is_timedelta_type == expected_is_timedelta_type


@pytest.mark.suite(
    decimal=dict(given_type=decimal.Decimal, expected_is_decimal_type=True),
)
def test_isdecimaltype(given_type, expected_is_decimal_type):
    # When
    is_decimal_type = inspection.isdecimaltype(given_type)
    # Then
    assert is_decimal_type == expected_is_decimal_type


@pytest.mark.suite(
    uuid=dict(given_type=uuid.UUID, expected_is_uuid_type=True),
)
def test_isuuidtype(given_type, expected_is_uuid_type):
    # When
    is_uuid_type = inspection.isuuidtype(given_type)
    # Then
    assert is_uuid_type == expected_is_uuid_type


@pytest.mark.suite(
    list=dict(given_type=list, expected_is_iterable_type=True),
    string=dict(given_type=str, expected_is_iterable_type=True),
    set=dict(given_type=set, expected_is_iterable_type=True),
    tuple=dict(given_type=tuple, expected_is_iterable_type=True),
    sequence=dict(given_type=t.Sequence, expected_is_iterable_type=True),
    collection=dict(given_type=t.Collection, expected_is_iterable_type=True),
    iterable=dict(given_type=t.Iterable, expected_is_iterable_type=True),
    iterator=dict(given_type=t.Iterator, expected_is_iterable_type=True),
    integer=dict(given_type=int, expected_is_iterable_type=False),
)
def test_isiterabletype(given_type, expected_is_iterable_type):
    # When
    is_iterable_type = inspection.isiterabletype(given_type)
    # Then
    assert is_iterable_type == expected_is_iterable_type


@pytest.mark.suite(
    iter=dict(given_type=iter([]).__class__, expected_is_iterator_type=True),
    iterator=dict(given_type=t.Iterator, expected_is_iterator_type=True),
)
def test_isiteratortype(given_type, expected_is_iterator_type):
    # When
    is_iterator_type = inspection.isiteratortype(given_type)
    # Then
    assert is_iterator_type == expected_is_iterator_type


@pytest.mark.suite(
    collection=dict(given_type=t.Collection, expected_is_collection_type=True),
    list=dict(given_type=list, expected_is_collection_type=True),
    set=dict(given_type=set, expected_is_collection_type=True),
    tuple=dict(given_type=tuple, expected_is_collection_type=True),
    string=dict(given_type=str, expected_is_collection_type=True),
    dict=dict(given_type=dict, expected_is_collection_type=True),
    mapping=dict(given_type=t.Mapping, expected_is_collection_type=True),
    integer=dict(given_type=int, expected_is_collection_type=False),
)
def test_iscollectiontype(given_type, expected_is_collection_type):
    # When
    is_collection_type = inspection.iscollectiontype(given_type)
    # Then
    assert is_collection_type == expected_is_collection_type


@pytest.mark.suite(
    mapping=dict(given_type=t.Mapping, expected_is_mapping_type=True),
    dict=dict(given_type=t.Dict, expected_is_mapping_type=True),
    mappingproxy=dict(given_type=types.MappingProxyType, expected_is_mapping_type=True),
    sqlite_row=dict(given_type=sqlite3.Row, expected_is_mapping_type=True),
)
def test_ismappingtype(given_type, expected_is_mapping_type):
    # When
    is_mapping_type = inspection.ismappingtype(given_type)
    # Then
    assert is_mapping_type == expected_is_mapping_type


class MyEnum(enum.Enum): ...


@pytest.mark.suite(
    enum=dict(given_type=enum.Enum, expected_is_enum_type=True),
    custom_enum=dict(given_type=MyEnum, expected_is_enum_type=True),
)
def test_isenumtype(given_type, expected_is_enum_type):
    # When
    is_enum_type = inspection.isenumtype(given_type)
    # Then
    assert is_enum_type == expected_is_enum_type


@pytest.mark.suite(
    classvar=dict(given_type=t.ClassVar[int], expected_is_classvar_type=True),
)
def test_isclassvartype(given_type, expected_is_classvar_type):
    # When
    is_classvar_type = inspection.isclassvartype(given_type)
    # Then
    assert is_classvar_type == expected_is_classvar_type


@pytest.mark.suite(
    classvar=dict(given_type=t.ClassVar[int], expected_should_unwrap=True),
    final=dict(given_type=t.Final[str], expected_should_unwrap=True),
    literal=dict(given_type=t.Literal[1], expected_should_unwrap=False),
)
def test_should_unwrap(given_type, expected_should_unwrap):
    # When
    should_unwrap = inspection.should_unwrap(given_type)
    # Then
    assert should_unwrap == expected_should_unwrap


class FromDict:
    @classmethod
    def from_dict(cls, d: dict): ...


@pytest.mark.suite(
    from_dict=dict(given_type=FromDict, expected_is_from_dict_class=True),
)
def test_isfromdictclass(given_type, expected_is_from_dict_class):
    # When
    is_from_dict_class = inspection.isfromdictclass(given_type)
    # Then
    assert is_from_dict_class == expected_is_from_dict_class


@dataclasses.dataclass(frozen=True)
class Frozen: ...


@dataclasses.dataclass
class Mutable: ...


@pytest.mark.suite(
    frozen=dict(given_type=Frozen, expected_is_frozen_dataclass=True),
    mutable=dict(given_type=Mutable, expected_is_frozen_dataclass=False),
)
def test_isfrozendataclass(given_type, expected_is_frozen_dataclass):
    # When
    is_frozen_dataclass = inspection.isfrozendataclass(given_type)
    # Then
    assert is_frozen_dataclass == expected_is_frozen_dataclass


@pytest.mark.suite(
    string=dict(given_obj="", expected_is_hashable=True),
    frozenset=dict(given_obj=frozenset(), expected_is_hashable=True),
    list=dict(given_obj=[], expected_is_hashable=False),
)
def test_ishashable(given_obj, expected_is_hashable):
    # When
    is_hashable = inspection.ishashable(given_obj)
    # Then
    assert is_hashable == expected_is_hashable


class TDict(t.TypedDict):
    string: str


@pytest.mark.suite(
    typeddict=dict(given_type=TDict, expected_is_typeddict=True),
    dict=dict(given_type=dict, expected_is_typeddict=False),
)
def test_istypeddict(given_type, expected_is_typeddict):
    # When
    is_typeddict = inspection.istypeddict(given_type)
    # Then
    assert is_typeddict == expected_is_typeddict


class TTuple(t.NamedTuple):
    string: str


NTuple = collections.namedtuple("NTuple", ("string",))


@pytest.mark.suite(
    typedtuple=dict(given_type=TTuple, expected_is_typedtuple=True),
    namedtuple=dict(given_type=NTuple, expected_is_typedtuple=False),
)
def test_istypedtuple(given_type, expected_is_typedtuple):
    # When
    is_typedtuple = inspection.istypedtuple(given_type)
    # Then
    assert is_typedtuple == expected_is_typedtuple


@pytest.mark.suite(
    typedtuple=dict(given_type=TTuple, expected_is_namedtuple=True),
    namedtuple=dict(given_type=NTuple, expected_is_namedtuple=True),
)
def test_isnamedtuple(given_type, expected_is_namedtuple):
    # When
    is_typedtuple = inspection.isnamedtuple(given_type)
    # Then
    assert is_typedtuple == expected_is_namedtuple


@pytest.mark.suite(
    fixed=dict(given_type=t.Tuple[str, int], expected_is_fixed_tuple=True),
    unfixed=dict(given_type=t.Tuple[str, ...], expected_is_fixed_tuple=False),
)
def test_isfixedtuple(given_type, expected_is_fixed_tuple):
    # When
    is_fixed_tuple = inspection.isfixedtupletype(given_type)
    # Then
    assert is_fixed_tuple == expected_is_fixed_tuple


@pytest.mark.suite(
    ref=dict(given_type=refs.ForwardRef("Ref"), expected_is_forwardref=True),
)
def test_isforwardref(given_type, expected_is_forwardref):
    # When
    is_forwardref = inspection.isforwardref(given_type)
    # Then
    assert is_forwardref == expected_is_forwardref


class Props:
    @property
    def prop(self): ...

    @functools.cached_property
    def cached_prop(self): ...


@pytest.mark.suite(
    property=dict(given_type=Props.prop, expected_is_property=True),
    cached_property=dict(given_type=Props.cached_prop, expected_is_property=True),
)
def test_isproperty(given_type, expected_is_property):
    # When
    is_property = inspection.isproperty(given_type)
    # Then
    assert is_property == expected_is_property


class GetDescriptor:
    def __get__(self): ...


class SetDescriptor:
    def __set__(self): ...


class DeleteDescriptor:
    def __delete__(self): ...


class SetNameDescriptor:
    def __set_name__(self): ...


class AllDescriptor(
    GetDescriptor, SetDescriptor, DeleteDescriptor, SetNameDescriptor
): ...


@pytest.mark.suite(
    get=dict(given_type=GetDescriptor, expected_is_descriptor=True),
    set=dict(given_type=SetDescriptor, expected_is_descriptor=True),
    delete=dict(given_type=DeleteDescriptor, expected_is_descriptor=True),
    setname=dict(given_type=SetNameDescriptor, expected_is_descriptor=True),
    all=dict(given_type=AllDescriptor, expected_is_descriptor=True),
)
def test_isdescriptor(given_type, expected_is_descriptor):
    # When
    is_descriptor = inspection.isdescriptor(given_type)
    # Then
    assert is_descriptor == expected_is_descriptor


@pytest.mark.suite(
    property=dict(given_type=Props.prop, expected_is_simple_attribute=False),
    cached_property=dict(
        given_type=Props.cached_prop, expected_is_simple_attribute=False
    ),
    cls=dict(given_type=Props, expected_is_simple_attribute=False),
    descriptor=dict(given_type=AllDescriptor, expected_is_simple_attribute=False),
    value=dict(given_type=1, expected_is_simple_attribute=True),
)
def test_issimpleattribute(given_type, expected_is_simple_attribute):
    # When
    is_simple_attribute = inspection.issimpleattribute(given_type)
    # Then
    assert is_simple_attribute == expected_is_simple_attribute


class MyABC(abc.ABC):
    @abc.abstractmethod
    def method(self): ...


@pytest.mark.suite(
    abc=dict(given_type=MyABC, expected_is_abstract=True),
    number=dict(given_type=numbers.Number, expected_is_abstract=True),
)
def test_isabstract(given_type, expected_is_abstract):
    # When
    is_abstract = inspection.isabstract(given_type)
    # Then
    assert is_abstract == expected_is_abstract


@pytest.mark.suite(
    string=dict(given_type=str, expected_is_text_type=True),
    bytes=dict(given_type=bytes, expected_is_text_type=True),
    bytearray=dict(given_type=bytearray, expected_is_text_type=True),
)
def test_istexttype(given_type, expected_is_text_type):
    # When
    is_text_type = inspection.istexttype(given_type)
    # Then
    assert is_text_type == expected_is_text_type


@pytest.mark.suite(
    int=dict(given_type=int, expected_is_number_type=True),
    float=dict(given_type=float, expected_is_number_type=True),
    decimal=dict(given_type=decimal.Decimal, expected_is_number_type=True),
    fraction=dict(given_type=fractions.Fraction, expected_is_number_type=True),
)
def test_isnumbertype(given_type, expected_is_number_type):
    # When
    is_number_type = inspection.isnumbertype(given_type)
    # Then
    assert is_number_type == expected_is_number_type


@pytest.mark.suite(
    fixed_tuple=dict(given_type=t.Tuple[str], expected_is_structured_type=True),
    namedtuple=dict(given_type=NTuple, expected_is_structured_type=True),
    typedict=dict(given_type=TDict, expected_is_structured_type=True),
    user_class=dict(given_type=MyClass, expected_is_structured_type=True),
    union=dict(given_type=t.Union[str, int], expected_is_structured_type=False),
    collection=dict(given_type=t.Collection[int], expected_is_structured_type=False),
    literal=dict(given_type=t.Literal[1], expected_is_structured_type=False),
    stdlib=dict(given_type=datetime.datetime, expected_is_structured_type=False),
)
def test_isstructuredtype(given_type, expected_is_structured_type):
    # When
    is_structured_type = inspection.isstructuredtype(given_type)
    # Then
    assert is_structured_type == expected_is_structured_type


T = t.TypeVar("T")


class MyGeneric(t.Generic[T]): ...


@pytest.mark.suite(
    typing_tuple=dict(given_type=t.Tuple, expected_is_generic=True),
    tuple=dict(given_type=tuple, expected_is_generic=False),
    generic=dict(given_type=MyGeneric, expected_is_generic=True),
)
def test_isgeneric(given_type, expected_is_generic):
    # When
    is_generic = inspection.isgeneric(given_type)
    # THen
    assert is_generic == expected_is_generic


@pytest.mark.suite(
    typing_tuple=dict(given_type=t.Tuple, expected_is_subscripted_generic=False),
    typing_tuple_sub=dict(
        given_type=t.Tuple[int], expected_is_subscripted_generic=True
    ),
    generic=dict(given_type=MyGeneric, expected_is_subscripted_generic=False),
    generic_sub=dict(given_type=MyGeneric[int], expected_is_subscripted_generic=True),
)
def test_issubscriptedgeneric(given_type, expected_is_subscripted_generic):
    # When
    is_subscripted_generic = inspection.issubscriptedgeneric(given_type)
    # Then
    assert is_subscripted_generic == expected_is_subscripted_generic


class Slots:
    __slots__ = ("attr",)


@pytest.mark.suite(
    slotted=dict(given_type=Slots, expected_attributes=Slots.__slots__),
    dataclass=dict(given_type=FieldClass, expected_attributes=("field",)),
)
def test_simple_attributes(given_type, expected_attributes):
    # When
    attributes = inspection.simple_attributes(given_type)
    # Then
    assert attributes == expected_attributes


@pytest.mark.suite(
    dict=dict(given_type=dict, expected_params={}),
    mapping=dict(given_type=t.Mapping, expected_params={}),
    typeddict=dict(given_type=TDict, expected_params={"string": mock.ANY}),
    dataclass=dict(given_type=FieldClass, expected_params={"field": mock.ANY}),
    tuple=dict(given_type=(), expected_params={}),
)
def test_safe_get_params(given_type, expected_params):
    # When
    params = inspection.safe_get_params(given_type)
    # Then
    assert params == expected_params
