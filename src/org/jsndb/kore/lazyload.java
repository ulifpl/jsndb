package org.jsndb.kore;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsndb.index.fileids;
import org.jsndb.serializer.parser;

public class lazyload<T, C> implements InvocationHandler {
	private boolean loaded;
	private ArrayList<Long> ids;
	private Class<C> genericClazz;
	private Collection<C> objlist;
	private datafile dtfl;
	private dataBase db;
	fileids fids;
	static HashMap<Class<?>, parser> jsons = new HashMap<>();

	@SuppressWarnings("unchecked")
	public lazyload(ArrayList<Long> ides, Class<C> gClazz, dataBase dtbs) {
		// db = dtbs;
		genericClazz = gClazz;
		ids = ides;
		dtfl = dtbs.getDatafile();
		fids = dtbs.getObjIds();
		db = dtbs;
		try {
			if (List.class.isAssignableFrom(gClazz) && !List.class.isInterface())
				objlist = (List<C>) gClazz.getDeclaredConstructor().newInstance();
			else
				objlist = new ArrayList<C>();
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static <C, T> T create(ArrayList<Long> ides, Class<C> genericClazz, Class<T> propClazz, dataBase dtbs) {

		switch (propClazz.getName()) {
		case "java.util.HashMap":
		case "java.util.Map":
			return (T) Proxy.newProxyInstance(genericClazz.getClassLoader(), new Class[] { Map.class }, new lazyload<T, C>(ides,
					genericClazz, dtbs));
		case "java.util.List":
		case "java.util.Set":
		case "java.util.ArrayList":
			return (T) Proxy.newProxyInstance(genericClazz.getClassLoader(), new Class[] { List.class }, new lazyload<T, C>(ides,
					genericClazz, dtbs));
			// case "java.util.ArrayList":
			// return (T) new lazyArrayList<C>(dtbs,genericClazz, ides);
			// case "java.util.HashMap":
			// return (T) new lazyArrayList<C>(dtbs,genericClazz,ides);
		default:
			break;
		}

		return null;
	}

	@Override
	public Object invoke(Object obj, Method methd, Object[] args) {
		if (!loaded) {
			List<C> listemp = new ArrayList<C>(ids.size());
			for (Long id : ids) {
				// must be fixed
				Long off = fids.getDataOff().get(id.longValue());
				//if (off == null)
				//	continue;
				listemp.add(parser.jsonToDeepBean(dtfl.read(off, (short) -1), genericClazz, db));
			}
			loaded = true;
			objlist.addAll(listemp);

		}
		try {
			return methd.invoke(objlist, args);
		} catch (InvocationTargetException e) {
			e.getCause().printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
