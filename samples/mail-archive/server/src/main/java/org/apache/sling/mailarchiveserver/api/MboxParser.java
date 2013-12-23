package org.apache.sling.mailarchiveserver.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.james.mime4j.dom.Message;

public interface MboxParser {
	
	Iterator<Message> parse(InputStream is) throws IOException;
	
}
