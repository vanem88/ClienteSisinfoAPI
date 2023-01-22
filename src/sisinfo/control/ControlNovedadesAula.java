package sisinfo.control;

import java.sql.Timestamp;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import comun.constantes.TIPO_USUARIOS;
import comun.constantes.USUARIO_AGREGADO_DESDE;
import persistencia.Persistencia;
import persistencia.dominio.AulaCompuesta;
import persistencia.dominio.AulaSisinfoParaActualizar;
import persistencia.dominio.AulaSisinfoPendiente;
import persistencia.dominio.Comentario;
import persistencia.dominio.Comision;
import persistencia.dominio.Comunicacion;
import persistencia.dominio.Participante;
import persistencia.dominio.ParticipanteComision;
import persistencia.dominio.Persona;
import persistencia.dominio.Texto;
import persistencia.dominio.TipoUsuario;
import server.actividades.ControlActividades;
import server.aula.ControlAula;
import server.evaluacion.ControlEvaluacion;
import server.rutas.ControlRutas;
import sisinfo.dto.PersonaDTO;
import sisinfo.util.AulaLog;
import utils.ManejoString;
import utils.Utils;

public class ControlNovedadesAula  extends ControlGeneral{
	
	private static final Logger LOGGERCONTROLNOVEDADES = Logger.getLogger( ControlNovedadesAula.class.getName() );
	
	public ControlNovedadesAula(Persistencia persistencia) {
		this.persistencia = persistencia;
	}
	
	public ControlNovedadesAula() {}
		
	// Agrega un docente a una comision
	public boolean agregaDocenteALaComision(AulaCompuesta aulaSisinfo,Comision comision,Persona personal, TipoUsuario tipoUsuarioDocente,Participante partAula) {
		try {
			/*Vector<?> participantesAdm = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && persona.id=="+personal.getId()+" && tipoUsuario.id=="+tipoUsuarioDocente.getId());
			if(participantesAdm!=null && participantesAdm.size()>0) {
				Participante participanteDocente = (Participante)participantesAdm.elementAt(0);
				Vector<?> participComision = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.ParticipanteComision", "aula.id=="+comision.getId()+" && participante.id=="+participanteDocente.getId());
				if(participComision==null || participComision.size()==0) {*/
					ParticipanteComision participanteDocenteComision = new ParticipanteComision();
					//participanteDocenteComision.setParticipante(participanteDocente);				
					participanteDocenteComision.setParticipante(partAula);
					participanteDocenteComision.setComision(comision);
					participanteDocenteComision.setFechaAlta(utils.Utils.hoySqlDate());
					participanteDocenteComision.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
					this.persistencia.hacerPersistente(participanteDocenteComision);
				/*}
				return true;
			}else
				return false;	*/
					return true;
		}catch(Exception ex) {
			LOGGERCONTROLNOVEDADES.log(Level.SEVERE,"Exception en agregaDocenteALaComision.... "+ex.getMessage());
			return false;
		}
	}
		
