package com.exadel.linkchecker.core.services.util;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.crx.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

public class LinkCheckerResourceUtil {
    private LinkCheckerResourceUtil() {}

    private static final Logger LOG = LoggerFactory.getLogger(LinkCheckerResourceUtil.class);

    public static void removeResource(String path, ResourceResolver resourceResolver) {
        try {
            Resource resource = resourceResolver.getResource(path);
            if (resource != null) {
                resourceResolver.delete(resource);
                resourceResolver.commit();
            }
        } catch (PersistenceException e) {
            LOG.error(String.format("Failed to delete resource %s", path), e);
        }
    }

    public static void saveFileToJCR(String path, byte[] contentBytes, String mimeType, ResourceResolver resolver) {
        if (contentBytes == null) {
            LOG.warn("File is null, saving in JCR is interrupted");
            return;
        }
        Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            LOG.warn("Session is null, saving in JCR is interrupted");
            return;
        }
        try (InputStream inputData = new ByteArrayInputStream(contentBytes)) {

            Node fileNode = JcrUtil.createPath(path, JcrConstants.NT_FOLDER, JcrConstants.NT_FILE, session, false);

            Node metadataNode = fileNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE);
            metadataNode.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);

            Binary contentValue = session.getValueFactory().createBinary(inputData);
            metadataNode.setProperty(JcrConstants.JCR_DATA, contentValue);

            Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis(lastModified.getTimeInMillis());
            metadataNode.setProperty(JcrConstants.JCR_LASTMODIFIED, lastModified);

            session.save();
        } catch (RepositoryException | IOException e) {
            LOG.error(String.format("Failed to create file node %s", path), e);
        }
    }
}