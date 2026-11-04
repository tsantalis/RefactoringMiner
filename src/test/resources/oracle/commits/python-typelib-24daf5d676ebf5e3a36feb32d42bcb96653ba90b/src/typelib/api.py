from __future__ import annotations

from typelib.codecs import Codec, DecoderT, EncoderT, codec
from typelib.marshals import AbstractMarshaller, marshal, marshaller
from typelib.py import compat, refs
from typelib.unmarshals import AbstractUnmarshaller, unmarshal, unmarshaller

__all__ = (
    "AbstractMarshaller",
    "AbstractUnmarshaller",
    "Codec",
    "codec",
    "compat",
    "decode",
    "encode",
    "marshal",
    "marshaller",
    "refs",
    "unmarshal",
    "unmarshaller",
)


def encode[T](
    value: T,
    *,
    t: type[T] | refs.ForwardRef | str | None = None,
    encoder: EncoderT = compat.json.dumps,
) -> bytes:
    """Encode a value into a bytes object.

    Args:
        value: The value to encode.
        t: A type hint for resolving the marshaller.
        encoder: A callable that takes a value and returns a bytes object.
    """
    marshalled = marshal(value=value, t=t)
    encoded = encoder(marshalled)
    return encoded


def decode[T](
    t: type[T] | refs.ForwardRef | str,
    value: bytes,
    *,
    decoder: DecoderT = compat.json.loads,
) -> T:
    """Decode a bytes object into an instance of `t`.

    Args:
        t: A type hint for resolving the unmarshaller.
        value: The value to decode.
        decoder: A callable that takes a bytes object and returns a Python value.
    """
    decoded = decoder(value)
    unmarshalled = unmarshal(t=t, value=decoded)
    return unmarshalled
