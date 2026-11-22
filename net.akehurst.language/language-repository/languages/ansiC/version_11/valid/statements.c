int classify(int n) {
    int result = 0;
    if (n < 0)
        result = -1;
    else if (n == 0)
        result = 0;
    else
        result = 1;

    if (n > 10)
        if (n > 100) result = 3;
        else result = 2;

    switch (n) {
        case 1:
        case 2:
            result = 12;
            break;
        case 3: {
            int x = n * 2;
            result = x;
        }
        break;
        default:
            ;
    }

    for (;;) break;
    for (int i = 0, j = 10; i < j; i++, j--) continue;
    for (result = 0; result < 3; ++result) {}

    while (n > 0) n--;
    do { n++; } while (n < 5);

    goto end;
end:
    return result;
}
