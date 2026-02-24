"""
Synthetic big-grammar lexer and parser in SLY.

Designed as a stress test for grammar complexity with 30+ token types
and 50+ parser rules. Matches the structure of the Alpaca/Fastparse
BigGrammar implementations so all three libraries parse the same inputs.

Input format: whitespace-separated keywords from {tok0..tok29, integers}.
"""

from pathlib import Path
from sly import Lexer, Parser


class BigGrammarLexer(Lexer):
    tokens = {
        TOK0, TOK1, TOK2, TOK3, TOK4, TOK5, TOK6, TOK7, TOK8, TOK9,
        TOK10, TOK11, TOK12, TOK13, TOK14, TOK15, TOK16, TOK17, TOK18, TOK19,
        TOK20, TOK21, TOK22, TOK23, TOK24, TOK25, TOK26, TOK27, TOK28, TOK29,
        NUM,
    }
    ignore = ' \t\n'

    # Keywords: tok0 through tok29 (must be defined before NUM to take priority)
    TOK0  = r'\btok0\b'
    TOK1  = r'\btok1\b'
    TOK2  = r'\btok2\b'
    TOK3  = r'\btok3\b'
    TOK4  = r'\btok4\b'
    TOK5  = r'\btok5\b'
    TOK6  = r'\btok6\b'
    TOK7  = r'\btok7\b'
    TOK8  = r'\btok8\b'
    TOK9  = r'\btok9\b'
    TOK10 = r'\btok10\b'
    TOK11 = r'\btok11\b'
    TOK12 = r'\btok12\b'
    TOK13 = r'\btok13\b'
    TOK14 = r'\btok14\b'
    TOK15 = r'\btok15\b'
    TOK16 = r'\btok16\b'
    TOK17 = r'\btok17\b'
    TOK18 = r'\btok18\b'
    TOK19 = r'\btok19\b'
    TOK20 = r'\btok20\b'
    TOK21 = r'\btok21\b'
    TOK22 = r'\btok22\b'
    TOK23 = r'\btok23\b'
    TOK24 = r'\btok24\b'
    TOK25 = r'\btok25\b'
    TOK26 = r'\btok26\b'
    TOK27 = r'\btok27\b'
    TOK28 = r'\btok28\b'
    TOK29 = r'\btok29\b'
    NUM   = r'\d+'

    def error(self, t):
        self.index += 1


