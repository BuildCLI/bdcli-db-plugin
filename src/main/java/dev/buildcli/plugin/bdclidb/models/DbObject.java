package dev.buildcli.plugin.bdclidb.models;

public record DbObject(String name, String sql, String connection) {
  public DbObject withSql(String value) {
    return new DbObject(name, value, connection);
  }

  public DbObject withConnection(String connection) {
    return new DbObject(name, sql, connection);
  }
}
