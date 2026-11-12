// AI contribution: 50%
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// The dashboard is one static page. The built files are served by nginx in production.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  build: { outDir: "dist" },
  test: {
    environment: "jsdom",
    setupFiles: ["src/test/setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
