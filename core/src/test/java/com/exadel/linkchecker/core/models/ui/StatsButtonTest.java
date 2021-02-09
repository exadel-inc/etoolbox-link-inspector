package com.exadel.linkchecker.core.models.ui;

import com.day.crx.JcrConstants;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
class StatsButtonTest {
    private static final String STATS_RESOURCE_PATH_PARAM = "statsResourcePath";
    private static final String TEST_RESOURCE_PATH = "/content/linkchecker/data/stats";

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private StatsButton fixture;

    @BeforeEach
    public void setup() {
        SlingHttpServletRequest request = context.request();
        request.setAttribute(STATS_RESOURCE_PATH_PARAM, TEST_RESOURCE_PATH);

        fixture = request.adaptTo(StatsButton.class);
        assertNotNull(fixture);
    }

    @Test
    void testResourceExists() {
        context.create().resource(TEST_RESOURCE_PATH,
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, JcrConstants.NT_UNSTRUCTURED);

        assertTrue(fixture.isStatsResourceExist());
    }

    @Test
    void testResourceNotExist() {
        assertFalse(fixture.isStatsResourceExist());
    }
}