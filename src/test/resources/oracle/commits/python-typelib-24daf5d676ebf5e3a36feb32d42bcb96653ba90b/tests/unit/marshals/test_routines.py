from __future__ import annotations

import datetime
import decimal
import fractions
import pathlib
import re
import typing
import uuid

import pytest

from typelib.marshals import routines

from tests import models


@pytest.mark.suite(
    bytes=dict(given_input=b"1", expected_output=b"1"),
)
def test_bytes_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.BytesMarshaller(bytes, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    string=dict(given_input="1", expected_output="1"),
    number=dict(given_input=1, expected_output="1"),
    bool=dict(given_input=True, expected_output="True"),
)
def test_str_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.StringMarshaller(str, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    decimal=dict(given_input=decimal.Decimal("1.0"), expected_output="1.0"),
)
def test_decimal_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.DecimalMarshaller(decimal.Decimal, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    fraction=dict(given_input=fractions.Fraction("1/2"), expected_output="1/2"),
)
def test_fraction_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.FractionMarshaller(fractions.Fraction, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    uuid=dict(given_input=uuid.UUID(int=0), expected_output=str(uuid.UUID(int=0))),
)
def test_uuid_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.UUIDMarshaller(uuid.UUID, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    path=dict(
        given_input=pathlib.Path("/path/to/file"),
        expected_output=str(pathlib.Path("/path/to/file")),
    ),
)
def test_path_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.PathMarshaller(pathlib.Path, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    pattern=dict(given_input=re.compile("1"), expected_output="1"),
)
def test_pattern_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.PatternMarshaller(re.Pattern, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    date=dict(
        given_input=datetime.date(1969, 12, 31),
        expected_output=datetime.date(1969, 12, 31).isoformat(),
    ),
)
def test_date_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.DateMarshaller(datetime.date, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    datetime=dict(
        given_input=datetime.datetime(1969, 12, 31),
        expected_output=datetime.datetime(1969, 12, 31).isoformat(),
    ),
)
def test_datetime_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.DateTimeMarshaller(datetime.datetime, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    time=dict(
        given_input=datetime.time(tzinfo=datetime.UTC),
        expected_output="00:00:00+00:00",
    ),
)
def test_time_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.TimeMarshaller(datetime.time, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    timedelta=dict(given_input=datetime.timedelta(seconds=1), expected_output="PT1S"),
)
def test_timedelta_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.TimeDeltaMarshaller(datetime.timedelta, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    dict=dict(
        given_input={"field": "value"},
        expected_output={"field": "value"},
    ),
)
def test_mapping_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.MappingMarshaller(typing.Mapping, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    list=dict(
        given_input=["field", "value"],
        expected_output=["field", "value"],
    ),
    tuple=dict(
        given_input=("field", "value"),
        expected_output=["field", "value"],
    ),
)
def test_iterable_marshaller(given_input, expected_output):
    # Given
    given_marshaller = routines.IterableMarshaller(typing.Iterable, {})
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    integer=dict(
        given_input=1,
        given_literal=typing.Literal[1],
        given_context={},
        expected_output=1,
    ),
)
def test_literal_marshaller(given_input, given_literal, given_context, expected_output):
    # Given
    given_marshaller = routines.LiteralMarshaller(given_literal, given_context)
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    bytes_number=dict(
        given_input=b"1",
        given_union=typing.Union[int, str],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
            str: routines.StringMarshaller(str, {}),
        },
        expected_output=1,
    ),
    string_number=dict(
        given_input="1",
        given_union=typing.Union[int, str],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
            str: routines.StringMarshaller(str, {}),
        },
        expected_output=1,
    ),
    string=dict(
        given_input="string",
        given_union=typing.Union[int, str],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
            str: routines.StringMarshaller(str, {}),
        },
        expected_output="string",
    ),
    integer=dict(
        given_input=1,
        given_union=typing.Union[int, str],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
            str: routines.StringMarshaller(str, {}),
        },
        expected_output=1,
    ),
    float=dict(
        given_input=1.0,
        given_union=typing.Union[int, str],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
            str: routines.StringMarshaller(str, {}),
        },
        expected_output=1,
    ),
    optional_date_none=dict(
        given_input=None,
        given_union=typing.Optional[datetime.date],
        given_context={
            datetime.date: routines.DateMarshaller(datetime.date, {}),
            type(None): routines.NoOpMarshaller(type(None), {}),
        },
        expected_output=None,
    ),
    optional_date_date=dict(
        given_input=datetime.date.today(),
        given_union=typing.Optional[datetime.date],
        given_context={
            datetime.date: routines.DateMarshaller(datetime.date, {}),
            type(None): routines.NoOpMarshaller(type(None), {}),
        },
        expected_output=datetime.date.today().isoformat(),
    ),
)
def test_union_marshaller(given_input, given_union, given_context, expected_output):
    # Given
    given_marshaller = routines.UnionMarshaller(given_union, given_context)
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    dict_literal=dict(
        given_input={"field": "1"},
        given_mapping=typing.Mapping[str, int],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
            str: routines.StringMarshaller(str, {}),
        },
        expected_output={"field": 1},
    ),
)
def test_subscripted_mapping_marshaller(
    given_input, given_mapping, given_context, expected_output
):
    # Given
    given_marshaller = routines.SubscriptedMappingMarshaller(
        given_mapping, given_context
    )
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    generic_iterable=dict(
        given_iterable=typing.Iterable[int],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
        },
        expected_output=[2, 1],
    ),
    list=dict(
        given_iterable=list[int],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
        },
        expected_output=[2, 1],
    ),
    tuple=dict(
        given_iterable=tuple[int, ...],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
        },
        expected_output=[2, 1],
    ),
    set=dict(
        given_iterable=set[int],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
        },
        expected_output=[2, 1],
    ),
)
@pytest.mark.suite(
    tuple=dict(
        given_input=("2", "1"),
    ),
    list=dict(
        given_input=["2", "1"],
    ),
)
def test_subscripted_iterable_marshaller(
    given_input, given_iterable, given_context, expected_output
):
    # Given
    given_marshaller = routines.SubscriptedIterableMarshaller(
        given_iterable, given_context
    )
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    tuple=dict(
        given_input=("field", 1),
        given_tuple=tuple[str, int],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
            str: routines.StringMarshaller(str, {}),
        },
        expected_output=["field", 1],
    ),
    list=dict(
        given_input=["field", 1],
        given_tuple=tuple[str, int],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
            str: routines.StringMarshaller(str, {}),
        },
        expected_output=["field", 1],
    ),
    extra_item=dict(
        given_input=["field", 1, "extra"],
        given_tuple=tuple[str, int],
        given_context={
            int: routines.IntegerMarshaller(int, {}),
            str: routines.StringMarshaller(str, {}),
        },
        expected_output=["field", 1],
    ),
)
def test_fixed_tuple_marshaller(
    given_input, given_tuple, given_context, expected_output
):
    # Given
    given_marshaller = routines.FixedTupleMarshaller(given_tuple, given_context)
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


