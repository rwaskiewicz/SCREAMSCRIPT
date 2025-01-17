package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.craftinginterpreters.lox.TokenType.*;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private static final Map<String, TokenType> keywords;

  // start is the position of the first character in the lexeme being scanned
  private int start = 0;
  // current is the position of the character that we're currently considering
  private int current = 0;
  // tracks what source line `current` is on so we can produce tokens that know their location
  private int line = 1;

  static {
    keywords = new HashMap<>();
    keywords.put("AND", AND);
    keywords.put("CLASS", CLASS);
    keywords.put("ELSE", ELSE);
    keywords.put("FALSE", FALSE);
    keywords.put("FOR", FOR);
    keywords.put("FUN", FUN);
    keywords.put("IF", IF);
    keywords.put("NIL", NIL);
    keywords.put("OR", OR);
    keywords.put("PRINT", PRINT);
    keywords.put("RETURN", RETURN);
    keywords.put("SUPER", SUPER);
    keywords.put("THIS", THIS);
    keywords.put("TRUE", TRUE);
    keywords.put("VAR", VAR);
    keywords.put("WHILE", WHILE);
  }

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while(!isAtEnd()) {
      // we are at the beginning of the next lexeme
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(':
        addToken(LEFT_PAREN);
        break;
      case ')':
        addToken(RIGHT_PAREN);
        break;
      case '{':
        addToken(LEFT_BRACE);
        break;
      case '}':
        addToken(RIGHT_BRACE);
        break;
      case ',':
        addToken(COMMA);
        break;
      case '.':
        addToken(DOT);
        break;
      case '-':
        addToken(MINUS);
        break;
      case '+':
        addToken(PLUS);
        break;
      case ';':
        addToken(SEMICOLON);
        break;
      case '*':
        addToken(STAR);
        break;
      // cases where we need to determine if the character is a single character operator or not
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;
      case '/':
        if (match('/')) {
          // a comment goes until the end of the line, but we don't care to add it as a token
          while (peek() != '\n' && !isAtEnd()) {
            advance();
          }
        } else if (match('*')) {
          // skip past the immediate STAR we're currently on
          advance();

          // move to the next STAR
          peek();
          while (peek() != '*' && !isAtEnd()) {
            advance();
          }

          if (!isAtEnd()) {
            // advance over -that- STAR
            advance();

            if (match('/')) {
              return;
            }
          }
        } else {
          addToken(SLASH);
        }
      case ' ':
      case '\r':
      case '\t':
        // ignore whitespace
        break;
      case '\n':
        line++;
        break;
      case '"':
        string();
        break;
      default:
        if (isDigit(c)){
          number();
        } else if (isAlpha(c)) {
          identifier();
        }else {
          // keep on going, let's find as many errors as we can at this point
          Lox.error(line, "UNEXPECTED CHARACTER!");
        }
        break;
    }
  }

  private void identifier() {
    while(isAlphaNumeric(peek())) {
      advance();
    }
    // See if the identifier is a reserved word
    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null) {
      type = IDENTIFIER;
    }
    addToken(type);
  }

  private void number() {
    while(isDigit(peek())) {
      advance();
    }

    // detect if the next character is a dot or not
    if (peek() == '.' && isDigit(peekNext())) {
      advance();
    }

    while(isDigit(peek())) {
      advance();
    }

    addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private void string() {
    while(peek() != '"' && !isAtEnd()) {
      char currentChar = peek();
      // support for multiline strings, harder to keep them out then to keep them in
      if (currentChar == '\n') {
        line++;
      }

      // STRINGS MUST SCREAM
      if (currentChar >= 'a' && currentChar <= 'z') {
        Lox.error(line, "NON SCREAM STRING DETECTED! SCREAM IT OR ELSE!");
        return;
      }

      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "UNTERMINATED STRING!");
      return;
    }

    // the closing "
    advance();

    // trim the surrounding quotes
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }

  /**
   * Returns the character at the current position of the input stream with one character lookahead
   * @return the character at the current position of the input stream
   */
  private char peek() {
    if (isAtEnd()) {
      return '\0';
    }
    // one character lookahead
    // the smaller this number is (generally) the faster the scanner runs
    // the lexical grammar dictates how much lookahead that we need
    return source.charAt(current);
  }

  /**
   * Returns the character at the next position (relative to the current position) with two character lookup
   * @return the character at the next position (relative to the current position)
   */
  private char peekNext() {
    if (current + 1 >= source.length()) {
      return '\0';
    }

    // two character lookahead
    return source.charAt(current + 1);
  }

  private boolean isAlpha(char c) {
    return (c >= 'A' && c <= 'Z') || c == '_';
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  /**
   * Determines if the provided character matches the one at the current position
   * @param expected The character to test
   * @return True if there is a match, false if the character at the current position does not match or we've hit EOF
   */
  private boolean match(char expected) {
    if (isAtEnd()) {
      return false;
    }

    // also a small lookahead
    if (source.charAt(current) != expected) {
      return false;
    }

    // conditionally advance IFF we have a match here
    current++;
    return true;
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }
  /**
   * Moves the current counter forward, returns a character from the input
   * @return the character that we just passed over
   */
  private char advance() {
    current++;
    // need to advance, then return the one that we just 'passed' over, in the event of a bad character
    return source.charAt(current - 1);
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}
