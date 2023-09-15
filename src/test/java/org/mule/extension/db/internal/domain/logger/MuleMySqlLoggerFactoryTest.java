package org.mule.extension.db.internal.domain.logger;

import com.mysql.cj.log.Log;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Empty;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperMethod;
import net.bytebuddy.implementation.bind.annotation.This;
import org.junit.Test;
import org.mule.extension.db.api.logger.MuleMySqlLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static net.bytebuddy.implementation.MethodDelegation.to;
import static net.bytebuddy.implementation.bytecode.member.MethodInvocation.invoke;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class MuleMySqlLoggerFactoryTest {

  public static final String LOG_MESSAGE = "Logging";
  private ClassLoader classLoader;


  @Test
  public void testMuleMysqlLogger()
      throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    this.classLoader = Thread.currentThread().getContextClassLoader();
    Class<?> availableMySqlLogInterface = getAvailableMySqlLogInterface();
    MuleMySqlLogger delegatedLogger = mock(MuleMySqlLogger.class);

    MuleMySqlLogger interfaceToMuleMySqlLogger = new ByteBuddy()
        .subclass(delegatedLogger.getClass())
        .implement(availableMySqlLogInterface)
        .method(isDeclaredBy(availableMySqlLogInterface))
        .intercept(to(delegatedLogger))
        .make()
        .load(this.classLoader)
        .getLoaded()
        .getConstructor(String.class)
        .newInstance("MySql");

    interfaceToMuleMySqlLogger.logInfo(LOG_MESSAGE);
    verify(delegatedLogger, times(1)).logInfo(LOG_MESSAGE);
  }

  @Test
  public void testMysqlInterface()
      throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
    this.classLoader = Thread.currentThread().getContextClassLoader();
    Class<?> availableMySqlLogInterface = getAvailableMySqlLogInterface();
    MuleMySqlLogger delegatedLogger = mock(MuleMySqlLogger.class);

    Object interfaceToMuleMySqlLogger = new ByteBuddy()
        .subclass(availableMySqlLogInterface)
        //            .implement(availableMySqlLogInterface)
        .method(isDeclaredBy(availableMySqlLogInterface))
        .intercept(to(delegatedLogger))
        .make()
        .load(this.classLoader)
        .getLoaded()
        //            .getConstructor(String.class)
        //            .newInstance("MySql");
        .newInstance();
    ((Log) interfaceToMuleMySqlLogger).logInfo(LOG_MESSAGE);
    verify(delegatedLogger).logInfo(LOG_MESSAGE);
  }

  private Class<?> getAvailableMySqlLogInterface() {
    try {
      return classLoader.loadClass("com.mysql.cj.log.Log");
    } catch (ClassNotFoundException e) {
      try {
        return classLoader.loadClass("com.mysql.jdbc.log.Log");
      } catch (ClassNotFoundException ex) {
        throw new IllegalArgumentException(
                                           "Neither class, com.mysql.cj.log.Log or com.mysql.jdbc.log.Log, were found. " +
                                               "An unsupported driver was provided.",
                                           ex);
      }
    }
  }

  static class TestInterceptor {

    @RuntimeType
    public Object intercept(@This Object obj, @Origin Method method, @AllArguments Object[] args,
                            @SuperMethod(nullIfImpossible = true) Method superMethod, @Empty Object defaultValue)
        throws Throwable {
      return null;
    }
  }

}
