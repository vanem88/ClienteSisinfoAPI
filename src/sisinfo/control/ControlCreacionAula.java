package sisinfo.control;

import java.io.File;
import java.sql.Timestamp;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.Gson;
import comun.ExceptionArchivos;
import comun.constantes.AUTORIZACION_GRUPO;
import comun.constantes.CARPETAS_ARCHIVOS_RECURSO;
import comun.constantes.CONSTANTES_ENTERAS;
import comun.constantes.DATOS_GENERALES;
import comun.constantes.ESTADO_AULAS;
import comun.constantes.ESTADO_INSCRIPCION_ONLINE;
import comun.constantes.PARAMETROS_PAGINAS;
import comun.constantes.TIPO_AULAS;
import comun.constantes.TIPO_GRUPOS;
import comun.constantes.TIPO_USUARIOS;
import comun.constantes.USUARIO_AGREGADO_DESDE;
import persistencia.Persistencia;
import persistencia.dominio.AulaCompuesta;
import persistencia.dominio.Calendario;
import persistencia.dominio.Comision;
import persistencia.dominio.ConfiguracionGrupo;
import persistencia.dominio.Curso;
import persistencia.dominio.EstadoAula;
import persistencia.dominio.FAQ;
import persistencia.dominio.InscripcionOnLine;
import persistencia.dominio.Material;
import persistencia.dominio.Novedad;
import persistencia.dominio.Organizacion;
import persistencia.dominio.Participante;
import persistencia.dominio.ParticipanteComision;
import persistencia.dominio.Persona;
import persistencia.dominio.Pizarron;
import persistencia.dominio.PlantillaPermiso;
import persistencia.dominio.TipoAula;
import persistencia.dominio.TipoRecurso;
import persistencia.dominio.TipoUsuario;
import server.Singleton;
import server.aula.ControlAula;
import server.secretaria.aula.ControlConfigurarPermisosAula;
import server.secretaria.aula.ControlHerramientasAula;
import server.secretaria.aula.ControlPlantillaPermiso;
import sisinfo.dto.AulaDTO;
import sisinfo.dto.ComisionDTO;
import sisinfo.dto.PersonaDTO;
import sisinfo.util.AulaLog;
import sisinfo.util.Util;
import utils.ManejoArchivoDeTexto;
import utils.ManejoString;
import utils.Utils;

public class ControlCreacionAula  extends ControlGeneral{
	
	private static final Logger LOGGERCONTROL = Logger.getLogger( ControlCreacionAula.class.getName() );
	
	public ControlCreacionAula(Persistencia persistencia) {
		this.persistencia = persistencia;
	}
	
	/*
	 * Crea aulas automaticamente cuando existan peticiones desde Sisinfo
	 * @param gson, nombreCurso, codOrganizacion, idAdministrador,plantillaPermisos, fechaInicioUso, idAulaSisinfo
	 */
	public Curso creaAulaDatosMinimos(String nombreCurso,String codigoAulaSisinfo, Long codOrganizacion, Persona personaResponsableAdministrador, String idAulaSisinfo,String cuatrimestre) throws Exception{
		try{
			ControlPlantillaPermiso ctrlPlantillaPermiso = new ControlPlantillaPermiso(this.persistencia);
			ControlHerramientasAula ctrlHerrs= new ControlHerramientasAula(persistencia);
			
			java.sql.Timestamp inicioCreacion = utils.Utils.hoySqlDate();
			String pathraizArchivos = "";
			
			Timestamp fechaInicioUso =  utils.Utils.hoySqlDate();			
			TipoAula tipoAulaCurso = (TipoAula)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoAula","nombre==\""+TIPO_AULAS.CURSO+"\"").elementAt(0);
			EstadoAula estadoAulaActivo = (EstadoAula)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.EstadoAula","descripcion ==\""+ESTADO_AULAS.ACTIVO+"\"").elementAt(0);
			
