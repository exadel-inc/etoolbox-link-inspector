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

package com.exadel.etoolbox.linkinspector.core.services;

import com.adobe.granite.ui.components.ds.DataSource;

public interface GridDataSource {
    /**
     * Generates {@link DataSource} necessary for displaying items on the Link Inspector's page
     * within the grid (granite/ui/components/coral/foundation/table)
     *
     * @param page - page number of current page
     * @param limit - limit of items for dynamic loading
     * @param offset - offset for dynamic loading
     * @param type - type of link for filtering
     * @param substring - url substring for filtering
     * @return the {@link DataSource} object containing data related to grid items
     */
    DataSource getDataSource(String page, String limit, String offset, String type, String substring);
}
