from __future__ import annotations

import datetime
import decimal
import fractions
import pathlib
import re
import typing
import uuid

import pytest

from typelib.marshals import api
from typelib.py import compat, refs

from tests import models


@pytest.mark.suite(
    forwardref=dict(
        given_type=refs.forwardref("decimal.Decimal"),
        given_input=decimal.Decimal("1.0"),
        expected_output="1.0",
    ),
    unresolvable=dict(
        given_type=...,
        given_input="foo",
        expected_output="foo",
    ),
    nonetype=dict(
        given_type=None,
        given_input=None,
        expected_output=None,
    ),
    literal=dict(
        given_type=typing.Literal[1, 2, 3],
        given_input=2,
        expected_output=2,
    ),
    union=dict(
        given_type=typing.Union[int, str],
        given_input=2,
        expected_output=2,
    ),
    optional_none=dict(
        given_type=typing.Optional[int | str],
        given_input=None,
        expected_output=None,
    ),
    datetime=dict(
        given_type=datetime.datetime,
        given_input=datetime.datetime.fromtimestamp(0, datetime.UTC),
        expected_output=datetime.datetime.fromtimestamp(0, datetime.UTC).isoformat(),
    ),
    date=dict(
        given_type=datetime.date,
        given_input=datetime.datetime.fromtimestamp(0, datetime.UTC).date(),
        expected_output=datetime.datetime.fromtimestamp(0, datetime.UTC)
        .date()
        .isoformat(),
    ),
    time=dict(
        given_type=datetime.time,
        given_input=datetime.datetime.fromtimestamp(0, tz=datetime.UTC)
        .time()
        .replace(tzinfo=datetime.UTC),
        expected_output=datetime.datetime.fromtimestamp(0, tz=datetime.UTC)
        .time()
        .replace(tzinfo=datetime.UTC)
        .isoformat(),
    ),
    timedelta=dict(
        given_type=datetime.timedelta,
        given_input=datetime.timedelta(seconds=1),
        expected_output="PT1S",
    ),
    uuid=dict(
        given_type=uuid.UUID,
        given_input=uuid.UUID(int=0),
        expected_output=str(uuid.UUID(int=0)),
    ),
    pattern=dict(
        given_type=re.Pattern,
        given_input=re.compile("1"),
        expected_output="1",
    ),
    path=dict(
        given_type=pathlib.Path,
        given_input=pathlib.Path("/path/to/file"),
        expected_output=str(pathlib.Path("/path/to/file")),
    ),
    decimal=dict(
        given_type=decimal.Decimal,
        given_input=decimal.Decimal("1.0"),
        expected_output="1.0",
    ),
    fraction=dict(
        given_type=fractions.Fraction,
        given_input=fractions.Fraction("1/2"),
        expected_output="1/2",
    ),
    string=dict(
        given_type=str,
        given_input="1",
        expected_output="1",
    ),
    bytes=dict(
        given_type=bytes,
        given_input=b"1",
        expected_output=b"1",
    ),
    bytearray=dict(
        given_type=bytearray,
        given_input=bytearray(b"1"),
        expected_output=bytearray(b"1"),
    ),
    memoryview=dict(
        given_type=memoryview,
        given_input=memoryview(b"foo"),
        expected_output=memoryview(b"foo"),
    ),
    typeddict=dict(
        given_type=models.TDict,
        given_input={"field": "1", "value": 2},
        expected_output={"field": "1", "value": 2},
    ),
    direct_recursive=dict(
        given_type=models.RecursiveType,
        given_input=models.RecursiveType(
            cycle=models.RecursiveType(cycle=models.RecursiveType())
        ),
        expected_output={"cycle": {"cycle": {"cycle": None}}},
    ),
    indirect_recursive=dict(
        given_type=models.IndirectCycleType,
        given_input=models.IndirectCycleType(
            indirect=models.CycleType(
                cycle=models.IndirectCycleType(indirect=models.CycleType())
            )
        ),
        expected_output={"indirect": {"cycle": {"indirect": {"cycle": None}}}},
    ),
    enum_type=dict(
        given_type=models.GivenEnum,
        given_input=models.GivenEnum.one,
        expected_output=models.GivenEnum.one.value,
    ),
    attrib_conflict=dict(
        given_type=models.Parent,
        given_input=models.Parent(
            intersection=models.ParentIntersect(a=0),
            child=models.Child(intersection=models.ChildIntersect(b=0)),
        ),
        expected_output={"intersection": {"a": 0}, "child": {"intersection": {"b": 0}}},
    ),
    nested_type_alias=dict(
        given_type=models.NestedTypeAliasType,
        given_input=models.NestedTypeAliasType(alias=[1]),
        expected_output={"alias": [1]},
    ),
    recursive_alias=dict(
        given_type=models.RecursiveAlias,
        given_input={"cycle": {"cycle": {"cycle": 1}}},
        expected_output={"cycle": {"cycle": {"cycle": 1}}},
    ),
    subscripted_generic=dict(
        given_type=models.DataGeneric[str, int],
        given_input=models.DataGeneric(field="data", value=1),
        expected_output={"field": "data", "value": 1},
    ),
    unsubscripted_generic=dict(
        given_type=models.DataGeneric,
        given_input=models.DataGeneric(field="data", value=1),
        expected_output={"field": "data", "value": 1},
    ),
    nested_generic=dict(
        given_type=models.NestedGeneric[int],
        given_input=models.NestedGeneric(gen=models.SimpleGeneric(field=1)),
        expected_output={"gen": {"field": 1}},
    ),
    legacy_subscripted_generic=dict(
        given_type=models.LegacyDataGeneric[str, int],
        given_input=models.LegacyDataGeneric(field="data", value=1),
        expected_output={"field": "data", "value": 1},
    ),
    legacy_unsubscripted_generic=dict(
        given_type=models.LegacyDataGeneric,
        given_input=models.LegacyDataGeneric(field="data", value=1),
        expected_output={"field": "data", "value": 1},
    ),
    legacy_nested_generic=dict(
        given_type=models.LegacyNestedGeneric[int],
        given_input=models.LegacyNestedGeneric(gen=models.LegacySimpleGeneric(field=1)),
        expected_output={"gen": {"field": 1}},
    ),
)
def test_marshal(given_type, given_input, expected_output):
    # When
    output = api.marshal(given_input, t=given_type)
    # Then
    assert output == expected_output


def test_type_alias_type_marshal():
    # Given
    # Can't reliably test the `type` statement till 3.12 is the min version.
    # type IntList = list[int]
    IntList = compat.TypeAliasType("IntList", "list[int]")
    given_input = [1, 2]
    expected_output = [1, 2]
    # When
    output = api.marshal(given_input, t=IntList)
    # Then
    assert output == expected_output
