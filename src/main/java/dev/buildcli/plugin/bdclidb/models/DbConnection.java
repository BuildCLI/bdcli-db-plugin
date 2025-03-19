package dev.buildcli.plugin.bdclidb.models;

public record DbConnection(String name, String url, String user, String password, String platform) {
  public DbConnection withUrl(String url) {
    return new DbConnection(name, url, user, password, platform);
  }

  public DbConnection withUser(String user) {
    return new DbConnection(name, url, user, password, platform);
  }

  public DbConnection withPassword(String password) {
    return new DbConnection(name, url, user, password, platform);
  }

  public DbConnection withPlatform(String platform) {
    return new DbConnection(name, url, user, password, platform);
  }
}
