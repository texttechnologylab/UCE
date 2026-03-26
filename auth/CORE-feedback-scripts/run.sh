export KCADM="docker exec -i uce-keycloak-auth /opt/keycloak/bin/kcadm.sh"

./core_uce_users.sh hashes.txt provisioned-users.csv
