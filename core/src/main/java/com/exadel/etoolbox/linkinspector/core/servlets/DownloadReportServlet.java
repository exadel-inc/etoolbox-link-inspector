package com.exadel.etoolbox.linkinspector.core.servlets;

import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.util.CsvUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component(service = {Servlet.class})
@SlingServletResourceTypes(
        resourceTypes = "/bin/etoolbox/link-inspector/download",
        methods = HttpConstants.METHOD_GET
)
public class DownloadReportServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadReportServlet.class);
    private static final String CONTENT_DISPOSITION_HEADER_NAME = "Content-Disposition";
    private static final String CONTENT_DISPOSITION_HEADER_VALUE = "attachment; filename=\"report.csv\"";

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        ResourceResolver resourceResolver = request.getResourceResolver();
        Resource resource = resourceResolver.getResource(DataFeedService.CSV_REPORT_NODE_PATH);

        if (resource != null) {

            final List<CSVRecord> records = extractRecords(resource);
            response.setContentType(CsvUtil.CSV_MIME_TYPE);
            response.setHeader(CONTENT_DISPOSITION_HEADER_NAME, CONTENT_DISPOSITION_HEADER_VALUE);

            try (OutputStream out = response.getOutputStream();
                 CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8),
                         CSVFormat.DEFAULT.withHeader(CsvUtil.CSV_COLUMNS)
                 )
            ) {
                for (CSVRecord record : records) {
                    csvPrinter.printRecord((Object[])getValues(record));
                }
                csvPrinter.flush();
            } catch (IOException e) {
                LOG.error("Failed to build CSV for downloading.", e);
            }
        }
    }

    private List<CSVRecord> extractRecords(Resource resource) {
        final List<CSVRecord> records = new ArrayList<>();
        int size = resource.getValueMap().get(CsvUtil.REPORTS_SIZE_PROPERTY_NAME, 0);
        for (int i = 1; i <= size; i++) {
            Resource report = resource.getResourceResolver().getResource(
                    CsvUtil.convertPageNumberToPath(DataFeedService.CSV_REPORT_NODE_PATH, i)
            );
            if (report != null) {
                records.addAll(CsvUtil.readCsvItems(report.adaptTo(InputStream.class), CsvUtil.CSV_COLUMNS));
            }
        }
        return records;
    }

    private String[] getValues(CSVRecord record) {
        return Arrays.stream(CsvUtil.CSV_COLUMNS).map(record::get)
                .toArray(String[]::new);
    }
}
