#nonexisting symbol
x = 1;
z = x;
z += 1;

while (x <= 0) {
  undef_outside_scope = x;
}

while (uuu <= 0) {
  undef_outside_scope = undef;
}

M = 5;

for j = undef:M {
        print undef, j;
    }

x = undef_outside_scope; # undef