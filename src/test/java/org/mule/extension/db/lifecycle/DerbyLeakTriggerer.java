package org.mule.extension.db.lifecycle;

import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;

public class DerbyLeakTriggerer implements Runnable {

  private static final Logger LOGGER = getLogger(DerbyLeakTriggerer.class);

  @Override
  public void run() {
    try {
      Class<?> driverClass =
          Thread.currentThread().getContextClassLoader().loadClass(DerbyArtifactLifecycleListenerTestCase.DRIVER_NAME);
      Driver embeddedDriver = (Driver) driverClass.getDeclaredConstructor().newInstance();
      DriverManager.registerDriver(embeddedDriver);
      String urlConnection = "jdbc:derby:myDB;create=true;user=me;password=mine";


      try (Connection con = DriverManager.getConnection(urlConnection)) {
        try (Statement statement = con.createStatement()) {
          String sql = "SELECT 1 FROM (VALUES(1)) AS DummyTable";
          statement.execute(sql);
        }
      } catch (SQLException e) {
        LOGGER.error("Connection could not be established: {}", e.getMessage(), e);
        fail("Connection could not be established");
      }


//      Connection con = DriverManager.getConnection(urlConnection);
//      Statement statement = con.createStatement();
//      String sql = "SELECT 1 FROM (VALUES(1)) AS DummyTable";
//      statement.execute(sql);
//    } catch (SQLException e) {
//      LOGGER.error(e.getMessage(), e);
//      fail("Connection could not be established");
    } catch (ReflectiveOperationException | SQLException e) {
      LOGGER.error(e.getMessage(), e);
      fail("Could not load the driver");
    }
  }
}
