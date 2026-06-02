#include <iostream>
#include <string>
#include <vector>

namespace school {

enum class GradeLevel {
    Freshman,
    Sophomore,
    Junior,
    Senior,
    Graduate
};

struct ContactInfo {
    std::string email;
    std::string phone;
    bool verified;
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

    void updateGpa(double newGpa) {
        gpa = newGpa;
    }

    void addCourse(const std::string& courseName);
    void addCourse(const std::string& courseName, int credits);

    void printInfo() const override {
        std::cout << "ID: " << studentId
                  << ", Name: " << name
                  << ", GPA: " << gpa
                  << ", Level: " << static_cast<int>(level) << std::endl;
    }
};

int Student::nextId = 2000;

Student::Student(const std::string& studentName, double studentGpa, GradeLevel gradeLevel)
    : Person(studentName), studentId(nextId++), gpa(studentGpa), level(gradeLevel) {}

void Student::addCourse(const std::string& courseName) {
    std::cout << "Enrolling in course: " << courseName << std::endl;
}

void Student::addCourse(const std::string& courseName, int credits) {
    std::cout << "Enrolling in course: " << courseName << " (" << credits << " credits)" << std::endl;
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
    school::Student student("Nikolaos", 4.3, school::GradeLevel::Graduate);
    std::vector<school::Student> students;
    students.push_back(student);

    student.printInfo();
    student.addCourse("Refactoring", 4);
    student.updateGpa(3.5);

    std::cout << "Average GPA: " << school::averageGpa(students) << std::endl;
    return 0;
}
