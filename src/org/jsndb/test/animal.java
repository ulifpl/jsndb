package org.jsndb.test;

import java.util.HashMap;
import java.util.Map;
import org.jsndb.annotations.jsndbObjectId;

public class animal {
	@jsndbObjectId
	private Long animalId;
	private String nombre;
	private String raza;
	private persona owner;
	private Map<String, persona> dueños = new HashMap<String, persona>();

	public animal() {}

	public animal(String nombre, String raza, persona owner) {
		this.nombre = nombre;
		this.raza = raza;
		this.owner = owner;
	}

	public Long getAnimalId() { return animalId; }
	public void setAnimalId(Long animalId) { this.animalId = animalId; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public String getRaza() { return raza; }
	public void setRaza(String raza) { this.raza = raza; }
	public persona getOwner() { return owner; }
	public void setOwner(persona owner) { this.owner = owner; }
	public Map<String, persona> getDueños() { return dueños; }
	public void setDueños(Map<String, persona> dueños) { this.dueños = dueños; }
}
