package org.jsndb.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * A segmented list implementation designed to optimize insertions and deletions.
 * The list is partitioned into multiple sub-lists ("segments"). When a segment grows too large, 
 * it is split; when too small, it may be merged. This reduces the number of elements that 
 * need to be shifted during array-based modifications.
 * @param <T> element type.
 */
public class partedList<T> implements List<T>, RandomAccess {
	/** List of segments. */
	protected ArrayList<ArrayList<T>> plist;
	/** Array of cumulative sizes (end indices) for each segment. */
	protected int[] sizes = new int[1];
	/** Last modified segment index. */
	protected int modidicado = Integer.MAX_VALUE;
	/** Pointer to the most recently accessed segment. */
	protected ArrayList<T> current;
	/** Maximum size for a segment before splitting. */
	private static final int min = 5000;
	/** Total size of the list. */
	private int size;
	/** Mutation counter to trigger re-balancing/splitting. */
	private int modcount = 0;

	/**
	 * Creates a new partitioned list with one empty segment.
	 */
	public partedList() {
		plist = new ArrayList<ArrayList<T>>();
		plist.add(new ArrayList<T>());
	}

	@Override
	public boolean add(T e) {
		modidicado = sizes.length - 1;
		(current = plist.get(plist.size() - 1)).add(e);
		incrase();
		return true;
	}

	@Override
	public void add(int index, T e) {
		modidicado = Arrays.binarySearch(sizes, index);
		if (modidicado < 0)
			modidicado = -(++modidicado);
		if (modidicado == plist.size())
			modidicado--;
		if (modidicado > 0)
			index -= (sizes[modidicado - 1] + 1);
		(current = plist.get(modidicado)).add(index, e);
		incrase();
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T o : c)
			add(o);
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		for (T o : c)
			add(index++, o);
		return false;
	}

	@Override
	public void clear() {
		// min = 15000;
		size = 0;
		modcount = 0;
		plist.clear();
		plist.add(new ArrayList<T>(min));
		current = plist.get(0);
		modidicado = Integer.MAX_VALUE;
		sizes = new int[1];
	}

	@Override
	public boolean contains(Object o) {

		throw new UnknownError("method not implemented yet");
	}

	@Override
	public boolean containsAll(Collection<?> c) {

		throw new UnknownError("method not implemented yet");
	}

	@Override
	public T get(int index) {
		modidicado = Arrays.binarySearch(sizes, index);
		if (modidicado < 0)
			modidicado = (modidicado + 1) * -1;
		if (modidicado > 0)
			index -= (sizes[modidicado - 1] + 1);
		return plist.get(modidicado).get(index);
	}

	@Override
	public boolean isEmpty() {
		return size < 1;
	}

	@Override
	public T remove(int index) {
		T o = null;
		modidicado = Arrays.binarySearch(sizes, index);
		if (modidicado < 0)
			modidicado = -(++modidicado);
		if (modidicado > 0)
			index -= (sizes[modidicado - 1] + 1);
		o = (current = plist.get(modidicado)).remove(index);
		decrase();
		return o;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public T set(int index, T e) {
		modidicado = Arrays.binarySearch(sizes, index);
		if (modidicado < 0)
			modidicado = (modidicado + 1) * -1;
		if (modidicado > 0)
			index -= (sizes[modidicado - 1] + 1);
		return plist.get(modidicado).set(index, e);
	}

	/**
	 * Internal counter management for size increase.
	 */
	private void incrase() {
		size++;
		modcount++;
		int mod = modidicado;
		for (; mod < sizes.length; mod++)
			sizes[mod]++;
		if (modcount > min)
			split();

	}

	/**
	 * Internal counter management for size decrease.
	 */
	protected void decrase() {
		size--;
		modcount++;
		int mod = modidicado;
		if (plist.get(mod).isEmpty())
			modcount = Integer.MAX_VALUE;
		else
			for (; mod < sizes.length; mod++)
				sizes[mod]--;
		if (modcount > min)
			split();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public int indexOf(Object arg0) {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public Iterator<T> iterator() {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public int lastIndexOf(Object arg0) {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public ListIterator<T> listIterator() {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public ListIterator<T> listIterator(int arg0) {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public Object[] toArray() {
		// Object[] o = new Object[size];
		// int c = 0;
		ArrayList<T> out = new ArrayList<T>(size);
		for (ArrayList<T> l : plist)
			out.addAll(l);
		// for (T obj : l)
		// o[c++] = obj;
		// return o;
		// return out.toArray();
		return out.toArray();
	}

	@Override
	public <B> B[] toArray(B[] arg0) {
		throw new UnknownError("method not implemented yet");
	}

	/**
	 * Analyzes segments and splits or merges them to maintain balance.
	 * Recalculates the {@code sizes} array.
	 */
	protected void split() {
		for (int nn = 0; nn < plist.size(); nn++) {
			current = plist.get(nn);
			if (current.size() > min) {
				int mid = current.size() / 2;
				ArrayList<T> l2 = new ArrayList<T>(current.subList(mid, current.size()));
				current.subList(mid, current.size()).clear();
				plist.add(nn + 1, l2);
			} else {
				if (plist.size() < 2)
					continue;
				if (current.size() < min / 2) {
					ArrayList<T> old = plist.remove(nn);
					if (nn == plist.size()) {
						nn--;
						current = plist.get(nn);
						for (int n3 = 0; n3 < old.size(); n3++) {
							current.add(old.get(n3));
						}
					} else {
						current = plist.get(nn);
						for (int n3 = 0; n3 < current.size(); n3++) {
							old.add(current.get(n3));
						}
						plist.set(nn, old);
					}
					nn--;
				}
			}
		}
		sizes = new int[plist.size()];
		for (int nn = 0; nn < plist.size(); nn++) {
			for (int n2 = nn; n2 < plist.size(); n2++)
				sizes[n2] += plist.get(nn).size();
		}
		for (int n = 0; n < plist.size(); n++) {
			sizes[n] += -1;
		}
		// if (!(min < 3000))
		// min = (int) (size() * 0.02);
		modcount = 0;
	}

	/**
	 * Manually triggers a recalculation of segment sizes.
	 */
	public void resetSize() {
		size = 0;
		sizes = new int[plist.size()];
		for (int n = 0; n < plist.size(); n++) {
			size += plist.get(n).size();
			sizes[n] = size-1;
		}

	}

}
