package sisinfo.util;

public class Util{  
 
 
  public static String convert(String in) {
	  return in.replace("&amp;","&")   
	           .replace("&nbsp;"," ")   
	           .replace("&lt;","<")   
	           .replace("&gt;",">")   
	           .replace("&ntilde;","�")   
	           .replace("&Ntilde;","�")   
	           .replace("&aacute;","�")   
	           .replace("&eacute;","�")   
	           .replace("&iacute;","�")   
	           .replace("&oacute;","�")   
	           .replace("&uacute;","�")   
	           .replace("&iquest;","�")   
	           .replace("&iexcl;","�")   
	           .replace("&quot;","\"")   
	           .replace("&#039;","'");   
	 }
  
  //Rol que viene desde sisinfo y es equivalente a Docente Colaborador en Evelia
  public static String AyudanteAlumno = "Ayudante Alumno";
  
  
}
