class MathLexer(Lexer):
    literals = {'+', '-', '*', '/', '(', ')'}
    tokens = {NUM}
    ignore = ' \t\n'
    NUM = r'\d+'

class MathParser(Parser):
    tokens = MathLexer.tokens
    precedence = (
        ('left', '+', '-'),
        ('left', '*', '/'),
    )

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
