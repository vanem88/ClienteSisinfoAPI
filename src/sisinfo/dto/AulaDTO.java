package sisinfo.dto;

import java.io.Serializable;

public class AulaDTO implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String id_aula;	
	private String nombre; 
	private String codigo; 
	private String codigo_organizacion;
	private String nombre_organizacion;
    private String peridodo;
	private PersonaDTO docente_responsable;	
	private PersonaDTO[] equipo_docente;
	private ComisionDTO[] comisiones;
	
	public AulaDTO() {}

	public String getId_aula() {
		return id_aula;
	}

	public void setId_aula(String id_aula) {
		this.id_aula = id_aula;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}
	
	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getCodigo_organizacion() {
		return codigo_organizacion;
	}

	public void setCodigo_organizacion(String codigo_organizacion) {
		this.codigo_organizacion = codigo_organizacion;
	}

	public String getNombre_organizacion() {
		return nombre_organizacion;
	}

	public void setNombre_organizacion(String nombre_organizacion) {
		this.nombre_organizacion = nombre_organizacion;
	}

	public String getPeridodo() {
		return peridodo;
	}

	public void setPeridodo(String peridodo) {
		this.peridodo = peridodo;
	}

	public PersonaDTO getDocente_responsable() {
		return docente_responsable;
	}

	public void setDocente_responsable(PersonaDTO docente_responsable) {
		this.docente_responsable = docente_responsable;
	}

	public PersonaDTO[] getEquipo_docente() {
		return equipo_docente;
	}

	public void setEquipo_docente(PersonaDTO[] equipo_docente) {
		this.equipo_docente = equipo_docente;
	}

	public ComisionDTO[] getComisiones() {
		return comisiones;
	}

	public void setComisiones(ComisionDTO[] comisiones) {
		this.comisiones = comisiones;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}	

}
