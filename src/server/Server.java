package server;

import java.util.*;
import utils.IniFile;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;
import persistencia.*;

public class Server{
		
	static public final String NOMBRE_SERVIDOR_PRODUCCION = "www.siat.unrc.edu.ar";
	static public String NOMBRE_SERVIDOR = "";//Si estamos en el de pruebas se deberia cambiar en configuracion.ini a locahost
	static public String RUTA_HTTP_SERVIDOR = "";	
	static public String RUTA_ANTIVIRUS = "";///f-prot/f-prot/ en Produccion y /f-prot/ en Prueba
	static public String SEMILLA_ENCRIPTACION = "";//Semilla para darle al Algoritmo de Encriptación
	static public String PATH_ARCHIVOS = ""; 
	static public String PATH_LOGS = ""; 
	static public boolean DEBUG_SIAT = true;
	static public String APLICACION = "";
	static public String SERVIDOR_EXTERNO = "";
	static public String PORT_SERVIDOR_EXTERNO = "";
	static public String GETTRANSPORT_SERVIDOR_EXTERNO = "";
	static public String USUARIO_SERVIDOR_EXTERNO = "";
	static public String CLAVE_SERVIDOR_EXTERNO = "";
	static public String HOST_SERVIDOR_EXTERNO = "";
	
	public Server(String archivoIni){
		try{
			
			System.out.println("FECHA y HORA del (RE)INICIO del SERVER ========> "+utils.Utils.hoySqlDate());
			System.out.println("HOME_DIR DE SINGLETON .................."+Singleton.HOME_DIR);
			System.out.println("DIR_ARCHIVOS DE SINGLETON........."+Singleton.DIR_ARCHIVOS);
						
			IniFile iniFile = new IniFile();
			iniFile.setNameFile(archivoIni); 

			NOMBRE_SERVIDOR = iniFile.getParametro("nombreServidor").trim();
			RUTA_HTTP_SERVIDOR = "https://"+NOMBRE_SERVIDOR+"/";

			SEMILLA_ENCRIPTACION = iniFile.getParametro("semillaEncriptacion").trim();
			
   		    PATH_ARCHIVOS = iniFile.getParametro("PATH_ARCHIVOS").trim(); 
   		    PATH_LOGS = iniFile.getParametro("PATH_LOGS").trim(); 
			
			String CONFIGURACION_DEBUG = iniFile.getParametro("DEBUG_SIAT").trim();
			if(CONFIGURACION_DEBUG!=null && CONFIGURACION_DEBUG.compareTo("si")==0)DEBUG_SIAT = true;
					
			APLICACION = iniFile.getParametro("aplicacion").trim();
			
			SERVIDOR_EXTERNO = iniFile.getParametro("servidorExterno").trim();
			HOST_SERVIDOR_EXTERNO = iniFile.getParametro("smtpHostservidorExterno").trim();
			USUARIO_SERVIDOR_EXTERNO = iniFile.getParametro("usuarioCorreoServidorExterno").trim();
			CLAVE_SERVIDOR_EXTERNO = iniFile.getParametro("claveCorreoServidorExterno").trim();
			GETTRANSPORT_SERVIDOR_EXTERNO = iniFile.getParametro("getTransportservidorExterno").trim();
			PORT_SERVIDOR_EXTERNO = iniFile.getParametro("portservidorExterno").trim();
			
			comun.ImprimirReportes.ImprimirReporte("URL("+ iniFile.getParametro("url").trim()+")");
	    	
	    	// Configuracion TJDO (Obteniendo datos del archivo de configuración siatReloaded.ini)
			String pmfClass = iniFile.getParametro("pmfClass").trim();//com.triactive.jdo.PersistenceManagerFactoryImpl
			String driver = iniFile.getParametro("driver").trim();//org.gjt.mm.mysql.Driver
			String url = iniFile.getParametro("url").trim();
			String userName = iniFile.getParametro("userName").trim();
			String password = iniFile.getParametro("password").trim();
			String autoCreateTable = iniFile.getParametro("autoCreateTable").trim();
			String validateConstraints = iniFile.getParametro("validateConstraints").trim();
			String validateTables = iniFile.getParametro("validateTables").trim();

			// Para configuracion TJDO
			Properties props_actual = new Properties();

			// Set the PersistenceManagerFactoryClass to the TJDO class.
			props_actual.setProperty("javax.jdo.PersistenceManagerFactoryClass",pmfClass);

			// Set the JDBC driver name.
			props_actual.setProperty("javax.jdo.option.ConnectionDriverName",driver);

			// Setea los los datos de la base de datos.
			props_actual.setProperty("javax.jdo.option.ConnectionUserName", userName);
			props_actual.setProperty("javax.jdo.option.ConnectionPassword", password);
			props_actual.setProperty("com.triactive.jdo.autoCreateTables", autoCreateTable);			
			props_actual.setProperty("com.triactive.jdo.validateConstraints", validateConstraints);			
			props_actual.setProperty("com.triactive.jdo.validateTables", validateTables);			

			// Set the connection URL for the Cloudscape database.
			props_actual.setProperty("javax.jdo.option.ConnectionURL",url);
			
			// SETEO LOS VALORES DE LA CONECCION A LA BASE DE DATOS PARA SER USADOS EN LA EJECUCION DE QUERIES DIRECTAMENTE EN SQL
			Singleton.driver = driver;
			Singleton.url = url;
			Singleton.userName = userName;
			Singleton.password = password;

			// Get an instance of the PersistenceManagerFactory via JDOHelper.
			PersistenceManagerFactory pmf_actual =	JDOHelper.getPersistenceManagerFactory(props_actual);

			// Inicializa el OidGenerator
			OidGenerator.init(pmf_actual);

			// setear el SINGLETON
			Singleton.setPmf(pmf_actual);				

			//	Setea el valor semilla que usa el algoritmo de encriptacion			
			server.general.Util.setString(Server.SEMILLA_ENCRIPTACION);
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	
	public static final String getPathArchivos() {
		return PATH_ARCHIVOS;
	}
	
	public static final String getPathLogss() {
		return PATH_LOGS;
	}

	
		
}