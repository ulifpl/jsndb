package org.jsndb.kore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.jsndb.cfg;
import org.jsndb.error;
import org.jsndb.error.errortype;
import org.jsndb.index.fileids;
import org.jsndb.index.superindex;
import org.jsndb.serializer.parser;
import org.jsndb.util.logger;

/**
 * Manages the core database files, directories, and configuration.
 * Handles the initialization of storage components and index management.
 */
public class dataBase {
	/** Indicates if the database is initialized and ready for use. */
	private boolean ready;
	/** Base directory path for database storage. */
	private String base;
	/** Map of classes to their respective metadata. */
	private HashMap<Class<?>, metaData> mdatas = new HashMap<>();
	/** Cache for indices using soft references to manage memory. */
	private HashMap<String, HashMap<String, SoftReference<superindex>>> indices = new HashMap<>();
	/** System-dependent file separator. */
	private String fseparator = System.getProperty("file.separator");
	/** System-dependent line separator. */
	public String nline = System.getProperty("line.separator");
	/** Path to the indices subdirectory. */
	private String indexPath = fseparator + "indices";
	/** Path to the offsets subdirectory. */
	private String offsetPath = fseparator + "offsets";
	/** Path to the data subdirectory. */
	private String dataPath = fseparator + "data";
	/** Path to the configuration subdirectory. */
	private String cfgPath = fseparator + "cfg";
	/** Manager for direct file data operations. */
	private datafile datafs;
	/** Current database configuration. */
	private cfg cfig;
	/** Manager for mapping object IDs to file positions. */
	private fileids objIds;
	/** Tracks the last used unique key for object IDs. */
	private long lastKey;
	/** Global object cache to avoid redundant JSON parsing across different operations. */
	private HashMap<Long, SoftReference<HashMap<String, Object>>> globalCache = new HashMap<>();
	public static long hitCount = 0;
	public static long missCount = 0;

	/**
	 * Retrieves an object from the global cache if available.
	 * @param id object ID.
	 * @return cached property map or null if not found or evicted.
	 */
	public HashMap<String, Object> getFromGlobalCache(Long id) {
		SoftReference<HashMap<String, Object>> ref = globalCache.get(id);
		HashMap<String, Object> map = (ref != null) ? ref.get() : null;
		if (map != null) hitCount++;
		else missCount++;
		return map;
	}

	/**
	 * Adds or updates an object in the global cache.
	 * @param id object ID.
	 * @param map property map to cache.
	 */
	public void putInGlobalCache(Long id, HashMap<String, Object> map) {
		globalCache.put(id, new SoftReference<HashMap<String, Object>>(map));
	}

	/**
	 * Removes an object from the global cache.
	 * @param id object ID.
	 */
	public void removeFromGlobalCache(Long id) {
		globalCache.remove(id);
	}

	/** Default error handler that logs errors to the console. */
	public static errorhandler ehandler = new errorhandler() {
		@Override
		public synchronized void error(org.jsndb.error e) {
			logger.line("errorhandler.error()#" + e.getDetail());
			if (e.getException() != null)
				e.getException().printStackTrace();
		}
	};

