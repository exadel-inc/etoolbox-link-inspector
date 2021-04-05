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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.BiConsumer;

public class CsvUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CsvUtil.class);

    public static final String CSV_MIME_TYPE = "text/csv";

    public static final String SEMICOLON = ";";
    public static final String QUOTE = "\"";
    public static final String AT_SIGN = "@";

    private CsvUtil() {}

    public static String wrapIfContainsSemicolon(String value) {
        if (value.contains(SEMICOLON)) {
            return StringUtils.wrap(value, QUOTE);
        }
        return value;
    }

    public static String buildLocation(String path, String propertyName) {
        return path + AT_SIGN + propertyName;
    }

    public static <T> byte[] itemsToCsvByteArray(Collection<T> items, BiConsumer<CSVPrinter, T> printRecord,
                                                 String[] columnHeaders) {
        if (items.isEmpty()) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter csvPrinter = new CSVPrinter(
                     new OutputStreamWriter(out, StandardCharsets.UTF_8),
                     CSVFormat.DEFAULT.withHeader(columnHeaders)
             )
        ) {
            items.forEach(item -> printRecord.accept(csvPrinter, item));
            csvPrinter.flush();
            return out.toByteArray();
        } catch (IOException e) {
            LOG.error(String.format("Failed to build CSV, the number of items: %s", items.size()), e);
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
    }
}