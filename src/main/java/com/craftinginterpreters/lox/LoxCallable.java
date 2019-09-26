package com.craftinginterpreters.lox;

import java.util.List;

  // Implementer's job is to return the value the call expr produces
interface LoxCallable {
  int arity();
  // The class impl call() may need interpreter
  Object call(Interpreter interpreter, List<Object> arguments);
}
