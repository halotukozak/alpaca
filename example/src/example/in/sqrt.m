for x = 1:9 {
    sqrt_x = 1.0;
    for i = 1:10000 sqrt_x = (sqrt_x + x / sqrt_x) / 2;
    print x, sqrt_x;
}