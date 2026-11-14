int f(int a, int b, int *p, int arr[], struct S { int x; } s, struct S *ps) {
    int r = a + b * 2 - (a - b) / 3 % 4;
    r = a << 2 | b >> 1 & 0xFF ^ ~a;
    r += a > b ? a : b;
    r = a == b && a != 0 || !b;
    r = a < b, r = a <= b, r = a >= b;
    r = -a + +b - *p + arr[2] + s.x + ps->x;
    r = ++a + b++ - --a - b--;
    r = (int) 3.5e2 + (unsigned long) 'c' + sizeof(int) + sizeof r + _Alignof(double);
    r <<= 1; r >>= 2; r &= 3; r |= 4; r ^= 5; r *= 6; r /= 7; r %= 8; r -= 9;
    p = &arr[a];
    r = 10u + 10UL + 10ll + 0777 + 0x1Fu + 1.0f + .5 + 2. + 0x1.8p3 + L'x' + '\n' + '\0';
    char *str = "multi" "part" u8"string" "\t\"escaped\"";
    r = (int[]){1, 2, 3}[a];
    r = _Generic(a, int: 1, double: 2, default: 0);
    return r;
}
