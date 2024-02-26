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
 * On load action.
 */
(function (window, $, Granite) {
    'use strict';

    const TABLE_BODY_URL = '/content/etoolbox-link-inspector/datasource.html?page=' + new URL(window.location.href).searchParams.get('page');

    function onInitAction($container) {
        $.ajax({
            url: TABLE_BODY_URL,
            type: 'GET',
            success: function (data, textStatus, xhr) {
                $container.empty()
                $container.append($(data).find('table'))
            }
        });
    }

    // On Load
    $(window).on('load', function() {
//        onInitAction($('.elc-report-container'));
    });

})(window, Granite.$, Granite);