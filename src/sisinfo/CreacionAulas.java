package sisinfo;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.JsonException;
import org.apache.http.entity.StringEntity;
import com.google.gson.Gson;
import com.sun.jdori.common.sco.Date;

import comun.constantes.DATOS_GENERALES;
import comun.constantes.ESTADO_AULAS;
import comun.constantes.TIPO_USUARIOS;
import comun.constantes.USUARIO_AGREGADO_DESDE;
import persistencia.Persistencia;
import persistencia.dominio.Aula;
import persistencia.dominio.AulaCompuesta;
import persistencia.dominio.Curso;
import persistencia.dominio.Organizacion;
import persistencia.dominio.Persona;
import persistencia.dominio.TipoUsuario;
import server.Singleton;
import utils.ManejoArchivoDeTexto;
import utils.ManejoString;
import utils.Utils;
import utils.UtilsGenerarHtml;
import sisinfo.control.ControlCreacionAula;
import sisinfo.dto.*;
import sisinfo.util.AulaLog;

public class CreacionAulas {
	
	private Persistencia persistencia;
	private ControlCreacionAula controlAulaCliente;
	private Gson gson;
	Vector<String> aulasCreadas;
	
	private static final Logger LOGGER = Logger.getLogger( CreacionAulas.class.getName() );

	public CreacionAulas() {
		gson = new Gson();		
	}
		
