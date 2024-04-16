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
 * Pagination action definition.
 */
(function ($) {
    'use strict'

    $(document).on('click', '.elc-pagination-item', function(event) {
        const $target = $(event.target);
        if (!$target.hasClass('disabled')) {
            const page = $target.attr('data-page');
            window.location = window.location.pathname + '?page=' + page;
        }
    });

})(Granite.$);