package com.arkonas.ranks.formula;

import java.util.Map;

/**
 * A tiny arithmetic expression evaluator used by the cost-formula system to generate rank
 * requirement values from a formula instead of hand-authoring each rank. Pure (no Bukkit), so it
 * is fully unit-testable.
 *
 * <p>Grammar (recursive descent):</p>
 * <pre>
 *   expr    := term (('+' | '-') term)*
 *   term    := power (('*' | '/' | '%') power)*
 *   power   := unary ('^' power)?          // right-associative
 *   unary   := ('-' | '+') unary | primary
 *   primary := number | variable | func '(' expr (',' expr)* ')' | '(' expr ')'
 * </pre>
 *
 * <p>Variables may be written bare ({@code index}) or braced ({@code {index}}); both resolve to the
 * same name. Supported functions: {@code min max pow} (2-arg) and {@code floor ceil round abs sqrt}
 * (1-arg). Unknown variables/functions and syntax errors throw {@link IllegalArgumentException}.</p>
 */
public final class MathExpression {

  /** A compiled node in the expression tree. */
  @FunctionalInterface
  interface Node {
    double eval(Map<String, Double> vars);
  }

  private final Node root;
  private final String source;

  private MathExpression(Node root, String source) {
    this.root = root;
    this.source = source;
  }

  public static MathExpression compile(String source) {
    if (source == null || source.isBlank()) {
      throw new IllegalArgumentException("Empty expression");
    }
    Parser parser = new Parser(source);
    Node node = parser.parseExpression();
    parser.expectEnd();
    return new MathExpression(node, source);
  }

  public double eval(Map<String, Double> vars) {
    return root.eval(vars);
  }

  public String getSource() {
    return source;
  }

  // --- Parser -------------------------------------------------------------------------------

  private static final class Parser {
    private final String s;
    private int pos;

    Parser(String s) {
      this.s = s;
    }

    Node parseExpression() {
      Node node = parseTerm();
      while (true) {
        char c = peek();
        if (c == '+') {
          next();
          Node left = node;
          Node right = parseTerm();
          node = v -> left.eval(v) + right.eval(v);
        } else if (c == '-') {
          next();
          Node left = node;
          Node right = parseTerm();
          node = v -> left.eval(v) - right.eval(v);
        } else {
          return node;
        }
      }
    }

    private Node parseTerm() {
      Node node = parsePower();
      while (true) {
        char c = peek();
        if (c == '*') {
          next();
          Node left = node;
          Node right = parsePower();
          node = v -> left.eval(v) * right.eval(v);
        } else if (c == '/') {
          next();
          Node left = node;
          Node right = parsePower();
          node = v -> left.eval(v) / right.eval(v);
        } else if (c == '%') {
          next();
          Node left = node;
          Node right = parsePower();
          node = v -> left.eval(v) % right.eval(v);
        } else {
          return node;
        }
      }
    }

    private Node parsePower() {
      Node base = parseUnary();
      if (peek() == '^') {
        next();
        Node exp = parsePower(); // right-associative
        Node b = base;
        return v -> Math.pow(b.eval(v), exp.eval(v));
      }
      return base;
    }

    private Node parseUnary() {
      char c = peek();
      if (c == '-') {
        next();
        Node operand = parseUnary();
        return v -> -operand.eval(v);
      }
      if (c == '+') {
        next();
        return parseUnary();
      }
      return parsePrimary();
    }

    private Node parsePrimary() {
      char c = peek();
      if (c == '(') {
        next();
        Node node = parseExpression();
        expect(')');
        return node;
      }
      if (c == '{') {
        return parseBracedVariable();
      }
      if (Character.isDigit(c) || c == '.') {
        return parseNumber();
      }
      if (Character.isLetter(c) || c == '_') {
        return parseIdentifier();
      }
      throw error("Unexpected character '" + c + "'");
    }

    private Node parseNumber() {
      int start = pos;
      while (pos < s.length() && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
        pos++;
      }
      double value;
      try {
        value = Double.parseDouble(s.substring(start, pos));
      } catch (NumberFormatException e) {
        throw error("Bad number '" + s.substring(start, pos) + "'");
      }
      return v -> value;
    }

    private Node parseBracedVariable() {
      next(); // consume '{'
      int start = pos;
      while (pos < s.length() && s.charAt(pos) != '}') {
        pos++;
      }
      if (pos >= s.length()) {
        throw error("Unterminated '{' variable");
      }
      String name = s.substring(start, pos).trim();
      next(); // consume '}'
      if (name.isEmpty()) {
        throw error("Empty variable name");
      }
      return variableNode(name);
    }

    private Node parseIdentifier() {
      int start = pos;
      while (pos < s.length()
          && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_')) {
        pos++;
      }
      String name = s.substring(start, pos);
      if (peek() == '(') {
        return parseFunctionCall(name);
      }
      return variableNode(name);
    }

    private Node parseFunctionCall(String name) {
      expect('(');
      Node first = parseExpression();
      java.util.List<Node> args = new java.util.ArrayList<>();
      args.add(first);
      while (peek() == ',') {
        next();
        args.add(parseExpression());
      }
      expect(')');
      return function(name, args);
    }

    private static Node variableNode(String name) {
      String key = name.toLowerCase(java.util.Locale.ROOT);
      return v -> {
        Double value = v.get(key);
        if (value == null) {
          value = v.get(name);
        }
        if (value == null) {
          throw new IllegalArgumentException("Unknown variable: " + name);
        }
        return value;
      };
    }

    private Node function(String rawName, java.util.List<Node> args) {
      String name = rawName.toLowerCase(java.util.Locale.ROOT);
      switch (name) {
        case "floor":
          return unary(name, args, Math::floor);
        case "ceil":
          return unary(name, args, Math::ceil);
        case "round":
          return unary(name, args, x -> (double) Math.round(x));
        case "abs":
          return unary(name, args, Math::abs);
        case "sqrt":
          return unary(name, args, Math::sqrt);
        case "min":
          return binary(name, args, Math::min);
        case "max":
          return binary(name, args, Math::max);
        case "pow":
          return binary(name, args, Math::pow);
        default:
          throw error("Unknown function: " + rawName);
      }
    }

    private Node unary(String name, java.util.List<Node> args, java.util.function.DoubleUnaryOperator op) {
      if (args.size() != 1) {
        throw error("Function " + name + " expects 1 argument, got " + args.size());
      }
      Node a = args.get(0);
      return v -> op.applyAsDouble(a.eval(v));
    }

    private Node binary(String name, java.util.List<Node> args, java.util.function.DoubleBinaryOperator op) {
      if (args.size() != 2) {
        throw error("Function " + name + " expects 2 arguments, got " + args.size());
      }
      Node a = args.get(0);
      Node b = args.get(1);
      return v -> op.applyAsDouble(a.eval(v), b.eval(v));
    }

    void expectEnd() {
      if (peek() != '\0') {
        throw error("Unexpected trailing input");
      }
    }

    private char peek() {
      skipWhitespace();
      return pos < s.length() ? s.charAt(pos) : '\0';
    }

    private void next() {
      skipWhitespace();
      pos++;
    }

    private void expect(char c) {
      if (peek() != c) {
        throw error("Expected '" + c + "'");
      }
      next();
    }

    private void skipWhitespace() {
      while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
        pos++;
      }
    }

    private IllegalArgumentException error(String message) {
      return new IllegalArgumentException(message + " in expression: " + s);
    }
  }
}
