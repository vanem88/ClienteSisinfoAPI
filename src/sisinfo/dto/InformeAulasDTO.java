package sisinfo.dto;

import java.io.Serializable;
import java.util.Vector;

//Estructura JSON para enviar respuesta a la API de sisinfo con los problemas encontrados.
public class InformeAulasDTO implements Serializable {

	private static final long serialVersionUID = 1L;
	private String id_aula;
	private int status;//1 se creo el aula - 0 no se creo el aula
	private Vector informe = new Vector();//Vector de String informando los errores ocurridos!
	
	public InformeAulasDTO() {}
		
	public String getId_aula() {
		return id_aula;
	}

	public void setId_aula(String id_aula) {
		this.id_aula = id_aula;
	}


	public int getStatus() {
		return status;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
	public Vector getInforme() {
		return informe;
	}
	
	public void setInforme(Vector informe) {
		this.informe = informe;
	}
	
}
