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
import com.exadel.etoolbox.linkinspector.core.services.GridDataSource;
import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Component(service = GridDataSource.class)
public class GridDataSourceImpl implements GridDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(GridDataSourceImpl.class);
    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_VALUES_SIZE = 500;

    @Reference
    private DataFeedService dataFeedService;

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSource getDataSource(String page, String limit, String offset, String type) {
        LOG.debug("GridDataSource initialization");

        int pageNumber = NumberUtils.isNumber(page) ? Integer.parseInt(page) : DEFAULT_PAGE_NUMBER;

        List<Resource> resources = dataFeedService.dataFeedToResources(type).stream()
                .skip((long) DEFAULT_PAGE_VALUES_SIZE * (pageNumber - 1))
                .limit(DEFAULT_PAGE_VALUES_SIZE).collect(Collectors.toList());

        if (NumberUtils.isNumber(offset)) {
            resources = resources.stream().skip(Long.parseLong(offset)).collect(Collectors.toList());
        }

        if (NumberUtils.isNumber(limit)) {
            resources = resources.stream().limit(Long.parseLong(limit)).collect(Collectors.toList());
        }

        return new SimpleDataSource(resources.iterator());
    }
}
