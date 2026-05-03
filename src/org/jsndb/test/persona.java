package org.jsndb.test;

import java.util.List;
import org.jsndb.annotations.jsndbObjectId;

public class persona {
	@jsndbObjectId
	private Long personaId;
	private String name;
	private int edad;
	private List<animal> mascota;
	private long fecha;
	private animal masq;

	public persona() {}

	public persona(String name, int edad, List<animal> mascota, long fech) {
		this.fecha = fech;
		this.name = name;
		this.edad = edad;
		this.mascota = mascota;
	}

	public Long getPersonaId() { return personaId; }
	public void setPersonaId(Long personaId) { this.personaId = personaId; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public int getEdad() { return edad; }
	public void setEdad(int edad) { this.edad = edad; }
	public List<animal> getMascota() { return mascota; }
	public void setMascota(List<animal> mascota) { this.mascota = mascota; }
	public long getFecha() { return fecha; }
	public void setFecha(long fecha) { this.fecha = fecha; }
	public animal getMasq() { return masq; }
	public void setMasq(animal masq) { this.masq = masq; }
}
