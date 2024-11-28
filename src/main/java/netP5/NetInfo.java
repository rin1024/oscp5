/**
 * A network library for processing which supports UDP, TCP, and Multicast.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

public class NetInfo {
  // Logger instance for debugging and logging purposes
  private static final Logger L = Logger.getLogger(NetInfo.class.getName());

  // Constructor
  public NetInfo() {}

  /** Prints basic network information: hostname and IP address. */
  public static void print() {
    try {
      java.net.InetAddress i1 = java.net.InetAddress.getLocalHost();
      L.debug("### hostname/ip : " + i1); // Hostname and IP address
      L.debug("### hostname : " + i1.getHostName()); // Hostname only
      L.debug("### ip : " + i1.getHostAddress()); // IP address only
    } catch (Exception e) {
      L.error(e); // Log error if network information cannot be retrieved
    }
  }

  /**
   * Retrieves the local host's IP address as a String.
   *
   * @return The IP address or "ERROR" if retrieval fails.
   */
  public static String getHostAddress() {
    try {
      java.net.InetAddress i = java.net.InetAddress.getLocalHost();
      return i.getHostAddress();
    } catch (Exception e) {
      L.warn(e); // Log warning if unable to retrieve IP address
    }
    return "ERROR";
  }

  /**
   * Logs and returns the local host's IP address.
   *
   * @return The local IP address.
   */
  public static String lan() {
    L.info("host address : " + getHostAddress());
    return getHostAddress();
  }

  /**
   * Retrieves the WAN (external) IP address by querying a public IP check service.
   *
   * @return The WAN IP address or null if retrieval fails.
   */
  public static String wan() {
    String myIp = null;
    URL u = null;
    String URLstring = "http://checkip.dyndns.org";
    boolean isConnectedToInternet = false;
    L.info("Checking internet connection ...");
    try {
      u = new URL(URLstring);
    } catch (MalformedURLException e) {
      L.warn("Bad URL " + URLstring + " " + e); // Log if URL is malformed
    }

    InputStream in = null;
    try {
      in = u.openStream();
      isConnectedToInternet = true;
    } catch (IOException e) {
      L.warn(
          "Unable to open "
              + URLstring
              + "\nEither the "
              + URLstring
              + " is unavailable or this machine is not connected to the internet!");
    }

    if (isConnectedToInternet) {
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        String theToken = "";
        while ((line = br.readLine()) != null) {
          theToken += line; // Concatenate response lines
        }
        br.close();

        // Parse the response to extract the WAN IP
        StringTokenizer st = new StringTokenizer(theToken, " <>", false);
        while (st.hasMoreTokens()) {
          String myToken = st.nextToken();
          if (myToken.compareTo("Address:") == 0) {
            myToken = st.nextToken(); // Extract IP address
            myIp = myToken;
            L.info("WAN address : " + myIp);
          }
        }
      } catch (IOException e) {
        L.warn("I/O error reading " + URLstring + " Exception = " + e);
      }
    }
    return myIp;
  }

  /**
   * Retrieves available network interfaces and their details.
   *
   * @return A map of network interfaces with attributes.
   */
  public static Map<String, Map> getNetworkInterfaces() {
    Map<String, Map> m = new HashMap<String, Map>();
    Enumeration<NetworkInterface> nets;
    try {
      nets = NetworkInterface.getNetworkInterfaces();
      for (NetworkInterface netint : Collections.list(nets)) {
        Map<String, Object> m1 = new HashMap<String, Object>();
        m.put(netint.getDisplayName(), m1); // Use display name as key
        m1.put("name", netint.getName());
        m1.put("display-name", netint.getDisplayName());
        m1.put("mac", netint.getHardwareAddress());
        m1.put("network-interface", netint);

        // Iterate through IP addresses associated with the network interface
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
          m1.put("inet-address", inetAddress);
        }
      }
    } catch (SocketException e) {
      L.error(e);
    }

    return m;
  }

  /**
   * Retrieves all IP addresses associated with the host, categorized by type (loopback, site-local,
   * etc.).
   *
   * @return A formatted string containing all IP addresses.
   */
  public static String getIpAddress() {
    String ip = "";
    try {
      Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (enumNetworkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
        Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
        while (enumInetAddress.hasMoreElements()) {
          InetAddress inetAddress = enumInetAddress.nextElement();

          String ipAddress = "";
          if (inetAddress.isLoopbackAddress()) {
            ipAddress = "LoopbackAddress: ";
          } else if (inetAddress.isSiteLocalAddress()) {
            ipAddress = "SiteLocalAddress: ";
          } else if (inetAddress.isLinkLocalAddress()) {
            ipAddress = "LinkLocalAddress: ";
          } else if (inetAddress.isMulticastAddress()) {
            ipAddress = "MulticastAddress: ";
          }
          ip += ipAddress + inetAddress.getHostAddress() + "\n"; // Append IP details
        }
      }
    } catch (SocketException e) {
      L.error(e);
      ip += "Something Wrong! " + e.toString() + "\n"; // Append error message
    }

    return ip;
  }

  // TODO: Implement support for bonjour/zeroconf
}
