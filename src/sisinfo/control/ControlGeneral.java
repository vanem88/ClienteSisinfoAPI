package sisinfo.control;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import comun.constantes.CARPETAS_ARCHIVOS_RECURSO;
import comun.constantes.CONSTANTES_ENTERAS;
import comun.constantes.DATOS_GENERALES;
import comun.constantes.PARAMETROS_PAGINAS;
import comun.constantes.TIPO_USUARIOS;
import comun.constantes.USUARIO_AGREGADO_DESDE;
import persistencia.dominio.Aula;
import persistencia.dominio.AulaCompuesta;
import persistencia.dominio.Calendario;
import persistencia.dominio.Comision;
import persistencia.dominio.ConfiguracionGrupo;
import persistencia.dominio.ItemPlantillaPermiso;
import persistencia.dominio.MaterialAdicional;
import persistencia.dominio.Participante;
import persistencia.dominio.ParticipanteComision;
import persistencia.dominio.PermisoAcceso;
import persistencia.dominio.Persona;
import persistencia.dominio.Pizarron;
import persistencia.dominio.PlantillaPermiso;
import persistencia.dominio.TipoAula;
import persistencia.dominio.TipoUsuario;
import server.Control;
import server.aula.ControlAula;
import server.secretaria.aula.ControlPlantillaPermiso;
import sisinfo.dto.PersonaDTO;
import sisinfo.util.AulaLog;
import sisinfo.util.Registros;
import sisinfo.util.Util;
import utils.ManejoString;
import utils.Utils;
import utils.UtilsGenerarHtml;
import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import utils.mail.EnviarMailGmail;

public class ControlGeneral extends Control{
	
	private static final Logger LOGGERCONTROLGENERAL = Logger.getLogger( ControlGeneral.class.getName() );
	
