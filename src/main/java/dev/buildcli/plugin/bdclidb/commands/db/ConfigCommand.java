package dev.buildcli.plugin.bdclidb.commands.db;

import dev.buildcli.plugin.bdclidb.commands.db.config.ConnectionCommand;
import dev.buildcli.plugin.bdclidb.commands.db.config.ObjectCommand;
import dev.buildcli.plugin.bdclidb.models.Scope;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(name = "config", aliases = {"c"}, description = "Config database connection (global or local) and dbObjects",
    mixinStandardHelpOptions = true, subcommands = {ConnectionCommand.class, ObjectCommand.class}
)
public class ConfigCommand {
  @ArgGroup
  private Scope scope;

  public boolean isLocal() {
    return scope == null || scope.isLocal();
  }
}
