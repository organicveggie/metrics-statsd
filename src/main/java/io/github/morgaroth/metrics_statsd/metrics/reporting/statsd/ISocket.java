package io.github.morgaroth.metrics_statsd.metrics.reporting.statsd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 *
 * @author mayconbordin
 */
public interface ISocket {
    public void connect(InetSocketAddress address) throws IOException;
    public void connect(InetSocketAddress address, Charset charset) throws IOException;
    public void close() throws IOException;
    public void send(String message) throws IOException;
}
