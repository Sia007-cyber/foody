# Foody GitHub Actions CI/CD

The workflow in `.github/workflows/ci-cd.yml` validates every pull request and
every pushed branch. A push to `main` deploys only after both validation jobs
pass. `workflow_dispatch` supports an intentional redeploy of `main`; dispatches
from other refs run CI but cannot deploy.

The backend job uses Java 21 and Maven's dependency cache, and runs `clean
verify` (tests plus executable JAR packaging). The frontend job uses Node.js 22,
the npm cache, `npm ci`, tests, lint, and a production build. The production
build explicitly leaves `VITE_API_BASE_URL` empty so `/api` and `/uploads` stay
same-origin. No production secret is needed at build time.

The deployment job runs on the production VPS's self-hosted runner, downloads
those exact CI artifacts, and makes one archive locally. It does not build or
check out application source on the VPS. The GitHub environment and job
concurrency group serialize production deployments; queued newer deployments do
not cancel an active one.

## GitHub configuration

Use the existing GitHub Environment named exactly `Production` (environment
names are case-sensitive in this deployment contract). Environment protection
rules such as required reviewers, protected `main`, and an optional wait timer
are strongly recommended. The self-hosted deployment path needs no production
host, SSH user, private-key, or known-hosts value in GitHub.

After one successful self-hosted deployment, these obsolete Environment values
can be deleted: variables `PROD_HOST` and `PROD_USER`, and secrets
`PROD_SSH_PRIVATE_KEY` and `PROD_SSH_KNOWN_HOSTS`.

Repository Actions settings must allow read access to contents; the workflow
declares no write permission. Branch protection should require the `Backend
tests and package` and `Frontend tests, lint, and build` checks before merging.

## Self-hosted runner and one-time VPS setup

The repository's GitHub Actions runner must be registered at repository scope,
online with the standard `self-hosted`, `Linux`, and `X64` labels, and installed
as a service running as the unprivileged `foody-deploy` user. Never run the
runner as root. Restrict repository administration and workflow changes because
a self-hosted runner is production infrastructure.

The existing application/service setup in `PRODUCTION.md` must already be
complete. Verify the deployment boundary with these safe checks on the VPS:

```sh
getent passwd foody-deploy
sudo systemctl status 'actions.runner.*' --no-pager
sudo stat -c '%U:%G %a %n' /usr/local/sbin/foody-deploy
sudo stat -c '%U:%G %a %n' /var/lib/foody-deploy/incoming
sudo visudo -cf /etc/sudoers.d/foody-deploy
sudo -u foody-deploy sudo -n -l
```

The script must report `root:root 755`, the incoming directory must be writable
only by the deployment account (normally `foody-deploy:foody-deploy 700`), and
the runner service must run as `foody-deploy`. The routine pipeline never copies
or updates the privileged program.

The exact `/etc/sudoers.d/foody-deploy` content is:

```sudoers
foody-deploy ALL=(root) NOPASSWD: /usr/local/sbin/foody-deploy
```

This permits no arbitrary `systemctl`, shell, editor, or file-copy command as
root. The root-owned deployment program validates both arguments, snapshots and
validates the unprivileged incoming tar archive, and only then operates on
Foody's release path and service. Whenever `deploy/vps/foody-deploy` changes,
review it and reinstall it manually before enabling the corresponding workflow
change.

The deploy job uses only `[self-hosted, Linux, X64]`, is guarded to `main`
pushes or manual dispatches on `main`, and depends on both GitHub-hosted CI jobs.
Pull-request code never schedules work on the production runner.

## Activation, verification, rollback, and retention

Each release ID combines the 40-character commit SHA, Actions run ID, and run
attempt. The self-hosted job clears a run-specific staging directory under
`RUNNER_TEMP`, downloads the backend and frontend artifacts produced by CI,
packages them, and atomically places the complete archive at
`/var/lib/foody-deploy/incoming/foody-<release-id>.tar.gz`. It then invokes only:

```sh
sudo -n /usr/local/sbin/foody-deploy "$RELEASE_ID" "$ARCHIVE"
```

The privileged script refuses an existing directory, installs the full archive
under `/opt/foody/releases/<release-id>`, fixes root ownership and read-only file
permissions, then atomically replaces `/opt/foody/current`. Only after activation
does it restart `foody-backend.service`.

It requires the service to be active and performs bounded checks against:

- `http://127.0.0.1:8080/api/businesses`
- `https://dayca.ir/api/businesses`
- `https://dayca.ir/`

If restart or health verification fails, it atomically restores the previously
recorded release, restarts the backend, verifies rollback health, and still exits
non-zero so Actions shows a failed deployment. On a first-ever deployment with
no previous release, it removes the failed current link and stops the service.
It never changes the environment or uploads directory.

After a successful deployment only, cleanup retains the six newest releases and
always excludes both the current release and that deployment's rollback target.
Unexpectedly named directories are not deleted.

Flyway migrations are forward-only. Application rollback does not undo a
database migration and the deployment script never attempts a down migration.
Automatic application rollback is safe only when every newly applied migration
is backward-compatible with the previous application. For an incompatible
migration, use a planned maintenance window and a tested, matched database and
uploads backup/restore procedure.

Primary-admin provisioning remains the separate, manual procedure documented in
`deploy/PRIMARY_ADMIN_PROVISIONING.md`; CI/CD never invokes it.

## Developer workflow

Work on a feature or fix branch, push it, and open a pull request. CI runs the
backend and frontend validation without deployment. After review and all
required checks pass, merge to `main`; that push repeats full CI and deploys the
validated artifacts automatically. Use the Actions `workflow_dispatch` control
on `main` only for an intentional manual redeploy.
