a = 0;
b = 1;
while (b < 1000) {
    print b;
    b += a;
    a = b - a;
}