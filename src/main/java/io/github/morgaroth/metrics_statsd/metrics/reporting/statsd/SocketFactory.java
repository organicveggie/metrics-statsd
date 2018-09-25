package io.github.morgaroth.metrics_statsd.metrics.reporting.statsd;

/**
 *
 * @author mayconbordin
 */
public class SocketFactory {
    public static final String TCP = "tcp";
    public static final String UDP = "udp";
    
    public static ISocket newInstance(String protocol) {
        switch (protocol) {
            case TCP:
                return new TCPSocket();
            case UDP:
                return new UDPSocket();
            default:
                throw new IllegalArgumentException("Protocol "+protocol+" is not supported");
        }
    }
}
