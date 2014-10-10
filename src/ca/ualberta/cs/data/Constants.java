package ca.ualberta.cs.data;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Constants {
	
	private static final String BUNDLE_NAME = "ca.ualberta.cs.data.settings";
    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
   
    public static String getValue(String key){
          try{
                return RESOURCE_BUNDLE.getString(key);
          }catch(MissingResourceException mex){
                return "";
          }catch(Exception ex){
                return "Not Found";
          }
    }  
}
