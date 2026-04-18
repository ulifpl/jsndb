package org.jsndb.util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility class to detect and manage circular references during object serialization.
 * Maintains a tree-like structure of processed objects.
 */
public class circleRef {
	/** Parent reference in the object graph. */
	circleRef parent;
	/** The actual object being processed. */
	Object val;
	/** The transformed (map) representation of the object. */
	HashMap<String, Object> transformed;
	/** List of child references discovered within this object. */
	ArrayList<circleRef> childs = new ArrayList<circleRef>();

	/**
	 * Creates a new circular reference tracker node.
	 * @param obj object to track.
	 * @param p parent tracker node.
	 */
	public circleRef(Object obj, circleRef p) {
		val = obj;
		parent = p;
		// TODO Auto-generated constructor stub
	}

	/** Gets the parent tracker node. */
	public circleRef getParent() {
		return parent;
	}

	/** Gets the tracked object. */
	public Object getVal() {
		return val;
	}

	/** Gets the list of child tracker nodes. */
	public ArrayList<circleRef> getChilds() {
		return childs;
	}

	/** Gets the transformed property map. */
	public HashMap<String, Object> getTransformed() {
		return transformed;
	}

	/** Sets the transformed property map. */
	public void setTransformed(HashMap<String, Object> transformed) {
		this.transformed = transformed;
	}

	/**
	 * Checks if this node has a parent.
	 * @return true if parent is not null.
	 */
	public boolean hasParent() {
		if (parent != null)
			return true;
		return false;
	}

}
