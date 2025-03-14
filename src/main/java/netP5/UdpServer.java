/**
 * A network library for processing which supports UDP, TCP and Multicast.
 *
 * <p>##copyright##
 *
 * <p>This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * <p>This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * <p>You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 *
 * @author ##author##
 * @modified ##date##
 * @version ##version##
 */
package netP5;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import org.apache.log4j.Logger;

public final class UdpServer extends Observable implements Transmitter {

  public static final Logger L = Logger.getLogger(UdpServer.class.getName());
  private static String broadcastAddress;

  private final InternalServer server;

  public UdpServer(final int thePort, final int theDatagramSize) {
    this(null, thePort, theDatagramSize);
  }

  public UdpServer(final String theHost, final int thePort, final int theDatagramSize) {
    /* This is a very basic UDP server listening for incoming message and forwarding the message to all registered
     * observers. This server can be used for simple networking operations with a small amount of clients. For
     * larger scale network operations make use of more sophisticated services such as for example netty.io, apache
     * mina - for NAT traversal consider JSTUN - or use a messaging middleware such as rabbitMQ or the messaging
     * library zeroMQ */

    server = new InternalServer(theHost, thePort, theDatagramSize);
  }

  public boolean close() {
    try {
      server.thread.interrupt();
      server.channel.close();
      return true;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  public boolean send(byte[] theContent) {
    /* TODO send to all clients */
    return false;
  }

  public boolean send(byte[] theContent, Collection<InetSocketAddress> theAddress) {
    InetSocketAddress[] o = new InetSocketAddress[theAddress.size()];
    return send(theContent, theAddress.toArray(o));
  }

  public boolean send(byte[] theContent, String theHost, int thePort) {
    return send(theContent, new InetSocketAddress(theHost, thePort));
  }

  public boolean send(byte[] theContent, SocketAddress... theAddress) {
    try {

      ByteBuffer buffer = ByteBuffer.allocate(theContent.length);
      ((Buffer) buffer).clear();
      buffer.put(theContent);
      ((Buffer) buffer).flip();
      for (SocketAddress addr : theAddress) {
        String remoreAddress = ((InetSocketAddress) addr).getAddress().toString().split("/")[1];
        try {
          if (remoreAddress.equals(broadcastAddress)) {
            server.channel.socket().setBroadcast(true);
          }
          server.channel.send(buffer, addr);
        } catch (Exception e2) {
          L.error("Could not send datagram " + e2.toString() + ": socket = " + remoreAddress);
        }
      }
      return true;
    } catch (Exception e) {
      L.error("Could not send datagram " + e);
    }
    return false;
  }

  class InternalServer implements Runnable {
    private DatagramChannel channel;
    private final int port;
    private final int size;
    private final Thread thread;
    private final String host;

    InternalServer(String theHost, int thePort, int theDatagramSize) {
      host = theHost;
      port = thePort;
      size = theDatagramSize;
      thread = (new Thread(this));
      thread.start();
    }

    public void run() {
      /* Create a selector to multiplex client connections. */
      try {
        Selector selector = SelectorProvider.provider().openSelector();
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        InetSocketAddress isa =
            (host == null) ? new InetSocketAddress(port) : new InetSocketAddress(host, port);
        channel.socket().bind(isa);
        channel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(size));
        L.info(
            "starting server, listening on port "
                + port
                + " ("
                + isa.getAddress().getHostAddress()
                + ":"
                + isa.getPort()
                + " "
                + isa.getAddress().getLocalHost()
                + ":"
                + isa.getPort()
                + ")");

        /* Let's listen for incoming messages */
        while (!Thread.currentThread().isInterrupted()) {
          /* Wait for task or until timeout expires */
          int timeout = 1000;
          if (selector.select(timeout) == 0) {
            /* just checking if we are still alive. */
            continue;
          }

          /* Get iterator on set of keys with I/O to process */

          Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
          while (keyIter.hasNext()) {
            SelectionKey key = keyIter.next(); /* Key is bit mask */
            /* Client socket channel has pending data? */
            if (key.isReadable()) {

              DatagramChannel channel0 = (DatagramChannel) key.channel();
              ByteBuffer buffer = ((ByteBuffer) key.attachment());
              ((Buffer) buffer).clear(); /* Prepare buffer for receiving */
              SocketAddress client = channel0.receive(buffer);
              InetSocketAddress addr = (InetSocketAddress) client;

              if (client != null) {
                /* handle received message */
                ((Buffer) buffer).flip();
                final Map<String, Object> m = new HashMap<String, Object>();
                final byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                DatagramSocket socket = channel0.socket();
                m.put("socket-type", "udp");
                m.put("socket-ref", channel0);
                m.put("received-at", System.currentTimeMillis());
                m.put("socket-address", addr.getAddress().getHostAddress());
                m.put("socket-port", addr.getPort());
                m.put("local-port", socket.getLocalPort());
                m.put("data", data);
                setChanged();
                notifyObservers(m);
              }
            }

            keyIter.remove();
          }
        }
      } catch (IOException e) {
        L.warn(
            "Couldn't start UDP server on port "
                + port
                + " "
                + e
                + " Is there another application using the same port?");
      }

      L.info("thread interrupted and closed.");
    }
  }

  /* TODO consider to use java.util.concurrent.Executor here instead of Thread. */

  /** */
  public void setBroadcastAddress(String _broadcastAddress) {
    broadcastAddress = _broadcastAddress;
  }
}