	//Registra a un usuario en Evelia.
	public boolean registrarEnEvelia(PersonaDTO usuario,String tipoUsuario,AulaCompuesta aulaSisinfo,Comision comision,/*TipoUsuario tUAdmin,*/TipoUsuario tUResponsable,TipoUsuario tUAlumno,TipoUsuario tUDocente,TipoUsuario tUColaborador,AulaLog InformeAula) {
		try {
			LOGGERCONTROLGENERAL.log(Level.INFO,"Registrando usuario en Evelia........");
			String apellido = usuario.getApellido();
			String nombre = usuario.getNombre();
			String email = usuario.getEmail();
			String telefono = usuario.getTelefono();
			String dni = usuario.getNumero_documento();
			String tipo_doc = usuario.getTipo_documento();
			if(apellido!=null && apellido.compareTo("")!=0 && nombre!=null && nombre.compareTo("")!=0 && email!=null && email.compareTo("")!=0 ) {
				String respuesta = agregarNuevaPersona(apellido, nombre, tipo_doc ,dni, email,telefono);
				if(respuesta.compareTo("Creado")==0) {
					Persona persona = (Persona)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc == \""+dni+"\"").elementAt(0);
					boolean res = agregaUsuarioAlAula(tipoUsuario,aulaSisinfo,comision,persona,tUResponsable,tUAlumno,tUDocente,tUColaborador,InformeAula);
					if(!res) Registros.RegistrarInfo(Level.SEVERE,"No se pudo agregar el "+tipoUsuario+" con DNI "+persona.getNumeroDoc()+" al aula "+aulaSisinfo.getIdAulaSisinfo(), InformeAula);
					return res;
				}else {
					Registros.RegistrarInfo(Level.SEVERE,"El "+tipoUsuario+" que intenta agregar al aula "+aulaSisinfo.getId()+", no pudo ser registrado. Controle su e-mail.", InformeAula);
					return false;
				}						
			}else {
				Registros.RegistrarInfo(Level.INFO,"El usuario con DNI "+dni+" no esta registrado en Evelia, para registrarlo debe enviar email, nombre y apellido.", InformeAula);
				//return false;
				return true;
			}			
		}catch(Exception ex) {
			LOGGERCONTROLGENERAL.log(Level.SEVERE,"Exception registrarEnEvelia"+ex.getMessage());
			return false;
		}
	}
	
	public Persona buscarPersona(String numeroDoc) {
		try{
			numeroDoc = ManejoString.eliminarPuntosDelDocumento(numeroDoc);
			return (Persona)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc == \""+numeroDoc+"\"").elementAt(0);			
		} catch (Exception ee) {
			return null;
		}		
	}
	
	public String agregarNuevaPersona(String apellidoPersona, String nombrePersona, String tipoDocPersona, String numeroDoc, String emailPersona,String telefono) throws Exception {
		try{
			Persona persona = new Persona();
			numeroDoc = ManejoString.eliminarPuntosDelDocumento(numeroDoc);		
			String password = server.general.Util.encrypt(utils.Utils.getNuevaClave().toString());
			
			if(password!=null) {						
				String cuerpo = "Hola \""+nombrePersona+" "+apellidoPersona+"\"\n\n";
				cuerpo +="Bienvenido al "+DATOS_GENERALES.SIGLA_SISTEMA+", el Campus Virtual de la "+DATOS_GENERALES.NOMBRE_INSTITUCION+".\n\n";
				cuerpo +="Estos son los datos necesarios para ingresar al Campus Virtual:\n\n";
				cuerpo +="Usuario:\"" +numeroDoc+"\"\n\n";
				cuerpo +="Contraseña:\"" +server.general.Util.decrypt(password)+"\"\n\n";
				cuerpo +="Acceda a la página del "+DATOS_GENERALES.SIGLA_SISTEMA+" en "+UtilsGenerarHtml.enlaceNombreServidor()+" y podrá comenzar a utilizar el sistema.\n\n";
				cuerpo +=DATOS_GENERALES.SALUDO_MAIL_GENERICO;
				
				EnviarMailGmail enviarMailGmail = new EnviarMailGmail();					
				MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
				mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
				mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
				mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
				mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
				mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
				CommandMap.setDefaultCommandMap(mc);
				Thread.currentThread().setContextClassLoader(getClass().getClassLoader()); 
				
				if(!enviarMailGmail.enviandoMailRegistro(true, emailPersona, "Bienvenido al "+DATOS_GENERALES.SIGLA_SISTEMA, cuerpo,  "","")){
					return "Correo invalido";
				} 
				
				persona.setLogin(numeroDoc);
				persona.setPassword(password);
				persona.setNombre(nombrePersona.toUpperCase());
				persona.setApellido(apellidoPersona.toUpperCase());
				persona.setTipoDoc(tipoDocPersona);
				persona.setNumeroDoc(numeroDoc);
				persona.setEmail(emailPersona);
				persona.setTelefono(telefono);
				persona.setNumeroCelular(telefono);
				persona.setFechaCambioClave(Utils.hoySqlDate());
				persona.setCreadaDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				
				persistencia.hacerPersistente(persona);
				TipoUsuario tipousuario = (TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.ALUMNO+"\" ").elementAt(0);
				Aula campus = (Aula)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Aula","nombre","nombre==\"Campus\"",true).elementAt(0);
				Participante participante = new Participante();
				participante.setAula(campus);
				participante.setPersona(persona);
				participante.setTipoUsuario(tipousuario);
				participante.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				participante.setFechaAlta(utils.Utils.hoySqlDate());
				
				persistencia.hacerPersistente(participante);
				return "Creado";
			}else
			    return "";
		} catch (Exception ee) {
			LOGGERCONTROLGENERAL.log(Level.SEVERE, "Exception agregar nueva persona en Cliente Api sisinfo.");
			Utils.generarExcepcion(ee,this.getClass().getSimpleName()+".java","agregarNuevaPersona en Cliente Api sisinfo");
			return "";
		}
	}
	
	//Crea una nueva persona y envia a su correo los datos de acceso
	//public String agregarNuevaPersona(String apellidoPersona, String nombrePersona, String tipoDocPersona, String numeroDoc, String emailPersona,String telefono,Long idPersonaAct) throws Exception {
	public String agregarNuevaPersonayEnviaMail(String apellidoPersona, String nombrePersona, String tipoDocPersona, String numeroDoc, String emailPersona,String telefono) throws Exception {
		try{
			Persona personal = null;
			numeroDoc = ManejoString.eliminarPuntosDelDocumento(numeroDoc);
			try {
				personal = (Persona)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc == \""+numeroDoc+"\"").elementAt(0);
			}catch (Exception ex) {}
			
			if (personal != null){
				return "Ya existe";  
			}
			
			return agregarNuevaPersona( apellidoPersona,  nombrePersona,  tipoDocPersona,  numeroDoc,  emailPersona, telefono);		
		} catch (Exception ee) {
			LOGGERCONTROLGENERAL.log(Level.SEVERE, "Exception agregar nueva persona.");
			Utils.generarExcepcion(ee,this.getClass().getSimpleName()+".java","agregarNuevaPersona");
			return "";
		}
	}
			
	//Agrega una persona al aula.
	public boolean agregaUsuarioAlAula(String tipoUsuario,AulaCompuesta aulaSisinfo,Comision comision,Persona personal, /*TipoUsuario tipoUsuarioAdmin,*/TipoUsuario tipoUsuarioResponsable,TipoUsuario tipoUsuarioAlumno,TipoUsuario tipoUsuarioDocente,TipoUsuario tipoUsuarioColaborador,AulaLog InformeAula) {
		try {
			boolean agregoResp = false;
			LOGGERCONTROLGENERAL.log(Level.INFO,"Agregando el "+tipoUsuario+" "+personal.getNumeroDoc()+" a "+aulaSisinfo.getNombre()+" comision: "+comision);
			
			if(tipoUsuario.compareTo(TIPO_USUARIOS.RESPONSABLE)==0) {					
				//Agrego al usuario como responsable.
				Participante participanteResponsable = new Participante();
				participanteResponsable.setPersona(personal);
				participanteResponsable.setTipoUsuario(tipoUsuarioResponsable);
				participanteResponsable.setAula(aulaSisinfo);
				participanteResponsable.setFechaAlta(utils.Utils.hoySqlDate());
				participanteResponsable.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				persistencia.hacerPersistente(participanteResponsable);
								
				Vector comisiones = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Comision","nombre","aulaCompuesta.id == "+aulaSisinfo.getId());
				for(int i = 0;i<comisiones.size();i++) {
					Comision aulaComision = (Comision) comisiones.elementAt(i);	
					ParticipanteComision participanteRespComision = new ParticipanteComision();
					participanteRespComision.setParticipante(participanteResponsable);				
					participanteRespComision.setComision(aulaComision);
					participanteRespComision.setFechaAlta(utils.Utils.hoySqlDate());
					participanteRespComision.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
					persistencia.hacerPersistente(participanteRespComision);
				}							
				agregoResp = true;					
			}
			
			if(tipoUsuario.compareTo(TIPO_USUARIOS.DOCENTE)==0 || tipoUsuario.compareTo(TIPO_USUARIOS.COLABORADOR)==0 ) {				
				if(comision==null) {
					//Agrego al usuario como docente.
					Participante participanteDocente = new Participante();
					participanteDocente.setPersona(personal);
					if(tipoUsuario.compareTo(TIPO_USUARIOS.DOCENTE)==0)
						participanteDocente.setTipoUsuario(tipoUsuarioDocente);
					else participanteDocente.setTipoUsuario(tipoUsuarioColaborador);							
					participanteDocente.setAula(aulaSisinfo);
					participanteDocente.setFechaAlta(utils.Utils.hoySqlDate());
					participanteDocente.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
					persistencia.hacerPersistente(participanteDocente);
					agregoResp = true;
				}else 			
					Registros.RegistrarInfo(Level.SEVERE,"El docente con DNI "+personal.getNumeroDoc()+", que intenta registrar en la comisión "+comision.getNombre()+", no es docente del aula "+aulaSisinfo.getIdAulaSisinfo(), InformeAula);
			}
			
			if(tipoUsuario.compareTo(TIPO_USUARIOS.ALUMNO)==0) {
				Participante participanteAlumno = null;
				
				Vector participantesAalumno = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && persona.id=="+personal.getId()+" && tipoUsuario.id=="+tipoUsuarioAlumno.getId());
				if(participantesAalumno.size()==0){						
					//Agrego al usuario como alumno.
					participanteAlumno = new Participante();
					participanteAlumno.setAula(aulaSisinfo);
					participanteAlumno.setPersona(personal);
					participanteAlumno.setTipoUsuario(tipoUsuarioAlumno);
					participanteAlumno.setFechaAlta(utils.Utils.hoySqlDate());
					participanteAlumno.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
					persistencia.hacerPersistente(participanteAlumno);					
				}else {
					participanteAlumno = (Participante)participantesAalumno.elementAt(0);
				}
				
				ParticipanteComision participanteAlumnoComision = new ParticipanteComision();
				participanteAlumnoComision.setParticipante(participanteAlumno);				
				participanteAlumnoComision.setComision(comision);
				participanteAlumnoComision.setFechaAlta(utils.Utils.hoySqlDate());
				participanteAlumnoComision.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				this.persistencia.hacerPersistente(participanteAlumnoComision);	
			
				agregoResp = true;
			}
			
			//Si no existe, agrego al usuario en la organizacion, como alumno.
			Vector participantesOrganizacion = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getAulaCompuesta().getId()+" && persona.id=="+personal.getId()+" && tipoUsuario.id=="+tipoUsuarioAlumno.getId());
			if(participantesOrganizacion.size()==0){
				//Debemos asociar a esta persona en la organizacion
				Participante participanteOrganizacion = new Participante();
				participanteOrganizacion.setAula(aulaSisinfo.getAulaCompuesta());
				participanteOrganizacion.setPersona(personal);
				participanteOrganizacion.setTipoUsuario(tipoUsuarioAlumno);
				participanteOrganizacion.setFechaAlta(utils.Utils.hoySqlDate());
				participanteOrganizacion.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				persistencia.hacerPersistente(participanteOrganizacion);						
				agregoResp = true;
			}
			
			return agregoResp;
		}catch(Exception ex) {
			ex.printStackTrace();
			LOGGERCONTROLGENERAL.log(Level.SEVERE,"Exception en agregaUsuarioAlAula"+ex.getMessage());
			return false;
		}
	}
	
	//Obtiene todos los participantes webMaster del Campus a replicar
	public Vector obtenerParticipantesAReplicar(Long idAula)throws Exception{
		try{
			String filtro = "aula.id=="+idAula+" && replicar=="+true+"  && tipoUsuario.nombre!=\""+TIPO_USUARIOS.WEBMASTER+"\"";
			Vector personas=(Vector) this.persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante",filtro);
			return personas;
		} catch (Exception ee) {
			Utils.generarExcepcion(ee,this.getClass().getSimpleName()+".java","obtenerParticipantesAReplicar");
			return new Vector();
		}
	}
	
	//Crea la comision con los datos basicos
	public Comision crearDatosBasicosComision(AulaLog InformeAula, AulaCompuesta aulaSisinfo, int cantidadDeComisiones ,String nombreComision,TipoAula tipoAulaCurso,String pathraizArchivos,Vector participantesDeAula,/*Participante participanteAdministrador,*/Participante participanteResponsable,ConfiguracionGrupo configuracionGrupo2,ConfiguracionGrupo configuracionGrupoBorr2,Vector participantesDocentes) {
		boolean mkdir;
		
		try {
			// crea el materialAdicional de la comision
			MaterialAdicional materialAdicionalComision = new MaterialAdicional();
			this.persistencia.hacerPersistente(materialAdicionalComision);
			
			// crea el Pizarron de la carrera
			Pizarron pizarronComision = new Pizarron();
			this.persistencia.hacerPersistente(pizarronComision);
	
			//crea el calendario de la carrera
			Calendario calendarioComision = new Calendario();
			persistencia.hacerPersistente(calendarioComision);
	
			Comision comision = new Comision();
			if(cantidadDeComisiones>0) comision.setNombre(nombreComision);
			else comision.setNombre(aulaSisinfo.getNombre());
	
			comision.setTipoAula(tipoAulaCurso);
			comision.setAulaCompuesta(aulaSisinfo);
			comision.setFechaCreacion(utils.Utils.hoySqlDate());
			comision.setEspacioFisicoDisponible(new Float(CONSTANTES_ENTERAS.CUPO_ESPACIO_FISICO_COMISION));//Limite espacio fisico disponible (20MB)
			comision.setConfiguracionGrupo(configuracionGrupo2);
			comision.setConfiguracionGrupoBorrador(configuracionGrupoBorr2);
			comision.setMaterialAdicional(materialAdicionalComision);
			comision.setPizarron(pizarronComision);
		    comision.setCalendario(calendarioComision);
			this.persistencia.hacerPersistente(comision);
			
			materialAdicionalComision.setNombre(comision.getId().toString());
							
			// creo el directorio para el curso
			// creo la carpeta para la comision dentro del curso en la carpeta actividades
			File directorioActividadesComision = new File(pathraizArchivos+File.separator+CARPETAS_ARCHIVOS_RECURSO.ACTIVIDADES+File.separator+PARAMETROS_PAGINAS.ID_COMISION+comision.getId());
			mkdir = directorioActividadesComision.mkdirs();
			
			// creo la carpeta para la comision dentro del curso en la carpeta materiales adicionales
			File directorioMaterialesAdicionales = new File(pathraizArchivos+File.separator+CARPETAS_ARCHIVOS_RECURSO.MATERIALES_ADICIONALES+File.separator+PARAMETROS_PAGINAS.ID_COMISION+comision.getId());
			mkdir = directorioMaterialesAdicionales.mkdirs();
			
			// creo la carpeta para la comision dentro del curso en la carpeta pizarron
			File directorioPizarronComision = new File(pathraizArchivos+File.separator+CARPETAS_ARCHIVOS_RECURSO.PIZARRON+File.separator+PARAMETROS_PAGINAS.ID_COMISION+comision.getId());
			mkdir = directorioPizarronComision.mkdirs();
					
			// crear el participante comision para el webmaster y para otros participantes de la organizacion que deban de replicarse
			for(int j=0;j<participantesDeAula.size();j++){
				Participante participWMAula=(Participante)participantesDeAula.elementAt(j);
				ParticipanteComision participanteComision = new ParticipanteComision();
				participanteComision.setParticipante(participWMAula);
				participanteComision.setComision(comision);
				participanteComision.setFechaAlta(utils.Utils.hoySqlDate());
				participanteComision.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				this.persistencia.hacerPersistente(participanteComision);
			}
	
			//Administrador
			/*if(participanteAdministrador!=null){
				ParticipanteComision participanteComision = new ParticipanteComision();
				participanteComision.setParticipante(participanteAdministrador);
				participanteComision.setComision(comision);
				participanteComision.setFechaAlta(utils.Utils.hoySqlDate());
				participanteComision.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				this.persistencia.hacerPersistente(participanteComision);
			}	*/
			
			//Responsable
			if(participanteResponsable!=null) {
				ParticipanteComision participanteComisionResp = new ParticipanteComision();
				participanteComisionResp.setParticipante(participanteResponsable);
				participanteComisionResp.setComision(comision);
				participanteComisionResp.setFechaAlta(utils.Utils.hoySqlDate());
				participanteComisionResp.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				this.persistencia.hacerPersistente(participanteComisionResp);
			}	
			
			if(cantidadDeComisiones==0) {
				// crear el participante comision para el equipo docente
				for(int j=0;j<participantesDocentes.size();j++){
					Participante participDocente=(Participante)participantesDocentes.elementAt(j);
					ParticipanteComision participanteComisionDoc = new ParticipanteComision();
					participanteComisionDoc.setParticipante(participDocente);
					participanteComisionDoc.setComision(comision);
					participanteComisionDoc.setFechaAlta(utils.Utils.hoySqlDate());
					participanteComisionDoc.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
					this.persistencia.hacerPersistente(participanteComisionDoc);
				}					
			}							
			return comision;
		}catch(Exception ex) {
			ex.printStackTrace();
			Registros.RegistrarInfo(Level.SEVERE,"No se pudo crear la nueva comision "+nombreComision, InformeAula);
			return null;
		}
	}
	
	//Resgistra los estudiantes en una comision
	public boolean cargarEstudiantesAUnaComision(AulaCompuesta aulaSisinfo, PersonaDTO[] estudiantes,AulaLog log,Comision comision,ControlAula controlAula,/*TipoUsuario tUAdmin,*/TipoUsuario tUResponsable,TipoUsuario tUAlumno,TipoUsuario tUDocente,TipoUsuario tUColaborador) {
		boolean cargoTodoOK = true;
		PersonaDTO estudianteJSON = null;
		log.setInfoaula(log.getInfoaula()+"Estudiantes: \n");
		Persona personaEstudiante;
		
		for(int j=0;j<estudiantes.length;j++){	
			String numeroDoc = "";
			try {
				personaEstudiante = null;
				estudianteJSON =  estudiantes[j];
				numeroDoc = estudianteJSON.getNumero_documento();
				try {
					personaEstudiante = (Persona)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc==\""+numeroDoc+"\"").elementAt(0);
				}catch(Exception ex) {}
				
				if(personaEstudiante!=null) {
					LOGGERCONTROLGENERAL.log(Level.INFO, "Estudiante: "+numeroDoc);
					Vector estudiantesOrganizacion = controlAula.getParticipantes(aulaSisinfo.getAulaCompuesta().getId(),personaEstudiante.getId());						
					if(estudiantesOrganizacion.isEmpty()){		
						Participante participanteAlumnoOrg = new Participante();
						participanteAlumnoOrg.setAula(aulaSisinfo.getAulaCompuesta());
						participanteAlumnoOrg.setPersona(personaEstudiante);
						participanteAlumnoOrg.setTipoUsuario(tUAlumno);
						participanteAlumnoOrg.setFechaAlta(utils.Utils.hoySqlDate());
						participanteAlumnoOrg.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
						persistencia.hacerPersistente(participanteAlumnoOrg);
					}
					LOGGERCONTROLGENERAL.log(Level.INFO, "Se registro estudiante en organizacion");
					Vector participantesAlumno = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && persona.id=="+personaEstudiante.getId()+" && tipoUsuario.id=="+tUAlumno.getId());
					if(participantesAlumno.isEmpty()) {							
						Participante participanteAlumno = null;
						participanteAlumno = new Participante();
						participanteAlumno.setAula(aulaSisinfo);
						participanteAlumno.setPersona(personaEstudiante);
						participanteAlumno.setTipoUsuario(tUAlumno);
						participanteAlumno.setFechaAlta(utils.Utils.hoySqlDate());
						participanteAlumno.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
						persistencia.hacerPersistente(participanteAlumno);							
						LOGGERCONTROLGENERAL.log(Level.INFO, "Se registro estudiante en aula");
						
						ParticipanteComision participanteAlumnoComision = new ParticipanteComision();
						participanteAlumnoComision.setParticipante(participanteAlumno);
						participanteAlumnoComision.setComision(comision);
						participanteAlumnoComision.setFechaAlta(utils.Utils.hoySqlDate());
						participanteAlumnoComision.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
						this.persistencia.hacerPersistente(participanteAlumnoComision);	
						log.setInfoaula(log.getInfoaula()+estudianteJSON.getNombre()+" "+estudianteJSON.getApellido()+" - "+estudianteJSON.getNumero_documento()+"\n");						
						LOGGERCONTROLGENERAL.log(Level.INFO, "Se registro estudiante en comision");
						
					}else {
						Participante participanteAlumno = (Participante)participantesAlumno.elementAt(0);
						ParticipanteComision participanteAlumnoComision = new ParticipanteComision();
						participanteAlumnoComision.setParticipante(participanteAlumno);
						participanteAlumnoComision.setComision(comision);
						participanteAlumnoComision.setFechaAlta(utils.Utils.hoySqlDate());
						participanteAlumnoComision.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
						this.persistencia.hacerPersistente(participanteAlumnoComision);	
						log.setInfoaula(log.getInfoaula()+estudianteJSON.getNombre()+" "+estudianteJSON.getApellido()+" - "+estudianteJSON.getNumero_documento()+"\n");
						LOGGERCONTROLGENERAL.log(Level.INFO, "Se registro estudiante en comision");
					}
										
				}else {	
					LOGGERCONTROLGENERAL.log(Level.INFO, "Estudiante no resgiatrdo en evelia: "+numeroDoc);
					if(!registrarEnEvelia(estudianteJSON,TIPO_USUARIOS.ALUMNO,aulaSisinfo,comision,/*tUAdmin,*/tUResponsable,tUAlumno, tUDocente, tUColaborador,log ))
						cargoTodoOK = false;
				}
			}catch(Exception ex){
				Registros.RegistrarInfo(Level.SEVERE,"No se puede registrar al estudiante "+numeroDoc+ " "+ex.getMessage(), log);
				cargoTodoOK = false;
			}
		}//for
		return cargoTodoOK;
	}
	
	//Crea los permisos para todos los recursos del nivel.(aula/comision) 
	public void crearPermisoAcceso(ControlPlantillaPermiso ctrlPlantillaPermiso, Long idPlant, Aula aula,String nivel) throws Exception{
		
		PlantillaPermiso plant = (PlantillaPermiso)persistencia.getObjectoPorId("persistencia.dominio.PlantillaPermiso",idPlant);			
		for(Iterator it=plant.getItemPlantillaPermisos().iterator();it.hasNext();){
			try {
				ItemPlantillaPermiso itemPlantilla=(ItemPlantillaPermiso) it.next();
			
				if((itemPlantilla.getNivel().compareTo("aula")==0 && nivel.compareTo("aula")==0) || (itemPlantilla.getNivel().compareTo("comision")==0  && nivel.compareTo("comision")==0)){					
					PermisoAcceso permisoAcceso = new PermisoAcceso();
					permisoAcceso.setRecurso(itemPlantilla.getRecurso());
					permisoAcceso.setTipoPermiso(itemPlantilla.getTipoPermiso());
					permisoAcceso.setAula(aula);
					permisoAcceso.setTipoUsuario(itemPlantilla.getTipoUsuario());						
					persistencia.hacerPersistente(permisoAcceso);					
				}						
			}catch (Exception ee) {					
				LOGGERCONTROLGENERAL.log(Level.SEVERE,"Exception crearPermisoAcceso. "+ee.getMessage());
			}
		}
   }
	
	//Envia un correo electronico
	public String enviarMail(String emailPersona,String cuerpo) {
    	try {
        	EnviarMailGmail enviarMailGmail = new EnviarMailGmail();					
			MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
			mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
			mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
			mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
			mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
			mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
			CommandMap.setDefaultCommandMap(mc);
			Thread.currentThread().setContextClassLoader(getClass().getClassLoader()); 
			if(!enviarMailGmail.enviandoMailRegistro(true, emailPersona, "Bienvenido al "+DATOS_GENERALES.SIGLA_SISTEMA, cuerpo,  "",""))
				return "Correo no enviado";
			else 
				return "";
    	}catch(Exception ex){
    		return "Correo no enviado";
    	}
	}
}
