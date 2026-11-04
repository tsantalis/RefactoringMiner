"""Utilities for automatic unmarshalling of inputs according to a callable object's signature.

Examples: Typical Usage
    >>> from typelib import binding
    >>>
    >>> def foo(val: int) -> int:
    ...     return val * 2
    ...
    >>> bound = binding.bind(foo)
    >>> bound("2")
    4
    >>> bound.call("3")
    '33'
"""

from __future__ import annotations

import abc
import dataclasses
import functools
import inspect
import typing as tp

from typelib import unmarshals
from typelib.py import classes, compat, inspection

type BindingT = tp.MutableMapping[str | int, unmarshals.AbstractUnmarshaller]


def bind[**P, R](obj: tp.Callable[P, R]) -> BoundRoutine[P, R]:
    """Create a type-enforced, bound routine for a callable object.

    Note:
        In contrast to [`typelib.binding.wrap`][], this function creates a new,
        type-enforced [`BoundRoutine`][typelib.binding.BoundRoutine] instance. Rather than
        masquerading as the given `obj`, we encapsulate it in the routine
        instance, which is more obvious and provides developers with the ability to
        side-step type enforcement when it is deemed unnecessary, which should be
        most of the time if your code is strongly typed and statically analyzed.

    Tip: TL;DR
        This function returns an object that walks like your duck and quacks like your duck,
        but doesn't look like your duck.

    Args:
        obj: The callable object to bind.
    """
    binding: AbstractBinding[P] = _get_binding(obj)
    routine: BoundRoutine[P, R] = BoundRoutine(
        call=obj,
        binding=binding,
    )
    return routine


def wrap[**P, R](obj: tp.Callable[P, R]) -> tp.Callable[..., R]:
    """Wrap a callable object for runtime type coercion of inputs.

    Note:
        If a class is given, we will attempt to wrap the init method.

    Warning:
        This is a useful feature. It is also very *surprising*. By wrapping a callable
        in this decorator, we end up with *implicit* behavior that's not obvious to the
        caller or a fellow developer.

        You're encouraged to prefer [`typelib.binding.bind`][] for similar
        functionality, less the implicit nature, especially when a class is given.

    Tip: TL;DR
        This function returns an object walks, quacks, and tries to look like your duck.

    Args:
        obj: The callable object to wrap.
             Maybe be a function, a callable class instance, or a class.
    """

    binding: AbstractBinding[P] = _get_binding(obj)

    if inspect.isclass(obj):
        obj.__init__ = wrap(obj.__init__)
        return obj

    @functools.wraps(obj)  # type: ignore[arg-type]
    def binding_wrapper(*args: tp.Any, __binding=binding, **kwargs: tp.Any) -> R:
        bargs, bkwargs = __binding(args, kwargs)
        return obj(*bargs, **bkwargs)

    return binding_wrapper


@classes.slotted(dict=False, weakref=True)
@dataclasses.dataclass
class BoundRoutine[**P, R]:
    """A type-enforced, bound routine for a callable object."""

    call: tp.Callable[P, R]
    """The callable object."""
    binding: AbstractBinding[P]
    """The parameter->type binding."""

    def __call__(self, *args: tp.Any, **kwargs: tp.Any) -> R:
        """Binding an input to the parameters of `call`,

        then call the callable and return the result."""
        bargs, bkwargs = self.binding(args=args, kwargs=kwargs)
        return self.call(*bargs, **bkwargs)


@compat.cache
def _get_binding(obj: tp.Callable) -> AbstractBinding:
    sig = inspection.cached_signature(obj)
    params = sig.parameters
    binding: BindingT = {}
    has_pos_only = False
    has_kwd_only = False
    has_args = False
    has_kwargs = False
    has_pos_or_kwd = False
    max_pos: int | None = None
    varkwd: unmarshals.AbstractUnmarshaller | None = None
    varpos: unmarshals.AbstractUnmarshaller | None = None
    for i, (name, param) in enumerate(params.items()):
        unmarshaller: unmarshals.AbstractUnmarshaller = unmarshals.unmarshaller(
            param.annotation
        )
        binding[name] = binding[i] = unmarshaller
        has_kwd_only = has_kwd_only or param.kind == inspect.Parameter.KEYWORD_ONLY
        has_pos_or_kwd = (
            has_pos_or_kwd or param.kind == inspect.Parameter.POSITIONAL_OR_KEYWORD
        )
        if param.kind == param.POSITIONAL_ONLY:
            has_pos_only = True
            max_pos = i
            continue
        if param.kind == param.VAR_KEYWORD:
            has_kwargs = True
            varkwd = unmarshaller
            continue
        if param.kind == param.VAR_POSITIONAL:
            has_args = True
            max_pos = i - 1
            varpos = unmarshaller

    truth = _Truth(
        has_pos_only=has_pos_only,
        has_kwd_only=has_kwd_only,
        has_args=has_args,
        has_kwargs=has_kwargs,
        has_pos_or_kwd=has_pos_or_kwd,
    )
    binding_cls = _BINDING_CLS_MATRIX[truth]
    return binding_cls(
        binding=binding,
        signature=sig,
        varkwd=varkwd,
        varpos=varpos,
        startpos=max_pos + 1 if max_pos is not None else None,
    )


