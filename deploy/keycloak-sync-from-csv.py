#!/usr/bin/env python3
import csv
import json
import os
import sys
import urllib.parse
import urllib.request


def _env(name: str, default: str | None = None) -> str | None:
    value = os.environ.get(name)
    if value is None or value.strip() == "":
        return default
    return value.strip()


def _env_bool(name: str, default: bool) -> bool:
    value = _env(name)
    if value is None:
        return default
    return value.lower() in {"1", "true", "yes", "y", "on"}

def _env_int(name: str, default: int | None = None) -> int | None:
    value = _env(name)
    if value is None:
        return default
    try:
        return int(value, 10)
    except Exception:
        raise ValueError(f"{name} must be an integer, got: {value!r}")


class KeycloakClient:
    def __init__(self, base_url: str, realm: str, token: str):
        self.base_url = base_url.rstrip("/")
        self.realm = realm
        self.token = token

    def _req(self, method: str, path: str, body: dict | list | None = None, query: dict | None = None):
        url = f"{self.base_url}{path}"
        if query:
            url += "?" + urllib.parse.urlencode(query)
        data = None
        headers = {
            "Authorization": f"Bearer {self.token}",
            "Accept": "application/json",
        }
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            headers["Content-Type"] = "application/json"
        req = urllib.request.Request(url=url, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                raw = resp.read()
                if not raw:
                    return resp.status, None, dict(resp.headers)
                try:
                    return resp.status, json.loads(raw.decode("utf-8")), dict(resp.headers)
                except Exception:
                    return resp.status, raw.decode("utf-8", errors="replace"), dict(resp.headers)
        except urllib.error.HTTPError as e:
            raw = e.read()
            try:
                payload = json.loads(raw.decode("utf-8"))
            except Exception:
                payload = raw.decode("utf-8", errors="replace")
            return e.code, payload, dict(e.headers)

    def list_groups(self) -> list[dict]:
        status, payload, _ = self._req("GET", f"/admin/realms/{self.realm}/groups")
        if status != 200:
            raise RuntimeError(f"Failed to list groups: HTTP {status} {payload}")
        return payload or []

    def create_group(self, name: str) -> str:
        status, payload, headers = self._req(
            "POST",
            f"/admin/realms/{self.realm}/groups",
            body={"name": name},
        )
        if status not in (201, 204):
            raise RuntimeError(f"Failed to create group {name}: HTTP {status} {payload}")

        location = headers.get("Location") or headers.get("location")
        if location:
            return location.rstrip("/").split("/")[-1]

        groups = self.list_groups()
        for g in groups:
            if g.get("name") == name:
                return g["id"]
        raise RuntimeError(f"Created group {name} but couldn't resolve its id")

    def get_user_by_username(self, username: str) -> dict | None:
        status, payload, _ = self._req(
            "GET",
            f"/admin/realms/{self.realm}/users",
            query={"username": username, "exact": "true"},
        )
        if status != 200:
            raise RuntimeError(f"Failed to search user {username}: HTTP {status} {payload}")
        if not payload:
            return None
        return payload[0]

    def create_user(self, username: str, email: str | None, first: str | None, last: str | None) -> str:
        body: dict = {"username": username, "enabled": True}
        if email:
            body["email"] = email
        if first:
            body["firstName"] = first
        if last:
            body["lastName"] = last

        status, payload, headers = self._req(
            "POST",
            f"/admin/realms/{self.realm}/users",
            body=body,
        )
        if status not in (201, 204):
            raise RuntimeError(f"Failed to create user {username}: HTTP {status} {payload}")

        location = headers.get("Location") or headers.get("location")
        if location:
            return location.rstrip("/").split("/")[-1]

        user = self.get_user_by_username(username)
        if user and user.get("id"):
            return user["id"]
        raise RuntimeError(f"Created user {username} but couldn't resolve its id")

    def update_user(self, user_id: str, email: str | None, first: str | None, last: str | None):
        status, payload, _ = self._req("GET", f"/admin/realms/{self.realm}/users/{user_id}")
        if status != 200:
            raise RuntimeError(f"Failed to fetch user {user_id}: HTTP {status} {payload}")
        user = payload or {}
        if email is not None:
            user["email"] = email
        if first is not None:
            user["firstName"] = first
        if last is not None:
            user["lastName"] = last
        user["enabled"] = True

        status, payload, _ = self._req("PUT", f"/admin/realms/{self.realm}/users/{user_id}", body=user)
        if status not in (204,):
            raise RuntimeError(f"Failed to update user {user_id}: HTTP {status} {payload}")

    def set_user_password(self, user_id: str, password: str, temporary: bool):
        status, payload, _ = self._req(
            "PUT",
            f"/admin/realms/{self.realm}/users/{user_id}/reset-password",
            body={"type": "password", "value": password, "temporary": bool(temporary)},
        )
        if status not in (204,):
            raise RuntimeError(f"Failed to set password for user {user_id}: HTTP {status} {payload}")

    def add_user_to_group(self, user_id: str, group_id: str):
        status, payload, _ = self._req(
            "PUT",
            f"/admin/realms/{self.realm}/users/{user_id}/groups/{group_id}",
            body=None,
        )
        if status not in (204,):
            raise RuntimeError(f"Failed to add user {user_id} to group {group_id}: HTTP {status} {payload}")


def get_admin_token(base_url: str, username: str, password: str) -> str:
    token_url = f"{base_url.rstrip('/')}/realms/master/protocol/openid-connect/token"
    data = urllib.parse.urlencode(
        {
            "grant_type": "password",
            "client_id": "admin-cli",
            "username": username,
            "password": password,
        }
    ).encode("utf-8")
    req = urllib.request.Request(
        url=token_url,
        data=data,
        method="POST",
        headers={"Content-Type": "application/x-www-form-urlencoded", "Accept": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
        token = payload.get("access_token")
        if not token:
            raise RuntimeError("Failed to obtain admin token")
        return token


def parse_groups(groups_raw: str | None) -> list[str]:
    if not groups_raw:
        return []
    parts = [p.strip() for p in groups_raw.split(";")]
    return [p for p in parts if p]


def main() -> int:
    csv_path = _env("CSV_PATH") or (sys.argv[1] if len(sys.argv) > 1 else None)
    if not csv_path:
        print("CSV_PATH (or first CLI arg) is required", file=sys.stderr)
        return 2

    base_url = _env("KC_BASE_URL")
    realm = _env("KC_REALM")
    admin_user = _env("KC_ADMIN_USERNAME")
    admin_pw = _env("KC_ADMIN_PW")
    if not base_url or not realm or not admin_user or not admin_pw:
        print("KC_BASE_URL, KC_REALM, KC_ADMIN_USERNAME, KC_ADMIN_PW are required", file=sys.stderr)
        return 2

    dry_run = _env_bool("DRY_RUN", False)
    create_groups = _env_bool("CREATE_GROUPS", True)
    create_users = _env_bool("CREATE_USERS", True)
    update_users = _env_bool("UPDATE_USERS", True)
    # Limit how many CSV data rows to process (excluding the header).
    # Prefer MAX_ROWS (container/internal), but support KEYCLOAK_SYNC_MAX_ROWS (compose/.env-friendly).
    max_rows = _env_int("MAX_ROWS")
    if max_rows is None:
        max_rows = _env_int("KEYCLOAK_SYNC_MAX_ROWS")
    if max_rows is not None and max_rows < 0:
        raise ValueError("MAX_ROWS / KEYCLOAK_SYNC_MAX_ROWS must be >= 0")

    token = get_admin_token(base_url, admin_user, admin_pw)
    kc = KeycloakClient(base_url, realm, token)

    group_cache: dict[str, str] = {}
    if create_groups:
        for g in kc.list_groups():
            name = g.get("name")
            gid = g.get("id")
            if name and gid:
                group_cache[name] = gid

    def ensure_group(name: str) -> str:
        if name in group_cache:
            return group_cache[name]
        if not create_groups:
            raise RuntimeError(f"Group {name} does not exist (CREATE_GROUPS=false)")
        if dry_run:
            fake_id = f"DRYRUN:{name}"
            group_cache[name] = fake_id
            print(f"[DRY_RUN] create group: {name}")
            return fake_id
        gid = kc.create_group(name)
        group_cache[name] = gid
        print(f"created group: {name}")
        return gid

    with open(csv_path, "r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        if not reader.fieldnames:
            print("CSV must have a header row", file=sys.stderr)
            return 2

        required = {"type", "name"}
        missing = required - set([h.strip() for h in reader.fieldnames if h])
        if missing:
            print(f"CSV missing required columns: {', '.join(sorted(missing))}", file=sys.stderr)
            return 2

        rows_processed = 0
        for row in reader:
            rows_processed += 1
            if max_rows is not None and rows_processed > max_rows:
                print(f"max rows reached ({max_rows}), stopping.")
                break

            type_raw = (row.get("type") or "").strip().upper()
            name = (row.get("name") or "").strip()
            if not type_raw or not name:
                continue

            if type_raw == "GROUP":
                ensure_group(name)
                continue

            if type_raw != "USER":
                raise RuntimeError(f"Unknown type {type_raw} for name={name}")

            if not create_users and not update_users:
                continue

            email = (row.get("email") or "").strip() or None
            first = (row.get("firstName") or "").strip() or None
            last = (row.get("lastName") or "").strip() or None
            password = (row.get("password") or "").strip() or None
            temporary = ((row.get("temporary") or "").strip().lower() in {"1", "true", "yes", "y", "on"})
            groups = parse_groups((row.get("groups") or "").strip() or None)

            user = kc.get_user_by_username(name)
            user_id: str | None = None

            if user is None:
                if not create_users:
                    raise RuntimeError(f"User {name} does not exist (CREATE_USERS=false)")
                if dry_run:
                    user_id = f"DRYRUN:{name}"
                    print(f"[DRY_RUN] create user: {name} email={email} firstName={first} lastName={last}")
                else:
                    user_id = kc.create_user(name, email, first, last)
                    print(f"created user: {name}")
            else:
                user_id = user.get("id")
                if user_id is None:
                    raise RuntimeError(f"User search returned no id for username={name}")
                if update_users:
                    if dry_run:
                        print(f"[DRY_RUN] update user: {name} email={email} firstName={first} lastName={last}")
                    else:
                        kc.update_user(user_id, email, first, last)
                        print(f"updated user: {name}")

            if password:
                if dry_run:
                    print(f"[DRY_RUN] set password: {name} temporary={temporary}")
                else:
                    assert user_id is not None
                    kc.set_user_password(user_id, password, temporary)
                    print(f"set password: {name} temporary={temporary}")

            if groups:
                for gname in groups:
                    gid = ensure_group(gname)
                    if dry_run:
                        print(f"[DRY_RUN] add user {name} to group {gname}")
                    else:
                        if str(gid).startswith("DRYRUN:"):
                            continue
                        assert user_id is not None
                        kc.add_user_to_group(user_id, gid)
                        print(f"added user {name} to group {gname}")

    print("done")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
