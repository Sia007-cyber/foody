# One-time primary-admin provisioning

Run this only from the production host, with the deployed backend JAR and its normal
production database/JWT environment already available. The command starts no HTTP
server. It creates a new account only when neither identity exists; otherwise both
identity fields must resolve to the same existing account. A different existing
primary admin causes a safe failure.

For `zsh`, collect all sensitive values without putting them in shell history:

```zsh
read -r "foody_pa_email?Primary-admin email: "
read -r "foody_pa_phone?Primary-admin phone: "
read -r "foody_pa_name?Primary-admin full name: "
read -rs "foody_pa_password?Primary-admin password: "; print

FOODY_PRIMARY_ADMIN_PROVISION_EMAIL="$foody_pa_email" \
FOODY_PRIMARY_ADMIN_PROVISION_PHONE="$foody_pa_phone" \
FOODY_PRIMARY_ADMIN_PROVISION_FULL_NAME="$foody_pa_name" \
FOODY_PRIMARY_ADMIN_PROVISION_PASSWORD="$foody_pa_password" \
java -jar <DEPLOYED_BACKEND_JAR> \
  --spring.profiles.active=<PRODUCTION_PROFILE> \
  --spring.main.web-application-type=none \
  --foody.primary-admin-provision.enabled=true

unset foody_pa_email foody_pa_phone foody_pa_name foody_pa_password
```

The password must be 8–128 characters. It is BCrypt-encoded by the application and
is never accepted through an HTTP endpoint. Remove the provisioning values from any
deployment environment after this process exits. Re-running for the same established
primary admin is a no-op; attempting to establish another primary admin fails.
