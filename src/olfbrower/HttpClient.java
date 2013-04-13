package olfbrower;
import java.io.BufferedReader;  
import java.io.IOException;  
import java.io.InputStream;  
import java.io.InputStreamReader;  
import java.io.OutputStream;  
import java.net.HttpURLConnection;  
import java.net.MalformedURLException;  
import java.net.URL;  
import java.net.URLConnection;  
import java.util.HashMap;  
import java.util.Iterator;  
import java.util.Map;  
import java.util.Map.Entry;  
  
/** 
 * HttpClient 
 *  
 * @author Tang Ren email: <a 
 *         href="mailto:tangren1206@163.com">tangren1206@163.com</a> 
 *  
 * <a href="mailto:post@@http://www.51etest.com/dede/login.php@@userid=xxxx&amp;pwd=xxxxxx&amp;gotopage=/dede/&amp;dopost=login">post@@http://www.51etest.com/dede/login.php@@userid=xxxx&pwd=xxxxxx&gotopage=/dede/&dopost=login 
</a> *  
 * post@@http://www.51etest.com/dede/ad_add.php@@dopost=save&tagname=test&typeid=0&adname=test&starttime=2008-05-29 
 * 20:58:25&endtime=2008-06-28 20:58:25×et=0&normbody=test&expbody=test 
 *  
 */  
  
/** 
 *  
 */  
public class HttpClient {  
  
    private static final String USER_AGENT_VALUE = "Mozilla/4.0 (compatible; MSIE 6.0; Windows XP)";  
  
  
    /** 
     * Post parameter splitor default is symbol '&'. 
     */  
    private static final String POST_PARAMETER_SPLITOR = "&";  
  
    private static final String POST_PARAMETER_KV_SPLITOR = "=";  
  
    private String cookie = null;  

  
    public HttpClient() {  
      
  
    }  
  
    /** 
     * Request specifid url with 'GET' method. And return HTTP response content. 
     *  
     * @param url 
     * @return 
     */  
    public String getMethod(String url, boolean keepCookie) { 
    	
        if (url == null || url.length() == 0) {  
            return "Requst url could not be null or empty.";  
        }  
  
        StringBuffer result = new StringBuffer();  
        try {  
            HttpURLConnection httpURLConnection = this.getHttpURLConnection(  
                    url, keepCookie);  
  
            // Set request properties.  
            this.settingHttpRequestHeader(httpURLConnection);  
  
            httpURLConnection.setRequestMethod("GET");  
  
            // Getting or setting cookie  
            this.gettingOrSettingCookie(httpURLConnection, keepCookie);  
  
            InputStream httpInputStream = httpURLConnection.getInputStream();  
            BufferedReader httpBufferedReader = new BufferedReader(  
                    new InputStreamReader(httpInputStream, "UTF-8"));  
            result.append(this.readBufferedContent(httpBufferedReader));  
  
            // Connect to host.  
            httpURLConnection.connect();  
        } catch (IOException e) {  
            e.printStackTrace();  
            return "getHttpURLConnection failed.";  
        }  
        System.out.println(url+"回应："+result.toString());
        return result.toString();  
    }  
  
