package org.jsndb.kore;

import org.jsndb.index.fileids;
import org.jsndb.serializer.parser;

public class loader {

	// public loader(dataBase dbase, ) {
	// dtfl = dbase.getDatafile();
	// fids = dbase.getObjIds();
	// genericClazz = gClazz;
	// }

	public static <C> C getByid(dataBase dbase, Long id, Class<C> gClazz,short clascode) {
		datafile dtfl = dbase.getDatafile();
		fileids fids = dbase.getObjIds();
		return parser.jsonToDeepBean(dtfl.read(fids.getDataOff().get(id.longValue()), (short) -1), gClazz, dbase);
	}

	public static String getJsonById(dataBase dbase, Long id,short clascode) {
		datafile dtfl = dbase.getDatafile();
		fileids fids = dbase.getObjIds();
		return dtfl.read(fids.getDataOff().get(id.longValue()), (short) -1);
	}
}
