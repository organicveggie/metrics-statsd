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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * A client to a StatsD server.
 * Note that the current version (1d9d9bf32aa5c7fe4f48d61165bed805cc8f3480) of etsy/statsd does not do anything with
 * tags; this code still accepts and sends them.
 */
@NotThreadSafe
public class StatsD implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(StatsD.class);

    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final DatagramSocketFactory socketFactory;

    private InetSocketAddress address;
    private DatagramSocket socket;
    private int failures;

    /**
     * Creates a new client which connects to the given address using the
     * default {@link DatagramSocketFactory}.
     * 
     * @param host
     *            the hostname of the StatsD server.
     * @param port
     *            the port of the StatsD server. This is typically 8125.
     */
    public StatsD(final String host, final int port) {
        this(new InetSocketAddress(host, port), new DatagramSocketFactory());
    }

    /**
     * Creates a new client which connects to the given address and socket
     * factory.
     * 
     * @param address
     *            the address of the StatsD server
     * @param socketFactory
     *            the socket factory
     */
    public StatsD(final InetSocketAddress address, final DatagramSocketFactory socketFactory) {
        this.address = address;
        this.socketFactory = socketFactory;
    }

    /**
     * Resolves the address hostname if present.
     * <p/>
     * Creates a datagram socket through the factory.
     * 
     * @throws IllegalStateException
     *             if the client is already connected
     * @throws IOException
     *             if there is an error connecting
     */
    public void connect() throws IOException {
        if (socket != null) {
            throw new IllegalStateException("Already connected");
        }

        if (address.getHostName() != null) {
            this.address = new InetSocketAddress(address.getHostName(), address.getPort());
        }

        this.socket = socketFactory.createSocket();
    }

    private void sendBytes(String data) {
        try {
            byte[] bytes = data.getBytes(UTF_8);
            socket.send(socketFactory.createPacket(bytes, bytes.length, address));
            failures = 0;
        } catch (IOException e) {
            failures++;

            if (failures == 1) {
                LOG.warn("unable to send packet to statsd at '{}:{}'",
                        address.getHostName(), address.getPort());
            } else {
                LOG.debug("unable to send packet to statsd at '{}:{}'",
                        address.getHostName(), address.getPort());
            }
        }
    }

    /**
     * Sends the given measurement to the server. Logs exceptions.
     * 
     * @param name
     *            the name of the metric
     * @param value
     *            the value of the metric
     * @param tags
     *            tags to add after |# ; etsy/statsd doesn't seem to support these.
     */
    public void sendGauge(final String name, final String value, @Nullable final String[] tags) {
        // foo:103|g
        String formatted = String.format("%s:%s|g%s", sanitize(name), sanitize(value), buildTags(tags));
        sendBytes(formatted);
    }

    /**
     * send a counter to statsd
     * @param name the name of the metric
     * @param value the amount to count
     * @param sample the sample fraction (between 0 and 1, or null)
     * @param tags the tags
     */
    public void sendCounter(final String name, final long value, @Nullable Float sample, @Nullable final String[] tags) {
        // foo:1c
        //foo:1c@0.1
        String formatted = String.format("%s:%d|c%s%s", sanitize(name), value, buildSample(sample),
                buildTags(tags));
        sendBytes(formatted);
    }

    /**
     * send a timing duration
     * @param name the name of the metric
     * @param value the time value in milliseconds.
     */
    public void sendTiming(final String name, final float value, @Nullable final String[] tags) {
        //glork:320|ms
        String formatted = String.format("%s:%.2f|ms%s", sanitize(name), value, buildTags(tags));
        sendBytes(formatted);
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
        this.socket = null;
    }

    private String sanitize(final String s) {
        return WHITESPACE.matcher(s).replaceAll("-");
    }

    private String buildTags(@Nullable final String[] tags) {
        if (tags == null || tags.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder("|#");
        for (int i = 0; i < tags.length; i++) {
            sb.append(tags[i]);
            if (i < tags.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }


    private String buildSample(@Nullable Float sample) {
        if (sample == null) {
            return "";
        }
        return String.format("|@%.2f", sample);
    }
}
