// AI contribution: 50%
/** The NikolAI view for the GitHub page the user is on. */

import type { GitHubPage } from "../shared/page";
import type { Clients } from "../shared/settings";
import { CiView } from "./other/CiView";
import { PlanningView } from "./other/PlanningView";
import { StoryCard } from "./other/StoryCard";
import { PullView } from "./pull/PullView";
import { RepoView } from "./repo/RepoView";
import { SignInUrlContext } from "./StateMessage";

export function App({
  page,
  section,
  clients,
}: {
  page: GitHubPage;
  section: string | null;
  clients: Clients;
}) {
  return (
    <SignInUrlContext.Provider value={clients.signInUrl}>
      <PageView page={page} section={section} clients={clients} />
    </SignInUrlContext.Provider>
  );
}

function PageView({
  page,
  section,
  clients,
}: {
  page: GitHubPage;
  section: string | null;
  clients: Clients;
}) {
  switch (page.kind) {
    case "repo":
      return <RepoView repo={page} section={section} client={clients.nikolai} />;
    case "pull":
      return <PullView pull={page} section={section} clients={clients} />;
    case "issue":
      return <StoryCard issue={page} client={clients.nikolai} />;
    case "project":
      return <PlanningView project={page} client={clients.nikolai} />;
    case "actions":
      return <CiView repo={page} client={clients.nikolai} />;
  }
}
