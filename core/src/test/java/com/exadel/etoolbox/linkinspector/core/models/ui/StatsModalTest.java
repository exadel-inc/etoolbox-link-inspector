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

package com.exadel.etoolbox.linkinspector.core.models.ui;

import com.day.crx.JcrConstants;
import com.exadel.etoolbox.linkinspector.core.services.data.GenerationStatsProps;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class StatsModalTest {
    private static final String TEST_RESOURCE_PATH = "/content/link-inspector/data/stats";
    private static final String TEST_LAST_GENERATED = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    private static final String TEST_SEARCH_PATH = "/content/search-path";
    private static final String[] TEST_EXCLUDED_PATHS =
            {"/content/search-path/excluded-1", "/content/search-path/excluded-2"};
    private static final String EXPECTED_EXCLUDED_PATHS = "/content/search-path/excluded-1, /content/search-path/excluded-2";
    private static final boolean TEST_CHECK_ACTIVATION = true;
    private static final boolean TEST_SKIP_MODIFIED_AFTER_ACTIVATION = true;
    private static final String TEST_LAST_MOD_BOUNDARY = "2011-12-03T10:15:30+01:00";
    private static final String[] TEST_EXCLUDED_PROPS = {"excluded-1", "excluded-2"};
    private static final String EXPECTED_EXCLUDED_PROPS = "excluded-1, excluded-2";

    private static final String[] TEST_EXCLUDED_LINKS_PATTERNS = {"(.*)/excluded-1", "(.*)/excluded-2"};
    private static final String EXPECTED_EXCLUDED_LINKS_PATTERNS = "(.*)/excluded-1, (.*)/excluded-2";
    private static final String TEST_EXCLUDE_TAGS = "true";
    private static final String[] TEST_STATUS_CODES = {"400", "404"};
    private static final String EXPECTED_STATUS_CODES = "400, 404";

    private static final String[] TEST_STATISTICS = new String[] {"External: 100/1", "Internal: 20/8"};

    private static final String ALL_STATUS_CODES_MSG = "All error codes outside the range '200-207'";

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @BeforeEach
    void setup() {
        context.addModelsForClasses(StatsModal.class);
    }

    @Test
    void testEmptyResourceProps() {
        Resource statsResource = context.create().resource(TEST_RESOURCE_PATH,
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, JcrConstants.NT_UNSTRUCTURED);

        StatsModal model = statsResource.adaptTo(StatsModal.class);
        assertNotNull(model);

        assertNull(model.getLastGenerated());
        assertNull(model.getSearchPath());
        assertEquals(StringUtils.EMPTY, model.getExcludedPaths());
        assertFalse(model.getCheckActivation());
        assertFalse(model.getSkipModifiedAfterActivation());
        assertNull(model.getLastModifiedBoundary());
        assertEquals(StringUtils.EMPTY, model.getExcludedProperties());
        assertEquals(StringUtils.EMPTY, model.getExcludedLinksPatterns());
        assertNull(model.getExcludeTags());
        assertEquals(StringUtils.EMPTY, model.getAllowedStatusCodes());
        assertTrue(MapUtils.isEmpty(model.getStatistics()));
    }

    @Test
    void testAllResourceProps() {
        Map<String, Object> stats = new HashMap<>();

        stats.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, JcrConstants.NT_UNSTRUCTURED);

        stats.put(GenerationStatsProps.PN_LAST_GENERATED, TEST_LAST_GENERATED);
        stats.put(GenerationStatsProps.PN_SEARCH_PATH, TEST_SEARCH_PATH);
        stats.put(GenerationStatsProps.PN_EXCLUDED_PATHS, TEST_EXCLUDED_PATHS);
        stats.put(GenerationStatsProps.PN_CHECK_ACTIVATION, TEST_CHECK_ACTIVATION);
        stats.put(GenerationStatsProps.PN_SKIP_MODIFIED_AFTER_ACTIVATION, TEST_SKIP_MODIFIED_AFTER_ACTIVATION);
        stats.put(GenerationStatsProps.PN_LAST_MODIFIED_BOUNDARY, TEST_LAST_MOD_BOUNDARY);
        stats.put(GenerationStatsProps.PN_EXCLUDED_PROPERTIES, TEST_EXCLUDED_PROPS);

        stats.put(GenerationStatsProps.PN_EXCLUDED_LINK_PATTERNS, TEST_EXCLUDED_LINKS_PATTERNS);
        stats.put(GenerationStatsProps.PN_EXCLUDED_TAGS, TEST_EXCLUDE_TAGS);
        stats.put(GenerationStatsProps.PN_ALLOWED_STATUS_CODES, TEST_STATUS_CODES);

        stats.put(GenerationStatsProps.PN_STATISTICS, TEST_STATISTICS);

        Resource statsResource = context.create().resource(TEST_RESOURCE_PATH, stats);

        StatsModal model = statsResource.adaptTo(StatsModal.class);
        assertNotNull(model);

        assertEquals(TEST_LAST_GENERATED, model.getLastGenerated());
        assertEquals(TEST_SEARCH_PATH, model.getSearchPath());
        assertEquals(EXPECTED_EXCLUDED_PATHS, model.getExcludedPaths());
        assertEquals(TEST_CHECK_ACTIVATION, model.getCheckActivation());
        assertEquals(TEST_SKIP_MODIFIED_AFTER_ACTIVATION, model.getSkipModifiedAfterActivation());
        assertEquals(TEST_LAST_MOD_BOUNDARY, model.getLastModifiedBoundary());
        assertEquals(EXPECTED_EXCLUDED_PROPS, model.getExcludedProperties());
        assertEquals(EXPECTED_EXCLUDED_LINKS_PATTERNS, model.getExcludedLinksPatterns());
        assertEquals(TEST_EXCLUDE_TAGS, model.getExcludeTags());
        assertEquals(EXPECTED_STATUS_CODES, model.getAllowedStatusCodes());
        assertEquals(2, model.getStatistics().size());
    }

    @Test
    void testAllStatusCodes() {
        Resource statsResource = context.create().resource(
                TEST_RESOURCE_PATH,
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, JcrConstants.NT_UNSTRUCTURED,
                GenerationStatsProps.PN_ALLOWED_STATUS_CODES, -1);

        StatsModal model = statsResource.adaptTo(StatsModal.class);
        assertNotNull(model);

        assertEquals(ALL_STATUS_CODES_MSG, model.getAllowedStatusCodes());
    }
}