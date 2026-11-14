extern int counter;
static const unsigned long long int big = 1ULL << 40;
volatile short flags;
register char c;
_Thread_local int tls;
_Atomic int ai;
_Atomic(long) al;
_Alignas(16) char buffer[64];
_Alignas(double) int aligned;
long double ld;
signed char sc;
_Bool flag;
double _Complex z;

int (*compare)(const void *, const void *);
int *(*table[10])(int, char **);
void (*signal(int sig, void (*handler)(int)))(int);
char *argv[];
int matrix[3][4];
const char *const names[] = { "a", "b", "c", };

enum Colour { RED, GREEN = 5, BLUE, };
enum Colour colour = BLUE;

union Value {
    int i;
    float f;
    struct { unsigned int low : 16, high : 16; };
    unsigned : 0;
};

struct Point { int x, y; } origin = { .x = 0, .y = 0 };
struct Point points[3] = { [0] = { 1, 2 }, [2].x = 5 };

typedef unsigned int uint;
typedef int (*Callback)(void *data, uint size);
uint count;
Callback callbacks[4];

_Static_assert(sizeof(int) >= 2, "int is too small");

inline int square(int x) { return x * x; }
_Noreturn void fail(const char *message, ...);
void process(int n, int values[static n], int restrict_copy[const restrict]);
int old_style(a, b) int a; char *b; { return a; }
