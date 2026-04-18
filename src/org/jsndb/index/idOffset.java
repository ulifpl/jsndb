package org.jsndb.index;

import java.util.Comparator;

/**
 * Data container that pairs an object ID with its byte offset in the data file.
 * Implements Comparable for sorting by object ID.
 */
public class idOffset implements Comparable<idOffset> {
	/** Object identifier. */
	private Long idObj;
	/** Byte offset in the datafile. */
	private final Long offset;

	/**
	 * Creates a new ID-offset pair.
	 * @param id object identifier.
	 * @param offset byte offset.
	 */
	public idOffset(Long id, Long offset) {
		this.idObj = id;
		this.offset = offset;
	}

	/**
	 * Gets the object identifier.
	 * @return ID as Long.
	 */
	public Long getIdObj() {
		return idObj;
	}

	/**
	 * Sets the object identifier.
	 * @param id ID to set.
	 */
	public void setIdObj(Long id) {
		this.idObj = id;
	}

	/**
	 * Gets the byte offset.
	 * @return offset as Long.
	 */
	public Long getOffset() {
		return offset;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof idOffset)
			if (((idOffset) obj).getIdObj() == getIdObj())
				return true;
		return false;
	}

	@Override
	public int compareTo(idOffset o) {
		return (int) (getIdObj() - o.getIdObj());
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(idObj);
	}

	/**
	 * Comparator for sorting idOffset objects by their object ID.
	 */
	public static class comp implements Comparator<idOffset> {
		@Override
		public int compare(idOffset o1, idOffset o2) {
			return (int) (o1.getIdObj() - o2.getIdObj());
		}

	}

}
