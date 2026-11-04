from __future__ import annotations

import datetime
import decimal
import fractions
import pathlib
import re
import typing
import uuid

import pytest

from typelib.py import compat, refs
from typelib.unmarshals import api

from tests import models


@pytest.mark.suite(
    forwardref=dict(
        given_type=refs.forwardref("decimal.Decimal"),
        given_input="1.0",
        expected_output=decimal.Decimal("1.0"),
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
        given_input="2",
        expected_output=2,
    ),
    union=dict(
        given_type=typing.Union[int, str],
        given_input="2",
        expected_output=2,
    ),
    datetime=dict(
        given_type=datetime.datetime,
        given_input=0,
        expected_output=datetime.datetime.fromtimestamp(0, datetime.UTC),
    ),
    date=dict(
        given_type=datetime.date,
        given_input=0,
        expected_output=datetime.datetime.fromtimestamp(0, datetime.UTC).date(),
    ),
    time=dict(
        given_type=datetime.time,
        given_input=0,
        expected_output=datetime.datetime.fromtimestamp(0, tz=datetime.UTC)
        .time()
        .replace(tzinfo=datetime.UTC),
    ),
    timedelta=dict(
        given_type=datetime.timedelta,
        given_input=0,
        expected_output=datetime.timedelta(seconds=0),
    ),
    uuid=dict(
        given_type=uuid.UUID,
        given_input=0,
        expected_output=uuid.UUID(int=0),
    ),
    pattern=dict(
        given_type=re.Pattern,
        given_input="1",
        expected_output=re.compile("1"),
    ),
    path=dict(
        given_type=pathlib.Path,
        given_input="/path/to/file",
        expected_output=pathlib.Path("/path/to/file"),
    ),
    decimal=dict(
        given_type=decimal.Decimal,
        given_input="1.0",
        expected_output=decimal.Decimal("1.0"),
    ),
    fraction=dict(
        given_type=fractions.Fraction,
        given_input="1/2",
        expected_output=fractions.Fraction("1/2"),
    ),
    string=dict(
        given_type=str,
        given_input=1,
        expected_output="1",
    ),
    bytes=dict(
        given_type=bytes,
        given_input=1,
        expected_output=b"1",
    ),
    bytearray=dict(
        given_type=bytearray,
        given_input=1,
        expected_output=bytearray(b"1"),
    ),
    memoryview=dict(
        given_type=memoryview,
        given_input="foo",
        expected_output=memoryview(b"foo"),
    ),
    typeddict=dict(
        given_type=models.TDict,
        given_input={"field": 1, "value": "2"},
        expected_output=models.TDict(field="1", value=2),
    ),
    direct_recursive=dict(
        given_type=models.RecursiveType,
        given_input={"cycle": {"cycle": {}}},
        expected_output=models.RecursiveType(
            cycle=models.RecursiveType(cycle=models.RecursiveType())
        ),
    ),
    indirect_recursive=dict(
        given_type=models.IndirectCycleType,
        given_input={"indirect": {"cycle": {"indirect": {}}}},
        expected_output=models.IndirectCycleType(
            indirect=models.CycleType(
                cycle=models.IndirectCycleType(indirect=models.CycleType())
            )
        ),
    ),
    type_alias_type=dict(
        given_type=compat.TypeAliasType("IntList", "list[int]"),
        given_input='["1", "2"]',
        expected_output=[1, 2],
    ),
    enum_type=dict(
        given_type=models.GivenEnum,
        given_input="one",
        expected_output=models.GivenEnum.one,
    ),
    union_std_lib=dict(
        given_type=models.UnionSTDLib,
        given_input={"timestamp": 0},
        expected_output=models.UnionSTDLib(
            timestamp=datetime.datetime.fromtimestamp(0, datetime.UTC)
        ),
    ),
    optional_none=dict(
        given_type=typing.Optional[int | str],
        given_input=None,
        expected_output=None,
    ),
    attrib_conflict=dict(
        given_type=models.Parent,
        given_input={"intersection": {"a": 0}, "child": {"intersection": {"b": 0}}},
        expected_output=models.Parent(
            intersection=models.ParentIntersect(a=0),
            child=models.Child(intersection=models.ChildIntersect(b=0)),
        ),
    ),
    nested_type_alias=dict(
        given_type=models.NestedTypeAliasType,
        given_input={"alias": ["1"]},
        expected_output=models.NestedTypeAliasType(alias=[1]),
    ),
    recursive_alias=dict(
        given_type=models.RecursiveAlias,
        given_input={"cycle": {"cycle": {"cycle": "1"}}},
        expected_output={"cycle": {"cycle": {"cycle": 1}}},
    ),
    subscripted_generic=dict(
        given_type=models.DataGeneric[str, int],
        given_input={"field": "data", "value": "1"},
        expected_output=models.DataGeneric(field="data", value=1),
    ),
    unsubscripted_generic=dict(
        given_type=models.DataGeneric,
        given_input={"field": "data", "value": "1"},
        expected_output=models.DataGeneric(field="data", value=1),
    ),
    nested_generic=dict(
        given_type=models.NestedGeneric[int],
        given_input={"gen": {"field": "1"}},
        expected_output=models.NestedGeneric(gen=models.SimpleGeneric(field=1)),
    ),
)
def test_unmarshal(given_type, given_input, expected_output):
    # When
    output = api.unmarshal(given_type, given_input)
    # Then
    assert output == expected_output
