package server;

import javax.jdo.PersistenceManagerFactory;

public class Singleton {

  static private PersistenceManagerFactory pmf=null;
  static private Server server;
  
  
  /**---------------------------------- SERVER PRUEBA (prueba.evelia.unrc.edu.ar) ------------------------------------------------------------------------------**/
  
  //static public final String HOME_DIR = "C:\\desarrolloVane\\ClienteSisinfoAPI\\";
  //static public final String DIR_ARCHIVOS = "C:\\desarrolloVane\\ClienteSisinfoAPI\\";
  
   static public final String HOME_DIR = "/home/matias/ClienteSisinfoAPI/";
   //para probar en montagna porque desde matias no tengo permiso en /var/lib/tomcat9/webapps/
   static public final String DIR_ARCHIVOS = "/home/matias/ClienteSisinfoAPI/";
  
   
   //En produccion
 // static public final String HOME_DIR = "/opt/ClienteSisinfoAPI/";
 // static public final String DIR_ARCHIVOS = "/var/lib/tomcat9/webapps/";
   

  /**---------------------------------- LOCAL (ambiente de desarrollo, localhost) ------------------------------------------------------------------------------**/

    /*Archivos de configuraci�n*/ 
   static public final String ARCHIVO_INI_CONFIGURACION_SERVIDOR_DE_PRUEBAS_o_PRODUCCION = HOME_DIR+"config.ini";
   static private String archivoIni = ARCHIVO_INI_CONFIGURACION_SERVIDOR_DE_PRUEBAS_o_PRODUCCION;
   static public String HOME_DIR_ARCHIVOS = "";
   static public String HOME_DIR_LOGS = "";
 
   static public int cantidadBeginTransaction = 0;
   static public int cantidadCommitTransaction = 0;
   static public String driver;
   static public String  url;
   static public String userName;
   static public String password;
   
   //Tiempo de espera para consultar a la API de sisinfo si hay aulas para crear
   static public int tiempoEsperaConsultaAPI = 0;
   
   public Singleton() {}
   
   static synchronized public PersistenceManagerFactory getPmf() {
     if (server==null){
       server = new Server(archivoIni);
       generarRutasAplicacion();
     }
     return pmf;
   }
   
   static synchronized public void reiniciarBaseDatos(){
     server = new Server(archivoIni);
     generarRutasAplicacion();
   }
   
   static synchronized public void setPmf(PersistenceManagerFactory pmfParametro) {
     pmf = pmfParametro;
   }

   private static void generarRutasAplicacion(){
 	  HOME_DIR_ARCHIVOS = DIR_ARCHIVOS+Server.getPathArchivos();
 	  HOME_DIR_LOGS = DIR_ARCHIVOS+Server.getPathLogss();
 	  
   }
   
 }