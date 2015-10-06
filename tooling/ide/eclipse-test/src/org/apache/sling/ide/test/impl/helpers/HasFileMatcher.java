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
package org.apache.sling.ide.test.impl.helpers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HasFileMatcher extends TypeSafeMatcher<IProject> {

    private final String fileName;
    private final byte[] contents;


    public HasFileMatcher(String fileName, byte[] contents) {
        this.fileName = fileName;
        this.contents = contents;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("project with a file named " + fileName + " and contents (elided)");
    }

    @Override
    protected void describeMismatchSafely(IProject item, Description mismatchDescription) {
        mismatchDescription.appendText("at location ").appendText(fileName).appendText(" found member ")
                .appendValue(item.findMember(fileName)).appendText("; contents (elided)");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hamcrest.TypeSafeMatcher#matchesSafely(java.lang.Object)
     */
    @Override
    protected boolean matchesSafely(IProject item) {
        if (item == null) {
            return false;
        }

        IResource maybeFile = item.findMember(fileName);

        if (maybeFile == null || maybeFile.getType() != IResource.FILE) {
            return false;
        }

        if (contents == null) {
            return true;
        }

        IFile file = (IFile) maybeFile;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try ( InputStream in = file.getContents()) {
            IOUtils.copy(in, out);
        } catch (CoreException | IOException e) {
            throw new RuntimeException(e);
        }

        return Arrays.equals(out.toByteArray(), contents);
    }

}