package sisinfo.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Registros {

	private static final Logger LOGGERREGISTRO = Logger.getLogger( Registros.class.getName() );
	
	/*
	 * Registra la informacion obtenida durante las actualizaciones en el archivo Log y en el informe que se arma
	 * para mostrar en el inbox de las aulas pendientes. 
	 * */
	public static void RegistrarInfo(Level level,String info, AulaLog InformeAula) {
		LOGGERREGISTRO.log(level, info);
		InformeAula.setInfoerrores(level+"- "+info+".<br>"+InformeAula.getInfoerrores());
	}
	
	
}
