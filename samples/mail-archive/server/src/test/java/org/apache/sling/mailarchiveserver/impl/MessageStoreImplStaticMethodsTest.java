/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.mailarchiveserver.impl;

import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.getDomainNodeName;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.getListNodeName;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.makeJcrFriendly;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.removeRe;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MessageStoreImplStaticMethodsTest {
	
	@Test
	public void testMakeJcrFriendly() {
		assertEquals("Remove each char", "", makeJcrFriendly("\"\uFFFD\uFFFD!@#$%^&*()+={}[]<>,/?\\\\;:'\\\""));
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
