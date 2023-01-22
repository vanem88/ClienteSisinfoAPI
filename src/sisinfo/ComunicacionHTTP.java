package sisinfo;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class ComunicacionHTTP {
	
	private static final Logger LOGGER = Logger.getLogger( ComunicacionHTTP.class.getName() );

	public static String HTTPGet(String url,String usuario, String contrasena, Timestamp fechaNovedadesDesde, Timestamp fechaNovedadesHasta) throws Exception{				
		CloseableHttpClient httpClient = HttpClients.createDefault();					
	    try {	    	
	    	HttpGet request  = new HttpGet(url);
	    	
	    	RequestConfig requestConfig = RequestConfig.custom()
	    	        .setConnectTimeout(300000).setConnectionRequestTimeout(60000) //300.000 = 5 min
	    	        .setSocketTimeout(300000).build();
	    	request.setConfig(requestConfig);
	    	
	       	String auth = usuario + ":" + contrasena;
	        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
	        String authHeader = "Basic " + new String(encodedAuth);
	        request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
	        
	        SimpleDateFormat formato = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");	
	        if(fechaNovedadesDesde!=null & fechaNovedadesHasta != null) {	        		 
	        	URI uri = new URIBuilder(request.getURI())
			    		  .addParameter("fecha_desde",formato.format(fechaNovedadesDesde))
				          .addParameter("fecha_hasta", formato.format(fechaNovedadesHasta))
				          .build();
				       ((HttpRequestBase) request).setURI(uri);
	        }else if(fechaNovedadesDesde!=null) {
	        	URI uri = new URIBuilder(request.getURI())
			    		  .addParameter("fecha_desde",formato.format(fechaNovedadesDesde))
				          .build();
				       ((HttpRequestBase) request).setURI(uri);
	        }       
	        
	        LOGGER.log(Level.INFO, request.toString());
			
	        HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
	        if (statusCode != 200) {
	            throw new RuntimeException("Failed with HTTP error code : " + statusCode);	          
	        }
	                 
	        HttpEntity httpEntity = response.getEntity();
	        String apiOutput = EntityUtils.toString(httpEntity, StandardCharsets.UTF_8);
	        
	        return apiOutput;
	    }catch(Exception ex) {
	    	throw new Exception("Exception HTTPGet. "+ex.getMessage());
	    }finally{
	    	httpClient.close();	    
		}
	}
		 
	public static void HTTPPost(String url,String usuario, String contrasena,StringEntity body) throws Exception {
	    CloseableHttpClient httpClient = HttpClients.createDefault();			    
	    try{		    	
	    	HttpPost request = new HttpPost(url);
	    	
	    	//Armo el header para autenticar con usuario y contraseña
	        String auth = usuario + ":" + contrasena;
	        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
	        String authHeader = "Basic " + new String(encodedAuth);
	        request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);	       
	        request.setEntity(body);	        
	        request.setHeader("Content-type", "application/json");
	        
	        RequestConfig requestConfig = RequestConfig.custom()
	    	        .setConnectTimeout(300000).setConnectionRequestTimeout(60000)
	    	        .setSocketTimeout(300000).build();
	    	request.setConfig(requestConfig);
	             
	        HttpResponse response = httpClient.execute(request);
	        int statusCode = response.getStatusLine().getStatusCode();	   
	        if (statusCode != 200){
	            throw new RuntimeException("Failed with HTTP error code : " + statusCode);
	        }
	        
			//HttpEntity entity = response.getEntity();
			//String result = EntityUtils.toString(entity);			
			//LOGGER.log(Level.INFO, "Respuesta de la API: "+result);
	    }catch(Exception ex) {
	    	throw new Exception("Exception HTTPPost. "+ex.getMessage());					
	    }finally{
	        httpClient.close();	        
	    }
	}

}
