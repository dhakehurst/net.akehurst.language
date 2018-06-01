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
package net.akehurst.language.parser.sppf;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import net.akehurst.language.core.sppt.FixedList;

/**
 *
 * A List with a fixed number of elements, i.e. it is unmodifiable after creation. This is basically an unmodifiable array with some convenience methods.
 *
 * @param <T>
 *            the type of the elements in the list.
 */
// would be nice to use some existing implementation, but we don't want to add third-party dependencies just for this
public class FixedListSimple<T> implements FixedList<T> {

    private final int hashCode_cache;
    private final T[] elements;

    public FixedListSimple(final T... elements) {
        this.hashCode_cache = Objects.hash(elements);
        this.elements = elements;
    }

    @Override
    public int size() {
        return this.elements.length;
    }

    @Override
    public T get(final int index) {
        return this.elements[index];
    }

    @Override
    public FixedList<T> append(final T nextElement) {
        final T[] newElements = Arrays.copyOf(this.elements, this.elements.length + 1);
        newElements[this.elements.length] = nextElement;
        return new FixedListSimple<>(newElements);
    }

    @Override
    public boolean isEmpty() {
        return 0 == this.elements.length;
    }

    // --- Iterable ---
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return this.index < FixedListSimple.this.elements.length;
            }

            @Override
            public T next() {
                if (this.hasNext() == false) {
                    throw new NoSuchElementException();
                }
                return FixedListSimple.this.elements[this.index++];
            }
        };
    }

    // --- Object ---
    @Override
    public int hashCode() {
        return this.hashCode_cache;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof FixedListSimple<?>) {
            final FixedListSimple<T> other = (FixedListSimple<T>) obj;
            return Arrays.equals(this.elements, other.elements);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(this.elements);
    }

}
