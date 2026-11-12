// AI contribution: 50%
/** The staff page: create a course (instructors only) and list the courses they can manage. */

import type { CourseSummary, CurrentUser } from "@nikolai/api-client";
import type { DashboardClient } from "../lib/api";
import { CourseList } from "./CourseList";
import { CreateCourseForm } from "./CreateCourseForm";

export function StaffView({
  client,
  user,
  courses,
  onCourseCreated,
  onSignedOut,
}: {
  client: DashboardClient;
  user: CurrentUser;
  courses: CourseSummary[];
  onCourseCreated: (course: CourseSummary) => void;
  onSignedOut: () => void;
}) {
  return (
    <>
      {user.is_instructor && (
        <CreateCourseForm client={client} onCreated={onCourseCreated} onSignedOut={onSignedOut} />
      )}
      <CourseList courses={courses} />
    </>
  );
}
