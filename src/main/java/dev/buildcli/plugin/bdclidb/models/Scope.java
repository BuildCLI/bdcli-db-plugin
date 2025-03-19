package dev.buildcli.plugin.bdclidb.models;

import picocli.CommandLine;

public class Scope {
  @CommandLine.Option(names = {"--global", "-g"}, description = "Global scope", defaultValue = "false")
  private boolean global;
  @CommandLine.Option(names = {"--local", "-l"}, description = "Local scope", defaultValue = "true")
  private boolean local;

  public boolean isGlobal() {
    return global;
  }

  public boolean isLocal() {
    return local;
  }
}