class AbstractBinding[**P](abc.ABC):
    """The abstract base class for all type-enforced bindings.

    Note:
        "Bindings" are callables which leverage the type annotations in a signature to
        unmarshal inputs.

        We differentiate each subclass based upon the possible combinations of parameter
        kinds:

        - Positional-only arguments
        - Keyword-only arguments
        - Positional-or-Keyword arguments
        - Variable-positional arguments (`*args`)
        - Variable-keyword arguments (`**kwargs`)

        This allows us to micro-optimize the call for each subclass to exactly what is
        necessary for the that combination, which can lead to a significant speedup in
        hot loops.
    """

    __slots__ = ("binding", "signature", "startpos", "varkwd", "varpos")

    def __init__(
        self,
        *,
        signature: inspect.Signature,
        binding: BindingT,
        varkwd: unmarshals.AbstractUnmarshaller | None = None,
        varpos: unmarshals.AbstractUnmarshaller | None = None,
        startpos: int | None = None,
    ):
        """Constructor.

        Args:
            signature: The signature for the binding.
            binding: A mapping of parameter names and positions to unmarshallers.
                     This accounts for positional, keyword, or positional-or-keyword arguments.
            varkwd: The unmarshaller for var-keyword arguments (`**kwargs`).
            varpos: The unmarshaller for var-positional arguments (`*args`).
            startpos: The start position of var-positional arguments (`*args`).
                      This accounts for the fact that var-positional comes after positional-only.
        """
        self.signature = signature
        self.binding = binding
        self.varkwd = varkwd or unmarshals.noop()
        self.varpos = varpos or unmarshals.noop()
        self.startpos = startpos

    def __repr__(self):
        return f"<{self.__class__.__name__}(signature={self.signature})>"

    @abc.abstractmethod
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        """Inspect the given `args` and `kwargs` and unmarshal them.

        Args:
            args: The positional arguments.
            kwargs: The keyword arguments.
        """


class AnyParamKindBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize key attributes
        binding = self.binding
        varpos: unmarshals.AbstractUnmarshaller = self.varpos
        varkwd: unmarshals.AbstractUnmarshaller = self.varkwd
        # Split the supplied args into the positional and the var-args
        #   Implementation note: if there are positional args and var-args,
        #   positional args will always be first.
        posargs = args[: self.startpos]
        varargs = args[self.startpos :]
        # Unmarshal the positional arguments.
        umargs = (
            *(binding[i](v) if i in binding else v for i, v in enumerate(posargs)),
            *(varpos(v) for v in varargs),
        )
        # Unmarshal the keyword arguments.
        umkwargs = {k: binding.get(k, varkwd)(v) for k, v in kwargs.items()}
        return umargs, umkwargs


class PosArgsKwargsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize key attributes
        binding = self.binding
        varpos: unmarshals.AbstractUnmarshaller = self.varpos
        varkwd: unmarshals.AbstractUnmarshaller = self.varkwd
        # Split the supplied args into the positional and the var-args
        #   Implementation note: if there are positional args and var-args,
        #   positional args will always be first.
        posargs = args[: self.startpos]
        varargs = args[self.startpos :]
        umargs = (
            *(binding[i](v) if i in binding else v for i, v in enumerate(posargs)),
            *(varpos(v) for v in varargs),
        )
        umkwargs = {k: varkwd(v) for k, v in kwargs.items()}
        return umargs, umkwargs


class PosKwdKwargsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize key attributes
        binding = self.binding
        varkwd: unmarshals.AbstractUnmarshaller = self.varkwd
        # Unmarshal the args
        umargs = (*(binding[i](v) if i in binding else v for i, v in enumerate(args)),)
        # Unmarshal the keyword arguments.
        umkwargs = {k: binding.get(k, varkwd)(v) for k, v in kwargs.items()}
        return umargs, umkwargs


class PosKwdArgsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize key attributes
        binding = self.binding
        varpos: unmarshals.AbstractUnmarshaller = self.varpos
        # Split the supplied args into the positional and the var-args
        #   Implementation note: if there are positional args and var-args,
        #   positional args will always be first.
        posargs = args[: self.startpos]
        varargs = args[self.startpos :]
        # Unmarshal the positional arguments.
        umargs = (
            *(binding[i](v) if i in binding else v for i, v in enumerate(posargs)),
            *(varpos(v) for v in varargs),
        )
        # Unmarshal the keyword arguments.
        umkwargs = {k: binding[k](v) if k in binding else v for k, v in kwargs.items()}
        return umargs, umkwargs


class PosKwargsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize key attributes
        binding = self.binding
        varkwd: unmarshals.AbstractUnmarshaller = self.varkwd
        # Unmarshal the args
        umargs = (*(binding[i](v) if i in binding else v for i, v in enumerate(args)),)
        # Unmarshal the keyword arguments.
        umkwargs = {k: varkwd(v) for k, v in kwargs.items()}
        return umargs, umkwargs


class PosKwdBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize key attributes
        binding = self.binding
        # Unmarshal the args
        umargs = (*(binding[i](v) if i in binding else v for i, v in enumerate(args)),)
        # Unmarshal the keyword arguments.
        umkwargs = {k: binding[k](v) if k in binding else k for k, v in kwargs.items()}
        return umargs, umkwargs


class PosArgsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize key attributes
        binding = self.binding
        varpos: unmarshals.AbstractUnmarshaller = self.varpos
        # Split the supplied args into the positional and the var-args
        #   Implementation note: if there are positional args and var-args,
        #   positional args will always be first.
        posargs = args[: self.startpos]
        varargs = args[self.startpos :]
        umargs = (
            *(binding[i](v) if i in binding else v for i, v in enumerate(posargs)),
            *(varpos(v) for v in varargs),
        )
        return umargs, kwargs


class PosBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize key attributes
        binding = self.binding
        # Unmarshal the args
        umargs = (*(binding[i](v) if i in binding else v for i, v in enumerate(args)),)
        return umargs, kwargs


class KwdArgsKwargsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize the key attributes
        binding = self.binding
        varpos: unmarshals.AbstractUnmarshaller = self.varpos
        varkwd: unmarshals.AbstractUnmarshaller = self.varkwd
        # Unmarshal the positional args.
        umargs = (*(varpos(v) for v in args),)
        # Unmarshal the keyword args.
        umkwargs = {k: binding.get(k, varkwd)(v) for k, v in kwargs.items()}
        return umargs, umkwargs


class KwdArgsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize the key attributes
        binding = self.binding
        varpos: unmarshals.AbstractUnmarshaller = self.varpos
        # Unmarshal the positional arguments
        umargs = (*(varpos(v) for v in args),)
        # Unmarshal the keyword arguments.
        umkwargs = {k: binding[k](v) if k in binding else k for k, v in kwargs.items()}
        return umargs, umkwargs


class KwdKwargsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        binding = self.binding
        varkwd: unmarshals.AbstractUnmarshaller = self.varkwd
        # Unmarshal the keyword arguments.
        umkwargs = {k: binding.get(k, varkwd)(v) for k, v in kwargs.items()}
        return args, umkwargs


class KwdBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        binding = self.binding
        # Unmarshal the keyword arguments.
        umkwargs = {k: binding[k](v) if k in binding else k for k, v in kwargs.items()}
        return args, umkwargs


class ArgsKwargsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize the key attributes
        varpos: unmarshals.AbstractUnmarshaller = self.varpos
        varkwd: unmarshals.AbstractUnmarshaller = self.varkwd
        # Unmarshal the positional arguments.
        umargs = (*(varpos(v) for v in args),)
        # Unmarshal the keyword arguments.
        umkwargs = {k: varkwd(v) for k, v in kwargs.items()}
        return umargs, umkwargs


class KwargsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize the key attributes
        varkwd: unmarshals.AbstractUnmarshaller = self.varkwd
        # Unmarshal the keyword arguments.
        umkwargs = {k: varkwd(v) for k, v in kwargs.items()}
        return args, umkwargs


class ArgsBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize the key attributes
        varpos: unmarshals.AbstractUnmarshaller = self.varpos
        # Unmarshal the positional arguments.
        umargs = (*(varpos(v) for v in args),)
        return umargs, kwargs


class PosOrKwdBinding[**P](AbstractBinding[P]):
    def __call__(
        self, args: tuple[tp.Any], kwargs: dict[str, tp.Any]
    ) -> tuple[P.args, P.kwargs]:  # type: ignore[valid-type]
        # Localize the key attributes
        binding = self.binding
        # Unmarshal the positional args.
        umargs = (*(binding[i](v) if i in binding else v for i, v in enumerate(args)),)
        # Unmarshal the keyword arguments.
        umkwargs = {k: binding[k](v) if k in binding else k for k, v in kwargs.items()}
        return umargs, umkwargs