	/**
	 * Initializes a new database instance in the specified directory.
	 * Creates necessary subdirectories and loads existing configuration.
	 * @param dirpath root directory for the database.
	 */
	public dataBase(String dirpath) {
		File dir = new File(dirpath);
		try {
			dir.mkdirs();
			if (!dir.exists() || !dir.canWrite() || !dir.isDirectory())
				return;
			base = dir.getAbsolutePath();
			logger.line(base);
			logger.line(indexPath);
			dir = new File(base + indexPath);
			dir.createNewFile();
			indexPath = base + indexPath;
			dir = new File(base + offsetPath);
			dir.createNewFile();
			offsetPath = base + offsetPath;
			dir = new File(base + dataPath);
			dir.createNewFile();
			dataPath = base + dataPath;
			dir = new File(base + cfgPath);
			dir.createNewFile();
			cfgPath = base + cfgPath;
			FileInputStream fis = new FileInputStream(dir);
			byte[] cfgbyte = new byte[1024 * 10];
			int size = fis.read(cfgbyte);
			fis.close();
			if (size > -1)
				cfgbyte = Arrays.copyOf(cfgbyte, size);
			else
				cfgbyte = null;
			synchronized (dataBase.class) {
				cfig = new cfg();
				if (cfgbyte != null && cfgbyte.length > 0)
					cfig = (cfg) parser.jsonToDeepBean(new String(cfgbyte, "UTF-8"), cfg.class,this);
				datafs = datafile.getInstance(dataPath);
				if (datafs == null) {
					return;
				}
				objIds = fileids.getInstance(getOffsetPath(), this);
				if (objIds == null) {
					return;
				}
				ready = true;
				cfig.setClosedwell(false);
				savecfg();
			}
		} catch (Exception e) {
			logger.line("dataBase.dataBase() " + dir);
			dataBase.getEhandler().error(new error("can't create directory:" + dir.getAbsolutePath(), errortype.externalException, e));
		}
	}

	/**
	 * Gets the absolute path to the indices directory.
	 * @return absolute index path.
	 */
	public String getIndexPath() {
		return indexPath;
	}

	/**
	 * Gets the absolute path to the offsets directory.
	 * @return absolute offset path.
	 */
	public String getOffsetPath() {
		return offsetPath;
	}

	/**
	 * Gets the absolute path to the configuration file.
	 * @return absolute configuration path.
	 */
	public String getCfgPath() {
		return cfgPath;
	}

	/**
	 * Gets the manager for data file operations.
	 * @return datafile instance.
	 */
	public datafile getDatafile() {
		return datafs;
	}

	/**
	 * Gets the manager for object ID offsets.
	 * @return fileids instance.
	 */
	public fileids getObjIds() {
		return objIds;
	}

	/** Execution counter for metadata preparation. */
	public static int cter = 0;

	/**
	 * Retrieves or prepares metadata for a given class.
	 * @param clazz class to get metadata for.
	 * @return metaData instance or null if preparation fails.
	 */
	public <T> metaData getMdatas(Class<T> clazz) {
		cter++;
		if (prepare(clazz))
			return mdatas.get(clazz);
		return null;
	}

	/**
	 * Prepares an index for a specific property of a class.
	 * Generates a unique index code if it doesn't exist.
	 * @param clazz class the property belongs to.
	 * @param name name of the property to index.
	 * @return superindex instance.
	 */
	public <C> superindex prepareIndex(Class<?> clazz, String name) {
		superindex indice = null;
		if (!cfig.getIndexscode().containsKey(clazz.getName()))
			cfig.getIndexscode().put(clazz.getName(), new HashMap<String, Short>());
		if (!cfig.getIndexscode().get(clazz.getName()).containsKey(name)) {
			HashSet<Short> ab = new HashSet<Short>();
			for (HashMap<String, Short> b : cfig.getIndexscode().values())
				ab.addAll(b.values());
			for (short n = 1; n < Short.MAX_VALUE; n++) {
				if (!ab.contains(n)) {
					if (n == Short.MAX_VALUE - 1) {
						throw new IllegalStateException("Limit index type to persist overloaded. max=256");
					} else {
						cfig.getIndexscode().get(clazz.getName()).put(name, n);
						break;
					}
				}
			}
			savecfg();
			getMdatas(clazz).getPropIdxs().add(name);
			indice = superindex.getInstance(this, name, cfig.getIndexscode().get(clazz.getName()).get(name),
					cfig.getClasscode().get(clazz.getName()), clazz.getName(), clazz);
			indice.reindex();
		}
		if (indice == null)
			indice = superindex.getInstance(this, name, cfig.getIndexscode().get(clazz.getName()).get(name),
					cfig.getClasscode().get(clazz.getName()), clazz.getName(), clazz);
		// getPropIdxs().add(name);
		return indice;
	}

