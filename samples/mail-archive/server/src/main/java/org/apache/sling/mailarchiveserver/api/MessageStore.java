package org.apache.sling.mailarchiveserver.api;

import java.io.IOException;
import java.util.Iterator;

import org.apache.james.mime4j.dom.Message;

public interface MessageStore {
	
	void save(Message m) throws IOException;
	
	void saveAll(Iterator<Message> iterator) throws IOException;
	
}
