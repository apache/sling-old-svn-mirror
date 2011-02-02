package org.apache.sling.junit.scriptable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

/** Output stream for our fake HTTP response class */
public class TestServletOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    
    @Override
    public String toString() {
        return bos.toString();
    }
    
    @Override
    public void write(int b) throws IOException {
        bos.write(b);
    }
}
