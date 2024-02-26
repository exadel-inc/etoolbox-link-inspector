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
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

public class CsvUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CsvUtil.class);

    public static final String CSV_MIME_TYPE = "text/csv";
    public static final String SEMICOLON = ";";
    public static final String QUOTE = "\"";
    public static final String AT_SIGN = "@";
    public static final String CSV_COLUMN_PROPERTY_LOCATION = "Property Location";
    public static final String CSV_COLUMN_TYPE = "Type";
    public static final String CSV_COLUMN_LINK = "Link";
    public static final String CSV_COLUMN_CODE = "Code";
    public static final String CSV_COLUMN_STATUS_MESSAGE = "Status Message";

    /**
     * The columns represented in the Csv report
     */
    public static final String[] CSV_COLUMNS = {
            CSV_COLUMN_LINK,
            CSV_COLUMN_TYPE,
            CSV_COLUMN_CODE,
            CSV_COLUMN_STATUS_MESSAGE,
            "Page",
            "Page Path",
            "Component Name",
            "Component Type",
            CSV_COLUMN_PROPERTY_LOCATION
    };

    /**
     * Property name for count of reports
     */
    public static final String REPORTS_SIZE_PROPERTY_NAME = "size";

    private CsvUtil() {
    }

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

    public static List<CSVRecord> readCsvItems(InputStream inputStream, String[] headersArray) {
        List<CSVRecord> csvRecords = new ArrayList<>();
        if (inputStream == null) {
            return csvRecords;
        }
        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader(headersArray);
        Reader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            final Iterable<CSVRecord> records = csvFormat.parse(reader);
            for (CSVRecord linkRecord : records) {
                csvRecords.add(linkRecord);
            }
        } catch (IOException e) {
            LOG.error("Failed to read csv item form jcr.", e);
        }
        return csvRecords;
    }

    public static String convertPageNumberToPath(String basePath, int page) {
        return String.format("%s/%d.csv", basePath, page);
    }
}