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

import com.exadel.etoolbox.linkinspector.api.Link;
import com.exadel.etoolbox.linkinspector.core.models.LinkImpl;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LinksCounter {
    public static final LinksCounter EMPTY = new LinksCounter();

    private final Map<String, AtomicInteger> statistics;

    public LinksCounter() {
        statistics = new TreeMap<>();
    }

    public Map<String, Integer> getStatistics() {
        return statistics
                .entrySet()
                .stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue().get()), HashMap::putAll);
    }

    public void checkIn(Link link) {
        String type = StringUtils.defaultIfBlank(link.getType(), LinkImpl.DEFAULT_TYPE);
        AtomicInteger count = statistics.get(type);
        if (count == null) {
            count = new AtomicInteger(0);
            statistics.put(type, count);
        }
        count.incrementAndGet();
    }

    @Override
    public String toString() {
        Map<String, Integer> stats = getStatistics();
        return "Total: " + stats.values().stream().mapToInt(i -> i).sum()
                + ". By type: " + stats.entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(", "));
    }
}