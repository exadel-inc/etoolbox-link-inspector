package com.exadel.etoolbox.linkinspector.core.services.helpers.impl;

import com.exadel.etoolbox.linkinspector.core.models.Link;
import com.exadel.etoolbox.linkinspector.core.models.LinkStatus;
import com.exadel.etoolbox.linkinspector.core.models.ui.GridViewItem;
import com.exadel.etoolbox.linkinspector.core.services.data.DataFeedService;
import com.exadel.etoolbox.linkinspector.core.services.data.models.GridResource;
import com.exadel.etoolbox.linkinspector.core.services.helpers.CsvHelper;
import com.exadel.etoolbox.linkinspector.core.services.util.CsvUtil;
import com.exadel.etoolbox.linkinspector.core.services.util.LinkInspectorResourceUtil;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implements {@link CsvHelper} interface to provide an OSGi service which provides helper methods for csv processing
 */
@Component(service = CsvHelper.class)
public class CsvHelperImpl implements CsvHelper {

    private static final Logger LOG = LoggerFactory.getLogger(CsvHelperImpl.class);

    /**
     * Max size of page with csv report info.
     */
    private static final int PAGE_ITEMS_LIMIT = 500;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GridResource> readCsvReport(ResourceResolver resourceResolver, int page) {
        return dataFeedToGridResources(resourceResolver, page);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateCsvReport(ResourceResolver resourceResolver, List<GridViewItem> gridViewItems) {
        List<List<GridViewItem>> partitionGridViewItems = ListUtils.partition(gridViewItems, PAGE_ITEMS_LIMIT);
        LinkInspectorResourceUtil.removeResource(DataFeedService.CSV_REPORT_NODE_PATH, resourceResolver);
        LinkInspectorResourceUtil.createNode(DataFeedService.CSV_REPORT_NODE_PATH, resourceResolver);
        LinkInspectorResourceUtil
                .addParamToNode(DataFeedService.CSV_REPORT_NODE_PATH, resourceResolver, CsvUtil.REPORTS_SIZE_PROPERTY_NAME, (long) partitionGridViewItems.size());

        for (int i = 0; i < partitionGridViewItems.size(); i++) {
            saveCsvReport(resourceResolver, partitionGridViewItems.get(i), i + 1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveCsvReport(ResourceResolver resourceResolver, List<GridViewItem> gridViewItems, int page) {
        byte[] csvContentBytes = CsvUtil.itemsToCsvByteArray(gridViewItems, this::printViewItemToCsv, null);
        String reportPath = CsvUtil.convertPageNumberToPath(DataFeedService.CSV_REPORT_NODE_PATH, page);
        LinkInspectorResourceUtil.removeResource(reportPath, resourceResolver);
        LinkInspectorResourceUtil.saveFileToJCR(reportPath,
                csvContentBytes,
                CsvUtil.CSV_MIME_TYPE,
                resourceResolver);
    }

    private List<GridResource> dataFeedToGridResources(ResourceResolver resourceResolver, int page) {
        Optional<InputStream> inputStream = Optional
                .ofNullable(resourceResolver.getResource(CsvUtil.convertPageNumberToPath(DataFeedService.CSV_REPORT_NODE_PATH, page)))
                .map(resource -> resource.adaptTo(InputStream.class));

        return inputStream.map(stream -> CsvUtil.readCsvItems(stream, CsvUtil.CSV_COLUMNS)
                .stream().map(this::parseGridResourceFromCsv)
                .collect(Collectors.toList())).orElse(Collections.emptyList());
    }


    private GridResource parseGridResourceFromCsv(CSVRecord csvRecord) {
        String code = csvRecord.get(CsvUtil.CSV_COLUMN_CODE);
        String statusMessage = csvRecord.get(CsvUtil.CSV_COLUMN_STATUS_MESSAGE);
        Link link = new Link(csvRecord.get(CsvUtil.CSV_COLUMN_LINK), Link.Type.valueOf(csvRecord.get(CsvUtil.CSV_COLUMN_TYPE).toUpperCase()));
        link.setStatus(new LinkStatus(Integer.parseInt(code), statusMessage));
        return new GridResource(link,
                StringUtils.substringBeforeLast(csvRecord.get(CsvUtil.CSV_COLUMN_PROPERTY_LOCATION), CsvUtil.AT_SIGN),
                StringUtils.substringAfterLast(csvRecord.get(CsvUtil.CSV_COLUMN_PROPERTY_LOCATION), CsvUtil.AT_SIGN),
                DataFeedService.GRID_RESOURCE_TYPE);
    }

    private void printViewItemToCsv(CSVPrinter csvPrinter, GridViewItem viewItem) {
        try {
            csvPrinter.printRecord(
                    CsvUtil.wrapIfContainsSemicolon(viewItem.getLink()),
                    viewItem.getLinkType(),
                    viewItem.getLinkStatusCode(),
                    CsvUtil.wrapIfContainsSemicolon(viewItem.getLinkStatusMessage()),
                    CsvUtil.wrapIfContainsSemicolon(viewItem.getPageTitle()),
                    viewItem.getPagePath(),
                    CsvUtil.wrapIfContainsSemicolon(viewItem.getComponentName()),
                    viewItem.getComponentType(),
                    CsvUtil.buildLocation(viewItem.getPath(), viewItem.getPropertyName())
            );
        } catch (IOException e) {
            LOG.error(String.format("Failed to build CSV for the grid resource %s", viewItem.getLink()), e);
        }
    }
}
