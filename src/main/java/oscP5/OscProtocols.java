package oscP5;

/** OscProtocols as enum */
public enum OscProtocols {
  UNDEFINED(-1),
  UDP(0),
  MULTICAST(1),
  TCP(2);

  private final int id;

  /**
   * set OscProtocols
   *
   * @param _id 0 ~
   */
  private OscProtocols(int _id) {
    this.id = _id;
  }

  /**
   * get OscProtocols id
   *
   * @return id as int
   */
  public int getId() {
    return this.id;
  }

  /**
   * get OscProtocols id
   *
   * @param _id 0 ~
   * @return OscProtocols
   */
  public static OscProtocols from(int _id) {
    for (OscProtocols oscProtocols : OscProtocols.values()) {
      if (oscProtocols.getId() == _id) {
        return oscProtocols;
      }
    }
    return UNDEFINED;
  }

  /**
   * get OscProtocols from String
   *
   * @param _oscProtocols 取得したいOscProtocolsのStringをセット
   * @return OscProtocols
   */
  public static OscProtocols from(String _oscProtocols) {
    for (OscProtocols oscProtocols : OscProtocols.values()) {
      if (oscProtocols.toString().equals(_oscProtocols)) {
        return oscProtocols;
      }
    }
    return UNDEFINED;
  }
}
