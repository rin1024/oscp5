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
  // Constants representing the ON/OFF state of OSC properties.
  public static final boolean ON = OscProperties.ON;
  public static final boolean OFF = OscProperties.OFF;

  // Logger for debugging and information.
  protected static final Logger L = Logger.getLogger(OscP5.class.getName());

  // Map for storing OSC plug configurations.
  protected Map<String, List<OscPlug>> _myOscPlugMap = new HashMap<>();

  // Version and build date of the library.
  private static final String VERSION = Version.version;
  private static final String BUILD_DATE = Version.buildDate;

  // Static variables for managing broadcast addresses.
  private static int welcome = 0;
  private static String broadcastAddress;

  // OSC properties and configuration details.
  private OscProperties _myOscProperties;
  private Method _myEventMethod;
  private Method _myPacketMethod;
  private Class<?> _myEventClass = OscMessage.class;
  private boolean isEventMethod;
  private boolean isPacketMethod;
  private boolean isBroadcast = false;
  private Transmitter transmit;
  private Object parent;

  /**
   * Constructor initializing an UDP server with a specified port.
   *
   * @param theParent The parent object (typically a Processing sketch).
   * @param theListeningPort The port to listen for incoming OSC messages.
   */
  public OscP5(final Object theParent, final int theListeningPort) {
    OscProperties properties = new OscProperties(theListeningPort);
    init(theParent, properties);
  }

  /**
   * Constructor initializing a server with a specified protocol.
   *
   * @param theParent The parent object.
   * @param theListeningPort The port to listen for incoming OSC messages.
   * @param theProtocol The network protocol (UDP/TCP).
   */
  public OscP5(final Object theParent, final int theListeningPort, final OscProtocols theProtocol) {
    OscProperties properties = new OscProperties(theListeningPort);
    properties.setNetworkProtocol(theProtocol);
    init(theParent, properties);
  }

  /**
   * Constructor initializing a client with a remote address and protocol.
   *
   * @param theParent The parent object.
   * @param theRemoteAddress The remote server address.
   * @param theRemotePort The remote server port.
   * @param theProtocol The network protocol (UDP/TCP).
   */
  public OscP5(
      final Object theParent,
      String theRemoteAddress,
      final int theRemotePort,
      final OscProtocols theProtocol) {
    OscProperties properties = new OscProperties(new NetAddress(theRemoteAddress, theRemotePort));
    properties.setNetworkProtocol(theProtocol);
    init(theParent, properties);
  }

  /**
   * General constructor for custom OSC properties.
   *
   * @param theParent The parent object.
   * @param theProperties The OSC properties to use.
   */
  public OscP5(final Object theParent, final OscProperties theProperties) {
    init(theParent, theProperties);
  }

  /**
   * Initializes the OSC server/client based on the provided properties.
   *
   * @param theParent The parent object.
   * @param theProperties The OSC configuration properties.
   */
  private void init(Object theParent, OscProperties theProperties) {
    if (welcome++ < 1) {
      L.debug("[OscP5 " + VERSION + "]" + theProperties.toString().replaceAll("[\r\n]", ", "));
    }

    // Parent setup
    parent = (theParent == null) ? new Object() : theParent;
    registerDispose(parent);
    _myOscProperties = theProperties;

    // Check if parent contains event methods for OSC handling
    _myEventMethod = checkEventMethod(parent, "oscEvent", new Class<?>[] {OscMessage.class});
    _myPacketMethod = checkEventMethod(parent, "oscEvent", new Class<?>[] {OscBundle.class});
    isEventMethod = _myEventMethod != null;
    isPacketMethod = _myPacketMethod != null;

    // Setup network protocol-specific configurations
    OscProtocols p = _myOscProperties.networkProtocol();
    if (p == OscProtocols.UDP) {
      // Handle UDP client/server initialization
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
    } else if (p == OscProtocols.TCP) {
      // Handle TCP client/server initialization
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
    } else if (p == OscProtocols.MULTICAST) {
      // Multicast is not implemented in this version
      L.info("Multicast is not yet implemented with this version.");
    } else {
      L.info(_myOscProperties.networkProtocol() + " is Unknown protocol.");
    }

    broadcastAddress = "";
  }

  // オブジェクトの状態変化に応じて特定の処理を行うためのメソッド。
  public void update(Observable ob, Object map) {
    // OSCパケットを受信した際に呼び出される。引数はマップ型を想定している。
    process(map);
  }

  // バージョン情報を返す。
  public String version() {
    return VERSION;
  }

  // リソースを解放する処理
  public void dispose() {
    transmit.close(); // 通信処理を終了。
    stop(); // 停止処理を実行。
  }

  /* TODO notify clients and servers. */
  public void stop() {
    L.info("stopping oscP5.");
  }

  // OSC関連のリスナーを登録するメソッド。
  public void addListener(OscEventListener _theListener) {
    _myOscProperties.listeners().add(_theListener);
  }

  // 指定したリスナーを削除する。
  public void removeListener(OscEventListener _theListener) {
    _myOscProperties.listeners().remove(_theListener);
  }

  // 登録されているリスナーの一覧を取得する。
  public List<OscEventListener> listeners() {
    return _myOscProperties.listeners();
  }

  // 指定したリスナーが既に登録されているか確認する。
  public boolean hasSameListener(OscEventListener _theListener) {
    for (int i = listeners().size() - 1; i >= 0; i--) {
      OscEventListener theListener = (OscEventListener) listeners().get(i);
      if (theListener == _theListener) {
        return true;
      }
    }
    return false;
  }

  // ProcessingのPAppletと連携するための処理を登録する。
  private void registerDispose(Object theObject) {
    try {
      Object parent = null;
      String child = "processing.core.PApplet";
      try {
        Class<?> childClass = Class.forName(child);
        Class<?> parentClass = Object.class;
        if (parentClass.isAssignableFrom(childClass)) {
          // if (Object.class.isAssignableFrom(childClass)) {
          parent = childClass.newInstance();
          parent = theObject;
        }
      } catch (Exception e) {
        L.debug("registerDispose failed (1): " + e.getCause());
      }
      try {
        Method method = parent.getClass().getMethod("registerMethod", String.class, Object.class);
        method.invoke(parent, new Object[] {"dispose", this});
      } catch (Exception e) {
        L.debug("registerDispose failed (2): " + e.getCause());
      }
    } catch (NullPointerException e) {
      L.debug("registerDispose failed (4): " + e.getCause());
    }
  }

  // イベントメソッド（oscEventなど）が存在するかチェック。
  private Method checkEventMethod(Object theObject, String theMethod, Class<?>[] theClass) {
    Method method = null;
    try {
      method = theObject.getClass().getDeclaredMethod(theMethod, theClass);
      method.setAccessible(true);
    } catch (Exception e) {
      // エラー時の処理は特になし。
    }
    return method;
  }

  // 送信処理（UDPパケット）を実装するスタティックメソッド。
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
    } catch (Exception e) {
      throw e; // エラー時の例外スロー。
    }
  }

  /** OSCメッセージを自動的に特定のオブジェクトのメソッドに転送する機能。 指定されたアドレスパターンと型タグを用いて、メソッド呼び出しを管理。 */
  public void plug(
      final Object theObject,
      final String theMethodName,
      final String theAddrPattern,
      final String theTypeTag) {
    // OscPlugオブジェクトを作成し、メソッドを登録。
    final OscPlug myOscPlug = new OscPlug();
    myOscPlug.plug(theObject, theMethodName, theAddrPattern, theTypeTag);

    // アドレスパターンに基づいてマップに追加。
    if (_myOscPlugMap.containsKey(theAddrPattern)) {
      _myOscPlugMap.get(theAddrPattern).add(myOscPlug);
    } else {
      List<OscPlug> myOscPlugList = new ArrayList<OscPlug>();
      myOscPlugList.add(myOscPlug);
      _myOscPlugMap.put(theAddrPattern, myOscPlugList);
    }
  }

  /** 型タグなしでオーバーロードされたplugメソッド。 対象のオブジェクトとメソッド、アドレスパターンを用いてメソッドを登録。 */
  public void plug(
      final Object theObject, final String theMethodName, final String theAddrPattern) {
    final Class<?> myClass = theObject.getClass();
    final Method[] myMethods = myClass.getDeclaredMethods();
    Class<?>[] myParams = null;

    for (int i = 0; i < myMethods.length; i++) {
      String myTypetag = "";
      try {
        myMethods[i].setAccessible(true); // プライベートメソッドのアクセス権を設定。
      } catch (Exception e) {
      }

      // メソッド名が一致する場合。
      if ((myMethods[i].getName()).equals(theMethodName)) {
        myParams = myMethods[i].getParameterTypes();
        OscPlug myOscPlug = new OscPlug();
        for (Class<?> c : myParams) {
          myTypetag += myOscPlug.checkType(c.getName()); // パラメータ型から型タグを生成。
        }

        // 新しいOscPlugオブジェクトを登録。
        myOscPlug.plug(theObject, theMethodName, theAddrPattern, myTypetag);

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

  /** OscMessageを適切なリスナーやメソッドに転送。 */
  private void callMethod(final OscMessage theOscMessage) throws ClassCastException {
    // リスナーにOSCメッセージを通知。
    for (int i = listeners().size() - 1; i >= 0; i--) {
      ((OscEventListener) listeners().get(i)).oscEvent(theOscMessage);
    }

    // メッセージが配列の場合。
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

    // 登録されたメソッドを実行。
    if (_myOscPlugMap.containsKey(theOscMessage.getAddress())) {
      List<OscPlug> myOscPlugList = _myOscPlugMap.get(theOscMessage.getAddress());
      for (OscPlug plug : myOscPlugList) {
        if (!plug.isArray && plug.checkMethod(theOscMessage, false)) {
          theOscMessage.isPlugged = true;
          invoke(plug.getObject(), plug.getMethod(), theOscMessage.getArguments());
        }
      }
    }

    // 登録されていない場合はデフォルトのoscEventを呼び出す。
    if (isEventMethod) {
      try {
        invoke(parent, _myEventMethod, new Object[] {theOscMessage});
      } catch (ClassCastException e) {
        throw new ClassCastException(e.toString());
      }
    }
  }

  /** 指定されたオブジェクトのメソッドを引数と共に実行。 */
  private void invoke(final Object theObject, final Method theMethod, final Object[] theArgs) {
    try {
      theMethod.invoke(theObject, theArgs);
    } catch (IllegalArgumentException e) {
      L.error(e.toString());
    } catch (IllegalAccessException e) {
      L.error(e.toString());
    } catch (InvocationTargetException e) {
      L.error("error method: " + theMethod.getName() + ", error info: " + e.toString());
    }
  }

  /** OscPacketを処理するメソッド。メッセージまたはバンドルに対応。 */
  public void process(Object o) {
    if (o instanceof Map) {
      process(OscPacket.parse((Map) o));
    }
  }

  /** OscPacketを処理するメソッド。メッセージまたはバンドルに対応。 */
  private void process(OscPacket thePacket) {
    if (thePacket instanceof OscMessage) {
      try {
        callMethod((OscMessage) thePacket); // OscMessageを処理。
      } catch (ClassCastException e) {
        L.error(e.toString());
      }
    } else if (thePacket instanceof OscBundle) {
      if (isPacketMethod) {
        invoke(parent, _myPacketMethod, new Object[] {thePacket}); // バンドル全体を処理。
      } else {
        OscBundle bundle = (OscBundle) thePacket;
        for (OscPacket p : bundle.messages) {
          process(p); // バンドル内のメッセージを個別に処理。
        }
      }
    }
  }

  /**
   * Retrieves the OscProperties object associated with the current instance.
   *
   * @return The OscProperties object containing OSC configuration details.
   */
  public OscProperties properties() {
    return _myOscProperties;
  }

  /**
   * Checks if broadcasting is enabled for this instance.
   *
   * @return True if broadcasting is enabled; false otherwise.
   */
  public boolean isBroadcast() {
    return isBroadcast;
  }

  /**
   * Retrieves the IP address of the host machine.
   *
   * @return A string representing the IP address of the host.
   */
  public String ip() {
    return NetInfo.getHostAddress();
  }

  /**
   * Sends an OSC packet to the configured destination.
   *
   * @param thePacket The OSC packet to be transmitted.
   */
  public void send(final OscPacket thePacket) {
    transmit.send(thePacket.getBytes());
  }

  /**
   * Sends an OSC message to the configured destination using a specified address pattern and
   * arguments.
   *
   * @param theAddrPattern The OSC address pattern for the message.
   * @param theArguments The arguments to include in the OSC message.
   */
  public void send(final String theAddrPattern, final Object... theArguments) {
    transmit.send(new OscMessage(theAddrPattern, theArguments).getBytes());
  }

  /**
   * Sends an OSC message to a specific NetAddress using a given address pattern and arguments.
   *
   * @param theNetAddress The target NetAddress to send the message to.
   * @param theAddrPattern The OSC address pattern for the message.
   * @param theArguments The arguments to include in the OSC message.
   */
  public void send(final NetAddress theNetAddress, String theAddrPattern, Object... theArguments) {
    transmit.send(
        new OscMessage(theAddrPattern, theArguments).getBytes(),
        theNetAddress.address(),
        theNetAddress.port());
  }

  /**
   * Sends an OSC packet to a specific NetAddress.
   *
   * @param theNetAddress The target NetAddress to send the packet to.
   * @param thePacket The OSC packet to be sent.
   */
  public void send(final NetAddress theNetAddress, final OscPacket thePacket) {
    transmit.send(thePacket.getBytes(), theNetAddress.address(), theNetAddress.port());
  }

  /**
   * Sends an OSC message to a list of NetAddresses using a given address pattern and arguments.
   *
   * @param theList The list of NetAddresses to send the message to.
   * @param theAddrPattern The OSC address pattern for the message.
   * @param theArguments The arguments to include in the OSC message.
   */
  public void send(final List<NetAddress> theList, String theAddrPattern, Object... theArguments) {
    send(theList, new OscMessage(theAddrPattern, theArguments));
  }

  /**
   * Sends an OSC packet to a list of NetAddresses.
   *
   * @param theList The list of NetAddresses to send the packet to.
   * @param thePacket The OSC packet to be sent.
   */
  public void send(final List<NetAddress> theList, final OscPacket thePacket) {
    for (NetAddress addr : theList) {
      transmit.send(thePacket.getBytes(), addr.address(), addr.port());
    }
  }

  /**
   * Sends an OSC packet using a specified remote socket.
   *
   * @param thePacket The OSC packet to be sent.
   * @param theRemoteSocket The remote socket (SocketChannel or DatagramChannel) to send the packet
   *     through.
   * @return True if the packet is sent successfully; false otherwise.
   */
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
        }
      }
    }
    return false;
  }

  /**
   * Serializes an object into a byte array.
   *
   * @param o The object to serialize.
   * @return A byte array representing the serialized object, or an empty byte array if the object
   *     is not serializable.
   */
  public static final byte[] serialize(Object o) {
    if (o instanceof Serializable) {
      return serialize((Serializable) o);
    }
    return new byte[0];
  }

  /**
   * Serializes a Serializable object into a byte array.
   *
   * @param o The Serializable object to serialize.
   * @return A byte array representing the serialized object.
   */
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
        L.error(e.toString());
      }
    }
    return bytes;
  }

  /**
   * Deserializes a byte array back into an object.
   *
   * @param theBytes The byte array representing the serialized object.
   * @return The deserialized object, or null if deserialization fails.
   */
  public static final Object deserialize(byte[] theBytes) {
    ByteArrayInputStream bis = new ByteArrayInputStream(theBytes);
    ObjectInput in = null;
    Object o = null;
    try {
      in = new ObjectInputStream(bis);
      o = in.readObject();
    } catch (IOException e) {
      L.error(e.toString());
    } catch (ClassNotFoundException e) {
      L.error(e.toString());
    } finally {
      try {
        bis.close();
        in.close();
      } catch (IOException e) {
        L.error(e.toString());
      }
    }
    return o;
  }

  /**
   * Sends an OSC message to a specific host and port using a given address pattern and arguments.
   *
   * @param thePort The port number to send the message to.
   * @param theAddrPattern The OSC address pattern for the message.
   * @param theAddress The target host address.
   * @param theArguments The arguments to include in the OSC message.
   */
  public void send(
      final int thePort,
      final String theAddrPattern,
      final String theAddress,
      final Object... theArguments) {
    // TODO: Implement this method using _myOscNetManager.
  }

  /**
   * Sends an OSC packet to a specific TCP client.
   *
   * @param theClient The TcpClient instance to send the packet to.
   * @param thePacket The OSC packet to be sent.
   */
  public void send(final TcpClient theClient, final OscPacket thePacket) {
    // TODO: Implement the TCP client send logic.
  }

  /**
   * Sends an OSC message to a specific TCP client using a given address pattern and arguments.
   *
   * @param theClient The TcpClient instance to send the message to.
   * @param theAddrPattern The OSC address pattern for the message.
   * @param theArguments The arguments to include in the OSC message.
   */
  public void send(
      final TcpClient theClient, final String theAddrPattern, final Object... theArguments) {
    send(theClient, new OscMessage(theAddrPattern, theArguments));
  }

  /**
   * Sends an OSC packet to a specific host and port.
   *
   * @param theHost The target host address.
   * @param thePort The port number to send the packet to.
   * @param thePacket The OSC packet to be sent.
   */
  public void send(final String theHost, final int thePort, final OscPacket thePacket) {
    transmit.send(thePacket.getBytes(), theHost, thePort);
  }

  /**
   * Sends an OSC packet to a specific host and port.
   *
   * @param thePacket The OSC packet to be sent.
   * @param theHost The target host address.
   * @param thePort The port number to send the packet to.
   */
  public void send(final OscPacket thePacket, final String theHost, final int thePort) {
    transmit.send(thePacket.getBytes(), theHost, thePort);
  }

  /**
   * Sends an OSC message to a NetAddress using a static method.
   *
   * @param theNetAddress The NetAddress to send the message to.
   * @param theOscMessage The OSC message to be sent.
   * @throws SocketException If a socket error occurs.
   * @throws IOException If an I/O error occurs.
   */
  public static void flush(final NetAddress theNetAddress, final OscMessage theOscMessage)
      throws SocketException, IOException {
    flush(theNetAddress, theOscMessage.getBytes());
  }

  /**
   * Sends an OSC packet to a NetAddress using a static method.
   *
   * @param theNetAddress The NetAddress to send the packet to.
   * @param theOscPacket The OSC packet to be sent.
   * @throws SocketException If a socket error occurs.
   * @throws IOException If an I/O error occurs.
   */
  public static void flush(final NetAddress theNetAddress, final OscPacket theOscPacket)
      throws SocketException, IOException {
    flush(theNetAddress, theOscPacket.getBytes());
  }

  /**
   * Sends an OSC message to a NetAddress using a static method with specified arguments.
   *
   * @param theNetAddress The NetAddress to send the message to.
   * @param theAddrPattern The OSC address pattern for the message.
   * @param theArguments The arguments to include in the OSC message.
   * @throws SocketException If a socket error occurs.
   * @throws IOException If an I/O error occurs.
   */
  public static void flush(
      final NetAddress theNetAddress, final String theAddrPattern, final Object... theArguments)
      throws SocketException, IOException {
    flush(theNetAddress, (new OscMessage(theAddrPattern, theArguments)).getBytes());
  }

  /**
   * Pauses execution for a specified duration.
   *
   * @param theMillis The duration to sleep in milliseconds.
   */
  public static void sleep(final long theMillis) {
    try {
      Thread.sleep(theMillis);
    } catch (Exception e) {
    }
  }

  /**
   * Sends an OSC packet to all addresses in a NetAddressList.
   *
   * @param theNetAddressList The list of NetAddresses to send the packet to.
   * @param thePacket The OSC packet to be sent.
   */
  public void send(final NetAddressList theNetAddressList, final OscPacket thePacket) {
    // TODO: Implement this method using _myOscNetManager.
  }

  /**
   * Sends an OSC message to all addresses in a NetAddressList using a given address pattern and
   * arguments.
   *
   * @param theNetAddressList The list of NetAddresses to send the message to.
   * @param theAddrPattern The OSC address pattern for the message.
   * @param theArguments The arguments to include in the OSC message.
   */
  public void send(
      final NetAddressList theNetAddressList,
      final String theAddrPattern,
      final Object... theArguments) {
    // TODO: Implement this method using _myOscNetManager.
  }

  /**
   * Sets the broadcast address for the current instance.
   *
   * @param _broadcastAddress The new broadcast address to set.
   */
  public static void setBroadcastAddress(String _broadcastAddress) {
    broadcastAddress = _broadcastAddress;
  }
}
