/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.core.sppt;

import java.util.Iterator;

/**
 *
 * A List with a fixed number of elements, i.e. it is unmodifiable after creation. This is basically an unmodifiable array with some convenience methods.
 *
 * @param <T>
 *            the type of the elements in the list.
 */
// would be nice to use some existing implementation, but we don't want to add third-party dependencies just for this
public interface FixedList<T> extends Iterable<T> {

    /**
     * @return number of elements in the list
     */
    int size();

    /**
     * @return the indexed element from the list
     */
    T get(final int index);

    /**
     * @return true if the list is empty
     */
    boolean isEmpty();

    /**
     * @return a clone of this FixedList with the nextElement appended to it
     */
    FixedList<T> append(final T nextElement);

    // --- Iterable ---
    @Override
    Iterator<T> iterator();

    // --- Object ---
    @Override
    int hashCode();

    @Override
    boolean equals(final Object obj);

    @Override
    String toString();

}
