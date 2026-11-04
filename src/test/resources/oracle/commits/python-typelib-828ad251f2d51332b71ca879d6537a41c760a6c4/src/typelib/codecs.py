"""Interfaces for managing type-enforced wire protocols (codecs)."""

from __future__ import annotations

import dataclasses
import typing as t

from typelib import marshals, serdes, unmarshals
from typelib.py import classes, compat, inspection

__all__ = ("Codec", "codec")


T = t.TypeVar("T")


@compat.cache
def codec(
    t: type[T],
    *,
    marshaller: marshals.AbstractMarshaller[T] | None = None,
    unmarshaller: unmarshals.AbstractUnmarshaller[T] | None = None,
    encoder: EncoderT = compat.json.dumps,
    decoder: DecoderT = compat.json.loads,
    codec_cls: type[CodecT[T]] | None = None,
) -> CodecT[T]:
    """Factory function for creating a [`Codec`][typelib.Codec] instance.

    Note:
        In the simplest case, all that needs be provided is the first parameter (`t`). We will
        generate a marshaller, unmarshaller and build a codec. However, we provide ample
        means for customization:

        - You can pass in a subclass of [`Codec`][typelib.codecs.Codec] to `codec_cls`.
        - You may supply custom `marshaller` or `unmarshaller` callables - we will generate
          one using the high-level APIs from [`marshals`][typelib.marshals] and
          [`unmarshals`][typelib.unmarshals] if not supplied.
        - The `encoder` and `decoder` default to JSON, using either
          stdlib [`json`][] or [`orjson`](https://github.com/ijl/orjson){.external}
          if available. You may provide custom `encoder` and `decoder` callables, the only
          requirement is they ser/des to/from `bytes`.

        /// tip
        If you installed the `json` extra when you installed this library, then you have
        installed [`orjson`](https://github.com/ijl/orjson){.external}.
        ///

    Args:
        t: The type to create the interchange protocol for.
        marshaller: The marshaller used to marshal inputs into the associated type. (optional)
        unmarshaller: The unmarshaller used to unmarshal inputs into the associated type. (optional)
        encoder: The encoder for encoding data for over-the-wire (defaults to JSON).
        decoder: The decoder for decoding data from over-the-wire (defaults to JSON).
        codec_cls: The codec class definition, if overloading (optional).

    """
    marshal = marshaller or marshals.marshaller(t=t)
    unmarshal = unmarshaller or unmarshals.unmarshaller(t=t)
    cls = codec_cls or Codec
    if inspection.isbytestype(t):
        cdc = cls(
            marshal=marshal,
            unmarshal=unmarshal,
            encoder=lambda v: v,  # type: ignore[arg-type,return-value]
            decoder=lambda v: v,  # type: ignore[arg-type,return-value]
        )
        return cdc
    cdc = cls(
        marshal=marshal,
        unmarshal=unmarshal,
        encoder=encoder,
        decoder=decoder,
    )
    return cdc


@classes.slotted(dict=False, weakref=False)
@dataclasses.dataclass(frozen=True)
class Codec(t.Generic[T]):
    """A standard wire protocol (codec).

    This codec enables you to directly encode and decode your data model into your wire protocol.
    """

    marshal: marshals.AbstractMarshaller[T]
    """The marshaller used to convert an instance of `T` to a serializable object."""
    unmarshal: unmarshals.AbstractUnmarshaller[T]
    """The unmarshaller used to convert a deserialized object into an instance of `T`."""
    encoder: EncoderT
    """The encoder used to serialize a marshalled `T` into bytes."""
    decoder: DecoderT
    """The decoder used to deserialize a bytes-like object into a Python data structure for marshalling into `T`."""

    def encode(self, value: T) -> bytes:
        """Encode an instance of `T` to bytes.

        We will first marshal the given instance using the
        [`marshal`][typelib.Codec.marshal], then encode the marshalled data
        into bytes using the [`encoder`][typelib.Codec.encoder].

        Args:
            value: The instance to encode.
        """
        marshalled = self.marshal(value)
        encoded = self.encoder(marshalled)
        return encoded

    def decode(self, value: bytes) -> T:
        """Decode an instance of `T` from bytes.

        We will first decode the data from bytes using the
        [`decoder`][typelib.Codec.decoder], then unmarshal the data into an
        instance of `T` using [`unmarshal`][typelib.Codec.unmarshal].

        Args:
            value: The bytes to decode.
        """
        decoded = self.decoder(value)
        unmarshalled = self.unmarshal(decoded)
        return unmarshalled


CodecT = compat.TypeAliasType("CodecT", Codec, type_params=(T,))
"""Generic type alias with an upper bound of [`Codec`][typelib.Codec]."""
EncoderT: t.TypeAlias = t.Callable[[serdes.MarshalledValueT], bytes]
"""Protocol for a wire serializer."""
DecoderT: t.TypeAlias = t.Callable[[bytes], serdes.MarshalledValueT]
"""Protocol for a wire deserializer."""
