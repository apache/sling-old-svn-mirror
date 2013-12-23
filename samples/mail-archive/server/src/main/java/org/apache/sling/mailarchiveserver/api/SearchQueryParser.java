package org.apache.sling.mailarchiveserver.api;

import java.util.List;
import java.util.Map;

public interface SearchQueryParser {
    
	Map<String, List<String>> parse(String query);
	
}
