package net.xdob.icap4j.codec;


public class IcapRequestDecoder extends IcapMessageDecoder {

  public IcapRequestDecoder() {
    super();
  }


  @Override
  protected FullIcapRequest createMessage(String[] initialLine) {
    return new DefaultFullIcapRequest(IcapVersion.valueOf(initialLine[2]), IcapMethod.valueOf(initialLine[0]), initialLine[1], "");
  }

}
