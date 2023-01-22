package sisinfo;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import comun.constantes.ESTADO_AULAS;
import persistencia.Persistencia;
import persistencia.dominio.AulaSisinfoPendiente;
import sisinfo.dto.AulaDTO;
import sisinfo.dto.InformeAulasDTO;
import sisinfo.util.Util;
import utils.Utils;

public final class AulasPendientesEIncompletas {
	
	private static final Logger LOGGERAulasPendientes = Logger.getLogger( AulasPendientesEIncompletas.class.getName() );
		
	// Registra en la tabla AULA_SISINFO_PENDIENTE el aula que no pudo ser creada con su correspondiente informe
	public static void RegistrarAulaSisinfoPendiente(AulaDTO aula,InformeAulasDTO informeCreacion, String informeCompletar, String estado) throws Exception {
		Persistencia persistenciaInforme  = new Persistencia();
		try {
			persistenciaInforme.crearTransaccion();			
			AulaSisinfoPendiente aulaSisinfoPendiente = null;
			try {
				aulaSisinfoPendiente = (AulaSisinfoPendiente)persistenciaInforme.getObjectosPorClaseYFiltro("persistencia.dominio.AulaSisinfoPendiente", "nombre", "idAulaSisinfo == \""+aula.getId_aula()+"\"",true).elementAt(0);
			}catch (Exception e) {}
			if(aulaSisinfoPendiente == null) {
				aulaSisinfoPendiente = new AulaSisinfoPendiente();
				aulaSisinfoPendiente.setIdAulaSisinfo(aula.getId_aula());
				aulaSisinfoPendiente.setCodigoOrganizacion(aula.getCodigo_organizacion());
				aulaSisinfoPendiente.setNombre(aula.getNombre());
				aulaSisinfoPendiente.setPeriodo(aula.getPeridodo());
				aulaSisinfoPendiente.setEstado(estado);
				aulaSisinfoPendiente.setCantidadIntentosCreacion(1);
				aulaSisinfoPendiente.setFechaIntentoCreacion(Utils.hoySqlDate());
				
				String informeX = utils.Utils.hoySqlDate().toString()+"<br>";
				if(estado.compareTo(ESTADO_AULAS.SIN_CREAR)==0) {
					Vector informes = informeCreacion.getInforme();					
					for(int p=0;p<informes.size();p++) 
						informeX += Util.convert((String)informes.elementAt(p))+"<br>";							
					aulaSisinfoPendiente.setInforme(informeX.getBytes());					
				}else {
					informeCompletar = informeX + Util.convert(informeCompletar);
					aulaSisinfoPendiente.setInforme(informeCompletar.getBytes());
				}
				persistenciaInforme.hacerPersistente(aulaSisinfoPendiente);	
			}else {
				//Intente crearla
				aulaSisinfoPendiente.setCantidadIntentosCreacion(aulaSisinfoPendiente.getCantidadIntentosCreacion()+1);
				aulaSisinfoPendiente.setEstado(estado);
				String informeNuevo = new String ();
				String informeViejo = new String (aulaSisinfoPendiente.getInforme());
				informeNuevo = utils.Utils.hoySqlDate().toString()+"<br>";
				if(estado.compareTo(ESTADO_AULAS.SIN_CREAR)==0) {
					Vector<?> informes = informeCreacion.getInforme();
					for(int p=0;p<informes.size();p++) 
						informeNuevo += Util.convert((String)informes.elementAt(p))+"<br>";										
				}else {					
					informeNuevo += Util.convert(informeCompletar);													
				}
				informeNuevo += "----------------------------------------------------------------------------------------------------------<br>";				
				informeNuevo += informeViejo;
				
				aulaSisinfoPendiente.setInforme(informeNuevo.getBytes());
				persistenciaInforme.hacerPersistente(aulaSisinfoPendiente);
			}										
			
			persistenciaInforme.commit();
		}catch (Exception e) {
			LOGGERAulasPendientes.log(Level.SEVERE, "Exception AulasPendientesEIncompletas.");
			persistenciaInforme.rollback();
			throw new Exception("EXCEPTION RegistrarAulaSisinfoPendiente. "+e.getMessage());	
		}				
	}
	
	//Elimino el aula de la tabla AULA_SISINFO_PENDIENTE porque ya se completo correctamente.
	public static void EliminarAulaSisinfoPendiente(AulaDTO aula) throws Exception {
		Persistencia persistenciaInforme  = new Persistencia();
		try {
			persistenciaInforme.crearTransaccion();				
			AulaSisinfoPendiente aulaSisinfoPendiente = null;
			try{
				aulaSisinfoPendiente = (AulaSisinfoPendiente)persistenciaInforme.getObjectosPorClaseYFiltro("persistencia.dominio.AulaSisinfoPendiente", "nombre", "idAulaSisinfo == \""+aula.getId_aula()+"\"",true).elementAt(0);
			}catch (Exception e) {}
			
			if(aulaSisinfoPendiente!=null) {
				persistenciaInforme.deletePersistente(aulaSisinfoPendiente);			
				persistenciaInforme.commit();
			}else
				persistenciaInforme.rollback();
		}catch (Exception e) {
			LOGGERAulasPendientes.log(Level.SEVERE, "Exception EliminarAulaSisinfoPendiente.");
			persistenciaInforme.rollback();
			throw new Exception("EXCEPTION EliminarAulaSisinfoPendiente. "+e.getMessage());	
		}				
	}
	

}
