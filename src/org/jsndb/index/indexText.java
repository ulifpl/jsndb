package org.jsndb.index;

import java.util.ArrayList;
import java.util.HashMap;

import org.jsndb.kore.enums.crud;

/**
 * Placeholder for full-text search index implementation.
 * Currently not implemented.
 */
public class indexText extends superindex {

	@Override
	public void change(Long id, crud c, HashMap<String, Object> o, ArrayList<Long> arrayList,
			HashMap<Long, HashMap<String, Object>> cache) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void add(Long id, ArrayList<Long> childs, int order, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	protected ArrayList<blockidx> getBlokcs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean isFragmented() {
		// TODO Auto-generated method stub
		return false;
	}

}
