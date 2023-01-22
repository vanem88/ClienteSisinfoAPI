package sisinfo;

import java.sql.Timestamp;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import comun.constantes.ESTADO_AULAS;
import comun.constantes.PARAMETROS_PAGINAS;
import comun.constantes.TIPO_AULAS;
import comun.constantes.TIPO_USUARIOS;
import persistencia.Persistencia;
import persistencia.dominio.AulaCompuesta;
import persistencia.dominio.AulaSisinfoParaActualizar;
import persistencia.dominio.Comision;
import persistencia.dominio.Participante;
import persistencia.dominio.ParticipanteComision;
import persistencia.dominio.Persona;
import persistencia.dominio.PlantillaPermiso;
import persistencia.dominio.TipoAula;
import persistencia.dominio.TipoRecurso;
import persistencia.dominio.TipoUsuario;
import server.aula.ControlAula;
import server.secretaria.aula.ControlConfigurarPermisosAula;
import server.secretaria.aula.ControlPlantillaPermiso;
import sisinfo.control.ControlNovedadesAula;
import sisinfo.dto.AulaDTO;
import sisinfo.dto.ComisionDTO;
import sisinfo.dto.PersonaDTO;
import sisinfo.util.AulaLog;
import sisinfo.util.AulaPendienteActualizar;
import sisinfo.util.Registros;
import sisinfo.util.TimestampAdapter;
import sisinfo.util.Util;

public class NovedadesAulas {
	
	private static final Logger LOGGERNovedades = Logger.getLogger( NovedadesAulas.class.getName() );
	private Persistencia persistencia = null;
	private ControlNovedadesAula controlNovedades = null;
	private Gson gson;
	
	public NovedadesAulas() {
		gson = new GsonBuilder()
		        .registerTypeAdapter(Timestamp.class, new TimestampAdapter())
		        .create();		
	}		
			
