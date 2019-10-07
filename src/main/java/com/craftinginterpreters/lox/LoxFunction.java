package com.craftinginterpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure;
  private final boolean isInitializer;

  public LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.closure = closure;
    this.declaration = declaration;
    this.isInitializer = isInitializer;
  }

  LoxFunction bind(LoxInstance instance) {
    // Create a new env nestled in the original method's closure, a sort of a closure in a closure
    // Becomes the parent of the method body's environment (second diagram with the new 'synthetic' environment)
    Environment environment = new Environment(closure);
    // Declare 'this' in the new parent environment and bind it to the provided instance
    environment.define("this", instance);
    // for isInitializer, pass on the original method's value
    return new LoxFunction(declaration, environment, isInitializer);
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    Environment environment = new Environment(closure);
    for (int i = 0; i < arguments.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i));
    }
    try {
      interpreter.executeBlock(declaration.body, environment);
    } catch (Return returnValue) {
      if (isInitializer) {
        return closure.getAt(0, "this");
      }
      return returnValue.value;
    }

    if (isInitializer) {
      return closure.getAt(0, "this");
    }
    return null;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}
