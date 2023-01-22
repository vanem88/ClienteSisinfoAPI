package sisinfo.dto;

import java.io.Serializable;

public class ComisionDTO implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String nombre;
	PersonaDTO[] estudiantes;
	PersonaDTO[] docentes; //solo dni
	

	public ComisionDTO() {}
	
	public String getNombre() {
		return nombre;
	}
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	public PersonaDTO[] getEstudiantes() {
		return estudiantes;
	}
	public void setEstudiantes(PersonaDTO[] estudiantes) {
		this.estudiantes = estudiantes;
	}
	public PersonaDTO[] getDocentes() {
		return docentes;
	}
	public void setDocentes(PersonaDTO[] docentes) {
		this.docentes = docentes;
	}

	
	
}
