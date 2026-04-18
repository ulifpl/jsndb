package org.jsndb.test;

public class lol extends l {

	public void lll() {
		super.exe();
	}

	@Override
	public void ovver() {
		System.out.println("lol.l()");
	}

	public static void main(String[] args) {
		new lol().lll();
	}
}

class l {

	public void exe() {
		ovver();
	}
	
	public void ovver() {
		System.out.println("l.l()");
	}
}