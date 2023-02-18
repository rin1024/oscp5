/**
 * An OSC (Open Sound Control) library for processing.
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
package oscP5;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import netP5.NetAddress;
import netP5.NetAddressList;
import netP5.NetInfo;
import netP5.NetP5;
import netP5.TcpClient;
import netP5.TcpServer;
import netP5.Transmitter;
import netP5.UdpClient;
import netP5.UdpServer;
import org.apache.log4j.Logger;

/**
 * oscP5 is an osc implementation for the programming environment processing. osc is the acronym for
 * open sound control, a network protocol developed at cnmat, uc berkeley. open sound control is a
 * protocol for communication among computers, sound synthesizers, and other multimedia devices that
 * is optimized for modern networking technology and has been used in many application areas. for
 * further specifications and application implementations please visit the official osc site.
 */

/**
 * TODO add better error message handling for oscEvents, see this post
 * http://forum.processing.org/topic/oscp5-major-problems-with-error-handling# 25080000000811163
 *
 * <p>TODO add option to define host IP, see this thread:
 * http://forum.processing.org/two/discussion/2550/oscp5-android-cannot-send-only-receive
 */
public class OscP5 implements Observer {
  public static final boolean ON = OscProperties.ON;
  public static final boolean OFF = OscProperties.OFF;

  /* a static variable used when creating an oscP5 instance with a specified network protocol. */
  public static final int UDP = OscProperties.UDP;

  /* a static variable used when creating an oscP5 instance with a specified network protocol. */
  public static final int MULTICAST = OscProperties.MULTICAST;

  /* a static variable used when creating an oscP5 instance with a specified network protocol. */
  public static final int TCP = OscProperties.TCP;

  protected static final Logger L = Logger.getLogger(OscP5.class.getName());
  protected Map<String, List<OscPlug>> _myOscPlugMap = new HashMap<String, List<OscPlug>>();

  private static final String VERSION = Version.version;
  private static final String BUILD_DATE = Version.buildDate;

  private static int welcome = 0;
  private static String broadcastAddress;

  private OscProperties _myOscProperties;
  private Method _myEventMethod;
  private Method _myPacketMethod;
  private Class<?> _myEventClass = OscMessage.class;
  private boolean isEventMethod;
  private boolean isPacketMethod;
  private boolean isBroadcast = false;
  private Transmitter transmit;
  private Object parent;

  /** default constructor, starts an UDP server with maximum packet size of 1536 bytes. */
  public OscP5(final Object theParent, final int theListeningPort) {
    OscProperties properties = new OscProperties(theListeningPort);
    init(theParent, properties);
  }

  /** Constructor for a server using theProtocol. */
  public OscP5(final Object theParent, final int theListeningPort, final int theProtocol) {
    OscProperties properties = new OscProperties(theListeningPort);
    properties.setNetworkProtocol(theProtocol);
    init(theParent, properties);
  }

  /** Constructor for a client using theProtocol. */
  public OscP5(
      final Object theParent,
      String theRemoteAddress,
      final int theRemotePort,
      final int theProtocol) {
    OscProperties properties = new OscProperties(new NetAddress(theRemoteAddress, theRemotePort));
    properties.setNetworkProtocol(theProtocol);
    init(theParent, properties);
  }

  public OscP5(final Object theParent, final OscProperties theProperties) {
    init(theParent, theProperties);
  }