			Vector organizaciones = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Organizacion", "nombre", "codigoAulaSisinfo == \""+codOrganizacion+"\"",true);
			Organizacion organizacion = null;
			if(organizaciones!= null && organizaciones.size()>0)
			     organizacion = (Organizacion)organizaciones.elementAt(0);
					
			PlantillaPermiso  plantillaPermisos = organizacion.getPlantillaPermisosAulas();
			
			ConfiguracionGrupo configuracionGrupo = new ConfiguracionGrupo();
			configuracionGrupo.setConfiguracionGralHerramientas(new Boolean(true));
			configuracionGrupo.setObservacion("Configuración inicial");
			configuracionGrupo.setConfiguracionGralAtributoCupo(new Boolean(true));
			configuracionGrupo.setAtributoGralCupo(new Integer(9));
			configuracionGrupo.setConfiguracionGralAtributoMultigrupo(new Boolean(true));
			configuracionGrupo.setAtributoGralMultigrupo(new Boolean(true));
			configuracionGrupo.setConfiguracionGralAtributoProhibido(new Boolean(true));
			configuracionGrupo.setAtributoGralProhibido(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupo.setConfiguracionGralAtributoRechazado(new Boolean(true));
			configuracionGrupo.setAtributoGralRechazado(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupo.setConfiguracionGralAtributoCurioso(new Boolean(true));
			configuracionGrupo.setAtributoGralCurioso(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupo.setConfiguracionGralAtributoTipo(new Boolean(true));
			configuracionGrupo.setAtributoGralTipo(TIPO_GRUPOS.PUBLICO);
			persistencia.hacerPersistente(configuracionGrupo);

			ConfiguracionGrupo configuracionGrupoBorr = new ConfiguracionGrupo();
			configuracionGrupoBorr.setConfiguracionGralHerramientas(new Boolean(true));
			configuracionGrupoBorr.setObservacion("Configuración inicial borrador");
			configuracionGrupoBorr.setConfiguracionGralAtributoCupo(new Boolean(true));
			configuracionGrupoBorr.setAtributoGralCupo(new Integer(9));
			configuracionGrupoBorr.setConfiguracionGralAtributoMultigrupo(new Boolean(true));
			configuracionGrupoBorr.setAtributoGralMultigrupo(new Boolean(true));
			configuracionGrupoBorr.setConfiguracionGralAtributoProhibido(new Boolean(true));
			configuracionGrupoBorr.setAtributoGralProhibido(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupoBorr.setConfiguracionGralAtributoRechazado(new Boolean(true));
			configuracionGrupoBorr.setAtributoGralRechazado(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupoBorr.setConfiguracionGralAtributoCurioso(new Boolean(true));
			configuracionGrupoBorr.setAtributoGralCurioso(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupoBorr.setConfiguracionGralAtributoTipo(new Boolean(true));
			configuracionGrupoBorr.setAtributoGralTipo(TIPO_GRUPOS.PUBLICO);
			persistencia.hacerPersistente(configuracionGrupoBorr);

			ConfiguracionGrupo configuracionGrupoTodos = new ConfiguracionGrupo();
			configuracionGrupoTodos = new ConfiguracionGrupo();
			configuracionGrupoTodos.setConfiguracionGralHerramientas(new Boolean(true));
			configuracionGrupoTodos.setConfiguracionGralAtributoCupo(new Boolean(true));
			configuracionGrupoTodos.setConfiguracionGralAtributoMultigrupo(new Boolean(true));
			configuracionGrupoTodos.setConfiguracionGralAtributoProhibido(new Boolean(true));
			configuracionGrupoTodos.setConfiguracionGralAtributoRechazado(new Boolean(true));
			configuracionGrupoTodos.setConfiguracionGralAtributoCurioso(new Boolean(true));
			configuracionGrupoTodos.setConfiguracionGralAtributoTipo(new Boolean(true));
			persistencia.hacerPersistente(configuracionGrupoTodos);

			ConfiguracionGrupo configuracionGrupoBorradorTodos = new ConfiguracionGrupo();
			configuracionGrupoBorradorTodos = new ConfiguracionGrupo();
			configuracionGrupoBorradorTodos.setConfiguracionGralHerramientas(new Boolean(true));
			configuracionGrupoBorradorTodos.setConfiguracionGralAtributoCupo(new Boolean(true));
			configuracionGrupoBorradorTodos.setConfiguracionGralAtributoMultigrupo(new Boolean(true));
			configuracionGrupoBorradorTodos.setConfiguracionGralAtributoProhibido(new Boolean(true));
			configuracionGrupoBorradorTodos.setConfiguracionGralAtributoRechazado(new Boolean(true));
			configuracionGrupoBorradorTodos.setConfiguracionGralAtributoCurioso(new Boolean(true));
			configuracionGrupoBorradorTodos.setConfiguracionGralAtributoTipo(new Boolean(true));
			persistencia.hacerPersistente(configuracionGrupoBorradorTodos);

			InscripcionOnLine inscripcionOnLine = new InscripcionOnLine();
			inscripcionOnLine.setEstado(ESTADO_INSCRIPCION_ONLINE.INACTIVA); 
			persistencia.hacerPersistente(inscripcionOnLine);
			
			//crea el material de la carrera
			Material materialCurso = new Material();
			persistencia.hacerPersistente(materialCurso);
			
			//crea el calendario de la carrera
			Calendario calendarioCurso = new Calendario();
			persistencia.hacerPersistente(calendarioCurso);
			
			//crea las Novedades de la carrera
			Novedad novedadCurso = new Novedad();
			persistencia.hacerPersistente(novedadCurso);
			
			//crea el Pizarron de la carrera
			Pizarron pizarronCurso = new Pizarron();
			persistencia.hacerPersistente(pizarronCurso);
			
			// crea las FAQ de la carrera
			FAQ faqCurso = new FAQ();	
			persistencia.hacerPersistente(faqCurso);
			
			
			Curso curso = new Curso();
			curso.setAulaCompuesta(organizacion);  
			curso.setVersionSiat(DATOS_GENERALES.VERSION_SISTEMA);
			curso.setTipoAula(tipoAulaCurso);
			curso.setNombre(nombreCurso);
			curso.setFechaCreacion(utils.Utils.hoySqlDate());
			curso.setEstadoAula(estadoAulaActivo);
			curso.setAnioPlanEstudio(0);
			curso.setEspacioFisicoDisponible(new Float(CONSTANTES_ENTERAS.CUPO_ESPACIO_FISICO_AULA));//Limite espacio fisico disponible (300MB)
			curso.setFechaInicioUso(fechaInicioUso); 
			curso.setConfiguracionGrupo(configuracionGrupo);
			curso.setConfiguracionGrupoBorrador(configuracionGrupoBorr);
			curso.setConfiguracionGrupoTodos(configuracionGrupoTodos);
			curso.setConfiguracionGrupoBorradorTodos(configuracionGrupoBorradorTodos);
			curso.setInscripcionOnline(inscripcionOnLine);
			curso.setMaterial(materialCurso);		
			curso.setCalendario(calendarioCurso);
			curso.setNovedad(novedadCurso);
			curso.setPizarron(pizarronCurso);
			curso.setFaq(faqCurso);
			curso.setIdAulaSisinfo(idAulaSisinfo);
			curso.setPeriodo(cuatrimestre);
			curso.setCodigoAulaSisinfo(codigoAulaSisinfo);
			if(plantillaPermisos!=null)	curso.setPlantillaPermiso(plantillaPermisos);
			persistencia.hacerPersistente(curso);
		
			curso.setCodigo(String.valueOf(curso.getId()));	
			materialCurso.setNombre(curso.getId().toString());
					
			//Tpos de usuario 
			TipoUsuario tipoUsuarioWM = (TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.WEBMASTER+"\" ").elementAt(0);
			TipoUsuario tipoUsuarioAlumno =(TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.ALUMNO+"\" ").elementAt(0);
			TipoUsuario tipoUsuarioResponsable =(TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.RESPONSABLE+"\" ").elementAt(0);
		    //TipoUsuario tipoUsuarioAdmin = (TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.ADMINISTRADOR+"\" ").elementAt(0);
				
			Vector participantesDeAula =  new Vector();
			
			/////////////////////////////////WEBMASTER///////////////////////////////////////////////////////////
			Vector webMasters = obtenerPersonasWMDeCampus();
			for(int i=0;i<webMasters.size();i++){
				try {
					Persona personaWebMaster = (Persona)webMasters.elementAt(i);
					Participante participanteWebMaster = new Participante();
					participanteWebMaster.setPersona(personaWebMaster);   
					participanteWebMaster.setTipoUsuario(tipoUsuarioWM);
					participanteWebMaster.setAula(curso);
					participanteWebMaster.setFechaAlta(utils.Utils.hoySqlDate());
					participanteWebMaster.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
					persistencia.hacerPersistente(participanteWebMaster);
					participantesDeAula.add(participanteWebMaster);		
				}catch(Exception e) {
					LOGGERCONTROL.log(Level.SEVERE, "Exception creando participante WB en el aula.");
				}
			}		

			Vector partReplicar = obtenerParticipantesAReplicar(organizacion.getId());
			for(int j=0;j<partReplicar.size();j++){
				try {
					Participante participRep = (Participante)partReplicar.elementAt(j);
					Participante participante = new Participante();
					participante.setPersona(participRep.getPersona());   
					participante.setTipoUsuario(participRep.getTipoUsuario());
					participante.setAula(curso);
					participante.setFechaAlta(utils.Utils.hoySqlDate());
					participante.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
					persistencia.hacerPersistente(participante);
					participantesDeAula.add(participante);
				}catch(Exception e) {
					LOGGERCONTROL.log(Level.SEVERE, "Creando participante a replicar en el aula.");
				}
			}
			
			///////////////////////////////// ADMINISTRADOR ///////////////////////////////////////////////////////////			
			//crea el administrador del curso, esta persona sera cargada como Responsable-Administrador
			/*Participante participanteAdministrador = new Participante();
			Vector participantes = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+curso.getId()+" && persona.id=="+personaResponsableAdministrador.getId()+" && tipoUsuario.id=="+tipoUsuarioAdmin.getId());
			if(participantes.size()==0){
				participanteAdministrador.setPersona(personaResponsableAdministrador);
				participanteAdministrador.setTipoUsuario(tipoUsuarioAdmin);
				participanteAdministrador.setAula(curso);
				participanteAdministrador.setFechaAlta(utils.Utils.hoySqlDate());
				participanteAdministrador.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				persistencia.hacerPersistente(participanteAdministrador);
			}else{
				participanteAdministrador=(Participante)participantes.elementAt(0);
			}	*/
			
		    //Vector participantesOrganizacion = controlAula.getParticipantes(curso.getAulaCompuesta().getId(),personaResponsableAdministrador.getId());
			Vector participantesOrganizacion = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+curso.getAulaCompuesta().getId()+" && persona.id=="+personaResponsableAdministrador.getId()+" && tipoUsuario.id=="+tipoUsuarioAlumno.getId());
			
			if(participantesOrganizacion.size()==0){
				//Debemos asociar a esta persona en la organizacion
				Participante participanteOrganizacion = new Participante();
				participanteOrganizacion.setAula(curso.getAulaCompuesta());
				participanteOrganizacion.setPersona(personaResponsableAdministrador);
				participanteOrganizacion.setTipoUsuario(tipoUsuarioAlumno);
				participanteOrganizacion.setFechaAlta(utils.Utils.hoySqlDate());
				participanteOrganizacion.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				persistencia.hacerPersistente(participanteOrganizacion);
			}
			
			//////////////////////////// RESPONSABLE ///////////////////////////////////////////////////////////
			Participante participanteResponsable = new Participante();
			Vector participantesResp = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+curso.getId()+" && persona.id=="+personaResponsableAdministrador.getId()+" && tipoUsuario.id=="+tipoUsuarioResponsable.getId()); 
			
			if(participantesResp.size()==0){
				participanteResponsable.setPersona(personaResponsableAdministrador);
				participanteResponsable.setTipoUsuario(tipoUsuarioResponsable);
				participanteResponsable.setAula(curso);
				participanteResponsable.setFechaAlta(utils.Utils.hoySqlDate());
				participanteResponsable.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
				persistencia.hacerPersistente(participanteResponsable);
			}else{
				participanteResponsable=(Participante)participantesResp.elementAt(0);
			}
		
			///////////////////////////////////////////
			crearPermisoAcceso(ctrlPlantillaPermiso,plantillaPermisos.getId(),curso,"aula");
		    
		    Vector tiposDeRecursos =  persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoRecurso","orden,nombre", "creacionAulaSisinfo=="+true);
			TipoRecurso tipoRecurso = null;
			for(int s=0;s<tiposDeRecursos.size();s++) {
				tipoRecurso = (TipoRecurso) tiposDeRecursos.elementAt(s);
				ctrlHerrs.crearAulaHerramienta(curso, tipoRecurso);				
			}			
			pathraizArchivos = server.Singleton.HOME_DIR_ARCHIVOS+File.separator+PARAMETROS_PAGINAS.ID_AULA+curso.getId();
			
			//creo la carpeta donde se almacenaran los archivos del curso
			File directorioCurso = new File(pathraizArchivos);
			boolean mkdir = directorioCurso.mkdirs();
			
			if(!mkdir) new ExceptionArchivos("No se pudo crear la carpeta para el curso", this.getClass().getSimpleName()+".java", "crearAulaAutomatica");
			pathraizArchivos += File.separator;

			File directorioMaterialCurso = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.MATERIALES);
			mkdir = directorioMaterialCurso.mkdirs();			
			//creo la carpeta para el curso dentro de la carpeta actividades
			File directorioActividadesCurso = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.ACTIVIDADES);
			mkdir = directorioActividadesCurso.mkdirs();				
			File directorioSoftwareCurso = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.SOFTWARE);
			mkdir = directorioSoftwareCurso.mkdirs();
			//crea la carperta para los materiales adicionales de las comisiones del curso
			File directorioMaterialAdicionalCurso = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.MATERIALES_ADICIONALES);
			mkdir = directorioMaterialAdicionalCurso.mkdirs();
			File directorioNoticias = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.NOTICIAS);
			mkdir = directorioNoticias.mkdirs();
			File directorioPizarron = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.PIZARRON);
			mkdir = directorioPizarron.mkdirs();
			File directorioFAQ = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.FAQ);
			mkdir = directorioFAQ.mkdirs();
			//creo la carpeta para almacenar los archivos de los foros del curso
			File directorioForo = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.FORO);
			mkdir = directorioForo.mkdirs();
			
			java.sql.Timestamp finCreacion = utils.Utils.hoySqlDate();
			//------------Para Monitorear creación de Aulas automaticas----------------------
			String rutaArchivo = "";
			String contenidoArchivo = "";
			try {					
				contenidoArchivo += "---------------------Creacion del Aula Automatica: "+nombreCurso+"---------------------\n";
				contenidoArchivo += "Fecha y Hora Inicio de la Creacion: "+inicioCreacion+"\n";
				contenidoArchivo += "Organizacion del Aula: "+organizacion.getNombre()+"\n";
				contenidoArchivo += "Responsable del Aula: "+personaResponsableAdministrador.getNombreYApellido()+" - "+personaResponsableAdministrador.getNumeroDoc()+"\n";
				contenidoArchivo += "Fecha y Hora Fin de la Creacion: "+finCreacion+"\n";
				contenidoArchivo += "-----------------------------------------------------------------------------------------------------------\n\n";
				String hoy = +Utils.dia(Utils.hoySqlDate())+"-"+Utils.mesEnTexto(Utils.hoySqlDate())+"-"+Utils.anio(Utils.hoySqlDate());
				rutaArchivo = Singleton.HOME_DIR_LOGS+"//creacionAulas//"+"aulas_automaticas_creadas_"+hoy+".log";
				ManejoArchivoDeTexto.creacionYCargaArchivoTexto(rutaArchivo, contenidoArchivo,true);   
			} catch (Exception ee) {
				LOGGERCONTROL.log(Level.SEVERE, " El sistema no puede encontrar la ruta especificada :"+rutaArchivo);
			}		
			
			return curso;
		
		} catch (Exception ee) {	
			LOGGERCONTROL.log(Level.SEVERE, "Exception creaAulaDatosMinimos "+ee.getMessage());
			Utils.generarExcepcion(ee,this.getClass().getSimpleName()+".java","creaAulaDatosMinimos");
			return null;
		}
	}
			
	/*
	 * Completa el aula con las comisiones y el equipo docente
	 * @param gson,aulaSisinfo, aulaDTO
	 */
	public int completarAulaConComisioensyEquipoDocente(Gson gson,Curso aulaSisinfo,AulaDTO aulaDTO,AulaLog log) throws Exception{
		int cargOK = 0;
		try{
			String codOrganizacion = aulaSisinfo.getAulaCompuesta().getCodigo();
			ComisionDTO[] comisiones = aulaDTO.getComisiones();
			PersonaDTO[] equipoDocente = aulaDTO.getEquipo_docente();
						
			TipoUsuario tUDocente =(TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.DOCENTE+"\" ").elementAt(0);
			TipoUsuario tUResponsable =(TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.RESPONSABLE+"\" ").elementAt(0);
			TipoUsuario tUAlumno =(TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.ALUMNO+"\" ").elementAt(0);
			TipoUsuario tUWM = (TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.WEBMASTER+"\" ").elementAt(0);
			TipoUsuario tUColaborador = (TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.COLABORADOR+"\" ").elementAt(0);
			//TipoUsuario tUAdmin = (TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.ADMINISTRADOR+"\" ").elementAt(0);
						
			PlantillaPermiso  pantillaPermisoComision =  aulaSisinfo.getPlantillaPermiso();				
		
			/*Vector participantesAdminAula = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && tipoUsuario.id=="+tipoUsuarioAdmin.getId()); 
			Long idAdministrador = null;
			if(participantesAdminAula.size()>0) {
				Participante participanteAdministrador = (Participante)participantesAdminAula.elementAt(0);
				idAdministrador = participanteAdministrador.getId();
			}	*/
			
			Vector participantesRespAula = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && tipoUsuario.id=="+tUResponsable.getId()); 
			Long idResponsable = null;
			if(participantesRespAula.size()>0) {
				Participante participanteResponsable = (Participante)participantesRespAula.elementAt(0);
				idResponsable = participanteResponsable.getId();
			}	
							
			ControlPlantillaPermiso ctrlPlantillaPermiso = new ControlPlantillaPermiso(this.persistencia);
			ControlConfigurarPermisosAula ctrlConfigurarPermisosAula = new ControlConfigurarPermisosAula(this.persistencia);
			String pathraizArchivos = server.Singleton.HOME_DIR_ARCHIVOS+File.separator+PARAMETROS_PAGINAS.ID_AULA+aulaSisinfo.getId();
			TipoAula tipoAulaCurso = (TipoAula)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoAula","nombre==\""+TIPO_AULAS.CURSO+"\"").elementAt(0);
					
			Organizacion organizacion = (Organizacion)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Organizacion","codigo==\""+codOrganizacion+"\"").elementAt(0);
						
			///////////////////////////
			// CARGA EQUIPO DOCENTE //
			//////////////////////////
			ControlAula controlAula = new ControlAula(persistencia);
			PersonaDTO equipoDocenteAulaJSON = null;
			Persona personaEquipoDocente = null;
								
			Vector participantesOrganizacion = new Vector();
			log.setInfoaula("Equipo docente: \n");
			for (int l=0;l<equipoDocente.length;l++) {	
				try {
					equipoDocenteAulaJSON =  equipoDocente[l];
					try {
						personaEquipoDocente = (Persona)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc ==\""+ManejoString.eliminarPuntosDelDocumento(equipoDocenteAulaJSON.getNumero_documento())+"\"").elementAt(0);
					}catch(Exception ex) {}
					if(personaEquipoDocente!=null ) {
						participantesOrganizacion = controlAula.getParticipantes(aulaSisinfo.getAulaCompuesta().getId(),personaEquipoDocente.getId());
						if(participantesOrganizacion.isEmpty()){
							//Debemos asociar a esta persona en la organizacion
							Participante participanteOrganizacion = new Participante();
							participanteOrganizacion.setAula(aulaSisinfo.getAulaCompuesta());
							participanteOrganizacion.setPersona(personaEquipoDocente);
							participanteOrganizacion.setTipoUsuario(tUAlumno);
							participanteOrganizacion.setFechaAlta(utils.Utils.hoySqlDate());
							participanteOrganizacion.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
							persistencia.hacerPersistente(participanteOrganizacion);			
						}
						//Creamos el objeto Participante de la Persona para el Aula que se esta creando y se lo asociamos con el objeto Persona
						Participante participante = new Participante();
						participante.setPersona(personaEquipoDocente);						
						if(equipoDocenteAulaJSON.getRol().compareTo(Util.AyudanteAlumno)==0) participante.setTipoUsuario(tUColaborador);
						else participante.setTipoUsuario(tUDocente);						
						participante.setEstadoParticipante(null);
						participante.setAula(aulaSisinfo);
						participante.setFechaAlta(utils.Utils.hoySqlDate());
						participante.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
						persistencia.hacerPersistente(participante);
						log.setInfoaula(log.getInfoaula()+personaEquipoDocente.getNombre()+" "+personaEquipoDocente.getApellido()+" - "+personaEquipoDocente.getNumeroDoc()+"\n");
					}						
				}catch(Exception e){
					LOGGERCONTROL.log(Level.SEVERE, "EXCEPTION EQUIPO DOCENTE");						
				}
			}// for equipoDocente.size()					

			///////////////////////////////////////////////////////////
			// COMIENZO del ciclo para la creacion de las comisiones //
			///////////////////////////////////////////////////////////
			Comision comision = null;
								
			Vector participantesDeAula = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && tipoUsuario.id=="+tUWM.getId());
			Vector partReplicar = obtenerParticipantesAReplicar(organizacion.getId());
			for(int j=0;j<partReplicar.size();j++){
				Participante participRep = (Participante)partReplicar.elementAt(j);
				participantesDeAula.add(participRep);
			}					
													
			//Participante participanteAdministrador = (Participante)persistencia.getObjectoPorId("persistencia.dominio.Participante",idAdministrador);				
			Participante participanteResponsable = (Participante)persistencia.getObjectoPorId("persistencia.dominio.Participante",idResponsable);
			Vector participantesDocentes = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && tipoUsuario.id=="+tUDocente.getId());
			
			if (comisiones != null && comisiones.length>0) {	
				for (int i = 0; i < comisiones.length; i++) {
					try {						
						ComisionDTO comisionJSON =  comisiones[i];
						log.setInfoaula(log.getInfoaula()+"\nComision: "+comisionJSON.getNombre()+"\n");
						
						String nombreComision = comisionJSON.getNombre();							
						comision = creaComision(log,aulaSisinfo,comisiones.length ,nombreComision,tipoAulaCurso,pathraizArchivos,participantesDeAula,/*participanteAdministrador,*/participanteResponsable,null);
						if(comision!=null) {
							cargOK = 1;	
															
							//Asigna estudiantes a cada comision
							PersonaDTO[] estudientes = comisionJSON.getEstudiantes();	
							this.cargarEstudiantesAUnaComision(aulaSisinfo,estudientes,log, comision,controlAula,/*tUAdmin,*/ tUResponsable, tUAlumno, tUDocente,tUColaborador);
							
							//Asigna docentes a cada comision
							PersonaDTO[] docentes = comisionJSON.getDocentes();	
							this.cargarDocentesAUnaComsiion(aulaSisinfo, docentes, log, tUDocente,tUColaborador, comision, controlAula);									
							
							//Asigno permisos a nivel comision
							crearPermisoAcceso(ctrlPlantillaPermiso,pantillaPermisoComision.getId(),comision,"comision");
						}else
							LOGGERCONTROL.log(Level.SEVERE, " No se pudo crear la comisión.");
						
					}catch(Exception e){
						LOGGERCONTROL.log(Level.SEVERE, " EXCEPTION CARGA COMISION"+e.getMessage());
					}
				}//for comisiones.size()
								
				if(comisiones.length>0)						
					AgregaHerramComision(ctrlConfigurarPermisosAula,aulaSisinfo);		
								
			}else {
				cargOK = 1;
				//CREAO UNA UNICA COMISION//////////////////////////////////////////////////////////////////////////////////////////////
				try {					
					String nombreComision = aulaSisinfo.getNombre();
					log.setInfoaula(log.getInfoaula()+"\nComision unica: "+nombreComision+"\n");
						
					comision = creaComision(log,aulaSisinfo,0 ,nombreComision,tipoAulaCurso,pathraizArchivos,participantesDeAula,/*participanteAdministrador,*/participanteResponsable,participantesDocentes); 
					if(comision!=null) {
						cargOK = 1;											
						//Asigno permisos a nivel comision
						crearPermisoAcceso(ctrlPlantillaPermiso,pantillaPermisoComision.getId(),comision,"comision");						
						//Agregar herramientas a las comsiones
						AgregaHerramComision(ctrlConfigurarPermisosAula,aulaSisinfo);						
					}else
						LOGGERCONTROL.log(Level.SEVERE, " No se pudo crear la comisión.");
					
				}catch(Exception e){
					LOGGERCONTROL.log(Level.SEVERE, " EXCEPTION CARGA COMISION UNICA"+e.getMessage());
				}				
			}
			return cargOK;					
		} catch (Exception ee) {
			LOGGERCONTROL.log(Level.SEVERE,"Exception completarAulaConComisioensyEquipoDocente");
			return 0;			
		}		
	}
	
	private void AgregaHerramComision(ControlConfigurarPermisosAula ctrlConfigurarPermisosAula,Curso aulaSisinfo)throws Exception {
		AulaCompuesta aulaCompuesta = (AulaCompuesta)persistencia.getObjectoPorId("persistencia.dominio.Aula",aulaSisinfo.getId());
		Object[] arrayComs =  aulaCompuesta.getAulas().toArray();
		Vector tiposDeRecursos =  persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoRecurso","orden,nombre", "creacionAulaSisinfo=="+true);
		
		TipoRecurso tipoRecurso = null;
		for(int s=0;s<tiposDeRecursos.size();s++) {
			tipoRecurso = (TipoRecurso) tiposDeRecursos.elementAt(s);
			ctrlConfigurarPermisosAula.agregarHerramientaAulas(arrayComs, tipoRecurso,TIPO_AULAS.COMISION);	
		}			
	}
	
	private Comision creaComision(AulaLog informeAula,Curso aulaSisinfo, int nroComisiones, String nombreComision, TipoAula tipoAulaCurso,String pathraizArchivos,Vector participantesDeAula,/*Participante participanteAdministrador,*/Participante participanteResponsable,Vector participantesDocentes) {
		try {
			ConfiguracionGrupo configuracionGrupo2 = new ConfiguracionGrupo();
			configuracionGrupo2.setConfiguracionGralHerramientas(new Boolean(true));
			configuracionGrupo2.setObservacion("Configuración inicial");
			configuracionGrupo2.setConfiguracionGralAtributoCupo(new Boolean(true));
			configuracionGrupo2.setAtributoGralCupo(new Integer(9));
			configuracionGrupo2.setConfiguracionGralAtributoMultigrupo(new Boolean(true));
			configuracionGrupo2.setAtributoGralMultigrupo(new Boolean(true));
			configuracionGrupo2.setConfiguracionGralAtributoProhibido(new Boolean(true));
			configuracionGrupo2.setAtributoGralProhibido(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupo2.setConfiguracionGralAtributoRechazado(new Boolean(true));
			configuracionGrupo2.setAtributoGralRechazado(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupo2.setConfiguracionGralAtributoCurioso(new Boolean(true));
			configuracionGrupo2.setAtributoGralCurioso(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupo2.setConfiguracionGralAtributoTipo(new Boolean(true));
			configuracionGrupo2.setAtributoGralTipo(TIPO_GRUPOS.PUBLICO);
			persistencia.hacerPersistente(configuracionGrupo2);
			
			ConfiguracionGrupo configuracionGrupoBorr2 = new ConfiguracionGrupo();
			configuracionGrupoBorr2.setConfiguracionGralHerramientas(new Boolean(true));
			configuracionGrupoBorr2.setObservacion("Configuración inicial borrador");
			configuracionGrupoBorr2.setConfiguracionGralAtributoCupo(new Boolean(true));
			configuracionGrupoBorr2.setAtributoGralCupo(new Integer(9));
			configuracionGrupoBorr2.setConfiguracionGralAtributoMultigrupo(new Boolean(true));
			configuracionGrupoBorr2.setAtributoGralMultigrupo(new Boolean(true));
			configuracionGrupoBorr2.setConfiguracionGralAtributoProhibido(new Boolean(true));
			configuracionGrupoBorr2.setAtributoGralProhibido(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupoBorr2.setConfiguracionGralAtributoRechazado(new Boolean(true));
			configuracionGrupoBorr2.setAtributoGralRechazado(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupoBorr2.setConfiguracionGralAtributoCurioso(new Boolean(true));
			configuracionGrupoBorr2.setAtributoGralCurioso(AUTORIZACION_GRUPO.LIDER_Y_PARTICIPANTES);
			configuracionGrupoBorr2.setConfiguracionGralAtributoTipo(new Boolean(true));
			configuracionGrupoBorr2.setAtributoGralTipo(TIPO_GRUPOS.PUBLICO);
			persistencia.hacerPersistente(configuracionGrupoBorr2);
	
			return this.crearDatosBasicosComision(informeAula,aulaSisinfo,nroComisiones,nombreComision,tipoAulaCurso,pathraizArchivos,participantesDeAula,/*participanteAdministrador,*/participanteResponsable,configuracionGrupo2,configuracionGrupoBorr2,participantesDocentes);
		}catch(Exception ex){
			LOGGERCONTROL.log(Level.SEVERE,"Exception creaComision");
			return null;			
		}
		
	}
	
	/*
	 * Registra los docentes en una comision.
	 * El arreglo de PersonaDTO solo tiene cargados los DNI, el docente que se registre en una comision tiene que estar previamente registrado en Evelia y en el aula
	 * de la comision.
	 * */
	public boolean cargarDocentesAUnaComsiion(AulaCompuesta aulaSisinfo, PersonaDTO[] docentes,AulaLog log,TipoUsuario tUDocente,TipoUsuario tUDocenteColaborador,Comision comision,ControlAula controlAula) throws Exception {
		boolean cargoTodoOK = true;
		PersonaDTO docenteJSON = null;
		Persona personaDocente = null;
		log.setInfoaula(log.getInfoaula()+"Docentes: \n");
		for(int j=0;j<docentes.length;j++){
			try {
				docenteJSON =  docentes[j];
				String numeroDoc = ManejoString.eliminarPuntosDelDocumento(docenteJSON.getNumero_documento());	
			    Vector personas = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","numeroDoc==\""+numeroDoc+"\"");
				if(personas!=null & personas.size()>0) {
					//Docente registrado en Evelia
					personaDocente = (Persona)personas.elementAt(0);
					if(personaDocente!=null) {
						Vector participantes = persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Participante", "aula.id=="+aulaSisinfo.getId()+" && persona.id=="+personaDocente.getId()+" && (tipoUsuario.id=="+tUDocente.getId() +" || tipoUsuario.id=="+tUDocenteColaborador.getId()+" )" );
						if(participantes!=null & participantes.size()>0){
							//Docente participante del aula
							Participante participante = (Participante)participantes.get(0);
			    			if(participante!=null){									    				
			    				Vector participantesComision = controlAula.getParticipantesComisionConTipoUsuario(comision.getId(),personaDocente.getId(),tUDocente.getNombre());
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
			    				String error = "El docente "+personaDocente.getNumeroDoc()+", que intenta registrar en la comisión, no es docente del aula.";
			    				LOGGERCONTROL.log(Level.SEVERE,error);
			    				log.setInfoerrores(log.getInfoerrores()+error+"<br>");
			    			}
						}else {
							String error = "El docente "+personaDocente.getNumeroDoc()+", que intenta registrar en la comisión, no es docente del aula.";
		    				LOGGERCONTROL.log(Level.SEVERE,error);
		    				log.setInfoerrores(log.getInfoerrores()+error+"<br>");
						}
					}else {
						String error = "El docente "+numeroDoc+", que intenta registrar en la comisión, no esta registrado en Evelia.";
						LOGGERCONTROL.log(Level.SEVERE,error);
	    				log.setInfoerrores(log.getInfoerrores()+error+"<br>");
					}
				}else {
					String error = "El docente "+numeroDoc+", que intenta registrar en la comisión, no esta registrado en Evelia.";
    				LOGGERCONTROL.log(Level.SEVERE,error);
    				log.setInfoerrores(log.getInfoerrores()+error+"<br>");
				}
			}catch(Exception ex){
				LOGGERCONTROL.log(Level.SEVERE,"CARGA DOCENTES EN COMISIONES" );
			}
		}//for	
		return cargoTodoOK;
	}
	
	//Obtiene todos los usuarios webMaster del Campus
	public Vector obtenerPersonasWMDeCampus() throws Exception{
		try{
			String filtro = " participantes.contains(participante)";
			filtro +=" && participante.aula.nombre==\""+TIPO_AULAS.CAMPUS+"\" && participante.tipoUsuario.nombre==\""+TIPO_USUARIOS.WEBMASTER+"\"";
			String vars="Participante participante";
			return this.persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Persona","apellido",filtro,vars);			
		} catch (Exception ee) {
			Utils.generarExcepcion(ee,this.getClass().getSimpleName()+".java","obtenerPersonasWMDeCampus");
			return new Vector();
		}
	}
	
	
	//Modifica los datos personales de un usuario y registra informacion en los log.
	public boolean ModificarDatosPersonales(PersonaDTO usuario,Persona personal) {
		try {
			if((usuario.getNombre()!=null && usuario.getNombre().compareTo(personal.getNombre())!=0 ) || (  usuario.getApellido()!=null && usuario.getApellido().compareTo(personal.getApellido())!=0)) {
				personal.setNombre(usuario.getNombre());
				personal.setApellido(usuario.getApellido());
				LOGGERCONTROL.log(Level.INFO, "Se cambiaron algunos datos personales (nombre y/o apellido) del usuario: "+personal.getNumeroDoc());
			}			
		}catch(Exception e) {
			LOGGERCONTROL.log(Level.SEVERE, "Exception cambiando datos personales. ");			
		}
		return true;
	}
			
	
	/*
	 * Crea una organización
	 * @param codigoOrganizacion
	 * @param nombreOrganizacion
	 */
	/*public AulaCompuesta crearOrganizacionParaAulaAutomatica(String codigoOrganizacion,String nombreOrganizacion) {
		try{
				String filtroTipoAula="nombre == \""+TIPO_AULAS.FACULTAD+"\"";			
				String filtro="tipoPlantilla==\"organizacion\" ";
				Vector plantillas = (Vector)persistencia.getObjectosPorClase("persistencia.dominio.PlantillaPermiso","nombre",filtro,"","",null,null,null);							
				PlantillaPermiso  pantillaPermisoOrg = (PlantillaPermiso) plantillas.elementAt(0);	
			
				TipoOrganizacion tipoOrg = (TipoOrganizacion)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoOrganizacion","nombre==\""+TIPO_AULAS.ORGANIZACION_GRADO+"\"").firstElement();
				TipoAula tipoAula= (TipoAula)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoAula",filtroTipoAula).firstElement();
				EstadoAula estadoAulaActivo = (EstadoAula)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.EstadoAula","descripcion ==\""+ESTADO_AULAS.ACTIVO+"\"").elementAt(0);
				PlantillaPermiso plantillaAula = (PlantillaPermiso)persistencia.getObjectoPorId("persistencia.dominio.PlantillaPermiso",pantillaPermisoOrg.getId());
			
				Organizacion  organizacion = new Organizacion();
				organizacion.setVersionSiat(DATOS_GENERALES.VERSION_SISTEMA);
				organizacion.setTipoAula(tipoAula);
				organizacion.setTipoOrganizacion(tipoOrg);
				organizacion.setNombre(nombreOrganizacion);
				organizacion.setFechaCreacion(utils.Utils.hoySqlDate());
				organizacion.setEstadoAula(estadoAulaActivo);
				organizacion.setPlantillaPermisosAulas(plantillaAula);
				organizacion.setEnviarMailsNotificacion(false);			
				organizacion.setCodigo(codigoOrganizacion);
				persistencia.hacerPersistente(organizacion);
				Organizacion campus = (Organizacion)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.Organizacion","nombre==\""+TIPO_AULAS.CAMPUS+"\" ").elementAt(0);
				organizacion.setAulaCompuesta(campus);
				
				ControlPlantillaPermiso ctrlPlantillaPermisos= new ControlPlantillaPermiso(this.persistencia);
				ctrlPlantillaPermisos.agregarPermisosDePlantilla_Org(pantillaPermisoOrg.getId(),organizacion);
				PlantillaPermiso plantillaOrg = (PlantillaPermiso)persistencia.getObjectoPorId("persistencia.dominio.PlantillaPermiso",pantillaPermisoOrg.getId());
				organizacion.setPlantillaPermiso(plantillaOrg); 

				TipoUsuario tipoUsuario=(TipoUsuario)persistencia.getObjectosPorClaseYFiltro("persistencia.dominio.TipoUsuario","nombre==\""+TIPO_USUARIOS.WEBMASTER+"\" ").elementAt(0);
				ControlCrearAula1 ctrlCrearAula= new ControlCrearAula1(persistencia);
				Vector webMasters = ctrlCrearAula.obtenerPersonasWMDeCampus();
				for(int i=0;i<webMasters.size();i++){
					Persona personaWebMaster = (Persona)webMasters.elementAt(i);
					Participante participanteWebMaster = new Participante();
					participanteWebMaster.setPersona(personaWebMaster);   
					participanteWebMaster.setTipoUsuario(tipoUsuario);
					participanteWebMaster.setAula(organizacion);
					participanteWebMaster.setFechaAlta(utils.Utils.hoySqlDate());
					participanteWebMaster.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
					persistencia.hacerPersistente(participanteWebMaster);
				}

				Vector partReplicar = ctrlCrearAula.obtenerParticipantesAReplicar(campus.getId());
				for(int j=0;j<partReplicar.size();j++){
					Participante participRep = (Participante)partReplicar.elementAt(j);
					Participante participante = new Participante();
					participante.setPersona(participRep.getPersona());   
					participante.setTipoUsuario(participRep.getTipoUsuario());
					participante.setAula(organizacion);
					participante.setFechaAlta(utils.Utils.hoySqlDate());
					participante.setAgregadoDesde(USUARIO_AGREGADO_DESDE.CREACION_AULA_AUTOMATICA);
					persistencia.hacerPersistente(participante);
				}

				String pathraizArchivos = "";
				//crea el material de la carrera
				Material materialOrganizacion = new Material();
				pathraizArchivos = server.Singleton.HOME_DIR_ARCHIVOS+File.separator+PARAMETROS_PAGINAS.ID_AULA+organizacion.getId();
			
				//creo la carpeta donde se almacenaran los archivos del curso
				File directorioCurso = new File(pathraizArchivos);
				boolean mkdir = directorioCurso.mkdirs();
				
				pathraizArchivos += File.separator;
				
				File directorioMaterialCurso = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.MATERIALES);
				mkdir = directorioMaterialCurso.mkdirs();
				materialOrganizacion.setNombre(organizacion.getId().toString());
				persistencia.hacerPersistente(materialOrganizacion);
				organizacion.setMaterial(materialOrganizacion);

				//creo la carpeta para el curso dentro de la carpeta actividades
				File directorioActividadesCurso = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.ACTIVIDADES);
				mkdir = directorioActividadesCurso.mkdirs();

				//crea el software de la carrera
				Software softwareCurso = new Software();
				File directorioSoftwareCurso = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.SOFTWARE);
				mkdir = directorioSoftwareCurso.mkdirs();
				softwareCurso.setNombre(organizacion.getId().toString());
				persistencia.hacerPersistente(softwareCurso);
				organizacion.setSoftware(softwareCurso);
		
				//crea la carpeta para los materiales adicionales de las comisiones del curso
				File directorioMaterialAdicionalCurso = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.MATERIALES_ADICIONALES);
				mkdir = directorioMaterialAdicionalCurso.mkdirs();
			
				//crea el calendario de la carrera
				Calendario calendarioCurso = new Calendario();
				persistencia.hacerPersistente(calendarioCurso);
				organizacion.setCalendario(calendarioCurso);

				//crea las Novedades de la carrera
				Novedad novedadCurso = new Novedad();

				File directorioNoticias = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.NOTICIAS);
				mkdir = directorioNoticias.mkdirs();
				persistencia.hacerPersistente(novedadCurso);
				organizacion.setNovedad(novedadCurso);

				//crea el Pizarron de la carrera
				Pizarron pizarronCurso = new Pizarron();
				File directorioPizarron = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.PIZARRON);
				mkdir = directorioPizarron.mkdirs();
				persistencia.hacerPersistente(pizarronCurso);
				organizacion.setPizarron(pizarronCurso);

				// crea las FAQ de la carrera
				FAQ faqCurso = new FAQ();
				File directorioFAQ = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.FAQ);
				mkdir = directorioFAQ.mkdirs();
				persistencia.hacerPersistente(faqCurso);
				organizacion.setFaq(faqCurso);

				//creo la carpeta para almacenar los archivos de los foros del curso
				File directorioForo = new File(pathraizArchivos+CARPETAS_ARCHIVOS_RECURSO.FORO);
				mkdir = directorioForo.mkdirs();
				
				//Cargando herramientas x defecto.
				ControlHerramientasAula ctrlHerrams= new ControlHerramientasAula(persistencia);
				ctrlHerrams.crearHerramientasObligatorias(organizacion,TIPO_AULAS.FACULTAD);
				
				//crearPermisoAcceso(ctrlPlantillaPermisos,pantillaPermisoOrg.getId(),orga,null);
				
				
				LOGGERCONTROL.log(Level.INFO, "Se creo la organización correctamente");
				return organizacion;
			}catch(Exception ee){
				LOGGERCONTROL.log(Level.SEVERE, "No se creo la organización.");
				return null;
			}			
		}
		*/
	
	
	
}
