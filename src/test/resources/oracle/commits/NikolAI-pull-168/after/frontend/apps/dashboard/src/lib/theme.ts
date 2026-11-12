// AI contribution: 50%
/** Keep the document's `data-theme` in step with the operating system's light/dark choice. */

interface SystemTheme {
  matches: boolean;
  addEventListener: (type: "change", listener: (event: { matches: boolean }) => void) => void;
}

type ThemedRoot = Pick<HTMLElement, "dataset">;

export function followSystemTheme(media: SystemTheme, root: ThemedRoot): void {
  const apply = (dark: boolean): void => {
    root.dataset.theme = dark ? "dark" : "light";
  };
  apply(media.matches);
  media.addEventListener("change", (event) => {
    apply(event.matches);
  });
}
