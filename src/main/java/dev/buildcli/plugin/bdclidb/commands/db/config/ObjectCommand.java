package dev.buildcli.plugin.bdclidb.commands.db.config;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.plugin.bdclidb.commands.db.ConfigCommand;
import dev.buildcli.plugin.bdclidb.models.DbConnection;
import dev.buildcli.plugin.bdclidb.models.DbObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

import static dev.buildcli.core.utils.config.ConfigContextLoader.*;
import static dev.buildcli.core.utils.input.InteractiveInputUtils.*;
import static dev.buildcli.plugin.bdclidb.constants.ConfigConstants.BUILD_CLI_OBJECT;
import static dev.buildcli.plugin.bdclidb.utils.ConnectionUtils.loadConnections;
import static dev.buildcli.plugin.bdclidb.utils.ObjectUtils.loadObjects;

@Command(name = "object", aliases = {"obj", "o"}, description = "Set DbObjects to interact with database", mixinStandardHelpOptions = true)
public class ObjectCommand implements BuildCLICommand {
  private final Logger logger = LoggerFactory.getLogger(ObjectCommand.class);
  @ParentCommand
  private ConfigCommand parent;

  @Option(names = {"--name", "-n"}, description = "Object name")
  private String name;

  @Option(names = {"--sql", "-s"}, description = "Object SQL")
  private String sql;

  @Option(names = {"--connection", "-c"}, description = "Object connection name")
  private String connection;

  private BuildCLIConfig getConfig() {
    return parent.isLocal() ? getLocalConfig() : getGlobalConfig();
  }

  @Override
  public void run() {
    var config = getConfig();
    var objects = new HashMap<>(loadObjects(config));
    var connections = new HashMap<>(loadConnections(config));
    var connectionNames = connections.values().stream().filter(Objects::nonNull).map(DbConnection::name).toList();

    if (Stream.of(name, sql, connection).anyMatch(Objects::nonNull)) {
      var name = this.name != null ? this.name : question("Enter object name");
      var sql = this.sql != null ? this.sql : question("Enter object sql");
      var connection = this.connection != null ? this.connection :
          (connectionNames.isEmpty() ? question("Enter object connection name") : options("Select a connection", connectionNames));

      var object = new DbObject(name, sql, connection);

      if (objects.containsKey(name) && confirm("Are you sure you want overwrite this object?")) {
        objects.put(name, object);
      } else if (!objects.containsKey(name) && confirm("Are you sure you want add this object?")) {
        objects.put(name, object);
      }

    } else {

      do {
        var name = question("Enter object name");
        var sql = question("Enter object sql");
        var connection = connectionNames.isEmpty() ? question("Enter object connection name") : options("Select a connection", connectionNames);

        var object = new DbObject(name, sql, connection);

        if (objects.containsKey(name) && confirm("Are you sure you want overwrite this object?")) {
          objects.put(name, object);
        } else if (!objects.containsKey(name) && confirm("Are you sure you want add this object?")) {
          objects.put(name, object);
        }

      } while (confirm("Do you want to continue?"));
    }

    for (var entry : objects.entrySet()) {
      var name = entry.getKey();
      var object = entry.getValue();

      config.addOrSetProperty("%s.%s.name".formatted(BUILD_CLI_OBJECT, name), name);
      config.addOrSetProperty("%s.%s.sql".formatted(BUILD_CLI_OBJECT, name), object.sql());
      config.addOrSetProperty("%s.%s.connection".formatted(BUILD_CLI_OBJECT, name), object.connection());
    }

    saveConfig(config, parent.isLocal());
  }

  private void saveConfig(BuildCLIConfig config, boolean isLocal) {
    if (isLocal) {
      saveLocalConfig(config);
    } else {
      saveGlobalConfig(config);
    }
  }
}
