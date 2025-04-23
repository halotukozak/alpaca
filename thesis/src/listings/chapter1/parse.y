{
/*%%%*/
value_expr($3);
$1->nd_value = $3;
$$ = $1;
/*%
$$ = dispatch2(massign, $1, $3);
%*/
}
| var_lhs tOP_ASGN command_call
{
value_expr($3);
$$ = new_op_assign($1, $2, $3);
}
| primary_value '[' opt_call_args rbracket tOP_ASGN command_call
{
/*%%%*/
NODE *args;

value_expr($6);
if (!$3) $3 = NEW_ZARRAY();
args = arg_concat($3, $6);
if ($5 == tOROP) {
    $5 = 0;
}
else if ($5 == tANDOP) {
    $5 = 1;
}
$$ = NEW_OP_ASGN1($1, $5, args);
fixpos($$, $1);
/*%
$$ = dispatch2(aref_field, $1, escape_Qundef($3));
$$ = dispatch3(opassign, $$, $5, $6);
%*/
}