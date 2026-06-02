#include <stdio.h>

struct Student {
    int id;
    double gpa;
};

void print_student(struct Student student) {
    printf("ID: %d GPA: %.2f\n", student.id, student.gpa);
}

int main() {
    struct Student student = {123, 3.5};
    print_student(student);
    return 0;
}