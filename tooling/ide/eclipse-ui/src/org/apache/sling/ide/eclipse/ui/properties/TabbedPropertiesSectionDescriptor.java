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
package org.apache.sling.ide.eclipse.ui.properties;

import java.util.List;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.tabbed.AdvancedPropertySection;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ISectionDescriptor;
import org.eclipse.ui.views.properties.tabbed.ISectionDescriptorProvider;

public class TabbedPropertiesSectionDescriptor implements
		ISectionDescriptorProvider {

	@Override
	public ISectionDescriptor[] getSectionDescriptors() {
		return new ISectionDescriptor[] {new ISectionDescriptor() {
			
			@Override
			public String getTargetTab() {
				return null;
			}
			
			@Override
			public ISection getSectionClass() {
				return new AdvancedPropertySection();
			}
			
			@Override
			public List getInputTypes() {
				return null;
			}
			
			@Override
			public String getId() {
				return "org.apache.sling.ide.eclipse-ui.propertySection1";
			}
			
			@Override
			public IFilter getFilter() {
				return null;
			}
			
			@Override
			public int getEnablesFor() {
				return 0;
			}
			
			@Override
			public String getAfterSection() {
				return null;
			}
			
			@Override
			public boolean appliesTo(IWorkbenchPart part, ISelection selection) {
				if (!(selection instanceof IStructuredSelection)) {
					return false;
				}
				IStructuredSelection iss = (IStructuredSelection) selection;
				Object first = iss.getFirstElement();
				if (first==null) {
					return false;
				}
				if (!(first instanceof JcrNode)) {
					return false;
				}
				return true;
			}
		}
		};
	}

}
