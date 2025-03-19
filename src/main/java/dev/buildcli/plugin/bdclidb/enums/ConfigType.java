package dev.buildcli.plugin.bdclidb.enums;

import picocli.CommandLine.ITypeConverter;

public enum ConfigType {
  CONNECTION, DBOBJECT;

  public static ConfigType fromString(String s) {
    return switch (s) {
      case "connection" -> CONNECTION;
      case "dbobject" -> DBOBJECT;
      default -> throw new IllegalStateException("Unexpected value: " + s);
    };
  }

  public static class ConfigTypeConverter implements ITypeConverter<ConfigType> {
    @Override
    public ConfigType convert(String s) throws Exception {
      if (s == null || s.isEmpty()) {
        return null;
      }

      return fromString(s.toLowerCase());
    }
  }
}
