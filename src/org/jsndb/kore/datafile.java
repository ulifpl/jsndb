package org.jsndb.kore;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import org.jsndb.error;
import org.jsndb.error.errortype;
import org.jsndb.index.idOffset;
import org.jsndb.util.logger;
import org.jsndb.util.mapList;

public abstract class datafile {

	private RandomAccessFile rf;
	private String path;

	public static datafile getInstance(String p) {
		datafile dat = new datafile() {
		};
		try {
			dat.path = p;
			dat.rf = new RandomAccessFile(Paths.get(dat.path).toFile(), "rw");
			if (dat.rf.length() == 0)
				return dat;
			dat.size = dat.rf.length();
			return dat;
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("can't read file:" + dat.path, errortype.externalException, e));
		}
		dat.close();
		return null;
	}

	public synchronized void close() {
		try {
			if (in != null) {
				in.close();
				in = null;
			}
			if (rf != null) {
				rf.close();
				rf = null;
			}
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("can't close datafile resources", errortype.externalException, e));
		}
	}

	public static int countread;

	long from, to;
	byte[] lon = new byte[100];

	public String read(Long offset, short clscode) {

		temptime = System.nanoTime();
		countread++;
		try {
			rf.seek(offset);
			int size = 0;
			short ccode = 0;
			ByteBuffer bb = ByteBuffer.wrap(lon);
			int len = lon.length - 5;
			long rfl=rf.length();
			long poi=rf.getFilePointer();
			if (len > (rf.length() - rf.getFilePointer()))
				len = (int) (rf.length() - rf.getFilePointer());
			rf.readFully(lon, 5, len);
			ccode = bb.asShortBuffer().get(3);
			size = bb.asIntBuffer().get(4);
			if (clscode >= 0) {
				if (clscode != ccode) {
					postime += System.nanoTime() - temptime;
					return null;
				}
			}
			if ((size + 20) > lon.length) {
				lon = new byte[size + 20];
				rf.seek(offset);
				len = lon.length - 5;
				if (len > (rf.length() - rf.getFilePointer()))
					len = (int) (rf.length() - rf.getFilePointer());
				rf.readFully(lon, 5, len);
			}
			postime += System.nanoTime() - temptime;
			return new String(lon, 20, size, "UTF-8");
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("can't read file:", errortype.externalException, e));
		}
		return null;
	}

	public static int count = 0;
	public static int count2 = 0;
	private long size;

	long temptime;
	public static long postime;

	DataInputStream in;

	public void resetInputStream() {
		try {
			if (in != null)
				in.close();
			in = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(path))));
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("database rota:", errortype.dbBrokenError, e));
		}
	}

	public String next(short clazzcode, long[] id) {
		try {
			while (in.read() > -1) {
				if (in.readShort() == clazzcode) {
					id[0] = in.readLong();
					byte[] b = new byte[in.readInt()];
					in.readFully(b);
					return new String(b, "UTF-8");
				}
				in.readLong();
				int n = in.readInt();
				while (n > 0)
					n -= in.skipBytes(n);
			}
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("database broken:", errortype.dbBrokenError, e));
		}
		return null;
	}

	public ArrayList<String> read(ArrayList<idOffset> pairs) {
		ArrayList<String> list = new ArrayList<String>(pairs.size());
		for (idOffset pair : pairs) {
			if (pair.getOffset() < size) {
				list.add(read(pair.getOffset(), (short) -1));
			} else {
				list.add("");
			}
		}
		return list;
	}

	ByteBuffer bbw = ByteBuffer.allocate(1_000_000);
	static int n = 0;

	public idOffset write(String s, long id, short clazzcode) {
		try {
			byte[] b = s.getBytes("UTF-8");

			while (bbw.capacity() - bbw.position() < (b.length + 1 + 2 + 8 + 4)) {
				if (bbw.position() == 0)
					bbw = ByteBuffer.allocate((int) (bbw.capacity() * 1.25));
				n++;
				if (bbw.position() > 0)
					flush();
			}
			int offset = bbw.position();
			bbw.put((byte) 255);
			bbw.putShort(clazzcode);
			bbw.putLong(id);
			bbw.putInt(b.length);
			bbw.put(b);
			return new idOffset(id, size + offset);
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("can't write file:", errortype.externalException, e));
		}
		return null;
	}

	public void compact(mapList<Long, Long> offs) throws IOException {
		logger.line("~~~~~~datafile.compact()");
		RandomAccessFile rfold = rf;
		// File fl = Paths.get(path.concat(".tmp")).toFile();
		Path tmpfile = Paths.get(path.concat(".tmp"));
		rf = new RandomAccessFile(tmpfile.toFile(), "rw");
		rf.setLength(0);
		size = 0;
		int n = 0;
		for (; n < offs.size(); n++) {
			rfold.seek(offs.getEnryByIndex(n).getValue());
			rfold.read();
			short clcde = rfold.readShort();
			long id = rfold.readLong();
			int size = rfold.readInt();
			lon = new byte[size];
			rfold.readFully(lon, 0, size);
			Long o = write(new String(lon, "UTF-8"), id, clcde).getOffset();
			offs.put(offs.getKeyByIndex(n), o);
		}
		flush();
		rfold.close();
		rf.close();
		Path realfl = Paths.get(path);
		Files.move(tmpfile, realfl, StandardCopyOption.REPLACE_EXISTING);
		rf = new RandomAccessFile(realfl.toFile(), "rw");
	}

	public boolean flush() {
		try {
			bbw.flip();
			rf.seek(rf.length());
			rf.write(bbw.array(), 0, bbw.limit());
			rf.getFD().sync();

			bbw.clear();
			size = rf.length();
			if (bbw.capacity() > 1_000_000)
				bbw = ByteBuffer.allocate(1_000_000);
			return true;
		} catch (IOException e) {
			dataBase.getEhandler().error(new error("can't flush datafile:", errortype.externalException, e));
		}
		return false;
	}
}
