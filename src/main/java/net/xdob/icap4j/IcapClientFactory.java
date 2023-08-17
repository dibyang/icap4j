package net.xdob.icap4j;

public interface IcapClientFactory {
  int DEFAULT_PORT = 1344;
  IcapClient getClient(String ip, int port);
  void shutdown();
  int getNodeMaxConn();
  void setNodeMaxConn(int nodeMaxConn);
}
