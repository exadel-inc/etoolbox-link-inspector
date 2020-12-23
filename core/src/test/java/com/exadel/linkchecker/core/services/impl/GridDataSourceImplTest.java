package com.exadel.linkchecker.core.services.impl;

import com.exadel.linkchecker.core.services.data.DataFeedService;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class GridDataSourceImplTest {
    private static final String DATAFEED_SERVICE_FIELD = "dataFeedService";

    private final GridDataSourceImpl gridDataSource = new GridDataSourceImpl();

    private DataFeedService dataFeedService;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        dataFeedService = mock(DataFeedService.class);
        PrivateAccessor.setField(gridDataSource, DATAFEED_SERVICE_FIELD, dataFeedService);
    }

    @Test
    void testGetDataSource() {
        when(dataFeedService.dataFeedToResources()).thenReturn(Collections.emptyList());

        assertNotNull(gridDataSource.getDataSource());
    }
}