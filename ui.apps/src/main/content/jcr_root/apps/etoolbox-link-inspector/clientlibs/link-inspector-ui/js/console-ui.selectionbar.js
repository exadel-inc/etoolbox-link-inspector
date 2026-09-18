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
 *
 * Granite renders the "N selected (escape)" bar (.granite-collection-selectionbar) as a fixed overlay
 * pinned to the top of the viewport. Within the standard AEM layout it lands on top of the shell header,
 * so the console action bar that is rendered below stays visible and operable.
 *
 * When the console is opened through the Unified Shell (/ui#/aem/etoolbox/link-inspector.html) the AEM
 * shell header is not rendered, the console action bar moves to the very top of the page and the overlay
 * covers it, making "Fix Selected", "Filter", "Scan", etc. unreachable.
 *
 * Instead of hardcoding a Unified Shell detection, the overlap is measured at runtime and the overlay is
 * suppressed only when it really covers the action bar. This keeps the default AEM layout intact.
 */
(function (window, document, $) {
    'use strict';

    var SELECTION_BAR_SELECTOR = '.granite-collection-selectionbar';
    var ACTION_BAR_ANCHOR_SELECTOR = '#elc-replace-by-pattern';
    var ACTION_BAR_SELECTOR = '.foundation-collection-actionbar, betty-titlebar, .betty-ActionBar';
    var OVERLAP_CLASS = 'elc-selectionbar-overlapping';

    /** Resolves the console (non-selection) action bar holding the console actions */
    function getConsoleActionBar() {
        var anchor = document.querySelector(ACTION_BAR_ANCHOR_SELECTOR);
        if (anchor && anchor.closest) {
            var bar = anchor.closest(ACTION_BAR_SELECTOR);
            if (bar) {
                return bar;
            }
        }
        var shell = document.querySelector('coral-shell');
        return shell && shell.querySelector(ACTION_BAR_SELECTOR);
    }

    function isDisplayed(element) {
        if (!element) {
            return false;
        }
        var rect = element.getBoundingClientRect();
        return rect.width > 0 && rect.height > 0;
    }

    function updateSelectionBar() {
        var selectionBar = document.querySelector(SELECTION_BAR_SELECTOR);
        if (!selectionBar) {
            return;
        }
        selectionBar.classList.remove(OVERLAP_CLASS);
        if (!isDisplayed(selectionBar)) {
            return;
        }
        var actionBar = getConsoleActionBar();
        if (!isDisplayed(actionBar)) {
            return;
        }
        var selectionRect = selectionBar.getBoundingClientRect();
        var actionRect = actionBar.getBoundingClientRect();
        if (selectionRect.bottom > actionRect.top + 1 && selectionRect.top < actionRect.bottom - 1) {
            selectionBar.classList.add(OVERLAP_CLASS);
        }
    }

    var updateScheduled = false;
    function scheduleUpdate() {
        if (updateScheduled) {
            return;
        }
        updateScheduled = true;
        window.requestAnimationFrame(function () {
            updateScheduled = false;
            updateSelectionBar();
        });
    }

    $(document).on(
        'foundation-mode-change foundation-selections-change foundation-contentloaded',
        scheduleUpdate
    );
    $(window).on('resize', scheduleUpdate);
    $(document).ready(scheduleUpdate);

})(window, document, Granite.$);