    public String postMethod(String url, Map parameters, boolean keepCookie) {  
        StringBuffer httpResponse = new StringBuffer();  
  
        HttpURLConnection httpURLConnection = null;  
        OutputStream httpOutputStream = null;  
        try {  
            httpURLConnection = this.getHttpURLConnection(url, keepCookie);  
            // Set request properties.  
            this.settingHttpRequestHeader(httpURLConnection);  
  
            // Set request method with 'POST'  
            httpURLConnection.setRequestMethod("POST");  
  
            // Set connection output is true.  
            httpURLConnection.setDoOutput(true);  
            // Getting or setting cookie  
            this.gettingOrSettingCookie(httpURLConnection, keepCookie);  
            // Get Http output stream  
            httpOutputStream = httpURLConnection.getOutputStream();  
  
            // Build post parameters string  
            StringBuffer postParams = new StringBuffer();  
            int index = 0;  
            for (Iterator<Entry> iter = parameters.entrySet().iterator(); iter  
                    .hasNext(); index++) {  
                Entry<String, String> entry = iter.next();  
                postParams.append(index != 0 ? "&" : "");  
                postParams.append(entry.getKey());  
                postParams.append("=");  
                postParams.append(entry.getValue());  
            }  
            httpOutputStream.write(postParams.toString().getBytes());  
  
            BufferedReader httpBufferedReader = new BufferedReader(  
                    new InputStreamReader(httpURLConnection.getInputStream()));  
            httpResponse.append(this.readBufferedContent(httpBufferedReader));  
        } catch (IOException e) {  
            e.printStackTrace();  
            return null;  
        } finally {  
            try {  
                httpOutputStream.close();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
        System.out.println(url+"  POST回应："+httpResponse.toString());
        return httpResponse.toString();  
    }  
  
    /** 
     * Setting HTTP request header properties 
     *  
     * @param httpURLConnection 
     */  
    private void settingHttpRequestHeader(HttpURLConnection httpURLConnection) {  
        if (httpURLConnection == null)  
            return;  
        httpURLConnection.setRequestProperty("User-Agent", USER_AGENT_VALUE);  
        // TODO setting some other properties here . . .  
    }  
  
    /** 
     * Get HttpURLConnection by specified url string. 
     *  
     * @param url 
     * @return 
     * @throws IOException 
     */  
    private HttpURLConnection getHttpURLConnection(String url,  
            boolean keepCookie) throws IOException {  
        URL urlObj = new URL(url);  
        URLConnection urlConnection = urlObj.openConnection();  
        if (urlConnection instanceof HttpURLConnection)  
            return (HttpURLConnection) urlConnection;  
        throw new MalformedURLException();  
    }  
  
    /** 
     * Read bufferedReader buffered content. 
     *  
     * @param bufferedReader 
     * @return 
     */  
    private String readBufferedContent(BufferedReader bufferedReader) {  
        if (bufferedReader == null)  
            return null;  
        StringBuffer result = new StringBuffer();  
        String line = null;  
        try {  
            while ((line = bufferedReader.readLine()) != null) {  
                result.append(line);  
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
            return null;  
        }  
        return result.toString();  
    }  
  
    /** 
     * Parse and create parameter map with parameter string 
     *  
     * @param parameterString 
     * @return 
     */  
    private Map parsePostParameters(String parameterString) {  
        if (parameterString == null || parameterString.length() == 0)  
            return null;  
        Map result = new HashMap<String, String>();  
  
        // only one parameter key-value pair  
        if (parameterString.indexOf(POST_PARAMETER_SPLITOR) == -1) {  
            if (parameterString.indexOf(POST_PARAMETER_KV_SPLITOR) != -1)  
                result.put(parameterString.split(POST_PARAMETER_KV_SPLITOR)[0],  
                        parameterString.split(POST_PARAMETER_KV_SPLITOR)[1]);  
        } else {  
            String[] keyValues = parameterString.split(POST_PARAMETER_SPLITOR);  
            for (int i = 0; i < keyValues.length; i++) {  
                String keyValue = keyValues[i];  
                result.put(keyValue.split(POST_PARAMETER_KV_SPLITOR)[0],  
                        keyValue.split(POST_PARAMETER_KV_SPLITOR)[1]);  
            }  
        }  
        return result;  
    }  
  
    /** 
     * Get or set cookie. 
     *  
     * @param httpURLConnection 
     * @param keepCookie 
     */  
    private void gettingOrSettingCookie(HttpURLConnection httpURLConnection,  
            boolean keepCookie) {  
        // Getting or setting cookie.  
        if (cookie == null || cookie.length() == 0) {  
            String setCookie = httpURLConnection.getHeaderField("Set-Cookie");  

            cookie = setCookie.substring(0, setCookie.indexOf(";"));
            System.out.println("cookie:"+cookie);
        } else if (keepCookie) {  
            httpURLConnection.setRequestProperty("Cookie", cookie);  
        }  
    }  
}  
