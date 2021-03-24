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

import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.exadel.etoolbox.linkinspector.core.services.GridDataSource;
import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.adobe.granite.ui.components.ds.DataSource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = GridDataSource.class)
public class GridDataSourceImpl implements GridDataSource {
    private static final Logger LOG = LoggerFactory.getLogger(GridDataSourceImpl.class);

    @Reference
    private DataFeedService dataFeedService;

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSource getDataSource() {
        LOG.debug("GridDataSource initialization");
        return new SimpleDataSource(dataFeedService.dataFeedToResources().iterator());
    }
}
