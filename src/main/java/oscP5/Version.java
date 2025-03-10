// via: https://www.gwtcenter.com/handling-version-number-uniformly-by-gradle
package oscP5;

public class Version {
  public static String version = "2.1.4";
  public static String buildDate = "2025-02-19 10:55:23";

  /**
   * バージョン情報を返す
   *
   * @return version
   */
  public static String getVersion() {
    return version;
  }

  /**
   * コンパイルした日時を返す
   *
   * @return buildDate
   */
  public static String getBuildDate() {
    return buildDate;
  }
}
