package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

// Runtime representation of a LoxClass
public class LoxInstance {
    private LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    public LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LoxFunction method = klass.findMethod(name.lexeme);
        // Need to create an environment for 'this' keyword that is encountered
        // Semi-confusingly enough, we're passing in the Java 'this' as an arg to bind()
        if (method != null) {
            // 'this' (the arg) is the function that we will use to bind the keyword 'this' (in JLox to)
            return method.bind(this);
        }
        // Design decision - throw instead of implicitly returning nil
        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}
