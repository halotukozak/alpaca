class MatrixParser(Parser):
tokens = MatrixScanner.tokens

precedence = (
    ('nonassoc', 'IFX'),
    ('nonassoc', 'ELSE'),
    ('nonassoc', 'EQUAL'),
)

@_('"{" instructions "}"')
def block(self, p: YaccProduction):
    raise NotImplementedError

@_('instruction')
def block(self, p: YaccProduction):
    raise NotImplementedError

@_('IF "(" condition ")" block %prec IFX')
def instruction(self, p: YaccProduction):
    raise NotImplementedError

@_('IF "(" condition ")" block ELSE block')
def instruction(self, p: YaccProduction):
    raise NotImplementedError

@_('expr EQUAL expr')
def condition(self, p: YaccProduction):
    args = [p.expr0, p.expr1]
    raise NotImplementedError
