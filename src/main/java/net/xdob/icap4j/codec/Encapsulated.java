package net.xdob.icap4j.codec;


import io.netty.buffer.ByteBuf;

import java.io.UnsupportedEncodingException;
import java.util.*;


public final class Encapsulated {

  private List<Entry> entries;

  public Encapsulated() {
    entries = new ArrayList<Encapsulated.Entry>();
  }

  /**
   * Creates an instance based on the value given.
   *
   * @param headerValue valid Encapsulated value.
   */
  public Encapsulated(String headerValue) {
    this();
    parseHeaderValue(headerValue);
  }

  /**
   * Gets whether a given entry exists in the header value.
   *
   * @param entity the entity such as REQHDR, RESHDR and so on.
   * @return boolean true if the entity in question is present.
   */
  public boolean containsEntry(IcapElEnum entity) {
    for (Entry entry : entries) {
      if (entry.getName().equals(entity)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets whether the message contains a body entity. This can be any valid body entry
   * including the null-body entity which indicates that the message does not contain.
   * a body.
   *
   * @return the correct @see {@link IcapElEnum} value.
   */
  public IcapElEnum containsBodyEntry() {
    IcapElEnum body = null;
    for (Entry entry : entries) {
      if (entry.getName().equals(IcapElEnum.OPTBODY)) {
        body = entry.getName();
        break;
      } else if (entry.getName().equals(IcapElEnum.REQBODY)) {
        body = entry.getName();
        break;
      } else if (entry.getName().equals(IcapElEnum.RESBODY)) {
        body = entry.getName();
        break;
      } else if (entry.getName().equals(IcapElEnum.NULLBODY)) {
        body = entry.getName();
        break;
      }
    }
    return body;
  }

  /**
   * Iterator method. This method will provide the next available
   * Entry. Eventually it will return null.
   * Whether to return the next valid entry in the list dependens on the
   *
   * @return @see {@link IcapElEnum} or null if no more entries are available.
   * @see Encapsulated#setEntryAsProcessed(IcapElEnum) method.
   */
  public IcapElEnum getNextEntry() {
    IcapElEnum entryName = null;
    for (Entry entry : entries) {
      if (!entry.isProcessed()) {
        entryName = entry.getName();
        break;
      }
    }
    return entryName;
  }


  /**
   * reports that a given entry was processed and that the @see Encapsulated#getNextEntry()
   * can now return the next entry in line or null if no more are present.
   *
   * @param entryName the entry that was procesed.
   */
  public void setEntryAsProcessed(IcapElEnum entryName) {
    Entry entry = getEntryByName(entryName);
    if (entry != null) {
      entry.setIsProcessed();
    }
  }

  /**
   * Sets an entry with it's corresponding position.
   *
   * @param name     the name of the Entry.
   * @param position the position of the entry within the icap message.
   */
  public void addEntry(IcapElEnum name, int position) {
    Entry entry = new Entry(name, position);
    entries.add(entry);
  }

  public String getString() {
    StringBuilder builder = new StringBuilder();
    Collections.sort(entries);
    builder.append("Encapsulated: ");
    Iterator<Entry> entryIterator = entries.iterator();
    while (entryIterator.hasNext()) {
      Entry entry = entryIterator.next();
      builder.append(entry.getName().getValue());
      builder.append("=");
      builder.append(Integer.toString(entry.getPosition()));
      if (entryIterator.hasNext()) {
        builder.append(',').append(IcapCodecUtil.SPACE);
      }
    }
    return builder.toString();
  }


  public int encode(ByteBuf buffer) throws UnsupportedEncodingException {
    int index = buffer.readableBytes();
    Collections.sort(entries);
    buffer.writeBytes("Encapsulated: ".getBytes(IcapCodecUtil.ASCII_CHARSET));
    Iterator<Entry> entryIterator = entries.iterator();
    while (entryIterator.hasNext()) {
      Entry entry = entryIterator.next();
      buffer.writeBytes(entry.getName().getValue().getBytes(IcapCodecUtil.ASCII_CHARSET));
      buffer.writeBytes("=".getBytes(IcapCodecUtil.ASCII_CHARSET));
      buffer.writeBytes(Integer.toString(entry.getPosition()).getBytes(IcapCodecUtil.ASCII_CHARSET));
      if (entryIterator.hasNext()) {
        buffer.writeByte(',');
        buffer.writeByte(IcapCodecUtil.SPACE);
      }
    }
    buffer.writeBytes(IcapCodecUtil.CRLF);
    buffer.writeBytes(IcapCodecUtil.CRLF);
    return buffer.readableBytes() - index;
  }

  /*
  REQMOD request: 	 [req-hdr] req-body
  REQMOD response: 	{[req-hdr] req-body} | {[res-hdr] res-body}
  RESPMOD request:	 [req-hdr] [res-hdr] res-body
  RESPMOD response:	 [res-hdr] res-body
  OPTIONS response:	 opt-body
   */
  private void parseHeaderValue(String headerValue) {
    if (headerValue == null) {
      throw new IcapDecodingError("No value associated with Encapsualted header");
    }
    StringTokenizer tokenizer = new StringTokenizer(headerValue, ",");
    while (tokenizer.hasMoreTokens()) {
      String parameterString = tokenizer.nextToken();
      if (parameterString != null) {
        String[] parameter = splitParameter(parameterString.trim());
        try {
          int value = Integer.parseInt(parameter[1]);
          Entry entry = new Entry(IcapElEnum.fromString(parameter[0]), value);
          entries.add(entry);
        } catch (NumberFormatException nfe) {
          throw new IcapDecodingError("the Encapsulated header value [" + parameter[1] + "] for the key [" + parameter[0] + "] is not a number");
        }
      }
    }
    Collections.sort(entries);
  }

  private String[] splitParameter(String parameter) {
    int offset = parameter.indexOf('=');
    if (offset <= 0) {
      throw new IcapDecodingError("Encapsulated header value was not understood [" + parameter + "]");
    }
    String key = parameter.substring(0, offset);
    String value = parameter.substring(offset + 1, parameter.length());
    if (value.contains(",")) {
      value = value.substring(0, value.indexOf(','));
    }
    return new String[]{key.trim(), value};
  }

  private Entry getEntryByName(IcapElEnum entryName) {
    Entry returnValue = null;
    for (Entry entry : entries) {
      if (entry.getName().equals(entryName)) {
        returnValue = entry;
        break;
      }
    }
    return returnValue;
  }

  final static class Entry implements Comparable<Entry> {

    private final IcapElEnum name;
    private final Integer position;
    private boolean processed;

    public Entry(IcapElEnum name, Integer position) {
      this.name = name;
      this.position = position;
    }

    public IcapElEnum getName() {
      return name;
    }

    public int getPosition() {
      return position;
    }

    public void setIsProcessed() {
      processed = true;
    }

    public boolean isProcessed() {
      return processed;
    }

    @Override
    public int compareTo(Entry entry) {
      if (this.name.equals(IcapElEnum.NULLBODY)) {
        return 1;
      }
      return this.position.compareTo(entry.position);
    }

    @Override
    public String toString() {
      return name + "=" + position + " : " + processed;
    }
  }

  @Override
  public String toString() {
    return getString();
  }
}
