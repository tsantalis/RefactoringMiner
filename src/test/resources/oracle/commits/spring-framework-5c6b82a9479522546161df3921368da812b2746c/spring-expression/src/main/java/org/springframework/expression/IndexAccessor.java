/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression;

import org.springframework.lang.Nullable;

/**
 * An index accessor is able to read from (and possibly write to) an indexed
 * structure of an object.
 *
 * <p>This interface places no restrictions on what constitutes an indexed
 * structure. Implementors are therefore free to access indexed values any way
 * they deem appropriate.
 *
 * <p>An index accessor can optionally specify an array of target classes for
 * which it should be called. However, if it returns {@code null} or an empty
 * array from {@link #getSpecificTargetClasses()}, it will be called for all
 * indexing operations and given a chance to determine if it can read from or
 * write to the indexed structure.
 *
 * <p>Index accessors are considered to be ordered, and each will be called in
 * turn. The only rule that affects the call order is that any index accessor
 * which specifies explicit support for the target class via
 * {@link #getSpecificTargetClasses()} will be called first, before other
 * generic index accessors.
 *
 * @author Jackmiking Lee
 * @author Sam Brannen
 * @since 6.2
 * @see PropertyAccessor
 */
public interface IndexAccessor extends TargetedAccessor {

	/**
	 * Get the set of classes for which this index accessor should be called.
	 * <p>Returning {@code null} or an empty array indicates this is a generic
	 * index accessor that can be called in an attempt to access an index on any
	 * type.
	 * @return an array of classes that this index accessor is suitable for
	 * (or {@code null} or an empty array if a generic index accessor)
	 */
	@Override
	@Nullable
	Class<?>[] getSpecificTargetClasses();

	/**
	 * Called to determine if this index accessor is able to read a specified
	 * index on a specified target object.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the index is being accessed
	 * @param index the index being accessed
	 * @return true if this index accessor is able to read the index
	 * @throws AccessException if there is any problem determining whether the
	 * index can be read
	 */
	boolean canRead(EvaluationContext context, Object target, Object index) throws AccessException;

	/**
	 * Called to read an index from a specified target object.
	 * <p>Should only succeed if {@link #canRead} also returns {@code true}.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the index is being accessed
	 * @param index the index being accessed
	 * @return a TypedValue object wrapping the index value read and a type
	 * descriptor for it
	 * @throws AccessException if there is any problem reading the index value
	 */
	TypedValue read(EvaluationContext context, Object target, Object index) throws AccessException;

	/**
	 * Called to determine if this index accessor is able to write to a specified
	 * index on a specified target object.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the index is being accessed
	 * @param index the index being accessed
	 * @return true if this index accessor is able to write to the index
	 * @throws AccessException if there is any problem determining whether the
	 * index can be written to
	 */
	boolean canWrite(EvaluationContext context, Object target, Object index) throws AccessException;

	/**
	 * Called to write to an index on a specified target object.
	 * <p>Should only succeed if {@link #canWrite} also returns {@code true}.
	 * @param context the evaluation context in which the access is being attempted
	 * @param target the target object upon which the index is being accessed
	 * @param index the index being accessed
	 * @param newValue the new value for the index
	 * @throws AccessException if there is any problem writing to the index value
	 */
	void write(EvaluationContext context, Object target, Object index, @Nullable Object newValue)
			throws AccessException;

}
