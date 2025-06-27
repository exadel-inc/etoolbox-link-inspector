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

import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.exadel.etoolbox.linkinspector.core.models.ui.PaginationModel;
import com.exadel.etoolbox.linkinspector.core.services.GridDataSource;
import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.data.models.DataFilter;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link GridDataSource} service interface that provides data sources
 * for the Link Inspector grid.
 * <p>
 * This implementation retrieves link data from the {@link DataFeedService} and applies
 * pagination, filtering, and sorting as required by the UI. It transforms the data into
 * a format that can be consumed by the AEM Granite UI components.
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
@Component(service = GridDataSource.class)
public class GridDataSourceImpl implements GridDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(GridDataSourceImpl.class);
    private static final int DEFAULT_PAGE_NUMBER = 1;

    @Reference
    private DataFeedService dataFeedService;

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSource getDataSource(String page, String limit, String offset, String type, String substring) {
        LOG.debug("GridDataSource initialization");

        int pageNumber = NumberUtils.isNumber(page) ? Integer.parseInt(page) : DEFAULT_PAGE_NUMBER;

        List<Resource> resources = dataFeedService.dataFeedToResources(new DataFilter(type, substring)).stream()
                .skip((long) PaginationModel.DEFAULT_PAGE_SIZE * (pageNumber - 1))
                .limit(PaginationModel.DEFAULT_PAGE_SIZE)
                .collect(Collectors.toList());

        if (NumberUtils.isNumber(offset)) {
            resources = resources.stream().skip(Long.parseLong(offset)).collect(Collectors.toList());
        }

        if (NumberUtils.isNumber(limit)) {
            resources = resources.stream().limit(Long.parseLong(limit)).collect(Collectors.toList());
        }

        return new SimpleDataSource(resources.iterator());
    }
}
