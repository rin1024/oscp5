/**
 * UDP、TCP、およびマルチキャストをサポートするProcessing用のネットワークライブラリ。
 *
 * <p>##copyright##
 *
 * <p>このライブラリはフリーソフトウェアです；あなたはGNU Lesser General Public Licenseの条件に従って、
 * このライブラリを再配布または修正できます；ライセンスのバージョン2.1、または（あなたの選択で）それ以降のバージョンに基づくものです。
 *
 * <p>このライブラリは、役立つことを期待して配布されていますが、商業性や特定の目的への適合性など、いかなる保証もありません； 詳細についてはGNU Lesser General Public
 * Licenseをご覧ください。
 *
 * <p>GNU Lesser General Public Licenseのコピーをこのライブラリと一緒に受け取ったはずです； もし受け取っていない場合は、Free Software
 * Foundation, Inc.に書き送ってください。 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * @author ##author##
 * @modified ##date##
 * @version ##version##
 */
package netP5;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Bytes {

  private Bytes() {}

  /** オブジェクト配列をリスト形式でフォーマットされた文字列に変換します */
  public static String getAsString(Object[] theObject) {
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < theObject.length; i++) {
      s.append("[" + i + "]" + " " + theObject[i] + "\n");
    }
    return s.toString();
  }

  /** バイト配列を文字列に変換します */
  public static String getAsString(byte[] theBytes) {
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < theBytes.length; i++) {
      s.append((char) theBytes[i]);
    }
    return s.toString();
  }

  /** バイト配列を整数（int）に変換します */
  public static int toInt(byte abyte0[]) {
    return (abyte0[3] & 0xff)
        + ((abyte0[2] & 0xff) << 8)
        + ((abyte0[1] & 0xff) << 16)
        + ((abyte0[0] & 0xff) << 24);
  }

  /** バイト配列を長整数（long）に変換します */
  public static long toLong(byte abyte0[]) {
    return ((long) abyte0[7] & 255L)
        + (((long) abyte0[6] & 255L) << 8)
        + (((long) abyte0[5] & 255L) << 16)
        + (((long) abyte0[4] & 255L) << 24)
        + (((long) abyte0[3] & 255L) << 32)
        + (((long) abyte0[2] & 255L) << 40)
        + (((long) abyte0[1] & 255L) << 48)
        + (((long) abyte0[0] & 255L) << 56);
  }

  /** バイト配列を浮動小数点数（float）に変換します */
  public static float toFloat(byte abyte0[]) {
    int i = toInt(abyte0);
    return Float.intBitsToFloat(i);
  }

  /** バイト配列を倍精度浮動小数点数（double）に変換します */
  public static double toDouble(byte abyte0[]) {
    long l = toLong(abyte0);
    return Double.longBitsToDouble(l);
  }

  /** 整数をバイト配列に変換します（デフォルトは4バイト） */
  public static byte[] toBytes(int i) {
    return toBytes(i, new byte[4]);
  }

  /** 整数をバイト配列に変換します（指定されたバイト配列に変換） */
  public static byte[] toBytes(int i, byte abyte0[]) {
    abyte0[3] = (byte) i;
    i >>>= 8;
    abyte0[2] = (byte) i;
    i >>>= 8;
    abyte0[1] = (byte) i;
    i >>>= 8;
    abyte0[0] = (byte) i;
    return abyte0;
  }

  /** 長整数をバイト配列に変換します（デフォルトは8バイト） */
  public static byte[] toBytes(long l) {
    return toBytes(l, new byte[8]);
  }

  /** 長整数をバイト配列に変換します（指定されたバイト配列に変換） */
  public static byte[] toBytes(long l, byte abyte0[]) {
    abyte0[7] = (byte) (int) l;
    l >>>= 8;
    abyte0[6] = (byte) (int) l;
    l >>>= 8;
    abyte0[5] = (byte) (int) l;
    l >>>= 8;
    abyte0[4] = (byte) (int) l;
    l >>>= 8;
    abyte0[3] = (byte) (int) l;
    l >>>= 8;
    abyte0[2] = (byte) (int) l;
    l >>>= 8;
    abyte0[1] = (byte) (int) l;
    l >>>= 8;
    abyte0[0] = (byte) (int) l;
    return abyte0;
  }

  /** 2つのバイト配列が等しいかどうかを比較します */
  public static boolean areEqual(byte abyte0[], byte abyte1[]) {
    int i = abyte0.length;
    if (i != abyte1.length) {
      return false;
    }
    for (int j = 0; j < i; j++) {
      if (abyte0[j] != abyte1[j]) {
        return false;
      }
    }
    return true;
  }

  /** 2つのバイト配列を結合します */
  public static byte[] append(byte abyte0[], byte abyte1[]) {
    byte abyte2[] = new byte[abyte0.length + abyte1.length];
    System.arraycopy(abyte0, 0, abyte2, 0, abyte0.length);
    System.arraycopy(abyte1, 0, abyte2, abyte0.length, abyte1.length);
    return abyte2;
  }

  /** 3つのバイト配列を結合します */
  public static byte[] append(byte abyte0[], byte abyte1[], byte abyte2[]) {
    byte abyte3[] = new byte[abyte0.length + abyte1.length + abyte2.length];
    System.arraycopy(abyte0, 0, abyte3, 0, abyte0.length);
    System.arraycopy(abyte1, 0, abyte3, abyte0.length, abyte1.length);
    System.arraycopy(abyte2, 0, abyte3, abyte0.length + abyte1.length, abyte2.length);
    return abyte3;
  }

  /** バイト配列の一部をコピーします */
  public static byte[] copy(byte abyte0[], int i) {
    return copy(abyte0, i, abyte0.length - i);
  }

  /** バイト配列の指定した範囲をコピーします */
  public static byte[] copy(byte abyte0[], int i, int j)
      throws java.lang.ArrayIndexOutOfBoundsException {
    byte abyte1[] = new byte[j];
    System.arraycopy(abyte0, i, abyte1, 0, j);
    return abyte1;
  }

  /** バイト配列をマージします */
  public static void merge(byte abyte0[], byte abyte1[], int i, int j, int k) {
    System.arraycopy(abyte0, i, abyte1, j, k);
  }

  /** バイト配列を指定位置にマージします */
  public static void merge(byte abyte0[], byte abyte1[], int i) {
    System.arraycopy(abyte0, 0, abyte1, i, abyte0.length);
  }

  /** バイト配列をマージします */
  public static void merge(byte abyte0[], byte abyte1[]) {
    System.arraycopy(abyte0, 0, abyte1, 0, abyte0.length);
  }

  /** バイト配列の一部をマージします */
  public static void merge(byte abyte0[], byte abyte1[], int i, int j) {
    System.arraycopy(abyte0, 0, abyte1, i, j);
  }

  // バイト配列の指定範囲を16進数の文字列に変換する
  public static String toString(byte abyte0[], int i, int j) {
    char ac[] = new char[j * 2]; // 16進数の2桁ずつで1バイトを表現するため、サイズを設定
    int k = i;
    int l = 0;
    // 指定された範囲でバイト配列を16進数に変換
    for (; k < i + j; k++) {
      byte byte0 = abyte0[k];
      ac[l++] = hexDigits[byte0 >>> 4 & 0xf]; // 高位4ビットを16進数に変換
      ac[l++] = hexDigits[byte0 & 0xf]; // 下位4ビットを16進数に変換
    }

    return new String(ac); // 結果を文字列として返す
  }

  // バイト配列全体を16進数の文字列に変換する
  public static String toString(byte abyte0[]) {
    return toString(abyte0, 0, abyte0.length); // 全体を変換
  }

  // バイト配列の内容を16進数と文字で出力する
  public static void printBytes(byte[] byteArray) {
    // 配列の各バイトを16進数と対応する文字で表示
    for (int i = 0; i < byteArray.length; i++) {
      System.out.print(
          (char) byteArray[i] // 文字として表示
              + " ("
              + hexDigits[byteArray[i] >>> 4 & 0xf] // 高位4ビットの16進数表示
              + ""
              + hexDigits[byteArray[i] & 0xf] // 下位4ビットの16進数表示
              + ")  ");
      if ((i + 1) % 4 == 0) { // 4バイトごとに改行
        System.out.print("\n");
      }
    }
  }

  // 16進数の文字配列。各バイトの16進数表現をここから取得
  public static final char hexDigits[] = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  };

  // 整数をリトルエンディアン形式の4バイト配列に変換
  private static byte[] toByteArray(int in_int) {
    byte a[] = new byte[4];
    // 整数の各バイトを抽出して配列に格納
    for (int i = 0; i < 4; i++) {
      int b_int = (in_int >> (i * 8)) & 255;
      byte b = (byte) (b_int);
      a[i] = b;
    }
    return a; // 配列を返す
  }

  // 整数をビッグエンディアン形式の4バイト配列に変換
  private static byte[] toByteArrayBigEndian(int theInt) {
    byte a[] = new byte[4];
    // 整数をビッグエンディアン形式で配列に格納
    for (int i = 0; i < 4; i++) {
      int b_int = (theInt >> (i * 8)) & 255;
      byte b = (byte) (b_int);
      a[3 - i] = b; // 配列の先頭に格納
    }
    return a; // 配列を返す
  }

  // 4バイトのバイト配列を整数に変換
  private static int asInt(byte[] byte_array_4) {
    int ret = 0;
    // 4バイトを結合して整数を構築
    for (int i = 0; i < 4; i++) {
      int b = (int) byte_array_4[i];
      if (i < 3 && b < 0) { // 負の値を補正
        b = 256 + b;
      }
      ret += b << (i * 8); // 各バイトを適切な位置にシフトして加算
    }
    return ret; // 整数を返す
  }

  // リトルエンディアン形式で入力ストリームから整数を読み取る
  public static int toIntLittleEndian(InputStream theInputStream) throws java.io.IOException {
    byte[] byte_array_4 = new byte[4];
    // 4バイトをリトルエンディアン形式で読み込む
    byte_array_4[0] = (byte) theInputStream.read();
    byte_array_4[1] = (byte) theInputStream.read();
    byte_array_4[2] = (byte) theInputStream.read();
    byte_array_4[3] = (byte) theInputStream.read();

    return asInt(byte_array_4); // 読み取ったバイト配列を整数に変換
  }

  // ビッグエンディアン形式で入力ストリームから整数を読み取る
  public static int toIntBigEndian(InputStream theInputStream) throws java.io.IOException {
    byte[] byte_array_4 = new byte[4];
    /* TCP ヘッダーの32ビット値（ビッグエンディアン形式）を整数に変換 */
    byte_array_4[3] = (byte) theInputStream.read();
    byte_array_4[2] = (byte) theInputStream.read();
    byte_array_4[1] = (byte) theInputStream.read();
    byte_array_4[0] = (byte) theInputStream.read();
    return asInt(byte_array_4); // 読み取ったバイト配列を整数に変換
  }

  // 入力ストリームから長さを読み取り、その長さ分を文字列として返す
  public static String toString(InputStream ins) throws java.io.IOException {
    int len = toIntLittleEndian(ins); // 長さをリトルエンディアン形式で読み取る
    return toString(ins, len); // 指定長のデータを文字列に変換
  }

  // 入力ストリームから指定された長さ分を文字列として読み取る
  private static String toString(InputStream ins, int len) throws java.io.IOException {
    String ret = new String();
    for (int i = 0; i < len; i++) {
      ret += (char) ins.read(); // 各バイトを文字として読み取る
    }
    return ret; // 文字列を返す
  }

  // 整数をビッグエンディアン形式で出力ストリームに書き込む
  public static void toStream(OutputStream os, int i) throws Exception {
    byte[] byte_array_4 = toByteArrayBigEndian(i); // 整数をビッグエンディアン形式でバイト配列に変換
    os.write(byte_array_4); // 出力ストリームに書き込む
  }

  // 文字列を出力ストリームに書き込む
  public static void toStream(OutputStream os, String s) throws Exception {
    int len_s = s.length();
    toStream(os, len_s); // 文字列の長さを先に書き込む
    for (int i = 0; i < len_s; i++) {
      os.write((byte) s.charAt(i)); // 文字列の各文字をバイトとして書き込む
    }
    os.flush(); // バッファをフラッシュ
  }

  // バイト配列を出力ストリームに書き込む
  public static void toStream(OutputStream os, byte[] theBytes) throws Exception {
    int myLength = theBytes.length;
    toStream(os, myLength); // バイト配列の長さを先に書き込む
    os.write(theBytes); // バイト配列を書き込む
    os.flush(); // バッファをフラッシュ
  }

  // 入力ストリームからバイト配列を読み取る（長さ付き）
  public static byte[] toByteArray(InputStream ins) throws java.io.IOException {
    int len = toIntLittleEndian(ins); // 長さをリトルエンディアン形式で読み取る
    try {
      return toByteArray(ins, len); // 長さ分のバイトを読み取る
    } catch (Exception e) {
      return new byte[0]; // 例外発生時は空の配列を返す
    }
  }

  // 入力ストリームから指定された長さ分をバイト配列として読み取る
  protected static byte[] toByteArray(InputStream ins, int an_int)
      throws java.io.IOException, Exception {

    byte[] ret = new byte[an_int]; // 指定された長さ分のバイト配列を作成

    int offset = 0;
    int numRead = 0;
    int outstanding = an_int;

    while ((offset < an_int) && ((numRead = ins.read(ret, offset, outstanding)) > 0)) {
      offset += numRead; // 読み取ったバイト数を更新
      outstanding = an_int - offset; // 残りの読み取るバイト数
    }
    if (offset < ret.length) {
      throw new Exception(
          "Could not completely read from stream, numRead="
              + numRead
              + ", ret.length="
              + ret.length); // ストリームから全てのデータを読み取れなかった場合
    }
    return ret; // 読み取ったバイト配列を返す
  }

  // 入力ストリームからファイルに書き込む
  private static void toFile(InputStream ins, FileOutputStream fos, int len, int buf_size)
      throws java.io.FileNotFoundException, java.io.IOException {

    byte[] buffer = new byte[buf_size];

    int len_read = 0;
    int total_len_read = 0;

    while (total_len_read + buf_size <= len) {
      len_read = ins.read(buffer); // バッファサイズごとに読み込む
      total_len_read += len_read;
      fos.write(buffer, 0, len_read); // ファイルに書き込む
    }

    if (total_len_read < len) {
      toFile(ins, fos, len - total_len_read, buf_size / 2); // 残りのデータを再帰的に書き込む
    }
  }

  // 入力ストリームからファイルに書き込む（指定長）
  private static void toFile(InputStream ins, File file, int len)
      throws java.io.FileNotFoundException, java.io.IOException {

    FileOutputStream fos = new FileOutputStream(file); // 出力ファイルを開く

    toFile(ins, fos, len, 1024); // 1024バイトずつ書き込む
  }

  // 入力ストリームからファイルに書き込む
  public static void toFile(InputStream ins, File file)
      throws java.io.FileNotFoundException, java.io.IOException {

    int len = toIntLittleEndian(ins); // 長さをリトルエンディアン形式で読み取る
    toFile(ins, file, len); // ファイルに書き込む
  }

  // 出力ストリームにファイルを転送する
  public static void toStream(OutputStream os, File file)
      throws java.io.FileNotFoundException, Exception {

    toStream(os, (int) file.length()); // ファイルのサイズを転送
    byte b[] = new byte[1024];
    InputStream is = new FileInputStream(file); // ファイルを入力ストリームとして開く
    int numRead = 0;

    while ((numRead = is.read(b)) > 0) {
      os.write(b, 0, numRead); // 出力ストリームに書き込む
    }
    os.flush(); // バッファをフラッシュ
  }
}
