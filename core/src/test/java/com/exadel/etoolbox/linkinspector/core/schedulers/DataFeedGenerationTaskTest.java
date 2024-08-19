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

package com.exadel.etoolbox.linkinspector.core.schedulers;

import com.exadel.etoolbox.contractor.ContractorException;
import com.exadel.etoolbox.contractor.service.tasking.Contractor;
import com.exadel.etoolbox.linkinspector.core.services.job.DataFeedJobExecutor;
import junitx.util.PrivateAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataFeedGenerationTaskTest {

    private final DataFeedGenerationTask fixture = new DataFeedGenerationTask();
    private final Contractor contractor = mock(Contractor.class);

    @BeforeEach
    void setup() throws NoSuchFieldException {
        PrivateAccessor.setField(fixture, "contractor", contractor);
    }

    @Test
    void testRun() throws ContractorException {
        DataFeedGenerationTask.Config config = mock(DataFeedGenerationTask.Config.class);
        when(config.enabled()).thenReturn(true);

        AtomicReference<String> runTopic = new AtomicReference<>();
        when(contractor.runExclusive(anyString())).then(invocation -> {
            runTopic.set(invocation.getArgument(0));
            return null;
        });

        fixture.activate(config);
        fixture.run();
        assertEquals(DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC, runTopic.get());
    }

    @Test
    void testRunNotEnabled() throws ContractorException {
        DataFeedGenerationTask.Config config = mock(DataFeedGenerationTask.Config.class);
        when(config.enabled()).thenReturn(false);

        AtomicReference<String> runTopic = new AtomicReference<>();
        when(contractor.runExclusive(anyString())).then(invocation -> {
            runTopic.set(invocation.getArgument(0));
            return null;
        });

        fixture.activate(config);
        fixture.run();
        assertNull(runTopic.get());
    }

    @Test
    void testDeactivate() {
        AtomicReference<String> discardTopic = new AtomicReference<>();
        when(contractor.discardAll(anyString())).then(invocation -> {
            discardTopic.set(invocation.getArgument(0));
            return null;
        });

        fixture.deactivate();
        assertEquals(DataFeedJobExecutor.GENERATE_DATA_FEED_TOPIC, discardTopic.get());
    }
}