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
#define __meminit
#define __meminitdata
#define __meminitconst
#define __init_or_module
#define __initdata_or_module
#define __init_refok
#define __sched
#define noinstr
#define __cpuinit
// The system calls, i.e., SYSCALL_DEFINE1(unshare, unsigned long, unshare_flags) {...}, are defined like the kernel does, as functions named sys_ and the name of the call.
#define SYSCALL_DEFINE0(name) long sys_##name(void)
#define SYSCALL_DEFINE1(name, t1, a1) long sys_##name(t1 a1)
#define SYSCALL_DEFINE2(name, t1, a1, t2, a2) long sys_##name(t1 a1, t2 a2)
#define SYSCALL_DEFINE3(name, t1, a1, t2, a2, t3, a3) long sys_##name(t1 a1, t2 a2, t3 a3)
#define SYSCALL_DEFINE4(name, t1, a1, t2, a2, t3, a3, t4, a4) long sys_##name(t1 a1, t2 a2, t3 a3, t4 a4)
#define SYSCALL_DEFINE5(name, t1, a1, t2, a2, t3, a3, t4, a4, t5, a5) long sys_##name(t1 a1, t2 a2, t3 a3, t4 a4, t5 a5)
#define SYSCALL_DEFINE6(name, t1, a1, t2, a2, t3, a3, t4, a4, t5, a5, t6, a6) long sys_##name(t1 a1, t2 a2, t3 a3, t4 a4, t5 a5, t6 a6)
// The macros giving a string literal concatenated with other literals, which cannot be parsed as calls, i.e., "ipw2100-" __stringify(1) ".fw", are defined like the kernel does.
#define __stringify_1(x...) #x
#define __stringify(x...) __stringify_1(x)
// The macros with a type argument, which cannot be parsed as calls, i.e., container_of(ptr, struct task_struct, rcu), are defined like the kernel does, as casts of the pointer,
// and offsetof(struct task_struct, thread) as the builtin of the compiler.
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
#define offsetof(TYPE, MEMBER) __builtin_offsetof(TYPE, MEMBER)
#define offsetofend(TYPE, MEMBER) (offsetof(TYPE, MEMBER) + sizeof_field(TYPE, MEMBER))
// The log levels concatenated with the format of printk, i.e., printk(KERN_ALERT "BUG: ..."), are defined like the kernel does.
#define KERN_SOH "\001"
#define KERN_EMERG KERN_SOH "0"
#define KERN_ALERT KERN_SOH "1"
#define KERN_CRIT KERN_SOH "2"
#define KERN_ERR KERN_SOH "3"
#define KERN_WARNING KERN_SOH "4"
#define KERN_NOTICE KERN_SOH "5"
#define KERN_INFO KERN_SOH "6"
#define KERN_DEBUG KERN_SOH "7"
#define KERN_DEFAULT ""
#define KERN_CONT KERN_SOH "c"
