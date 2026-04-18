package org.jsndb.index;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.jsndb.error;
import org.jsndb.error.errortype;
import org.jsndb.kore.dataBase;
import org.jsndb.util.logger;
import org.jsndb.util.mapList;

/**
 * Manages the mapping between object IDs and their byte offsets in the data file.
 * Persists these mappings to a dedicated offset file.
 */
public abstract class fileids {
	/** Output stream for the global offsets file. */
	private static DataOutputStream dou;
	/** In-memory map of object ID to byte offset. */
	private mapList<Long, Long> dataoff = new mapList<>();
	/** Number of deleted or fragmented entries. */
	private int fragments;
	/** Reference to the database instance. */
	private static dataBase db;

	/**
	 * Initializes and retrieves the singleton-like instance of fileids.
	 * Loads existing offsets from the disk file.
	 * @param path path to the offsets file (not used, uses database path).
	 * @param dbase database instance.
	 * @return initialized fileids instance.
	 */
	public static synchronized fileids getInstance(String path, dataBase dbase) {
		fileids flids = new fileids() {
		};
		try {
			flids.db = dbase;
			File fl = new File(dbase.getOffsetPath());
			fl.createNewFile();
			if (dou == null) {
				logger.line("fileids.getInstance()     " + fl.length());
				dou = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fl, true)));
				logger.line("fileids.getInstance()     " + fl.length());
			}
			DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(fl)));
			while (din.read() > -1) {
				// din.readShort();
				Long oid = din.readLong();
				Long off = din.readLong();
				if (off < 0) {
					flids.dataoff.remove(oid);
					flids.fragments++;
				} else {
					flids.dataoff.put(oid, off);
				}
			}
			if (flids.fragments > 1000 && (flids.fragments / (double) flids.dataoff.size()) > 0.05)
				flids.comact();
			din.close();
			logger.line("fileids.getInstance() loaded: " + flids.dataoff.size() + "       merge:" + flids.dataoff.size());
			return flids;
		} catch (FileNotFoundException e) {
			dataBase.getEhandler().error(new error("can't read file:" + path, errortype.externalException, e));
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("index table malformed :" + path, errortype.externalException, e));
		}
		return null;
	}

	/**
	 * Returns the in-memory map of IDs to offsets.
	 * @return mapList instance.
	 */
	public mapList<Long, Long> getDataOff() {
		return dataoff;
	}

	/**
	 * Adds a batch of ID-offset pairs to the file and memory.
	 * @param pairs list of pairs to add.
	 * @return true if all were added successfully.
	 */
	public final boolean add(ArrayList<idOffset> pairs) {
		for (idOffset dof : pairs) {
			if (!add(dof))
				return false;
		}
		return true;
	}

	/**
	 * Adds a single ID-offset pair to the file and memory.
	 * @param dof pair to add.
	 * @return true if successful.
	 */
	public final boolean add(idOffset dof) {
		try {
			dou.write(255);
			// dou.writeShort(clazzcode);
			dou.writeLong(dof.getIdObj());
			dou.writeLong(dof.getOffset());
			if (dof.getOffset() < 0)
				getDataOff().remove(dof.getIdObj());
			else
				getDataOff().put(dof.getIdObj(), dof.getOffset());
			return true;
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("can't write ids to file:", errortype.externalException, e));
		}
		return false;
	}

	/**
	 * Flushes the offset output stream to disk.
	 */
	public void flush() {
		try {
			dou.flush();
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("can't flush ids to file:", errortype.externalException, e));
		}
	}

	/**
	 * Compacts both the data file and the offset file to reclaim space.
	 * @throws IOException if disk operations fail.
	 */
	private void comact() throws IOException {
		File flw = new File(db.getOffsetPath().concat(".tmp"));
		flw.delete();
		dou.close();
		db.getDatafile().compact(getDataOff());
		dou = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(flw)));
		ArrayList<idOffset> pairs = new ArrayList<idOffset>();
		for (int n = 0; n < getDataOff().size(); n++) {
			pairs.add(new idOffset(getDataOff().getKeyByIndex(n), getDataOff().getEnryByIndex(n).getValue()));
		}
		add(pairs);
		flush();
		File old = new File(db.getOffsetPath());
		old.delete();
		flw.renameTo(old);
	}

	/**
	 * Closes the offset output stream.
	 */
	public static void close() {
		try {
			if (dou != null) {
				dou.close();
				dou = null;
			}
			db = null;
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("can't close ids file:", errortype.externalException, e));
		}
	}
}
