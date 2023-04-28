package net.xdob.icap4j.codec;

import java.nio.charset.Charset;


public final class IcapCodecUtil {

  /**
   * Preview early termination sequence
   * <p>
   * 0; ieof
   */
  public static final Byte[] IEOF_SEQUENCE = new Byte[]{48, 59, 32, 105, 101, 111, 102};

  public static final byte[] NATIVE_IEOF_SEQUENCE = new byte[]{48, 59, 32, 105, 101, 111, 102};

  public static final String IEOF_SEQUENCE_STRING = "0; ieof";

  /**
   * Space
   */
  public static final byte SPACE = 32;

  /**
   * Carriage return
   */
  public static final byte CR = 13;

  /**
   * Line feed character
   */
  public static final byte LF = 10;

  /**
   * carriage return line feed
   */
  public static final byte[] CRLF = new byte[]{CR, LF};

  /**
   * Colon ':'
   */
  public static final byte COLON = 58;


  public static final Charset ASCII_CHARSET = Charset.forName("ASCII");


  private IcapCodecUtil() {
  }

  /**
   * Valiation method for Icap header names.
   *
   * @param name to be validated
   */
  public static void validateHeaderName(String name) {
    if (name == null) {
      throw new NullPointerException("name");
    }
    for (int i = 0; i < name.length(); i++) {
      char caracter = name.charAt(i);
      if (caracter > 127) {
        throw new IllegalArgumentException("name contains non-ascii character: " + name);
      }

      // Check prohibited characters.
      switch (caracter) {
        case '\t':
        case '\n':
        case 0x0b:
        case '\f':
        case '\r':
        case ' ':
        case ',':
        case ':':
        case ';':
        case '=':
          throw new IllegalArgumentException("name contains one of the following prohibited characters: =,;: \\t\\r\\n\\v\\f: " + name);
      }
    }
  }

  /**
   * Validation method for Icap header values
   *
   * @param value to be validated
   */
  public static void validateHeaderValue(String value) {
    if (value == null) {
      throw new NullPointerException("value");
    }

    // 0 - the previous character was neither CR nor LF
    // 1 - the previous character was CR
    // 2 - the previous character was LF
    int state = 0;

    for (int i = 0; i < value.length(); i++) {
      final char caracter = value.charAt(i);

      // Check the absolutely prohibited characters.
      if (caracter == 0x0b | caracter == '\f') {
        throw new IllegalArgumentException("value contains a prohibited character " + caracter + ": " + value);
      }

      // Check the CRLF (HT | SP) pattern
      if (state == 0) {
        if (caracter == '\r') {
          state = 1;
        }
        if (caracter == '\n') {
          state = 2;
        }
      } else if (state == 1) {
        if (caracter == '\n') {
          state = 2;
        } else {
          throw new IllegalArgumentException("Only '\\n' is allowed after '\\r': " + value);
        }
      } else if (state == 2) {
        if (caracter == '\t') {
          state = 0;
        } else {
          throw new IllegalArgumentException("Only ' ' and '\\t' are allowed after '\\n': " + value);
        }
      }
    }

    if (state != 0) {
      throw new IllegalArgumentException("value must not end with '\\r' or '\\n':" + value);
    }
  }
}
