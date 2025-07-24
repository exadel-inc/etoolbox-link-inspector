/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exadel.etoolbox.linkinspector.core.services.util;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.NameConstants;
import com.day.crx.JcrConstants;
import org.apache.sling.api.resource.*;
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
import java.util.*;
import java.util.stream.Stream;

/**
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 * Utility class providing helper methods for JCR resource manipulation in the link inspector context.
 * <p>
 * This class contains methods for common operations such as creating, modifying, and removing resources,
 * working with JCR nodes and properties, and checking resource metadata like modification times.
 * All methods are static and the class cannot be instantiated.
 */
public class LinkInspectorResourceUtil {

    private static final Logger LOG = LoggerFactory.getLogger(LinkInspectorResourceUtil.class);

    private LinkInspectorResourceUtil() {
    }

    /**
     * Removes a resource at the specified path using the provided ResourceResolver.
     * Logs an error if the deletion fails.
     *
     * @param path Path of the resource to be removed
     * @param resourceResolver ResourceResolver to use for the operation
     */
    public static void removeResource(String path, ResourceResolver resourceResolver) {
        try {
            Resource resource = resourceResolver.getResource(path);
            if (resource != null) {
                resourceResolver.delete(resource);
                resourceResolver.commit();
            }
        } catch (PersistenceException e) {
            LOG.warn(String.format("Failed to delete resource %s", path), e);
        }
    }

    /**
     * Creates a JCR node at the specified path with the default node type (nt:unstructured).
     * Logs a warning if the session is null, and an error if the node creation fails.
     *
     * @param path Path where the node should be created
     * @param resourceResolver ResourceResolver to use for the operation
     */
    public static void createNode(String path, ResourceResolver resourceResolver) {
        try {
            Session session = resourceResolver.adaptTo(Session.class);
            if (session == null) {
                LOG.warn("Session is null, recreating node is interrupted");
                return;
            }
            JcrUtil.createPath(path, JcrConstants.NT_UNSTRUCTURED, session);
            session.save();
        } catch (RepositoryException e) {
            LOG.info("Failed to recreate node {}", path, e);
        }
    }

    /**
     * Adds or updates a Long parameter to a JCR node at the specified path.
     * Logs an error if the operation fails.
     *
     * @param path Path of the node to modify
     * @param resourceResolver ResourceResolver to use for the operation
     * @param paramName Name of the parameter to add
     * @param paramValue Long value to set
     */
    public static void addParamToNode(String path, ResourceResolver resourceResolver, String paramName, Long paramValue) {
        try {
            Resource resource = resourceResolver.getResource(path);
            if (resource != null) {
                ModifiableValueMap valueMap = resource.adaptTo(ModifiableValueMap.class);
                if (valueMap == null) {
                    return;
                }
                valueMap.put(paramName, paramValue);
                resourceResolver.commit();
            }
        } catch (PersistenceException e) {
            LOG.info("Failed to add parameter to node", e);
        }
    }

    /**
     * Saves a file to the JCR repository at the specified path.
     * Creates the necessary nodes and sets appropriate metadata properties.
     * Logs warnings if file content or session is null, and errors if the operation fails.
     *
     * @param path Path where the file should be created
     * @param contentBytes Byte array containing the file content
     * @param mimeType MIME type of the file
     * @param resolver ResourceResolver to use for the operation
     */
    public static void saveFileToJCR(String path, byte[] contentBytes, String mimeType, ResourceResolver resolver) {
        if (contentBytes == null) {
            LOG.warn("File is null, saving to JCR is interrupted");
            return;
        }
        Session session = resolver.adaptTo(Session.class);
        if (session == null) {
            LOG.warn("Session is null, saving to JCR is interrupted");
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
            LOG.warn(String.format("Failed to create file node %s", path), e);
        }
    }

    /**
     * Replaces a string within a property value, supporting both String and String[] types.
     * Returns null if no changes were made, otherwise returns the new value.
     *
     * @param value Original property value (String or String[])
     * @param current String to be replaced
     * @param replacement Replacement string
     * @return Modified value if changes were made, or null if no changes
     */
    public static Object replaceStringInPropValue(Object value, String current, String replacement) {
        if (value instanceof String) {
            String currentValue = (String) value;
            String newValue = currentValue.replace(current, replacement);
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

    /**
     * Gets the last modification timestamp of a resource by checking both cq:lastModified
     * and jcr:lastModified properties. Returns the most recent timestamp if both exist.
     *
     * @param resource The resource to check
     * @return The last modification time as an Instant, or null if not available
     */
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

    /**
     * Gets the last replication timestamp of a resource by checking the cq:lastReplicated property.
     *
     * @param resource The resource to check
     * @return The last replication time as an Instant, or null if not available
     */
    public static Instant getCqReplicated(Resource resource) {
        return Optional.of(resource.getValueMap())
                .map(valueMap -> valueMap.get(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED, Date.class))
                .map(Date::toInstant)
                .orElse(null);
    }

    /**
     * Checks if a resource is a Page or Asset based on its jcr:primaryType.
     *
     * @param resource The resource to check
     * @return True if the resource is a Page or Asset, false otherwise
     */
    public static boolean isPageOrAsset(Resource resource) {
        return Optional.of(resource.getValueMap())
                .map(valueMap -> valueMap.get(JcrConstants.JCR_PRIMARYTYPE))
                .filter(type -> NameConstants.NT_PAGE.equals(type) || DamConstants.NT_DAM_ASSET.equals(type))
                .isPresent();
    }

    /**
     * Checks if a resource was modified before its activation by comparing
     * the last modified timestamp with the last replicated timestamp.
     *
     * @param resource The resource to check
     * @return True if the resource was modified before activation, false otherwise
     */
    public static boolean isModifiedBeforeActivation(Resource resource) {
        return isModifiedBeforeActivation(getLastModified(resource), getCqReplicated(resource));
    }

    /**
     * Checks if a modification timestamp is before an activation timestamp.
     *
     * @param lastModified The last modification timestamp
     * @param lastReplicated The last replication timestamp
     * @return True if modified before activation, or if either timestamp is null
     */
    public static boolean isModifiedBeforeActivation(Instant lastModified, Instant lastReplicated) {
        if (lastModified != null && lastReplicated != null) {
            return lastModified.isBefore(lastReplicated);
        }
        return true;
    }
}