# BuildCLI DB Plugin Documentation

The BuildCLI DB Plugin is an official plugin designed to integrate database functionalities into BuildCLI. It provides the ability to configure database connections and objects, as well as to connect to databases and interact with data using an interactive REPL (Read-Evaluate-Print Loop).

## Table of Contents

1. [Overview](#overview)
2. [Plugin Architecture](#plugin-architecture)
3. [Available Commands](#available-commands)
    - [database (db)](#database-db)
    - [config](#config)
        - [connection](#connection)
        - [object](#object)
    - [connect](#connect)
4. [Usage Examples](#usage-examples)
5. [Configuration Scopes](#configuration-scopes)
6. [Building the Plugin](#building-the-plugin)
7. [License](#license)

---

## Overview

The BuildCLI DB Plugin provides the following main features:
- **Configuring Database Connections:** Easily add or modify connection settings (URL, user, password, platform) using the `connection` command.
- **Configuring Database Objects:** Define SQL queries (objects) that allow you to retrieve data from your database.
- **Connecting and Data Retrieval:** Use the `connect` command to connect to the configured database, execute SQL queries for your defined objects, and open an interactive REPL session with the retrieved results.

The plugin is built using Java and Maven, and it integrates with the BuildCLI framework using Picocli for command line interactions.

---

## Plugin Architecture

The project is organized into the following main packages and files:

- **Main Command:**
    - `BdcliDBCommand.java`: Registers the main command under the name `database` (with alias `db`). It delegates to subcommands and displays help when run without a subcommand.

- **Configuration Commands:**
    - `ConfigCommand.java`: Parent command for database configuration.
    - `ConnectionCommand.java`: Configures database connection settings (name, URL, user, password, platform).
    - `ObjectCommand.java`: Configures database objects that define SQL queries for interacting with the database.

- **Connection Command:**
    - `ConnectCommand.java`: Loads the database connections and objects, connects to the database, retrieves data (only for SQL queries starting with "select"), and then starts a REPL session where each object’s data is available as a variable.

- **Utility Classes:**
    - Utility classes (such as `ConnectionUtils` and `ObjectUtils`) handle the loading of configurations, connections, and objects.
    - The REPL functionality is implemented in `Repl.java` and `ReplFunctions.java`.

- **Constants, Models, and Enums:**
    - These include configuration constants, models for `DbConnection`, `DbObject`, and the `Scope` (which defines whether the configuration is local or global), as well as the `ConfigType` enum.

---

## Available Commands

### database (db)

The main entry point of the plugin. Running this command without any subcommands displays the usage information.

- **Command:** `database` or `db`
- **Description:** Main command for BuildCLI DB Plugin.
- **Example:**
  ```bash
  buildcli db --help
  ```

### config

This command is used to configure database connections and objects. It offers two subcommands: `connection` and `object`.

#### connection

Configures database connection details. You can either supply the parameters directly via command-line options or use the interactive mode if options are omitted.

- **Command:** `database config connection` (aliases: `con`, `c`)
- **Options:**
    - `--name, -n`: The name of the connection.
    - `--url, -U`: The JDBC URL for the database.
    - `--user, -u`: The username for database access.
    - `--platform, -P`: The database platform (e.g., mysql, postgresql).
- **Behavior:**
    - If all required options are provided, the connection is added (or overwritten with confirmation) directly.
    - If options are missing, the plugin prompts the user for input.
- **Example (Non-interactive):**
  ```bash
  buildcli db config connection --name mydb --url jdbc:mysql://localhost:3306/mydb --user root --platform mysql
  ```
- **Example (Interactive):**  
  Simply run:
  ```bash
  buildcli db config connection
  ```
  The plugin will then prompt for the connection name, URL, user, password, and platform.

#### object

Configures database objects that define SQL queries to interact with the database. Each object typically includes a name, the associated connection, and an SQL statement.

- **Command:** `database config object` (aliases: `obj`, `o`)
- **Options:**  
  While specific options may vary, typical parameters include:
    - `--name`: The name of the database object.
    - `--connection`: The name of the associated database connection.
    - `--sql`: The SQL query to retrieve data (should begin with "select" for retrieval purposes).
- **Behavior:**
    - Similar to the connection command, parameters can be provided via options or entered interactively.
- **Example (Non-interactive):**
  ```bash
  buildcli db config object --name usersList --connection mydb --sql "SELECT * FROM users"
  ```
- **Example (Interactive):**  
  Simply run:
  ```bash
  buildcli db config object
  ```
  The plugin will prompt for the necessary parameters.

### connect

Connects to the configured database and executes the SQL queries defined in the objects. It retrieves data from the database and opens an interactive REPL session where each object's results are stored as a variable.

- **Command:** `database connect` (aliases: `con`)
- **Description:** Connects to the database using the configured connections and objects.
- **Behavior:**
    - Loads connections and objects from the configuration.
    - Executes SQL queries that begin with the keyword "select".
    - Stores the retrieved results in a map (keyed by the object name).
    - Launches a REPL session where you can work with the data interactively.
- **Example:**
  ```bash
  buildcli db connect
  ```
  Once connected, you can work within the REPL environment where, for example, you might type:
  ```groovy
  println(usersList)
  ```
  to display the data retrieved by the `usersList` object.

---

## Usage Examples

### 1. Display Help Information

To view all available commands and options:
```bash
buildcli db --help
```

### 2. Configure a Database Connection Non-Interactively

```bash
buildcli db config connection --name mydb --url jdbc:mysql://localhost:3306/mydb --user root --platform mysql
```
The command will prompt you to enter the connection password and confirm any overwrite if the connection already exists.

### 3. Configure a Database Object Non-Interactively

```bash
buildcli db config object --name usersList --connection mydb --sql "SELECT * FROM users"
```
This command sets up an object that will retrieve all users from the database.

### 4. Connect and Start REPL

```bash
buildcli db connect
```
This command will:
- Load the configured connections and objects.
- Connect to the specified database.
- Execute the SQL queries (only for queries starting with "select").
- Open an interactive REPL session where the results are stored in variables (e.g., `usersList`).

---

## Configuration Scopes

The plugin supports both **local** and **global** configurations. The configuration scope is determined by the `Scope` setting:
- **Local Configuration:** Changes affect only the current project.
- **Global Configuration:** Changes apply system-wide.

If no specific scope is provided, the plugin defaults to local configuration. When configuring connections or objects, you can change the scope based on your needs, and the plugin will save the configuration accordingly.

---

## Building the Plugin

The plugin is built using Maven. To compile and package the plugin along with its dependencies, run the following command in the project root:
```bash
mvn clean package
```
This command creates a JAR file (with dependencies) named `bdclidb.jar` (or similar) that can be used with BuildCLI.

---

## License

This plugin is distributed under the [MIT License](LICENSE).
