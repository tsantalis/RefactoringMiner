#include <stdio.h>

struct Student {
    int id;
    double gpa;
};

void print_student(struct Student student) {
    printf("Student ID: %d GPA: %.2f\n", student.id, student.gpa);
}

int main() {
    struct Student student = {456, 3.8};
    print_student(student);
    return 0;
}