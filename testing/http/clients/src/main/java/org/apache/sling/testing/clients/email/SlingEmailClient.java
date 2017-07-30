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
package org.apache.sling.testing.clients.email;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingClientConfig;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Accesses email stored by a mock SMTP server deployed to Sling 
 *
 * <p>Requires that the <tt>org.apache.sling.testing.email</tt> bundle is deployed.</p>
 */
public final class SlingEmailClient extends SlingClient {

	/**
	 * The well-known path under which the EmailServlet is deployed
	 */
	private static final String EMAIL_SERVLET_PATH = "/system/sling/testing/email";
	
	/**
	 * The well-known property name of the email body contents
	 */
	private static final String PN_CONTENT = "-Content-";
	
	
	private final ObjectMapper mapper = new ObjectMapper();

	public SlingEmailClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
		super(http, config);
	}
	
	/**
	 * Retrieves the actual bind port of the SMTP server
	 * 
	 * @return the port value
	 * @throws ClientException in case of any errors
	 */
	public int getBindPort() throws ClientException {
        try {
			SlingHttpResponse mockEmailConfig = doGet(EMAIL_SERVLET_PATH + "/config", SC_OK);
			
			JsonNode configNode = mapper.readTree(mockEmailConfig.getContent());
			return configNode.get("bindPort").getIntValue();
		} catch (IOException e) {
			throw new ClientException("Failed retrieving configuration", e);
		}
	}
	
	/**
	 * Retrieves the list of mail messages currently stored
	 * 
	 * @return the list of messages, possibly empty
	 * @throws ClientException in case of any errors
	 */
	public List<EmailMessage> getMessages() throws ClientException {
    	List<EmailMessage> emails = new ArrayList<>();
    	
        try {
			SlingHttpResponse response = doGet(EMAIL_SERVLET_PATH + "/messages", SC_OK);
			JsonNode messages = mapper.readTree(response.getContent());
			for ( JsonNode emailNode : messages.get("messages") ) {
				EmailMessage msg = new EmailMessage(emailNode.get(PN_CONTENT).getTextValue());
				Iterator<String> fieldNames = emailNode.getFieldNames();
				while ( fieldNames.hasNext() ) {
					String fieldName = fieldNames.next();
					if ( fieldName.equals(PN_CONTENT) ) {
						continue;
					}
					msg.addHeader(fieldName, emailNode.get(fieldName).getTextValue());
				}
					
				emails.add(msg);
			}
		} catch (IOException e) {
			throw new ClientException("Failed retrieving email messages", e);
		}
        
        
        return emails;		
	}
	
	/**
	 * Deletes all mail messages currently stored
	 * 
	 * @throws ClientException in case of any errors
	 */
	public void deleteMessages() throws ClientException {
		doDelete(EMAIL_SERVLET_PATH, Collections.<NameValuePair>emptyList(), 
				Collections.<Header> emptyList(), SC_NO_CONTENT);		
	}
}
