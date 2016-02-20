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
package org.apache.sling.ide.eclipse.ui.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.apache.sling.ide.eclipse.ui.browser.AbstractOpenInBrowserHandler;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.wst.server.core.IServer;

public class OpenHtmlInBrowserHandler extends AbstractOpenInBrowserHandler {

	protected URL getUrlToOpen(JcrNode node, IServer server) throws MalformedURLException {

        return new URL("http", server.getHost(), server.getAttribute(ISlingLaunchpadServer.PROP_PORT, 8080),
                node.getJcrPath() + ".html");
    }

}
