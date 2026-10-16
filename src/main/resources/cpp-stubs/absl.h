// The declarations of Abseil that change how CDT parses the code, i.e., absl::StatusOr<std::vector<int>> x = f(); is a declaration and not a comparison,
// given to CDT before the C and C++ files that use Abseil, since its headers are not available.
// The classes are declared without members, the functions are declared as templates, so that they are parsed with template arguments, i.e., absl::down_cast<T*>(x).
// The macros that are attributes of the declarations, i.e., ABSL_GUARDED_BY(mu), are defined as empty, while the macros that are statements or expressions,
// i.e., ABSL_RETURN_IF_ERROR(x) or ABSL_PREDICT_FALSE(x), are not defined, so that they are parsed as calls.
// The lines starting with // are removed before parsing.
namespace absl {
struct Status;
struct string_view;
struct Duration;
struct Time;
struct TimeZone;
struct CivilSecond;
struct Mutex;
struct MutexLock;
struct ReaderMutexLock;
struct WriterMutexLock;
struct ReleasableMutexLock;
struct CondVar;
struct Condition;
struct Notification;
struct BlockingCounter;
struct Barrier;
struct Cord;
struct AlphaNum;
struct Hex;
struct Dec;
struct BitGen;
struct InsecureBitGen;
struct int128;
struct uint128;
struct HashState;
struct ScopedMockLog;
struct SourceLocation;
struct FormatSpec;
struct ParsedFormat;
struct Nullable;
struct Nonnull;
template<class... __rm_t__> struct StatusOr;
template<class... __rm_t__> struct Span;
template<class... __rm_t__> struct flat_hash_map;
template<class... __rm_t__> struct flat_hash_set;
template<class... __rm_t__> struct node_hash_map;
template<class... __rm_t__> struct node_hash_set;
template<class... __rm_t__> struct btree_map;
template<class... __rm_t__> struct btree_set;
template<class... __rm_t__> struct btree_multimap;
template<class... __rm_t__> struct btree_multiset;
template<class... __rm_t__> struct InlinedVector;
template<class... __rm_t__> struct FixedArray;
template<class... __rm_t__> struct FunctionRef;
template<class... __rm_t__> struct AnyInvocable;
template<class... __rm_t__> struct NoDestructor;
template<class... __rm_t__> struct Cleanup;
template<class... __rm_t__> struct Overload;
template<class... __rm_t__> struct Hash;
template<class... __rm_t__> struct optional;
template<class... __rm_t__> struct variant;
template<class... __rm_t__> struct any_invocable;
template<class... __rm_t__> struct LogStreamer;
template<class... __rm_t__> int OkStatus(...);
template<class... __rm_t__> int StrCat(...);
template<class... __rm_t__> int StrAppend(...);
template<class... __rm_t__> int StrFormat(...);
template<class... __rm_t__> int StrJoin(...);
template<class... __rm_t__> int StrSplit(...);
template<class... __rm_t__> int StrReplaceAll(...);
template<class... __rm_t__> int StrContains(...);
template<class... __rm_t__> int StartsWith(...);
template<class... __rm_t__> int EndsWith(...);
template<class... __rm_t__> int StreamFormat(...);
template<class... __rm_t__> int PrintF(...);
template<class... __rm_t__> int FPrintF(...);
template<class... __rm_t__> int SNPrintF(...);
template<class... __rm_t__> int Substitute(...);
template<class... __rm_t__> int SimpleAtoi(...);
template<class... __rm_t__> int SimpleAtof(...);
template<class... __rm_t__> int SimpleAtod(...);
template<class... __rm_t__> int AsciiStrToLower(...);
template<class... __rm_t__> int AsciiStrToUpper(...);
template<class... __rm_t__> int StripAsciiWhitespace(...);
template<class... __rm_t__> int CEscape(...);
template<class... __rm_t__> int CUnescape(...);
template<class... __rm_t__> int PairFormatter(...);
template<class... __rm_t__> int Format(...);
template<class... __rm_t__> int InvalidArgumentError(...);
template<class... __rm_t__> int InternalError(...);
template<class... __rm_t__> int UnimplementedError(...);
template<class... __rm_t__> int NotFoundError(...);
template<class... __rm_t__> int FailedPreconditionError(...);
template<class... __rm_t__> int AlreadyExistsError(...);
template<class... __rm_t__> int ResourceExhaustedError(...);
template<class... __rm_t__> int UnavailableError(...);
template<class... __rm_t__> int CancelledError(...);
template<class... __rm_t__> int PermissionDeniedError(...);
template<class... __rm_t__> int OutOfRangeError(...);
template<class... __rm_t__> int DeadlineExceededError(...);
template<class... __rm_t__> int AbortedError(...);
template<class... __rm_t__> int DataLossError(...);
template<class... __rm_t__> int UnauthenticatedError(...);
template<class... __rm_t__> int UnknownError(...);
template<class... __rm_t__> int IsNotFound(...);
template<class... __rm_t__> int IsInvalidArgument(...);
template<class... __rm_t__> int IsInternal(...);
template<class... __rm_t__> int IsUnimplemented(...);
template<class... __rm_t__> int IsResourceExhausted(...);
template<class... __rm_t__> int IsAlreadyExists(...);
template<class... __rm_t__> int IsFailedPrecondition(...);
template<class... __rm_t__> int IsUnavailable(...);
template<class... __rm_t__> int IsCancelled(...);
template<class... __rm_t__> int MakeSpan(...);
template<class... __rm_t__> int MakeConstSpan(...);
template<class... __rm_t__> int WrapUnique(...);
template<class... __rm_t__> int make_unique(...);
template<class... __rm_t__> int down_cast(...);
template<class... __rm_t__> int bit_cast(...);
template<class... __rm_t__> int implicit_cast(...);
template<class... __rm_t__> int Uniform(...);
template<class... __rm_t__> int HashOf(...);
template<class... __rm_t__> int forward(...);
template<class... __rm_t__> int move(...);
template<class... __rm_t__> int Seconds(...);
template<class... __rm_t__> int Milliseconds(...);
template<class... __rm_t__> int Microseconds(...);
template<class... __rm_t__> int Nanoseconds(...);
template<class... __rm_t__> int Minutes(...);
template<class... __rm_t__> int Hours(...);
template<class... __rm_t__> int InfiniteDuration(...);
template<class... __rm_t__> int ZeroDuration(...);
template<class... __rm_t__> int ParseDuration(...);
template<class... __rm_t__> int FormatDuration(...);
template<class... __rm_t__> int FormatTime(...);
template<class... __rm_t__> int ParseTime(...);
template<class... __rm_t__> int Now(...);
template<class... __rm_t__> int GetCurrentTimeNanos(...);
template<class... __rm_t__> int SleepFor(...);
template<class... __rm_t__> int FromUnixNanos(...);
template<class... __rm_t__> int FromUnixMicros(...);
template<class... __rm_t__> int FromUnixMillis(...);
template<class... __rm_t__> int FromUnixSeconds(...);
template<class... __rm_t__> int ToUnixNanos(...);
template<class... __rm_t__> int ToUnixMicros(...);
template<class... __rm_t__> int ToUnixMillis(...);
template<class... __rm_t__> int ToUnixSeconds(...);
template<class... __rm_t__> int ToInt64Nanoseconds(...);
template<class... __rm_t__> int ToInt64Microseconds(...);
template<class... __rm_t__> int ToInt64Milliseconds(...);
template<class... __rm_t__> int ToInt64Seconds(...);
template<class... __rm_t__> int ToDoubleNanoseconds(...);
template<class... __rm_t__> int ToDoubleMicroseconds(...);
template<class... __rm_t__> int ToDoubleMilliseconds(...);
template<class... __rm_t__> int ToDoubleSeconds(...);
template<class... __rm_t__> int LocalTimeZone(...);
template<class... __rm_t__> int UTCTimeZone(...);
template<class... __rm_t__> int SetVLogLevel(...);
template<class... __rm_t__> int SetMinLogLevel(...);
template<class... __rm_t__> int c_all_of(...);
template<class... __rm_t__> int c_any_of(...);
template<class... __rm_t__> int c_none_of(...);
template<class... __rm_t__> int c_for_each(...);
template<class... __rm_t__> int c_find(...);
template<class... __rm_t__> int c_find_if(...);
template<class... __rm_t__> int c_find_if_not(...);
template<class... __rm_t__> int c_count(...);
template<class... __rm_t__> int c_count_if(...);
template<class... __rm_t__> int c_equal(...);
template<class... __rm_t__> int c_linear_search(...);
template<class... __rm_t__> int c_sort(...);
template<class... __rm_t__> int c_stable_sort(...);
template<class... __rm_t__> int c_reverse(...);
template<class... __rm_t__> int c_reverse_copy(...);
template<class... __rm_t__> int c_copy(...);
template<class... __rm_t__> int c_copy_if(...);
template<class... __rm_t__> int c_transform(...);
template<class... __rm_t__> int c_iota(...);
template<class... __rm_t__> int c_accumulate(...);
template<class... __rm_t__> int c_adjacent_difference(...);
template<class... __rm_t__> int c_min_element(...);
template<class... __rm_t__> int c_max_element(...);
template<class... __rm_t__> int c_minmax_element(...);
template<class... __rm_t__> int c_binary_search(...);
template<class... __rm_t__> int c_lower_bound(...);
template<class... __rm_t__> int c_upper_bound(...);
template<class... __rm_t__> int c_fill(...);
template<class... __rm_t__> int c_remove_if(...);
template<class... __rm_t__> int c_unique(...);
template<class... __rm_t__> int c_contains(...);
template<class... __rm_t__> int c_partition(...);
template<class... __rm_t__> int c_shuffle(...);
template<class... __rm_t__> int c_inner_product(...);
template<class... __rm_t__> int c_set_difference(...);
template<class... __rm_t__> int c_set_intersection(...);
template<class... __rm_t__> int c_set_union(...);
template<class... __rm_t__> int c_includes(...);
template<class... __rm_t__> int c_mismatch(...);
template<class... __rm_t__> int c_search(...);
enum class StatusCode : int { kOk, kCancelled, kUnknown, kInvalidArgument, kDeadlineExceeded, kNotFound, kAlreadyExists, kPermissionDenied, kResourceExhausted, kFailedPrecondition, kAborted, kOutOfRange, kUnimplemented, kInternal, kUnavailable, kDataLoss, kUnauthenticated };
enum class LogSeverity : int { kInfo, kWarning, kError, kFatal };
enum class MockLogDefault { kIgnoreUnexpected, kDisallowUnexpected };
enum PadSpec { kNoPad, kZeroPad2, kZeroPad3, kZeroPad4, kZeroPad5, kZeroPad6, kZeroPad7, kZeroPad8, kZeroPad9, kZeroPad10, kZeroPad11, kZeroPad12, kZeroPad13, kZeroPad14, kZeroPad15, kZeroPad16, kSpacePad2, kSpacePad3, kSpacePad4, kSpacePad5, kSpacePad6, kSpacePad7, kSpacePad8, kSpacePad9, kSpacePad10, kSpacePad11, kSpacePad12, kSpacePad13, kSpacePad14, kSpacePad15, kSpacePad16 };
enum ConstInitType { kConstInit };
}
// the attributes of the declarations
#define ABSL_GUARDED_BY(x)
#define ABSL_PT_GUARDED_BY(x)
#define ABSL_ACQUIRED_AFTER(...)
#define ABSL_ACQUIRED_BEFORE(...)
#define ABSL_EXCLUSIVE_LOCKS_REQUIRED(...)
#define ABSL_SHARED_LOCKS_REQUIRED(...)
#define ABSL_LOCKS_EXCLUDED(...)
#define ABSL_LOCK_RETURNED(x)
#define ABSL_LOCKABLE
#define ABSL_SCOPED_LOCKABLE
#define ABSL_EXCLUSIVE_LOCK_FUNCTION(...)
#define ABSL_SHARED_LOCK_FUNCTION(...)
#define ABSL_UNLOCK_FUNCTION(...)
#define ABSL_EXCLUSIVE_TRYLOCK_FUNCTION(...)
#define ABSL_ASSERT_EXCLUSIVE_LOCK(...)
#define ABSL_NO_THREAD_SAFETY_ANALYSIS
#define ABSL_DEPRECATED(message)
#define ABSL_MUST_USE_RESULT
#define ABSL_CONST_INIT
#define ABSL_ATTRIBUTE_WEAK
#define ABSL_ATTRIBUTE_UNUSED
#define ABSL_ATTRIBUTE_NOINLINE
#define ABSL_ATTRIBUTE_ALWAYS_INLINE
#define ABSL_ATTRIBUTE_LIFETIME_BOUND
#define ABSL_ATTRIBUTE_PURE_FUNCTION
#define ABSL_ATTRIBUTE_CONST_FUNCTION
#define ABSL_ATTRIBUTE_COLD
#define ABSL_ATTRIBUTE_HOT
#define ABSL_ATTRIBUTE_NORETURN
#define ABSL_ATTRIBUTE_NONNULL(...)
#define ABSL_ATTRIBUTE_PACKED
#define ABSL_ATTRIBUTE_REINITIALIZES
