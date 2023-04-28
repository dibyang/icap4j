package net.xdob.icap4j;

import java.util.concurrent.TimeoutException;

public class TimeoutValueException extends TimeoutException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new exception for the given timeout deadline and actual timeout.
   *
   * @param timeoutDeadline How long was the expected timeout in milliseconds.
   * @param timeoutActual   How long we actually waited in milliseconds.
   * @return a new TimeoutValueException.
   */
  public static TimeoutValueException fromMilliseconds(final long timeoutDeadline, final long timeoutActual) {
    return new TimeoutValueException(min0(timeoutDeadline), min0(timeoutActual));
  }

  /**
   * Returns the given {@code value} if positive, otherwise returns 0.
   *
   * @param value any timeout
   * @return the given {@code value} if positive, otherwise returns 0.
   */
  private static long min0(final long value) {
    return value < 0 ? 0 : value;
  }

  private final long actual;

  private final long deadline;

  /**
   * Creates a new exception for the given timeout deadline and actual timeout.
   *
   * @param deadline How long was the expected timeout.
   * @param actual   How long we actually waited.
   */
  public TimeoutValueException(final long deadline, final long actual) {
    super(String.format("Timeout deadline: %s, actual: %s", deadline, actual));
    this.actual = actual;
    this.deadline = deadline;
  }

  /**
   * Gets how long was the expected timeout in milliseconds.
   *
   * @return how long was the expected timeout in milliseconds.
   */
  public long getActual() {
    return actual;
  }

  /**
   * Gets how long we actually waited in milliseconds.
   *
   * @return how long we actually waited in milliseconds.
   */
  public long getDeadline() {
    return deadline;
  }

}
