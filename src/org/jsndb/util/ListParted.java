package org.jsndb.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

/**
 * A partitioned list that uses a HashMap to track which segment each object resides in.
 * This provides O(1) performance for `contains` checks and improves object removal by finding the segment quickly.
 * @param <T> element type.
 */
public class ListParted<T> implements List<T>, RandomAccess {
	/** Mapping of element to the segment (ArrayList) it resides in. */
	HashMap<T, ArrayList<T>> olist;

	/** List of segments. */
	protected ArrayList<ArrayList<T>> plist;
	/** Array of cumulative sizes (end indices) for each segment. */
	protected int[] sizes = new int[1];
	/** Last modified segment index. */
	protected int modidicado = Integer.MAX_VALUE;
	/** Pointer to the most recently accessed segment. */
	protected ArrayList<T> current;
	/** Maximum size for a segment before splitting. */
	private static final int min = 3000;
	/** Total size of the list. */
	private int size;
	/** Mutation counter to trigger re-balancing/splitting. */
	private int modcount = 0;

	/**
	 * Creates a new partitioned list with one empty segment and an empty lookup map.
	 */
	public ListParted() {
		plist = new ArrayList<ArrayList<T>>();
		plist.add(new ArrayList<T>());
		olist = new HashMap<>();
	}

	@Override
	public boolean add(T e) {
		modidicado = sizes.length - 1;
		(current = plist.get(plist.size() - 1)).add(e);
		olist.put(e, current);
		incrase();
		return true;
	}

	@Override
	public void add(int index, T e) {
		modidicado = Arrays.binarySearch(sizes, index);
		if (modidicado < 0)
			modidicado = (modidicado + 1) * -1;
		if (modidicado == plist.size())
			modidicado--;
		if (modidicado > 0)
			index -= (sizes[modidicado - 1] + 1);
		(current = plist.get(modidicado)).add(index, e);
		olist.put(e, current);
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
		olist.clear();
		modidicado = Integer.MAX_VALUE;
		sizes = new int[1];
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
			modidicado = (modidicado + 1) * -1;
		if (modidicado > 0)
			index -= (sizes[modidicado - 1] + 1);
		o = (current = plist.get(modidicado)).remove(index);
		olist.remove(o);
		decrase();
		return o;
	}

	@Override
	public T set(int index, T e) {
		modidicado = Arrays.binarySearch(sizes, index);
		if (modidicado < 0)
			modidicado = (modidicado + 1) * -1;
		if (modidicado > 0)
			index -= (sizes[modidicado - 1] + 1);
		T r = (current = plist.get(modidicado)).set(index, e);
		olist.remove(r);
		olist.put(e, current);
		return r;
	}

	/**
	 * Internal counter management for size increase.
	 */
	private void incrase() {
		size++;
		modcount++;
		for (; modidicado < sizes.length; modidicado++)
			sizes[modidicado]++;
		if (modcount > min)
			split();

	}

	/**
	 * Internal counter management for size decrease.
	 */
	protected void decrase() {
		size--;
		modcount++;
		if (plist.get(modidicado).isEmpty())
			modcount = Integer.MAX_VALUE;
		else
			for (; modidicado < sizes.length; modidicado++)
				sizes[modidicado]--;
		if (modcount > min)
			split();
	}

	@Override
	public int size() {
		return size;
	}

	/**
	 * Analyzes segments and splits or merges them to maintain balance.
	 * Updates the lookup map for affected elements.
	 */
	protected void split() {
		for (int nn = 0; nn < plist.size(); nn++) {
			current = plist.get(nn);
			if (current.size() > min) {
				int mid = current.size() / 2;
				ArrayList<T> l2 = new ArrayList<T>(current.subList(mid, current.size()));
				current.subList(mid, current.size()).clear();
				plist.add(nn + 1, l2);
				for (int n2 = 0; n2 < l2.size(); n2++)
					olist.put(l2.get(n2), l2);
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
							olist.put(old.get(n3), current);
						}
					} else {
						current = plist.get(nn);
						for (int n3 = 0; n3 < current.size(); n3++) {
							old.add(current.get(n3));
							olist.put(current.get(n3), old);
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

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object o) {
		return removeI((T) o) > -1;
	}

	/**
	 * Removes an object from the list using the lookup map for optimization.
	 * @param o object to remove.
	 * @return real position from which it was removed, or Integer.MIN_VALUE if not found.
	 */
	public int removeI(T o) {
		int realPos = 0;
		if (olist.containsKey(o)) {
			current = olist.remove(o);
			if (current != null) {
				for (int n = 0; n < plist.size(); n++) {
					modidicado=n;
					if (plist.get(n) == current)
						break;
					realPos += plist.get(n).size();
				}
				for (int n = 0; n < current.size(); n++) {
					if (current.get(n).equals(o)) {
						realPos += n;
						current.remove(n);
						decrase();
						return realPos;
					}
				}
				throw new UnknownError("report this bug code removeI");
			}
			throw new UnknownError("report this bug code removeI");
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int indexOf(Object arg0) {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public Iterator<T> iterator() {
		throw new UnknownError("method not implemented yet");
		// return merge().iterator();
	}

	@Override
	public int lastIndexOf(Object arg0) {
		throw new UnknownError("method not implemented yet");
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		int ini = 0;
		int fin = 0;
		//int otemp = toIndex;
		// if (fromIndex >= size() || toIndex > size())
		// return new ArrayList<T>();
		while (fromIndex > plist.get(ini).size())
			fromIndex -= plist.get(ini++).size();

		while (toIndex > plist.get(fin).size())
			toIndex -= plist.get(fin++).size();
		if (ini == fin)
			toIndex -= fromIndex;
		partedList<T> pl = new partedList<T>();
		pl.plist.clear();

		for (int n = ini; n <= fin; n++) {
			pl.plist.add((ArrayList<T>) plist.get(n));
		}
		ArrayList<T> first = new ArrayList<T>(pl.plist.get(0).subList(fromIndex, pl.plist.get(0).size()));
		pl.plist.set(0, first);
		ArrayList<T> last = new ArrayList<T>(pl.plist.get(pl.plist.size() - 1).subList(0, toIndex));
		pl.plist.set(pl.plist.size() - 1, last);
		pl.resetSize();
		return pl;
	}

	@Override
	public ListIterator<T> listIterator() {
		throw new UnknownError("method not implemented yet");
		// return merge().listIterator();
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
		throw new UnknownError("method not implemented yet");
		// return merge().toArray();
	}

	@Override
	public <B> B[] toArray(B[] arg0) {
		throw new UnknownError("method not implemented yet");
		// return merge().toArray(arg0);
	}

	@Override
	public boolean contains(Object o) {
		return olist.containsKey(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {

		throw new UnknownError("method not implemented yet");
	}
}