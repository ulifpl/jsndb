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
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import org.jsndb.error;
import org.jsndb.beans.classtool;
import org.jsndb.error.errortype;
import org.jsndb.kore.dataBase;
import org.jsndb.kore.enums.crud;
import org.jsndb.serializer.parser;
import org.jsndb.util.entryM;
import org.jsndb.util.logger;
import org.jsndb.util.mapList;

public abstract class superindex {

	private static boolean change = false;
	private static ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
	private short indexcode;
	// private Class<?> mainClazz;
	// private Class<?> genericClazz;
	private String cName;
	private String pname;
	private static dataBase db;
	private static DataOutputStream dout;
	private short clazzcode;
	private ArrayList<superindex> fathersIndex;

	public static <T, C> superindex getInstance(dataBase dbase, String prop, short ixcode, short clsscode, String clsName, Class<?> c) {
		superindex si = null;

		if (c == null) {
			if (dbase.getCfig().getExtProp().get(clsName).contains(prop))
				si = indexChild.create(dbase, prop, ixcode, clsscode);
			si = index.create(dbase, prop, ixcode, clsscode);
			si.cName = clsName;
		} else {
			classtool clt = classtool.getInstance(c);
			if (clt.getExtProps().contains(prop)) {
				si = indexChild.create(dbase, prop, ixcode, clsscode);
				if (si != null) {
					ArrayList<String> s = dbase.getCfig().getExtProp().get(c.getName());
					if (s == null) {
						s = new ArrayList<String>();
						dbase.getCfig().getExtProp().put(c.getName(), s);
					}
					s.add(prop);

					HashMap<String, ArrayList<String>> m = dbase.getCfig().getFathers().get(clt.getFieldClazz(prop).getName());
					if (m == null) {
						m = new HashMap<>();
						ArrayList<String> a = new ArrayList<String>();
						m.put(c.getName(), a);
						dbase.getCfig().getFathers().put(clt.getFieldClazz(prop).getName(), m);
					}
					if (!m.get(c.getName()).contains(prop)) {
						m.get(c.getName()).add(prop);
						dbase.savecfg();
					}
				}
			} else {
				si = index.create(dbase, prop, ixcode, clsscode);
			}
			si.cName = clsName;
		}
		return si;
	}

	public abstract void change(Long id, crud c, HashMap<String, Object> o, ArrayList<Long> arrayList,
			HashMap<Long, HashMap<String, Object>> cache);

	public void write(long objId, int order, ArrayList<Long> childs) {
		write(objId, order, childs, null);
	}

	public void write(long objId, int order, ArrayList<Long> childs, Object value) {
		byte[] valBytes = encodeValue(value);
		int size = 1 + 2 + 8 + 4 + 4 + (childs == null ? 0 : childs.size() * 8) + 4 + valBytes.length;
		if (buffer.remaining() < size) {
			flush();
			if (buffer.remaining() < size) {
				buffer = ByteBuffer.allocate(size);
			}
		}
		buffer.put((byte) 255);
		buffer.putShort(indexcode);
		buffer.putLong(objId);
		buffer.putInt(order);
		if (childs != null) {
			buffer.putInt(childs.size());
			for (long ch : childs)
				buffer.putLong(ch);
		} else {
			buffer.putInt(0);
		}
		buffer.putInt(valBytes.length);
		if (valBytes.length > 0)
			buffer.put(valBytes);
	}

	/**
	 * Encodes a value with a type tag prefix for storage in the index file.
	 * Format: byte(type) + payload
	 * Type tags: 'S'=String, 'I'=int(4b), 'L'=long(8b), 'D'=double(8b)
	 */
	private static byte[] encodeValue(Object value) {
		if (value == null) return new byte[0];
		if (value instanceof String) {
			byte[] sv = ((String) value).getBytes(java.nio.charset.StandardCharsets.UTF_8);
			byte[] r = new byte[1 + sv.length]; r[0] = 'S';
			System.arraycopy(sv, 0, r, 1, sv.length); return r;
		}
		if (value instanceof Integer) {
			int v = (Integer) value;
			return new byte[]{'I', (byte)(v>>24),(byte)(v>>16),(byte)(v>>8),(byte)v };
		}
		if (value instanceof Long) {
			long v = (Long) value;
			return new byte[]{'L',(byte)(v>>56),(byte)(v>>48),(byte)(v>>40),(byte)(v>>32),(byte)(v>>24),(byte)(v>>16),(byte)(v>>8),(byte)v};
		}
		if (value instanceof Double || value instanceof Float) {
			long v = Double.doubleToLongBits(((Number)value).doubleValue());
			return new byte[]{'D',(byte)(v>>56),(byte)(v>>48),(byte)(v>>40),(byte)(v>>32),(byte)(v>>24),(byte)(v>>16),(byte)(v>>8),(byte)v};
		}
		// Fallback: store as String
		byte[] sv = value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
		byte[] r = new byte[1 + sv.length]; r[0] = 'S';
		System.arraycopy(sv, 0, r, 1, sv.length); return r;
	}

