package org.jsndb;

import java.util.ArrayList;
import java.util.HashMap;
import org.jsndb.annotations.jsndbObjectId;

/**
 * Configuration class for the jsndb database system.
 * This class stores metadata about classes, indexes, and versions.
 */
public class cfg {
	@jsndbObjectId
	private Long id;
	private boolean closedwell;
	private String version = "1.0";
	private String coreversion = "1.0";
	private long datalength;
	private long indexlength;
	
	/**
	 * Map of index codes: Class name -> Property name -> Index ID.
	 */
	private HashMap<String, HashMap<String, Short>> indexscode = new HashMap<String, HashMap<String, Short>>();
	
	/**
	 * Map of class codes: Class name -> Class ID.
	 */
	private HashMap<String, Short> classcode = new HashMap<String, Short>();
	
	/**
	 * Map of external properties: Class name -> List of external property names.
	 */
	private HashMap<String, ArrayList<String>> extProp = new HashMap<>();
	
	/**
	 * Map of parent relationships: Class name -> Property name -> List of parent class names.
	 */
	private HashMap<String, HashMap<String, ArrayList<String>>> fathers = new HashMap<>();

	/**
	 * Limit in MB for cache growth before cleaning during large operations.
	 */
	private int cacheCleanupLimitMB = 100;

	/**
	 * Gets the current cache cleanup limit in MB.
	 * @return limit in MB.
	 */
	public int getCacheCleanupLimitMB() {
		return cacheCleanupLimitMB;
	}

	/**
	 * Sets the cache cleanup limit in MB.
	 * @param limitMB limit to set.
	 */
	public void setCacheCleanupLimitMB(int limitMB) {
		this.cacheCleanupLimitMB = limitMB;
	}

	/**
	 * Checks if the database was closed properly in the last session.
	 * @return true if closed properly, false otherwise.
	 */
	public boolean isClosedwell() {
		return closedwell;
	}

	/**
	 * Sets the status of whether the database was closed properly.
	 * @param closed true if closed properly.
	 */
	public void setClosedwell(boolean closed) {
		closedwell = closed;
	}

	/**
	 * Gets the configuration ID.
	 * @return the configuration ID.
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Sets the configuration ID.
	 * @param id the configuration ID.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Gets the map of index codes.
	 * @return the index codes map.
	 */
	public HashMap<String, HashMap<String, Short>> getIndexscode() {
		return indexscode;
	}

	/**
	 * Sets the map of index codes.
	 * @param indexscode the index codes map.
	 */
	public void setIndexscode(HashMap<String, HashMap<String, Short>> indexscode) {
		this.indexscode = indexscode;
	}

	/**
	 * Gets the map of class codes.
	 * @return the class codes map.
	 */
	public HashMap<String, Short> getClasscode() {
		return classcode;
	}

	/**
	 * Sets the map of class codes.
	 * @param classcode the class codes map.
	 */
	public void setClasscode(HashMap<String, Short> classcode) {
		this.classcode = classcode;
	}

	/**
	 * Gets the total length of index files.
	 * @return the index length.
	 */
	public long getIndexlength() {
		return indexlength;
	}

	/**
	 * Sets the total length of index files.
	 * @param indexlength the index length.
	 */
	public void setIndexlength(long indexlength) {
		this.indexlength = indexlength;
	}

	/**
	 * Gets the total length of data files.
	 * @return the data length.
	 */
	public long getDatalength() {
		return datalength;
	}

	/**
	 * Sets the total length of data files.
	 * @param datalength the data length.
	 */
	public void setDatalength(long datalength) {
		this.datalength = datalength;
	}

	/**
	 * Gets the configuration version.
	 * @return the version string.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Sets the configuration version.
	 * @param version the version string.
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Gets the core engine version.
	 * @return the core version string.
	 */
	public String getCoreversion() {
		return coreversion;
	}

	/**
	 * Sets the core engine version.
	 * @param coreversion the core version string.
	 */
	public void setCoreversion(String coreversion) {
		this.coreversion = coreversion;
	}

	/**
	 * Gets the map of external properties.
	 * @return map of external properties.
	 */
	public HashMap<String, ArrayList<String>> getExtProp() {
		return extProp;
	}

	/**
	 * Sets the map of external properties.
	 * @param extProp map of external properties.
	 */
	public void setExtProp(HashMap<String, ArrayList<String>> extProp) {
		this.extProp = extProp;
	}

	/**
	 * Gets the parent relationship map.
	 * @return fathers relationship map.
	 */
	public HashMap<String, HashMap<String, ArrayList<String>>> getFathers() {
		return fathers;
	}

	/**
	 * Sets the parent relationship map.
	 * @param fathers fathers relationship map.
	 */
	public void setFathers(HashMap<String, HashMap<String, ArrayList<String>>> fathers) {
		this.fathers = fathers;
	}

}
