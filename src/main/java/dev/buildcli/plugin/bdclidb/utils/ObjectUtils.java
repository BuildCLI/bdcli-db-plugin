package dev.buildcli.plugin.bdclidb.utils;

import dev.buildcli.core.domain.configs.BuildCLIConfig;
import dev.buildcli.plugin.bdclidb.models.DbObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static dev.buildcli.plugin.bdclidb.constants.ConfigConstants.BUILD_CLI_OBJECT;

public final class ObjectUtils {
  private static final Logger logger = LoggerFactory.getLogger(ObjectUtils.class);

  private ObjectUtils() {
  }

  public static Map<String, DbObject> loadObjects(BuildCLIConfig config) {
    var objects = new HashMap<String, DbObject>();
    String regex = "^buildcli\\.db\\.object\\.([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)$";
    Pattern pattern = Pattern.compile(regex);

    for (var item : config.getProperties().stream().filter(conf -> conf.name().startsWith(BUILD_CLI_OBJECT)).toList()) {
      var matcher = pattern.matcher(item.name());

      if (matcher.matches()) {
        var objectName = matcher.group(1);
        var property = matcher.group(2);

        DbObject object;
        if (objects.containsKey(objectName)) {
          object = objects.get(objectName);
        } else {
          object = new DbObject(objectName, null, null);
        }
        setDbObjectProperty(objects, item, property, objectName, object);
      }

    }
    return objects;
  }

  private static void setDbObjectProperty(HashMap<String, DbObject> objects, BuildCLIConfig.ImmutableProperty item, String property, String objectName, DbObject object) {
    System.out.println(property);
    switch (property) {
      case "name" -> {
      }
      case "sql" -> {
        object = object.withSql(item.value());
      }
      case "connection" -> {
        object = object.withConnection(item.value());
      }
      default -> {
        logger.warn("Unknown property: {}", property);
      }
    }

    objects.put(objectName, object);
  }
}
