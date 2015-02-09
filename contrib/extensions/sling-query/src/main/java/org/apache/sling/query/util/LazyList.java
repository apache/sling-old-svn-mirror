/*-
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.query.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class LazyList<E> implements List<E> {

	private final class LazyListIterator implements ListIterator<E> {
		private int index = 0;

		private LazyListIterator() {
			this.index = 0;
		}

		private LazyListIterator(int index) {
			this.index = index;
		}

		@Override
		public boolean hasNext() {
			fillToSize(index + 1);
			return arrayList.size() > index;
		}

		@Override
		public E next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			return arrayList.get(index++);
		}

		@Override
		public boolean hasPrevious() {
			return index > 0;
		}

		@Override
		public E previous() {
			if (!hasPrevious()) {
				throw new NoSuchElementException();
			}
			fillToSize(index);
			return arrayList.get(--index);
		}

		@Override
		public int nextIndex() {
			return index;
		}

		@Override
		public int previousIndex() {
			return index - 1;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(E e) {
			throw new UnsupportedOperationException();
		}
	}

	private final List<E> arrayList;

	private final Iterator<E> iterator;

	public LazyList(Iterator<E> iterator) {
		this.arrayList = new ArrayList<E>();
		this.iterator = iterator;
	}

	private void fillAll() {
		while (fillNext() != -1)
			;
	}

	private void fillToSize(int size) {
		for (int s = arrayList.size(); s < size; s++) {
			if (fillNext() == -1) {
				break;
			}
		}
	}

	private int fillNext() {
		if (iterator.hasNext()) {
			E element = iterator.next();
			arrayList.add(element);
			return arrayList.size() - 1;
		}
		return -1;
	}

	@Override
	public int size() {
		fillAll();
		return arrayList.size();
	}

	@Override
	public boolean isEmpty() {
		return arrayList.isEmpty() && !iterator.hasNext();
	}

	@Override
	public boolean contains(Object o) {
		return indexOf(o) != -1;
	}

	@Override
	public Iterator<E> iterator() {
		return new LazyListIterator();
	}

	@Override
	public Object[] toArray() {
		fillAll();
		return arrayList.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		fillAll();
		return arrayList.toArray(a);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public E get(int index) {
		fillToSize(index + 1);
		return arrayList.get(index);
	}

	@Override
	public int indexOf(Object o) {
		int index = arrayList.indexOf(o);
		if (index > -1) {
			return index;
		}
		int addedIndex;
		while ((addedIndex = fillNext()) != -1) {
			E element = arrayList.get(addedIndex);
			if (element == null && o == null) {
				return addedIndex;
			} else if (element != null && element.equals(o)) {
				return addedIndex;
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		fillAll();
		return arrayList.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return new LazyListIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return new LazyListIterator(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		fillToSize(toIndex);
		return arrayList.subList(fromIndex, toIndex);
	}

	@Override
	public boolean equals(Object o) {
		fillAll();
		return arrayList.equals(o);
	}

	@Override
	public int hashCode() {
		fillAll();
		return arrayList.hashCode();
	}

	@Override
	public boolean add(E e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public E set(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(int index, E element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public E remove(int index) {
		throw new UnsupportedOperationException();
	}

}
