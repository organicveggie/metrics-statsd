package io.github.morgaroth.metrics_statsd.metrics.reporting.statsd;

import io.github.morgaroth.metrics_statsd.metrics.reporting.statsd.ISocket;
import io.github.morgaroth.metrics_statsd.metrics.reporting.statsd.SocketFactory;
import io.github.morgaroth.metrics_statsd.metrics.reporting.statsd.TCPSocket;
import io.github.morgaroth.metrics_statsd.metrics.reporting.statsd.UDPSocket;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mayconbordin
 */
public class SocketFactoryTest {

    @Test
    public void testNewUDPInstance() {
        ISocket result = SocketFactory.newInstance(SocketFactory.UDP);
        assertEquals(result.getClass(), UDPSocket.class);
    }
    
    @Test
    public void testNewTCPInstance() {
        ISocket result = SocketFactory.newInstance(SocketFactory.TCP);
        assertEquals(result.getClass(), TCPSocket.class);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNewInvalidInstance() {
        SocketFactory.newInstance("");
    }
}
