package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  final Environment globals = new Environment();
  private Environment environment = globals;

  Interpreter() {
    // functions and variables occupy the same namespace
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() {
        return 0;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)System.currentTimeMillis()/1000.0;
      }

      @Override
      public String toString() {
        return "<native fn>";
      }
    });
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch(RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private String stringify(Object object) {
    if (object == null) {
      return "nil";
    }

    // Hack to work around Java adding ".0" to integer valued doubles
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }
    return object.toString();
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) {
        return left;
      }
    } else {
      if (!isTruthy(left)) {
        return left;
      }
    }

    return evaluate(expr.right);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double)right;
    }
    // unreachable
    return null;
  }

  private void checkNumberOperand(Token operator, Object operand) {
      if (operand instanceof Double) {
        return;
      }
      throw new RuntimeError(operator, "Operand must be a number");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) {
      return;
    }
    throw new RuntimeError(operator, "Operands must be numbers");
  }

  private void checkDivisibleByZero(Token operator, Object right) {
    if(right instanceof Double && (Double)right == 0.0) {
      throw new RuntimeError(operator, "Cannot divide by zero");
    }
  }

  private boolean isTruthy(Object object) {
    if (object == null){
      return false;
    }
    if (object instanceof Boolean) {
      return (boolean)object;
    }
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) {
      return true;
    }
    if (a == null) {
      return false;
    }
    return a.equals(b);
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  private void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double)left > (double)right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double)right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left < (double)right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left <= (double)right;
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left - (double)right;
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        checkDivisibleByZero(expr.operator, right);
        return (double)left / (double)right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        }

        if(left instanceof String || right instanceof String) {
          return stringify(left) + stringify(right);
        }
        throw new RuntimeError(expr.operator, "Operands must be two numbers or one must be a string");
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double)left * (double)right;
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL:
        return isEqual(left, right);
    }

    // unreachable
    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {         
    // Typically this expression is just an identifier that looks up the function by name
    Object callee = evaluate(expr.callee);

    // Subtle semantic choice here, since the args could have side effects, order eval may be user visible
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    if(!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes");
    }

    LoxCallable function = (LoxCallable)callee;

    // Take the Python approach to arity - if the args don't match the functions arity, throw
    if (function.arity() != arguments.size()) {
      throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }
    
    return function.call(this, arguments);
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    environment.assign(expr.name, value);
    return value;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }
}