  private void init(Object theParent, OscProperties theProperties) {
    welcome();

    parent = (theParent == null) ? new Object() : theParent;
    registerDispose(parent);
    _myOscProperties = theProperties;

    _myEventMethod = checkEventMethod(parent, "oscEvent", new Class<?>[] {OscMessage.class});
    _myPacketMethod = checkEventMethod(parent, "oscEvent", new Class<?>[] {OscBundle.class});
    isEventMethod = _myEventMethod != null;
    isPacketMethod = _myPacketMethod != null;

    // L.info( _myEventMethod + " = " + (isEventMethod ? "true" : "false") + "\n" + _myPacketMethod
    // + " = " + (isPacketMethod ? "true" : "false") );

    switch (_myOscProperties.networkProtocol()) {
      case (OscProperties.UDP):
        if (!_myOscProperties.remoteAddress().equals(OscProperties.defaultnetaddress)) {
          UdpClient udpclient =
              NetP5.createUdpClient(
                  _myOscProperties.remoteAddress().address(),
                  _myOscProperties.remoteAddress().port());
          transmit = udpclient;
        } else {
          UdpServer udpserver =
              NetP5.createUdpServer(
                  _myOscProperties.host(),
                  _myOscProperties.listeningPort(),
                  _myOscProperties.datagramSize());
          udpserver.setBroadcastAddress(broadcastAddress);
          udpserver.addObserver(this);
          transmit = udpserver;
        }
        break;
      case (OscProperties.TCP):
        if (!_myOscProperties.remoteAddress().equals(OscProperties.defaultnetaddress)) {
          TcpClient tcpclient =
              NetP5.createTcpClient(
                  _myOscProperties.remoteAddress().address(),
                  _myOscProperties.remoteAddress().port());
          tcpclient.addObserver(this);
          transmit = tcpclient;
        } else {
          TcpServer tcpserver = NetP5.createTcpServer(_myOscProperties.listeningPort());
          tcpserver.addObserver(this);
          transmit = tcpserver;
        }
        break;
      case (OscProperties.MULTICAST):
        L.info("Multicast is not yet implemented with this version. ");
        break;
      default:
        L.info(_myOscProperties.networkProtocol() + " is Unknown protocol.");
        break;
    }

    broadcastAddress = "";
  }

  public void update(Observable ob, Object map) {
    /* gets called when an OSC packet was received, a Map is expected as second argument. */
    process(map);
  }

  private void welcome() {
    if (welcome++ < 1) {
      L.info(
          "OscP5 "
              + VERSION
              + " "
              + "infos, comments, questions at http://www.sojamo.de/libraries/oscP5");
    }
  }

  public String version() {
    return VERSION;
  }

  public void dispose() {
    transmit.close();
    stop();
  }

  /* TODO notify clients and servers. */
  public void stop() {
    L.info("stopping oscP5.");
  }

  /**
   * イベントリスナー追加
   *
   * @param _theListener instance of OscEventListener
   */
  public void addListener(OscEventListener _theListener) {
    _myOscProperties.listeners().add(_theListener);
  }

  /**
   * イベントリスナー削除
   *
   * @param _theListener instance of OscEventListener
   */
  public void removeListener(OscEventListener _theListener) {
    _myOscProperties.listeners().remove(_theListener);
  }

  /**
   * イベントリスナ一覧取得
   *
   * @return List of OscEventListener
   */
  public List<OscEventListener> listeners() {
    return _myOscProperties.listeners();
  }

