package org.apache.sling.mailarchiveserver.impl;

import static org.apache.james.mime4j.dom.field.FieldName.SUBJECT;
import static org.apache.sling.mailarchiveserver.util.MessageFieldName.CONTENT;
import static org.apache.sling.mailarchiveserver.util.MessageFieldName.HTML_BODY;
import static org.apache.sling.mailarchiveserver.util.MessageFieldName.LIST_ID;
import static org.apache.sling.mailarchiveserver.util.MessageFieldName.NAME;
import static org.apache.sling.mailarchiveserver.util.MessageFieldName.PLAIN_BODY;
import static org.apache.sling.mailarchiveserver.util.MessageFieldName.X_ORIGINAL_HEADER;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.Text;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.apache.james.mime4j.stream.Field;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.mailarchiveserver.api.AttachmentFilter;
import org.apache.sling.mailarchiveserver.api.MessageProcessor;
import org.apache.sling.mailarchiveserver.api.MessageStore;
import org.apache.sling.mailarchiveserver.api.ThreadKeyGenerator;
import org.apache.sling.mailarchiveserver.util.MailArchiveServerConstants;
import org.apache.sling.mailarchiveserver.util.MessageFieldName;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Service(MessageStore.class)
public class MessageStoreImpl implements MessageStore {

    public static final String PLAIN_MIMETYPE = "text/plain";
    public static final String HTML_MIMETYPE = "text/html";

    @Reference
    private	ResourceResolverFactory resourceResolverFactory;
    @Reference
    ThreadKeyGenerator threadKeyGen;
    @Reference
    AttachmentFilter attachmentFilter;
    @Reference(
            cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE, 
            policy=ReferencePolicy.DYNAMIC,
            referenceInterface=MessageProcessor.class)
    private SortedSet<ServiceReference> messageProcessorRefs = new TreeSet<ServiceReference>();
    private List<MessageProcessor> messageProcessors = new ArrayList<MessageProcessor>();
    private boolean processorsUpdated;
    private BundleContext bundleContext = null;

    static final String FIELD_SEPARATOR = " : ";
    
    // TODO should be configurable, and should probably be in ThreadKeyGeneratorImpl
    private static final String[] RE_PREFIXES = { "re:", "aw:", "fw:", "re ", "aw ", "fw ", "\u7B54\u590D"};
    
    // TODO for testing??
    String archivePath = MailArchiveServerConstants.ARCHIVE_PATH;
    String resourceTypeKey = MailArchiveServerConstants.RT_KEY;

    private static final Logger logger = LoggerFactory.getLogger(MessageStoreImpl.class);

    @Activate
    private void activate(BundleContext bc) {
        bundleContext = bc;
    }

    protected ResourceResolver getResourceResolver() throws LoginException {
        return resourceResolverFactory.getAdministrativeResourceResolver(null);
    }

    public void save(Message msg) throws IOException {
        ResourceResolver resolver = null;
        try {
            resolver = getResourceResolver();
            save(resolver, msg);
        } catch (LoginException e) {
            throw new RuntimeException("LoginException", e);
        } finally {
            if(resolver != null) {
                resolver.close();
            }
        }
    }

    private void save(ResourceResolver resolver, Message msg) throws IOException, LoginException {
        // apply message processors
        for(MessageProcessor processor : getSortedMessageProcessors()) {
            logger.debug("Calling {}", processor);
            processor.processMessage(msg);
        }

        // into path: archive/domain/list/thread/message
        final Map<String, Object> msgProps = new HashMap<String, Object>();
        final List<BodyPart> attachments = new ArrayList<BodyPart>(); 

        msgProps.put(resourceTypeKey, MailArchiveServerConstants.MESSAGE_RT);

        StringBuilder plainBody = new StringBuilder();
        StringBuilder htmlBody = new StringBuilder();
        Boolean hasBody = false;

        if (!msg.isMultipart()) {
            plainBody = new StringBuilder(getTextPart(msg)); 
        } else {
            Multipart multipart = (Multipart) msg.getBody();
            recursiveMultipartProcessing(multipart, plainBody, htmlBody, hasBody, attachments);
        }

        msgProps.put(PLAIN_BODY, plainBody.toString().replaceAll("\r\n", "\n"));
        if (htmlBody.length() > 0) {
            msgProps.put(HTML_BODY, htmlBody.toString());
        }

        msgProps.putAll(getMessagePropertiesFromHeader(msg.getHeader()));
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new DefaultMessageWriter().writeHeader(msg.getHeader(), baos);
        String origHdr = baos.toString(MailArchiveServerConstants.DEFAULT_ENCODER.charset().name());
        msgProps.put(X_ORIGINAL_HEADER, origHdr);
        
        final Header hdr = msg.getHeader();
        final String listIdRaw = hdr.getField(LIST_ID).getBody();
        final String listId = listIdRaw.substring(1, listIdRaw.length()-1); // remove < and >

        final String list = getListNodeName(listId);
        final String domain = getDomainNodeName(listId);
        final String subject = (String) msgProps.get(SUBJECT);
        final String threadPath = threadKeyGen.getThreadKey(subject);
        final String threadName = removeRe(subject);

        Resource parentResource = assertEachNode(resolver, archivePath, domain, list, threadPath, threadName);

        String msgNodeName = makeJcrFriendly((String) msgProps.get(NAME));
        boolean isMsgNodeCreated = assertResource(resolver, parentResource, msgNodeName, msgProps);
        if (isMsgNodeCreated) {
            Resource msgResource = resolver.getResource(parentResource, msgNodeName);
            for (BodyPart att : attachments) {
                if (!attachmentFilter.isEligible(att)) {
                    continue;
                }
                final Map<String, Object> attProps = new HashMap<String, Object>();
                parseHeaderToProps(att.getHeader(), attProps);
                Body body = att.getBody();
                if (body instanceof BinaryBody) {
                    attProps.put(CONTENT, ((BinaryBody) body).getInputStream());
                } else if (body instanceof TextBody) {
                    attProps.put(CONTENT, ((TextBody) body).getInputStream());
                }

                String attNodeName = Text.escapeIllegalJcrChars(att.getFilename());
                assertResource(resolver, msgResource, attNodeName, attProps);
            }

            updateThread(resolver, parentResource, msgProps);
        }
    }

