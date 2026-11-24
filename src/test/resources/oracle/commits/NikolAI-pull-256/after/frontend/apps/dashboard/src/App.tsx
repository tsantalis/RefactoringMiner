// AI contribution: 50%
/**
 * Sign in, see staff courses, create one, and manage a course's code, teams and members.
 * Students land on their team's planning page; staff open any team's from the course page.
 */

import { useState } from "react";
import { isDashboardRole } from "@nikolai/api-client";
import type { DashboardClient } from "./lib/api";
import { useCurrentUser } from "./lib/useCurrentUser";
import { useDashboardNavigation } from "./lib/useDashboardNavigation";
import { isStaffUser, planningRoute } from "./lib/planningRoute";
import { useLandOnOwnTeam } from "./lib/useLandOnOwnTeam";
import { CoursePage } from "./views/CoursePage";
import { UNSAVED_CHANGES_MESSAGE } from "./lib/useUnsavedChanges";
import { Header } from "./views/Header";
import { LoadState } from "./views/LoadState";
import { PlanningPage } from "./views/PlanningPage";
import { RequirementsPage } from "./views/RequirementsPage";
import { StaffView } from "./views/StaffView";
import { StudentMessage } from "./views/StudentMessage";

// Stays above the 50-line limit deliberately: this is the page's single routing JSX branch.
// It was 96 lines on main and is 109 with the planning page (#241), whose routing decision lives
// in `planningRoute` so its branch here is one element. Splitting the body selection into a
// child would thread a dozen props (client, user, courses, page, dirty ref, navigation ops) for
// one declarative tree. The rubric's "generally" applies; see code-quality-rubric.md section 3.
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
  const { location, requirementsDirty, openCourse, openRequirements, closeRequirements, openTeam } =
    useDashboardNavigation();
  const [returnFocusTo, setReturnFocusTo] = useState<string | null>(null);
  useLandOnOwnTeam(state.kind === "ok" ? state.data : null, location, openTeam);

  if (state.kind !== "ok") {
    return <LoadState state={state} signInUrl={signInUrl} onRetry={reload} />;
  }

  const user = state.data;
  const dashboardCourses = user.courses.filter((course) => isDashboardRole(course.role));
  const canUseDashboard = isStaffUser(user);
  const open = dashboardCourses.find((course) => course.id === location.course) ?? null;
  const planning = planningRoute(user, location, open !== null, () => {
    openTeam(null);
  });
  const body =
    planning !== null ? (
      <PlanningPage
        key={`planning-${planning.teamId}`}
        client={client}
        teamId={planning.teamId}
        back={planning.back}
        onSignedOut={markSignedOut}
      />
    ) : open !== null && location.page === "requirements" ? (
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
        onOpenPlanning={openTeam}
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