	/*
	 * Crea las aulas obtenidas en un json al consumir la API de sisinfo
	 * @param usuario
	 * @param contrasena
	 */
	public void CrearAulas(String usuario, String contrasena,String urlsolicitud_aulas) throws Exception{
		String error = "";
		int aulaCreada = 0;
		
		//Informe de las aulas creadas o no con su respectivo informe que se envia a la API
		Vector informesAulas = new Vector();
		aulasCreadas = new Vector();
				
		try {	    	
			/////////////////////////////////////////
			//Consulto si hay aulas para crear
			/////////////////////////////////////////
			String resultadoAulas = ComunicacionHTTP.HTTPGet(urlsolicitud_aulas,usuario, contrasena,null,null);
			if(resultadoAulas!="[]")
				LOGGER.log(Level.INFO, "Json de aulas a crear: "+resultadoAulas);
			
			AulaDTO[] aulas = null;
			try {
				aulas = gson.fromJson(resultadoAulas, AulaDTO[].class); 				
			}catch(Exception jsone) {
				LOGGER.log(Level.SEVERE, "JsonException Json de aulas a crear");
			}
				
			ControlCreacionAula controlAulaCliente = null;
			if(aulas != null && aulas.length > 0) {
				Date dt = new Date();
		        int anio = dt.getYear() + 1900;
		        
		        for(int i = 0;i < aulas.length;i++) {							
					AulaDTO aula = aulas[i];
					if(aula!=null) {
						//Informe que se enviara a la API de sisinfo.
						InformeAulasDTO informeAulaJSON = new InformeAulasDTO();
						try {
							persistencia  = new Persistencia();
							controlAulaCliente = new ControlCreacionAula(persistencia);					
							persistencia.crearTransaccion();
							aulaCreada = 0;						
						    informeAulaJSON.setId_aula(aula.getId_aula());
						   
							Vector aulasSisInfo = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.AulaCompuesta", "nombre", "idAulaSisinfo == \""+aula.getId_aula()+"\"",true);
							AulaCompuesta aulaSisinfo = null;
							if(aulasSisInfo!= null && aulasSisInfo.size()>0)
								aulaSisinfo = (AulaCompuesta)aulasSisInfo.elementAt(0);							
																			
							boolean yaCreada = false;
							if(aulaSisinfo==null) {//Si el aula no existe
								Long codOrganizacion = new Long(aula.getCodigo_organizacion());										
								Vector organizaciones = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Organizacion", "nombre", "codigoAulaSisinfo == \""+aula.getCodigo_organizacion()+"\"",true);
								Organizacion organizacion = null;
								if(organizaciones!= null && organizaciones.size()>0)
								     organizacion = (Organizacion)organizaciones.elementAt(0);
																
								if(organizacion==null) { 
									//Envia mail solicitando crear una nueva organizacion.
									String cuerpo = "Hola\n\n";
									cuerpo +="Desde el servicio de creación de aulas automáticas Sisinfo - Evelia se solicita la creación de una organización con los siguientes datos: \n\n";
									cuerpo +="Código de la organización: "+codOrganizacion+"\n";
									cuerpo +="Nombre de la organización: "+aula.getNombre_organizacion()+"\n";
									cuerpo +="Muchas Gracias\n\n";
									cuerpo +="Creación de aulas automáticas Sisinfo - Evelia";
										
									String mensaje = controlAulaCliente.enviarMail(DATOS_GENERALES.MAIL_CONTACTO,cuerpo);
									if(mensaje == "")
										mensaje = "Se envió un e-mail solicitando la creación de una organización con código: "+codOrganizacion;	
									else
										mensaje = mensaje+". Error al intentar enviar el e-mail solicitanco la creación de una organización.";
									informeAulaJSON.setStatus(0);
									informeAulaJSON.getInforme().add(mensaje);	
									LOGGER.log(Level.SEVERE, mensaje);								
								}else {			
									//buscar responsable con dni para luego obtener el id
									PersonaDTO docenteResponsable = aula.getDocente_responsable();
									if(docenteResponsable!=null) {
										String dniResponsable = docenteResponsable.getNumero_documento();	
										if(dniResponsable!=null && dniResponsable.compareTo("")!=0) {
											//Controlo el docente responsable, si no existe lo registro en el evelia
											String respuesta = "Ya existe";
											Persona respAdmi = (Persona)controlAulaCliente.buscarPersona(dniResponsable);
											if(respAdmi==null)
												respuesta = controlAulaCliente.agregarNuevaPersona(docenteResponsable.getApellido(), docenteResponsable.getNombre(), docenteResponsable.getTipo_documento(),dniResponsable , docenteResponsable.getEmail(),docenteResponsable.getTelefono());
												
											if(respuesta.compareTo("Ya existe")==0 || respuesta.compareTo("Creado")==0) {
												if(respuesta.compareTo("Ya existe")==0 )
													controlAulaCliente.ModificarDatosPersonales(docenteResponsable,respAdmi);
												
												String numeroDoc = ManejoString.eliminarPuntosDelDocumento(dniResponsable);
												respAdmi = (Persona)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc == \""+numeroDoc+"\"").elementAt(0);
												//Se crea el aula solo con el usuario responsable.
												Curso curso = controlAulaCliente.creaAulaDatosMinimos(aula.getNombre()+" ("+aula.getCodigo()+") - "+String.valueOf(anio),aula.getCodigo(), codOrganizacion, respAdmi, aula.getId_aula(),aula.getPeridodo());
												if(curso != null) {
													informeAulaJSON.setStatus(1);
													aulaCreada = 1;
													aulasCreadas.add(curso.getIdAulaSisinfo());
													LOGGER.log(Level.INFO, "SE CREO EL AULA "+ curso.getNombre());																								
												}else {
													informeAulaJSON.setStatus(0);
													informeAulaJSON.getInforme().add("EL AULA "+ curso.getNombre()+" NO SE PUEDO CREAR");	
													LOGGER.log(Level.SEVERE, "EL AULA "+ curso.getNombre()+" NO SE PUEDO CREAR");													
												}											
											}else {
												informeAulaJSON.setStatus(0);
												if(respuesta.compareTo("Correo invalido")==0) {
													informeAulaJSON.getInforme().add("No se pudo registrar al responsable del aula "+docenteResponsable.getNumero_documento()+" porque su email "+docenteResponsable.getEmail()+" es inválido");
													LOGGER.log(Level.SEVERE, "No se pudo registrar al responsable del aula "+docenteResponsable.getNumero_documento()+" porque su email "+docenteResponsable.getEmail()+" es invalido");
												}else { 
													informeAulaJSON.getInforme().add("Ocurrió un error cuando se intento crear al responsable del aula. Controle sus datos.");
													LOGGER.log(Level.SEVERE, "Ocurrió un error cuando se intento crear al responsable del aula. Controle sus datos.");
												}
											}
										}else {
											error = "No se envió el DNI del docente responsable para el aula.";
											informeAulaJSON.setStatus(0);
											informeAulaJSON.getInforme().add(error);	
											LOGGER.log(Level.SEVERE, error);	
										}	
									}else {
										error = "No se envió un docente responsable para el aula.";
										informeAulaJSON.setStatus(0);
										informeAulaJSON.getInforme().add(error);	
										LOGGER.log(Level.SEVERE, error);	
									}	
								}								
							}else {
								error = "El aula "+aulaSisinfo.getIdAulaSisinfo()+" fue creada el día "+aulaSisinfo.getFechaCreacion();
								informeAulaJSON.setStatus(1);
								informeAulaJSON.getInforme().add(error);
								LOGGER.log(Level.INFO, error);	
								yaCreada = true;
							}							
							if(aulaCreada == 1) { 
								persistencia.commit();									
						    }else {
								persistencia.rollback();	
								if(!yaCreada)
									AulasPendientesEIncompletas.RegistrarAulaSisinfoPendiente(aula,informeAulaJSON,"",ESTADO_AULAS.SIN_CREAR);										
							}
							
							informesAulas.add(informeAulaJSON);	
						}catch (Exception e) {							
							error = "No se pudo crear el aula. Revisar el log.";
							informeAulaJSON.setStatus(0);
							informeAulaJSON.getInforme().add(error);	
							informesAulas.add(informeAulaJSON);	
							LOGGER.log(Level.SEVERE, error+" ROLLBACK .....................................");	
							AulasPendientesEIncompletas.RegistrarAulaSisinfoPendiente(aula,informeAulaJSON,"",ESTADO_AULAS.SIN_CREAR);
							persistencia.rollback();
						}						
						
					}//if aula!=null
				}//for aulas
				
				
				/////////////////////////////////////////
				//ENVIO ESTADO DE LAS AULAS
				/////////////////////////////////////////
				try {
					LOGGER.log(Level.INFO, "Enviando informe a la API ........................ ");
					StringEntity informe = new StringEntity(gson.toJson(informesAulas),"UTF-8");						
					ComunicacionHTTP.HTTPPost(urlsolicitud_aulas, usuario, contrasena, informe);					
				}catch(Exception ex){
					LOGGER.log(Level.SEVERE, "Error al enviar respuesta de aulas creadas."+ex.getMessage());
				}	
				
