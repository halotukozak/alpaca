"""
Synthetic big-grammar lexer and parser in SLY.

Designed as a stress test for grammar complexity with 30+ token types
and 50+ parser rules. Matches the structure of the Alpaca/Fastparse
BigGrammar implementations so all three libraries parse the same inputs.

Input format: whitespace-separated keywords from {ka..kz, la..ld, integers}.
Token names are 2-char codes to match Alpaca/Fastparse which use short codes
to avoid prefix shadowing in the regex lexer.
"""

from pathlib import Path
from sly import Lexer, Parser


class BigGrammarLexer(Lexer):
    tokens = {
        KA, KB, KC, KD, KE, KF, KG, KH, KI, KJ,
        KK, KL, KM, KN, KO, KP, KQ, KR, KS, KT,
        KU, KV, KW, KX, KY, KZ, LA, LB, LC, LD,
        NUM,
    }
    ignore = ' \t\n'

    # Keywords: ka through kz, la through ld (2-char codes, no prefix conflicts)
    KA = r'\bka\b'
    KB = r'\bkb\b'
    KC = r'\bkc\b'
    KD = r'\bkd\b'
    KE = r'\bke\b'
    KF = r'\bkf\b'
    KG = r'\bkg\b'
    KH = r'\bkh\b'
    KI = r'\bki\b'
    KJ = r'\bkj\b'
    KK = r'\bkk\b'
    KL = r'\bkl\b'
    KM = r'\bkm\b'
    KN = r'\bkn\b'
    KO = r'\bko\b'
    KP = r'\bkp\b'
    KQ = r'\bkq\b'
    KR = r'\bkr\b'
    KS = r'\bks\b'
    KT = r'\bkt\b'
    KU = r'\bku\b'
    KV = r'\bkv\b'
    KW = r'\bkw\b'
    KX = r'\bkx\b'
    KY = r'\bky\b'
    KZ = r'\bkz\b'
    LA = r'\bla\b'
    LB = r'\blb\b'
    LC = r'\blc\b'
    LD = r'\bld\b'
    NUM = r'\d+'

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
    @_('KA KB')
    def statement(self, p):
        return ('pair_ab', p.KA, p.KB)

    @_('KC KD')
    def statement(self, p):
        return ('pair_cd', p.KC, p.KD)

    @_('KE KF')
    def statement(self, p):
        return ('pair_ef', p.KE, p.KF)

    @_('KG KH')
    def statement(self, p):
        return ('pair_gh', p.KG, p.KH)

    @_('KI KJ')
    def statement(self, p):
        return ('pair_ij', p.KI, p.KJ)

    @_('KK KL')
    def statement(self, p):
        return ('pair_kl', p.KK, p.KL)

    @_('KM KN')
    def statement(self, p):
        return ('pair_mn', p.KM, p.KN)

    @_('KO KP')
    def statement(self, p):
        return ('pair_op', p.KO, p.KP)

    @_('KQ KR')
    def statement(self, p):
        return ('pair_qr', p.KQ, p.KR)

    @_('KS KT')
    def statement(self, p):
        return ('pair_st', p.KS, p.KT)

    @_('KU KV')
    def statement(self, p):
        return ('pair_uv', p.KU, p.KV)

    @_('KW KX')
    def statement(self, p):
        return ('pair_wx', p.KW, p.KX)

    @_('KY KZ')
    def statement(self, p):
        return ('pair_yz', p.KY, p.KZ)

    @_('LA LB')
    def statement(self, p):
        return ('pair_la_lb', p.LA, p.LB)

    @_('LC LD')
    def statement(self, p):
        return ('pair_lc_ld', p.LC, p.LD)

    # --- Triple rules (3-token sequences): rules 15-24 ---
    @_('KA KB KC')
    def statement(self, p):
        return ('triple_abc', p.KA, p.KB, p.KC)

    @_('KD KE KF')
    def statement(self, p):
        return ('triple_def', p.KD, p.KE, p.KF)

    @_('KG KH KI')
    def statement(self, p):
        return ('triple_ghi', p.KG, p.KH, p.KI)

    @_('KJ KK KL')
    def statement(self, p):
        return ('triple_jkl', p.KJ, p.KK, p.KL)

    @_('KM KN KO')
    def statement(self, p):
        return ('triple_mno', p.KM, p.KN, p.KO)

    @_('KP KQ KR')
    def statement(self, p):
        return ('triple_pqr', p.KP, p.KQ, p.KR)

    @_('KS KT KU')
    def statement(self, p):
        return ('triple_stu', p.KS, p.KT, p.KU)

    @_('KV KW KX')
    def statement(self, p):
        return ('triple_vwx', p.KV, p.KW, p.KX)

    @_('KY KZ LA')
    def statement(self, p):
        return ('triple_yz_la', p.KY, p.KZ, p.LA)

    @_('LB LC LD')
    def statement(self, p):
        return ('triple_lb_lc_ld', p.LB, p.LC, p.LD)

    # --- Quad rules (4-token sequences): rules 25-34 ---
    @_('KA KB KC KD')
    def statement(self, p):
        return ('quad_abcd', p.KA, p.KB, p.KC, p.KD)

    @_('KE KF KG KH')
    def statement(self, p):
        return ('quad_efgh', p.KE, p.KF, p.KG, p.KH)

    @_('KI KJ KK KL')
    def statement(self, p):
        return ('quad_ijkl', p.KI, p.KJ, p.KK, p.KL)

    @_('KM KN KO KP')
    def statement(self, p):
        return ('quad_mnop', p.KM, p.KN, p.KO, p.KP)

    @_('KQ KR KS KT')
    def statement(self, p):
        return ('quad_qrst', p.KQ, p.KR, p.KS, p.KT)

    @_('KU KV KW KX')
    def statement(self, p):
        return ('quad_uvwx', p.KU, p.KV, p.KW, p.KX)

    @_('KY KZ LA LB')
    def statement(self, p):
        return ('quad_yz_la_lb', p.KY, p.KZ, p.LA, p.LB)

    @_('KA KF KK KP')
    def statement(self, p):
        return ('quad_skip0', p.KA, p.KF, p.KK, p.KP)

    @_('KB KG KL KQ')
    def statement(self, p):
        return ('quad_skip1', p.KB, p.KG, p.KL, p.KQ)

    @_('KC KH KM KR')
    def statement(self, p):
        return ('quad_skip2', p.KC, p.KH, p.KM, p.KR)

    # --- Numeric rules: rules 35-39 ---
    @_('NUM')
    def statement(self, p):
        return ('num', int(p.NUM))

    @_('NUM NUM')
    def statement(self, p):
        return ('num_pair', int(p.NUM0), int(p.NUM1))

    @_('KA NUM')
    def statement(self, p):
        return ('ka_num', p.KA, int(p.NUM))

    @_('KK NUM')
    def statement(self, p):
        return ('kk_num', p.KK, int(p.NUM))

    @_('KU NUM')
    def statement(self, p):
        return ('ku_num', p.KU, int(p.NUM))

    # --- Five-token rules: rules 40-49 ---
    @_('KA KB KC KD KE')
    def statement(self, p):
        return ('quint_abcde',)

    @_('KF KG KH KI KJ')
    def statement(self, p):
        return ('quint_fghij',)

    @_('KK KL KM KN KO')
    def statement(self, p):
        return ('quint_klmno',)

    @_('KP KQ KR KS KT')
    def statement(self, p):
        return ('quint_pqrst',)

    @_('KU KV KW KX KY')
    def statement(self, p):
        return ('quint_uvwxy',)

    @_('KZ LA LB LC LD')
    def statement(self, p):
        return ('quint_z_la_lb_lc_ld',)

    @_('KA KD KG KJ KM')
    def statement(self, p):
        return ('quint_step3a',)

    @_('KB KE KH KK KN')
    def statement(self, p):
        return ('quint_step3b',)

    @_('KC KF KI KL KO')
    def statement(self, p):
        return ('quint_step3c',)

    @_('KP KS KV KY LB')
    def statement(self, p):
        return ('quint_step3d',)

    # --- Single-token rules: rules 50-54 ---
    @_('KA')
    def statement(self, p):
        return ('single', p.KA)

    @_('KK')
    def statement(self, p):
        return ('single', p.KK)

    @_('KU')
    def statement(self, p):
        return ('single', p.KU)

    @_('KF')
    def statement(self, p):
        return ('single', p.KF)

    @_('KP')
    def statement(self, p):
        return ('single', p.KP)

    def error(self, p):
        if p:
            self.errok()
            return next(self.tokens, None)
        return None


if __name__ == "__main__":
    lexer = BigGrammarLexer()
    parser = BigGrammarParser()

    # Small test input using various rule patterns
    test_input = "ka kb kc kd 42 kk kl ku kv kf kg kh ki kj"

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
