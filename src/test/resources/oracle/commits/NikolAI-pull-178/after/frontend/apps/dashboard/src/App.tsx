// AI contribution: 50%
/** Sign in, see staff courses, create one, and manage a course's code, teams and members. */

import { useCallback, useEffect, useState } from "react";
import { isStaffRole } from "@nikolai/api-client";
import type { DashboardClient } from "./lib/api";
import { useCurrentUser } from "./lib/useCurrentUser";
import { CoursePage } from "./views/CoursePage";
import { Header } from "./views/Header";
import { LoadState } from "./views/LoadState";
import { StaffView } from "./views/StaffView";
import { StudentMessage } from "./views/StudentMessage";

function courseIdFromUrl(): string | null {
  return new URLSearchParams(window.location.search).get("course");
}

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
  const [openCourseId, setOpenCourseId] = useState<string | null>(() =>
    typeof window === "undefined" ? null : courseIdFromUrl(),
  );
  const [returnFocusTo, setReturnFocusTo] = useState<string | null>(null);

  useEffect(() => {
    const onPop = (): void => {
      setOpenCourseId(courseIdFromUrl());
    };
    window.addEventListener("popstate", onPop);
    return () => {
      window.removeEventListener("popstate", onPop);
    };
  }, []);

  const openCourse = useCallback((courseId: string | null) => {
    const url = new URL(window.location.href);
    if (courseId === null) {
      url.searchParams.delete("course");
    } else {
      url.searchParams.set("course", courseId);
    }
    window.history.pushState(null, "", url);
    setOpenCourseId(courseId);
  }, []);

  if (state.kind !== "ok") {
    return <LoadState state={state} signInUrl={signInUrl} onRetry={reload} />;
  }

  const user = state.data;
  const staffCourses = user.courses.filter((course) => isStaffRole(course.role));
  const isStaff = user.is_instructor || staffCourses.length > 0;
  const open = staffCourses.find((course) => course.id === openCourseId) ?? null;
  const body =
    open !== null ? (
      <CoursePage
        key={open.id}
        client={client}
        course={open}
        onBack={() => {
          setReturnFocusTo(open.id);
          openCourse(null);
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
        {isStaff ? (
          <StaffView
            client={client}
            user={user}
            courses={staffCourses}
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
          <StudentMessage />
        )}
      </>
    );

  return (
    <main className="page">
      <Header user={user} client={client} onSignedOut={markSignedOut} />
      {body}
    </main>
  );
}
