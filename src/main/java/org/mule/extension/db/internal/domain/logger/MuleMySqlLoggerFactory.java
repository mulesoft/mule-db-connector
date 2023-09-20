/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.logger;

import org.mule.extension.db.api.logger.MuleMySqlLogger;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.DynamicType;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;

/**
 * Factory class that creates proxy instances of {@link MuleMySqlLogger} with the provided
 * delegated logger and class loader.
 */
public class MuleMySqlLoggerFactory {

  public static final String MYSQL_DRIVER_CLASS = "com.mysql.jdbc.Driver";
  public static final String NEW_MYSQL_DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
  private ClassLoader classLoader;
  private MuleMySqlLogger delegatedLogger;

  public MuleMySqlLoggerFactory(ClassLoader classLoader, MuleMySqlLogger delegatedLogger) {
    this.classLoader = classLoader;
    this.delegatedLogger = delegatedLogger;
  }

  /**
   * This method uses ByteBuddy to dynamically create a subclass of the provided
   * delegated logger class that implements the available Log interface (either com.mysql.cj.log.Log or com.mysql.jdbc.log.Log).
   * The intercepted methods from the available Log interface are delegated to
   * the provided delegated logger
   *
   * @return A new instance of the proxy {@link MuleMySqlLogger} with the specified
   * configuration.
   * @throws MuleRuntimeException If an error occurs while creating the proxy instance,
   *                              such as instantiation, access, or I/O exceptions.
   */
  public MuleMySqlLogger create() {
    Class<?> availableMySqlLogInterface = getAvailableMySqlLogInterface();

    DynamicType.Builder.MethodDefinition.ReceiverTypeDefinition<? extends MuleMySqlLogger> typeDefinition = new ByteBuddy()
        .subclass(MuleMySqlLogger.class)
        .implement(availableMySqlLogInterface)
        .method(isDeclaredBy(MuleMySqlLogger.class))
        .intercept(to(delegatedLogger));

    try (DynamicType.Unloaded<? extends MuleMySqlLogger> dynamicType = typeDefinition
        .make()) {
      return dynamicType.load(this.classLoader, new ClassLoadingStrategy.ForBootstrapInjection(null, null))
          .getLoaded()
          .getConstructor(String.class)
          .newInstance("MySql");
    } catch (
        InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | IOException e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of " + getClass().getName()), e);
    }
  }

  private Class<?> getAvailableMySqlLogInterface() {
    try {
      return classLoader.loadClass("com.mysql.cj.log.Log");
    } catch (ClassNotFoundException e) {
      try {
        return classLoader.loadClass("com.mysql.jdbc.log.Log");
      } catch (ClassNotFoundException ex) {
        throw new IllegalArgumentException("Neither class, com.mysql.cj.log.Log or com.mysql.jdbc.log.Log, were found. " +
            "An unsupported driver was provided.", ex);
      }
    }
  }

}