	// Registra los docentes en una comision.
	// El arreglo de PersonaDTO solo tiene cargados los DNI, el docente que se registre en una comision tiene que estar previamente registrado en Evelia y en el aula de la comision.
	public boolean modificarDocentesAUnaComsiion(AulaCompuesta aulaSisinfo, PersonaDTO[] docentes,AulaLog log,TipoUsuario tUAdmin,TipoUsuario tUResponsable,TipoUsuario tUAlumno,TipoUsuario tUDocente,TipoUsuario tUColaborador,Comision comision,ControlAula controlAula) {
		boolean modificoTodoOK = true;
		PersonaDTO docenteJSON = null;
		Persona personaDocente = null;
		log.setInfoaula(log.getInfoaula()+"Docentes: \n");
		for(int j=0;j<docentes.length;j++){
			try {
				docenteJSON =  docentes[j];
				String numeroDoc = ManejoString.eliminarPuntosDelDocumento(docenteJSON.getNumero_documento());	
			    Vector<?> personas = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc==\""+numeroDoc+"\"");
				if(personas!=null & personas.size()>0) {
					//Docente registrado en Evelia
					personaDocente = (Persona)personas.elementAt(0);
					if(personaDocente!=null) {					
						Vector<?> participantes = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && persona.id=="+personaDocente.getId()+" && (tipoUsuario.id=="+tUDocente.getId()+" || tipoUsuario.id=="+tUColaborador.getId()+")");
						if(participantes!=null & participantes.size()>0){
							//Docente participante del aula
							Participante participante = (Participante)participantes.get(0);
			    			if(participante!=null){									    				
			    				Vector<?> participantesComision = controlAula.getParticipantesComisionConTipoUsuario(comision.getId(),personaDocente.getId(),tUDocente.getNombre());
								if(participantesComision.isEmpty()){
									ParticipanteComision participanteComision = new ParticipanteComision();
									participanteComision.setParticipante(participante);
									participanteComision.setComision(comision);
									participanteComision.setFechaAlta(utils.Utils.hoySqlDate());
									participanteComision.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
									persistencia.hacerPersistente(participanteComision);
									log.setInfoaula(log.getInfoaula()+docenteJSON.getNumero_documento()+"\n");
								}
			    			}else {
			    				String error = "El docente "+personaDocente.getNumeroDoc()+", que intenta registrar en la comision, no es docente del aula. Debe enviar los datos personales con el equipo docente.";
			    				LOGGERCONTROLNOVEDADES.log(Level.SEVERE,error);
			    				log.setInfoerrores(log.getInfoerrores()+error+"<br>");
			    				modificoTodoOK = false;
			    			}
						}else {
							String error = "El docente "+personaDocente.getNumeroDoc()+", que intenta registrar en la comision, no es docente del aula. Debe enviar los datos personales con el equipo docente. ";
							LOGGERCONTROLNOVEDADES.log(Level.SEVERE,error);
		    				log.setInfoerrores(log.getInfoerrores()+error+"<br>");
		    				modificoTodoOK = false;
						}
					}else {
						String error = "El docente "+numeroDoc+", que intenta registrar en la comision, no esta registrado en Evelia. Debe enviar los datos personales con el equipo docente.";
						LOGGERCONTROLNOVEDADES.log(Level.SEVERE,error);
	    				log.setInfoerrores(log.getInfoerrores()+error+"<br>");
	    				modificoTodoOK = false;
					}
				}else {
					if(!registrarEnEvelia(docenteJSON,TIPO_USUARIOS.DOCENTE,aulaSisinfo,comision,/*tUAdmin,*/tUResponsable,tUAlumno, tUDocente,tUColaborador,log )) {
						String error = "El docente "+numeroDoc+", que intenta registrar en la comision, no esta registrado en Evelia. Debe enviar los datos personales con el equipo docente.";
						LOGGERCONTROLNOVEDADES.log(Level.SEVERE,error);
	    				log.setInfoerrores(log.getInfoerrores()+error+"<br>");
	    				modificoTodoOK = false;
					}				
				}
			}catch(Exception ex){
				LOGGERCONTROLNOVEDADES.log(Level.SEVERE,"MODIFICACION DOCENTES EN COMISIONES. "+ex.getMessage() );
			}
		}//for	
		return modificoTodoOK;
	}
		
	/***************************************************************************************************/
	/* METODOS PARA MANIPUTAR A TABLA AULA_SISINFO_PARA_ACTUALIZAR, DONDE SE REGISTARN LAS AULAS, CON SU 
	 * RESPECTIVA FECHA DE CONSULTA, QUE NO SE PUDIERON ACTUALIZAR                                     */
	/***************************************************************************************************/
	
	//Elimina, de la tabla "AULA_SISINFO_PARA_ACTUALIZAR", el aula que estaba pendiente de actualizar
	public void EliminoAulaPendienteDeActualizar(String idAulaSisinfo,Timestamp fechaD) {	
		Persistencia persistencia1  = new Persistencia();
		try {			
			persistencia1.crearTransaccion();
			AulaSisinfoParaActualizar aulaSisinfoParaActualizar = (AulaSisinfoParaActualizar)persistencia1.getObjectosPorClaseYFiltro("persistencia.dominio.AulaSisinfoParaActualizar","idAulaSisinfo==\""+idAulaSisinfo+"\"").elementAt(0);
			persistencia1.deletePersistente(aulaSisinfoParaActualizar);
			persistencia1.commit();
		}catch(Exception ex) {
			LOGGERCONTROLNOVEDADES.log(Level.SEVERE, "No se pudo eliminar el aula "+idAulaSisinfo+" con fecha desde "+fechaD+" de la tabla AULA_SISINFO_PARA_ACTUALIZAR "+ex);
			persistencia1.rollback();
		}		
	}
	
	//Agrega, en la tabla "AULA_SISINFO_PARA_ACTUALIZAR", el aula que no se pudo actualizar
	public void InsertarAulaPendienteDeActualizar(String idAulaSisinfo,String informe,Timestamp fechaDesde/*,Timestamp fechaHasta*/) {
		Persistencia persistencia1  = new Persistencia();
		try {			
			persistencia1.crearTransaccion();
			Vector aulaSisinfoParaActualizarAnterior = persistencia1.getObjectosPorClaseYFiltro("persistencia.dominio.AulaSisinfoParaActualizar","idAulaSisinfo==\""+idAulaSisinfo+"\"");
			if(aulaSisinfoParaActualizarAnterior==null || (aulaSisinfoParaActualizarAnterior!=null && aulaSisinfoParaActualizarAnterior.size()==0)) {			
				AulaSisinfoParaActualizar aulaSisinfoParaActualizar = new AulaSisinfoParaActualizar();
				aulaSisinfoParaActualizar.setIdAulaSisinfo(idAulaSisinfo);
				aulaSisinfoParaActualizar.setFechaNovedadesDesde(fechaDesde);
				//aulaSisinfoParaActualizar.setFechaNovedadesHasta(fechaHasta);
				aulaSisinfoParaActualizar.setInforme(informe.getBytes());
				persistencia1.hacerPersistente(aulaSisinfoParaActualizar);
			}
			persistencia1.commit();
		}catch(Exception ex) {
			LOGGERCONTROLNOVEDADES.log(Level.SEVERE, "No se pudo registra el aula "+idAulaSisinfo+" en la tabla AULA_SISINFO_PARA_ACTUALIZAR "+ex);
			persistencia1.rollback();
		}
		
	}
			
	/***************************************************************************************************/
	/*                                             ELIMINAR                                            */
	/***************************************************************************************************/
	
	//Elimina el docente o rsponsable de todas las comisiones en las que participa.
	public boolean EliminarDocenteOResponsableDelAula(Participante participanteAEliminar ) {
		try {
			LOGGERCONTROLNOVEDADES.log(Level.INFO,participanteAEliminar.getTipoUsuario().getNombre()+" a eliminar del aula: "+participanteAEliminar.getPersona().getNumeroDoc());
			ControlEvaluacion controlEvaluacion = new ControlEvaluacion(persistencia);
			ControlRutas controlRutas = new ControlRutas(persistencia);
			
			Vector participantesComision = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.ParticipanteComision", "participante.id=="+participanteAEliminar.getId());
			if(participantesComision!=null && participantesComision.size()>0) {
				for(int j = 0;j<participantesComision.size();j++) {
					ParticipanteComision pce = (ParticipanteComision)participantesComision.elementAt(j);
					LOGGERCONTROLNOVEDADES.log(Level.INFO,"por elimine docente en la comision: "+pce.getComision().getNombre());
					persistencia.deletePersistente(pce);					
				}											
			}			
			//LOGGERCONTROLNOVEDADES.log(Level.INFO,"por eliminar evaluaciones");
			//LOGGERCONTROLNOVEDADES.log(Level.INFO,"Aula: "+participanteAEliminar.getAula().getId());
			//LOGGERCONTROLNOVEDADES.log(Level.INFO,"Persona: "+participanteAEliminar.getPersona().getId());			
			//controlEvaluacion.eliminandoEvaluacionesDeParticipantesQueSeBorraDelAula(participanteAEliminar);
			LOGGERCONTROLNOVEDADES.log(Level.INFO,"por eliminar rutas");
			controlRutas.eliminandoRutasDeParticipantesQueSeBorraDelAula(participanteAEliminar);
			LOGGERCONTROLNOVEDADES.log(Level.INFO,"por eliminar participante");
			persistencia.deletePersistente(participanteAEliminar);
			return true;
		}catch(Exception ex) {
			ex.printStackTrace();
			LOGGERCONTROLNOVEDADES.log(Level.SEVERE,"Exception EliminarDocenteOResponsableDelAula");
			return false;
		}
	}
	
	//Elimina el alumno de una comsion.
	public boolean EliminarAlumnoDeUnaComision(ParticipanteComision alumnoAEliminar,TipoUsuario tipoUsuarioAlumno) {
		try {	
			LOGGERCONTROLNOVEDADES.log(Level.INFO,"Alumno a eliminar: "+alumnoAEliminar.getParticipante().getPersona().getNombreYApellido()+" de la comision: "+alumnoAEliminar.getComision().getNombre());
			ControlActividades controlActividades = new ControlActividades(persistencia);
			//ControlEvaluacion controlEvaluacion = new ControlEvaluacion(persistencia);
			ControlRutas controlRutas = new ControlRutas(persistencia);
			
			Long idAula = alumnoAEliminar.getComision().getAulaCompuesta().getId();
			Participante estudianteAEliminar = alumnoAEliminar.getParticipante();
			Long idPersona = estudianteAEliminar.getPersona().getId();
						
			controlActividades.eliminandoNotaDeParticipantesQUeCambianDeComision(alumnoAEliminar);	
			//controlEvaluacion.eliminandoEvaluacionesDeParticipantesQueCambianDeComision(alumnoAEliminar);
			//LOGGERCONTROLNOVEDADES.log(Level.INFO,"Se eliminaron las evaluaciones" );
			controlRutas.eliminandoRutasDeParticipantesQueCambianDeComision(alumnoAEliminar);
			persistencia.deletePersistente(alumnoAEliminar);
						
			Vector participanteAlumnoOtrasComisiones = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.ParticipanteComision","comision.aulaCompuesta.id=="+idAula+" && participante.persona.id=="+idPersona+" && participante.tipoUsuario.id=="+tipoUsuarioAlumno.getId());
			if(participanteAlumnoOtrasComisiones==null || participanteAlumnoOtrasComisiones.size()==0) {
				LOGGERCONTROLNOVEDADES.log(Level.INFO,"El alumno no participa de niguna otra comision");
				//El estudiante no participa de otras comisiones, entonces lo elimino del aula.
				//controlEvaluacion.eliminandoEvaluacionesDeParticipantesQueSeBorraDelAula(estudianteAEliminar);
				//LOGGERCONTROLNOVEDADES.log(Level.INFO,"Se eliminaron las evaluaciones del aula del alumno");
				controlRutas.eliminandoRutasDeParticipantesQueSeBorraDelAula(estudianteAEliminar);
				persistencia.deletePersistente(estudianteAEliminar);				
			}else
				LOGGERCONTROLNOVEDADES.log(Level.INFO,"El alumno participa de otras comisiones, por ende no se elimina nada del aula");
						
			return true;
		}catch(Exception ex) {
			LOGGERCONTROLNOVEDADES.log(Level.SEVERE,"Exception eliminando alumno: "+ex.getMessage());
			return false;
		}
	}
	
	//Elimina la comision pasada como parametro
	public boolean EliminarComision(Long idComision) {
		try{					
			 Comision comision = (Comision) persistencia.getObjectoPorId("persistencia.dominio.Comision",idComision);
			 LOGGERCONTROLNOVEDADES.log(Level.INFO, "Eliminando comision: "+idComision+" "+comision.getNombre());
			 
			 Vector herrams = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.AulaHerramienta", "aula.id=="+idComision);
			 persistencia.deletePersistente(herrams);
			 
			 Vector config = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.ConfiguracionPorAula", "aula.id=="+idComision);
			 persistencia.deletePersistente(config);
			 
			 Vector partic = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.ParticipanteComision", "comision.id=="+idComision);			 
			 persistencia.deletePersistente(partic);
			 			 
			 /*Vector eac = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Evaluacion","","aula.id=="+comision.getId(),true);
			 persistencia.deletePersistente(eac);
			 LOGGERCONTROLNOVEDADES.log(Level.INFO, "Se eliminaron las evaluaciones de la comisión");*/
			 
			 Vector msjInicio = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.PersonaMensajeIngreso", "","aula.id=="+comision.getId(),true);
			 persistencia.deletePersistente(msjInicio);
			 
			 Vector seguimAlmacMaterial = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.SeguimientoAlmacenamientoMaterial", "","aula.id=="+comision.getId(),true);
			 persistencia.deletePersistente(seguimAlmacMaterial);
			 
			 Vector activ = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Actividad", "","aula.id=="+comision.getId(),true);
			 persistencia.deletePersistente(activ);
			 
			 Vector foro = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Foro", "","aula.id=="+comision.getId(),true);
			 persistencia.deletePersistente(foro);
			 			 
			 //agregue
			 Vector ruta = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Ruta", "","aula.id=="+comision.getId(),true);
			 persistencia.deletePersistente(ruta);
			 
			 Vector msjInicio2 = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.MensajeIngreso", "","aula.id=="+comision.getId(),true);
			 persistencia.deletePersistente(msjInicio2);
			 
			 Vector msjInicio3 = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.MensajeIngreso", "","nivelAula.id=="+comision.getId(),true);
			 persistencia.deletePersistente(msjInicio3);
			 
			 Comunicacion comunicacion = comision.getPizarron();
			 if(comunicacion!=null){
				 String filtroPizarron = "comunicacion.id == "+ comunicacion.getId();
				 Vector textos = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Texto",filtroPizarron);
				 for(int i=0;i<textos.size();i++){
					 Texto txt =(Texto) textos.elementAt(i);
					 if(txt.isConComentarios() && !txt.getComentarios().isEmpty()){
						 eliminarComentariosHijos(txt.getId(),true,null);
					 }
				 }				 
				 persistencia.deletePersistente(textos);
			 }
						 
			 persistencia.deletePersistente(comision);
			 LOGGERCONTROLNOVEDADES.log(Level.INFO, "Se elimino la comisión.");
			 return true;
		}catch(Exception ee){
			ee.printStackTrace();
			LOGGERCONTROLNOVEDADES.log(Level.SEVERE, "EXCEPTION EliminarComision. "+ee.getMessage());
			return false;
		}
	}
	
	//Elimina los comentarios de los pizarrones de la comision.
	private void eliminarComentariosHijos(Long idTxtPpal,boolean ppal,Long idComentPadre)throws Exception{
		try{
			String filtro=" textoPadre.id=="+idTxtPpal+" && principal=="+ppal;
			if(!ppal) filtro += " && comentPadre.id=="+idComentPadre;
			Vector comentarios = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Comentario",filtro);
			for(int i=0;i<comentarios.size();i++){
				 Comentario com =(Comentario) comentarios.elementAt(i);
				 if(!com.getComentHijos().isEmpty()){
					 eliminarComentariosHijos(idTxtPpal,false,com.getId());
				 }
			 }
			 persistencia.deletePersistente(comentarios);
		}catch(Exception ee){
			Utils.generarExcepcion(ee,this.getClass().getSimpleName()+".java","eliminarComentariosHijos");
		}
	}
		
}
