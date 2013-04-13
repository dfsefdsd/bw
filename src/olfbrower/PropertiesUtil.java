package olfbrower;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;


public final class PropertiesUtil {
	
	private PropertiesUtil(){}
	
	private static Properties props = new Properties();
	
	static{

		try {
			props.load(new FileInputStream(getFile()));
			check(props);
		} catch (Exception e) {
			e.printStackTrace();
			check(props);
		}
	}
	
	
	public static String getProperty(String key) {
		return props.getProperty(key);
	}	
	public static void setProperty(String key,String value) {
		 props.setProperty(key,value);
	}	
	/**
	 * 检查合法性
	 * @param props
	 */
	private static void check(Properties props){
	/*	if(!props.containsKey("index")){
			props.put("index", "http://113.105.93.30:8080/personalfocus");
		}
		if(!props.containsKey("userId")){
			props.put("userId","");
		}
		if(!props.containsKey("password")){
			props.put("password","");
		}
		if(!props.containsKey("flushTime")){
			props.put("flushTime","5");
		}
		if(!props.containsKey("checkTime")){
			props.put("checkTime","5");
		}
		if(!props.containsKey("base")){
			props.put("base","http://113.105.93.30:8080/personalfocus");
		}
		if(!props.containsKey("autoRecognize")){
			props.put("autoRecognize","true");
		}*/
	}
	
	public static boolean save(){
		try {
			props.store(new FileOutputStream(getFile()), "互联网关注平台专用浏览器配置");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private static File getFile(){
		File file=new File("conf/config.properties");
		if(!file.exists()){
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return file;
	}
}


