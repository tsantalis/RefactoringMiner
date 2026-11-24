// The macros and keywords of Qt, given to CDT before the C++ files that use Qt, since its headers are not available.
// The macros of the class bodies and the declarations, i.e., Q_OBJECT, Q_PROPERTY(...) or Q_DECLARE_METATYPE(T), are defined as empty,
// like the export and attribute macros, and the keywords of the access specifiers, i.e., public slots: and signals:, are defined as in Qt.
// The macros used as statements or expressions, i.e., Q_UNUSED(x); or Q_D(Class);, stay undefined, so they parse as calls.
// The lines starting with // are removed before parsing.
#define Q_OBJECT
#define Q_GADGET
#define Q_NAMESPACE
#define Q_NAMESPACE_EXPORT(...)
#define Q_PROPERTY(...)
#define Q_ENUM(x)
#define Q_ENUMS(x)
#define Q_ENUM_NS(x)
#define Q_FLAG(x)
#define Q_FLAGS(x)
#define Q_FLAG_NS(x)
#define Q_INTERFACES(...)
#define Q_CLASSINFO(name, value)
#define Q_PLUGIN_METADATA(...)
#define Q_DISABLE_COPY(Class)
#define Q_DISABLE_COPY_MOVE(Class)
#define Q_DECLARE_PRIVATE(Class)
#define Q_DECLARE_PRIVATE_D(Dptr, Class)
#define Q_DECLARE_PUBLIC(Class)
#define Q_DECLARE_METATYPE(Type)
#define Q_DECLARE_FLAGS(Flags, Enum)
#define Q_DECLARE_OPERATORS_FOR_FLAGS(Flags)
#define Q_DECLARE_INTERFACE(Class, Iid)
#define Q_DECLARE_TYPEINFO(Type, Flags)
#define Q_SIGNALS public
#define Q_SLOTS
#define Q_SIGNAL
#define Q_SLOT
#define Q_EMIT
#define Q_INVOKABLE
#define Q_SCRIPTABLE
#define Q_DECL_EXPORT
#define Q_DECL_IMPORT
#define Q_DECL_DEPRECATED
#define Q_REQUIRED_RESULT
#define Q_DECL_OVERRIDE override
#define Q_DECL_FINAL final
#define Q_DECL_CONSTEXPR constexpr
#define Q_DECL_NOEXCEPT noexcept
#define Q_DECL_NOTHROW noexcept
#define signals public
#define slots
#define emit
