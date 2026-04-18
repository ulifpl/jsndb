package org.jsndb.qwery;

import java.util.ArrayList;

/**
 * Represents a logical connection between multiple queries (AND, OR).
 * Allows building complex nested query structures.
 */
public abstract class connector extends qwery {
	/** List of simple queries or sub-connectors in this group. */
	private ArrayList<qwery> list = new ArrayList<qwery>();
	/** Specific list of child connectors for hierarchical traversal. */
	private ArrayList<connector> conns = new ArrayList<connector>();
	/** The logical type of this connection (AND, OR). */
	private types type;

	/**
	 * Logical operator types.
	 */
	public enum types {
		/** All criteria must be met. */
		and, 
		/** At least one criterion must be met. */
		or,
	};

	/**
	 * Creates an AND connector for the given queries.
	 * @param qweries list of query criteria.
	 * @return a connector instance.
	 */
	public static connector and(qwery... qweries) {
		return conect(qweries, types.and);
	}

	/**
	 * Creates an OR connector for the given queries.
	 * @param qweries list of query criteria.
	 * @return a connector instance.
	 */
	public static connector or(qwery... qweries) {
		return conect(qweries, types.or);
	}

//	public static connector not(qwery... qweries) {
//		return conect(qweries, types.not);
//	}

	/**
	 * Internal factory to build a connector.
	 */
	private static connector conect(qwery[] qs, types t) {
		connector conn = new connector() {
		};
		conn.setType(t);
		for (qwery q : qs) {
			if (q != null) {
				if (q instanceof qwery)
					conn.list.add(q);
				if (q instanceof connector)
					conn.conns.add((connector) q);
			}
		}
	//	if (conn.list.size() > 1)
			return conn;
		//return null;
	}

	/**
	 * Gets the list of sub-queries.
	 * @return list of qwery objects.
	 */
	public ArrayList<qwery> getList() {
		return list;
	}

	/**
	 * Sets the list of sub-queries.
	 * @param list list of qwery objects.
	 */
	public void setList(ArrayList<qwery> list) {
		this.list = list;
	}

	/**
	 * Gets the logical type.
	 * @return types enum value.
	 */
	public types getType() {
		return type;
	}

	/**
	 * Sets the logical type.
	 * @param tipo types enum value.
	 */
	public void setType(types tipo) {
		this.type = tipo;
	}

}
