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

import Handlebars from "handlebars";
import marked from "marked";
import URL from "url";
import pathInfo from "pathinfo";

export default class BlogComponent {
  constructor() {

    this.renderer = new marked.Renderer();
    this.renderer.blockquote = function (quote) {
      return "<blockquote class='blockquote'>" + quote + "</blockquote>";
    }


    this.templateURL = "../../hbstemplates/layout.html"
    this.basePath = URL.getAbsoluteParent(currentNode.resource.path, 4);
    this.baseResource = require("resource!" + this.basePath);
    this.stylesheet = "";
    this.partialContentTemplateURL = "../../hbstemplates/content.html";
    this.model = currentNode;
    this.model.viewTypeSmall = pathInfo.hasSelector("small");
    this.model.title = currentNode.properties.pageTitle || currentNode.properties.title || currentNode.properties["jcr:title"] || currentNode.resource.name;
  }

  resolveTitle(res) {
    return  res.properties.navTitle || res.properties.title || res.properties["jcr:title"] || res.name;
  }

  transformMarkdown(content) {
    var self = this;
    return marked(content, { renderer: self.renderer});
  }

  pages() {
    var self = this;
    var pages = [];
    var homePage =
    pages.push({
      path: pathInfo.hasSelector("small") ? this.basePath + ".small": this.basePath,
      title: self.resolveTitle(this.baseResource),
      active: ((currentNode.resource.path.indexOf(this.basePath + "/posts")===0 || currentNode.resource.path === this.basePath )? 'active' : '')
    });
    
    var rootPage = require("resource!" + this.basePath + "/pages");
    var children = rootPage.children;

    children.forEach(function(child){
      var nav = {
        path: pathInfo.hasSelector("small") ? child.path + ".small": child.path,
        title: self.resolveTitle(child),
        active: (currentNode.resource.path === child.path ? 'active' : '')
      };
      pages.push(nav);
    });

    return pages;
  }

  render() {
    this.init();
    Handlebars.registerPartial('content',require("text!" + this.partialContentTemplateURL));
    var templateSource= require("text!" + this.templateURL);
    var template = Handlebars.compile(templateSource);
    this.model.style = this.stylesheet;
    this.model.pagesNav = this.pages();

    return template(this.model);
  }
}
