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

package com.exadel.etoolbox.linkinspector.core.services.util;

import com.exadel.etoolbox.linkinspector.api.Result;
import com.exadel.etoolbox.linkinspector.core.models.LinkResult;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Utility class for counting and accumulating statistics about links by their types.
 * <p>
 * This class is used to track the frequency of different link types during
 * link inspection and validation processes. It provides thread-safe counting operations
 * and methods to retrieve statistics about the processed links.
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
public class LinksCounter {
    public static final LinksCounter EMPTY = new LinksCounter();

    private final Map<String, AtomicInteger> statistics;

    public LinksCounter() {
        statistics = new TreeMap<>();
    }

    /**
     * Retrieves a copy of the accumulated statistics as a simple Map.
     * Converts the AtomicInteger values to plain Integer values.
     *
     * @return Map containing link types as keys and their counts as values
     */
    public Map<String, Integer> getStatistics() {
        return statistics
                .entrySet()
                .stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue().get()), HashMap::putAll);
    }

    /**
     * Increments the counter for the specified link result type.
     * If the link type is blank, uses the default link type.
     * Creates a new counter for the type if it doesn't exist yet.
     *
     * @param result The link validation result to be counted
     */
    public void checkIn(Result result) {
        String type = StringUtils.defaultIfBlank(result.getType(), LinkResult.DEFAULT_TYPE);
        AtomicInteger count = statistics.get(type);
        if (count == null) {
            count = new AtomicInteger(0);
            statistics.put(type, count);
        }
        count.incrementAndGet();
    }

    /**
     * Returns a string representation of the counter's statistics.
     * Includes the total count of all link types and a breakdown by type.
     *
     * @return A formatted string showing the statistics summary
     */
    @Override
    public String toString() {
        Map<String, Integer> stats = getStatistics();
        return "Total: " + stats.values().stream().mapToInt(i -> i).sum()
                + ". By type: " + stats.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(", "));
    }
}