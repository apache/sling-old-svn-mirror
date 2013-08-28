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
package org.apache.sling.ide.eclipse.core;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MavenLaunchHelper {
	
	private static final String NL = System.getProperty("line.separator");

	public static String createMavenLaunchConfigMemento(String location, String goals, String profile, boolean offline, Map<String, String> mvnProperties) {
		final StringBuffer m2propStr = new StringBuffer();
		if (mvnProperties!=null) {
			final Set<Entry<String, String>> entries = mvnProperties.entrySet();
			for (Iterator<Entry<String, String>> it = entries.iterator(); it.hasNext();) {
				final Entry<String, String> entry = it.next();
				m2propStr.append("<listEntry value=\""+entry.getKey()+"="+entry.getValue()+"\">" + NL);
			}
		}
		
		String l = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"+ NL+
					"	<launchConfiguration type=\"org.eclipse.m2e.Maven2LaunchConfigurationType\" local=\"true\" path=\""+location+"\">"+ NL+
					"	<booleanAttribute key=\"M2_DEBUG_OUTPUT\" value=\"false\"/>"+ NL+
					"	<stringAttribute key=\"M2_GOALS\" value=\""+goals +"\"/>"+ NL+
					"	<booleanAttribute key=\"M2_NON_RECURSIVE\" value=\"false\"/>"+ NL+
					"	<booleanAttribute key=\"M2_OFFLINE\" value=\""+offline+"\"/>"+ NL+
					"	<stringAttribute key=\"M2_PROFILES\" value=\""+(profile==null ? "" : profile) +"\"/>"+ NL+
					"	<listAttribute key=\"M2_PROPERTIES\">"+ NL+
					m2propStr.toString() + NL +
					"	</listAttribute>"+ NL+
					"	<stringAttribute key=\"M2_RUNTIME\" value=\"EMBEDDED\"/>"+ NL+
					"	<booleanAttribute key=\"M2_SKIP_TESTS\" value=\"true\"/>"+ NL+
					"	<intAttribute key=\"M2_THREADS\" value=\"1\"/>"+ NL+
					"	<booleanAttribute key=\"M2_UPDATE_SNAPSHOTS\" value=\"true\"/>"+ NL+
					"	<booleanAttribute key=\"M2_WORKSPACE_RESOLUTION\" value=\"false\"/>"+ NL+
					"	<stringAttribute key=\"org.eclipse.jdt.launching.WORKING_DIRECTORY\" value=\""+location+"\"/>"+ NL+
					"	</launchConfiguration>";
		return l;
	}

}
