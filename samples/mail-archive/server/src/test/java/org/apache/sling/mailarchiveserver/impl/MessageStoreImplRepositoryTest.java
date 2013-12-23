package org.apache.sling.mailarchiveserver.impl;

import static org.apache.sling.mailarchiveserver.impl.MessageStoreImplRepositoryTestUtil.assertValueMap;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImplRepositoryTestUtil.getResourcePath;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImplRepositoryTestUtil.readTextFile;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImplRepositoryTestUtil.specialPathFromFilePath;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.discovery.impl.setup.MockedResourceResolver;
import org.apache.sling.mailarchiveserver.api.MboxParser;
import org.apache.sling.mailarchiveserver.util.MailArchiveServerConstants;
import org.apache.sling.mailarchiveserver.util.TU;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MessageStoreImplRepositoryTest {
	private ResourceResolver resolver;
	private Resource testRoot;
	private MessageStoreImpl store;

	static final String TEST_RT_KEY = "sling_resourceType";
	private static final String BODY_SUFFIX = "_body";
	private static final String HEADERS_SUFFIX = "_headers";

	private static final String SINGLEPART_FILE = "singlepart.txt";
	private static final String MULTIPART_FILE = "multipart.txt";
	private static final String WRONGBODY_FILE = "wrongbody.txt";
	private static final String MBOX_FILE = "three_messages.mbox";

	/**
	 * Some code is taken from http://svn.apache.org/repos/asf/sling/trunk/launchpad/test-services/src/main/java/org/apache/sling/launchpad/testservices/serversidetests/WriteableResourcesTest.java
	 */
	@Before
	public void setup() throws Exception {
		resolver = new MockedResourceResolver();
		assertNotNull("Expecting non-null ResourceResolver", resolver);
		final Resource root = resolver.getResource("/");
		assertNotNull("Expecting non-null root Resource", root);
		final String path = getClass().getSimpleName() + "_" + System.currentTimeMillis();
		testRoot = resolver.create(root, path, null);
		resolver.commit();

		store = new MessageStoreImpl() {
		    protected ResourceResolver getResourceResolver() {
		        return resolver;
		    }
		};
		store.threadKeyGen = new ThreadKeyGeneratorImpl();
		store.archivePath = testRoot.getPath() + "/";
		store.resourceTypeKey = TEST_RT_KEY;
	}

	@After
	public void cleanup() throws Exception {
		resolver.close();
		resolver = null;
		testRoot = null;
		store = null;
	}


	@Test
	public void testSaveMessage() throws FileNotFoundException, MimeException, IOException  {
		assertSaveMessage(SINGLEPART_FILE);
		assertSaveMessage(MULTIPART_FILE);
		assertSaveMessage(WRONGBODY_FILE);
	}

	@Test
	public void testStructure() throws IOException {
		MboxParser parser = new Mime4jMboxParserImpl();
		final File file = new File(TU.TEST_FOLDER, MBOX_FILE);
		store.saveAll(parser.parse(new FileInputStream(file)));
		assertStructure();
	}

	private void assertSaveMessage(String messageFile) throws MimeException, IOException, FileNotFoundException {
		MessageBuilder builder = new DefaultMessageBuilder();
		Message msg = builder.parseMessage(new FileInputStream(new File(TU.TEST_FOLDER, messageFile)));

		store.save(msg);

		final Resource r = resolver.getResource(getResourcePath(msg, store));
		assertNotNull("Expecting non-null Resource", r);
		final ModifiableValueMap m = r.adaptTo(ModifiableValueMap.class);

		File bodyFile = new File(TU.TEST_FOLDER, specialPathFromFilePath(messageFile, BODY_SUFFIX));
		if (bodyFile.exists()) {
			String expectedBody = readTextFile(bodyFile);
			assertValueMap(m, "Body", expectedBody);
		}

		File headersFile = new File(TU.TEST_FOLDER, specialPathFromFilePath(messageFile, HEADERS_SUFFIX));
		if (headersFile.exists()) {
			MessageStoreImplRepositoryTestUtil.assertHeaders(headersFile, m);
		}

		assertTrue(headersFile.exists() || bodyFile.exists()); // test at least something 
	}

	private void assertStructure() {
		List<String> types = new ArrayList<String>();
		types.add(MailArchiveServerConstants.DOMAIN_RT);
		types.add(MailArchiveServerConstants.LIST_RT);
		types.add(null);
		types.add(null);
		types.add(MailArchiveServerConstants.THREAD_RT);
		types.add(MailArchiveServerConstants.MESSAGE_RT);

		MessageStoreImplRepositoryTestUtil.assertLayer(testRoot, types, 0);
	}

}
