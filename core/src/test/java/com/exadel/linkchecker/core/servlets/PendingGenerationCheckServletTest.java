package com.exadel.linkchecker.core.servlets;

import com.exadel.linkchecker.core.services.helpers.RepositoryHelper;
import com.exadel.linkchecker.core.services.util.constants.CommonConstants;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import junitx.util.PrivateAccessor;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(AemContextExtension.class)
class PendingGenerationCheckServletTest {
    private static final String REPOSITORY_HELPER_FIELD = "repositoryHelper";

    private final AemContext context = new AemContext();

    private final PendingGenerationCheckServlet fixture = new PendingGenerationCheckServlet();

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;

    @BeforeEach
    void setup() throws NoSuchFieldException {
        RepositoryHelper repositoryHelper = mock(RepositoryHelper.class);
        PrivateAccessor.setField(fixture, REPOSITORY_HELPER_FIELD, repositoryHelper);

        when(repositoryHelper.getServiceResourceResolver()).thenReturn(context.resourceResolver());

        request = context.request();
        response = context.response();
    }

    @Test
    void testPendingNodePresent() {
        context.create().resource(CommonConstants.PENDING_GENERATION_NODE);

        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    @Test
    void testPendingNodeAbsent() {
        fixture.doPost(request, response);

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());
    }
}