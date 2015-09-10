package org.apache.sling.jcr.resource.internal.helper.jcr;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.spi.resource.provider.JCRQueryProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicJcrQueryProvider implements JCRQueryProvider<JcrProviderState> {

    private static final Logger log = LoggerFactory.getLogger(BasicJcrQueryProvider.class);

    @SuppressWarnings("deprecation")
    private static final String DEFAULT_QUERY_LANGUAGE = Query.XPATH;

    /** column name for node path */
    private static final String QUERY_COLUMN_PATH = "jcr:path";

    /** column name for score value */
    private static final String QUERY_COLUMN_SCORE = "jcr:score";

    @Override
    public String[] getSupportedLanguages(ResolveContext<JcrProviderState> ctx) {
        try {
            return ctx.getProviderState().getSession().getWorkspace().getQueryManager().getSupportedQueryLanguages();
        } catch (final RepositoryException e) {
            throw new SlingException("Unable to discover supported query languages", e);
        }
    }

    @Override
    public Iterator<Resource> findResources(ResolveContext<JcrProviderState> ctx, String query, String language) {
        try {
            final QueryResult res = JcrResourceUtil.query(ctx.getProviderState().getSession(), query, language);
            return new JcrNodeResourceIterator(ctx.getResourceResolver(), res.getNodes(), ctx.getProviderState().getHelperData());
        } catch (final javax.jcr.query.InvalidQueryException iqe) {
            throw new QuerySyntaxException(iqe.getMessage(), query, language, iqe);
        } catch (final RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }
    }

    @Override
    public Iterator<ValueMap> queryResources(final ResolveContext<JcrProviderState> ctx, String query, String language) {
        final String queryLanguage = ArrayUtils.contains(getSupportedLanguages(ctx), language) ? language : DEFAULT_QUERY_LANGUAGE;

        try {
            final QueryResult result = JcrResourceUtil.query(ctx.getProviderState().getSession(), query, queryLanguage);
            final String[] colNames = result.getColumnNames();
            final RowIterator rows = result.getRows();

            return new Iterator<ValueMap>() {

                private ValueMap next;

                {
                    next = seek();
                }

                public boolean hasNext() {
                    return next != null;
                };

                public ValueMap next() {
                    if ( next == null ) {
                        throw new NoSuchElementException();
                    }
                    final ValueMap result = next;
                    next = seek();
                    return result;
                }

                private ValueMap seek() {
                    ValueMap result = null;
                    while ( result == null && rows.hasNext() ) {
                        try {
                            final Row jcrRow = rows.nextRow();
                            final String resourcePath = ctx.getProviderState().getHelperData().pathMapper.mapJCRPathToResourcePath(jcrRow.getPath());
                            if ( resourcePath != null ) {
                                final Map<String, Object> row = new HashMap<String, Object>();

                                boolean didPath = false;
                                boolean didScore = false;
                                final Value[] values = jcrRow.getValues();
                                for (int i = 0; i < values.length; i++) {
                                    Value v = values[i];
                                    if (v != null) {
                                        String colName = colNames[i];
                                        row.put(colName,
                                            JcrResourceUtil.toJavaObject(values[i]));
                                        if (colName.equals(QUERY_COLUMN_PATH)) {
                                            didPath = true;
                                            row.put(colName,
                                                    ctx.getProviderState().getHelperData().pathMapper.mapJCRPathToResourcePath(JcrResourceUtil.toJavaObject(values[i]).toString()));
                                        }
                                        if (colName.equals(QUERY_COLUMN_SCORE)) {
                                            didScore = true;
                                        }
                                    }
                                }
                                if (!didPath) {
                                    row.put(QUERY_COLUMN_PATH, ctx.getProviderState().getHelperData().pathMapper.mapJCRPathToResourcePath(jcrRow.getPath()));
                                }
                                if (!didScore) {
                                    row.put(QUERY_COLUMN_SCORE, jcrRow.getScore());
                                }
                                result = new ValueMapDecorator(row);
                            }
                        } catch (final RepositoryException re) {
                            log.error(
                                "queryResources$next: Problem accessing row values",
                                re);
                        }
                    }
                    return result;
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        } catch (final javax.jcr.query.InvalidQueryException iqe) {
            throw new QuerySyntaxException(iqe.getMessage(), query, language,
                iqe);
        } catch (final RepositoryException re) {
            throw new SlingException(re.getMessage(), re);
        }

    }

}
