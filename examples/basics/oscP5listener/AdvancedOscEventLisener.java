import oscP5.OscEventListener;
import oscP5.OscMessage;

public class AdvancedOscEventLisener implements OscEventListener {
  public AdvancedOscEventLisener() {
    
  }

  public void oscEvent(OscMessage _m) {
    System.out.println(
      "[From " + _m.getHostAddress() + ":" + _m.getHostPort() + "] " + 
      "address: " + _m.getAddress() + 
      ", typetag: " + _m.getTypetag() + 
      "/// " + _m.intValue(0) + 
      " /// " + _m.intValue(1));
  }
}
