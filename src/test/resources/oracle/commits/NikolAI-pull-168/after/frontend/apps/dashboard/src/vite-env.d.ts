// AI contribution: 50%
/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** The NikolAI API origin. Defaults to production when unset. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
