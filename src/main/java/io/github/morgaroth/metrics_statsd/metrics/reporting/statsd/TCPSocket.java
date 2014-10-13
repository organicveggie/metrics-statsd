package io.github.morgaroth.metrics_statsd.metrics.reporting.statsd;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 *
 * @author mayconbordin
 */
public class TCPSocket implements ISocket {
    private Socket socket;
    private PrintWriter out;

    @Override
    public void connect(InetSocketAddress address) throws IOException {
        connect(address, null);
    }

    @Override
    public void connect(InetSocketAddress address, Charset charset) throws IOException {
        if (socket != null) {
            throw new IllegalStateException("Already connected");
        }
        
        this.socket = new Socket(address.getAddress(), address.getPort());
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void close() throws IOException {
        out.close();
        socket.close();
    }

    @Override
    public void send(String message) throws IOException {
        out.println(message);
    }
    
}
