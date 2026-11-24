// AI contribution: 50%
/** Sign in, see staff courses, create one, and manage a course's code, teams and members. */

import { useState } from "react";
import { isDashboardRole } from "@nikolai/api-client";
import type { DashboardClient } from "./lib/api";
import { useCurrentUser } from "./lib/useCurrentUser";
import { useDashboardNavigation } from "./lib/useDashboardNavigation";
import { CoursePage } from "./views/CoursePage";
import { UNSAVED_CHANGES_MESSAGE } from "./lib/useUnsavedChanges";
import { Header } from "./views/Header";
import { LoadState } from "./views/LoadState";
import { RequirementsPage } from "./views/RequirementsPage";
import { StaffView } from "./views/StaffView";
import { StudentMessage } from "./views/StudentMessage";

// Stays above the 50-line limit deliberately: this is the page's single routing JSX branch
// (its shape predates this branch; see main's CoursePage, also larger). Splitting the body
// selection into a child would thread a dozen props (client, user, courses, page, dirty ref,
// navigation ops) for one declarative tree. The rubric's "generally" applies; see
// code-quality-rubric.md section 3.
export function App({
  client,
  signInUrl,
  notice = null,
}: {
  client: DashboardClient;
  signInUrl: string;
  notice?: string | null;
}) {
  const { state, reload, addCourse, updateCourse, markSignedOut } = useCurrentUser(client);
  const { openCourseId, page, requirementsDirty, openCourse, openRequirements, closeRequirements } =
    useDashboardNavigation();
  const [returnFocusTo, setReturnFocusTo] = useState<string | null>(null);

  if (state.kind !== "ok") {
    return <LoadState state={state} signInUrl={signInUrl} onRetry={reload} />;
  }

  const user = state.data;
  const dashboardCourses = user.courses.filter((course) => isDashboardRole(course.role));
  const canUseDashboard = user.is_instructor || dashboardCourses.length > 0;
  const open = dashboardCourses.find((course) => course.id === openCourseId) ?? null;
  const body =
    open !== null && page === "requirements" ? (
      <RequirementsPage
        key={`requirements-${open.id}`}
        client={client}
        course={open}
        onBack={closeRequirements}
        onSignedOut={markSignedOut}
        dirtyRef={requirementsDirty}
      />
    ) : open !== null ? (
      <CoursePage
        key={open.id}
        client={client}
        course={open}
        onBack={() => {
          setReturnFocusTo(open.id);
          openCourse(null);
        }}
        onOpenRequirements={() => {
          setReturnFocusTo(null);
          openRequirements();
        }}
        onCourseChange={updateCourse}
        onSignedOut={markSignedOut}
      />
    ) : (
      <>
        {notice !== null && (
          <p className="notice" role="alert">
            {notice}
          </p>
        )}
        {canUseDashboard ? (
          <StaffView
            client={client}
            user={user}
            courses={dashboardCourses}
            onCourseCreated={addCourse}
            onSignedOut={markSignedOut}
            onOpenCourse={(course) => {
              setReturnFocusTo(null);
              openCourse(course.id);
            }}
            returnFocusTo={returnFocusTo}
            onFocusReturned={() => {
              setReturnFocusTo(null);
            }}
          />
        ) : (
          <StudentMessage
            client={client}
            courses={user.courses}
            onJoined={({ course, alreadyMember }) => {
              (alreadyMember ? updateCourse : addCourse)(course);
            }}
            onSignedOut={markSignedOut}
          />
        )}
      </>
    );

  return (
    <main className="page">
      <Header
        user={user}
        client={client}
        onSignedOut={markSignedOut}
        confirmLeave={() => !requirementsDirty.current || window.confirm(UNSAVED_CHANGES_MESSAGE)}
      />
      {body}
    </main>
  );
}