	/**
	 * Retrieves an existing index or creates a new one if necessary.
	 * @param cls class name (optional if clzz is provided).
	 * @param clzz class object.
	 * @param prop property name to get the index for.
	 * @return superindex instance.
	 */
	public superindex getIndice(String cls, Class<?> clzz, String prop) {
		if (indices.get(cls) == null)
			if (clzz != null)
				cls = clzz.getName();
		if (indices.get(cls) == null || indices.get(cls).get(prop) == null || indices.get(cls).get(prop).get() == null) {
			superindex si = null;
			try {
				if (clzz == null)
					clzz = Class.forName(cls);
				if (clzz != null) {
					si = prepareIndex(clzz, prop);
					cls = clzz.getName();
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			if (si == null)
				si = superindex.getInstance(this, prop, getCfig().getIndexscode().get(cls).get(prop), getCfig().getClasscode()
						.get(cls), cls, clzz);
			if (indices.get(cls) == null)
				indices.put(cls, new HashMap<String, SoftReference<superindex>>());
			indices.get(cls).put(prop, new SoftReference<superindex>(si));
		}
		return indices.get(cls).get(prop).get();
	}

	/**
	 * Checks if the database is initialized.
	 * @return true if ready.
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * Gets the root directory path.
	 * @return base path string.
	 */
	public String getBase() {
		return base;
	}

	/**
	 * Gets the current configuration.
	 * @return cfg instance.
	 */
	public cfg getCfig() {
		return cfig;
	}

	/**
	 * Gets the last generated key.
	 * @return long value of the last key.
	 */
	public long getLastKey() {
		return lastKey;
	}

	/**
	 * Sets the value of the last generated key.
	 * @param lastKey key value to set.
	 */
	public void setLastKey(long lastKey) {
		this.lastKey = lastKey;
	}

	/**
	 * Gets the global error handler.
	 * @return errorhandler instance.
	 */
	public static errorhandler getEhandler() {
		return ehandler;
	}

	/**
	 * Compacts the database files to reclaim space (placeholder).
	 * @param force whether to force the operation.
	 */
	public void compact(boolean force) {

	}

	/**
	 * Generates a unique key for a new object persistence.
	 * Iterates to find the next available ID.
	 * @return a unique Long key.
	 */
	public Long nextkey() {
		synchronized (this) {
			long max = 0;
			if (Long.MAX_VALUE > getObjIds().getDataOff().size()) {
				max = getLastKey();
				do {
					++max;
					if (max == Long.MAX_VALUE || max < 1)
						max = 1;
				} while (getObjIds().getDataOff().get(max) != null);
				setLastKey(max);
			}
			return max;
		}
	}

	/**
	 * Prepares metadata and class codes for a new class.
	 * @param clazz class to prepare.
	 * @return true if successful or already prepared.
	 */
	private synchronized <T> boolean prepare(Class<T> clazz) {
		if (mdatas.get(clazz) == null) {
			if (!cfig.getClasscode().containsKey(clazz.getName())) {
				HashSet<Short> ab = new HashSet<Short>();
				for (Short b : cfig.getClasscode().values())
					ab.add(b);
				for (short n = 1; n < Short.MAX_VALUE; n++) {
					if (!ab.contains(n)) {
						if (n == Short.MAX_VALUE - 1) {
							throw new IllegalStateException("Limit class type to persist overloaded. max=256");
						} else {
							cfig.getClasscode().put(clazz.getName(), n);
							break;
						}
					}
				}
				savecfg();
			}
			metaData meta = new metaData(clazz, this, cfig.getClasscode().get(clazz.getName()));
			if (meta.getClazz() == null)
				return false;
			mdatas.put(clazz, meta);
		}
		return true;
	}

	/**
	 * Saves the current configuration to the config file as JSON.
	 */
	public void savecfg() {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(cfgPath);
			fos.write(parser.beanToJson(cfig).getBytes("UTF-8"));
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes the database session properly.
	 * Clears ID cache and updates configuration status.
	 */
	public void close() {
		if (datafs != null) {
			datafs.close();
		}
		if (objIds != null) {
			fileids.close();
		}
		superindex.close();
		org.jsndb.beans.classtool.close();
		
		mdatas.clear();
		indices.clear();
		ready = false;
		
		cfig.setClosedwell(true);
		savecfg();
	}
}
