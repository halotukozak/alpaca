for n = 2:100 {
    p = 1;
    for d = 2:n-1 {
        nc = n;
        while (nc > 0) nc -= d;
        if (nc == 0) {
            p = 0;
            break;
        }
    }
    if (p == 1) {
        print n;
    }
}