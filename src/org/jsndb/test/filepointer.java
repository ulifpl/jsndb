package org.jsndb.test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;

public class filepointer {

	@SuppressWarnings("resource")
	public static void main(String[] args) {

		try {
			String p = System.getProperty("user.dir").concat(System.getProperty("file.separator")).concat("temp.tmp");
			new File(p).createNewFile();
			RandomAccessFile rf;
			FileChannel fc = (FileChannel) Files.newByteChannel(Paths.get(p), StandardOpenOption.READ, StandardOpenOption.WRITE);
			long size = 0;
			rf = new RandomAccessFile(new File(p), "rw");
			size = rf.getChannel().size();
			rf.seek(size);
			while (size < 1024 * 1024 * 40) {
				rf.write(40);
				size++;
			}
			//System.in.read();
			rf.getFD().sync();
			//HashSet<String> hs = new HashSet<String>();
			Random r = new Random(258);
			byte[] buf = new byte[1024];
			ByteBuffer bbuf = ByteBuffer.allocate(buf.length);

			int s = (int) size - buf.length;
			long time = System.currentTimeMillis();
			System.out.println("filepointer.main()");
			for (int n = 0; n < 6_000_000; n++) {
				rf.seek(r.nextInt(s));
				rf.readFully(buf);
			}
			time = System.currentTimeMillis() - time;
			System.out.println("filepointer.main() rf  " + time);

			r = new Random(258);
			time = System.currentTimeMillis();
			int c = 0;
			for (int n = 0; n < 6_000_000; n++) {
				fc.position(r.nextInt(s));
				c = bbuf.capacity();
				while (c > 0)
					c -= fc.read(bbuf);
				bbuf.flip();
			}
			time = System.currentTimeMillis() - time;
			System.out.println("filepointer.main() fc  " + time);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
