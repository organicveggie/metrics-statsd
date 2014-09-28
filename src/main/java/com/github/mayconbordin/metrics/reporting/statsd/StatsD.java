/**
 * Copyright (C) 2013 metrics-statsd contributors
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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.regex.Pattern;
import javax.annotation.concurrent.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A client to a StatsD server.
 */
@NotThreadSafe
public class StatsD implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(StatsD.class);
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private InetSocketAddress address;
    private ISocket socket;
    private int failures;

    /**
     * Creates a new client which connects to the given address with UDP protocol.
     *
     * @param host the hostname of the StatsD server.
     * @param port the port of the StatsD server. This is typically 8125.
     */
    public StatsD(final String host, final int port) {
        this(host, port, SocketFactory.UDP);
    }
    
    /**
     * Creates a new client which connects to the given address with the given protocol.
     *
     * @param host     the hostname of the StatsD server.
     * @param port     the port of the StatsD server. This is typically 8125.
     * @param protocol the protocol of the StatsD server.
     */
    public StatsD(final String host, final int port, final String protocol) {
        this(new InetSocketAddress(host, port), protocol);
    }

    /**
     * Creates a new client which connects to the given address and socket factory.
     *
     * @param address  the address of the Carbon server
     * @param protocol the socket protocol
     */
    public StatsD(final InetSocketAddress address, final String protocol) {
        this.address = address;
        this.socket = SocketFactory.newInstance(protocol);
    }
    
    /**
     * Creates a new client which connects to the given address and socket factory.
     *
     * @param address  the address of the Carbon server
     * @param socket   the socket
     */
    public StatsD(final InetSocketAddress address, ISocket socket) {
        this.address = address;
        this.socket = socket;
    }

    /**
     * Resolves the address hostname if present.
     * <p/>
     * Creates a datagram socket through the factory.
     *
     * @throws IllegalStateException if the client is already connected
     * @throws IOException           if there is an error connecting
     */
    public void connect() throws IOException, IllegalStateException {
        if (address.getHostName() != null) {
            this.address = new InetSocketAddress(address.getHostName(), address.getPort());
        }
        
        socket.connect(address, UTF_8);
    }

    /**
     * Sends the gauge to the server.
     * @param name  the name of the gauge
     * @param value the value of the gauge
     */
    public void sendGauge(final String name, final String value) {
        String formatted = String.format("%s:%s|g", sanitize(name), sanitize(value));
        send(formatted);
    }
    
    /**
     * Sends the counter to the server.
     * @param name  the name of the counter
     * @param value the amount by which the counter will be incremented
     */
    public void sendCounter(final String name, final long value) {
        String formatted = String.format("%s:%d|c", sanitize(name), value);
        send(formatted);
    }
    
    /**
     * Sends the counter to the server.
     * @param name       the name of the counter
     * @param value      the amount by which the counter will be incremented
     * @param sampleRate the rate by which the counter is being sampled (between 0 and 1)
     */
    public void sendCounter(final String name, final long value, final float sampleRate) {
        if (sampleRate < 0 || sampleRate > 1) {
            throw new IllegalArgumentException("Sample rate should be a value between 0 and 1");
        }
        
        String formatted = String.format("%s:%d|c|@%.2f", sanitize(name), value, sampleRate);
        send(formatted);
    }
    
    /**
     * Sends a timing duration.
     * @param name  the name of the timer
     * @param value the duration of the timing in milliseconds
     */
    public void sendTiming(final String name, final float value) {
        String formatted = String.format("%s:%.2f|ms", sanitize(name), value);
        send(formatted);
    }
    
    /**
     * Sends the formatted message to the statsd server.
     * @param message the message to be send
     */
    private void send(final String message) {
        try {
            socket.send(message);
            failures = 0;
        } catch (IOException e) {
            failures++;
            
            String msg = String.format("Unable to send packet to statsd at '%s:%s'", address.getHostName(), address.getPort());

            if (failures == 1) {
                LOG.warn(msg, e);
            } else {
                LOG.debug(msg, e);
            }
        }
    }

    /**
     * Returns the number of failed writes to the server.
     *
     * @return the number of failed writes to the server
     */
    public int getFailures() {
        return failures;
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
        socket = null;
    }

    private String sanitize(final String s) {
        return WHITESPACE.matcher(s).replaceAll("-");
    }
}