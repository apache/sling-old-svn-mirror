/*
 * =============================================================================
 *
 *   Copyright (c) 2011-2014, The THYMELEAF team (http://www.thymeleaf.org)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * =============================================================================
 */
package org.thymeleaf.templateparser.html;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.Configuration;
import org.thymeleaf.dom.Document;
import org.thymeleaf.dom.Node;
import org.thymeleaf.exceptions.ConfigurationException;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.templateparser.EntityResolver;
import org.thymeleaf.templateparser.ErrorHandler;
import org.thymeleaf.templateparser.ITemplateParser;
import org.thymeleaf.templateparser.TemplatePreprocessingReader;
import org.thymeleaf.util.ResourcePool;
import org.thymeleaf.util.StandardDOMTranslator;
import org.xml.sax.InputSource;

// NOTE: This is a slightly modified class. The nasty class loader stuff is removed to not break on OSGi.

/**
 * <p>
 *   Document parser implementation for non-XML HTML documents.
 * </p>
 *
 * @since 2.0.0
 *
 * @author Daniel Fern&aacute;ndez
 */
public abstract class AbstractHtmlTemplateParser implements ITemplateParser {

    private static final int BUFFER_SIZE = 8192;

    private final String templateModeName;
    private final NekoBasedHtmlParser parser;


    protected AbstractHtmlTemplateParser(final String templateModeName, final int poolSize) {
        this.templateModeName = templateModeName;
        parser = new NekoBasedHtmlParser(poolSize);
    }





    public final Document parseTemplate(final Configuration configuration, final String documentName, final Reader reader) {
        return this.parser.parseTemplate(configuration, documentName, getTemplatePreprocessingReader(reader));
    }




    public final List<Node> parseFragment(final Configuration configuration, final String fragment) {
        final String wrappedFragment = wrapFragment(fragment);
        final Document document =
                parseTemplate(
                        configuration,
                        null, // documentName
                        new StringReader(wrappedFragment));
        return unwrapFragment(document);
    }


    protected abstract String wrapFragment(final String fragment);
    protected abstract List<Node> unwrapFragment(final Document document);





    /*
     * This is defined in a class apart so that the classloader does not always try to load
     * neko and xerces classes that might not be in the classpath.
     */
    private static class NekoBasedHtmlParser {


        private final Logger logger = LoggerFactory.getLogger(this.getClass());
        // The org.apache.xerces.parsers.DOMParser is not used here as a type
        // parameter to avoid the class loader to try to load this xerces class
        // (and fail) before we control the error at the constructor.
        private final ResourcePool<Object> pool;
        private boolean canResetParsers = true;


        private NekoBasedHtmlParser(final int poolSize) {
            super();
            this.pool = new ResourcePool<Object>(new HtmlTemplateParserFactory(), poolSize);
        }




        public final Document parseTemplate(final Configuration configuration, final String documentName,
                final TemplatePreprocessingReader templateReader) {

            final DOMParser domParser = (DOMParser) this.pool.allocate();

            try {

                domParser.setErrorHandler(ErrorHandler.INSTANCE);
                domParser.setEntityResolver(new EntityResolver(configuration));

                domParser.parse(new InputSource(templateReader));
                final org.w3c.dom.Document domDocument = domParser.getDocument();

                if (this.canResetParsers) {
                    try {
                        /*
                         * Reset the parser so that it can be used again.
                         */
                        domParser.reset();
                    } catch (final UnsupportedOperationException ignored) {
                        if (this.logger.isWarnEnabled()) {
                            this.logger.warn(
                                    "[THYMELEAF] The HTML Parser implementation being used (\"{}\") does not implement " +
                                        "the \"reset\" operation. This will force Thymeleaf to re-create parser instances " +
                                        "each time they are needed for parsing templates, which is more costly. Enabling template " +
                                        "cache is recommended, and also using a parser library which implements \"reset\" such as " +
                                        "nekoHTML version 1.9.15 or newer.",
                                    domParser.getClass().getName());
                        }
                        this.canResetParsers = false;
                    }
                }

                return StandardDOMTranslator.translateDocument(domDocument, documentName, templateReader.getDocTypeClause());

            } catch (final TemplateProcessingException e) {
                throw e;
            } catch (final Exception e) {
                throw new TemplateInputException("Exception parsing document", e);
            } finally {

                if (templateReader != null) {
                    try {
                        templateReader.close();
                    } catch (final Exception ignored) {
                        // ignored
                    }
                }

                if (this.canResetParsers) {
                    this.pool.release(domParser);
                } else {
                    this.pool.discardAndReplace(domParser);
                }

            }
        }


    }



    static class HtmlTemplateParserFactory implements ResourcePool.IResourceFactory<Object> {

        HtmlTemplateParserFactory() {
            super();
        }


        public Object createResource() {

            try {

                final HTMLConfiguration config = new HTMLConfiguration();

                config.setFeature("http://xml.org/sax/features/namespaces", false);
                config.setFeature("http://cyberneko.org/html/features/override-doctype", true);
                config.setFeature("http://cyberneko.org/html/features/scanner/cdata-sections", true);

                // Avoids the inclusion of <HTML><BODY>, etc. around template fragments. Tag balancing will only
                // be performed inside the fragments' root nodes.
                config.setFeature("http://cyberneko.org/html/features/balance-tags/document-fragment", true);

                config.setProperty("http://cyberneko.org/html/properties/doctype/pubid", "");
                config.setProperty("http://cyberneko.org/html/properties/doctype/sysid", "");
                config.setProperty("http://cyberneko.org/html/properties/names/elems", "match");
                config.setProperty("http://cyberneko.org/html/properties/names/attrs", "no-change");

                return new DOMParser(config);

            } catch(final Exception e) {
                throw new ConfigurationException(
                    "Error while creating nekoHTML-based parser for " +
                    "LEGACYHTML5 template modes.", e);
            }

        }


    }




    /**
     *
     * @return the result
     * @since 2.0.11
     */
    protected boolean shouldAddThymeleafRootToParser() {
        return false;
    }



    /**
     *
     * @param reader reader
     * @return the result
     * @since 2.0.11
     */
    protected TemplatePreprocessingReader getTemplatePreprocessingReader(final Reader reader) {
        if (reader instanceof TemplatePreprocessingReader) {
            final TemplatePreprocessingReader templatePreprocessingReader = (TemplatePreprocessingReader) reader;
            return new TemplatePreprocessingReader(
                    templatePreprocessingReader.getInnerReader(), BUFFER_SIZE, shouldAddThymeleafRootToParser());
        }
        return new TemplatePreprocessingReader(reader, BUFFER_SIZE, shouldAddThymeleafRootToParser());
    }



}
