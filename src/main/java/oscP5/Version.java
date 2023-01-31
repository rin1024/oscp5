// via: https://www.gwtcenter.com/handling-version-number-uniformly-by-gradle
package oscP5;

public class Version {
  public static String version = "2.1.4";
  public static String buildDate = "2023-01-29 19:51:33";

  /**
   * バージョン情報を返す
   * @return version
   */
  public static String getVersion() {
    return version;
  }

  /**
   * コンパイルした日時を返す
   * @return buildDate
   */
  public static String getBuildDate() {
    return buildDate;
  }
}
