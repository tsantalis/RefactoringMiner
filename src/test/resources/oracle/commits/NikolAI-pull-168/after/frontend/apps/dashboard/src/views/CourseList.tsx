// AI contribution: 50%
/** The courses the signed-in staff member can manage, with their role and team count. */

import { ROLE_LABELS, type CourseSummary } from "@nikolai/api-client";
import { Card } from "@nikolai/ui";

export function CourseList({ courses }: { courses: CourseSummary[] }) {
  return (
    <Card title={<h2>Your courses</h2>}>
      {courses.length === 0 ? (
        <p className="muted card-body">You don't have any courses yet.</p>
      ) : (
        <ul className="courses">
          {courses.map((course) => (
            <li className="item" key={course.id}>
              <div>
                <h3>{course.name}</h3>
                <p className="muted">
                  {ROLE_LABELS[course.role]} · {course.teams.length}{" "}
                  {course.teams.length === 1 ? "team" : "teams"}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
