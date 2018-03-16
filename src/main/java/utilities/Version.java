/**
 * 
 */
package utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * @author Jan Hoinka
 * Contains the current version string and comparators for AptaSuite
 */
public class Version {

	public static Integer mayor = 0;
	public static Integer minor = 0;
	public static Integer patch = 1;
	
	// Get the information from the pom.properties files
	// maven automatically packs into the jar file
	static {
		
		InputStream file = Version.class.getClassLoader().getResourceAsStream("META-INF/maven/aptasuite/aptasuite/pom.properties");
		BufferedReader reader = new BufferedReader(new InputStreamReader(file));
		
		try {
			
            String line;
            while ((line = reader.readLine()) != null) {
                
            	if (line.startsWith("version")) {
            		
            		// get the version string 
            		String version = line.split("=")[1];
            		
            		// divide into mayor minor and patch 
            		String[] tokens = version.trim().split("\\.");
            		
            		mayor = Integer.parseInt(tokens[0]);
            		minor = Integer.parseInt(tokens[1]);
            		patch = Integer.parseInt(tokens[2]);
            		
            	}

            }
            
            AptaLogger.log(Level.INFO, Version.class, String.format("AptaSuite version read from file. Version is %s.%s.%s", mayor, minor, patch ));

        } catch (Exception e) {
        	
        	AptaLogger.log(Level.WARNING, Version.class, "Error reading version file.");
        	AptaLogger.log(Level.WARNING, Version.class, e);
        	
        } finally {
            try { reader.close(); file.close(); } catch (Throwable ignore) {}
        }
		
	}
	
	
	public static String versionString() {
		
		return String.format("%s.%s.%s", mayor, minor, patch);
		
	}
	
	/**
	 * Checks the release page of the GitHub repository for a newer version
	 */
	public static boolean newerVersionAvailable() {
		
		try {
		
		// if the local version could not be read to begin with, we return false
		if (mayor.intValue() == 0 && minor.intValue() == 0 && patch.intValue() == 1) { return false; }
		
		// try to get the json response for the latest release 
		URL url;
		try {
			url = new URL("https://api.github.com/repos/drivenbyentropy/aptasuite/releases/latest");
		} catch (MalformedURLException e1) {

			AptaLogger.log(Level.WARNING, Version.class, "Could not get JSON file from version request.");
        	AptaLogger.log(Level.CONFIG, Version.class, e1);
        	return false;
			
		}
		
		// now extract the tag name
		JSONTokener tokener = null;
		try {
			
			URLConnection con = url.openConnection();
			con.setConnectTimeout(10000);
			con.setReadTimeout(5000);
			InputStream in = con.getInputStream();
			
			tokener = new JSONTokener(in);
		} catch (IOException e) {

			AptaLogger.log(Level.WARNING, Version.class, "Error tokenizing JSON response from version request");
        	AptaLogger.log(Level.CONFIG, Version.class, e);
        	return false;
			
		}
		
		JSONObject obj = new JSONObject(tokener);
		String tag = obj.getString("tag_name");

		// now extract mayor minor and patch 
		String[] tokens = tag.trim().substring(1).split("\\.");
		
		int remote_mayor = Integer.parseInt(tokens[0]);
		int remote_minor = Integer.parseInt(tokens[1]);
		int remote_patch = Integer.parseInt(tokens[2]);
		
		// we have a new version if at least one of the remote versions is larger than the local one
		if ( remote_mayor > mayor.intValue() ) return true;
		if ( remote_mayor < mayor.intValue() ) return false;
		
		if ( remote_minor > minor.intValue() ) return true; 
		if ( remote_minor < minor.intValue() ) return false;
		
		if ( remote_patch > patch.intValue() ) return true;
		if ( remote_patch < patch.intValue() ) return false;
		
		}
		catch (Exception e) {
			
			AptaLogger.log(Level.WARNING, Version.class, "Error checking for newer version");
        	AptaLogger.log(Level.CONFIG, Version.class, e);
			
        	return false;
        	
		}
		
		return false;
		
	}
}
