package com.exadel.aembox.linkchecker.core.services.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.function.BiConsumer;

public class CsvUtil {
    private CsvUtil() {}

    private static final Logger LOG = LoggerFactory.getLogger(CsvUtil.class);

    public static final String CSV_MIME_TYPE = "text/csv";

    public static final String SEMICOLON = ";";
    public static final String QUOTE = "\"";
    public static final String AT_SIGN = "@";

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
             CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), CSVFormat.DEFAULT.withHeader(columnHeaders))
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