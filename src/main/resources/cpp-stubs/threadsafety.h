// The thread safety annotations of Clang without a library prefix, i.e., EXCLUSIVE_LOCKS_REQUIRED(cs_main) of Bitcoin Core's threadsafety.h,
// given to CDT before the C++ files that use them, since their headers are not available.
// They are attributes of the declarations, i.e., void f() EXCLUSIVE_LOCKS_REQUIRED(cs_main) {...} or [&]() EXCLUSIVE_LOCKS_REQUIRED(cs) {...},
// so they are defined as empty.
// The lines starting with // are removed before parsing.
#define LOCKABLE
#define SCOPED_LOCKABLE
#define GUARDED_BY(x)
#define PT_GUARDED_BY(x)
#define ACQUIRED_AFTER(...)
#define ACQUIRED_BEFORE(...)
#define EXCLUSIVE_LOCK_FUNCTION(...)
#define SHARED_LOCK_FUNCTION(...)
#define EXCLUSIVE_TRYLOCK_FUNCTION(...)
#define SHARED_TRYLOCK_FUNCTION(...)
#define UNLOCK_FUNCTION(...)
#define LOCK_RETURNED(x)
#define LOCKS_EXCLUDED(...)
#define EXCLUSIVE_LOCKS_REQUIRED(...)
#define SHARED_LOCKS_REQUIRED(...)
#define ASSERT_EXCLUSIVE_LOCK(...)
#define NO_THREAD_SAFETY_ANALYSIS