				//////////////////////////////////////////////////////////////
				//SOLICITO COMISIONES Y EQUIPO DOCENTE DE LAS AULAS CREADAS
				/////////////////////////////////////////////////////////////			
				if(aulasCreadas!= null && !aulasCreadas.isEmpty()) {
					for(int i = 0; i< aulasCreadas.size();i++) {
						String idAula = (String) aulasCreadas.get(i);
						try {	
							LOGGER.log(Level.INFO, "Consultando más datos del aula: "+idAula+" ........");
							String datosAula = ComunicacionHTTP.HTTPGet(urlsolicitud_aulas+"/"+idAula+"/docentes_estudiantes",usuario, contrasena,null,null);
							LOGGER.log(Level.INFO, "JSon aula a completar: "+datosAula);							
							CargarComisionesYEquipoDocente(datosAula);
						}catch(Exception ex){
							LOGGER.log(Level.SEVERE, "Error al solicitar las comisiones y el equipo docente del aula: "+idAula);								
						}														
					}					
				}else LOGGER.log(Level.INFO, "No se creo ningun aula.");					
			}else	LOGGER.log(Level.INFO, "No hay aulas para crear!");	
							
		}catch (java.lang.NullPointerException e) { 
			throw new Exception("Error al inicializar la base de datos al solicitar las aulas. "+e.getMessage());
		}catch (java.net.ConnectException e) { 
			throw new Exception("Error al solicitar las aulas. java.net.ConnectException. "+e.getMessage());
		}catch (java.io.IOException e) { 
			throw new Exception("Error al solicitar las aulas. java.io.IOException. "+e.getMessage());
		}catch (Exception e) { 
			throw new Exception("Error al solicitar las aulas. Exception. "+e.getMessage());
		}			
	}
	
		
	/*
	 * Carga las comisiones y el equipo docente a un aula ya creada.
	 * @param JSON datosAula
	 */
	private void CargarComisionesYEquipoDocente(String datosAula) throws Exception {
		Curso aulaSisinfo = null;
		int cargaOK = 0;
		AulaLog InformeAula = new AulaLog();
			
		try {
			persistencia  = new Persistencia();
			controlAulaCliente = new ControlCreacionAula(persistencia);					
			persistencia.crearTransaccion();
									
			AulaDTO aulaDTO = null;
			try {
				aulaDTO =  (AulaDTO) gson.fromJson(datosAula, AulaDTO.class);
			}catch(Exception e) {
				LOGGER.log(Level.SEVERE, "Exception JsonException al buscar datos del aula.");
			}			
			if(aulaDTO != null) {					
				//LOGGER.log(Level.INFO, "Aula a completar: "+aulaDTO.getId_aula());				
				try {
					aulaSisinfo = (Curso)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Curso", "nombre", "idAulaSisinfo == \""+aulaDTO.getId_aula()+"\"",true).elementAt(0);							
				}catch(Exception ex) {}

				if(aulaSisinfo!=null) {//Si el aula esta creada						
					//Controlo el equipo docente, si no existen los registro en el evelia
					PersonaDTO[] equipoDocente = aulaDTO.getEquipo_docente();
					for(int p = 0;p < equipoDocente.length;p++) {						
						try {
							PersonaDTO equipoDocenteJSON =  equipoDocente[p];							
							String respuesta2 = "";
							try {
								respuesta2 = controlAulaCliente.agregarNuevaPersonayEnviaMail(equipoDocenteJSON.getApellido(), equipoDocenteJSON.getNombre(), equipoDocenteJSON.getTipo_documento(), equipoDocenteJSON.getNumero_documento() , equipoDocenteJSON.getEmail(),equipoDocenteJSON.getTelefono());
							}catch(Exception e) {}
														
							if(respuesta2.compareTo("Ya existe")==0 || respuesta2.compareTo("Creado")==0) {
								String numeroDoc = ManejoString.eliminarPuntosDelDocumento(equipoDocenteJSON.getNumero_documento());
								Persona personaed = (Persona)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc == \""+numeroDoc+"\"").elementAt(0);
								if(respuesta2.compareTo("Creado")==0) {
									personaed.setCreadaDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);	
									LOGGER.log(Level.INFO, "Se registro en evelia el docente "+ equipoDocenteJSON.getNumero_documento());	
								}else
									controlAulaCliente.ModificarDatosPersonales(equipoDocenteJSON,personaed);
							}else{
								if(respuesta2.compareTo("Correo invalido")==0) {
									LOGGER.log(Level.SEVERE, "No se pudo registrar el docente: "+equipoDocenteJSON.getNumero_documento()+" su correo es inválido");
									InformeAula.setInfoerrores("No se pudo registrar el docente: "+equipoDocenteJSON.getNumero_documento()+" su correo es inválido <br>"+InformeAula.getInfoerrores());
								}else {
									LOGGER.log(Level.SEVERE, "No se pudo registrar el docente: "+equipoDocenteJSON.getNumero_documento());
									InformeAula.setInfoerrores("No se pudo registrar el docente: "+equipoDocenteJSON.getNumero_documento()+"<br>"+InformeAula.getInfoerrores());
								}
							}	
						}catch(Exception e){
							LOGGER.log(Level.SEVERE, "EXCEPTION CONTROL EQUIPO DOCENTE");
						}
					}
										
					//Controlo los estudiantes de cada comision, si no existen los registro en el evelia
					ComisionDTO[] comisiones = aulaDTO.getComisiones();
					if(comisiones!=null && comisiones.length > 0) {										
						for(int p = 0;p < comisiones.length;p++) {
							ComisionDTO comisionJSON =  comisiones[p];
							PersonaDTO[] itEstudiantes = comisionJSON.getEstudiantes();
							for(int n=0;n<itEstudiantes.length;n++){
								try {
									PersonaDTO estudianteJSON = itEstudiantes[n];
									String respuesta3 = "";
									try {
										respuesta3 = controlAulaCliente.agregarNuevaPersonayEnviaMail(estudianteJSON.getApellido(), estudianteJSON.getNombre(), estudianteJSON.getTipo_documento(), estudianteJSON.getNumero_documento() , estudianteJSON.getEmail(),estudianteJSON.getTelefono());
									}catch(Exception e) {}									
									if(respuesta3.compareTo("Ya existe")==0 || respuesta3.compareTo("Creado")==0) {
										String numeroDoc = ManejoString.eliminarPuntosDelDocumento(estudianteJSON.getNumero_documento());
										Persona personaed = (Persona)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc == \""+numeroDoc+"\"").elementAt(0);
										if(respuesta3.compareTo("Creado")==0) {
											personaed.setCreadaDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);	
											LOGGER.log(Level.INFO, "Se registró en evelia el estudiante "+ estudianteJSON.getNumero_documento() );
										}else
											controlAulaCliente.ModificarDatosPersonales(estudianteJSON,personaed);										
									}else{
										if(respuesta3.compareTo("Correo invalido")==0) {
											LOGGER.log(Level.SEVERE, "No se pudo registrar el estudiante "+estudianteJSON.getNumero_documento()+", su email es inválido.");
											InformeAula.setInfoerrores("No se pudo registrar el estudiante "+estudianteJSON.getNumero_documento()+", su email es inválido.<br>"+InformeAula.getInfoerrores());
										}else {
										    LOGGER.log(Level.SEVERE, "No se pudo registrar el estudiante "+estudianteJSON.getNumero_documento());
											InformeAula.setInfoerrores("No se pudo registrar el estudiante "+estudianteJSON.getNumero_documento()+"<br>"+InformeAula.getInfoerrores());										   									    
										}
									}		
								}catch(Exception e){
									LOGGER.log(Level.SEVERE, "EXCEPTION CONTROL ESTUDIANTE");
								}
							}//for estudiante
						}//for comisiones
					}else 
						LOGGER.log(Level.INFO, "No existen comisiones para crear.");											
					try {
						cargaOK = controlAulaCliente.completarAulaConComisioensyEquipoDocente(gson,aulaSisinfo,aulaDTO,InformeAula);	
					}catch(Exception e) {
						LOGGER.log(Level.SEVERE, "Exception CargarComisionesYEquipoDocente.");
					}
				}else
					LOGGER.log(Level.SEVERE, "El aula "+aulaDTO.getId_aula()+" no ha sido creada.");				
			}else 
				LOGGER.log(Level.WARNING, "No hay datos para cargar");
			
			if(cargaOK == 1) {
				String nombreAula = aulaSisinfo.getNombre();
				String codOrganizacion = aulaSisinfo.getAulaCompuesta().getCodigo();
				LOGGER.log(Level.INFO, "COMMIT: SE COMPLETO LA CARGA DEL AULA."+ nombreAula);	
				persistencia.commit();					
				MonitorearAulas(nombreAula, InformeAula);				
				RegistroAulasPendientes(InformeAula, aulaDTO, nombreAula, codOrganizacion);							
			}else {
				LOGGER.log(Level.INFO, "ROLLBACK: NO SE COMPLETO LA CARGA DEL AULA. "+ aulaSisinfo.getNombre());
				persistencia.rollback();				
				AulasPendientesEIncompletas.RegistrarAulaSisinfoPendiente(aulaDTO,null,InformeAula.getInfoerrores(),ESTADO_AULAS.INCOMPLETA);
			}	
			
		}catch(Exception e) {
			persistencia.rollback();
			LOGGER.log(Level.SEVERE, "Exception CargarComisionesYEquipoDocente.");
			throw new Exception("EXCEPTION CargarComisionesYEquipoDocente. "+e.getMessage());	
		}
	}
	
	/*
	 * Monitorea actualizacion de Aulas automaticas
	 * */
	private void MonitorearAulas(String nombreAula, AulaLog InformeAula) throws Exception {		
		String rutaArchivo = "";						
		String contenidoArchivo = "---------------------Actualizacion de Aula Automatica: "+nombreAula+"---------------------\n";
		contenidoArchivo += InformeAula.getInfoaula()+"\n";
		contenidoArchivo += "-----------------------------------------------------------------------------------------------------------\n\n";
		String hoy = +Utils.dia(Utils.hoySqlDate())+"-"+Utils.mesEnTexto(Utils.hoySqlDate())+"-"+Utils.anio(Utils.hoySqlDate());
		rutaArchivo = Singleton.HOME_DIR_LOGS+"//creacionAulas//"+"aulas_automaticas_creadas_"+hoy+".log";
		ManejoArchivoDeTexto.creacionYCargaArchivoTexto(rutaArchivo, contenidoArchivo,true);   
		
	}
	
	/*
	 * Registra o elimina las aulas pendientes en la base de datos
	 * */
	private void RegistroAulasPendientes(AulaLog InformeAula, AulaDTO aulaDTO, String nombreAula, String codOrganizacion) throws Exception {	
		try {
		   if(InformeAula.getInfoerrores().compareTo("")!=0) {
			try {
				aulaDTO.setNombre(nombreAula);
				aulaDTO.setCodigo_organizacion(codOrganizacion);
			}catch(Exception ex) {}
			AulasPendientesEIncompletas.RegistrarAulaSisinfoPendiente(aulaDTO,null,InformeAula.getInfoerrores(),ESTADO_AULAS.INCOMPLETA);
		}else 
			AulasPendientesEIncompletas.EliminarAulaSisinfoPendiente(aulaDTO);	
		} catch (Exception ee) {
			throw new Exception("EXCEPTION RegistroAulasPendientes. "+ee.getMessage());	
		}
	}
	
	
	

	
	
}

