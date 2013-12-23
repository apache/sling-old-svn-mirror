package org.apache.sling.mailarchiveserver.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.mboxiterator.CharBufferWrapper;
import org.apache.james.mime4j.mboxiterator.MboxIterator;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.sling.mailarchiveserver.api.MboxParser;
import org.apache.sling.mailarchiveserver.util.MailArchiveServerConstants;

@Component
@Service(MboxParser.class)
public class Mime4jMboxParserImpl implements MboxParser {

	@Override
	public Iterator<Message> parse(InputStream is) throws IOException {
		return new Mime4jParserIterator(is);
	}

	static class Mime4jParserIterator implements Iterator<Message> {

		private Iterator<CharBufferWrapper> mboxIterator;
		private static final int BUFFER_SIZE = 10*1024*1024;
		String tempFileAbsPath = null;

		public Mime4jParserIterator(InputStream is) throws IOException {
			File tempFile = null;
			FileOutputStream fileOS = null;
			try {
				// create temp file
				tempFile = File.createTempFile("MAS_", ".mbox");
				tempFileAbsPath = tempFile.getAbsolutePath();
				fileOS = new FileOutputStream(tempFile);
				FileChannel fileChannel = fileOS.getChannel();
				byte[] buffer = new byte[BUFFER_SIZE];
				int read = 0;
				while ((read = is.read(buffer)) != -1) {
					ByteBuffer buf2 = MailArchiveServerConstants.DEFAULT_ENCODER.encode(CharBuffer.wrap(new String(buffer, 0, read)));
					fileChannel.write(buf2);
				}
				fileChannel.close();


				createMboxIterator(tempFile);
			} finally {
				if (tempFile.exists()) {
					tempFile.delete();
					tempFile = null;
				}
				if (fileOS != null) {
					fileOS.close();
					fileOS = null;
				}
				if (is != null) {
					is.close();
					is = null;
				}
			}
		}

		private void createMboxIterator(File f) throws FileNotFoundException, IOException {
			mboxIterator = MboxIterator.fromFile(f).charset(MailArchiveServerConstants.DEFAULT_ENCODER.charset()).build().iterator();
		}

		public boolean hasNext() {
			return mboxIterator.hasNext();
		}

		public Message next() {
			MessageBuilder builder = new DefaultMessageBuilder();
			Message message = null;
			try {
				message = builder.parseMessage(new ByteArrayInputStream(mboxIterator.next().toString().getBytes(MailArchiveServerConstants.DEFAULT_ENCODER.charset().name())));
			} catch (MimeException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return message;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
