package dev.buildcli.plugin.bdclidb.utils.repl;

import org.jline.terminal.Terminal;

import java.util.Map;
import java.util.Set;

public class ReplFunctions {

  /**
   * Clears the terminal screen.
   */
  public static void clearScreen(Repl repl) {
    Terminal terminal = repl.getTerminal();
    // ANSI escape code to clear screen
    terminal.writer().print("\033[H\033[2J");
    terminal.writer().flush();
  }

  /**
   * Prints help information and usage examples.
   */
  public static void printHelp(Repl repl) {
    Terminal terminal = repl.getTerminal();
    terminal.writer().println("Available commands:");
    terminal.writer().println("  :help           - Show this help and usage examples");
    terminal.writer().println("  :functions      - List registered functions");
    terminal.writer().println("  :vars           - List defined variables");
    terminal.writer().println("  :inspect <name> - Inspect a variable or database object");
    terminal.writer().println("  :tables         - List available database tables");
    terminal.writer().println("  :clear          - Clear the screen");
    terminal.writer().println("  :debug          - Toggle debug mode");
    terminal.writer().println("  exit/quit       - Exit the REPL");
    terminal.writer().println("");
    terminal.writer().println("Examples:");
    terminal.writer().println("  def x = 10");
    terminal.writer().println("  var y = 20");
    terminal.writer().println("  x = 30  (Python-style assignment)");
    terminal.writer().println("  def myFunction() { println 'Hello' }");
    terminal.writer().println("");
    terminal.writer().println("Any other input will be evaluated as Groovy code.");
    terminal.writer().flush();
  }

  /**
   * Lists all registered functions.
   */
  public static void listFunctions(Repl repl) {
    Terminal terminal = repl.getTerminal();
    if (repl.getScriptFunctions().isEmpty()) {
      terminal.writer().println("No functions registered");
    } else {
      terminal.writer().println("Registered functions:");
      for (String name : repl.getScriptFunctions().keySet()) {
        terminal.writer().println("  " + name + "()");
      }
    }
    terminal.writer().flush();
  }

  /**
   * Lists all defined variables.
   */
  public static void listVariables(Repl repl) {
    Terminal terminal = repl.getTerminal();
    terminal.writer().println("Defined variables:");
    var entries = (Set<Map.Entry<String, Object>>)repl.getBinding().getVariables().entrySet();
    if (entries.isEmpty()) {
      terminal.writer().println("No variables registered");
      return;
    }

    for (var entry : entries) {
      if (entry.getKey().equals("terminal") || entry.getKey().equals("reader") ||
          entry.getKey().equals("repl") || entry.getKey().startsWith("_")) {
        continue;
      }
      terminal.writer().println("  " + entry.getKey());
    }
    terminal.writer().flush();
  }

  /**
   * Inspects an object (variable or database object).
   *
   * @param variableName The name of the object.
   */
  public static void inspectObject(Repl repl, String variableName) {
    Terminal terminal = repl.getTerminal();
    try {
      Object obj = repl.getBinding().getVariable(variableName);
      if (obj == null) {
        terminal.writer().println("Variable '" + variableName + "' is null");
      } else {
        String inspect = repl.getGroovyShell().evaluate(variableName + ".inspect()").toString();
        terminal.writer().println(inspect);
      }
    } catch (Exception e) {
      terminal.writer().println("Cannot inspect '" + variableName + "': " + e.getMessage());
    }
    terminal.writer().flush();
  }

  /**
   * Lists available database tables (if any).
   */
  public static void listTables(Repl repl) {
    Terminal terminal = repl.getTerminal();
    try {
      Object result = repl.getGroovyShell().evaluate("if (binding.hasVariable('db')) { db.getTables() } else { 'No database connection available' }");
      terminal.writer().println(result);
    } catch (Exception e) {
      terminal.writer().println("Error listing tables: " + e.getMessage());
    }
    terminal.writer().flush();
  }

  /**
   * Toggles debug mode.
   */
  public static void toggleDebug(Repl repl) {
    boolean current = repl.isDebugMode();
    repl.setDebugMode(!current);
    repl.getTerminal().writer().println("Debug mode " + (repl.isDebugMode() ? "enabled" : "disabled"));
    repl.getTerminal().writer().flush();
  }
}
