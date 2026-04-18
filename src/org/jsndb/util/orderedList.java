package org.jsndb.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A List implementation that automatically maintains its elements in their natural sorted order.
 * Uses binary search for insertion and lookups.
 * @param <T> element type, must be Comparable.
 */
public class orderedList<T extends Comparable<T>> implements List<T> {
	/** Underlying list implementation. */
	List<T> list;

	/**
	 * Decorates an existing list to make it ordered.
	 * @param listimpl the list to wrap.
	 */
	public orderedList(List<T> listimpl) {
		list = listimpl;
	}

	/**
	 * Adds an element at the correct sorted position, ignoring the requested position.
	 */
	@Override
	public void add(int pos, T e) {
		add(e);
	}

	/**
	 * Adds an element at its sorted position.
	 * @param e element to add.
	 * @return always true.
	 */
	@Override
	public boolean add(T e) {
		int p = Collections.binarySearch(list, e);
		if (p < 0)
			p = (p + 1) * -1;
		list.add(p, e);
		return true;
	}

	/** Not supported for ordered lists. */
	@Override
	public T set(int pos, T e) {
		throw new UnknownError("not supported");
	}

	/**
	 * Replaces an existing equivalent element or adds it if not found.
	 * @param e element to replace or add.
	 */
	public void replace(T e) {
		int p = Collections.binarySearch(list, e);
		if (p < 0) {
			p = (p + 1) * -1;
			list.add(p, e);
		} else {
			list.set(p, e);
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> arg0) {
		for (T o : arg0)
			add(o);
		return true;
	}

	@Override
	public boolean addAll(int arg0, Collection<? extends T> arg1) {
		return addAll(arg1);
	}

	@Override
	public void clear() {
		list.clear();
	}

	/** Disabled for this specific implementation. */
	@Override
	public boolean contains(Object o) {
		throw new NullPointerException("sublist is readonly, iterator is disabled");
	}

	/**
	 * Checks if the list contains the given element using binary search.
	 * @param o element to check.
	 * @return true if found.
	 */
	public boolean contains(T o) {
		int p = Collections.binarySearch(list, o);
		if (p < 0)
			return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsAll(Collection<?> c) {
		boolean b = true;
		if (!c.isEmpty()) {
			for (T o : (Collection<T>) c) {
				if (contains(o)) {
					continue;
				} else {
					b = false;
					break;
				}
			}
			return b;
		}
		return false;
	}

	@Override
	public T get(int index) {
		return (T) list.get(index);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int indexOf(Object o) {
		return Collections.binarySearch(list, (T) o);
	}

	@Override
	public boolean isEmpty() {
		return list.isEmpty();
	}

	/** Disabled for this specific implementation. */
	@Override
	public Iterator<T> iterator() {
		throw new NullPointerException("sublist is readonly, iterator is disabled");
	}

	/** Disabled for this specific implementation. */
	@Override
	public int lastIndexOf(Object o) {
		throw new NullPointerException("sublist is readonly, iterator is disabled");
	}

	/** Disabled for this specific implementation. */
	@Override
	public ListIterator<T> listIterator() {
		throw new NullPointerException("sublist is readonly, iterator is disabled");
	}

	/** Disabled for this specific implementation. */
	@Override
	public ListIterator<T> listIterator(int index) {
		throw new NullPointerException("sublist is readonly, iterator is disabled");
	}

	@Override
	public boolean remove(Object o) {
		@SuppressWarnings("unchecked")
		int n = Collections.binarySearch(list, (T) o);
		if (n < 0)
			return false;
		return list.remove(n) != null;
	}

	@Override
	public T remove(int index) {
		return list.remove(index);
	}

	/** Disabled for this specific implementation. */
	@Override
	public boolean removeAll(Collection<?> c) {
		throw new NullPointerException("sublist is readonly, iterator is disabled");
	}

	/** Disabled for this specific implementation. */
	@Override
	public boolean retainAll(Collection<?> c) {
		throw new NullPointerException("sublist is readonly, iterator is disabled");
	}

	@Override
	public int size() {
		return list.size();
	}

	/** Disabled for this specific implementation. */
	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		throw new NullPointerException("sublist is readonly, iterator is disabled");
	}

	/** Disabled for this specific implementation. */
	@Override
	public Object[] toArray() {
		throw new NullPointerException("sublist is readonly, iterator is disabled");
	}

	/** Disabled for this specific implementation. */
	@Override
	public <B> B[] toArray(B[] a) {
		throw new NullPointerException("sublist is readonly, iterator is disabled");
	}

}
