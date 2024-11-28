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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import org.apache.log4j.Logger;

public class Multicast extends Observable implements Transmitter {

  // URL References for Multicast usage and examples:
  // [MulticastSocket
  // API](http://download.java.net/jdk7/archive/b123/docs/api/java/net/MulticastSocket.html)
  // [Multicast Questions](http://stackoverflow.com/questions/5072028/multicast-in-java)
  // [Multicasting from Mobile to
  // PC](http://stackoverflow.com/questions/15807733/udp-multicasting-from-mobile-to-pc)

  // Multicast address ranges and their uses:
  // Reserved [IANA]: 235.0.0.0-238.255.255.255
  // Organization-Local Scope: 239.0.0.0-239.255.255.255 (e.g., 239.1.1.1)

  private Thread thread;

  // Logger for this class
  public static Logger logger;

  // Multicast socket for communication
  private MulticastSocket socket;

  // Address of the multicast group
  private InetAddress address;

  // Multicast group address as a string
  private final String group;

  // Port for multicast communication
  private final int port;

  // List to store the local IP addresses of the host machine
  private final List<String> self;

  /**
   * Constructor to initialize the Multicast instance with default values for datagram size and TTL.
   *
   * @param theGroup The multicast group address.
   * @param thePort The port for multicast communication.
   */
  public Multicast(final String theGroup, final int thePort) {
    this(theGroup, thePort, 256, 1);
  }

  /**
   * Constructor to initialize the Multicast instance with specified values.
   *
   * @param theGroup The multicast group address.
   * @param thePort The port for multicast communication.
   * @param theDatagramSize The size of the datagram packets.
   * @param theTTL The Time To Live (TTL) value for the multicast packets.
   */
  public Multicast(
      final String theGroup, final int thePort, final int theDatagramSize, final int theTTL) {

    group = theGroup;
    port = thePort;
    self = new ArrayList<String>();

    try {
      // Create and configure the multicast socket
      socket = new MulticastSocket(thePort);
      address = InetAddress.getByName(theGroup);
      socket.joinGroup(address); // Join the multicast group

      final byte[] inBuf = new byte[theDatagramSize];

      // Discover local network interfaces and their IP addresses
      try {
        String ip;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
          NetworkInterface iface = interfaces.nextElement();
          // Skip loopback and inactive interfaces
          if (iface.isLoopback() || !iface.isUp()) continue;

          Enumeration<InetAddress> addresses = iface.getInetAddresses();
          while (addresses.hasMoreElements()) {
            InetAddress addr = addresses.nextElement();
            ip = addr.getHostAddress();
            System.out.println(iface.getDisplayName() + " " + ip);
            self.add(ip); // Add local IP to the list
          }
        }
      } catch (SocketException e) {
        throw new RuntimeException(e); // Handle network interface issues
      }

      // Runnable task to listen for incoming multicast packets
      Runnable server =
          new Runnable() {

            DatagramPacket inPacket = null;

            public void run() {
              setTimeToLive(theTTL); // Set the TTL for multicast packets
              while (true) {
                inPacket = new DatagramPacket(inBuf, inBuf.length);
                try {
                  socket.receive(inPacket); // Receive the incoming packet
                  byte[] data = new byte[inPacket.getLength()];
                  System.arraycopy(inBuf, 0, data, 0, data.length);
                  final Map<String, Object> m = new HashMap<String, Object>();
                  // Package received data along with metadata
                  m.put("socket-type", "multicast");
                  m.put("socket-ref", socket);
                  m.put("received-at", System.currentTimeMillis());
                  m.put("multicast-group", group);
                  m.put("multicast-sender", inPacket.getAddress().getHostAddress());
                  m.put("multicast-port", port);
                  m.put("data", data);
                  setChanged(); // Mark the Observable as changed
                  notifyObservers(m); // Notify observers with the received data
                } catch (IOException e) {
                  e.printStackTrace(); // Handle I/O errors during packet reception
                }
              }
            }
          };

      thread = new Thread(server);
      thread.start(); // Start the listener thread
    } catch (Exception e) {
      // Handle any initialization errors
    }
  }

  /**
   * Gets the multicast group address.
   *
   * @return The multicast group address.
   */
  public String getGroup() {
    return group;
  }

  /**
   * Gets the list of local IP addresses associated with the machine.
   *
   * @return A list of local IP addresses.
   */
  public List<String> getSelf() {
    return self;
  }

  /**
   * Checks if the sender of the multicast message is one of the local IP addresses.
   *
   * @param m A map containing multicast message details.
   * @return True if the sender is one of the local IPs, otherwise false.
   */
  public boolean isSelf(Map<String, Object> m) {
    Object o = m.get("multicast-sender");
    return o == null ? false : self.contains(o.toString());
  }

  /**
   * Sets the Time To Live (TTL) value for multicast packets.
   *
   * @param theTTL The TTL value to set.
   * @return The current Multicast instance for method chaining.
   */
  public Multicast setTimeToLive(int theTTL) {
    try {
      socket.setTimeToLive(theTTL); // Set the TTL for outgoing multicast packets
    } catch (IOException e) {
      e.printStackTrace(); // Handle I/O errors while setting TTL
    }
    return this;
  }

  /**
   * Sends a multicast message with the specified content.
   *
   * @param theContent The content of the message to send.
   * @return True if the message was sent successfully, otherwise false.
   */
  public boolean send(byte[] theContent) {
    try {
      socket.send(
          new DatagramPacket(theContent, theContent.length, address, port)); // Send the packet
      return true;
    } catch (IOException e) {
      e.printStackTrace(); // Handle errors during message sending
    }
    return false;
  }

  // Other send methods are not implemented and are currently using the default send(byte[]) method.
  public boolean send(byte[] theContent, Collection<InetSocketAddress> theAddress) {
    logging("info", "not implemented, use send(byte[]).");
    return false;
  }

  public boolean send(byte[] theContent, String theHost, int thePort) {
    logging("info", "not implemented, use send(byte[]).");
    return false;
  }

  public boolean send(byte[] theContent, SocketAddress... theAddress) {
    logging("info", "not implemented, use send(byte[]).");
    return false;
  }

  /**
   * Closes the multicast socket and interrupts the listener thread.
   *
   * @return False to indicate that the socket is closed, as there is no return value expected.
   */
  public boolean close() {
    thread.interrupt(); // Interrupt the listener thread
    try {
      socket.leaveGroup(socket.getInetAddress()); // Leave the multicast group
    } catch (IOException e) {
      e.printStackTrace(); // Handle errors while leaving the group
    }
    socket.close(); // Close the socket
    return false;
  }

  // TODO: Implement logger setting and custom logging functionality
  public void setLogger(Logger _logger) {
    logger = _logger;
  }

  // TODO
  private void logging(String _type, String _text) {
    try {
      if (logger == null) {
        if (_type.equals("warn") || _type.equals("error")) {
          System.err.println("[" + _type + "]" + _text);
        } else {
          System.out.println("[" + _type + "]" + _text);
        }
      } else {
        if (_type.equals("info")) {
          logging("info", _text);
        } else if (_type.equals("debug")) {
          logger.debug(_text);
        } else if (_type.equals("warn")) {
          logger.warn(_text);
        } else if (_type.equals("error")) {
          logger.error(_text);
        }
      }
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }
}
