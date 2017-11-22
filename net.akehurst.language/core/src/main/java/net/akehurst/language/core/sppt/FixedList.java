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

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class FixedList<T> implements Iterable<T> {

	@SuppressWarnings("rawtypes")
	public static FixedList EMPTY = new FixedList<>();

	private final int hashCode_cache;
	private final T[] elements;

	public FixedList(final T... elements) {
		this.hashCode_cache = Objects.hash(elements);
		this.elements = elements;
	}

	public int size() {
		return this.elements.length;
	}

	public T get(final int index) {
		return this.elements[index];
	}

	public FixedList<T> append(final T nextElement) {
		final T[] newElements = Arrays.copyOf(this.elements, this.elements.length + 1);
		newElements[this.elements.length] = nextElement;
		return new FixedList<>(newElements);
	}

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
				return this.index < FixedList.this.elements.length;
			}

			@Override
			public T next() {
				if (this.hasNext() == false) {
					throw new NoSuchElementException();
				}
				return FixedList.this.elements[this.index++];
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
		if (obj instanceof FixedList<?>) {
			final FixedList<T> other = (FixedList<T>) obj;
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
