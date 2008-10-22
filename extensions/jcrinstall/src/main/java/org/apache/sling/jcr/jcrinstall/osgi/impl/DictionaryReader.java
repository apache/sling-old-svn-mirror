package org.apache.sling.jcr.jcrinstall.osgi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/** Reads a Dictionary from an InputStream, using the
 *  syntax of the Properties class, enhanced to support
 *  multivalued properties.
 */
public class DictionaryReader {
    public static final String ARRAY_MARKER = "[]";
    
    /** Read Dictionary from the given InputStream,
     *  which is *not* closed before returning
     */
    public static Dictionary<?,?> load(InputStream is) throws IOException {
        final Properties p = new Properties();
        p.load(is);
        return convert(p);
    }
    
    /** Convert Properties to Dictionary. Properties having
     *  a name that ends with [] are assumed to be comma-separated
     *  lists of values, and are converted to an Array. The []
     *  is removed from the property name. 
     */
    public static Dictionary<?,?> convert(Properties p) {
        final Hashtable <String, Object> result = new Hashtable<String, Object>();
        
        for(Map.Entry<Object, Object> e : p.entrySet()) {
            final String key = (String)e.getKey();
            if(key.trim().endsWith(ARRAY_MARKER)) {
                final String newKey = key.substring(0, key.length() - ARRAY_MARKER.length()).trim();
                result.put(newKey, convertValue((String)e.getValue()));
            } else {
                result.put(key, e.getValue());
            }
        }
        
        return result;
    }
    
    /** Convert value to a String[], trimming all values */
    public static String [] convertValue(String value) {
        
        if(value.trim().length() == 0) {
            return new String[0];
        }
        
        return splitWithEscapes(value, ',');
    }
    
    /** Split string, ignoring separators that directly follow a backslash.
     *  All values are trimmed
     */
    public static String [] splitWithEscapes(String str, char separator) {
        final ArrayList<String> a = new ArrayList<String>();
        StringBuffer current = new StringBuffer();
        char lastChar = 0;
        for(int i=0; i < str.length(); i++) {
            final char c = str.charAt(i);
            
            if(c == separator) {
                if(lastChar == '\\') {
                    // replace lastchar with c
                    current.setCharAt(current.length() - 1, c);
                } else if(current.length() > 0) {
                    a.add(current.toString());
                    current = new StringBuffer();
                }
            } else {
                current.append(c);
            }
            
            lastChar = c;
        }
        if(current.length() > 0) {
            a.add(current.toString());
        }
        
        final String [] result = new String[a.size()];
        int i=0;
        for(String s : a) {
            result[i++] = s.trim();
        }
        return result;
    }
}
