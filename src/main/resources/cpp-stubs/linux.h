// The macros of the Linux kernel that change how CDT parses the code, given to CDT before the C files that include kernel headers, since its headers are not available.
// The macros that are attributes of the declarations, i.e., __init in void __init f(void) or the lock annotation __releases(lock) between a function declarator and its body,
// are defined as empty, while the macros that are statements or expressions, i.e., list_for_each_entry(p, head, member) or WARN_ON(x), are not defined, so that they are parsed as calls.
// The lines starting with // are removed before parsing.
#define __init
#define __exit
#define __initdata
#define __initconst
#define __exitdata
#define __ref
#define __weak
#define __always_inline inline
#define noinline
#define __noinline
#define __cold
#define __hot
#define __pure
#define __noreturn
#define __must_check
#define __maybe_unused
#define __always_unused
#define __used
#define __visible
#define __flatten
#define __latent_entropy
#define __read_mostly
#define __ro_after_init
#define __cacheline_aligned
#define __cacheline_aligned_in_smp
#define __packed
#define __user
#define __kernel
#define __iomem
#define __percpu
#define __rcu
#define __force
#define __nocast
#define __safe
#define __private
#define __bitwise
#define __kprobes
#define notrace
#define asmlinkage
#define __aligned(x)
#define __section(x)
#define __alias(x)
#define __printf(a, b)
#define __scanf(a, b)
#define __acquires(x)
#define __releases(x)
#define __must_hold(x)
#define __cond_acquires(...)
#define __cond_releases(...)
// The macros giving a string literal concatenated with other literals, which cannot be parsed as calls, i.e., "ipw2100-" __stringify(1) ".fw", are defined like the kernel does.
#define __stringify_1(x...) #x
#define __stringify(x...) __stringify_1(x)
// The macros with a type argument, which cannot be parsed as calls, i.e., container_of(ptr, struct task_struct, rcu), are defined like the kernel does, as casts of the pointer.
#define container_of(ptr, type, member) ((type *)(ptr))
#define container_of_const(ptr, type, member) ((type *)(ptr))
#define list_entry(ptr, type, member) ((type *)(ptr))
#define list_first_entry(ptr, type, member) ((type *)(ptr))
#define list_last_entry(ptr, type, member) ((type *)(ptr))
#define list_first_entry_or_null(ptr, type, member) ((type *)(ptr))
#define hlist_entry(ptr, type, member) ((type *)(ptr))
#define hlist_entry_safe(ptr, type, member) ((type *)(ptr))
#define llist_entry(ptr, type, member) ((type *)(ptr))
#define rb_entry(ptr, type, member) ((type *)(ptr))
#define rb_entry_safe(ptr, type, member) ((type *)(ptr))
#define sizeof_field(type, member) sizeof(type)
