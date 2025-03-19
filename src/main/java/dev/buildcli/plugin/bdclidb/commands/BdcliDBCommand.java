package dev.buildcli.plugin.bdclidb.commands;

import dev.buildcli.plugin.BuildCLICommandPlugin;
import dev.buildcli.plugin.bdclidb.commands.db.ConfigCommand;
import dev.buildcli.plugin.bdclidb.commands.db.ConnectCommand;
import picocli.CommandLine.Command;

import static picocli.CommandLine.usage;

@Command(name = "database", aliases = {"db"}, description = "", mixinStandardHelpOptions = true,
    subcommands = {ConfigCommand.class, ConnectCommand.class}
)
public class BdcliDBCommand extends BuildCLICommandPlugin {
  @Override
  public void run() {
    usage(this, System.out);
  }

  @Override
  public String version() {
    return "0.0.1-SNAPSHOT";
  }

  @Override
  public String name() {
    return "bdcli-db";
  }

  @Override
  public String description() {
    return "Build CLI Plugin";
  }

  @Override
  public String[] parents() {
    return null;
  }
}
