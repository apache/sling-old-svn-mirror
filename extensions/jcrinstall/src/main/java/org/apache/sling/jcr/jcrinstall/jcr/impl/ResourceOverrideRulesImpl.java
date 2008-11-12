package org.apache.sling.jcr.jcrinstall.jcr.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.jcr.jcrinstall.osgi.ResourceOverrideRules;

/** Simple path-based ResourceOverrideRules */
class ResourceOverrideRulesImpl implements ResourceOverrideRules {

    private final String [] mainPaths; 
    private final String [] overridePaths;
    
    private static final String SLASH = "/";
    
    ResourceOverrideRulesImpl(String [] mainPaths, String [] overridePaths) {
        if(mainPaths == null || overridePaths == null) {
            throw new IllegalArgumentException("Null mainPaths or overridePaths");
        }
        if(mainPaths.length != overridePaths.length) {
            throw new IllegalArgumentException("mainPaths and overridePaths are not the same size");
        }
        
        this.mainPaths = mainPaths;
        this.overridePaths = overridePaths;
    }
                                 
    public String[] getHigherPriorityResources(String uri) {
        return map(uri, mainPaths, overridePaths);
    }

    public String[] getLowerPriorityResources(String uri) {
        return map(uri, overridePaths, mainPaths);
    }
    
    private String [] map(String uri, String [] from, String [] to) {
        boolean addedSlash = false;
        if(!uri.startsWith(SLASH)) {
            uri = SLASH + uri;
            addedSlash = true;
        }
        
        List<String> mapped = null;
        for(int i=0; i < from.length; i++) {
            if(uri.startsWith(from[i])) {
                if(mapped == null) {
                    mapped = new ArrayList<String>();
                }
                String str = to[i] + uri.substring(from[i].length());
                if(addedSlash) {
                    str = str.substring(1);
                }
                mapped.add(str);
            }
        }
        
        if(mapped == null) {
            return new String[0];
        } else {
            final String [] result = new String[mapped.size()];
            int i=0;
            for(String str : mapped) {
                result[i++] = str;
            }
            return result;
        }
    }
}
