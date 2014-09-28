package com.github.mayconbordin.metrics.reporting.statsd;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
