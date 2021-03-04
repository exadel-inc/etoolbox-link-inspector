package com.exadel.aembox.linkchecker.core.services.util;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.NameConstants;
import com.day.crx.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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

    public static Object replaceStringInPropValue(Object value, String current, String replacement) {
        if (value instanceof String) {
            String currentValue = (String) value;
            String newValue = currentValue.replaceAll(current, replacement);
            if (!currentValue.equals(newValue)) {
                return newValue;
            }
        } else if (value instanceof String[]) {
            String[] currentValues = (String[]) value;
            String[] newValues = Arrays.stream(currentValues)
                    .map(currentValue -> currentValue.replaceAll(current, replacement))
                    .toArray(String[]::new);
            if (!Arrays.equals(currentValues, newValues)) {
                return newValues;
            }
        }
        return null;
    }

    public static Instant getLastModified(Resource resource) {
        ValueMap properties = resource.getValueMap();
        Date cqLastModified = properties.get(NameConstants.PN_PAGE_LAST_MOD, Date.class);
        Date jcrLastModified = properties.get(JcrConstants.JCR_LASTMODIFIED, Date.class);

        return Stream.of(cqLastModified, jcrLastModified)
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .map(Date::toInstant)
                .findFirst()
                .orElse(null);
    }

    public static Instant getCqReplicated(Resource resource) {
        return Optional.of(resource.getValueMap())
                .map(valueMap -> valueMap.get(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED, Date.class))
                .map(Date::toInstant)
                .orElse(null);
    }

    public static boolean isPageOrAsset(Resource resource) {
        return Optional.of(resource.getValueMap())
                .map(valueMap -> valueMap.get(JcrConstants.JCR_PRIMARYTYPE))
                .filter(type -> NameConstants.NT_PAGE.equals(type) || DamConstants.NT_DAM_ASSET.equals(type))
                .isPresent();
    }

    public static boolean isModifiedBeforeActivation(Resource resource) {
        return isModifiedBeforeActivation(getLastModified(resource), getCqReplicated(resource));
    }

    public static boolean isModifiedBeforeActivation(Instant lastModified, Instant lastReplicated) {
        if (lastModified != null && lastReplicated != null) {
            return lastModified.isBefore(lastReplicated);
        }
        return true;
    }
}