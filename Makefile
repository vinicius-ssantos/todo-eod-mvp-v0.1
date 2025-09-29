.PHONY: docs docs:verify

docs:
	mvn -q -DskipTests -f backend/pom.xml org.springdoc:springdoc-openapi-maven-plugin:generate
	bash scripts/verify_docs.sh --fix

docs:verify:
	bash scripts/verify_docs.sh

