from pathlib import Path
from sly import Lexer, Parser

class JsonLexer(Lexer):
    literals = ['{', '}', '[', ']', ':', ',']
    tokens = {NULL, BOOL, NUM, STRING}
    ignore = ' \t\n'

    NULL = r'null'
    BOOL = r'true|false'
    NUM = r'[-+]?\d+(\.\d+)?'
    STRING = r'\"(\\.|[^"\\])*\"'
    
    def error(self, t):
        print(f"Illegal character '{t.value[0]}'")
        self.index += 1


class JsonParser(Parser):
    tokens = JsonLexer.tokens

    @_('value')
    def program(self, p):
        return p.value
    
    @_('NULL')
    def value(self, p):
        return None
    
    @_('BOOL')
    def value(self, p):
        return p.BOOL == "true"
    
    @_('NUM')
    def value(self, p):
        return float(p.NUM)
    
    @_('STRING')
    def value(self, p):
        return p.STRING[1:-1]

    @_('object')
    def value(self, p):
        return p.object
    
    @_('array')
    def value(self, p):
        return p.array
    
    @_('"{" "}"')
    def object(self, p):
        return {}

    @_('"{" members "}"')
    def object(self, p):
        return dict(p.members)
    
    @_('member')
    def members(self, p):
        return [p.member]
    
    @_('members "," member')
    def members(self, p):
        return p.members + [p.member]
    
    @_('STRING ":" value')
    def member(self, p):
        return (p.STRING[1:-1], p.value)
    
    @_('"[" "]"')
    def array(self, p):
        return []
    
    @_('"[" elements "]"')
    def array(self, p):
        return p.elements
    
    @_('value')
    def elements(self, p):
        return [p.value]
    
    @_('elements "," value')
    def elements(self, p):
        return p.elements + [p.value]

    def error(self, p):
        if p:
            print(f"Syntax error at token {p.type} ('{p.value}')")
        else:
            print("Syntax error at EOF")


if __name__ == "__main__":
    # Test the parser
    lexer = JsonLexer()
    parser = JsonParser()

    file_path = Path(__file__).parent.parent / "inputs" / "iterative_json_3.txt"
    with file_path.open('r') as f:
        file_content = f.read()
    try:
        tokens = lexer.tokenize(file_content)
        result = parser.parse(tokens)
        print(f"Result Iterative: {result}")
    except Exception as e:
        print(f"Error Iterative: {e}")


    file_path = Path(__file__).parent.parent / "inputs" / "recursive_json_3.txt"
    with file_path.open('r') as f:
        file_content = f.read()
    try:
        tokens = lexer.tokenize(file_content)
        result = parser.parse(tokens)
        print(f"Result Recursive: {result}")
    except Exception as e:
        print(f"Error Recursive: {e}")
