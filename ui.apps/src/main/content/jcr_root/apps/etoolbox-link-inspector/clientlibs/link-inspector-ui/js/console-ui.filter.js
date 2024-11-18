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

/**
 * EToolbox Link Inspector clientlib.
 * "Filter" action definition.
 */
(function (Granite, $) {
    'use strict';

    function initFiltersDialog(searchParams){
        searchParams.delete('type');
        searchParams.delete('substring');
        var select = $("#linkTypesSelect").get(0).selectedItem.value;
        var text = $("#byTextFilter").get(0).value;
        if (select) {
            searchParams.append('type', select);
        }
        if (text) {
            searchParams.append('substring', text);
        }
        searchParams.set('page', '1');
        document.location.search = searchParams;
    }

    $(document).on('click', '#dialog-action', function () {
        initFiltersDialog(new URL(document.location).searchParams);
    })

    $(document).on('click', '#dialog-cancel', function () {
        var url = new URL(document.location)
        url.search = '';
        document.location = url;
    })

})(Granite, Granite.$);