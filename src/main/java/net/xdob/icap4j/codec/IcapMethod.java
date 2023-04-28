package net.xdob.icap4j.codec;

import io.netty.util.AsciiString;

import java.util.HashMap;
import java.util.Map;

import static io.netty.util.internal.ObjectUtil.checkNonEmptyAfterTrim;

/**
 * ICAP methods that are valid to use in messages.
 */
public class IcapMethod implements Comparable<IcapMethod> {
  public static final IcapMethod OPTIONS = new IcapMethod("OPTIONS");
  public static final IcapMethod REQMOD = new IcapMethod("REQMOD");
  public static final IcapMethod RESPMOD = new IcapMethod("RESPMOD");

  private final AsciiString name;

  IcapMethod(String name) {
    name = checkNonEmptyAfterTrim(name, "name");

    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (Character.isISOControl(c) || Character.isWhitespace(c)) {
        throw new IllegalArgumentException("invalid character in name");
      }
    }

    this.name = AsciiString.cached(name);
  }


  /**
   * Returns the name of this method.
   */
  public String name() {
    return name.toString();
  }

  /**
   * Returns the name of this method.
   */
  public AsciiString asciiName() {
    return name;
  }

  @Override
  public String toString() {
    return name.toString();
  }

  @Override
  public int compareTo(IcapMethod o) {
    if (o == this) {
      return 0;
    }
    return name.compareTo(o.name);
  }

  private static final Map<String, IcapMethod> methodMap;

  static {
    methodMap = new HashMap<>();
    methodMap.put(OPTIONS.name(), OPTIONS);
    methodMap.put(REQMOD.name(), REQMOD);
    methodMap.put(RESPMOD.name(), RESPMOD);
  }

  public static IcapMethod valueOf(String name) {
    IcapMethod result = methodMap.get(name);
    return result != null ? result : new IcapMethod(name);
  }
}
