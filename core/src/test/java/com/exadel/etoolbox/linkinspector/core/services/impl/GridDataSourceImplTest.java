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

package com.exadel.etoolbox.linkinspector.core.services.impl;

import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class GridDataSourceImplTest {
    private static final String DATAFEED_SERVICE_FIELD = "dataFeedService";

    private final GridDataSourceImpl gridDataSource = new GridDataSourceImpl();

    private DataFeedService dataFeedService;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        dataFeedService = mock(DataFeedService.class);
        PrivateAccessor.setField(gridDataSource, DATAFEED_SERVICE_FIELD, dataFeedService);
    }

    @Test
    void testGetDataSource() {
        when(dataFeedService.dataFeedToResources(StringUtils.EMPTY)).thenReturn(Collections.emptyList());

//        assertNotNull(gridDataSource.getDataSource());
    }
}