This is the development-related documentation. For other information please refer to the [readme](../README.md) or the [user manual](manual.md).

# Install unpublished dependencies

The following projects do not have Maven artifacts nor an official POM file, so they need to be fetched, repackaged and installed locally as well:

- [VAT-Spy Client Data](https://github.com/vatsimnetwork/vatspy-data-project) at revision `b2af486c0842fbb827535d2a9b0eee80a3c78469`

Automated checkout, build and installation can be performed on Linux systems by running [install-unpublished-dependencies.sh](../install-unpublished-dependencies.sh).

## Updating/packaging VAT-Spy data

Unfortunately, VAT-Spy data is not available through an official artifact and needs to be packaged locally using [deps/vatspy-data-project/install.sh](../deps/vatspy-data-project/install.sh). To package a specific commit hash provide the hash as first argument, otherwise the latest official version will be determined from VATSIM API.

The script is only available for Linux systems but may work on MacOS or in Windows git bash.

Please do not deploy the unofficial package to any servers, only install it locally on the build machine.

# Compilation

Make sure all dependencies have been installed locally in Maven.

Run `mvn clean install` to compile, locally install the status proxy and generate a "shaded" JAR file containing all dependencies as `target/legacy-status-proxy-vatsim.jar`.

# Updating dependencies

[Attribution Maven Plugin](https://github.com/jinnovations/attribution-maven-plugin) is used on every build to ensure all artifacts are listed in `src-gen/main/resources/de/energiequant/vatsim/compatibility/legacyproxy/attribution/attribution.xml`. At application startup that file is used to check that all required licenses have been included and copyright notices have been recorded for every dependency.

This requires additional changes to be made after adding a new dependency or changing versions in the POM file:

- All copyright notices must be recorded in [CopyrightNotice](../src/main/java/de/energiequant/vatsim/compatibility/legacyproxy/attribution/CopyrightNotice.java) class, otherwise an exception will be thrown when starting the proxy.
  - Use the official copyright notice if available, preferably from files included within the used JAR artifacts.
  - Some licenses or projects may require specific file contents to be included, for example some "notice" file for Apache 2.0 licensed projects.
  - Copyright, license or additional notices may change between version updates. Check that the copyright information is up-to-date. Fully replace the information if in doubt.
- If known licenses are referred by unknown aliases, adapt [License.java](../src/main/java/de/energiequant/vatsim/compatibility/legacyproxy/attribution/License.java) enum.
- If new licenses are introduced:
  - check license compatibility and implications on effective license for binary distribution
  - add a copy of the official license text to [src/main/resources/de/energiequant/vatsim/compatibility/legacyproxy/attribution/](../src/main/resources/de/energiequant/vatsim/compatibility/legacyproxy/attribution/)
  - adapt [License.java](../src/main/java/de/energiequant/vatsim/compatibility/legacyproxy/attribution/License.java) enum
  - if distribution license is affected:
    - confirm with project lead that the new license will not lead to any complications for development or future use and are really okay to be introduced to the project
    - change `EFFECTIVE_LICENSE` in [Main](../src/main/java/de/energiequant/vatsim/compatibility/legacyproxy/Main.java) class
    - check if the disclaimers or any program behaviour (mandatory license confirmation etc.) need to be adapted
    - update all accompanying documentation
