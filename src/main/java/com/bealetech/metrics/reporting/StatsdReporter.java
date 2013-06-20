/**
 * Copyright (C) 2012-2013 Sean Laurent
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
package com.bealetech.metrics.reporting;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.AbstractPollingReporter;
import com.yammer.metrics.stats.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class StatsdReporter extends AbstractPollingReporter implements MetricProcessor<Long> {

    private static final int MAX_UDPDATAGRAM_LENGTH = 1300; // In reality, usually closer to 1500

    public static enum StatType { COUNTER, TIMER, GAUGE }

    private static final Logger LOG = LoggerFactory.getLogger(StatsdReporter.class);

    protected final String prefix;
    protected final MetricPredicate predicate;
    protected final Locale locale = Locale.US;
    protected final Clock clock;

    protected final UDPSocketProvider socketProvider;
    protected DatagramSocket currentSocket = null;

    protected final VirtualMachineMetrics vm;

    protected Writer writer;
    protected ByteArrayOutputStream outputData;

    private boolean prependNewline = false;
    private boolean printVMMetrics = true;

    public interface UDPSocketProvider {
        DatagramSocket get() throws Exception;
        DatagramPacket newPacket(ByteArrayOutputStream out);
    }

    public StatsdReporter(String host, int port) throws IOException {
        this(Metrics.defaultRegistry(), host, port, null);
    }

    public StatsdReporter(String host, int port, String prefix) throws IOException {
        this(Metrics.defaultRegistry(), host, port, prefix);
    }

    public StatsdReporter(MetricsRegistry metricsRegistry, String host, int port) throws IOException {
        this(metricsRegistry, host, port, null);
    }

    public StatsdReporter(MetricsRegistry metricsRegistry, String host, int port, String prefix) throws IOException {
        this(metricsRegistry,
             prefix,
             MetricPredicate.ALL,
             new DefaultSocketProvider(host, port),
             Clock.defaultClock());
    }

    public StatsdReporter(MetricsRegistry metricsRegistry, String prefix, MetricPredicate predicate, UDPSocketProvider socketProvider, Clock clock) throws IOException {
        this(metricsRegistry, prefix, predicate, socketProvider, clock, VirtualMachineMetrics.getInstance());
    }

    public StatsdReporter(MetricsRegistry metricsRegistry, String prefix, MetricPredicate predicate, UDPSocketProvider socketProvider, Clock clock, VirtualMachineMetrics vm) throws IOException {
        this(metricsRegistry, prefix, predicate, socketProvider, clock, vm, "graphite-reporter");
    }

    public StatsdReporter(MetricsRegistry metricsRegistry, String prefix, MetricPredicate predicate, UDPSocketProvider socketProvider, Clock clock, VirtualMachineMetrics vm, String name) throws IOException {
        super(metricsRegistry, name);

        this.socketProvider = socketProvider;
        this.vm = vm;

        this.clock = clock;

        if (prefix != null) {
            // Pre-append the "." so that we don't need to make anything conditional later.
            this.prefix = prefix + ".";
        } else {
            this.prefix = "";
        }
        this.predicate = predicate;
        this.outputData = new ByteArrayOutputStream();
    }

    public boolean isPrintVMMetrics() {
        return printVMMetrics;
    }

    public void setPrintVMMetrics(boolean printVMMetrics) {
        this.printVMMetrics = printVMMetrics;
    }

    @Override
    public void run() {

        try {
            currentSocket = this.socketProvider.get();
            resetWriterState();

            final long epoch = clock.time() / 1000;
            if (printVMMetrics) {
                printVmMetrics(epoch);
            }
            printRegularMetrics(epoch);

            // Send UDP data
            sendDatagram();
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Error writing to Graphite", e);
            } else {
                LOG.warn("Error writing to Graphite: {}", e.getMessage());
            }
            if (writer != null) {
                try {
                    writer.flush();
                } catch (IOException e1) {
                    LOG.error("Error while flushing writer:", e1);
                }
            }
        } finally {
            if (currentSocket != null) {
                currentSocket.close();
            }
            writer = null;
        }
    }

    private void resetWriterState() {
        outputData.reset();
        prependNewline = false;
        writer = new BufferedWriter(new OutputStreamWriter(this.outputData));
    }

    private void sendDatagram() throws IOException {
        writer.flush();
        if (outputData.size() > 0) { // Don't send an empty datagram
            DatagramPacket packet = this.socketProvider.newPacket(outputData);
            packet.setData(outputData.toByteArray());
            currentSocket.send(packet);
        }
    }

    protected void printVmMetrics(long epoch) {
        // Memory
        sendFloat("jvm.memory.totalInit", StatType.GAUGE, vm.totalInit());
        sendFloat("jvm.memory.totalUsed", StatType.GAUGE, vm.totalUsed());
        sendFloat("jvm.memory.totalMax", StatType.GAUGE, vm.totalMax());
        sendFloat("jvm.memory.totalCommitted", StatType.GAUGE, vm.totalCommitted());

        sendFloat("jvm.memory.heapInit", StatType.GAUGE, vm.heapInit());
        sendFloat("jvm.memory.heapUsed", StatType.GAUGE, vm.heapUsed());
        sendFloat("jvm.memory.heapMax", StatType.GAUGE, vm.heapMax());
        sendFloat("jvm.memory.heapCommitted", StatType.GAUGE, vm.heapCommitted());

        sendFloat("jvm.memory.heapUsage", StatType.GAUGE, vm.heapUsage());
        sendFloat("jvm.memory.nonHeapUsage", StatType.GAUGE, vm.nonHeapUsage());

        for (Map.Entry<String, Double> pool : vm.memoryPoolUsage().entrySet()) {
            sendFloat("jvm.memory.memory_pool_usages." + sanitizeString(pool.getKey()), StatType.GAUGE, pool.getValue());
        }

        // Buffer Pool
        final Map<String, VirtualMachineMetrics.BufferPoolStats> bufferPoolStats = vm.getBufferPoolStats();
        if (!bufferPoolStats.isEmpty()) {
            sendFloat("jvm.buffers.direct.count", StatType.GAUGE, bufferPoolStats.get("direct").getCount());
            sendFloat("jvm.buffers.direct.memoryUsed", StatType.GAUGE, bufferPoolStats.get("direct").getMemoryUsed());
            sendFloat("jvm.buffers.direct.totalCapacity", StatType.GAUGE, bufferPoolStats.get("direct").getTotalCapacity());

            sendFloat("jvm.buffers.mapped.count", StatType.GAUGE, bufferPoolStats.get("mapped").getCount());
            sendFloat("jvm.buffers.mapped.memoryUsed", StatType.GAUGE, bufferPoolStats.get("mapped").getMemoryUsed());
            sendFloat("jvm.buffers.mapped.totalCapacity", StatType.GAUGE, bufferPoolStats.get("mapped").getTotalCapacity());
        }

        sendInt("jvm.daemon_thread_count", StatType.GAUGE, vm.daemonThreadCount());
        sendInt("jvm.thread_count", StatType.GAUGE, vm.threadCount());
        sendInt("jvm.uptime", StatType.GAUGE, vm.uptime());
        sendFloat("jvm.fd_usage", StatType.GAUGE, vm.fileDescriptorUsage());

        for (Map.Entry<Thread.State, Double> entry : vm.threadStatePercentages().entrySet()) {
            sendFloat("jvm.thread-states." + entry.getKey().toString().toLowerCase(), StatType.GAUGE, entry.getValue());
        }

        for (Map.Entry<String, VirtualMachineMetrics.GarbageCollectorStats> entry : vm.garbageCollectors().entrySet()) {
            final String name = "jvm.gc." + sanitizeString(entry.getKey());
            sendInt(name + ".time", StatType.GAUGE, entry.getValue().getTime(TimeUnit.MILLISECONDS));
            sendInt(name + ".runs", StatType.GAUGE, entry.getValue().getRuns());
        }
    }

    protected void printRegularMetrics(long epoch) {
        for (Map.Entry<String,SortedMap<MetricName,Metric>> entry : getMetricsRegistry().groupedMetrics(predicate).entrySet()) {
            for (Map.Entry<MetricName, Metric> subEntry : entry.getValue().entrySet()) {
                final Metric metric = subEntry.getValue();
                if (metric != null) {
                    try {
                        metric.processWith(this, subEntry.getKey(), epoch);
                    } catch (Exception ignored) {
                        LOG.error("Error printing regular metrics:", ignored);
                    }
                }
            }
        }
    }

    @Override
    public void processMeter(MetricName name, Metered meter, Long epoch) throws Exception {
        final String sanitizedName = sanitizeName(name);
        sendInt(sanitizedName + ".count", StatType.GAUGE, meter.count());
        sendFloat(sanitizedName + ".meanRate", StatType.TIMER, meter.meanRate());
        sendFloat(sanitizedName + ".1MinuteRate", StatType.TIMER, meter.oneMinuteRate());
        sendFloat(sanitizedName + ".5MinuteRate", StatType.TIMER, meter.fiveMinuteRate());
        sendFloat(sanitizedName + ".15MinuteRate", StatType.TIMER, meter.fifteenMinuteRate());
    }

    @Override
    public void processCounter(MetricName name, Counter counter, Long epoch) throws Exception {
        sendInt(sanitizeName(name) + ".count", StatType.GAUGE, counter.count());
    }

    @Override
    public void processHistogram(MetricName name, Histogram histogram, Long epoch) throws Exception {
        final String sanitizedName = sanitizeName(name);
        sendSummarizable(sanitizedName, histogram);
        sendSampling(sanitizedName, histogram);
    }

    @Override
    public void processTimer(MetricName name, Timer timer, Long epoch) throws Exception {
        processMeter(name, timer, epoch);
        final String sanitizedName = sanitizeName(name);
        sendSummarizable(sanitizedName, timer);
        sendSampling(sanitizedName, timer);
    }

    @Override
    public void processGauge(MetricName name, Gauge<?> gauge, Long epoch) throws Exception {
        sendObj(sanitizeName(name) + ".count", StatType.GAUGE, gauge.value());
    }

    protected void sendSummarizable(String sanitizedName, Summarizable metric) throws IOException {
        sendFloat(sanitizedName + ".min", StatType.TIMER, metric.min());
        sendFloat(sanitizedName + ".max", StatType.TIMER, metric.max());
        sendFloat(sanitizedName + ".mean", StatType.TIMER, metric.mean());
        sendFloat(sanitizedName + ".stddev", StatType.TIMER, metric.stdDev());
    }

    protected void sendSampling(String sanitizedName, Sampling metric) throws IOException {
        final Snapshot snapshot = metric.getSnapshot();
        sendFloat(sanitizedName + ".median", StatType.TIMER, snapshot.getMedian());
        sendFloat(sanitizedName + ".75percentile", StatType.TIMER, snapshot.get75thPercentile());
        sendFloat(sanitizedName + ".95percentile", StatType.TIMER, snapshot.get95thPercentile());
        sendFloat(sanitizedName + ".98percentile", StatType.TIMER, snapshot.get98thPercentile());
        sendFloat(sanitizedName + ".99percentile", StatType.TIMER, snapshot.get99thPercentile());
        sendFloat(sanitizedName + ".999percentile", StatType.TIMER, snapshot.get999thPercentile());
    }


    protected void sendInt(String name, StatType statType, long value) {
        sendData(name, String.format(locale, "%d", value), statType);
    }

    protected void sendFloat(String name, StatType statType, double value) {
        sendData(name, String.format(locale, "%2.2f", value), statType);
    }

    protected void sendObj(String name, StatType statType, Object value) {
        sendData(name, String.format(locale, "%s", value), statType);
    }

    protected String sanitizeName(MetricName name) {
        final StringBuilder sb = new StringBuilder()
                .append(name.getGroup())
                .append('.')
                .append(name.getType())
                .append('.');
        if (name.hasScope()) {
            sb.append(name.getScope())
                    .append('.');
        }
        return sb.append(name.getName()).toString();
    }

    protected String sanitizeString(String s) {
        return s.replace(' ', '-');
    }

    protected void sendData(String name, String value, StatType statType) {
        String statTypeStr = "";
        switch (statType) {
            case COUNTER:
                statTypeStr = "c";
                break;
            case GAUGE:
                statTypeStr = "g";
                break;
            case TIMER:
                statTypeStr = "ms";
                break;
        }

        try {
            if (prependNewline) {
                writer.write("\n");
            }
            if (!prefix.isEmpty()) {
                writer.write(prefix);
            }
            writer.write(sanitizeString(name));
            writer.write(":");
            writer.write(value);
            writer.write("|");
            writer.write(statTypeStr);
            prependNewline = true;
            writer.flush();

            if (outputData.size() > MAX_UDPDATAGRAM_LENGTH) {
                // Need to send our UDP packet now before it gets too big.
                sendDatagram();
                resetWriterState();
            }
        } catch (IOException e) {
            LOG.error("Error sending to Graphite:", e);
        }
    }

    public static class DefaultSocketProvider implements UDPSocketProvider {

        private final String host;
        private final int port;

        public DefaultSocketProvider(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public DatagramSocket get() throws Exception {
            return new DatagramSocket();
        }

        @Override
        public DatagramPacket newPacket(ByteArrayOutputStream out) {
            byte[] dataBuffer;

            if (out != null) {
                dataBuffer = out.toByteArray();
            }
            else {
                dataBuffer = new byte[8192];
            }

            try {
                return new DatagramPacket(dataBuffer, dataBuffer.length, InetAddress.getByName(this.host), this.port);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
