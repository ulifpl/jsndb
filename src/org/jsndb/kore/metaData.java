package org.jsndb.kore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.jsndb.cfg;
import org.jsndb.error;
import org.jsndb.beans.classtool;
import org.jsndb.error.errortype;
import org.jsndb.index.idOffset;
import org.jsndb.index.superindex;
import org.jsndb.kore.enums.crud;

/**
 * Metadata container for a persisted class.
 * Tracks class codes, property indices, and reflection tools.
 */
public class metaData {
	/** The class associated with this metadata. */
	private Class<?> clazz;
	/** Set of property names that are currently indexed. */
	private HashSet<String> propIdxs = new HashSet<String>();
	/** Global database configuration. */
	private cfg cfig;
	/** Reference to the database management instance. */
	private dataBase db;
	/** Reflection tool used to manipulate the associated class. */
	private classtool clstool;
	/** Short code identifier for the class. */
	private short clazzcode;

	/**
	 * Internal constructor to create metadata for a class.
	 * @param clazz class to persist.
	 * @param dbase database instance.
	 * @param classcode short identifier for the class.
	 */
	protected <T> metaData(Class<T> clazz, dataBase dbase, short classcode) {
		clstool = classtool.getInstance(clazz);
		if (clstool == null) {
			dataBase.getEhandler().error(new error("no primary key class:" + clazz.getName(), errortype.noIdError, null));
			throw new IllegalArgumentException("No primary key found class:" + clazz.getName());
		}
		if (cfig == null)
			cfig = dbase.getCfig();
		if (db == null)
			db = dbase;
		clazzcode = classcode;
		this.clazz = clazz;
		if (!cfig.getIndexscode().containsKey(clazz.getName()))
			cfig.getIndexscode().put(clazz.getName(), new HashMap<String, Short>());
		for (String name : cfig.getIndexscode().get(clazz.getName()).keySet()) {
			propIdxs.add(name);
		}
	}

	/**
	 * Updates all indices associated with this class.
	 * @param pair info about the stored object position.
	 * @param crud operation type (create, update, delete).
	 * @param obj mapped object data.
	 * @param extListIds mapping of external relationship IDs.
	 * @param cache query-time/save-time cache.
	 */
	public void updateidx(idOffset pair, crud crud, HashMap<String, Object> obj, HashMap<String, ArrayList<Long>> extListIds,
			HashMap<Long, HashMap<String, Object>> cache) {
	//	System.out.println("metaData.updateidx()");
		for (String prop : getPropIdxs()) {
		//	System.out.println("metaData.updateidx()");
			superindex idx = db.getIndice(clazz.getName(), clazz, prop);
		//	System.out.println("metaData.updateidx()"+idx);
			idx.change(pair.getIdObj(), crud, obj, extListIds.get(idx.getPname()), cache);
		}
	}

	/**
	 * Gets the reflection tool for the class.
	 * @return classtool instance.
	 */
	public classtool getClstool() {
		return clstool;
	}

	/**
	 * Gets the numeric code for the class.
	 * @return short class code.
	 */
	public short getClazzcode() {
		return clazzcode;
	}

	/**
	 * Gets the set of indexed properties.
	 * @return set of property names.
	 */
	public HashSet<String> getPropIdxs() {
		return propIdxs;
	}

	/**
	 * Gets the class object.
	 * @return Class instance.
	 */
	public Class<?> getClazz() {
		return clazz;
	}
}
