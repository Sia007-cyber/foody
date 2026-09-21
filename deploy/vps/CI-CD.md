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

The deployment job downloads those exact CI artifacts, makes one archive, and
copies only that archive to the VPS with OpenSSH. It does not build or check out
source on the VPS. The GitHub environment and job concurrency group serialize
production deployments; queued newer deployments do not cancel an active one.

## GitHub configuration

Use the existing GitHub Environment named exactly `Production` (environment
names are case-sensitive in this deployment contract). Environment protection
rules such as required reviewers, protected `main`, and an optional wait timer
are strongly recommended. Configure these values in that environment:

| Kind | Name | Value |
|---|---|---|
| Variable | `PROD_HOST` | The VPS DNS name or IPv4 address (for example `deploy.dayca.ir`; no scheme or port). |
| Variable | `PROD_USER` | `foody-deploy` |
| Secret | `PROD_SSH_PRIVATE_KEY` | The dedicated unencrypted Ed25519 private key used only by Actions. |
| Secret | `PROD_SSH_KNOWN_HOSTS` | A verified OpenSSH `known_hosts` line for `PROD_HOST`. |

Do not use a personal key or put the private key in the repository. Generate the
key on a trusted administrator machine with `ssh-keygen -t ed25519 -f
foody-github-actions -C foody-github-actions`. Install only the `.pub` value on
the server. Obtain the host public key through a trusted channel, compare its
fingerprint with the VPS console/provider record, then store the complete
`known_hosts` line as the secret. Do not disable `StrictHostKeyChecking`.

Repository Actions settings must allow read access to contents; the workflow
declares no write permission. Branch protection should require the `Backend
tests and package` and `Frontend tests, lint, and build` checks before merging.

## One-time VPS setup

Run the following as a VPS administrator. These commands do not alter
`/etc/foody/foody.env`, `/var/lib/foody/uploads`, MySQL, port exposure, or Nginx.
The existing application/service setup in `PRODUCTION.md` must already be
complete.

```sh
sudo adduser --system --group --home /var/lib/foody-deploy foody-deploy
sudo install -d -o foody-deploy -g foody-deploy -m 0700 /var/lib/foody-deploy
sudo install -d -o foody-deploy -g foody-deploy -m 0700 /var/lib/foody-deploy/incoming
sudo install -d -o root -g root -m 0755 /opt/foody
sudo install -d -o root -g root -m 0755 /opt/foody/releases
sudo install -o root -g root -m 0755 /tmp/foody-deploy /usr/local/sbin/foody-deploy
sudo install -d -o foody-deploy -g foody-deploy -m 0700 /var/lib/foody-deploy/.ssh
sudoedit /var/lib/foody-deploy/.ssh/authorized_keys
sudo chown foody-deploy:foody-deploy /var/lib/foody-deploy/.ssh/authorized_keys
sudo chmod 0600 /var/lib/foody-deploy/.ssh/authorized_keys
sudo visudo -f /etc/sudoers.d/foody-deploy
sudo chmod 0440 /etc/sudoers.d/foody-deploy
sudo visudo -cf /etc/sudoers.d/foody-deploy
```

Before running the commands, securely copy the reviewed repository file
`deploy/vps/foody-deploy` to `/tmp/foody-deploy`; the routine pipeline never
copies or updates this privileged program. Put exactly the dedicated public key
on one line in `authorized_keys`. The workflow needs remote commands and SCP, so
do not attach a false forced command to that key. Rely on the dedicated account,
key, filesystem permissions, and narrow sudo rule below, and keep SSH password
authentication disabled for this account.

The exact `/etc/sudoers.d/foody-deploy` content is:

```sudoers
foody-deploy ALL=(root) NOPASSWD: /usr/local/sbin/foody-deploy
```

This permits no arbitrary `systemctl`, shell, editor, or file-copy command as
root. The root-owned deployment program validates both arguments, snapshots and
validates the untrusted incoming tar archive, and only then operates on Foody's
release path and service. Whenever `deploy/vps/foody-deploy` changes, review it
and reinstall it manually with the `install` command above before enabling the
corresponding workflow change.

The deploy account also needs its login shell enabled because OpenSSH invokes
remote commands. Confirm `getent passwd foody-deploy` shows `/bin/bash` or
`/bin/sh`; if Ubuntu created `/usr/sbin/nologin`, set it once:

```sh
sudo usermod --shell /bin/bash foody-deploy
```

## Activation, verification, rollback, and retention

Each release ID combines the 40-character commit SHA, Actions run ID, and run
attempt. The privileged script refuses an existing directory, installs the full
archive under `/opt/foody/releases/<release-id>`, fixes root ownership and
read-only file permissions, then atomically replaces `/opt/foody/current`.
Only after activation does it restart `foody-backend.service`.

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
