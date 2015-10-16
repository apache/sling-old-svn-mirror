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
package org.apache.sling.ide.eclipse.ui.dnd.assistant;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.action.SubContributionManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Simple (static..) utility class which is handling showing an error message
 * in the workbench window's main status bar
 * <p>
 * TODO: consider rewriting this as a service - but considering this optional beautification atm
 */
public class StatusLineUtils {
    
    private static long statusModCnt = 0;
    
    private static boolean isShowing = false;
    
    private static final Object syncObj = new Object();

    private static IStatusLineManager getStatusLineManager() {
        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow==null) {
            return null;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage==null) {
            return null;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor!=null) {
            return activeEditor.getEditorSite().getActionBars().getStatusLineManager();
        }
        IViewReference[] viewRefs = activePage.getViewReferences();
        if (viewRefs!=null) {
            for (IViewReference aViewRef : viewRefs) {
                IViewPart view = aViewRef.getView(false);
                if (view!=null) {
                    return view.getViewSite().getActionBars().getStatusLineManager();
                }
            }
        }
        IEditorReference[] editorRefs = activePage.getEditorReferences();
        if (editorRefs!=null) {
            for (IEditorReference anEditorRef : editorRefs) {
                IEditorPart editor = anEditorRef.getEditor(false);
                if (editor!=null) {
                    return editor.getEditorSite().getActionBars().getStatusLineManager();
                }
            }
        }
        IWorkbenchPart activePart = activePage.getActivePart();
        if (activePart==null) {
            return null;
        }
        IWorkbenchPartSite site = activePart.getSite();
        if (site instanceof IEditorSite) {
            IEditorSite editorSite = (IEditorSite)site;
            return editorSite.getActionBars().getStatusLineManager();
        } else if (site instanceof IViewSite) {
            IViewSite viewSite = (IViewSite)site;
            return viewSite.getActionBars().getStatusLineManager();
        } else {
            return null;
        }
    }
    
    public static void resetErrorMessage() {
        synchronized(syncObj) {
            if (!isShowing) {
                return;
            }
            isShowing = false;
        }
        doSetErrorMessage(null);
    }

    public static void setErrorMessage(int durationInMillis, final String message) {
        final long myModCnt;
        synchronized(syncObj) {
            myModCnt = ++statusModCnt;
            isShowing = true;
        }
        doSetErrorMessage(message);
        Display.getDefault().timerExec(durationInMillis, new Runnable() {

            @Override
            public void run() {
                if (statusModCnt>myModCnt) {
                    return;
                }
                synchronized(syncObj) {
                    if (!isShowing) {
                        return;
                    }
                    isShowing = false;
                }
                doSetErrorMessage(null);
            }
            
        });
    }

    private static void doSetErrorMessage(final String message) {
        final IStatusLineManager statusLineManager = getStatusLineManager();
        if (statusLineManager!=null) {
            Display.getDefault().syncExec(new Runnable() {

                @Override
                public void run() {
                    if (statusLineManager instanceof SubContributionManager) {
                        SubContributionManager sub = (SubContributionManager)statusLineManager;
                        StatusLineManager parent = (StatusLineManager) sub.getParent();
                        parent.setErrorMessage(message);
                        parent.update(true);
                    } else {
                        statusLineManager.setErrorMessage(message);
                        statusLineManager.update(true);
                    }
                }
            });
        }
    }

}
