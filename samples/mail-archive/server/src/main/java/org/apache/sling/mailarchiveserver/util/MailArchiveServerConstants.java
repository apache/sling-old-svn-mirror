package org.apache.sling.mailarchiveserver.util;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class MailArchiveServerConstants {
	
	public static final String ARCHIVE_PATH = "/content/mailarchiveserver/archive/";
	
	//RT = ResourceType
	public static final String RT_KEY = "sling:resourceType";
	public static final String DOMAIN_RT = "mailarchiveserver/domain";
	public static final String LIST_RT = "mailarchiveserver/list";
	public static final String THREAD_RT = "mailarchiveserver/thread";
	public static final String MESSAGE_RT = "mailarchiveserver/message";

	public static final CharsetEncoder DEFAULT_ENCODER = Charset.forName("UTF-8").newEncoder();
}
