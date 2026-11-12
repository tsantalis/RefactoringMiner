// AI contribution: 50%
/** The courses the signed-in staff member can manage, with their role and team count. */

import { useEffect, useRef } from "react";
import { ROLE_LABELS, type CourseSummary } from "@nikolai/api-client";
import { Card } from "@nikolai/ui";

export function CourseList({
  courses,
  onOpen,
  returnFocusTo,
  onFocusReturned,
}: {
  courses: CourseSummary[];
  onOpen: (course: CourseSummary) => void;
  returnFocusTo: string | null;
  onFocusReturned: () => void;
}) {
  const buttonRefs = useRef<Map<string, HTMLButtonElement>>(new Map());

  useEffect(() => {
    if (returnFocusTo === null) return;
    const target = buttonRefs.current.get(returnFocusTo);
    target?.focus();
    onFocusReturned();
  }, [returnFocusTo, onFocusReturned]);

  return (
    <Card title={<h2>Your courses</h2>}>
      {courses.length === 0 ? (
        <p className="muted card-body">You don't have any courses yet.</p>
      ) : (
        <ul className="rows">
          {courses.map((course) => {
            const action = course.role === "instructor" ? "Manage" : "Open";
            return (
              <li className="item" key={course.id}>
                <div>
                  <h3>{course.name}</h3>
                  <p className="muted">
                    {ROLE_LABELS[course.role]} · {course.teams.length}{" "}
                    {course.teams.length === 1 ? "team" : "teams"}
                  </p>
                </div>
                <button
                  className="button small"
                  aria-label={`${action} ${course.name}`}
                  ref={(element) => {
                    if (element) buttonRefs.current.set(course.id, element);
                    else buttonRefs.current.delete(course.id);
                  }}
                  onClick={() => {
                    onOpen(course);
                  }}
                >
                  {action}
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </Card>
  );
}
