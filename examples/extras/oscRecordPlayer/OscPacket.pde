public class OscPacket {
  long timeCode;
  String address;
  String typeTags;
  ArrayList<Object> params;
  
  public OscPacket(long _timeCode, String _address, String _typeTags, ArrayList<Object> _params) {
    timeCode = _timeCode;
    address = _address;
    typeTags = _typeTags;
    params = _params;
  }
  
  public void send(OscP5 _osc, NetAddress _receiver) {
    OscMessage msg = new OscMessage(address);
    for (int i=0;i<params.size();i++) {
      msg.add(params.get(i));
    }
    _osc.send(_receiver, msg);
    
    //println(msg);
  }
  
  public String toString() {
    String paramsString = "";
    for (int i=0;i<params.size();i++) {
      paramsString += params.get(i) + ", ";
    }
    
    return "[" + timeCode + "]" + address + ", " + typeTags + " [" + paramsString + "]";
  }
}
