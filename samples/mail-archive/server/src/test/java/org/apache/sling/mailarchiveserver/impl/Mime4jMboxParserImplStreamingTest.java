package org.apache.sling.mailarchiveserver.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.sling.mailarchiveserver.impl.Mime4jMboxParserImpl.Mime4jParserIterator;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In this class there is a test that parses big file. It will take a while to execute.
 */
public class Mime4jMboxParserImplStreamingTest {

	private Mime4jMboxParserImpl parser = new Mime4jMboxParserImpl();
	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final String TEST_FOLDER = Mime4jMboxParserImplCountTest.TEST_FOLDER;
	private static final String TEST_FILE_NAME = TEST_FOLDER  + "mbox/tomcat-dev-201204.mbox";
	private static final double TEST_FILE_RATIO = 1.2;

	@Test
	public void testParserIsStreaming() throws IOException {
		File tempf = null;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
            final long maxMem = Runtime.getRuntime().maxMemory();
		    log.info("Max memory={}, reading from {}, might take a while...", maxMem, TEST_FILE_NAME);
		    log.info("TODO: this test fails with 'Size exceeds Integer.MAX_VALUE' if maxMem is over a certain limit");
			final File fileToSample = new File(TEST_FILE_NAME);
			final int count = (int) (maxMem * TEST_FILE_RATIO / fileToSample.length()) + 1;

			fis = new FileInputStream(fileToSample);
			final byte[] sample = new byte[(int) fileToSample.length()];
			assertEquals("Expecting the correct number of bytes read", fis.read(sample), fileToSample.length()); 

			tempf = File.createTempFile("MAS_", ".mbox");
			fos = new FileOutputStream(tempf);
			for (int i = 0; i < count; i++) {
				fos.write(sample);
			}
            fos.flush();
            fos.close();
			fos = null;

			parser.parse(new FileInputStream(tempf));

		} catch(OutOfMemoryError e) {
			fail("Got OutOfMemoryError, looks like the Parser is not streaming");
		} finally {
			if (tempf != null) {
				tempf.delete();
			}
			if (fis != null) {
				fis.close();
			}
			if (fos != null) {
				fos.close();
			}
		}
	}

	@Test
	public void testTempFileIsDeleted() throws IOException {
		File testFile = new File(TEST_FOLDER + "mbox/tomcat-dev-201204.mbox");
		Mime4jParserIterator iter = (Mime4jParserIterator) parser.parse(new FileInputStream(testFile));
		assertFalse("Expecting temp file to be deleted", new File(iter.tempFileAbsPath).exists());
	}	

}
