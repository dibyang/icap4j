package net.xdob.icap4j.codec;


public enum IcapElEnum {
  REQHDR("req-hdr"),
  RESHDR("res-hdr"),
  REQBODY("req-body"),
  RESBODY("res-body"),
  OPTBODY("opt-body"),
  NULLBODY("null-body");

  private String value;

  IcapElEnum(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static IcapElEnum fromString(String value) {
    if (value != null) {
      for (IcapElEnum entryName : IcapElEnum.values()) {
        if (value.equalsIgnoreCase(entryName.getValue())) {
          return entryName;
        }
      }
    }
    return null;
  }
}
