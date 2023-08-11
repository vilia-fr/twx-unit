# #!/bin/bash

set -e

for f in ext-sdk/*.jar; do 
	base=$(echo "$f" | sed -r 's/^ext-sdk\/(.*)-[0-9].*\.jar$/twx-ext-\1/g')
	
	echo
	echo "Installing $base ($f)"
	echo
	
	mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file \
		-Dfile="$f" \
		-DgroupId=twx \
		-DartifactId="$base" \
		-Dversion=9.0 \
		-Dpackaging=jar \
		-DlocalRepositoryPath=lib
done
