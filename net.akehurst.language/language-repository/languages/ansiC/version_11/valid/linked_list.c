#include <stdlib.h>
#define NEW(T) ((T *) malloc(sizeof(T)))

typedef struct Node {
    int value;
    struct Node *next;
} Node;

typedef struct {
    Node *head;
    size_t length;
} List;

static Node *node_new(int value, Node *next) {
    Node *n = malloc(sizeof *n);
    if (n == NULL) return NULL;
    n->value = value;
    n->next = next;
    return n;
}

void list_push(List *list, int value) {
    list->head = node_new(value, list->head);
    list->length++;
}

int list_sum(const List *list) {
    int sum = 0;
    for (const Node *n = list->head; n != NULL; n = n->next) {
        sum += n->value;
    }
    return sum;
}

void list_free(List *list) {
    Node *n = list->head;
    while (n) {
        Node *next = n->next;
        free(n);
        n = next;
    }
    list->head = NULL;
    list->length = 0;
}
