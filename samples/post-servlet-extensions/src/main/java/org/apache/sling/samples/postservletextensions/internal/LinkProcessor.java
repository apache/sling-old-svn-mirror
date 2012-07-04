/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.samples.postservletextensions.internal;

import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostProcessor;

/**
 * This processor allows to create links between nodes.
 *
 */
@Component
@Service(value=SlingPostProcessor.class)
@Property(name="sling.post.processor", value="link")
public class LinkProcessor implements SlingPostProcessor {

	public final LinkHelper linkHelper = new LinkHelper();

	public void process(SlingHttpServletRequest request,
			List<Modification> changes) throws Exception {

		Session session = request.getResourceResolver().adaptTo(Session.class);

		RequestParameter linkParam = request.getRequestParameter(":link");
		if (linkParam != null){
			String linkPath = linkParam.getString();
			// check if a new node have been created
			if (changes.size() > 0 && changes.get(0).getType() == ModificationType.CREATE) {
				// hack to get the resource path
				// is it possible to add the response to the method header ?
				String resourcePath = changes.get(0).getSource();
				Node source = (Node) session.getItem(resourcePath);

				// create a symetric link
				if (session.itemExists(linkPath)) {
					Item targetItem = session.getItem(linkPath);
					if (targetItem.isNode()) {
						linkHelper.createSymetricLink(source, (Node) targetItem, "link");
					}
				}
			}
		}


	}
}
