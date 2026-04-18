package org.jsndb.lazyLoad;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jsndb.kore.dataBase;
import org.jsndb.kore.loader;

/**
 * Custom ArrayList implementation that lazily loads objects from the database.
 * Objects are only fetched and instantiated when they are accessed by index or iterated over.
 * @param <T> type of objects in the list.
 */
public class lazyArrayList<T> extends ArrayList<T> {
	private static final long serialVersionUID = -5027377337196659402L;
	/** List of object IDs that belong to this collection. */
	List<Long> ids;
	/** Database instance for loading objects. */
	dataBase db;
	/** Class type of the objects in the list. */
	Class<T> cl;

	/**
	 * Creates a new lazy list.
	 * @param dbase database instance.
	 * @param c object class.
	 * @param list list of object identifiers.
	 */
	public lazyArrayList(dataBase dbase, Class<T> c, List<Long> list) {
		ids = list;
		db = dbase;
		cl = c;
	}

	@Override
	public T get(int index) {
		if (!(index < super.size())) {
			short clcode = db.getMdatas(cl).getClazzcode();
			while (!(index < super.size()))
				super.add(loader.getByid(db, ids.get(super.size()), cl, clcode));
		}
		return super.get(index);
	}

	@Override
	public Iterator<T> iterator() {
		if (super.size() < ids.size()) {
			int index = 0;
			short clcode = db.getMdatas(cl).getClazzcode();
			while (super.size() < ids.size())
				super.add(loader.getByid(db, ids.get(index++), cl, clcode));
		}
		return super.iterator();
	}

	@Override
	public int size() {
		return ids.size();
	}

}