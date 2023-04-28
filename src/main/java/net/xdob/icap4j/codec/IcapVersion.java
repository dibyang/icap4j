package net.xdob.icap4j.codec;


import io.netty.handler.codec.http.HttpVersion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides translation and handling for Icap version string.
 */
public final class IcapVersion {

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\S+)/(\\d+)\\.(\\d+)");

  private String protocolName;
  private int major;
  private int minor;
  private String text;

  public static final IcapVersion ICAP_1_0 = new IcapVersion("ICAP", 1, 0);

  /**
   * @param protocolName ICAP or different
   * @param major        the major version
   * @param minor        the minor version
   */
  private IcapVersion(String protocolName, int major, int minor) {
    this.protocolName = protocolName;
    this.major = major;
    this.minor = minor;
    this.text = protocolName + '/' + major + '.' + minor;
  }

  /**
   * parses a valid icap protocol version string.
   *
   * @param text the version (ICAP/1.0)
   */
  private IcapVersion(String text) {
    if (text == null) {
      throw new NullPointerException("text");
    }
    Matcher m = VERSION_PATTERN.matcher(text.trim().toUpperCase());
    if (!m.matches()) {
      throw new IllegalArgumentException("invalid version format: [" + text + "]");
    }
    protocolName = m.group(1);
    major = Integer.parseInt(m.group(2));
    minor = Integer.parseInt(m.group(3));
    this.text = text;
  }

  /**
   * Protocol name
   *
   * @return ICAP or different.
   */
  public String getProtocolName() {
    return protocolName;
  }

  /**
   * Major version
   *
   * @return 1
   */
  public int getMajorVersion() {
    return major;
  }

  /**
   * Minor version
   *
   * @return 0
   */
  public int getMinorVersion() {
    return minor;
  }

  /**
   * The text representation of this version.
   *
   * @return ICAP/1.0
   */
  public String getText() {
    return text;
  }

  /**
   * Returns an existing or new {@link HttpVersion} instance which matches to
   * the specified RTSP version string.  If the specified {@code text} is
   * equal to {@code "ICAP/1.0"}, {@link #ICAP_1_0} will be returned.
   * Otherwise, a new {@link HttpVersion} instance will be returned.
   */
  public static IcapVersion valueOf(String text) {
    if (text == null) {
      throw new NullPointerException("text");
    }
    if (text.trim().toUpperCase().equals("ICAP/1.0")) {
      return ICAP_1_0;
    }

    return new IcapVersion(text);
  }

  @Override
  public String toString() {
    return text;
  }
}
