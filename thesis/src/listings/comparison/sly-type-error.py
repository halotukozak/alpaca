class Calculator(Parser):
    @_('NUM')
    def expr(self, p):
        return p.NUM

    @_('expr "+" "+"')
    def expr(self, p):
        # TypeError: can only concatenate str (not "int") to str
        # We have never casted NUM to int, so p.expr0 is still a str
        return p.expr0 + 1
