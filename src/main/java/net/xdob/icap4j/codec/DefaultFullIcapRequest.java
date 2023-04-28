package net.xdob.icap4j.codec;


/**
 * Main Icap Request implementation. This is the starting point to create a Icap request.
 */
public class DefaultFullIcapRequest extends AbstractIcapMessage implements FullIcapRequest {

  private IcapMethod method;
  private String uri;
  private String host;
  private String service;


  public DefaultFullIcapRequest(IcapVersion icapVersion, IcapMethod method, String uri, String host) {
    super(icapVersion);
    this.method = method;
    this.uri = uri;
    this.setHost(host);
  }

  public DefaultFullIcapRequest(IcapMethod method, String host) {
    this(IcapVersion.ICAP_1_0, method, "", host);
  }

  private DefaultFullIcapRequest(IcapMethod method) {
    this(method, "");
    //this.addHeader(IcapHeaders.Names.ALLOW,"204");
  }

  public FullIcapRequest setMethod(IcapMethod method) {
    this.method = method;
    return this;
  }

  public IcapMethod getMethod() {
    return method;
  }


  @Override
  public String getUri() {
    return uri;
  }

  @Override
  public FullIcapRequest setUri(String uri) {
    this.uri = uri;
    return this;
  }

  public String getHost() {
    return host;
  }

  public FullIcapRequest setHost(String host) {
    this.host = host;
    this.headers().set(IcapHeaders.Names.HOST, host);
    return this;
  }

  public String getService() {
    return service;
  }

  public FullIcapRequest setService(String service) {
    this.service = service;
    return this;
  }

  public static FullIcapRequest options(String host, String service) {
    String uri = String.format("icap://%s/%s", host, service);
    DefaultFullIcapRequest request = new DefaultFullIcapRequest(IcapMethod.OPTIONS);
    request.setHost(host).setUri(uri).setService(service);
    return request;
  }

  public static FullIcapRequest reqmod(String host, String service) {
    String uri = String.format("icap://%s/%s", host, service);
    FullIcapRequest request = new DefaultFullIcapRequest(IcapMethod.REQMOD);
    return request.setUri(uri).setHost(host).setService(service);
  }


  public static FullIcapRequest respmod(String host, String service) {
    String uri = String.format("icap://%s/%s", host, service);
    FullIcapRequest request = new DefaultFullIcapRequest(IcapMethod.RESPMOD);
    request.setUri(uri).setHost(host).setService(service);
    return request;
  }

}
