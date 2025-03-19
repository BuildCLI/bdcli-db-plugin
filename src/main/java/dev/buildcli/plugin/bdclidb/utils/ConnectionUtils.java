package dev.buildcli.plugin.bdclidb.utils;

import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.plugin.bdclidb.models.DbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static dev.buildcli.plugin.bdclidb.constants.ConfigConstants.BUILD_CLI_CONNECTION;

public final class ConnectionUtils {
  private static final Logger logger = LoggerFactory.getLogger(ConnectionUtils.class);

  private static final Map<String, String> DRIVER_MAP = Map.of(
      "mysql", "com.mysql.cj.jdbc.Driver",
      "postgresql", "org.postgresql.Driver",
      "oracle", "oracle.jdbc.OracleDriver",
      "sqlite", "org.sqlite.JDBC",
      "h2", "org.h2.Driver"
  );

  private ConnectionUtils() {
  }

  public static Map<String, DbConnection> loadConnections(BuildCLIConfig config) {
    var connections = new HashMap<String, DbConnection>();
    var pattern = Pattern.compile("^buildcli\\.db\\.connection\\.([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)$");

    for (var item : config.getProperties().stream().filter(conf -> conf.name().startsWith(BUILD_CLI_CONNECTION)).toList()) {
      var matcher = pattern.matcher(item.name());

      if (matcher.matches()) {
        var connectionName = matcher.group(1);
        var property = matcher.group(2);

        DbConnection connection;
        if (connections.containsKey(connectionName)) {
          connection = connections.get(connectionName);

        } else {
          connection = new DbConnection(connectionName, null, null, null, null);

        }
        setDbConnectionProperty(connections, item, property, connectionName, connection);
      }

    }

    return connections;
  }

  public static Connection connect(DbConnection connection) {
    try {

      loadDriver(connection.platform());

      var drivers = DriverManager.getDrivers();

      while (drivers.hasMoreElements()) {
        var driver = drivers.nextElement();
        System.out.println(driver);
      }

      return DriverManager.getConnection(connection.url(), connection.user(), connection.password());
    } catch (SQLException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static void loadDriver(String platform) throws ClassNotFoundException {
    System.out.println(platform);
    if (!DRIVER_MAP.containsKey(platform)) {
      throw new IllegalArgumentException("Unknown platform: " + platform);
    }

    Class.forName(DRIVER_MAP.get(platform));
  }

  private static void setDbConnectionProperty(HashMap<String, DbConnection> connections, BuildCLIConfig.ImmutableProperty item, String property, String connectionName, DbConnection connection) {
    switch (property) {
      case "url" -> {
        connection = connection.withUrl(item.value());
      }
      case "user" -> {
        connection = connection.withUser(item.value());
      }
      case "password" -> {
        connection = connection.withPassword(item.value());
      }
      case "platform" -> {
        connection = connection.withPlatform(item.value());
      }
      case "name" -> {
      }
      default -> {
        logger.warn("Unknown property '{}'", property);
      }
    }

    connections.put(connectionName, connection);
  }
}
