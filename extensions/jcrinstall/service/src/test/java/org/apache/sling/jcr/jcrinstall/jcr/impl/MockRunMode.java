package org.apache.sling.jcr.jcrinstall.jcr.impl;

import org.apache.sling.runmode.RunMode;

class MockRunMode implements RunMode {

    private final String[] modes;
    
    MockRunMode(String [] modes) {
        this.modes = modes;
    }
    
    public String[] getCurrentRunModes() {
        return modes;
    }

    public boolean isActive(String[] runModes) {
        boolean result = false;
        
        main:
        for(String a : runModes) {
            for(String b : modes) {
                if(b.equals(a)) {
                    result = true;
                    break main;
                }
            }
        }
        
        return result;
    }

}
