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
package org.apache.sling.servlets.post;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest; 

@Component
@Service
public class PostResponseWithErrorHandling implements PostResponseCreator{

	public PostResponse createPostResponse(SlingHttpServletRequest request) {
		if (isSendError(request)) {
			return new HtmlResponse() {

				@Override
				protected void doSend(HttpServletResponse response) throws IOException {
					if (!this.isSuccessful()) {		
						response.sendError(this.getStatusCode(), this.getError().toString());
						return;
					}else{
						super.doSend(response);
					}
				}
			};
		}else{
			return null;
		}
	}

	protected boolean isSendError(SlingHttpServletRequest request){
		boolean sendError=false;
		String sendErrorParam=request.getParameter(SlingPostConstants.RP_SEND_ERROR);
		if (sendErrorParam!=null && "true".equalsIgnoreCase(sendErrorParam)){
			sendError=true;
		}
		return sendError;
	}
}