@pytest.mark.suite(
    context=dict(
        given_context={
            int: routines.IntegerMarshaller(int, {}, var="value"),
            str: routines.StringMarshaller(str, {}, var="field"),
        },
        expected_output=dict(field="data", value=1),
    ),
)
@pytest.mark.suite(
    dataclass=dict(
        given_cls=models.Data,
        given_input=models.Data(field="data", value=1),
    ),
    vanilla=dict(
        given_cls=models.Vanilla,
        given_input=models.Vanilla(field="data", value=1),
    ),
    vanilla_with_hints=dict(
        given_cls=models.VanillaWithHints,
        given_input=models.VanillaWithHints(field="data", value=1),
    ),
    named_tuple=dict(
        given_cls=models.NTuple,
        given_input=models.NTuple(field="data", value=1),
    ),
    typed_dict=dict(
        given_cls=models.TDict,
        given_input=models.TDict(field="data", value=1),
    ),
    generic=dict(
        given_cls=models.DataGeneric,
        given_input=models.DataGeneric(field="data", value=1),
    ),
    legacy_generic=dict(
        given_cls=models.LegacyDataGeneric,
        given_input=models.LegacyDataGeneric(field="data", value=1),
    ),
)
def test_structured_type_marshaller(
    given_input, given_cls, given_context, expected_output
):
    # Given
    given_marshaller = routines.StructuredTypeMarshaller(given_cls, given_context)
    # When
    output = given_marshaller(given_input)
    # Then
    assert output == expected_output


def test_invalid_literal():
    # Given
    given_marshaller = routines.LiteralMarshaller(typing.Literal[1], {})
    given_value = 2
    expected_exception = ValueError
    # When/Then
    with pytest.raises(expected_exception):
        given_marshaller(given_value)


def test_invalid_union():
    # Given
    given_marshaller = routines.UnionMarshaller(
        typing.Union[int, float],
        {
            int: routines.IntegerMarshaller(int, {}),
            float: routines.FloatMarshaller(float, {}),
        },
    )
    given_value = "value"
    expected_exception = ValueError
    # When/Then
    with pytest.raises(expected_exception):
        given_marshaller(given_value)


def test_enum_marshaller():
    # Given
    given_marshaller = routines.EnumMarshaller(models.GivenEnum, {})
    given_value = models.GivenEnum.one
    expected_value = models.GivenEnum.one.value
    # When
    unmarshalled = given_marshaller(given_value)
    # Then
    assert unmarshalled == expected_value
