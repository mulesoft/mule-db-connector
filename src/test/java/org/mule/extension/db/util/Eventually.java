/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.util;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.mule.tck.probe.Timeout;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Should the matcher be satisfied with delay?
 */
public class Eventually<T> extends BaseMatcher<T> {

  private final Matcher<T> matcher;
  private static final int TIMEOUT_SECS = 10;
  private static final long RETRY_INTERVAL = 100;

  /**
   * Creates a matcher that retry the matcher until it is satisfied or until the timeout.
   *
   * @param matcher the matcher that will be executed until the timeout
   */
  public static <T> Eventually<T> eventually(Matcher<T> matcher) {
    return new Eventually<>(matcher);
  }

  private Eventually(Matcher<T> matcher) {
    this.matcher = matcher;
  }

  @Override
  public boolean matches(Object item) {
    Timeout timeout = new Timeout(SECONDS.toMillis(TIMEOUT_SECS));
    while (!matcher.matches(item)) {
      if (timeout.hasTimedOut()) {
        return false;
      }
      try {
        sleep(RETRY_INTERVAL);
      } catch (InterruptedException e) {
        throw new RuntimeException("Interrupted while waiting for condition", e);
      }
    }
    return true;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("after " + TIMEOUT_SECS + " seconds ").appendDescriptionOf(matcher);
  }
}
