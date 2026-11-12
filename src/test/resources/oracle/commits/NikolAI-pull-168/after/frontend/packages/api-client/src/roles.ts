// AI contribution: 50%
/** The course roles the API sends, defined once for every NikolAI front end. */

import { literal, type Infer } from "./checks";

export const courseRole = literal("student", "ta", "instructor");
export type CourseRole = Infer<typeof courseRole>;

/** The display name of each role, shared by every list that names one. */
export const ROLE_LABELS: Record<CourseRole, string> = {
  instructor: "Instructor",
  ta: "TA",
  student: "Student",
};

/** TAs and instructors see the staff layout of every NikolAI view. */
export function isStaffRole(role: CourseRole): boolean {
  return role === "ta" || role === "instructor";
}