	//Consulta si existen novedades en los datos de las aulas y realiza las modificaciones.
	public void ConsultaNovedadesDatosBasicos(String urlnovedades_aulas,String usuario,String  contrasena, Timestamp fechaNovedadesDesde,Timestamp fechaNovedadesHasta, String idAulaSisinfoPendiente) throws Exception {
		try {	
			String novedadesAulas = "";
			if(idAulaSisinfoPendiente.compareTo("")==0) 
				novedadesAulas = ComunicacionHTTP.HTTPGet(urlnovedades_aulas,usuario, contrasena,fechaNovedadesDesde,fechaNovedadesHasta);
			else {
				urlnovedades_aulas = urlnovedades_aulas+"/"+idAulaSisinfoPendiente; 
				novedadesAulas = ComunicacionHTTP.HTTPGet(urlnovedades_aulas,usuario, contrasena,fechaNovedadesDesde,null);
			}			
			
			LOGGERNovedades.log(Level.INFO, "Json de aulas a modificar: "+novedadesAulas);
			
			AulaDTO[] aulasModificadas = gson.fromJson(novedadesAulas, AulaDTO[].class);
			boolean encontroError = false;
			if(aulasModificadas != null && aulasModificadas.length > 0) {
				for(int i = 0;i < aulasModificadas.length;i++) {							
					AulaDTO aula = aulasModificadas[i];
										
					if(aula!=null) {
						AulaLog InformeAula = new AulaLog();
						try {
							persistencia  = new Persistencia();
							controlNovedades = new ControlNovedadesAula(persistencia);	
							persistencia.crearTransaccion();
							String idAulaAModificar = aula.getId_aula();
							LOGGERNovedades.log(Level.INFO, "//////////////////////////////////////////////////////// AULA A ACTUALIZAR: "+idAulaAModificar);
																	
							AulaCompuesta aulaSisinfo = null;
							try {
								aulaSisinfo = (AulaCompuesta)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.AulaCompuesta", "nombre", "idAulaSisinfo == \""+idAulaAModificar+"\"",true).elementAt(0);							
							}catch(Exception ex) {}
															
							//Long idRespPartEliminar = null;
							PersonaDTO[] equipoDocente = null;
							ComisionDTO[] comisionesdto = null;
						    Participante responsableAEliminar = null;
							
							if(aulaSisinfo!=null) {//Si el aula existe
								TipoUsuario tipoUsuarioAdmin = (TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.ADMINISTRADOR+"\" ").elementAt(0);
								TipoUsuario tipoUsuarioAlumno =(TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.ALUMNO+"\" ").elementAt(0);
								TipoUsuario tipoUsuarioResponsable =(TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.RESPONSABLE+"\" ").elementAt(0);
								TipoUsuario tipoUsuarioDocente =(TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.DOCENTE+"\" ").elementAt(0);
								TipoUsuario tipoUsuarioColaborador = (TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.COLABORADOR+"\" ").elementAt(0);
								
								
								//Modifica Equipo Docente
								equipoDocente = aula.getEquipo_docente();
								if(equipoDocente!=null) {
									LOGGERNovedades.log(Level.INFO,"..........Actualizando Equipo Docente.");
									if(!ModificacionesEquipoDocente(aulaSisinfo,equipoDocente,InformeAula,tipoUsuarioAdmin, tipoUsuarioResponsable, tipoUsuarioAlumno, tipoUsuarioDocente,tipoUsuarioColaborador ))
										encontroError = true;									
								    LOGGERNovedades.log(Level.INFO,"encontro error? "+encontroError);
								}
								
								if(!encontroError) {
									//Modifica Docente responsable
									PersonaDTO docenteResponsable = aula.getDocente_responsable();
									if(docenteResponsable!=null) {		
										LOGGERNovedades.log(Level.INFO,".........Actualizando Docente Responsable.");		
										//Si existe otro responsable lo elimino
										Vector responsableActual = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && tipoUsuario.id=="+tipoUsuarioResponsable.getId());
										if(responsableActual!=null && responsableActual.size()>0) {
											Participante responsableAEliminarAux = (Participante)responsableActual.elementAt(0);
											if(docenteResponsable.getNumero_documento().compareTo(responsableAEliminarAux.getPersona().getNumeroDoc())!=0)
											      responsableAEliminar = (Participante)responsableActual.elementAt(0);	
										} 
																				
										if(!ModificarUsuario(aulaSisinfo,null,docenteResponsable,TIPO_USUARIOS.RESPONSABLE,InformeAula,tipoUsuarioAdmin,tipoUsuarioResponsable,tipoUsuarioAlumno,tipoUsuarioDocente,tipoUsuarioColaborador))
											encontroError = true;
										LOGGERNovedades.log(Level.INFO,"encontro error? "+encontroError);
									}
									
									if(!encontroError) {
										//Modifica Comisiones
										comisionesdto = aula.getComisiones();									
										if(comisionesdto!= null && comisionesdto.length>0) {
											LOGGERNovedades.log(Level.INFO,"..........Actualizando comisiones.");
											if(!ModificacionesComisiones(aulaSisinfo,comisionesdto,InformeAula, tipoUsuarioAdmin, tipoUsuarioResponsable, tipoUsuarioAlumno, tipoUsuarioDocente,tipoUsuarioColaborador))
												encontroError = true;																					
											LOGGERNovedades.log(Level.INFO,"encontro error? "+encontroError);
										}
										
										if(!encontroError) {
											//Elimina comisiones
											if(comisionesdto!=null && comisionesdto.length>0) {
												LOGGERNovedades.log(Level.INFO,"..........Controlando si existen comisiones para eliminar.");
												Vector comisionAEliminar = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Aula","nombre","aulaCompuesta.id=="+aulaSisinfo.getId(), true);
												if(comisionAEliminar!=null && comisionAEliminar.size()>0)
													for(int c = 0;c < comisionAEliminar.size();c++) {
														Comision comision = (Comision)comisionAEliminar.elementAt(c);	
														if(eliminarComision(comisionesdto,comision.getNombre())){
															if(!controlNovedades.EliminarComision(comision.getId())) {
																encontroError = true;
																Registros.RegistrarInfo(Level.SEVERE,"La comisión "+comision.getNombre()+", no se puedo eliminar del aula "+aulaSisinfo.getIdAulaSisinfo(), InformeAula);
															}
														}				
													}
												LOGGERNovedades.log(Level.INFO,"encontro error? "+encontroError);
											}
											
												
											if(!encontroError) {
												//Eliminar docentes y estudiantes de las comisiones								
												if(comisionesdto!= null && comisionesdto.length>0) {
													LOGGERNovedades.log(Level.INFO,".............Controlando si existen docentes y estudiantes en las comisiones para eliminar.");
													for(int p = 0;p < comisionesdto.length;p++) {	
														ComisionDTO comisiondto =  comisionesdto[p];											
														try {												
															PersonaDTO[] docentesComisiondto = comisiondto.getDocentes();
															
															Vector comisionesAula = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Aula","nombre","aulaCompuesta.id=="+aulaSisinfo.getId()+" && nombre==\""+comisiondto.getNombre()+"\"", true);
															if(comisionesAula!=null && comisionesAula.size()>0) {
																Comision comision = (Comision)comisionesAula.elementAt(0);																	
																
																try {
																	Vector docentesComision = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.ParticipanteComision","comision.id=="+comision.getId()+" && (participante.tipoUsuario.id=="+tipoUsuarioDocente.getId() +" || participante.tipoUsuario.id=="+tipoUsuarioColaborador.getId()+")");
																	if(docentesComision != null && docentesComision.size()>0) 
																		for(int s=0;s<docentesComision.size();s++) {
																			ParticipanteComision partDocCom = (ParticipanteComision)docentesComision.elementAt(s);
																			if(eliminarParticipanteDocenteComision(docentesComisiondto,partDocCom.getParticipante())) { 
																				LOGGERNovedades.log(Level.INFO,"Docente a eliminar: "+partDocCom.getParticipante().getPersona().getNombreYApellido()+" de la comision: "+partDocCom.getComision().getNombre());
																				persistencia.deletePersistente(partDocCom);																
																			}
																		}							
																}catch(Exception e){
																	encontroError = true;
																	Registros.RegistrarInfo(Level.SEVERE,"Error al intentar eliminar docentes en las comisiones del aula "+idAulaAModificar, InformeAula);
																}
																																
																try {
																	PersonaDTO[] participantes = comisiondto.getEstudiantes();
																	Vector estudiantesComision = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.ParticipanteComision","comision.id=="+comision.getId()+" && participante.tipoUsuario.id=="+tipoUsuarioAlumno.getId());
																	if(estudiantesComision != null && estudiantesComision.size()>0) 
																		for(int s=0;s<estudiantesComision.size();s++) {
																			ParticipanteComision partEstCom = (ParticipanteComision)estudiantesComision.elementAt(s);
																			String dniAEliminar = partEstCom.getParticipante().getPersona().getNumeroDoc();	
																			int eliminar = eliminarParticipanteAlumno(participantes,comisionesdto,dniAEliminar);
																			if(eliminar==1) {
																				if(!this.controlNovedades.EliminarAlumnoDeUnaComision(partEstCom,tipoUsuarioAlumno))
																					encontroError = true;
																			}else if(eliminar==2){
																				Registros.RegistrarInfo(Level.INFO,"El estudiante "+dniAEliminar+ " se ha cambiado de comisión, quedando por el momento inscripcto en ambas comisiones.", InformeAula);
																			}
																		}													
																}catch(Exception e){
																	encontroError = true;
																	Registros.RegistrarInfo(Level.SEVERE,"Error al intentar eliminar estudiantes en las comisiones del aula "+idAulaAModificar, InformeAula);
																}
																
															}
														}catch(Exception e){
															encontroError = true;
															Registros.RegistrarInfo(Level.SEVERE,"Error al intentar eliminar docentes y estudiantes en las comisiones del aula "+idAulaAModificar, InformeAula);
														}
												    }//for
													LOGGERNovedades.log(Level.INFO,"encontro error? "+encontroError);
												}	
													
												if(!encontroError) {
													if(equipoDocente!=null) {
														LOGGERNovedades.log(Level.INFO,"................Controlando si existe docentes del equipo docente para eliminar.");
														try{
															Vector participantesDocentesAula = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && (tipoUsuario.id=="+tipoUsuarioDocente.getId() +" || tipoUsuario.id=="+tipoUsuarioColaborador.getId() +")");
															for(int d=0;d <participantesDocentesAula.size();d++) {
																Participante docenteAEliminar = (Participante) participantesDocentesAula.elementAt(d);										
																if(eliminarParticipanteEquipoDocente(equipoDocente,docenteAEliminar,tipoUsuarioDocente,tipoUsuarioColaborador)) 
																	if(!controlNovedades.EliminarDocenteOResponsableDelAula(docenteAEliminar)) {
																		encontroError = true;
																		Registros.RegistrarInfo(Level.SEVERE,"El docente con DNI "+docenteAEliminar.getPersona().getNumeroDoc()+", no se puedo eliminar del aula "+idAulaAModificar, InformeAula);
																	}
															}
														}catch(Exception ex) {
															encontroError = true;
															Registros.RegistrarInfo(Level.SEVERE,"Problema al eliminar un docente del equipo docente.", InformeAula);
														}
														LOGGERNovedades.log(Level.INFO,"encontro error? "+encontroError);
													}
													
													if(!encontroError) {
														//Eliminar responsable anterior	
														if(responsableAEliminar!=null) {
															LOGGERNovedades.log(Level.INFO,".........Eliminando responsable anterior del aula.");
															if(!controlNovedades.EliminarDocenteOResponsableDelAula(responsableAEliminar)) { 
																encontroError = true;
																Registros.RegistrarInfo(Level.SEVERE,"El responsable con DNI "+responsableAEliminar.getPersona().getNumeroDoc()+", no se puedo eliminar del aula "+idAulaAModificar, InformeAula);
															}
															LOGGERNovedades.log(Level.INFO,"encontro error? "+encontroError);
														}	
													}//!encontroError
												}//!encontroError
											}//!encontroError
										}//!encontroError
									}//!encontroError
								}//!encontroError
									
								//Registro en la Tabla Aula_Sisinfo_pendiente los errores encontrados, en Evelia se puede visualizar el informe en la secretaria del campus.	
								if(InformeAula.getInfoerrores().compareTo("")!=0) {
									if(aulaSisinfo!=null) {
										aula.setNombre(aulaSisinfo.getNombre());
										aula.setCodigo_organizacion(aulaSisinfo.getAulaCompuesta().getCodigo());
									}else {
										if((aula.getNombre()==null || aula.getNombre().compareTo("")==0)) aula.setNombre("-");
										if((aula.getCodigo_organizacion()==null || aula.getCodigo_organizacion().compareTo("")==0)) aula.setCodigo_organizacion("-");											
									}
									String estado = ESTADO_AULAS.ACTUALIZACION;
									if(!encontroError) {
										InformeAula.setInfoerrores("El aula fue actualizada con exito, a continuacion se detalla información a considerar.<br>"+InformeAula.getInfoerrores());
										estado = "Información a considerar";
									}
									AulasPendientesEIncompletas.RegistrarAulaSisinfoPendiente(aula,null,InformeAula.getInfoerrores(),estado);
								}	
								
							}else { 
								Registros.RegistrarInfo(Level.INFO,"El aula "+aula.getId_aula()+", que intenta modificar, no ha sido creada", InformeAula);
								encontroError = true;
								AulasPendientesEIncompletas.RegistrarAulaSisinfoPendiente(aula,null,"El aula "+aula.getId_aula()+", que intenta modificar, no ha sido creada","Información a considerar");
								
							}
							
							if(!encontroError) {
								persistencia.commit();
								//Si era una actualizacion pendiente, elimino la actualizacion de la tabla AULA_SISINFO_PARA_ACTUALIZAR
								if(idAulaSisinfoPendiente.compareTo("")!=0) 	
									this.controlNovedades.EliminoAulaPendienteDeActualizar(idAulaSisinfoPendiente,fechaNovedadesDesde);
								LOGGERNovedades.log(Level.INFO, "/////////////////////////////////////////// (commit) SE REALIZARION TODAS LAS ACTUALIZACIONES DEL AULA: "+idAulaAModificar);
													
							}else {									
								persistencia.rollback();
								if((idAulaSisinfoPendiente==null || (idAulaSisinfoPendiente!=null && idAulaSisinfoPendiente.compareTo("")==0)) && aulaSisinfo!=null)	
									this.controlNovedades.InsertarAulaPendienteDeActualizar(aula.getId_aula(),InformeAula.getInfoerrores(),fechaNovedadesDesde/*,fechaNovedadesHasta*/);
						   		LOGGERNovedades.log(Level.INFO, "/////////////////////////////////////////// (rollback) NO SE REALIZARON ACTUALIZACIONES EN EL AULA: "+idAulaAModificar);
						   	}			
							
						}catch(Exception ex) {
							ex.printStackTrace();
							LOGGERNovedades.log(Level.SEVERE, "Exception ConsultaNovedadesDatosBasicos."+ex.getMessage());
							persistencia.rollback();
						}							
					}//aula!=null	
					encontroError = false;
				}//for aulas
			}else{			
				LOGGERNovedades.log(Level.INFO, "No existen actualizaciones desde la fecha: "+fechaNovedadesDesde);
				if(idAulaSisinfoPendiente.compareTo("")!=0) {
					ControlNovedadesAula controlNovedadesAux = new ControlNovedadesAula();	
					controlNovedadesAux.EliminoAulaPendienteDeActualizar(idAulaSisinfoPendiente,fechaNovedadesDesde);		
				}
			}
		}catch(Exception ex) {
			throw new Exception("Exception Json de consulta Novedades. "+ex.getMessage());
		}
	}
		
	//Controla si hay que eliminar el docente
	/*private boolean eliminarParticipante(PersonaDTO[] participantes,Participante participanteDelAula,TipoUsuario tipoUsuarioDocente,TipoUsuario tipoUsuarioColaborador) {
		String dniAEliminar = participanteDelAula.getPersona().getNumeroDoc();		
		if(tipoUsuarioColaborador!=null) {
			String rolAEliminar = participanteDelAula.getTipoUsuario().getNombre();
			for(int i=0; i<participantes.length; i++) {
				String dni = participantes[i].getNumero_documento();
				String rol = participantes[i].getRol();
				if(rol.compareTo(Util.AyudanteAlumno)==0)rol = tipoUsuarioColaborador.getNombre();
				else rol = tipoUsuarioDocente.getNombre();
				if(dniAEliminar.compareTo(dni)==0 && rolAEliminar.compareTo(rol)==0)
					return false;
			}			
		}else {
			for(int i=0; i<participantes.length; i++) {
				String dni = participantes[i].getNumero_documento();				
				if(dniAEliminar.compareTo(dni)==0)
					return false;
			}			
		}
			
		return true;
	}*/
	
	//Controla si hay que eliminar el docente	
	private boolean eliminarParticipanteEquipoDocente(PersonaDTO[] participantes,Participante participanteDelAula,TipoUsuario tipoUsuarioDocente,TipoUsuario tipoUsuarioColaborador) {
		String dniAEliminar = participanteDelAula.getPersona().getNumeroDoc();	
		String rolAEliminar = participanteDelAula.getTipoUsuario().getNombre();
		for(int i=0; i<participantes.length; i++) {
			String dni = participantes[i].getNumero_documento();
			String rol = participantes[i].getRol();
			if(rol.compareTo(Util.AyudanteAlumno)==0) rol = tipoUsuarioColaborador.getNombre();
			else rol = tipoUsuarioDocente.getNombre();
			if(dniAEliminar.compareTo(dni)==0 && rolAEliminar.compareTo(rol)==0)
				return false;
		}					
		return true;
	}
	
	//Controla si hay que eliminar el docente de una comision	
	private boolean eliminarParticipanteDocenteComision(PersonaDTO[] participantes,Participante participanteDelAula) {
		String dniAEliminar = participanteDelAula.getPersona().getNumeroDoc();		
		for(int i=0; i<participantes.length; i++) {
			String dni = participantes[i].getNumero_documento();				
			if(dniAEliminar.compareTo(dni)==0)
				return false;
		}						
		return true;
	}
	
	//Controla si hay que eliminar el alumno	0: no elimino   1: elimino     2: cambio de comision
	private int eliminarParticipanteAlumno(PersonaDTO[] participantes,ComisionDTO[] comisionesdto,String dniAEliminar) {
		//verifico si el estudiante que estoy controlando esta en la comision en el json de sisinfo
		for(int i=0; i<participantes.length; i++) {
			String dni = participantes[i].getNumero_documento();
			if(dni!= null && dniAEliminar.compareTo(dni)==0)
				return 0;//No lo elimino
		}		
		
		//Si no esta en la comision, verifico si esta en alguna otra, porque puede querer cambiarse de comision
		for(int j=0;j<comisionesdto.length;j++) {
			ComisionDTO comisiondto = comisionesdto[j];
			PersonaDTO[] participantesOtraCom = comisiondto.getEstudiantes();
			for(int i=0; i<participantesOtraCom.length; i++) {
				String dni = participantesOtraCom[i].getNumero_documento();
				if(dni!= null && dniAEliminar.compareTo(dni)==0)
					return 2;
			}		
		}
		//No esta en ninguna comision, por lo tanto lo elimino
		return 1;
	}
	
	//Controlo si hay que eliminar la comision
	private boolean eliminarComision(ComisionDTO[] comisiones,String nombreComisionAEliminar) {
		for(int i=0; i<comisiones.length; i++) {
			String nombre = comisiones[i].getNombre();
			if(nombreComisionAEliminar.compareTo(nombre)==0)
				return false;
		}
		return true;
	}
		
	//Realiza las modificaciones neceserias en las comisiones
	private boolean ModificacionesComisiones(AulaCompuesta aulaSisinfo,ComisionDTO[] comisionesdto, AulaLog InformeAula,TipoUsuario tipoUsuarioAdmin,TipoUsuario tipoUsuarioResponsable,TipoUsuario tipoUsuarioAlumno,TipoUsuario tipoUsuarioDocente,TipoUsuario tipoUsuarioColaborador) {
		boolean modifiqueTodas = true;			
		try {
			for(int p = 0;p < comisionesdto.length;p++) {					
				ComisionDTO comisiondto =  comisionesdto[p];	
				Vector comisionesAula = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Aula","nombre","aulaCompuesta.id=="+aulaSisinfo.getId()+" && nombre==\""+comisiondto.getNombre()+"\"", true);
				
				if(comisionesAula!=null && comisionesAula.size()>0) {
					Comision comision = (Comision)comisionesAula.elementAt(0);
					LOGGERNovedades.log(Level.INFO, "Comision a actualizar: "+comision.getNombre());
					if(!ActualizarEstudianetsyDocentesDeComision(aulaSisinfo,comision, comisiondto,InformeAula,tipoUsuarioAdmin, tipoUsuarioResponsable, tipoUsuarioAlumno, tipoUsuarioDocente,tipoUsuarioColaborador)) {
						modifiqueTodas = false;
						Registros.RegistrarInfo(Level.SEVERE,"No se pudo actualizar la comisión "+comision.getNombre(), InformeAula);						
					}	
				}else { 
					if(!AgregaComisionAlAula(aulaSisinfo,comisiondto,InformeAula, tipoUsuarioAdmin, tipoUsuarioResponsable, tipoUsuarioAlumno, tipoUsuarioDocente,tipoUsuarioColaborador)) {
						modifiqueTodas = false;							
					}
				}			
			}
		}catch(Exception e){
			LOGGERNovedades.log(Level.SEVERE, "EXCEPTION ModificacionesComisiones");
			modifiqueTodas = false;
		}
		return modifiqueTodas;		
	}
		
	//Agrega una comision al aula Sisiinfo	
	private boolean AgregaComisionAlAula(AulaCompuesta aulaSisinfo,ComisionDTO comisiondto,AulaLog InformeAula,TipoUsuario tUAdmin,TipoUsuario tUResponsable,TipoUsuario tUAlumno,TipoUsuario tUDocente,TipoUsuario tUColaborador) {
		try {
			String nombreComision = comisiondto.getNombre();
			LOGGERNovedades.log(Level.INFO, "Agregando comision: "+nombreComision);
			if(nombreComision!=null && nombreComision.compareTo("")!=0 ) {
				
				TipoUsuario tUWM = (TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.WEBMASTER+"\" ").elementAt(0);
				//Vector participantesAdminAula = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && tipoUsuario.id=="+tUAdmin.getId()); 
				Vector participantesRespAula = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && tipoUsuario.id=="+tUResponsable.getId()); 
				
				//Participante participanteAdministrador = null;
				Participante participanteResponsable = null;
				if(participantesRespAula!=null && participantesRespAula.size()>0) 
					participanteResponsable = (Participante)participantesRespAula.elementAt(0);	
												
				TipoAula tipoAulaCurso = (TipoAula)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoAula","nombre==\""+TIPO_AULAS.CURSO+"\"").elementAt(0);
				String pathraizArchivos = server.Singleton.HOME_DIR_ARCHIVOS+PARAMETROS_PAGINAS.ID_AULA+aulaSisinfo.getId();
				Vector participantesWMDeAula = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && tipoUsuario.id=="+tUWM.getId());
				Vector partReplicar = this.controlNovedades.obtenerParticipantesAReplicar(aulaSisinfo.getAulaCompuesta().getId());
				for(int j=0;j<partReplicar.size();j++){
					Participante participRep = (Participante)partReplicar.elementAt(j);
					participantesWMDeAula.add(participRep);
				}	
				Comision comision = this.controlNovedades.crearDatosBasicosComision(InformeAula,aulaSisinfo, 2, nombreComision, tipoAulaCurso, pathraizArchivos, participantesWMDeAula, /*participanteAdministrador,*/participanteResponsable,null,null,null);
				if(comision != null) {
					LOGGERNovedades.log(Level.INFO, "Se crearon los datos basicos de la comision"); 
					ControlAula controlAula = new ControlAula(persistencia);						
					//Cargar estudiantes a cada comision
					PersonaDTO[] estudientes = comisiondto.getEstudiantes();	
					if(controlNovedades.cargarEstudiantesAUnaComision(aulaSisinfo,estudientes,InformeAula, comision,controlAula,/*tUAdmin,*/tUResponsable,tUAlumno,tUDocente,tUColaborador)) {
					
						//Carga los docentes a cada comision
						PersonaDTO[] docentes = comisiondto.getDocentes();	
						if(this.controlNovedades.modificarDocentesAUnaComsiion(aulaSisinfo, docentes, InformeAula,tUAdmin, tUResponsable, tUAlumno, tUDocente,tUColaborador, comision, controlAula)) {		
							//Asigno permisos a nivel comision
							ControlPlantillaPermiso ctrlPlantillaPermiso = new ControlPlantillaPermiso(this.persistencia);
					    	PlantillaPermiso  plantillaPermisos = (PlantillaPermiso) aulaSisinfo.getPlantillaPermiso();
							this.controlNovedades.crearPermisoAcceso(ctrlPlantillaPermiso,plantillaPermisos.getId(),comision,"comision");
							
							//Agregar herramientas a las comsiones
							ControlConfigurarPermisosAula ctrlConfigurarPermisosAula = new ControlConfigurarPermisosAula(this.persistencia);
							
							Object[] arrayComs = new Object[1];
							arrayComs[0] = comision;
							Vector tiposDeRecursos =  persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoRecurso","orden,nombre", "creacionAulaSisinfo=="+true);
							
							TipoRecurso tipoRecurso = null;
							for(int s=0;s<tiposDeRecursos.size();s++) {
								tipoRecurso = (TipoRecurso) tiposDeRecursos.elementAt(s);
								ctrlConfigurarPermisosAula.agregarHerramientaAulas(arrayComs, tipoRecurso,TIPO_AULAS.COMISION);	
							}							
							return true;
						}else {
							Registros.RegistrarInfo(Level.SEVERE,"No se pudo crear la nueva comision "+nombreComision+". Existio un problema en la actualización de los docentes.", InformeAula);
							return false;
						}
					}else {
						Registros.RegistrarInfo(Level.SEVERE,"No se pudo crear la nueva comision "+nombreComision+". Existio un problema en la actualización de los estudiantes.", InformeAula);
						return false;
					}
				}else { 
					Registros.RegistrarInfo(Level.SEVERE,"No se pudo crear la nueva comision "+nombreComision, InformeAula);
					return false;
				}
			}else {
				Registros.RegistrarInfo(Level.SEVERE,"No se pudo crear la nueva comision porque no se envio su nombre.", InformeAula);
				return false;
			}			
		}catch(Exception e){
			LOGGERNovedades.log(Level.SEVERE, "EXCEPTION AgregaComisionAlAula");
			return false;
		}
	}
		
	//Consulta si existen novedades en el equipo docente del aula y realiza las modificaciones.
	private boolean ModificacionesEquipoDocente(AulaCompuesta aulaSisinfo,PersonaDTO[] equipoDocente, AulaLog InformeAula,TipoUsuario tipoUsuarioAdmin,TipoUsuario tipoUsuarioResponsable,TipoUsuario tipoUsuarioAlumno,TipoUsuario tipoUsuarioDocente,TipoUsuario tipoUsuarioColaborador ) {
		boolean modifico = true;	
		String rol;
		try {
			for(int p = 0;p < equipoDocente.length;p++) {					
				PersonaDTO docente =  equipoDocente[p];	
				if(docente.getRol().compareTo(Util.AyudanteAlumno)==0) rol = TIPO_USUARIOS.COLABORADOR;
				else rol = TIPO_USUARIOS.DOCENTE;
				if(!ModificarUsuario(aulaSisinfo,null,docente,rol,InformeAula, tipoUsuarioAdmin, tipoUsuarioResponsable, tipoUsuarioAlumno, tipoUsuarioDocente,tipoUsuarioColaborador))
					modifico = false;						
			}
			return modifico;	
		}catch(Exception e){
			LOGGERNovedades.log(Level.SEVERE, "EXCEPTION ModificacionesEquipoDocente");
			return false;
		}
	}
		
	//Modifica el usuario o lo registra si no existe
	private boolean ModificarUsuario(AulaCompuesta aulaSisinfo,Comision comision,PersonaDTO usuario,String tipoUsuario,AulaLog InformeAula,TipoUsuario tipoUsuarioAdmin,TipoUsuario tipoUsuarioResponsable,TipoUsuario tipoUsuarioAlumno,TipoUsuario tipoUsuarioDocente,TipoUsuario tipoUsuarioColaborador) {
		try {
			String dni = usuario.getNumero_documento();
			LOGGERNovedades.log(Level.INFO,"ModificarUsuario "+dni+" .....");
			if(dni!=null && dni.compareTo("")!=0) {
				//Controlo si la persona esta registrada en evelia
				Persona personal = null;
				try {
					personal = (Persona)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc == \""+dni+"\"").elementAt(0);
				}catch (Exception ex) {}				
				
				if (personal != null){//Ya esta registrado en Evelia
					//Verifico que no este registrado con el mismo rol
					Vector<Participante> participantes = null;
					if(tipoUsuario.compareTo(TIPO_USUARIOS.RESPONSABLE)==0)
						participantes = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && persona.id=="+personal.getId()+" && tipoUsuario.id=="+tipoUsuarioResponsable.getId());
					if(tipoUsuario.compareTo(TIPO_USUARIOS.DOCENTE)==0 || tipoUsuario.compareTo(TIPO_USUARIOS.COLABORADOR)==0) {
						if(tipoUsuario.compareTo(TIPO_USUARIOS.DOCENTE)==0) {
							participantes = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && persona.id=="+personal.getId()+" && tipoUsuario.id=="+tipoUsuarioDocente.getId());
							if(participantes.size()==0)
								participantes = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && persona.id=="+personal.getId()+" && tipoUsuario.id=="+tipoUsuarioColaborador.getId());
						}else
							participantes = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && persona.id=="+personal.getId()+" && tipoUsuario.id=="+tipoUsuarioColaborador.getId());
					}
					if(tipoUsuario.compareTo(TIPO_USUARIOS.ALUMNO)==0)
						participantes = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && persona.id=="+personal.getId()+" && tipoUsuario.id=="+tipoUsuarioAlumno.getId());
					
					if(participantes.size()==0){
					    LOGGERNovedades.log(Level.INFO,"Agrega un "+tipoUsuario+" al aula: "+aulaSisinfo.getNombre());
						boolean res = this.controlNovedades.agregaUsuarioAlAula(tipoUsuario,aulaSisinfo,comision,personal,/*tipoUsuarioAdmin,*/tipoUsuarioResponsable,tipoUsuarioAlumno,tipoUsuarioDocente,tipoUsuarioColaborador,InformeAula); 
						if(!res) Registros.RegistrarInfo(Level.SEVERE,"No se pudo agregar el "+tipoUsuario+" con DNI "+personal.getNumeroDoc()+" al aula "+aulaSisinfo.getIdAulaSisinfo(), InformeAula); 
						return res; 						
					}else {
						LOGGERNovedades.log(Level.INFO,"El usuario "+personal.getNumeroDoc()+" ya esta registrado como "+tipoUsuario+" en el aula "+aulaSisinfo.getNombre());
						Participante partAula = (Participante)participantes.elementAt(0);						
						if((tipoUsuario.compareTo(TIPO_USUARIOS.DOCENTE)==0 || tipoUsuario.compareTo(TIPO_USUARIOS.COLABORADOR)==0) & comision!=null) {
							Vector<Participante> participantesComision = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.ParticipanteComision", "comision.id=="+comision.getId()+" && participante.id=="+partAula.getId());
							if(participantesComision.size()==0) {
								LOGGERNovedades.log(Level.INFO,"Agrega el "+tipoUsuario+" "+ personal.getNumeroDoc()+" a la comision "+comision.getNombre());
								boolean res;
								if(tipoUsuario.compareTo(TIPO_USUARIOS.DOCENTE)==0)
									res = controlNovedades.agregaDocenteALaComision(aulaSisinfo,comision,personal,tipoUsuarioDocente,partAula);
								else
									res = controlNovedades.agregaDocenteALaComision(aulaSisinfo,comision,personal,tipoUsuarioColaborador,partAula);
								if(!res) Registros.RegistrarInfo(Level.SEVERE,"No se pudo agregar el "+tipoUsuario+" con DNI "+personal.getNumeroDoc()+" al aula "+aulaSisinfo.getIdAulaSisinfo(), InformeAula); 
								return res;
							} else
								LOGGERNovedades.log(Level.INFO,"El docente ya esta registrado en el aula y en la comisión, no se realizó ninguna acción....");
							
							return true;
						}else {
							if(tipoUsuario.compareTo(TIPO_USUARIOS.ALUMNO)==0) {
								//Puede querer cambiarse de comision
								Vector<Participante> participantesComision = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.ParticipanteComision", "comision.id=="+comision.getId()+" && participante.id=="+partAula.getId());
								if(participantesComision.size()==0) {
									//No participa de la comision, es decir que quiere inscribirse en otra comision
									//LOGGERNovedades.log(Level.INFO,"El estudiante esta registrado como alumno del aula pero no en la comisión que se necesita agregar");
									boolean res = this.controlNovedades.agregaUsuarioAlAula(tipoUsuario,aulaSisinfo,comision,personal,/*tipoUsuarioAdmin,*/tipoUsuarioResponsable,tipoUsuarioAlumno,tipoUsuarioDocente,tipoUsuarioColaborador,InformeAula); 
									if(!res) Registros.RegistrarInfo(Level.SEVERE,"No se pudo agregar el "+tipoUsuario+" con DNI "+personal.getNumeroDoc()+" al aula "+aulaSisinfo.getIdAulaSisinfo(), InformeAula); 
									return res;
								}else 
									return ModificarDatosPersonales(usuario,personal,InformeAula,"El usuario con dni "+dni +" ya es alumno de la comisión "+comision.getNombre()+" del aula "+aulaSisinfo.getNombre());
							}else 
								return ModificarDatosPersonales(usuario,personal,InformeAula,"El usuario con dni "+dni +" ya es "+tipoUsuario+" del aula.");
						}
					}					
				}else		{
					LOGGERNovedades.log(Level.INFO,"registarr en evelia");
					return this.controlNovedades.registrarEnEvelia(usuario,tipoUsuario,aulaSisinfo, comision, /*tipoUsuarioAdmin,*/ tipoUsuarioResponsable, tipoUsuarioAlumno, tipoUsuarioDocente,tipoUsuarioColaborador, InformeAula);
				}			
					
			}else {
				Registros.RegistrarInfo(Level.SEVERE,tipoUsuario+" sin número de documento.", InformeAula);
				//return false;
				return true;
			}
		}catch (Exception ex) {
			ex.printStackTrace();
			LOGGERNovedades.log(Level.SEVERE,"Exception ModificarUsuario");
			return false;
		}
	}
	
	//Modifica los datos personales de un usuario y registra informacion en los log.
	private boolean ModificarDatosPersonales(PersonaDTO usuario,Persona personal, AulaLog InformeAula,String info) {
		try {
			personal.setNombre(usuario.getNombre());
			personal.setApellido(usuario.getApellido());
			personal.setTipoDoc(usuario.getTipo_documento());
			personal.setNumeroDoc(usuario.getNumero_documento());
			personal.setEmail(usuario.getEmail());
			personal.setTelefono(usuario.getTelefono());
			return true;
		}catch(Exception e) {
			e.printStackTrace();
			Registros.RegistrarInfo(Level.SEVERE,info+" Error al intentar modificar datos personales del usuario.", InformeAula); 
			return false;
		}
	}
		
	//Actualiza estudiantes y docentes de una comision	
	private boolean ActualizarEstudianetsyDocentesDeComision(AulaCompuesta aulaSisinfo,Comision comision,ComisionDTO comisiondto, AulaLog InformeAula,TipoUsuario tipoUsuarioAdmin,TipoUsuario tipoUsuarioResponsable,TipoUsuario tipoUsuarioAlumno,TipoUsuario tipoUsuarioDocente,TipoUsuario tipoUsuarioColaborador){
		boolean actualizoTodo = true;		
		try {
			PersonaDTO[] estudiantes = comisiondto.getEstudiantes();
			if(estudiantes!=null && estudiantes.length>0) 
				for(int i =0;i<estudiantes.length;i++ ) {					
						PersonaDTO estudiante = estudiantes[i];
						if(!ModificarUsuario(aulaSisinfo,comision,estudiante,TIPO_USUARIOS.ALUMNO,InformeAula,tipoUsuarioAdmin, tipoUsuarioResponsable, tipoUsuarioAlumno, tipoUsuarioDocente,tipoUsuarioColaborador))
							actualizoTodo = false;						
				}					
			PersonaDTO[] docentes = comisiondto.getDocentes();
			if(docentes!=null && docentes.length>0) 
				for(int i =0;i<docentes.length;i++ ) {					
						PersonaDTO docente = docentes[i];
						if(!ModificarUsuario(aulaSisinfo,comision,docente,TIPO_USUARIOS.DOCENTE,InformeAula,tipoUsuarioAdmin, tipoUsuarioResponsable, tipoUsuarioAlumno, tipoUsuarioDocente,tipoUsuarioColaborador))
							actualizoTodo = false;						
				}			
			return actualizoTodo;		
		}catch(Exception e){
			LOGGERNovedades.log(Level.SEVERE, "EXCEPTION ActualizarEstudianetsyDocentesDeComision");
			return false;
		}
	}
	
	// Verifica si hay aulas sin actualizar en la tabla "AULA_SISINFO_PARA_ACTUALIZAR", si hay primero consulta por esas actualizaciones y
	// luego continua consultando por nuevas novedades.
	public void VerificaAulaSinActualizar(String urlnovedades_aulas,String usuario,String  contrasena) throws Exception {
		Persistencia persistencia1  = new Persistencia();
		try {			
			persistencia1.crearTransaccion();
			Vector<AulaSisinfoParaActualizar> aulasSisinfoParaActualizar = persistencia1.getObjectosPorClase("persistencia.dominio.AulaSisinfoParaActualizar");					
			
			Vector<AulaPendienteActualizar> aulasPendientes = new Vector<AulaPendienteActualizar>();
			for(AulaSisinfoParaActualizar aulaPendiente : aulasSisinfoParaActualizar) { 
				AulaPendienteActualizar aulaPendienteActualizar = new AulaPendienteActualizar();
				aulaPendienteActualizar.setIdAulaSisinfo(aulaPendiente.getIdAulaSisinfo());			
				aulaPendienteActualizar.setFechaNovedadDesde(aulaPendiente.getFechaNovedadesDesde());
				aulasPendientes.add(aulaPendienteActualizar);
			}
			persistencia1.commit();				
			for(AulaPendienteActualizar aula : aulasPendientes) {	
				LOGGERNovedades.log(Level.INFO, "Consultando por aula pendiente: "+aula.getIdAulaSisinfo()+" - "+aula.getFechaNovedadDesde());					
				ConsultaNovedadesDatosBasicos(urlnovedades_aulas,usuario,contrasena,aula.getFechaNovedadDesde(),null,aula.getIdAulaSisinfo());
			}
				
		}catch(Exception ex){
			persistencia1.rollback();
			throw new Exception("EXCEPTION VerificaAulaSinActualizar. "+ex.getMessage());
		}
	}
	
	/*public static Timestamp fechaCambioSegundo(Timestamp fecha,int sumaOResta) {
		Date dateDesde = new Date(fecha.getTime()); 
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateDesde); 
        if(sumaOResta==1)
        	calendar.add(Calendar.SECOND, 1);
        else
        	calendar.add(Calendar.SECOND, -1);
        Date dateDesdeNueva = calendar.getTime();				       
        Timestamp ts=new Timestamp(dateDesdeNueva.getTime());  
        return ts;        
	}*/
	
}
