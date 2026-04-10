from pathlib import Path
from sly import Lexer, Parser

class MathLexer(Lexer):
    literals = ['+', '-', '*', '/', '(', ')']
    tokens = {NUM}
    ignore = ' \t\n'

    NUM = r'\d+'
    
    def error(self, t):
        print(f"Illegal character '{t.value[0]}'")
        self.index += 1


class MathParser(Parser):
    tokens = MathLexer.tokens

    precedence = (
        ('left', "+", "-"),
        ('left', "*", "/"),
    )

    @_('expr')
    def program(self, p):
        return p.expr

    @_('expr "*" expr')
    def expr(self, p):
        return p.expr0 * p.expr1

    @_('expr "/" expr')
    def expr(self, p):
        return p.expr0 / p.expr1

    @_('expr "+" expr')
    def expr(self, p):
        return p.expr0 + p.expr1
    
    @_('expr "-" expr')
    def expr(self, p):
        return p.expr0 - p.expr1
    
    @_('"(" expr ")"')
    def expr(self, p):
        return p.expr

    @_('NUM')
    def expr(self, p):
        return int(p.NUM)

    def error(self, p):
        if p:
            print(f"Syntax error at token {p.type} ('{p.value}')")
        else:
            print("Syntax error at EOF")


if __name__ == "__main__":
    # Test the parser
    lexer = MathLexer()
    parser = MathParser()

    file_path = Path(__file__).parent.parent / "inputs" / "iterative_math_3.txt"
    with file_path.open('r') as f:
        file_content = f.read()
    try:
        tokens = lexer.tokenize(file_content)
        result = parser.parse(tokens)
        print(f"Result Iterative: {result}")
    except Exception as e:
        print(f"Error Iterative: {e}")


    file_path = Path(__file__).parent.parent / "inputs" / "recursive_math_3.txt"
    with file_path.open('r') as f:
        file_content = f.read()
    try:
        tokens = lexer.tokenize(file_content)
        result = parser.parse(tokens)
        print(f"Result Recursive: {result}")
    except Exception as e:
        print(f"Error Recursive: {e}")
