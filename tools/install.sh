#!/usr/bin/env bash

function fail() {
  echo "$@" >&2
  exit 1
}

set -e

myDir="$(cd "$(dirname "$0")" || fail "cannot find current directory" ; pwd)"
target="$HOME/bin/puppet-to-diagram"

pushd "$myDir/.."

sbt assembly

cat > "$target" <<EOF
#!/usr/bin/env bash
exec java -jar "\$0" "\$@"
EOF

cat target/scala-2.12/puppet-to-diagram-assembly-0.1.jar >> "$target"

chmod +x "$target"
