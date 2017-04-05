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

import org.apache.sling.ide.eclipse.ui.internal.SelectionUtils;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.ui.handlers.HandlerUtil;

public abstract class AbstractClipboardHandler extends AbstractHandler {

    private Clipboard clipboard;

    public AbstractClipboardHandler() {
        super();
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
    
        ISelection sel = HandlerUtil.getCurrentSelection(event);
        
        JcrNode node = SelectionUtils.getFirst(sel, JcrNode.class);
        if ( node == null ) {
            return null;
        }
        
        execute0(node);
    
        return null;
    }

    @Override
    public void setEnabled(Object evaluationContext) {
        
        ISelection sel = SelectionUtils.getSelectionFromEvaluationContext(evaluationContext);
        
        JcrNode node = SelectionUtils.getFirst(sel, JcrNode.class);
        
        setBaseEnabled(node != null && isEnabledFor(node));
    }

    @Override
    public void dispose() {
        super.dispose();
        
        if ( clipboard != null ) {
            clipboard.dispose();
        }
    }
    
    protected Clipboard clipboard() {
        if ( clipboard == null ) {
            clipboard = new Clipboard(null);
        }
        
        return clipboard;
    }
    
    protected abstract void execute0(JcrNode node);
    
    protected abstract boolean isEnabledFor(JcrNode node);

}