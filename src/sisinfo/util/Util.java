package sisinfo.util;

public class Util{  
 
 
  public static String convert(String in) {
	  return in.replace("&amp;","&")   
	           .replace("&nbsp;"," ")   
	           .replace("&lt;","<")   
	           .replace("&gt;",">")   
	           .replace("&ntilde;","ñ")   
	           .replace("&Ntilde;","Ñ")   
	           .replace("&aacute;","á")   
	           .replace("&eacute;","é")   
	           .replace("&iacute;","í")   
	           .replace("&oacute;","ó")   
	           .replace("&uacute;","ú")   
	           .replace("&iquest;","¿")   
	           .replace("&iexcl;","¡")   
	           .replace("&quot;","\"")   
	           .replace("&#039;","'");   
	 }
  
  //Rol que viene desde sisinfo y es equivalente a Docente Colaborador en Evelia
  public static String AyudanteAlumno = "Ayudante Alumno";
  
  
}
