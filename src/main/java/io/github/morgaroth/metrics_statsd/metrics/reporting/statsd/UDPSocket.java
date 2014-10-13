package io.github.morgaroth.metrics_statsd.metrics.reporting.statsd;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mayconbordin
 */
public class UDPSocket implements ISocket {
    private static final Logger LOG = LoggerFactory.getLogger(UDPSocket.class);
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private DatagramSocket socket;
    private InetSocketAddress address;
    private Charset charset;
    
    @Override
    public void connect(InetSocketAddress address, Charset charset) throws IOException {
        if (socket != null) {
            throw new IllegalStateException("Already connected");
        }
        
        this.address = address;
        this.charset = charset;
        this.socket = new DatagramSocket();
    }

    @Override
    public void connect(InetSocketAddress address) throws IOException {
        this.connect(address, UTF_8);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    public void send(String message) throws IOException {
        byte[] bytes = message.getBytes(charset);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address);
        socket.send(packet);
    }
    
}
