package org.apache.sling.mailarchiveserver.impl;

import static org.apache.sling.mailarchiveserver.impl.MessageStoreImplRepositoryTestUtil.readTextFile;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImpl.*;
import static org.apache.sling.mailarchiveserver.impl.MessageStoreImplRepositoryTestUtil.specialPathFromFilePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.message.BodyPart;
import org.junit.Test;

/**
 * In this class there is a test that parses big file. It will take a while to execute.
 */
public class Mime4jMboxParserImplTest {

    private Mime4jMboxParserImpl parser = new Mime4jMboxParserImpl();

    private static final String TEST_FOLDER = Mime4jMboxParserImplCountTest.TEST_FOLDER;
    private static final String WRONGBODY_MBOX = "wrongbody.mbox";

    @Test
    public void testMboxParsing() throws IOException {
        final String testPath = TEST_FOLDER + WRONGBODY_MBOX;
        Iterator<Message> iter = parser.parse(new FileInputStream(new File(testPath)));

        boolean fail = true;
        int i = 1;
        while (iter.hasNext()) {
            final Message message = iter.next();
            File bodyFile = new File(specialPathFromFilePath(testPath, "_bodyOf" + i, "txt"));
            if (bodyFile.exists()) {
                final String actual = getPlainBody(message);
                final String expected = readTextFile(bodyFile);
                assertEquals("Body #"+i, expected, actual);
                fail = false;
            }
            i++;
        }

        if (fail) {
            fail("No file with expected body.");
        }
    }

    /**
     *        code taken from http://www.mozgoweb.com/posts/how-to-parse-mime-message-using-mime4j-library/
     */
    private static String getPlainBody(Message msg) throws IOException {
        if (!msg.isMultipart()) {
            return getTextPart(msg);
        } else {
            Multipart multipart = (Multipart) msg.getBody();
            for (Entity enitiy : multipart.getBodyParts()) {
                BodyPart part = (BodyPart) enitiy;
                if (part.isMimeType("text/plain")) {
                    return getTextPart(part);
                }
            }
        }

        return null;
    }

}