    static void recursiveMultipartProcessing(Multipart multipart, StringBuilder plainBody, StringBuilder htmlBody, Boolean hasBody, List<BodyPart> attachments) throws IOException {
        for (Entity enitiy : multipart.getBodyParts()) {
            BodyPart part = (BodyPart) enitiy;
            if (part.getDispositionType() != null && !part.getDispositionType().equals("")) {
                // if DispositionType is null or empty, it means that it's multipart, not attached file
                attachments.add(part);
            } else {
                if (part.isMimeType(PLAIN_MIMETYPE) && !hasBody) {
                    plainBody.append(getTextPart(part));
                    hasBody = true;
                } else if (part.isMimeType(HTML_MIMETYPE) && !hasBody) {
                    htmlBody.append(getTextPart(part));
                } else if (part.isMultipart()) {
                    recursiveMultipartProcessing((Multipart) part.getBody(), plainBody, htmlBody, hasBody, attachments);
                } 
            }
        }
    }

    public void saveAll(Iterator<Message> iterator) throws IOException {
        ResourceResolver resolver = null;
        try {
            resolver = getResourceResolver();
            int mcount = 0;
            while (iterator.hasNext()) {
                Message msg = iterator.next();
                save(resolver, msg);

                mcount++;
                if (mcount % 100 == 0) {
                    logger.debug(mcount+" messages processed.");
                }
            }
            logger.info(mcount+" messages processed.");
        } catch(LoginException e) {
            throw new RuntimeException("LoginException", e);
        } finally {
            if(resolver != null) {
                resolver.close();
            }
        }
    }

    /**
     *	code taken from http://www.mozgoweb.com/posts/how-to-parse-mime-message-using-mime4j-library/
     */
    static String getTextPart(Entity part) throws IOException {
        TextBody tb = (TextBody) part.getBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        tb.writeTo(baos);
        return baos.toString(MailArchiveServerConstants.DEFAULT_ENCODER.charset().name());
    }

    static Map<String, Object> getMessagePropertiesFromHeader(Header hdr) {
        Map<String, Object> props = new HashMap<String, Object>();

        parseHeaderToProps(hdr, props);

        // message name
        String name;
        if (hdr.getField("Message-ID") != null) {
            name = hdr.getField("Message-ID").getBody();
            name = name.substring(1, name.length()-1); // remove < and >
        } else {
            name = Integer.toHexString(hdr.getField("Date").hashCode());
        }
        props.put(MessageFieldName.NAME, name);

        return props;
    }

    private static void parseHeaderToProps(Header hdr, Map<String, Object> props) {
        Set<String> processed = new HashSet<String>();
        for (Field f : hdr.getFields()) {
            String name = f.getName();
            if (!processed.contains(name)) {
                processed.add(name);
                String value = "";
                List<Field> fields = hdr.getFields(name);
                for (Field fl : fields) {
                    value += fl.getBody()+FIELD_SEPARATOR;
                }
                props.put(name, value.substring(0, value.length()-FIELD_SEPARATOR.length()));
            }
        }
    }

    static String getListNodeName(String listId) {
        String list = "";
        String[] split = listId.split("\\.");
        int splitL = split.length;
        if (splitL >= 4) {
            for (int i = 0; i < splitL-2; i++) {
                list += split[i] + ".";
            }
            list = list.substring(0, list.length() - 1);
        } else if (splitL == 3) {
            list = split[0];
        } else {
            throw new RuntimeException("List-Id is invalid: minimum 2 separatory dots required.");
        }
        return list;
    }

    static String getDomainNodeName(String listId) {
        String[] split = listId.split("\\.");
        int splitL = split.length;
        if (splitL >= 3) {
            return split[splitL-2] + "." + split[splitL-1];
        } else {
            throw new RuntimeException("List-Id is invalid: minimum 2 separatory dots required.");
        }
    }

