package dev.buildcli.plugin.bdclidb.commands.db.config;

import dev.buildcli.core.domain.BuildCLICommand;
import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.plugin.bdclidb.commands.db.ConfigCommand;
import dev.buildcli.plugin.bdclidb.models.DbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

import static dev.buildcli.core.utils.config.ConfigContextLoader.*;
import static dev.buildcli.core.utils.input.InteractiveInputUtils.confirm;
import static dev.buildcli.core.utils.input.InteractiveInputUtils.question;
import static dev.buildcli.plugin.bdclidb.constants.ConfigConstants.BUILD_CLI_CONNECTION;
import static dev.buildcli.plugin.bdclidb.utils.ConnectionUtils.loadConnections;

@Command(name = "connection", aliases = {"con", "c"}, description = "Database connections config", mixinStandardHelpOptions = true)
public class ConnectionCommand implements BuildCLICommand {
  private final Logger logger = LoggerFactory.getLogger(ConnectionCommand.class);
  @ParentCommand
  private ConfigCommand parent;

  @Option(names = {"--name", "-n"}, description = "Connection name")
  private String name;

  @Option(names = {"--url", "-U"}, description = "Connection url")
  private String url;

  @Option(names = {"--user", "-u"}, description = "Connection user")
  private String user;

  @Option(names = {"--platform", "-P"}, description = "Connection platform")
  private String platform;

  private BuildCLIConfig getConfig() {
    return parent.isLocal() ? getLocalConfig() : getGlobalConfig();
  }

  @Override
  public void run() {
    var config = getConfig();

    var connections = new HashMap<>(loadConnections(config));

    System.out.println(connections);

    if (Stream.of(name, url, user, platform).anyMatch(Objects::nonNull)) {
      var name = this.name != null ? this.name : question("Please enter a connection name");
      var url = this.url != null ? this.url : question("Please enter a connection url");
      var user = this.user != null ? this.user : question("Please enter a connection user");
      var password = question("Please enter a connection password");
      var platform = this.platform != null ? this.platform : question("Please enter a connection platform");
      addConnection(connections, name, url, user, password, platform);

    } else {
      do {
        var name = question("Please enter a connection name");
        var url = question("Please enter a connection url");
        var user = question("Please enter a connection user");
        var password = question("Please enter a connection password");
        var platform = question("Please enter a connection platform");

        addConnection(connections, name, url, user, password, platform);

      } while (confirm("Do you want to continue?"));
    }

    for (var entry : connections.entrySet()) {
      var connection = entry.getValue();
      var name = entry.getKey().toLowerCase();
      config.addOrSetProperty("%s.%s.name".formatted(BUILD_CLI_CONNECTION, name), connection.name());
      config.addOrSetProperty("%s.%s.url".formatted(BUILD_CLI_CONNECTION, name), connection.url());
      config.addOrSetProperty("%s.%s.user".formatted(BUILD_CLI_CONNECTION, name), connection.user());
      config.addOrSetProperty("%s.%s.password".formatted(BUILD_CLI_CONNECTION, name), connection.password());
      config.addOrSetProperty("%s.%s.platform".formatted(BUILD_CLI_CONNECTION, name), connection.platform());
    }

    saveConfig(config, parent.isLocal());
  }

  private void addConnection(HashMap<String, DbConnection> connections, String name, String url, String user, String password, String platform) {
    var connection = new DbConnection(name, url, user, password, platform);

    if (connections.containsKey(name.toLowerCase()) && confirm("Are you sure you want to overwrite this connection?")) {
      connections.put(name.toLowerCase(), connection);
    } else if (!connections.containsKey(name.toLowerCase()) &&confirm("Are you sure you want to add this connection?")) {
      connections.put(name.toLowerCase(), connection);
    }
  }

  private void saveConfig(BuildCLIConfig config, boolean isLocal) {
    if (isLocal) {
      saveLocalConfig(config);
    } else {
      saveGlobalConfig(config);
    }
  }

}
