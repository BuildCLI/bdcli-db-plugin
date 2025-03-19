package dev.buildcli.plugin.bdclidb.utils.repl;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Repl {
  private final Terminal terminal;
  private final LineReader reader;
  private final GroovyShell groovyShell;
  private final Binding binding;
  private final Map<String, Script> scriptFunctions = new HashMap<>();
  private boolean debugMode = false;

  public Repl() {
    try {
      // Terminal configuration
      terminal = TerminalBuilder.builder()
          .name("db")
          .system(true)
          .build();

      // Completer configuration with basic commands and dynamic suggestions
      List<String> baseCommands = List.of(":help", ":functions", ":vars", ":inspect", ":tables", ":clear", ":debug", "exit", "quit");
      Completer completer = new AggregateCompleter(
          new StringsCompleter(baseCommands),
          new StringsCompleter(scriptFunctions.keySet()),
          new StringsCompleter(bindingVariablesPlaceholder())
      );

      // LineReader configuration with history and completer
      reader = LineReaderBuilder.builder()
          .terminal(terminal)
          .parser(new DefaultParser())
          .completer(completer)
          .history(new DefaultHistory())
          .variable(LineReader.HISTORY_FILE, System.getProperty("user.home") + "/.db_repl_history")
          .build();

      // GroovyShell configuration with binding
      CompilerConfiguration config = new CompilerConfiguration();
      binding = new Binding();
      groovyShell = new GroovyShell(getClass().getClassLoader(), binding, config);

      // Add internal variables to the binding
      binding.setVariable("terminal", terminal);
      binding.setVariable("reader", reader);
      binding.setVariable("repl", this);

      // Pre-register a greeting function
      registerScript("saudacao", "println 'Olá! Seja bem-vindo ao DB REPL'");

    } catch (IOException e) {
      throw new RuntimeException("Failed to initialize REPL: " + e.getMessage(), e);
    }
  }

  /**
   * Helper method to populate the existing binding variables.
   */
  private String[] bindingVariablesPlaceholder() {
    if (binding == null) {
      return new String[0];
    }

    var vars = binding.getVariables();
    var keys = new String[vars.size()];

    for (int i = 0; i < vars.size(); i++) {
      keys[i] = vars.keySet().toArray()[i].toString();
    }

    return keys;
  }

  /**
   * Registers a function (script) in the REPL.
   *
   * @param name          The name of the function.
   * @param scriptContent The script content.
   */
  public void registerScript(final String name, final String scriptContent) {
    try {
      // Wrap the script in a function if it is not already wrapped
      String funcScript;
      if (scriptContent.trim().startsWith("def " + name)) {
        funcScript = scriptContent;
      } else {
        funcScript = "def " + name + "() {\n" + scriptContent + "\n}";
      }

      // Compile and evaluate the script to define the function
      Script script = groovyShell.parse(funcScript);
      script.run();
      scriptFunctions.put(name, script);

      printSuccess("Registered function: " + name);
    } catch (Exception e) {
      printError("Failed to register script: " + e.getMessage());
      if (debugMode) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Sets a variable in the Groovy binding.
   *
   * @param name  The variable name.
   * @param value The variable value.
   */
  public void setVariable(final String name, final Object value) {
    binding.setVariable(name, value);
    printSuccess("Variable set: " + name);
  }

  /**
   * Evaluates a Groovy expression.
   *
   * @param expression The expression to evaluate.
   * @return The result of the evaluation.
   */
  public Object evaluateGroovy(final String expression) {
    try {
      String trimmedExpression = expression.trim();

      // Case 1: Declaration with "def"
      if (trimmedExpression.startsWith("def ")) {
        // If it's a function definition, handle it specifically
        if (trimmedExpression.contains("(") && trimmedExpression.contains(")")) {
          handleFunctionDefinition(trimmedExpression);
          return null;
        }
        return handleVariableDeclaration(trimmedExpression);
      }
      // Case 2: Declaration with "var"
      else if (trimmedExpression.startsWith("var ")) {
        return handleVariableDeclaration(trimmedExpression);
      }
      // Case 3: Python-style assignment
      else if (isPythonStyleAssignment(trimmedExpression)) {
        String varName = trimmedExpression.substring(0, trimmedExpression.indexOf('=')).trim();
        Object result = groovyShell.evaluate(trimmedExpression);
        binding.setVariable(varName, groovyShell.evaluate(varName));
        return result;
      }

      // Regular evaluation for other cases
      return groovyShell.evaluate(trimmedExpression);
    } catch (Exception e) {
      printError("Error evaluating expression: " + e.getMessage());
      if (debugMode) {
        e.printStackTrace();
      }
      return null;
    }
  }

  /**
   * Handles function definition.
   */
  private void handleFunctionDefinition(String expression) {
    try {
      // Extract the function name
      String funcName = expression.substring(4, expression.indexOf('(')).trim();

      // Evaluate the expression to define the function
      groovyShell.evaluate(expression);

      // Create a wrapper method to call the function via the binding
      String wrapperCode = "def " + funcName + "Wrapper = { args -> " +
          "    return " + funcName + "(*args)" +
          "}";
      groovyShell.evaluate(wrapperCode);

      // Retrieve the wrapper and add it to the binding
      Object wrapper = groovyShell.evaluate(funcName + "Wrapper");
      binding.setVariable(funcName, wrapper);

      printSuccess("Function defined: " + funcName);
      scriptFunctions.put(funcName, null);
    } catch (Exception e) {
      printError("Error defining function: " + e.getMessage());
      if (debugMode) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Handles variable declarations with "def" or "var".
   * This implementation extracts the variable name and the expression,
   * evaluates the expression, and sets the variable in the binding.
   */
  private Object handleVariableDeclaration(String expression) {
    // Padrão para capturar "def" ou "var", seguido do nome da variável e a expressão após o "="
    Pattern pattern = Pattern.compile("^(def|var)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.+)$");
    Matcher matcher = pattern.matcher(expression);
    if (matcher.find()) {
      String varKeyword = matcher.group(1); // "def" ou "var" (não utilizado, mas pode ser útil para logs)
      String varName = matcher.group(2);      // Nome da variável
      String valueExpr = matcher.group(3);    // Expressão para o valor

      // Avalia a expressão do valor
      Object value = groovyShell.evaluate(valueExpr);
      // Armazena a variável no binding global
      binding.setVariable(varName, value);
      printSuccess("Variable set: " + varName);
      return value;
    }
    // Caso não corresponda ao padrão, tenta avaliar normalmente
    return groovyShell.evaluate(expression);
  }

  /**
   * Checks if the expression is a Python-style assignment.
   */
  private boolean isPythonStyleAssignment(String expression) {
    if (!expression.contains("=")) return false;
    int equalsPos = expression.indexOf('=');
    if (equalsPos > 0 &&
        (expression.charAt(equalsPos - 1) == '=' ||
            expression.charAt(equalsPos - 1) == '>' ||
            expression.charAt(equalsPos - 1) == '<' ||
            expression.charAt(equalsPos - 1) == '!')) {
      return false;
    }
    if (equalsPos < expression.length() - 1 && expression.charAt(equalsPos + 1) == '=') {
      return false;
    }
    String varName = expression.substring(0, equalsPos).trim();
    return !varName.isEmpty() && Character.isJavaIdentifierStart(varName.charAt(0));
  }

  /**
   * Prints an error message in red.
   */
  private void printError(String message) {
    terminal.writer().println(
        new AttributedStringBuilder()
            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED))
            .append(message)
            .toAnsi()
    );
    terminal.writer().flush();
  }

  /**
   * Prints a success message in green.
   */
  private void printSuccess(String message) {
    terminal.writer().println(
        new AttributedStringBuilder()
            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
            .append(message)
            .toAnsi()
    );
    terminal.writer().flush();
  }

  /**
   * Prints a message to the terminal.
   */
  public void println(String message) {
    terminal.writer().println(message);
    terminal.writer().flush();
  }

  /**
   * Reads multiline input if the code block is incomplete.
   */
  private String readMultiline(String initialLine) {
    StringBuilder codeBuilder = new StringBuilder(initialLine);
    while (!isCodeComplete(codeBuilder.toString())) {
      String nextLine = reader.readLine("... ");
      codeBuilder.append("\n").append(nextLine);
    }
    return codeBuilder.toString();
  }

  /**
   * Simple check to determine if the code is complete (balanced braces).
   */
  private boolean isCodeComplete(String code) {
    int openBraces = 0;
    int closeBraces = 0;
    for (char c : code.toCharArray()) {
      if (c == '{') {
        openBraces++;
      } else if (c == '}') {
        closeBraces++;
      }
    }
    return openBraces == closeBraces;
  }

  /**
   * Starts the REPL.
   */
  public void start() {
    printSuccess("DB REPL started. Type 'exit' to quit.");
    printSuccess("Type ':help' for available commands.");

    while (true) {
      try {
        String line = reader.readLine("db > ");
        if (line == null) {
          continue;
        }
        line = line.trim();
        if (line.isEmpty()) {
          continue;
        }
        // Check if the user wants to exit
        if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
          printSuccess("Exiting REPL...");
          break;
        }
        // Handle special commands starting with ':'
        if (line.startsWith(":")) {
          handleSpecialCommand(line);
          continue;
        } else {
          // Support for multiline input if the code is incomplete
          if (!isCodeComplete(line)) {
            line = readMultiline(line);
          }
          Object result = evaluateGroovy(line);
          if (result != null) {
            terminal.writer().println(result);
            terminal.writer().flush();
          }
        }
      } catch (UserInterruptException e) {
        printError("Interrupted");
      } catch (EndOfFileException e) {
        printSuccess("Exiting REPL...");
        break;
      } catch (Exception e) {
        printError("Error: " + e.getMessage());
        if (debugMode) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Handles special commands (starting with ':').
   */
  private void handleSpecialCommand(String command) {
    String[] parts = command.substring(1).split("\\s+", 2);
    String cmd = parts[0].toLowerCase();
    String args = parts.length > 1 ? parts[1] : "";

    switch (cmd) {
      case "help":
        ReplFunctions.printHelp(this);
        break;
      case "functions":
        ReplFunctions.listFunctions(this);
        break;
      case "vars":
        ReplFunctions.listVariables(this);
        break;
      case "inspect":
        if (!args.isEmpty()) {
          ReplFunctions.inspectObject(this, args);
        } else {
          printError("Usage: :inspect <variable-name>");
        }
        break;
      case "tables":
        ReplFunctions.listTables(this);
        break;
      case "clear":
        ReplFunctions.clearScreen(this);
        break;
      case "debug":
        ReplFunctions.toggleDebug(this);
        break;
      default:
        printError("Unknown command: " + cmd);
        ReplFunctions.printHelp(this);
        break;
    }
  }

  /**
   * Returns the Groovy binding.
   */
  public Binding getBinding() {
    return binding;
  }

  /**
   * Returns the GroovyShell.
   */
  public GroovyShell getGroovyShell() {
    return groovyShell;
  }

  /**
   * Returns the Terminal.
   */
  public Terminal getTerminal() {
    return terminal;
  }

  /**
   * Checks if debug mode is enabled.
   */
  public boolean isDebugMode() {
    return debugMode;
  }

  /**
   * Sets the debug mode.
   */
  public void setDebugMode(boolean debugMode) {
    this.debugMode = debugMode;
  }

  /**
   * Returns the registered script functions.
   */
  public Map<String, Script> getScriptFunctions() {
    return scriptFunctions;
  }
}
