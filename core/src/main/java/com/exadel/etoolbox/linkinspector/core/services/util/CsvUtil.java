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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * Utility class providing helper methods for CSV file generation and manipulation.
 * <p>
 * This class contains static utility methods for working with CSV data in the link inspector
 * context, such as creating CSV exports of link data, handling special characters in CSV fields,
 * and building location strings for resources.
 * <p>
 * <p><u>Note</u>: This class is not a part of the public API and is subject to change. Do not use it in your own code</p>
 */
public class CsvUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CsvUtil.class);

    public static final String CSV_MIME_TYPE = "text/csv";
    public static final String SEMICOLON = ";";
    public static final String QUOTE = "\"";
    public static final String AT_SIGN = "@";

    private CsvUtil() {}

    /**
     * Wraps a string value with quotes if it contains semicolons.
     * <p>
     * This method is used to ensure proper CSV formatting by escaping fields
     * containing the delimiter character.
     *
     * @param value The string value to check and potentially wrap
     * @return The original string wrapped in quotes if it contains semicolons, otherwise the original string
     */
    public static String wrapIfContainsSemicolon(String value) {
        if (StringUtils.contains(value, SEMICOLON)) {
            return StringUtils.wrap(value, QUOTE);
        }
        return value;
    }

    /**
     * Builds a location string by combining a path with a property name.
     * <p>
     * The format of the resulting string is "path@propertyName", which provides
     * a concise representation of where a link is stored in the content repository.
     *
     * @param path The path to the resource containing the property
     * @param propertyName The name of the property
     * @return A combined location string in the format "path@propertyName"
     */
    public static String buildLocation(String path, String propertyName) {
        return path + AT_SIGN + propertyName;
    }

    /**
     * Converts a collection of items to a CSV byte array.
     * <p>
     * This method provides a generic way to convert any collection of items into a CSV format
     * using a custom print function to determine how each item is represented in the CSV.
     * The method handles the creation of the CSV format, including optional column headers.
     *
     * @param <T> The type of items in the collection
     * @param items The collection of items to convert to CSV
     * @param printRecord A function that defines how to print each item to the CSV
     * @param columnHeaders Optional array of column headers (null for no headers)
     * @return Byte array containing the CSV data, or an empty array if conversion fails
     */
    public static <T> byte[] itemsToCsvByteArray(Collection<T> items, BiConsumer<CSVPrinter, T> printRecord,
                                                 String[] columnHeaders) {
        if (items.isEmpty()) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter csvPrinter = new CSVPrinter(
                     new OutputStreamWriter(out, StandardCharsets.UTF_8),
                     columnHeaders != null ? CSVFormat.DEFAULT.withHeader(columnHeaders) : CSVFormat.DEFAULT.withSkipHeaderRecord()
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