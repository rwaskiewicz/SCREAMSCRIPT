# SCREAMSCRIPT

_The language of your screams_

<hr>

SCREAMSCRIPT is a dynamic programming language heavily influenced by the Lox programming language.  One might even say I
took my implementation from the [Tree-Walk Interpreter](http://craftinginterpreters.com/a-tree-walk-interpreter.html) 
component of [Crafting Interpreters](http://craftinginterpreters.com), then just edited the source so that keywords,
identifiers, etc. could only be ~CAPTIAL~ SCREAM CASED, because that's exactly what I did. I even left the class names 
in place.

## Why

Because I wanted to take a joke at work a little too far.

## Interesting Tidbits

In the process of "making" SCREAMSCRIPT there were some interesting items of note that popped up:

### Variable vs Class Identifiers

This implementation of Lox/SCREAMSCRIPT doesn't distinguish identifiers as `class` or `variable` types. It's a big ol'
`Map` that stores everything together.  As a result, the casing-convention of `CapitalCasing` class names and using
`lowercase` for variable names was really the only thing keeping them from colliding. Without further changes, it's
possible to define a class and override it accidentally (or purposefully, it's a dynamic programming language where
this is _technically_ designed to be as such, even in Lox).

```
$ ./runScreamRepl.sh 
     
BUILD SUCCESSFUL in 1s
4 actionable tasks: 4 executed

AHHHHHHH > CLASS BAGEL {}
AHHHHHHH > PRINT BAGEL;
BAGEL
AHHHHHHH > VAR B = BAGEL();
AHHHHHHH > PRINT B;
BAGEL INSTANCE
AHHHHHHH > VAR BAGEL = B;
AHHHHHHH > PRINT BAGEL;
BAGEL INSTANCE
AHHHHHHH > VAR C = BAGEL();
CAN ONLY CALL FUNCTIONS AND CLASSES!
[LINE 1]
AHHHHHHH > PRINT "OHHHHHHHHH";
OHHHHHHHHH
```

### Keywords

Keywords should be SCREAM'ed. Like everything else. That's how you know it's all important. This really only required a
few small changes in the `Scanner` and `TokenType` classes.  There were however a few cases where Java `String` literals
were used instead of a static variable, which also hand replaced.

### String Variables

The contents of `String`s need to SCREAM. The `Scanner` was updated accordingly to signal to the user they've done
wrong by not screaming. If you don't scream it, you don't believe it.

Why don't people normalize text in SCREAM rather that lowercase? The world may never know. 

### Error Messages

Error messages were also manually converted to SCREAM. I did at a `.toUpperCase` call to the method that creates the
error, but this gave a small appreciation for the places we need error handling throughout the language impl.

## Running

I feel like there shouldn't need to be a disclaimer that SCREAMSCRIPT is not a production language. But if you are
inclined to run this, here's how you'd do it.

### REPL

`runScreamRepl.sh` is a very, very simple shell script in the root of this project that runs the build step (for the 
language) and starts the REPL:
```sh
$ ./runScreamRepl.sh 

BUILD SUCCESSFUL in 1s
4 actionable tasks: 4 executed
AHHHHHHH > VAR I = "HELLO WORLD!";
AHHHHHHH > PRINT I;
HELLO WORLD!
AHHHHHHH >
```

### Scream Files

`runScreamFile.sh` is a very, very simple shell script in the root of this project that runs the build step (for the 
language) and runs a provided file. Examples live in `scr/main/resources/scream-scripts`, and still use the `.scream`
extension.

For bacon.scream:
```
CLASS BACON {
    EAT() {
        PRINT "CRUNCH CRUNCH CRUNCH!";
    }
}

BACON().EAT();
```

We can run it as follows:
```sh
$ ./runScreamFile.sh -- src/main/resources/scream-scripts/ch12-classes/bacon.scream 

BUILD SUCCESSFUL in 1s
4 actionable tasks: 4 executed
CRUNCH CRUNCH CRUNCH!
```