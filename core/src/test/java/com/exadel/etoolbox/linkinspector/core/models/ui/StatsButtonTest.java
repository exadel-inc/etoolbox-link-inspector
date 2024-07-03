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
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class StatsButtonTest {
    private static final String STATS_RESOURCE_PATH_PARAM = "statsResourcePath";
    private static final String TEST_RESOURCE_PATH = "/content/link-inspector/data/stats";

    private final AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private StatsButton fixture;

    @BeforeEach
    public void setup() {
        context.request().setAttribute(STATS_RESOURCE_PATH_PARAM, TEST_RESOURCE_PATH);
        context.addModelsForClasses(StatsButton.class);
        fixture = context.request().adaptTo(StatsButton.class);
    }

    @Test
    void testResourceExists() {
        context.create().resource(
                TEST_RESOURCE_PATH,
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
                JcrConstants.NT_UNSTRUCTURED);

        assertTrue(fixture.statsResourceExists());
    }

    @Test
    void testResourceNotExist() {
        assertFalse(fixture.statsResourceExists());
    }
}