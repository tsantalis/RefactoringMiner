"""Write the API's OpenAPI schema without starting the server (US-11, ADR 0001 decision 11).

The frontend generates ``@nikolai/api-client`` from it (``pnpm generate:api``), and CI fails
when the committed copy is stale. Importing the app reads the CORS allow-list from settings, but
no secrets, so this runs anywhere the backend's dependencies are installed::

    uv run python -m app.openapi_schema ../frontend/packages/api-client/openapi.json
    uv run python -m app.openapi_schema            # print it instead
"""

# AI contribution: 50%

import argparse
import json
import sys
from collections.abc import Sequence
from pathlib import Path

from app.main import app


def openapi_json() -> str:
    """The schema as stable JSON: sorted keys, so regenerating an unchanged API is a no-op."""
    return json.dumps(app.openapi(), indent=2, sort_keys=True, ensure_ascii=False) + "\n"


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Write the backend's OpenAPI schema as JSON.")
    parser.add_argument("output", nargs="?", type=Path, help="file to write (default: stdout)")
    args = parser.parse_args(argv)
    schema = openapi_json()
    if args.output is None:
        # Bytes, not text: a Windows console's code page can't encode every docstring character.
        sys.stdout.buffer.write(schema.encode("utf-8"))
        sys.stdout.flush()
        return 0
    try:
        # "\n" on every OS, so regenerating on Windows doesn't show the file as modified.
        args.output.write_text(schema, encoding="utf-8", newline="\n")
    except OSError as error:
        print(f"Can't write the OpenAPI schema to {args.output}: {error.strerror}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