  /**
   * 指定したイベントリスナーを持っているか確認
   *
   * @param _theListener instance of OscEventListener
   * @return true if lisners has same listner
   */
  public boolean hasSameListener(OscEventListener _theListener) {
    for (int i = listeners().size() - 1; i >= 0; i--) {
      OscEventListener theListener = (OscEventListener) listeners().get(i);
      if (theListener == _theListener) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if we are dealing with a PApplet parent. If this is the case, register "dispose". Do so
   * quietly, no error messages will be displayed.
   */
  private void registerDispose(Object theObject) {
    try {
      Object parent = null;
      String child = "processing.core.PApplet";

      try {
        Class<?> childClass = Class.forName(child);
        Class<?> parentClass = Object.class;
        if (parentClass.isAssignableFrom(childClass)) {
          parent = childClass.newInstance();
          parent = theObject;
        }
      } catch (Exception e) {
        L.debug("OscP5.registerDispose()" + "registerDispose failed (1)" + e.getCause());
      }

      try {
        Method method = parent.getClass().getMethod("registerMethod", String.class, Object.class);
        try {
          method.invoke(parent, new Object[] {"dispose", this});
        } catch (Exception e) {
          L.debug("OscP5.registerDispose()" + "registerDispose failed (2)" + e.getCause());
        }

      } catch (NoSuchMethodException e) {
        L.debug("OscP5.registerDispose()" + "registerDispose failed (3)" + e.getCause());
      }

    } catch (NullPointerException e) {
      L.debug("OscP5.registerDispose()" + "registerDispose failed (4)" + e.getCause());
    }
  }

  /**
   * PApplet(parent)がoscEvent methodを保持しているか確認する
   *
   * @param theObject instance of PApplet
   * @param theMethod method name like "oscEvent"
   * @param theClass define OscMessage like "new Class< ? >[] { OscMessage.class }"
   * @return Method
   */
  private Method checkEventMethod(Object theObject, String theMethod, Class<?>[] theClass) {
    Method method = null;
    try {
      method = theObject.getClass().getDeclaredMethod(theMethod, theClass);
      method.setAccessible(true);
    } catch (SecurityException e1) {
    } catch (NoSuchMethodException e1) {
    }

    return method;
  }

  public static void flush(final NetAddress theNetAddress, final byte[] theBytes)
      throws SocketException, IOException {
    DatagramSocket mySocket;
    try {
      mySocket = new DatagramSocket();
      if (theNetAddress.address().equals(broadcastAddress)) {
        mySocket.setBroadcast(true);
      }

      DatagramPacket myPacket =
          new DatagramPacket(
              theBytes, theBytes.length, theNetAddress.inetaddress(), theNetAddress.port());
      mySocket.send(myPacket);
      mySocket.disconnect();
      mySocket.close();
      mySocket = null;
    } catch (SocketException e) {
      throw new SocketException(e.toString());
    } catch (IOException e) {
      throw new IOException(e.toString());
    }
  }

  /**
   * osc messages can be automatically forwarded to a specific method of an object. the plug method
   * can be used to by-pass parsing raw osc messages - this job is done for you with the plug
   * mechanism. you can also use the following array-types int[], float[], String[]. (but only as on
   * single parameter e.g. somemethod(int[] theArray) {} ).
   */
  public void plug(
      final Object theObject,
      final String theMethodName,
      final String theAddrPattern,
      final String theTypeTag) {
    final OscPlug myOscPlug = new OscPlug();
    myOscPlug.plug(theObject, theMethodName, theAddrPattern, theTypeTag);

    if (_myOscPlugMap.containsKey(theAddrPattern)) {
      _myOscPlugMap.get(theAddrPattern).add(myOscPlug);
    } else {
      List<OscPlug> myOscPlugList = new ArrayList<OscPlug>();
      myOscPlugList.add(myOscPlug);
      _myOscPlugMap.put(theAddrPattern, myOscPlugList);
    }
  }

  public void plug(
      final Object theObject, final String theMethodName, final String theAddrPattern) {
    final Class<?> myClass = theObject.getClass();
    final Method[] myMethods = myClass.getDeclaredMethods();
    Class<?>[] myParams = null;

    for (int i = 0; i < myMethods.length; i++) {
      String myTypetag = "";
      try {
        myMethods[i].setAccessible(true);
      } catch (Exception e) {
      }
      if ((myMethods[i].getName()).equals(theMethodName)) {
        myParams = myMethods[i].getParameterTypes();
        OscPlug myOscPlug = new OscPlug();
        for (Class<?> c : myParams) {
          myTypetag += myOscPlug.checkType(c.getName());
        }

        myOscPlug.plug(theObject, theMethodName, theAddrPattern, myTypetag);

        // _myOscPlugList.add(myOscPlug);

        if (_myOscPlugMap.containsKey(theAddrPattern)) {
          _myOscPlugMap.get(theAddrPattern).add(myOscPlug);
        } else {
          ArrayList<OscPlug> myOscPlugList = new ArrayList<OscPlug>();
          myOscPlugList.add(myOscPlug);
          _myOscPlugMap.put(theAddrPattern, myOscPlugList);
        }
      }
    }
  }

  private void callMethod(final OscMessage theOscMessage) throws ClassCastException {
    /* forward the received message to all OscEventListeners */
    for (int i = listeners().size() - 1; i >= 0; i--) {
      ((OscEventListener) listeners().get(i)).oscEvent(theOscMessage);
    }

    /* check if the arguments can be forwarded as array */
    if (theOscMessage.isArray) {
      if (_myOscPlugMap.containsKey(theOscMessage.getAddress())) {
        List<OscPlug> myOscPlugList = _myOscPlugMap.get(theOscMessage.getAddress());
        for (OscPlug plug : myOscPlugList) {
          if (plug.isArray && plug.checkMethod(theOscMessage, true)) {
            invoke(plug.getObject(), plug.getMethod(), theOscMessage.argsAsArray());
          }
        }
      }
    }

    if (_myOscPlugMap.containsKey(theOscMessage.getAddress())) {
      List<OscPlug> myOscPlugList = _myOscPlugMap.get(theOscMessage.getAddress());
      for (OscPlug plug : myOscPlugList) {
        if (!plug.isArray && plug.checkMethod(theOscMessage, false)) {
          theOscMessage.isPlugged = true;
          invoke(plug.getObject(), plug.getMethod(), theOscMessage.getArguments());
        }
      }
    }

    /* if no plug method was detected, then use the default oscEvent method */
    if (isEventMethod) {
      try {
        invoke(parent, _myEventMethod, new Object[] {theOscMessage});
      } catch (ClassCastException e) {
        throw new ClassCastException(e.toString());
      }
    }
  }

  /**
   * 対象のtheObjectにtheMethodをhookさせる?
   *
   * @param theObject instance of PApplet
   * @param theMethod instance of Method
   * @param theArgs arguments
   */
  private void invoke(final Object theObject, final Method theMethod, final Object[] theArgs) {
    try {
      theMethod.invoke(theObject, theArgs);
    } catch (IllegalArgumentException e) {
      L.error(e.toString());
    } catch (IllegalAccessException e) {
      L.error(e.toString());
    } catch (InvocationTargetException e) {
      L.error(e.toString());
      L.warn(
          "An error occured while forwarding an OscMessage\n "
              + "to a method in your program. please check your code for any \n"
              + "possible errors that might occur in the method where incoming\n "
              + "OscMessages are parsed e.g. check for casting errors, possible\n "
              + "nullpointers, array overflows ... .\n"
              + "method in charge : "
              + theMethod.getName()
              + "  "
              + e);
    }
  }

  public void process(Object o) {
    if (o instanceof Map) {
      process(OscPacket.parse((Map) o));
    }
  }

  private void process(OscPacket thePacket) {
    /* TODO add raw packet listener here */
    if (thePacket instanceof OscMessage) {
      try {
        callMethod((OscMessage) thePacket);
      } catch (ClassCastException e) {
        L.error(e.toString());
      }
    } else if (thePacket instanceof OscBundle) {
      if (isPacketMethod) {
        invoke(parent, _myPacketMethod, new Object[] {thePacket});
      } else {
        OscBundle bundle = (OscBundle) thePacket;
        for (OscPacket p : bundle.messages) {
          process(p);
        }
      }
    }
  }

  public OscProperties properties() {
    return _myOscProperties;
  }

  public boolean isBroadcast() {
    return isBroadcast;
  }

  public String ip() {
    return NetInfo.getHostAddress();
  }

  /* TODO */
  // public void setTimeToLive( int theTTL ) {
  // _myOscNetManager.setTimeToLive( theTTL );
  // }
  //
  // public TcpServer tcpServer( ) {
  // return _myOscNetManager.tcpServer( );
  // }
  //
  // public TcpClient tcpClient( ) {
  // return _myOscNetManager.tcpClient( );
  // }

  /**
   * you can send osc packets in many different ways. see below and use the send method that fits
   * your needs.
   */
  public void send(final OscPacket thePacket) {
    transmit.send(thePacket.getBytes());
  }

  public void send(final String theAddrPattern, final Object... theArguments) {
    transmit.send(new OscMessage(theAddrPattern, theArguments).getBytes());
  }

  public void send(final NetAddress theNetAddress, String theAddrPattern, Object... theArguments) {
    transmit.send(
        new OscMessage(theAddrPattern, theArguments).getBytes(),
        theNetAddress.address(),
        theNetAddress.port());
  }

  public void send(final NetAddress theNetAddress, final OscPacket thePacket) {
    transmit.send(thePacket.getBytes(), theNetAddress.address(), theNetAddress.port());
  }

  public void send(final List<NetAddress> theList, String theAddrPattern, Object... theArguments) {
    send(theList, new OscMessage(theAddrPattern, theArguments));
  }

  public void send(final List<NetAddress> theList, final OscPacket thePacket) {
    for (NetAddress addr : theList) {
      transmit.send(thePacket.getBytes(), addr.address(), addr.port());
    }
  }

  public boolean send(final OscPacket thePacket, final Object theRemoteSocket) {
    if (theRemoteSocket != null) {

      byte[] b = thePacket.getBytes();
      ByteBuffer buffer = ByteBuffer.allocate(b.length);
      buffer.clear();
      buffer.put(b);
      buffer.flip();

      if (theRemoteSocket instanceof SocketChannel) {
        try {
          ((SocketChannel) theRemoteSocket).write(buffer);
          return true;
        } catch (IOException e) {
          L.error(e.toString());
        }
      } else if (theRemoteSocket instanceof DatagramChannel) {
        try {
          DatagramChannel d = ((DatagramChannel) theRemoteSocket);

          L.info(
              String.format("channel :  " + d.isConnected() + " " + d.socket().getInetAddress()));
          ((DatagramChannel) theRemoteSocket).write(buffer);
          return true;
        } catch (IOException e) {
          L.error(e.toString());
        } catch (NotYetConnectedException e) {
          /* received a datagram packet, sender ip and port have been identified but we
           * are not able to connect to remote address due to no open socket availability. */
          // L.error(e.toString());
        }
      }
    }
    return false;
  }

  public static final byte[] serialize(Object o) {
    if (o instanceof Serializable) {
      return serialize((Serializable) o);
    }
    return new byte[0];
  }

  public static final byte[] serialize(Serializable o) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutput out = null;
    byte[] bytes = new byte[0];
    try {
      out = new ObjectOutputStream(bos);
      out.writeObject(o);
      bytes = bos.toByteArray();
    } catch (Exception e) {
    } finally {
      try {
        out.close();
        bos.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        L.error(e.toString());
      }
    }
    return bytes;
  }

  public static final Object deserialize(byte[] theBytes) {
    ByteArrayInputStream bis = new ByteArrayInputStream(theBytes);
    ObjectInput in = null;
    Object o = null;
    try {
      in = new ObjectInputStream(bis);
      o = in.readObject();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      L.error(e.toString());
    } catch (ClassNotFoundException e) {
      // TODO Auto-generated catch block
      L.error(e.toString());
    } finally {
      try {
        bis.close();
        in.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        L.error(e.toString());
      }
    }
    return o;
  }

  public void send(
      final int thePort,
      final String theAddrPattern,
      final String theAddress,
      final Object... theArguments) {
    /* TODO */
    // _myOscNetManager.send( theAddress , thePort , theAddrPattern , theArguments );
  }

  public void send(final TcpClient theClient, final OscPacket thePacket) {
    /* TODO */
  }

  public void send(
      final TcpClient theClient, final String theAddrPattern, final Object... theArguments) {
    send(theClient, new OscMessage(theAddrPattern, theArguments));
  }

  public void send(final String theHost, final int thePort, final OscPacket thePacket) {
    transmit.send(thePacket.getBytes(), theHost, thePort);
  }

  public void send(final OscPacket thePacket, final String theHost, final int thePort) {
    transmit.send(thePacket.getBytes(), theHost, thePort);
  }

  /**
   * a static method to send an OscMessage straight out of the box without having to instantiate
   * oscP5.
   */
  public static void flush(final NetAddress theNetAddress, final OscMessage theOscMessage)
      throws SocketException, IOException {
    flush(theNetAddress, theOscMessage.getBytes());
  }

  public static void flush(final NetAddress theNetAddress, final OscPacket theOscPacket)
      throws SocketException, IOException {
    flush(theNetAddress, theOscPacket.getBytes());
  }

  public static void flush(
      final NetAddress theNetAddress, final String theAddrPattern, final Object... theArguments)
      throws SocketException, IOException {
    flush(theNetAddress, (new OscMessage(theAddrPattern, theArguments)).getBytes());
  }

  public static void sleep(final long theMillis) {
    try {
      Thread.sleep(theMillis);
    } catch (Exception e) {
    }
  }

  public void send(final NetAddressList theNetAddressList, final OscPacket thePacket) {
    /* TODO */
    // _myOscNetManager.send( thePacket , theNetAddressList );
  }

  public void send(
      final NetAddressList theNetAddressList,
      final String theAddrPattern,
      final Object... theArguments) {
    /* TODO */
    // _myOscNetManager.send( theNetAddressList , theAddrPattern , theArguments );
  }

  /** */
  public static void setBroadcastAddress(String _broadcastAddress) {
    broadcastAddress = _broadcastAddress;
  }
}
