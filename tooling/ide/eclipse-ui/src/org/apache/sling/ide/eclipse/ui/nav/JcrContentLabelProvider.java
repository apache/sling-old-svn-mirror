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
package org.apache.sling.ide.eclipse.ui.nav;

import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.jdt.ui.ProblemsLabelDecorator;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.navigator.IDescriptionProvider;

/** WIP: label provider for content package view in project explorer **/
public class JcrContentLabelProvider extends LabelProvider implements ILabelProvider, IDescriptionProvider {

    // https://github.com/eclipse/webtools.javaee/blob/7c9fc5163a8c85bcefa8fff320f75ceddde9dc14/plugins/org.eclipse.jst.servlet.ui/servlet_ui/org/eclipse/jst/servlet/ui/internal/navigator/WebJavaLabelProvider.java
    private final ILabelDecorator problemsLabelDecorator;

    public JcrContentLabelProvider() {
        super();
        this.problemsLabelDecorator = new ProblemsLabelDecorator();
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof JcrNode) {
            JcrNode jcrNode = (JcrNode) element;
            long start = System.currentTimeMillis();
            Image image = jcrNode.getImage();
            // add problems marker overlay for errors (element must be the underlying IResource)
            image = problemsLabelDecorator.decorateImage(image, ((JcrNode) element).getResource());
            long end = System.currentTimeMillis();
            Activator.getDefault().getPluginLogger().tracePerformance("getImage for node at {0}", (end - start),
                    jcrNode.getJcrPath());
            return image;
        } else {
            // fallback to default
            return null;
        }
    }

    @Override
    public String getText(Object element) {
        if (element instanceof JcrNode) {
            JcrNode jcrNode = (JcrNode) element;
            return jcrNode.getLabel();
        } else {
            // fallback to the default
            return null;
        }
    }

    @Override
    public String getDescription(Object element) {
        if (element instanceof JcrNode) {
            JcrNode jcrNode = (JcrNode) element;
            return jcrNode.getDescription();
        } else {
            // fallback to the default
            return null;
        }
    }

}