class _Truth(tp.NamedTuple):
    """Internal truth entry for determining the binding algorithm.

    Attributes:
        has_pos_only: Whether the signature contains positional-only arguments.
        has_kwd_only: Whether the signature contains keyword-only arguments.
        has_args: Whether the signature contains variadic-positional arguments.
        has_kwargs: Whether the signature contains variadic-keyword arguments.
        has_pos_or_kwd: Whether the signature contains positional-or-keyword arguments.
    """

    has_pos_only: bool = False
    has_kwd_only: bool = False
    has_args: bool = False
    has_kwargs: bool = False
    has_pos_or_kwd: bool = False


_BINDING_CLS_MATRIX: dict[_Truth, type[AbstractBinding]] = {
    _Truth(
        has_pos_only=False,
        has_kwd_only=False,
        has_args=False,
        has_kwargs=False,
        has_pos_or_kwd=False,
    ): PosOrKwdBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=False,
        has_args=False,
        has_kwargs=False,
        has_pos_or_kwd=True,
    ): PosOrKwdBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=False,
        has_args=False,
        has_kwargs=True,
        has_pos_or_kwd=False,
    ): KwargsBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=False,
        has_args=False,
        has_kwargs=True,
        has_pos_or_kwd=True,
    ): PosKwdKwargsBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=False,
        has_args=True,
        has_kwargs=False,
        has_pos_or_kwd=False,
    ): ArgsBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=False,
        has_args=True,
        has_kwargs=False,
        has_pos_or_kwd=True,
    ): PosArgsBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=False,
        has_args=True,
        has_kwargs=True,
        has_pos_or_kwd=False,
    ): ArgsKwargsBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=False,
        has_args=True,
        has_kwargs=True,
        has_pos_or_kwd=True,
    ): ArgsKwargsBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=True,
        has_args=False,
        has_kwargs=False,
        has_pos_or_kwd=False,
    ): KwdBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=True,
        has_args=False,
        has_kwargs=False,
        has_pos_or_kwd=True,
    ): PosOrKwdBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=True,
        has_args=False,
        has_kwargs=True,
        has_pos_or_kwd=False,
    ): KwdKwargsBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=True,
        has_args=False,
        has_kwargs=True,
        has_pos_or_kwd=True,
    ): PosKwdKwargsBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=True,
        has_args=True,
        has_kwargs=False,
        has_pos_or_kwd=False,
    ): KwdArgsBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=True,
        has_args=True,
        has_kwargs=False,
        has_pos_or_kwd=True,
    ): PosOrKwdBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=True,
        has_args=True,
        has_kwargs=True,
        has_pos_or_kwd=False,
    ): KwdArgsKwargsBinding,
    _Truth(
        has_pos_only=False,
        has_kwd_only=True,
        has_args=True,
        has_kwargs=True,
        has_pos_or_kwd=True,
    ): KwdArgsKwargsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=False,
        has_args=False,
        has_kwargs=False,
        has_pos_or_kwd=False,
    ): PosBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=False,
        has_args=False,
        has_kwargs=False,
        has_pos_or_kwd=True,
    ): PosOrKwdBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=False,
        has_args=False,
        has_kwargs=True,
        has_pos_or_kwd=False,
    ): PosKwargsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=False,
        has_args=False,
        has_kwargs=True,
        has_pos_or_kwd=True,
    ): PosKwargsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=False,
        has_args=True,
        has_kwargs=False,
        has_pos_or_kwd=False,
    ): PosArgsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=False,
        has_args=True,
        has_kwargs=False,
        has_pos_or_kwd=True,
    ): PosArgsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=False,
        has_args=True,
        has_kwargs=True,
        has_pos_or_kwd=False,
    ): PosArgsKwargsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=False,
        has_args=True,
        has_kwargs=True,
        has_pos_or_kwd=True,
    ): AnyParamKindBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=True,
        has_args=False,
        has_kwargs=False,
        has_pos_or_kwd=False,
    ): PosKwdBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=True,
        has_args=False,
        has_kwargs=False,
        has_pos_or_kwd=True,
    ): PosKwdKwargsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=True,
        has_args=False,
        has_kwargs=True,
        has_pos_or_kwd=False,
    ): PosArgsKwargsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=True,
        has_args=False,
        has_kwargs=True,
        has_pos_or_kwd=True,
    ): PosKwdKwargsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=True,
        has_args=True,
        has_kwargs=False,
        has_pos_or_kwd=False,
    ): PosKwdArgsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=True,
        has_args=True,
        has_kwargs=False,
        has_pos_or_kwd=True,
    ): PosKwdArgsBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=True,
        has_args=True,
        has_kwargs=True,
        has_pos_or_kwd=False,
    ): AnyParamKindBinding,
    _Truth(
        has_pos_only=True,
        has_kwd_only=True,
        has_args=True,
        has_kwargs=True,
        has_pos_or_kwd=True,
    ): AnyParamKindBinding,
}
