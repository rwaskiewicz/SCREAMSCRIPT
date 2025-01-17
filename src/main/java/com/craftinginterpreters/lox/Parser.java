package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while(!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }

  private Stmt declaration() {
    try {
      if (match(CLASS)) {
        return classDeclaration();
      }
      if (match(FUN)) {
        return function("FUNCTION");
      }
      if (match(VAR)) {
        return varDeclaration();
      }
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "EXPECT CLASS NAME!");

    Expr.Variable superclass = null;
    if (match(LESS)) {
      consume(IDENTIFIER, "EXPECT SUPERCLASS NAME!");
      superclass = new Expr.Variable(previous());
    }

    consume(LEFT_BRACE, "EXPECT '{' BEFORE CLASS BODY!");

    List<Stmt.Function> methods = new ArrayList<>();
    while(!check(RIGHT_BRACE) && !isAtEnd()) {
      methods.add(function("METHOD"));
    }

    consume(RIGHT_BRACE, "EXPECT '}' AFTER CLASS BODY!");
    return new Stmt.Class(name, superclass, methods);
  }

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "EXPECT VARIABLE NAME!");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "EXPECT ';' AFTER VARIABLE DECLARATION!");
    return new Stmt.Var(name, initializer);
  }

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "EXPECT '(' AFTER 'WHILE'.");
    Expr condition = expression();
    consume(RIGHT_PAREN, "EXPECT ')' AFTER CONDITION!");
    Stmt body = statement();

    return new Stmt.While(condition, body);
  }

  private Stmt statement() {
    if (match(FOR)) {
      return forStatement();
    }
    if (match(IF)) {
      return ifStatement();
    }
    if (match(PRINT)) {
      return printStatement();
    }
    if (match(RETURN)) {
      return returnStatement();
    }
    if (match(WHILE)) {
      return whileStatement();
    }
    if (match(LEFT_BRACE)) {
      return new Stmt.Block(block());
    }

    return expressionStatement();
  }

  private Stmt forStatement() {
    consume(LEFT_PAREN, "EXPECT '(' AFTER 'FOR'!");

    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "EXPECT ';' AFTER LOOP CONDITION!");

    Expr increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "EXPECT ')' AFTER FOR CLAUSES!");

    Stmt body = statement();

    if (increment != null) {
      body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
    }

    if (condition == null) {
      condition = new Expr.Literal(true);
    }
    body = new Stmt.While(condition, body);

    if(initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "EXPECT '(' AFTER IF!");
    Expr condition = expression();
    consume(RIGHT_PAREN, "EXPECT ')' AFTER IF CONDITION!");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "EXPECT ';' AFTER VALUE!");
    return new Stmt.Print(value);
  }

  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    if (!check(SEMICOLON)) {
      value = expression();
    }
    consume(SEMICOLON, "EXPECT ';' AFTER RETURN VALUE!");
    return new Stmt.Return(keyword, value);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "EXPECT ';' AFTER EXPRESSION!");
    return new Stmt.Expression(expr);
  }

  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, "EXPECT " + kind + " NAME!");
    consume(LEFT_PAREN, "EXPECT '(' AFTER " + kind + " NAME!");
    List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "CANNOT HAVE MORE THAN 255 PARAMETERS!");
        }

        parameters.add(consume(IDENTIFIER, "EXPECT PARAMETER NAME!"));
      } while(match(COMMA));
    }
    consume(RIGHT_PAREN, "EXPECT ')' AFTER PARAMETERS");

    consume(LEFT_BRACE, "EXPECT '{' BEFORE " + kind + " BODY");
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
  }

  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while(!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "EXPECT '}' AFTER BLOCK!");
    return statements;
  }

  private Expr assignment() {
    Expr expr = or();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get)expr;
        return new Expr.Set(get.object, get.name, value);
      }
      error(equals, "INVALID ASSIGNMENT TARGET!");
    }

    return expr;
  }

  private Expr or() {                                
    Expr expr = and();

    while (match(OR)) {                              
      Token operator = previous();                   
      Expr right = and();                            
      expr = new Expr.Logical(expr, operator, right);
    }                                                

    return expr;                                     
  }   

  private Expr and() {                               
    Expr expr = equality();

    while (match(AND)) {                             
      Token operator = previous();                   
      Expr right = equality();                       
      expr = new Expr.Logical(expr, operator, right);
    }                                                

    return expr;                                     
  }  
  
  private Expr expression() {
    return assignment();
  }

  private Expr equality() {
    Expr expr = comparison();

    while(match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = addition();

    while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = addition();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr addition() {
    Expr expr = multiplication();

    while(match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = multiplication();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr multiplication() {
    Expr expr = unary();

    while(match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary () {
    if(match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return call();
  }

  private Expr call() {
    Expr expr = primary();

    while(true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER, "EXPECT A PROPERTY NAME AFTER '.'@");
        expr = new Expr.Get(expr, name);
      } else {
        break;
      }
    }

    return expr;
  }

  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "CANNOT HAVE MORE THAN 255 ARGUMENTS!");
        }
        arguments.add(expression());
      } while(match(COMMA));
    }
    Token paren = consume(RIGHT_PAREN, "EXPECT ')' AFTER ARGUMENTS!");

    return new Expr.Call(callee, paren, arguments);
  }

  private Expr primary() {
    if (match(FALSE)) {
      return new Expr.Literal(false);
    } else if (match(TRUE)) {
      return new Expr.Literal(true);
    } else if (match(NIL)) {
      return new Expr.Literal(null);
    } else if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    } else if (match(SUPER)) {
      Token keyword = previous();
      consume(DOT, "EXPECT '.' AFTER 'SUPER'!");
      Token method = consume(IDENTIFIER, "EXPECT SUPERCLASS METHOD NAME!");
      return new Expr.Super(keyword, method);
    } else if (match(THIS)) {
      return new Expr.This(previous());
    } else if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    } else if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "EXPECT ')' AFTER EXPRESSION!");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "EXPECT EXPRESSION!");
  }

  /**
   * See if the current token is any of the given {@code types}. Will consume a token as a byproduct.
   * @param types TokenTypes to check against the current token.
   */
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if(check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private Token consume(TokenType type, String message) {
    if(check(type)) {
      return advance();
    }
    throw error(peek(), message.toUpperCase());
  }

  /**
   * Determines if a provided type matches the current token or not.  only looks at a token, does not consume it.
   * @param type The type to matched against the current token.
   * @return true if the type matches the current token, false otherwise.
   */
  private boolean check(TokenType type) {
    if (isAtEnd()) {
      return false;
    }
    return peek().type == type;
  }

  /**
   * Consumes the current token and returns it.
   * @return the token that is consumed as a result of incrementing the current position.
   */
  private Token advance() {
    if (!isAtEnd()) {
      current++;
    }
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }
  
  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while(!isAtEnd()) {
      if(previous().type == SEMICOLON) {
        return;
      }

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}
