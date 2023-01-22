package sisinfo.util;

//Utilizada para pasar los string como parametro "por referencia"
public class AulaLog {
	
	//En infoaula se guardara todo lo que se escribira en el archivo log sobre la creacion correcta del aula.
	//ClienteSisinfoAPI\log\creacionAulas\aulas_automaticas_creadas_fecha.log
	private String infoaula = "";
	
	//En infoerrores se guardara todo lo que se mostrara en el inbox en Evelia sobre los problemas encontrados.
	//Este String tiene los problemea encontrados durante la solicitud para "completar" el aula.
	//En Secreataria del Campus, un Webmaster podra acceder a "Visualizar Aulas Pendientes Sisinfo"
	private String infoerrores = "";
	
	public AulaLog(){}
	
	public String getInfoaula() {
		return infoaula;
	}
	public void setInfoaula(String infoaula) {
		this.infoaula = infoaula;
	}

	public String getInfoerrores() {
		return infoerrores;
	}

	public void setInfoerrores(String infoerrores) {
		this.infoerrores = infoerrores;
	}

	
	
	
}
