/**
 * Copyright (C) 2014 metrics-statsd contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.github.jjagged.metrics.reporting.statsd;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Fail.failBecauseExceptionWasNotThrown;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class StatsDTest {
    DatagramSocketFactory socketFactory = mock(DatagramSocketFactory.class);
    InetSocketAddress address = new InetSocketAddress("example.com", 1234);
    StatsD statsD = new StatsD(address, socketFactory);

    DatagramSocket socket = mock(DatagramSocket.class);

    ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<InetSocketAddress> addressCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);

    @Before
    public void setUp() throws Exception {
        when(socketFactory.createSocket()).thenReturn(socket);

        when(socketFactory.createPacket(bytesCaptor.capture(), anyInt(), addressCaptor.capture())).thenCallRealMethod();
    }

    @Test
    public void connectsToStatsD() throws Exception {
        statsD.connect();

        verify(socketFactory).createSocket();
    }

    @Test
    public void measuresFailures() throws Exception {
        assertThat(statsD.getFailures()).isZero();
    }

    @Test
    public void disconnectsFromStatsD() throws Exception {
        statsD.connect();
        statsD.close();

        verify(socket).close();
    }

    @Test
    public void doesNotAllowDoubleConnections() throws Exception {
        statsD.connect();
        try {
            statsD.connect();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("Already connected");
        }
    }

    @Test
    public void testCount() throws Exception {
        statsD.connect();
        statsD.sendCounter("name counter", 42l, 0.10f, null);
        assertThat(new String(bytesCaptor.getValue())).isEqualTo(
                "name-counter:42|c|@0.10");
    }

    @Test
    public void testCountNoSample() throws Exception {
        statsD.connect();
        statsD.sendCounter("name counter", 42l, null, null);
        assertThat(new String(bytesCaptor.getValue())).isEqualTo(
                "name-counter:42|c");
    }

    @Test
    public void testCountTags() throws Exception {
        statsD.connect();
        statsD.sendCounter("name counter", 42l, 0.10f, new String[]{"tag1"});
        assertThat(new String(bytesCaptor.getValue())).isEqualTo(
                "name-counter:42|c|@0.10|#tag1");
    }

    @Test
    public void testTiming() throws Exception {
        statsD.connect();
        statsD.sendTiming("timing", 3.2f, null);
        assertThat(new String(bytesCaptor.getValue())).isEqualTo(
                "timing:3.20|ms");
    }

    @Test
    public void testTimingTags() throws Exception {
        statsD.connect();
        statsD.sendTiming("timing", 3.2f, new String[]{"tag1"});
        assertThat(new String(bytesCaptor.getValue())).isEqualTo(
                "timing:3.20|ms|#tag1");
    }

    @Test
    public void writesValuesToStatsD() throws Exception {
        statsD.connect();
        statsD.sendGauge("name", "value", null);

        assertThat(new String(bytesCaptor.getValue()))
                .isEqualTo("name:value|g");
    }

    @Test
    public void sanitizesNames() throws Exception {
        statsD.connect();
        statsD.sendGauge("name woo", "value", null);

        assertThat(new String(bytesCaptor.getValue())).isEqualTo(
                "name-woo:value|g");
    }

    @Test
    public void sanitizesValues() throws Exception {
        statsD.connect();
        statsD.sendGauge("name", "value woo", null);

        assertThat(new String(bytesCaptor.getValue())).isEqualTo(
                "name:value-woo|g");
    }

    @Test
    public void address() throws IOException {
        statsD.connect();
        statsD.sendGauge("name", "value", null);

        assertThat(addressCaptor.getValue()).isEqualTo(address);
    }
    
    @Test
    public void testTags() throws Exception {
        statsD.connect();
        statsD.sendGauge("name", "value", new String[]{"my", "tags"});
        
        assertThat(new String(bytesCaptor.getValue())).isEqualTo(
                "name:value|g|#my,tags");
    }

    @Test
    public void testEmptyTags() throws Exception {
        statsD.connect();
        statsD.sendGauge("name", "value", new String[]{});

        assertThat(new String(bytesCaptor.getValue())).isEqualTo(
                "name:value|g");
    }

    @Test
    public void testSendFailure() throws Exception {
        statsD.connect();
        doThrow(new IOException()).when(socket).send(any(DatagramPacket.class));
        statsD.sendGauge("name", "value", null);
        assertThat(statsD.getFailures()).isEqualTo(1);
    }
}
