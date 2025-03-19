package dev.buildcli.plugin.bdclidb.commands.db;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.core.utils.config.ConfigContextLoader;
import dev.buildcli.plugin.bdclidb.models.Scope;
import dev.buildcli.plugin.bdclidb.utils.ConnectionUtils;
import dev.buildcli.plugin.bdclidb.utils.repl.Repl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

import java.util.*;

import static dev.buildcli.plugin.bdclidb.utils.ConnectionUtils.loadConnections;
import static dev.buildcli.plugin.bdclidb.utils.ObjectUtils.loadObjects;
import static java.util.Objects.nonNull;

@Command(name = "connect", aliases = {"con"}, description = "Connect with database and navigate with objects",
    mixinStandardHelpOptions = true
)
public class ConnectCommand implements BuildCLICommand {
  private final Logger logger = LoggerFactory.getLogger(ConnectCommand.class);

  @ArgGroup
  private Scope scope;

  @Override
  public void run() {
    var config = getConfig();

    var connections = loadConnections(config);
    var objects = loadObjects(config);
    var objectsLoaded = 0;

    System.out.println(connections.values());
    System.out.println(objects.values());

    Map<String, List<Map<String, Object>>> mapObjects = new HashMap<>();

    logger.info("Loading objects from database...");
    for (var connection : connections.values()) {
      var objectsByConnectionName = objects.values().stream()
          .filter(o -> nonNull(o.connection()))
          .filter(o -> o.connection().equals(connection.name()))
          .filter(o -> nonNull(o.sql()) && !o.sql().isEmpty())
          .filter(o -> o.sql().toLowerCase().startsWith("select"))
          .toList();

      if (objectsByConnectionName.isEmpty()) {
        continue;
      }

      logger.info("Connecting to database {}", connection.name());
      try (var con = ConnectionUtils.connect(connection)) {
        logger.info("Connected to database {}", connection.name());

        logger.info("Retrieving objects from database {}", connection.name());

        try (var stmt = con.createStatement()) {
          for (var o : objectsByConnectionName) {
            var rs = stmt.executeQuery(o.sql());
            var rows = new LinkedList<Map<String, Object>>();
            while (rs.next()) {
              var row = new LinkedHashMap<String, Object>();
              for (var i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                var columnName = rs.getMetaData().getColumnName(i);
                row.put(columnName, rs.getObject(i));
              }

              if (!row.isEmpty()) {
                rows.add(row);
                objectsLoaded++;
              }
            }

            mapObjects.put(o.name(), rows);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

    }
    logger.info("Objects loaded from database: {}.", objectsLoaded);

    var repl = new Repl();

    for (var entry : mapObjects.entrySet()) {
      repl.setVariable(entry.getKey(), entry.getValue());
    }

    repl.start();

  }

  public BuildCLIConfig getConfig() {
    return scope == null || scope.isLocal() ? ConfigContextLoader.getLocalConfig() : ConfigContextLoader.getGlobalConfig();
  }
}
