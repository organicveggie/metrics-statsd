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
package com.github.mayconbordin.metrics.reporting.statsd;

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
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class StatsDTest {
    InetSocketAddress address = new InetSocketAddress("example.com", 1234);
    UDPSocket socket = mock(UDPSocket.class);
    StatsD statsD = new StatsD(address, socket);

    ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
    ArgumentCaptor<InetSocketAddress> addressCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);
    
    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void connectsToStatsD() throws Exception {
        statsD.connect();

        verify(socket).connect(address, Charset.forName("UTF-8"));
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
    public void testCount() throws Exception {
        statsD.connect();
        statsD.sendCounter("name counter", 42l, 0.10f);
        
        verify(socket, times(1)).send(stringCaptor.capture());
        assertThat(stringCaptor.getValue()).isEqualTo(
                "name-counter:42|c|@0.10");
    }

    @Test
    public void testCountNoSample() throws Exception {
        statsD.connect();
        statsD.sendCounter("name counter", 42l);
        
        verify(socket, times(1)).send(stringCaptor.capture());
        assertThat(stringCaptor.getValue()).isEqualTo(
                "name-counter:42|c");
    }

    @Test
    public void testTiming() throws Exception {
        statsD.connect();
        statsD.sendTiming("timing", 3.2f);
        
        verify(socket, times(1)).send(stringCaptor.capture());
        assertThat(stringCaptor.getValue()).isEqualTo(
                "timing:3.20|ms");
    }

    @Test
    public void writesValuesToStatsD() throws Exception {
        statsD.connect();
        statsD.sendGauge("name", "value");

        verify(socket, times(1)).send(stringCaptor.capture());
        assertThat(stringCaptor.getValue()).isEqualTo("name:value|g");
    }

    @Test
    public void sanitizesNames() throws Exception {
        statsD.connect();
        statsD.sendGauge("name woo", "value");

        verify(socket, times(1)).send(stringCaptor.capture());
        assertThat(stringCaptor.getValue()).isEqualTo("name-woo:value|g");
    }

    @Test
    public void sanitizesValues() throws Exception {
        statsD.connect();
        statsD.sendGauge("name", "value woo");

        verify(socket, times(1)).send(stringCaptor.capture());
        assertThat(stringCaptor.getValue()).isEqualTo("name:value-woo|g");
    }


    @Test
    public void testSendFailure() throws Exception {
        statsD.connect();
        doThrow(new IOException()).when(socket).send(any(String.class));
        
        statsD.sendGauge("name", "value");
        assertThat(statsD.getFailures()).isEqualTo(1);
    }
}
