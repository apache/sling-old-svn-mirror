/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.content;

import org.apache.sling.content.jcr.SimpleContent;

/**
 * The <code>SelectableSimpleContent</code> class extends the
 * <code>SimpleContent</code> class by implementing the
 * {@link SelectableContent} interface hence supporting selection as defined by
 * the Sling core bundle.
 * 
 * @ocm.mapped discriminator="false"
 */
public abstract class SelectableSimpleContent extends SimpleContent implements
        SelectableContent {

    /** @ocm.bean fieldName="selector" jcrName="sling:selector" */
    private Selector selector;

    public Selector getSelector() {
        return selector;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }
}