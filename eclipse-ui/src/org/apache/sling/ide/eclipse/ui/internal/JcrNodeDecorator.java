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
package org.apache.sling.ide.eclipse.ui.internal;

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.apache.sling.ide.eclipse.ui.nav.model.SyncDir;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class JcrNodeDecorator extends LabelProvider implements ILabelDecorator, ILightweightLabelDecorator {

    @Override
    public Image decorateImage(Image image, Object element) {
        return image;
    }

    @Override
    public String decorateText(String text, Object element) {
        return text;
    }

    @Override
    public void decorate(Object element, IDecoration decoration) {

        if (element instanceof JcrNode) {
            JcrNode node = (JcrNode) element;
            if (node.getPrimaryType() != null) {
                decoration.addSuffix(" [" + node.getPrimaryType() + "]");
            }

            if (node instanceof SyncDir) {
                decoration.addOverlay(SharedImages.CONTENT_OVERLAY, IDecoration.BOTTOM_RIGHT);
            }
        }
    }
}
