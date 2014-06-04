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
package org.apache.sling.ide.eclipse.ui;

import org.apache.sling.ide.eclipse.ui.internal.SharedImages;
import org.eclipse.jface.resource.ImageDescriptor;

public class WhitelabelSupport {

    private static volatile ImageDescriptor PROJECT_WIZARD_BANNER = SharedImages.SLING_LOG;
    private static volatile ImageDescriptor JCR_NODE_ICON = SharedImages.NT_UNSTRUCTURED_ICON;
    private static volatile ImageDescriptor WIZARD_BANNER = SharedImages.SLING_LOG;
    private static volatile ImageDescriptor PRODUCT_ICON = SharedImages.SLING_ICON;
    private static volatile String PRODUCT_NAME = "Sling";

    public static ImageDescriptor getJcrNodeIcon() {
        return JCR_NODE_ICON;
    }

    public static void setJcrNodeIcon(ImageDescriptor jcrNodeIcon) {
        JCR_NODE_ICON = jcrNodeIcon;
    }

    public static String getProductName() {
        return PRODUCT_NAME;
    }

    public static void setProductName(String productName) {
        PRODUCT_NAME = productName;
    }

    public static ImageDescriptor getWizardBanner() {
        return WIZARD_BANNER;
    }

    public static void setWizardBanner(ImageDescriptor wizardBanner) {
        WIZARD_BANNER = wizardBanner;
    }

    public static ImageDescriptor getProjectWizardBanner() {
        return PROJECT_WIZARD_BANNER;
    }

    public static void setProjectWizardBanner(ImageDescriptor projectWizardBanner) {
        PROJECT_WIZARD_BANNER = projectWizardBanner;
    }

    public static ImageDescriptor getProductIcon() {
        return PRODUCT_ICON;
    }

    public static void setProductIcon(ImageDescriptor productIcon) {
        PRODUCT_ICON = productIcon;
    }

}
