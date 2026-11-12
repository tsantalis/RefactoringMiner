"""The dashboard's origin may call the API from another site (US-18.1, #144)."""

# AI contribution: 50%

from fastapi.testclient import TestClient

from app.core.config import Settings
from app.main import create_app

DASHBOARD_ORIGIN = "https://app.nikolai.live"
EVIL_ORIGIN = "https://evil.test"


def client_for(origin: str) -> TestClient:
    """The real app, built with only the dashboard's origin allowed."""
    settings = Settings(_env_file=None, allowed_origins=[origin])
    return TestClient(create_app(settings))


def test_an_allowed_origin_can_read_the_api() -> None:
    response = client_for(DASHBOARD_ORIGIN).get("/health", headers={"Origin": DASHBOARD_ORIGIN})

    assert response.status_code == 200
    assert response.headers["access-control-allow-origin"] == DASHBOARD_ORIGIN
    assert response.headers["access-control-allow-credentials"] == "true"


def test_an_allowed_origin_passes_the_write_preflight() -> None:
    response = client_for(DASHBOARD_ORIGIN).options(
        "/api/courses",
        headers={
            "Origin": DASHBOARD_ORIGIN,
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "content-type",
        },
    )

    assert response.status_code == 200
    assert response.headers["access-control-allow-origin"] == DASHBOARD_ORIGIN
    assert response.headers["access-control-allow-credentials"] == "true"
    assert "POST" in response.headers["access-control-allow-methods"]
    assert "content-type" in response.headers["access-control-allow-headers"].lower()


def test_an_unlisted_origin_gets_no_cors_permission() -> None:
    response = client_for(DASHBOARD_ORIGIN).get("/health", headers={"Origin": EVIL_ORIGIN})

    assert "access-control-allow-origin" not in response.headers


def test_a_method_the_api_does_not_route_is_not_allowed() -> None:
    response = client_for(DASHBOARD_ORIGIN).options(
        "/api/courses",
        headers={"Origin": DASHBOARD_ORIGIN, "Access-Control-Request-Method": "DELETE"},
    )

    assert response.status_code == 400
    assert "DELETE" not in response.headers["access-control-allow-methods"]
