package org.jsndb.kore;

public abstract class errorhandler {
	public synchronized void error(org.jsndb.error e) {
		e.getException().printStackTrace();
	}
}
