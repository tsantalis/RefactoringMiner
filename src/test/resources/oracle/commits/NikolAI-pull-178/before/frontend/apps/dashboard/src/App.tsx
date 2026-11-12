// AI contribution: 50%
/** The dashboard's only page: sign in, see staff courses, and create a course. */

import { isStaffRole } from "@nikolai/api-client";
import type { DashboardClient } from "./lib/api";
import { useCurrentUser } from "./lib/useCurrentUser";
import { Header } from "./views/Header";
import { LoadState } from "./views/LoadState";
import { StaffView } from "./views/StaffView";
import { StudentMessage } from "./views/StudentMessage";

export function App({
  client,
  signInUrl,
  notice = null,
}: {
  client: DashboardClient;
  signInUrl: string;
  notice?: string | null;
}) {
  const { state, reload, addCourse, markSignedOut } = useCurrentUser(client);

  if (state.kind !== "ok") {
    return <LoadState state={state} signInUrl={signInUrl} onRetry={reload} />;
  }

  const user = state.data;
  const staffCourses = user.courses.filter((course) => isStaffRole(course.role));
  const isStaff = user.is_instructor || staffCourses.length > 0;
  return (
    <main className="page">
      <Header user={user} client={client} onSignedOut={markSignedOut} />
      {notice !== null && (
        <p className="notice" role="alert">
          {notice}
        </p>
      )}
      {isStaff ? (
        <StaffView
          client={client}
          user={user}
          courses={staffCourses}
          onCourseCreated={addCourse}
          onSignedOut={markSignedOut}
        />
      ) : (
        <StudentMessage />
      )}
    </main>
  );
}
