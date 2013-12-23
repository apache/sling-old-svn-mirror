package org.apache.sling.mailarchiveserver.impl;

import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.getDomainNodeName;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.getListNodeName;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.makeJcrFriendly;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.removeRe;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.Map;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.sling.mailarchiveserver.util.MailArchiveServerConstants;
import org.junit.Test;

public class MessageStoreImplStaticMethodsTest {
	
	@Test
	public void testMakeJcrFriendly() {
		assertEquals("Remove each char", "", makeJcrFriendly("��!@#$%^&*()+={}[]<>,/?\\;:'\""));
		assertEquals("Substitute each char with _ char, trimming", "a", makeJcrFriendly(".a_")); 
		assertEquals("Substitute each char with _ char", "b_e", makeJcrFriendly("b_ .-e"));
	}

	@Test
	public void testRemoveRe() {
		assertEquals(removeRe("abc"), "abc");
		assertEquals(removeRe("Re:re"), "re");
		assertEquals(removeRe("RE: abc"), "abc");
		assertEquals(removeRe("re: RE: "), "");
		assertEquals(removeRe(" re:  abc  "), "abc");
		assertEquals(removeRe(" re:fw:  aw:RE: FW: subj "), "subj");
		assertEquals(removeRe(""), "");
		assertEquals(removeRe("     "), "");
		assertEquals(removeRe("Re:   "), "");
	}
	
	@Test
	public void testNodeNamesFromListId() {
		assertEquals(getListNodeName("dev.sling.apache.org"), "dev.sling");
		assertEquals(getDomainNodeName("dev.sling.apache.org"), "apache.org");
		assertEquals(getListNodeName("proj.apache.org"), "proj");
		assertEquals(getDomainNodeName("proj.apache.org"), "apache.org");
		assertEquals(getListNodeName("a.b.c.apache.org"), "a.b.c");
		assertEquals(getDomainNodeName("a.b.c.apache.org"), "apache.org");
	}
	
}
