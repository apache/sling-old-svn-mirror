package org.apache.sling.launchpad.testservices.exported;

public enum TestEnum {
    FOO,
    BAR;
    
    public static TestEnum parse(String str) {
        if(str.contains("foo")) {
            return FOO;
        }
        return BAR;
    }
}
