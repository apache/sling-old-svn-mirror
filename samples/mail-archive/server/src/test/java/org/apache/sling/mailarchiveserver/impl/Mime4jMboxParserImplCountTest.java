package org.apache.sling.mailarchiveserver.impl;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.james.mime4j.dom.Message;
import org.apache.sling.mailarchiveserver.util.TU;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class Mime4jMboxParserImplCountTest {

	private static Mime4jMboxParserImpl parser = new Mime4jMboxParserImpl();
	private String filePath;
	private int expectedMessagesCount;
	
	@Parameters(name="{0}")
    public static Collection<Object[]> data() {
        List<Object[]> params = new ArrayList<Object[]>();
        params.add(new Object[] {"three_messages.mbox", 3} );
        params.add(new Object[] {"mbox/jackrabbit-dev-201201.mbox", 323} );
        params.add(new Object[] {"mbox/hadoop-common-dev-201202.mbox", 296} );
        params.add(new Object[] {"mbox/sling-dev-201203.mbox", 227} );
        params.add(new Object[] {"mbox/tomcat-dev-201204.mbox", 658} );
        return params;
    }
    
    public Mime4jMboxParserImplCountTest(String path, int count) {
    	filePath = path;
    	expectedMessagesCount = count;
    }

	@Test
	public void testParse() throws IOException {
		Iterator<Message> iter = parser.parse(new FileInputStream(new File(TU.TEST_FOLDER, filePath)));
		
		int cnt = 0;
		Set<Message> set = new HashSet<Message>();
		while (iter.hasNext()) {
			Message message = (Message) iter.next();
			cnt++;
			set.add(message);
		}
		assertEquals("Expecting correct number of messages parsed", expectedMessagesCount, cnt);
		assertEquals("Expecting all messages unique", expectedMessagesCount, set.size());
	}

}