class BigGrammarParser(Parser):
    tokens = BigGrammarLexer.tokens

    # ---------------------------------------------------------------
    # Root rule: a program is a sequence of statements
    # ---------------------------------------------------------------
    @_('statements')
    def program(self, p):
        return p.statements

    @_('statement')
    def statements(self, p):
        return [p.statement]

    @_('statements statement')
    def statements(self, p):
        return p.statements + [p.statement]

    # ---------------------------------------------------------------
    # Statements: each is a different combination of tokens
    # 50+ rules providing alternatives for a statement
    # ---------------------------------------------------------------

    # --- Pair rules (2-token sequences): rules 0-14 ---
    @_('TOK0 TOK1')
    def statement(self, p):
        return ('pair01', p.TOK0, p.TOK1)

    @_('TOK2 TOK3')
    def statement(self, p):
        return ('pair23', p.TOK2, p.TOK3)

    @_('TOK4 TOK5')
    def statement(self, p):
        return ('pair45', p.TOK4, p.TOK5)

    @_('TOK6 TOK7')
    def statement(self, p):
        return ('pair67', p.TOK6, p.TOK7)

    @_('TOK8 TOK9')
    def statement(self, p):
        return ('pair89', p.TOK8, p.TOK9)

    @_('TOK10 TOK11')
    def statement(self, p):
        return ('pair1011', p.TOK10, p.TOK11)

    @_('TOK12 TOK13')
    def statement(self, p):
        return ('pair1213', p.TOK12, p.TOK13)

    @_('TOK14 TOK15')
    def statement(self, p):
        return ('pair1415', p.TOK14, p.TOK15)

    @_('TOK16 TOK17')
    def statement(self, p):
        return ('pair1617', p.TOK16, p.TOK17)

    @_('TOK18 TOK19')
    def statement(self, p):
        return ('pair1819', p.TOK18, p.TOK19)

    @_('TOK20 TOK21')
    def statement(self, p):
        return ('pair2021', p.TOK20, p.TOK21)

    @_('TOK22 TOK23')
    def statement(self, p):
        return ('pair2223', p.TOK22, p.TOK23)

    @_('TOK24 TOK25')
    def statement(self, p):
        return ('pair2425', p.TOK24, p.TOK25)

    @_('TOK26 TOK27')
    def statement(self, p):
        return ('pair2627', p.TOK26, p.TOK27)

    @_('TOK28 TOK29')
    def statement(self, p):
        return ('pair2829', p.TOK28, p.TOK29)

    # --- Triple rules (3-token sequences): rules 15-24 ---
    @_('TOK0 TOK1 TOK2')
    def statement(self, p):
        return ('triple012', p.TOK0, p.TOK1, p.TOK2)

    @_('TOK3 TOK4 TOK5')
    def statement(self, p):
        return ('triple345', p.TOK3, p.TOK4, p.TOK5)

    @_('TOK6 TOK7 TOK8')
    def statement(self, p):
        return ('triple678', p.TOK6, p.TOK7, p.TOK8)

    @_('TOK9 TOK10 TOK11')
    def statement(self, p):
        return ('triple91011', p.TOK9, p.TOK10, p.TOK11)

    @_('TOK12 TOK13 TOK14')
    def statement(self, p):
        return ('triple121314', p.TOK12, p.TOK13, p.TOK14)

    @_('TOK15 TOK16 TOK17')
    def statement(self, p):
        return ('triple151617', p.TOK15, p.TOK16, p.TOK17)

    @_('TOK18 TOK19 TOK20')
    def statement(self, p):
        return ('triple181920', p.TOK18, p.TOK19, p.TOK20)

    @_('TOK21 TOK22 TOK23')
    def statement(self, p):
        return ('triple212223', p.TOK21, p.TOK22, p.TOK23)

    @_('TOK24 TOK25 TOK26')
    def statement(self, p):
        return ('triple242526', p.TOK24, p.TOK25, p.TOK26)

    @_('TOK27 TOK28 TOK29')
    def statement(self, p):
        return ('triple272829', p.TOK27, p.TOK28, p.TOK29)

    # --- Quad rules (4-token sequences): rules 25-34 ---
    @_('TOK0 TOK1 TOK2 TOK3')
    def statement(self, p):
        return ('quad0123', p.TOK0, p.TOK1, p.TOK2, p.TOK3)

    @_('TOK4 TOK5 TOK6 TOK7')
    def statement(self, p):
        return ('quad4567', p.TOK4, p.TOK5, p.TOK6, p.TOK7)

    @_('TOK8 TOK9 TOK10 TOK11')
    def statement(self, p):
        return ('quad891011', p.TOK8, p.TOK9, p.TOK10, p.TOK11)

    @_('TOK12 TOK13 TOK14 TOK15')
    def statement(self, p):
        return ('quad12131415', p.TOK12, p.TOK13, p.TOK14, p.TOK15)

    @_('TOK16 TOK17 TOK18 TOK19')
    def statement(self, p):
        return ('quad16171819', p.TOK16, p.TOK17, p.TOK18, p.TOK19)

    @_('TOK20 TOK21 TOK22 TOK23')
    def statement(self, p):
        return ('quad20212223', p.TOK20, p.TOK21, p.TOK22, p.TOK23)

    @_('TOK24 TOK25 TOK26 TOK27')
    def statement(self, p):
        return ('quad24252627', p.TOK24, p.TOK25, p.TOK26, p.TOK27)

    @_('TOK0 TOK5 TOK10 TOK15')
    def statement(self, p):
        return ('quad_skip0', p.TOK0, p.TOK5, p.TOK10, p.TOK15)

    @_('TOK1 TOK6 TOK11 TOK16')
    def statement(self, p):
        return ('quad_skip1', p.TOK1, p.TOK6, p.TOK11, p.TOK16)

    @_('TOK2 TOK7 TOK12 TOK17')
    def statement(self, p):
        return ('quad_skip2', p.TOK2, p.TOK7, p.TOK12, p.TOK17)

    # --- Numeric rules: rules 35-39 ---
    @_('NUM')
    def statement(self, p):
        return ('num', int(p.NUM))

    @_('NUM NUM')
    def statement(self, p):
        return ('num_pair', int(p.NUM0), int(p.NUM1))

    @_('TOK0 NUM')
    def statement(self, p):
        return ('tok0_num', p.TOK0, int(p.NUM))

    @_('TOK10 NUM')
    def statement(self, p):
        return ('tok10_num', p.TOK10, int(p.NUM))

    @_('TOK20 NUM')
    def statement(self, p):
        return ('tok20_num', p.TOK20, int(p.NUM))

    # --- Five-token rules: rules 40-49 ---
    @_('TOK0 TOK1 TOK2 TOK3 TOK4')
    def statement(self, p):
        return ('quint01234',)

    @_('TOK5 TOK6 TOK7 TOK8 TOK9')
    def statement(self, p):
        return ('quint56789',)

    @_('TOK10 TOK11 TOK12 TOK13 TOK14')
    def statement(self, p):
        return ('quint1011121314',)

    @_('TOK15 TOK16 TOK17 TOK18 TOK19')
    def statement(self, p):
        return ('quint1516171819',)

    @_('TOK20 TOK21 TOK22 TOK23 TOK24')
    def statement(self, p):
        return ('quint2021222324',)

    @_('TOK25 TOK26 TOK27 TOK28 TOK29')
    def statement(self, p):
        return ('quint2526272829',)

    @_('TOK0 TOK3 TOK6 TOK9 TOK12')
    def statement(self, p):
        return ('quint_step3a',)

    @_('TOK1 TOK4 TOK7 TOK10 TOK13')
    def statement(self, p):
        return ('quint_step3b',)

    @_('TOK2 TOK5 TOK8 TOK11 TOK14')
    def statement(self, p):
        return ('quint_step3c',)

    @_('TOK15 TOK18 TOK21 TOK24 TOK27')
    def statement(self, p):
        return ('quint_step3d',)

    # --- Single-token rules: rules 50-54 ---
    @_('TOK0')
    def statement(self, p):
        return ('single', p.TOK0)

    @_('TOK10')
    def statement(self, p):
        return ('single', p.TOK10)

    @_('TOK20')
    def statement(self, p):
        return ('single', p.TOK20)

    @_('TOK5')
    def statement(self, p):
        return ('single', p.TOK5)

    @_('TOK15')
    def statement(self, p):
        return ('single', p.TOK15)

    def error(self, p):
        if p:
            self.errok()
            return next(self.tokens, None)
        return None


if __name__ == "__main__":
    lexer = BigGrammarLexer()
    parser = BigGrammarParser()

    # Small test input using various rule patterns
    test_input = "tok0 tok1 tok2 tok3 42 tok10 tok11 tok20 tok21 tok5 tok6 tok7 tok8 tok9"

    print(f"Test input: {test_input}")
    try:
        tokens = lexer.tokenize(test_input)
        result = parser.parse(tokens)
        print(f"Parse result: {result}")
        print("BigGrammar parser test PASSED")
    except Exception as e:
        print(f"Error: {e}")

    # Test with a generated input file if available
    input_path = Path(__file__).parent.parent / "inputs" / "big_grammar_3.txt"
    if input_path.exists():
        print(f"\nTesting with generated input: {input_path}")
        with input_path.open('r') as f:
            content = f.read()
        try:
            tokens = lexer.tokenize(content)
            result = parser.parse(tokens)
            print(f"Parse result ({len(result)} statements): OK")
        except Exception as e:
            print(f"Error: {e}")
