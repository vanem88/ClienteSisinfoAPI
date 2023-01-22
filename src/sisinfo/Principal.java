package sisinfo;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import server.Singleton;
import server.datosGenerales.ControlDatosGenerales;
import utils.IniFile;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import comun.constantes.DATOS_GENERALES;
import persistencia.Persistencia;
import persistencia.dominio.AulaSisinfoNovedades;
import persistencia.dominio.DatosGenerales;

public class Principal {
	
	private static final Logger LOG_SISINFO = Logger.getLogger("sisinfo");	
	private static final Logger LOGGER = Logger.getLogger( Principal.class.getName() );
    
	public static void main(String[] args) {
		Handler fileHandler = null;
		SimpleDateFormat format = new SimpleDateFormat("d-M-Y"); 
		
		try {			
			//Consulto si hay un proceso ejecutandose
			if(terminoProceso()) {				
				try {			    	
				    if(CargarDatosGenerales()) {
				    	try {
							fileHandler = new FileHandler(Singleton.HOME_DIR_LOGS+"/clienteAPI_"+format.format(Calendar.getInstance().getTime()) + ".log",true);				
				        } catch (Exception e) {
				        	File directorioLogs = new File(Singleton.HOME_DIR_LOGS+"/creacionAulas");
				        	if(!directorioLogs.mkdirs()) e.printStackTrace();
				        	else fileHandler = new FileHandler(Singleton.HOME_DIR_LOGS+"/clienteAPI_"+format.format(Calendar.getInstance().getTime()) + ".log",true);
					    }
						
						SimpleFormatter simpleFormatter = new SimpleFormatter();
			            fileHandler.setFormatter(simpleFormatter);
			            fileHandler.setLevel(Level.ALL);                     
			            LOG_SISINFO.addHandler(fileHandler); 
											      
			            //Lee el archivo de configuracion
			            String archivoIni = Singleton.ARCHIVO_INI_CONFIGURACION_SERVIDOR_DE_PRUEBAS_o_PRODUCCION;			            
			            IniFile iniFile = new IniFile();
						iniFile.setNameFile(archivoIni); 		
						String usuario = iniFile.getParametro("usuarioApi");
						String contrasena = iniFile.getParametro("contrasenaApi");
						String urlsolicitud_aulas = iniFile.getParametro("urlsolicitud_aulas");
						String urlnovedades_aulas = iniFile.getParametro("urlnovedades_aulas");
						
					    if(usuario!= null && usuario != "" && contrasena!= null && contrasena != "") {
						    	LOGGER.log(Level.INFO, "Consultando si existen aulas a crear ...........");
								CreacionAulas restClient = new CreacionAulas();
								restClient.CrearAulas(usuario,contrasena,urlsolicitud_aulas);
												
								LOGGER.log(Level.INFO, "Consultando si existen novedades en las aulas ....................................................................");
								NovedadesAulas novedadesAulas = new NovedadesAulas();
								novedadesAulas.VerificaAulaSinActualizar(urlnovedades_aulas,usuario,contrasena);
														
								Timestamp fechaDesde = ObtenerFechaUltimaConsultaNovedades();
								if(fechaDesde == null) {
									fechaDesde = ObtenerFechaPrimerAulaCreada();
									if(fechaDesde != null) ModificarFechaUltimaConsulta(fechaDesde);
								}
								Date now = new java.util.Date();
						        Timestamp fechaHasta = new java.sql.Timestamp(now.getTime());				       
						              
						       	novedadesAulas.ConsultaNovedadesDatosBasicos(urlnovedades_aulas,usuario,contrasena,fechaDesde,fechaHasta,"");
								ModificarFechaUltimaConsulta(fechaHasta);
								LOGGER.log(Level.INFO, "Finalizo el proceso y se desbloqueo ....................................................................");						
						}					
					}				    
				}catch (Exception e) {					
					LOGGER.log(Level.SEVERE, e.getMessage() + "Exception, Por desbloquear el proceso ....................................................................");
					DesbloquearProceso();					
				} 
			}			
			if(fileHandler!=null) fileHandler.close();	
		}catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception main.");
			if(fileHandler!=null) fileHandler.close();	
		} 		
	}
	
	
	
	//Obtiene la fecha de a ultima consulta que se realizo, para consultar a partir de la misma.
	private static Timestamp ObtenerFechaUltimaConsultaNovedades() throws Exception {
		Persistencia persistencia  = new Persistencia();
		try {			
			persistencia.crearTransaccion();
			Vector aulaSisinfoNovedades = persistencia.getObjectosPorClase("persistencia.dominio.AulaSisinfoNovedades");
			AulaSisinfoNovedades aulaNov = (AulaSisinfoNovedades)aulaSisinfoNovedades.elementAt(0);
			Timestamp fechaUltimaActualizacion = aulaNov.getFechaUltimaConsulta();			
			persistencia.commit();				
			return fechaUltimaActualizacion;				
		}catch(Exception ex){
			persistencia.rollback();
			LOGGER.log(Level.SEVERE, "Exception ObtenerFechaUltimaConsultaNovedades.");
			throw new Exception("EXCEPTION ObtenerFechaUltimaConsultaNovedades. "+ex.getMessage());			
		}		
	}
	
	//Se ejecuta al inicio del proceso
	private static boolean terminoProceso(){
		Persistencia persistencia  = null;
		try {			
			persistencia  = new Persistencia();
			persistencia.crearTransaccion();
			Vector aulaSisinfoNovedades = persistencia.getObjectosPorClase("persistencia.dominio.AulaSisinfoNovedades");
			if(aulaSisinfoNovedades!=null && aulaSisinfoNovedades.size()>0) {
				AulaSisinfoNovedades aulaNov = (AulaSisinfoNovedades)aulaSisinfoNovedades.elementAt(0);
				if(aulaNov.getTerminoProceso()==1) {
					aulaNov.setTerminoProceso(0);
					persistencia.commit();
					return true;
				}else {
					persistencia.commit();
					return false;
				}	
			}else {
				AulaSisinfoNovedades aula = new AulaSisinfoNovedades();
				aula.setTerminoProceso(0);
				persistencia.hacerPersistente(aula);
				persistencia.commit();
				return true;
			} 
		}catch(Exception ex){
			persistencia.rollback();
			LOGGER.log(Level.SEVERE, "Exception terminoProceso.");
			return false;
		}
	}
	
	
	//Se ejecuta cuando finaliza todo el proceso
	private static void DesbloquearProceso() throws Exception {
		Persistencia persistencia  = new Persistencia();
		try {			
			persistencia.crearTransaccion();
			Vector aulaSisinfoNovedades = persistencia.getObjectosPorClase("persistencia.dominio.AulaSisinfoNovedades");
			if(aulaSisinfoNovedades!=null && aulaSisinfoNovedades.size()>0) {
				AulaSisinfoNovedades aula = (AulaSisinfoNovedades) aulaSisinfoNovedades.elementAt(0);
				aula.setTerminoProceso(1);						
			}else {
				AulaSisinfoNovedades aula = new AulaSisinfoNovedades();
				aula.setTerminoProceso(1);
				persistencia.hacerPersistente(aula);
			}
			persistencia.commit();	
		}catch(Exception ex){
			LOGGER.log(Level.SEVERE, "Exception DesbloquearProceso. "+ex.getMessage());
			persistencia.rollback();
			throw new Exception("EXCEPTION DesbloquearProceso. "+ex.getMessage());
		}
	}
	
	//Se ejecuta cuando finaliza todo el proceso
	private static void ModificarFechaUltimaConsulta(Timestamp fechaActualizada) throws Exception {
		Persistencia persistencia  = new Persistencia();
		try {			
			persistencia.crearTransaccion();
			Vector aulaSisinfoNovedades = persistencia.getObjectosPorClase("persistencia.dominio.AulaSisinfoNovedades");
			if(aulaSisinfoNovedades!=null && aulaSisinfoNovedades.size()>0) {
				AulaSisinfoNovedades aula = (AulaSisinfoNovedades) aulaSisinfoNovedades.elementAt(0);
				aula.setTerminoProceso(1);
				aula.setFechaUltimaConsulta(fechaActualizada);				
			}else {
				AulaSisinfoNovedades aula = new AulaSisinfoNovedades();
				aula.setTerminoProceso(1);
				aula.setFechaUltimaConsulta(fechaActualizada);
				persistencia.hacerPersistente(aula);
			}
			persistencia.commit();	
		}catch(Exception ex){
				LOGGER.log(Level.SEVERE, "Exception ModificarFechaUltimaConsulta. " +ex.getMessage());
				persistencia.rollback();
				throw new Exception("EXCEPTION ModificarFechaUltimaConsulta. "+ex.getMessage());
		}
	}
	
	private static Timestamp ObtenerFechaPrimerAulaCreada() {
		Persistencia persistencia  = new Persistencia();
		Connection conexionSQL = null;
		ResultSet resultado = null;
		Statement stmt = null;
		try {			
			persistencia.crearTransaccion();
			conexionSQL = DriverManager.getConnection(Singleton.url,Singleton.userName,Singleton.password);
			stmt = conexionSQL.createStatement();      
			resultado = stmt.executeQuery("SELECT Min(FECHA_CREACION) as fecha FROM `AULA`");		
		    resultado.next();
		    Timestamp fechaUltimaActualizacion = resultado.getTimestamp("fecha");
			if (resultado!= null) resultado.close();
			if (stmt!= null) stmt.close();
			persistencia.commit();	
			return fechaUltimaActualizacion;				
		}catch(Exception ex){
			try {
				LOGGER.log(Level.SEVERE, "Exception ObtenerFechaUltimaActualizacion.");
				persistencia.rollback();
				if (resultado!= null) resultado.close();
				if (stmt!= null) stmt.close();
			}catch(Exception exx){}
			return null;			
		}
	}
	
	private static boolean CargarDatosGenerales() {
		Persistencia persistencia = new Persistencia();
		try {			
			persistencia.crearTransaccion();				
			ControlDatosGenerales controlDatosGenerales = new ControlDatosGenerales(persistencia);
			DatosGenerales dato = controlDatosGenerales.obtenerDatosGenerales();
			
			DATOS_GENERALES.HOST_CORREO=dato.getHostCorreo();
			DATOS_GENERALES.USUARIO_CORREO=dato.getUsuarioCorreo();
			DATOS_GENERALES.CLAVE_CORREO=dato.getClaveCorreo();
			DATOS_GENERALES.PUERTO_CORREO=dato.getPort();
			DATOS_GENERALES.CANTIDAD_CORREOS_POR_MAIL=dato.getCantidadCorreosPorMail();				
			DATOS_GENERALES.MAIL_CONTACTO=dato.getMailContacto();
			DATOS_GENERALES.MAIL_ALMACENAMIENTO=dato.getMailAlmacenamiento();
			DATOS_GENERALES.MAIL_CREACION_AULAS=dato.getMailCreacionAulas();
			DATOS_GENERALES.SIGLA_SISTEMA= dato.getSiglaSistema();
			DATOS_GENERALES.HABILITAR_MENSAJERIA_INTERNA = dato.isHabilitarMensajeriaInterna();
			DATOS_GENERALES.HABILITAR_NOTIFICACIONES = dato.isHabilitarNotificaciones();
			DATOS_GENERALES.MAIL_CONTACTO = dato.getMailContacto();
			DATOS_GENERALES.NOTIFICAR_POR_MAIL = dato.isNotificarPorMail();
			DATOS_GENERALES.NOMBRE_INSTITUCION = dato.getNombreInstitucion();
			DATOS_GENERALES.SALUDO_MAIL_GENERICO = dato.getSaludoMailGenerico();		
			
			persistencia.commit();
			return true;
		}catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Exception CargarDatosGenerales.");
			persistencia.rollback();
			return false;
		} 	
	}
	
}
