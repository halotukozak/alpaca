# Example: COMPILATION THEORY PROJECT

---

The project is a compiler for a simple language, created during the compilation theory course. The language is a
statically typed language with a syntax similar to C, which supports basic arithmetic operations, control flow
statements, and variable definitions. The compiler was originally implemented in
Python ([SLY](https://sly.readthedocs.io/en/latest/sly.html)) and consists of several phases:
scanning, parsing, type checking, scoping, and interpreting. The Parser generates an abstract syntax tree (AST), and
Interpreter uses it to interpret the program. The TypeChecker also performs type checking and scoping to ensure the
correctness of the program. The project includes several example programs to demonstrate the project's functionality.