	/**
	 * Decodes a typed value from the encoded byte array.
	 */
	private static Object decodeValue(byte[] data) {
		if (data == null || data.length == 0) return null;
		char type = (char) data[0];
		switch (type) {
			case 'S': return new String(data, 1, data.length - 1, java.nio.charset.StandardCharsets.UTF_8);
			case 'I': return ((data[1]&0xFF)<<24)|((data[2]&0xFF)<<16)|((data[3]&0xFF)<<8)|(data[4]&0xFF);
			case 'L': {
				long v=0; for(int i=1;i<9;i++) v=(v<<8)|(data[i]&0xFF); return v;
			}
			case 'D': {
				long v=0; for(int i=1;i<9;i++) v=(v<<8)|(data[i]&0xFF);
				return Double.longBitsToDouble(v);
			}
			default: return new String(data, 1, data.length - 1, java.nio.charset.StandardCharsets.UTF_8);
		}
	}

	public static void flush() {
		if (!change)
			return;
		long time = System.currentTimeMillis();
		change = false;
		try {
			buffer.flip();
			getDout(db).write(buffer.array(), 0, buffer.limit());
			getDout(db).flush();
			buffer.clear();
			logger.line("superindex.flush(fin)  time:" + (System.currentTimeMillis() - time));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public boolean reindex() {
		logger.line("reindex(ini)  meminit: " + pname + "  "
				+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) + "    ");
		long time = System.currentTimeMillis();
		int counter = 0;
		try {
			String strjson;
			long[] id = new long[1];
			getDb().getDatafile().resetInputStream();
			HashMap<Long, HashMap<String, Object>> cache = new HashMap<>();
			int processCount = 0;
			long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			long threshold = getDb().getCfig().getCacheCleanupLimitMB() * 1024L * 1024L;

			while ((strjson = getDb().getDatafile().next(getClazzcode(), id)) != null) {
				counter++;
				processCount++;
				HashMap<String, Object> o = parser.jsonToFlatMap(strjson);
				if (o.get(parser.JSNDB_EXERNAL) != null
						&& ((HashMap<String, Object>) o.get(parser.JSNDB_EXERNAL)).containsKey(getPname())) {
					HashMap<String, ArrayList<Long>> m = (HashMap<String, ArrayList<Long>>) o.get(parser.JSNDB_EXERNAL);
					change(id[0], crud.create, o, m.get(getPname()), cache);
				} else {
					change(id[0], crud.create, o, null, cache);
				}
				
				// Memory-based cache cleanup
				if (processCount >= 100) {
					long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
					if ((currentMemory - initialMemory) > threshold) {
						cache.clear();
						// After clearing, update initialMemory for the next window
						initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
					}
					processCount = 0;
				}
			}
			flush();
			load(db, getIndexcode(), getClazzcode());
			System.gc();
			logger.line("reindex(fin)  time:" + (System.currentTimeMillis() - time) + "   counter:" + counter + "  memout:"
					+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public short getClazzcode() {
		return clazzcode;
	}

	protected abstract void add(Long id, ArrayList<Long> childs, int order, Object value);

	public short getIndexcode() {
		return indexcode;
	}

	public <T> void load(dataBase db, short idxcode, short clsscode) {
		clear();
		try {
			indexcode = idxcode;
			clazzcode = clsscode;
			// mainClazz = claz;
			if (Paths.get(db.getIndexPath()).toFile().length() < 19)
				return;
			System.out.println("superindex.load()");
			logger.line("superindex.load(1)  indexcoed:" + idxcode + "    meminit:"
					+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
			long time = System.currentTimeMillis();
			DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(db.getIndexPath())));
			mapList<Long, Long> ids = db.getObjIds().getDataOff();
			blockidx blk;
			while ((blk = next(din)) != null) {
				entryM<Long, Long> em = ids.getEntry(blk.id);
				if (em != null)
					blk.id = em.getKey();
				for (int n = 0; n < blk.childs.size(); n++) {
					entryM<Long, Long> lo = ids.getEntry(blk.childs.get(n));
					if(lo==null)
						continue;
					blk.childs.set(n, lo.getKey());
				}
				add(blk.id, blk.childs, blk.order, blk.value);
			}
			if (isFragmented())
				compact();
			din.close();
			System.gc();
			logger.line("superindex.load(2)  memout:" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
					+ "  time:" + (System.currentTimeMillis() - time));
		} catch (FileNotFoundException e) {
			dataBase.getEhandler().error(new error("can't read file:" + db.getIndexPath(), errortype.externalException, e));
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("index table malformed :" + db.getIndexPath(), errortype.externalException, e));
		}
	}

	protected abstract void clear();

	protected abstract ArrayList<blockidx> getBlokcs();

	protected abstract boolean isFragmented();

	private void compact() throws IOException {
		logger.line("####### superindex.compact()");
		DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(db.getIndexPath())));
		File f = new File(db.getIndexPath().concat(".tmp"));
		f.delete();
		DataOutputStream dou = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f), 1024 * 1024));
		blockidx b;
		while ((b = next(din, true)) != null) {
			if (b.idxcode != indexcode) {
				dou.write(b.ini);
				dou.writeShort(b.idxcode);
				dou.writeLong(b.id);
				dou.writeInt(b.order);
				dou.writeInt(b.childcount);
				for (long l : b.childs)
					dou.writeLong(l);
			}
		}
		ArrayList<blockidx> bk = getBlokcs();
		for (int n = 0; n < bk.size(); n++) {
			dou.write(255);
			dou.writeShort(indexcode);
			bk.get(n).id = bk.get(n).id;
			dou.writeLong(bk.get(n).id);
			dou.writeInt(n);
			dou.writeInt(bk.get(n).childcount);
			if (bk.get(n).childcount > 0)
				for (long l : bk.get(n).childs)
					dou.writeLong(l);
		}
		din.close();
		dou.flush();
		dou.close();
		getDout(db).close();
		dout = null;
		File flold = new File(db.getIndexPath());
		flold.delete();
		f.renameTo(flold);
	}

	private blockidx next(DataInputStream din) throws IOException {
		return next(din, false);
	}

	private blockidx next(DataInputStream din, boolean all) throws IOException {
		int ini = 0;
		while ((ini = din.read()) > -1) {
			if (ini != 255)
				throw new NullPointerException();
			short clcode = din.readShort();
			blockidx bid = new blockidx();
			bid.ini = (byte) ini;
			bid.idxcode = clcode;
			bid.childs = new ArrayList<Long>();
			bid.id = din.readLong();
			bid.order = din.readInt();
			bid.childcount = din.readInt();
			for (int n = 0; n < bid.childcount; n++)
				bid.childs.add(din.readLong());
			// Read embedded value (may be absent in older files)
			try {
				int valLen = din.readInt();
				if (valLen > 0) {
					byte[] valBytes = new byte[valLen];
					din.readFully(valBytes);
					bid.value = decodeValue(valBytes);
				}
			} catch (java.io.EOFException e) {
				// Older format without embedded value — value stays null
			}
			if (!all && clcode != indexcode)
				continue;
			return bid;
		}
		return null;
	}

	protected static DataOutputStream getDout(dataBase db) {
		if (dout != null)
			return dout;
		synchronized (superindex.class) {
			if (dout == null) {
				try {
					dout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(db.getIndexPath(), true), 1024 * 1024));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return dout;
	}

	public void setChange(boolean change) {
		superindex.change = change;
	}

	public String getPname() {
		return pname;
	}

	protected void setPname(String pname) {
		this.pname = pname;
	}

	protected dataBase getDb() {
		return db;
	}

	protected void setDb(dataBase db) {
		superindex.db = db;
	}

	public ArrayList<superindex> getFathersIndex() {
		if (fathersIndex != null)
			return fathersIndex;
		fathersIndex = new ArrayList<superindex>();
		HashMap<String, ArrayList<String>> m;
		if ((m = getDb().getCfig().getFathers().get(getcName())) == null)
			return fathersIndex;
		for (String clasF : m.keySet())
			for (String prop : m.get(clasF))
				fathersIndex.add(getDb().getIndice(clasF, null, prop));
		return fathersIndex;
	}

	public boolean hasParent(Long id) {
		if (!getFathersIndex().isEmpty()) {
			for (superindex si : getFathersIndex()) {
				ArrayList<Long> idex = ((indexChild) si).getFathers().get(id);
				if (idex != null && !idex.isEmpty()) {
					return true;
				}
			}
			// for (Long ie : idex) {
			// ((indexternal) si).getChilds().get(ie).remove(fid);
			// si.change(ie, crud.update, null, ((indexternal)
			// si).getChilds().get(ie), null);
			// }}
		}
		return false;
	}

	public String getcName() {
		return cName;
	}

	/**
	 * Closes the index output stream and resets static state.
	 */
	public static synchronized void close() {
		try {
			if (dout != null) {
				dout.close();
				dout = null;
			}
			db = null;
			buffer.clear();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class blockidx {
	// 1 + 2 + 8 + 4 + 4 + (childs == null ? 0 : childs.size() * 8) + 4 + valueBytes;
	byte ini;
	short idxcode;
	Long id;
	int order;
	int childcount;
	ArrayList<Long> childs;
	Object value; // Embedded indexed property value

}
