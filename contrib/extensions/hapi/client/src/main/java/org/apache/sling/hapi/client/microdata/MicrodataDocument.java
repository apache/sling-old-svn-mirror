/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/

package org.apache.sling.hapi.client.microdata;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.sling.hapi.client.*;
import org.apache.sling.hapi.client.forms.FormValues;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class MicrodataDocument implements Document {
    private org.jsoup.nodes.Document jsoupDocument;
    private HtmlClient client;

    public MicrodataDocument(String html, HtmlClient client, String baseUrl) {
        this.jsoupDocument = Jsoup.parse(html, baseUrl);
        this.client = client;
    }


    @Override
    public Items link(String rel) throws ClientException {
        Item me = toItem(jsoupDocument);
        return me.link(rel);
    }

    @Override
    public Items form(String rel) throws ClientException {
        Item me = toItem(jsoupDocument);
        return me.form(rel);
    }

    @Override
    public Items item(String rel) {
        List<Item> items = new ArrayList<Item>();

        for (Item i : items()) {
            if (((ItemImpl) i).el.hasClass(rel)) {
                items.add(i);
            }
        }

        return new ItemsImpl(items);
    }

    @Override
    public Items items() {
        return new ItemsImpl(selectItems(jsoupDocument, new ArrayList<Item>()));
    }

    @Override
    public String toString() {
        return this.jsoupDocument.toString();
    }

    private String toClass(String name) {
        return "." + name;
    }

    private List<Item> toItems(Elements el) {
        List<Item> items = new ArrayList<Item>();

        for (Element e : el) {
            items.add(toItem(e));
        }

        return items;
    }

    private Item toItem(Element el) {
        return new ItemImpl(el, this);
    }

    private List<Item> selectItems(Element e, List<Item> items) {
        if (e.hasAttr("itemscope") && !e.hasAttr("itemprop")) {
            items.add(new ItemImpl(e, this));
            return items;
        }

        for (Element c : e.children()) {
            selectItems(c, items);
        }

        return items;
    }


    private class ItemImpl implements Item {

        private Element el;
        private MicrodataDocument document;
        private ItemImpl followed;

        public ItemImpl(Element element, MicrodataDocument document) {
            if (element == null) throw new NullPointerException("element is mandatory");
            this.el = element;
            this.document = document;
        }

        @Override
        public Items prop(String name) throws ClientException {
            return new ItemsImpl(selectProps(getProxy().el, name, new ArrayList<Item>()));
        }

        @Override
        public Set<String> props() throws ClientException {
            return selectAllPropNames(getProxy().el, new HashSet<String>());
        }

        @Override
        public Items link(String rel) throws ClientException {
            return new ItemsImpl(toItems(getProxy().el.select("link[rel=" + rel + "], a[rel=" + rel + "]")));
        }

        @Override
        public Items form(String rel) throws ClientException {
            return new ItemsImpl(toItems(getProxy().el.select("form[data-rel=" + rel + "]")));
        }


        /* Private methods */

        private List<Item> selectProps(Element e, String name, List<Item> items) {
            for (Element c : e.children()) {
                if (c.hasAttr("itemprop")) {
                    if (c.attr("itemprop").equals(name)) {
                        items.add(new ItemImpl(c, document));
                    }

                    if (c.hasAttr("itemscope")) {
                        continue;
                    }
                }

                selectProps(c, name, items);
            }

            return items;
        }

        private Set<String> selectAllPropNames(Element e, Set<String> items) {
            for (Element c : e.children()) {
                if (c.hasAttr("itemprop")) {
                    items.add(c.attr("itemprop"));
                }

                if (c.hasAttr("itemscope")) {
                        continue;
                }

                selectAllPropNames(c, items);
            }
            return items;
        }

        private ItemImpl getProxy() throws ClientException {
            if (el.tagName().equalsIgnoreCase("a") && el.hasAttr("href")) {
                if (followed == null) {
                    followed = follow(el, document);
                }
                return followed;
            }

            return this;
        }

        private ItemImpl follow(Element el, MicrodataDocument doc) throws ClientException {
            if (el.hasAttr("itemscope")) {
                return new ItemImpl(el, doc);
            }

            if (el.tagName().equalsIgnoreCase("a") && el.hasAttr("href")) {
                String href = el.attr("href");

                if (href.startsWith("#")) {
                    Element first = el.ownerDocument().select(href).first();
                    return first == null ? null : follow(first, doc);
                }

                String absHref = el.attr("abs:href");
                MicrodataDocument d = (MicrodataDocument) doc.client.get(absHref);

                try {
                    URI uri = new URI(absHref);

                    String fragment = uri.getRawFragment();
                    if (fragment != null) {
                        Element e = d.jsoupDocument.getElementById(fragment);
                        return e == null ? null : follow(e, d);
                    }
                } catch (URISyntaxException ex) {
                    throw new ClientException("Error parsing URI: " + absHref, ex);
                }

                ItemsImpl items = (ItemsImpl) d.items();

                if (items.length() == 1) {
                    return (ItemImpl) items.at(0);
                }

                throw new ClientException("Unable determine item: " + absHref);
            }

            return new ItemImpl(el, doc);
        }

        @Override
        public String text() throws ClientException {
            // resolve element
            Element el = getProxy().el;

            // if it's a meta, get the value of the content attr
            if (el.tagName().equalsIgnoreCase("meta") && el.hasAttr("content")) {
                return el.attr("content");
            }

            // else, get the text value using jsoup
            return getProxy().el.text();
        }

        @Override
        public boolean bool() throws ClientException {
            return Boolean.parseBoolean(text());
        }

        @Override
        public int number() throws ClientException {
            return Integer.parseInt(text());
        }

        @Override
        public String href() {
            return el.attr("href");
        }

        @Override
        public String src() {
            return el.attr("src");
        }

        @Override
        public Document follow() throws ClientException {
            if ((el.tagName().equalsIgnoreCase("a") || el.tagName().equalsIgnoreCase("link")) && el.hasAttr("href")) {
                String href = el.attr("href");

                if (href.startsWith("#")) {
                    return document;
                }

                return document.client.enter(el.attr("abs:href"));
            }

            throw new ClientException("Unable to follow: " + el.toString());
        }

        @Override
        public Document submit(Iterable<NameValuePair> values) throws ClientException {
            if (el.tagName().equalsIgnoreCase("form")) {
                String action = el.attr("abs:action");
                if (action.length() == 0) {
                    action = el.baseUri();
                }

                String method = el.attr("method");

                if (method.length() == 0 || method.equalsIgnoreCase("get")) {
                    FormValues query = new FormValues(el, values);
                    String url = action + (action.contains("?") ? "?" : "&") + query.toString();
                    return document.client.enter(url);
                }

                if (method.equalsIgnoreCase("post")) {
                    String enctype = el.attr("enctype");

                    FormValues v = new FormValues(el, values);
                    if (enctype.length() == 0 || enctype.equalsIgnoreCase("application/x-www-form-urlencoded")) {
                        return document.client.post(action, v.toUrlEncodedEntity());
                    } else if (enctype.equalsIgnoreCase("multipart/form-data")) {
                        return document.client.post(action, v.toMultipartEntity());
                    }

                    throw new ClientException("Unsupported form enctype: " + enctype);
                }

                throw new ClientException("Unsupported form method: " + method);
            }

            throw new ClientException("The item is not a form");
        }


    }

    /**
     * Items impl for microdata
     */
    private class ItemsImpl implements Items {
        private List<Item> items;

        public ItemsImpl(List<Item> items) {
            this.items = items;
        }

        @Override
        public Iterator<Item> iterator() {
            return items.iterator();
        }

        @Override
        public Item at(int index) {
            return items.get(index);
        }

        @Override
        public int length() {
            return items.size();
        }

        @Override
        public Items prop(String name) throws ClientException {
            return items.get(0).prop(name);
        }

        @Override
        public Set<String> props() throws ClientException {
            return items.get(0).props();
        }

        @Override
        public Items link(String rel) throws ClientException {
            return items.get(0).link(rel);
        }

        @Override
        public Items form(String rel) throws ClientException {
            return items.get(0).form(rel);
        }

        @Override
        public String text() throws ClientException {
            return items.get(0).text();
        }

        @Override
        public boolean bool() throws ClientException {
            return items.get(0).bool();
        }

        @Override
        public int number() throws ClientException {
            return items.get(0).number();
        }

        @Override
        public String href() {
            return items.get(0).href();
        }

        @Override
        public String src() {
            return items.get(0).src();
        }

        @Override
        public Document follow() throws ClientException {
            return items.get(0).follow();
        }

        @Override
        public Document submit(Iterable<NameValuePair> values) throws ClientException {
            return items.get(0).submit(values);
        }
    }

}