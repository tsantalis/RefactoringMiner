#include <iostream>
#include <string>
#include <vector>

namespace school {

enum class GradeLevel {
    Freshman,
    Sophomore,
    Junior,
    Senior
};

struct ContactInfo {
    std::string email;
    std::string phone;
};

class Person {
protected:
    std::string name;

public:
    explicit Person(const std::string& personName)
        : name(personName) {}

    virtual ~Person() = default;

    std::string getName() const {
        return name;
    }

    virtual void printInfo() const = 0;
};

class Student : public Person {
private:
    static int nextId;
    int studentId;
    double gpa;
    GradeLevel level;
    ContactInfo contact;

public:
    Student(const std::string& studentName, double studentGpa, GradeLevel gradeLevel);

    int getStudentId() const {
        return studentId;
    }

    double getGpa() const {
        return gpa;
    }

    void setGpa(double newGpa) {
        gpa = newGpa;
    }

    void addCourse(const std::string& courseName);
    void addCourse(const std::string& courseName, int credits);

    void printInfo() const override {
        std::cout << "ID: " << studentId
                  << ", Name: " << name
                  << ", GPA: " << gpa << std::endl;
    }
};

int Student::nextId = 1000;

Student::Student(const std::string& studentName, double studentGpa, GradeLevel gradeLevel)
    : Person(studentName), studentId(nextId++), gpa(studentGpa), level(gradeLevel) {}

void Student::addCourse(const std::string& courseName) {
    std::cout << "Adding course: " << courseName << std::endl;
}

void Student::addCourse(const std::string& courseName, int credits) {
    std::cout << "Adding course: " << courseName << " (" << credits << " credits)" << std::endl;
}

static double averageGpa(const std::vector<Student>& students) {
    double total = 0.0;
    for(const Student& student : students) {
        total += student.getGpa();
    }
    return students.empty() ? 0.0 : total / students.size();
}

}

int main() {
    school::Student student("Antonino", 3.5, school::GradeLevel::Junior);
    std::vector<school::Student> students;
    students.push_back(student);

    student.printInfo();
    student.addCourse("Algorithms", 3);
    student.setGpa(3.7);

    std::cout << "Average GPA: " << school::averageGpa(students) << std::endl;
    return 0;
}