    private Resource assertEachNode(ResourceResolver resolver, String archive, String domain, String list, 
            String threadPath, String threadName) throws PersistenceException, LoginException {
        final String pathToMessage = archive+domain+"/"+list+"/"+threadPath;

        String path = pathToMessage;
        Resource resource = resolver.getResource(path);

        int cnt = 0;
        while (resource == null) {
            cnt++;
            path = path.substring(0, path.lastIndexOf("/"));
            resource = resolver.getResource(path);
        }

        if (cnt > 0) {
            int threadNodesNumber = threadPath.split("/").length;

            // bind paths
            List<String> nodePaths = new ArrayList<String>();
            nodePaths.add(domain);
            nodePaths.add(list);
            for (String node : threadPath.split("/")) {
                nodePaths.add(node);
            }

            // bind props
            List<Map<String, Object>> nodeProps = new ArrayList<Map<String, Object>>();
            nodeProps.add(setProperties(MailArchiveServerConstants.DOMAIN_RT, domain));
            nodeProps.add(setProperties(MailArchiveServerConstants.LIST_RT, list));
            for (int i = 0; i < threadNodesNumber-1; i++) {
                nodeProps.add(null);
            }
            nodeProps.add(setProperties(MailArchiveServerConstants.THREAD_RT, threadName));

            // checking
            for (int i = nodePaths.size()-cnt; i < nodePaths.size(); i++) {
                String name = nodePaths.get(i);
                assertResource(resolver, resource, name, nodeProps.get(i));
                resource = resolver.getResource(resource.getPath()+"/"+name);
            }
        }

        resource = resolver.getResource(pathToMessage);
        if (resource == null) {
            throw new RuntimeException("Parent resource cannot be null.");
        } else {
            return resource;
        }

    }

    private Map<String, Object> setProperties(String resourceType, String name) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(resourceTypeKey, resourceType);
        props.put(MessageFieldName.NAME, name);
        return props;

    }

    private boolean assertResource(ResourceResolver resolver, Resource parent, String name, Map<String, Object> newProps) 
            throws LoginException, PersistenceException {
        String checkPath = parent.getPath()+"/"+name;
        final Resource checkResource = resolver.getResource(checkPath);
        if (checkResource == null) {
            final Resource newResource = resolver.create(parent, name, newProps);
            resolver.commit();
            logger.debug(String.format("Resource created at %s .", newResource.getPath()));
            return true;
        } else {
            logger.debug(String.format("Resource at %s already exists.", checkResource.getPath()));
            return false;
        }
    }

    private static void updateThread(ResourceResolver resolver, Resource thread, Map<String, Object> msgProps) throws PersistenceException {
        final ModifiableValueMap thrdProps = thread.adaptTo(ModifiableValueMap.class);
        Long prop = (Long) thrdProps.get(MessageFieldName.LAST_UPDATE);
        Date updatedDate = null; 
        if (prop != null) {
            updatedDate = new Date(prop);
        }
        final String msgProp = (String) msgProps.get(FieldName.DATE);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
        final Date msgDate = sdf.parse(msgProp, new ParsePosition(0));
        if (updatedDate == null || msgDate.after(updatedDate)) {
            thrdProps.put(MessageFieldName.LAST_UPDATE, msgDate.getTime());
            resolver.commit();
        } 
    }

    static String makeJcrFriendly(String s) {
        return s.replaceAll("[\\s\\.-]+", "_").replaceAll("\\W", "").replaceAll("\\_", " ").trim().replaceAll("[ ]+", "_");
    }

    static String removeRe(String s) {
        s = s.trim();
        boolean flag = true;
        while (flag) {
            flag = false;
            for (String prefix : RE_PREFIXES) {
                if (s.toLowerCase().startsWith(prefix)) {
                    s = s.substring(3).trim();
                    flag = true;
                }
            }
        }
        return s.trim();
    }

    public synchronized void bindMessageProcessor(ServiceReference ref) {
        synchronized (messageProcessorRefs) {
            messageProcessorRefs.add(ref);
        }
        processorsUpdated = true;
        logger.info("Message processor {} added to pool.", ref);
    }

    public void unbindMessageProcessor(ServiceReference ref) {
        synchronized (messageProcessorRefs) {
            messageProcessorRefs.remove(ref);
        }
        processorsUpdated = true;
        logger.info("Message processor {} removed from pool.", ref);
    }

    private Collection<MessageProcessor> getSortedMessageProcessors() {
        if(processorsUpdated) {
            synchronized (messageProcessorRefs) {
                processorsUpdated = false;
                messageProcessors.clear();
                for(ServiceReference ref : messageProcessorRefs) {
                    messageProcessors.add((MessageProcessor)bundleContext.getService(ref));
                }
            }
            logger.debug("Updated sorted list of MessageProcessor: {}", messageProcessors);
        }
        return messageProcessors;
    }
}
