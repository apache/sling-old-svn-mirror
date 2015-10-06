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
package org.apache.sling.ide.eclipse.core.internal;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.Manifest;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.server.core.IServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class BundleStateHelper {

	private static final QualifiedName STATE_KEY = new QualifiedName("org.apache.sling.ide.eclipse.core.internal.bundlestatehelper", "state");
	private static final Object EMPTY_STATE = new Object();
	
	public static void resetBundleState(IServer server, IProject project) {
		try {
			project.setSessionProperty(STATE_KEY, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Object getBundleState(IServer server, IProject project) {
		if (project==null) {
			return null;
		}
		Object state;
		try {
			state = project.getSessionProperty(STATE_KEY);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		if (state!=null) {
			if (state==EMPTY_STATE) {
				return null;
			} else {
				return state;
			}
		}
        try {
        	if (server==null) {
        		return null;
        	}
			state = doRecalcDecorationState(server, project);
			project.setSessionProperty(STATE_KEY, state);
			if (state==EMPTY_STATE) {
				return null;
			} else {
				return String.valueOf(state);
			}
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static Object doRecalcDecorationState(IServer server, IProject project) {

        try {
        	if (!ProjectHelper.isBundleProject(project)) {
        		return EMPTY_STATE;
        	}
        	IJavaProject javaProject = ProjectHelper.asJavaProject(project);
            String hostname = server.getHost();
            int launchpadPort = server.getAttribute(ISlingLaunchpadServer.PROP_PORT, 8080);
            if (project.exists() && !javaProject.exists()) {
            	// then it's not a java project..
            	return EMPTY_STATE;
            }
			IPath outputLocation = javaProject.getOutputLocation();
			outputLocation = outputLocation.removeFirstSegments(1);
			IPath manifestFilePath = outputLocation.append("META-INF/MANIFEST.MF");
			IFile manifestFile = project.getFile(manifestFilePath);
			String bundlename;
			if (manifestFile.exists()) {
				Manifest manifest = new Manifest(manifestFile.getContents());
				bundlename = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
			} else {
				String groupId = ProjectHelper.getMavenProperty(project, "groupId");
				String artifactId = ProjectHelper.getMavenProperty(project, "artifactId");
				if (groupId==null || groupId.isEmpty()) {
					bundlename = artifactId;
				} else {
					bundlename = groupId + "." + artifactId;
				}
			}
			String username = server.getAttribute(ISlingLaunchpadServer.PROP_USERNAME, "admin");
			String password = server.getAttribute(ISlingLaunchpadServer.PROP_PASSWORD, "admin");
			GetMethod method = new GetMethod("http://"+hostname+":"+launchpadPort+"/system/console/bundles/"+bundlename+".json");
			int resultCode = getHttpClient(username, password).executeMethod(method);
			if (resultCode!=HttpStatus.SC_OK) {
				return " ["+resultCode+"]";
			}

            try ( InputStream input = method.getResponseBodyAsStream(); ) {
                JSONObject result = new JSONObject(new JSONTokener(new InputStreamReader(input)));
                JSONArray dataArray = (JSONArray) result.get("data");
                JSONObject firstElement = (JSONObject) dataArray.get(0);
                return " ["+firstElement.get("state")+"]";
            }
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

    private static HttpClient getHttpClient(String user, String password) {
        final HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(
            5000);

        // authentication stuff
        client.getParams().setAuthenticationPreemptive(true);
        Credentials defaultcreds = new UsernamePasswordCredentials(user,
            password);
        client.getState().setCredentials(AuthScope.ANY, defaultcreds);

        return client;
    }

}
