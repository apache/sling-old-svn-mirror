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
package org.apache.sling.ide.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class PathUtilTest {

    @Test
    public void getParent() {

        assertThat(PathUtil.getParent("/content/path"), equalTo("/content"));
    }

    @Test
    public void getParent_firstLevel() {

        assertThat(PathUtil.getParent("/content"), equalTo("/"));
    }

    @Test
    public void getParent_root() {

        assertThat(PathUtil.getParent("/"), nullValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void notPath() {

        PathUtil.getParent("not-a-path");
    }

    @Test(expected = IllegalArgumentException.class)
    public void notAbsolutePath() {

        PathUtil.getParent("a/relative/path");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyPath() {

        PathUtil.getParent("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullPath() {

        PathUtil.getParent(null);
    }
    
    @Test
    public void isAncestor_root() {
        
        assertThat(PathUtil.isAncestor("/", "/content"), equalTo(true));
        assertThat(PathUtil.isAncestor("/", "/content/child"), equalTo(true));
    }
    
    @Test
    public void isAncestor_same() {
        assertThat(PathUtil.isAncestor("/", "/"), equalTo(false));
        assertThat(PathUtil.isAncestor("/content", "/content"), equalTo(false));
    }

    @Test
    public void isAncestor_oneLevelBelow() {
        
        assertThat(PathUtil.isAncestor("/content", "/content/child"), equalTo(true));
        assertThat(PathUtil.isAncestor("/content/child", "/content/child/grand-child"), equalTo(true));
    }

    @Test
    public void isAncestor_moreLevelsBelow() {
        
        assertThat(PathUtil.isAncestor("/content", "/content/child/granchild"), equalTo(true));
    }

}
