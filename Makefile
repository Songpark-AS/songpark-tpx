#!/usr/bin/env make -f

export VERSION ?=$(shell git rev-parse HEAD)

DEPLOYMENT_DIR=deployment
PROJECT_DIR=tpx-clj


$(shell git rev-parse HEAD > VERSION.git)
$(shell mkdir -p $(PROJECT_DIR)/resources)


default:
	@echo "Check commands"
	@echo "When deploying or uploading, use the syntax 'make TARGET=<adsf> deploy|upload'"


build:
	@echo "Building TPX"
	sh $(DEPLOYMENT_DIR)/prep.sh
	(cd tpx-clj && lein with-profile uberjar uberjar)


upload:
	@echo "Deploying TPX to $(TARGET)"
	scp $(PROJECT_DIR)/target/uberjar/tpx.jar $(TARGET):/usr/local/tpx

deploy: build upload
