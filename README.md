# Legacy status proxy for VATSIM

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE.md)

This project provides a simple HTTP proxy server translating current VATSIM status file formats to outdated legacy formats to provide compatibility to monitor applications.

Click [here](https://github.com/dneuge/legacy-status-proxy-vatsim/releases/latest/download/legacy-status-proxy-vatsim.jar) to download the latest released version.

## Disclaimer and Intended Use

*This is a copy of [disclaimer.txt](src/main/resources/de/energiequant/vatsim/compatibility/legacyproxy/disclaimer.txt). The disclaimer has to be accepted before the server can be started.*

This disclaimer is a more application-specific addition to the general points stated in the [MIT license](LICENSE.md).

By using the application and accepting this disclaimer you confirm that you also accept all related software licenses. This includes (but may not be limited to) the MIT and Apache 2.0 licenses. See the list of dependencies for more information (available from "About" dialog on GUI or by running with `--version` on CLI). Generic copies of all license texts are available at runtime from the "About" dialog or by running `--license <LICENSE>` on CLI (see `--help` or `--version` for more information on usage). Redistribution of this software in binary form may be subject to the restrictions set by combination of the software licenses of all included dependencies.

The server is intended to enable "passive" clients (e.g. status monitors) to access more current data formats than they originally support. A "passive" client only consumes status information but does not perform any interaction with the live VATSIM network.

The server shall **not** be used to provide information to unsupported "active" clients (pilot or ATC clients) which directly interact with the actual VATSIM network (not just consuming status information). Providing manipulated information to such clients may result in unintended behaviour or enable otherwise unapproved or outdated software to access VATSIM. This may be a violation of the [Code of Conduct](https://www.vatsim.net/documents/code-of-conduct) (e.g. item A7 as of March 2021).

All data processed and served by the proxy server is subject to policies and restrictions set by VATSIM. By default requests are only served to local applications (identified by IP addresses `127.0.0.1` or `::1`). Depending on your local regulation served data may be considered privacy-relevant and thus should be handled carefully in accordance to local regulations and legal requirements as well as all other applicable policies. It is recommended to host this server strictly private and **not** open access to the general public.

By using the server you agree with all terms of the [MIT license](LICENSE.md), including (but not limited to) the general disclaimer:

> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
> IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
> FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
> AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
> LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
> THE SOFTWARE.

## Current state

- legacy Whazzup data files can be generated from JSON v3 data files
- network information ("URL index") is combined from legacy and JSON formats

## Known limitations

- providing service via IPv6 requires a host name (i.e. just an IPv6 address cannot be used as a local host name)
- non-ASCII characters on served data may be wrong
- restarting the server in quick succession is not possible (Socket bind failure for socket / Address already in use)
- some information may have been removed from recent formats and will be replaced by placeholders (e.g. flight plan revision numbers or controller coordinates)
  - clients may be unable to locate unknown online ATC stations as a result of ATC coordinates having been removed
- some information may only be available in recent formats (e.g. pilot ratings); no compatibility is provided for such data

## How to configure and run

The proxy server can be used as a GUI desktop application or via command line (CLI). You can use `--no-gui` to enforce the CLI version on a desktop system.

When running in default settings a local host name of `localhost` and a server port of `8080` will be assumed. By default, the server only serves content to clients with IP addresses `127.0.0.1` (IPv4 "localhost") or `::1` (IPv6 "localhost"). However, that IP filter is mainly useful to protect against unintentional serving of VATSIM data to third parties and should not be considered a security feature in regards to software vulnerabilities. It is still necessary to use a properly configured restrictive firewall/packet filter and to regularly check for updates as the server will be listening on all network interfaces and thus may pose a security risk even when configured correctly.

### Required software

You will need to have Java installed in order to run the proxy server. Minimum supported Java version is 1.8, maximum tested version is Java 11. If you don't have Java installed and don't know where to get it you may want to check [AdoptOpenJDK](https://adoptopenjdk.net/) and download a their Java 11 HotSpot JVM.

### Using the GUI (desktop application)

Download a released version as a JAR file and start it.

On the first run or after you have revoked your earlier agreement the "About" dialog will open automatically and present the disclaimer. You can switch to the other tabs for more information on included software dependencies and associated software licenses. Please read the disclaimer carefully and click the checkbox if you accept it. The checkbox is part of the server configuration. Click "Save configuration" to save your choice. Saving the configuration will create a new file (by default: `legacy-status-proxy-vatsim.properties`) in the folder you started the JAR file from. Close or move the "About" dialog when you are done. You can always reopen the dialog using the "About" button on the main window.

The proxy server will complete starting up in default configuration after you have accepted the disclaimer. It will stop automatically if you change your mind and revoke the agreement. You cannot run the server without accepting the disclaimer and software licenses.

The main window contains a log of everything happening in the proxy server. Errors will appear in red. Black messages are just shown for information and do not indicate errors. During normal operation no red messages should be visible and the status line below the log should indicate "Server is running." You can stop and (re)start the HTTP server using the "Run/Stop" button.

To test if everything works correctly, you can visit [http://localhost:8080/](http://localhost:8080/) with a browser on your local machine to check if everything works correctly. You should see a text file telling you that you just accessed the proxy and that refers to `url0=http://localhost:8080/vatsim-data.txt`. Try opening [that URL](http://localhost:8080/vatsim-data.txt) as well and you should see a large text file showing the current VATSIM network status. Check the log if this does not work.

It is strongly recommended to run the proxy server on the same machine you are running the other application(s) on that require(s) a compatibility layer. To allow access from other machines their IP addresses need to be added to the list in the "Configure" dialog, otherwise the server will only log `rejecting connection from ...`. If you see such messages confirm that the IP address is known to you and belongs to a computer you want to authorize, then copy the logged IP address to the configuration dialog and add the IP as described below.

Closing the main window or clicking "Quit" will stop the server.

#### Configuration options

Click the "Configure" button on the main window to open see the available options.

**Upstream connection**/**Base URL** should usually not be changed (default: `http://status.vatsim.net`). This is the first part of the address that will be used to fetch data from VATSIM. Changing the base URL requires the whole application to be restarted for the new setting to become effective.

Settings in **HTTP Server** may need to be changed if you want to open access to other computers or the default port is already used by another application. **Host name** should be set to the domain name or IPv4 address seen by all clients that are supposed to access the proxy (default: `localhost`). **Port** (default: `8080`) can be chosen freely but ports below 1024 may be reserved and generally unavailable depending on your system (e.g. on Linux). **Encode data file in UTF-8 instead of ISO-8859-1** should be toggled if the client you are using displays ATIS information without line-breaks and free-text information such as pilot or controller names are truncated or contain garbage. Note that this setting depends on the client being used; serving multiple clients at the same time may require a second instance configured differently (see the CLI options below). To apply any of the HTTP server settings the server needs to be restarted using the "Run/Stop" button. Changing the host name may also require a restart of all previously connected clients.

**Access (IP Filter)** controls which clients are allowed to access the proxy server. You can add a new IPv4 or IPv6 address by entering it in the **IP Address** field and clicking **Add**. Addresses need to be entered exactly as printed on the log; if in doubt first access the server from a blocked machine and copy & paste the IP address from the log to the configuration window. To block access for a previously allowed client select its IP address from the list and click **Remove**. Clicking **Reset** will revert to the default configuration ("localhost" meaning IPv4 address `127.0.0.1` and IPv6 address `::1` expanded to `0:0:0:0:0:0:0:1`). All changes to the IP filter are immediately effective as shown in the list. Every change is also confirmed by a log message `Access is now allowed from ...`.

**Save configuration** will, if not already done, create a file to persist your current settings (by default: `legacy-status-proxy-vatsim.properties` in the directory you started the JAR from). This will also save whether you accepted the disclaimer or rejected it (same as in the "About" dialog).

### Setting up clients

Before setting up a client please check if an updated version of the client has already been made available. It may not be necessary to use this proxy server.

If your wanted client is not listed here check the client settings if it allows to enter a data source URL (maybe already indicating `http://status.vatsim.net/`) and change it to point to your proxy server (by default: `http://localhost:8080/`). Restart the client if it does not seem to update and check if the proxy server reports any errors. Double-check with a browser on the same machine if the URL you specified delivers the "index" as seen on [http://status.vatsim.net/](http://status.vatsim.net/) and the URL listed as `url0` shows a CSV-like dump of the network status.

In addition to this description please refer to the GitHub discussions for [compatibility](https://github.com/dneuge/legacy-status-proxy-vatsim/discussions/categories/compatibility) for any questions or to describe how to set up additional clients.

#### QuteScoop

Open QuteScoop's Preferences dialog. In the "Network" tab change "network" from "VATSIM" to "user defined network" and enter the URL to your proxy server (default: `http://localhost:8080/`). Re-enable "ATC bookings" or bookings will be missing. Restart QuteScoop to get rid of any cached information. The status line should display "User Network - today ....z: ... clients" and you should see all pilots and controllers as usual.

View the text of some ATIS station. If you do not see line breaks try changing the *encode data file in UTF-8 instead of ISO-8859-1* option as described above, restart the proxy server and reload the data in QuteScoop (F5).

#### ServInfo

The status proxy needs to be configured as a network, do *not* configure anything in *Proxy settings*.

##### as a custom network

Open *Options/Connection Parameters*. In section *Custom Network Settings* select a free slot such as *Custom Network 1*. Choose a *Network name* (for example `VATSIM Legacy`) and enter the proxy address as *Locator path url* (default: `http://localhost:8080/`). Confirm the settings with *OK*.

Use the icon with two networked computers to retrieve data through the proxy; do not click the VATSIM logo.

##### as "VATSIM"

Close ServInfo and navigate to the application's folder. Open `servinfo.ini` with a text editor. Find the `VATSIM Servers Locator Url` setting and change the URL to your server address (default: `http://localhost:8080/`). Save the file, close the editor and start ServInfo.

Click the VATSIM logo to retrieve data through the proxy.

### Using the CLI (command-line)

The CLI is useful if you want to start multiple instances, run it without a window in the background or on a server machine.

If the JAR file cannot be run directly on your system you have to prefix the command with `java -jar`. 

Arguments are appended to the end of the command. Available options are:

| Option                             | Description                                                                                             |
| ---------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `--no-gui`                         | force to stay on CLI, do not start GUI even if available                                                |
| `--no-classpath-check`             | disables check for possibly broken Java class path at application startup                               |
| `--help`                           | displays a complete list of all available options                                                       |
| `--version`                        | displays version and dependency information together with the associated licenses                       |
| `--license LICENSE`                | displays the specified license identified by the keys shown on `--help` or `--version`                  |
| `--disclaimer`                     | displays the disclaimer                                                                                 |
| `--accept-disclaimer-and-licenses` | accepts the disclaimer and licenses                                                                     |
| `--config FILE`                    | use the specified file for configuration instead of the default `legacy-status-proxy-vatsim.properties` |
| `--save-config`                    | saves the configuration after processing CLI options (creates the file if it does not exist yet)        |

Configuration via CLI is not supported yet. It is recommended to create a configuration via the GUI and use the resulting configuration file for CLI. It is also possible to persist the configuration using `--save-config` in the state immediately after parsing CLI options. Combining `--save-config` with a non-existing `--config FILE` thus creates a new file with default values that can be edited manually in a text editor. Saving a configuration with `--accept-disclaimer` will persist the agreement.

## FAQ / Common issues

### ATIS has no line breaks / free text is truncated or contains weird characters

**Cause:** Clients are not served in their expected character set.

Try to change the **encode data file in UTF-8 instead of ISO-8859-1** option and restart the server (see above for details). This will depend on the client you are using: VATSIM changed the character set in the last years of the old CSV-like data file format and some (pre-release versions of) clients had already been migrated, others not. Serving multiple clients at the same time may require a second server instance to work around this incompatibility.

### Some controllers are not visible

**Possible cause:** The client misses airspace data for affected controllers.

Check if there is updated airspace information for the affected client. Before JSON format version 3 all online controllers had coordinates and many clients relied on that information to display unknown ATC stations. JSON v3 removed those coordinates. Currently, there is no good way such data could be supplied by the proxy.

### Saving configuration is not possible (disabled)

**Cause:** The proxy was launched from a system directory, for example by using special functions such as "recent files" on Windows.

The configuration file is assumed to be located at the current working directory which may not be a valid location in some cases. Try to launch the proxy from the directory it is actually located in (i.e. navigate to the correct folder in Windows Explorer). You can also create a shortcut which allows you to choose the working directory. Alternatively, you can run the proxy with a specific configuration file path provided by command-line argument `--config FILE`.

### Server cannot be started

#### Socket bind failure for socket / Address already in use

**Cause 1:** Another application already runs on the selected port.

Configure the proxy server to use another port or terminate or reconfigure the other application.

**Cause 2:** Another instance of the proxy server is already running.

Quit either instance.

**Cause 3:** The proxy server has recently been restarted.

Proxy restarts in quick succession are currently not possible if clients have accessed the server recently. Wait a moment and try again; the port should become available again after approximately 30 to 60 seconds.

#### Permission denied

**Cause:** You are not allowed to run a server on the selected port.

Configure the proxy to use another port or check your system permissions (e.g. ports below 1024 are reserved on Linux).

### Server responds with "Forbidden" (log: rejecting connection from...)

**Cause:** The affected client's IP address has not been allowed to access the server. By default, proxy service is limited to "localhost" (`127.0.0.1` or `::1`).

Check if the client should really be allowed to access the server and add the IP address exactly as printed on the server log to your configuration.

### Server responds with "Not authoritative" (HTTP status 421 Misdirected Request)

**Cause:** The affected client did not call the proxy server by its configured host name.

Check the local host name (default: `localhost`) in your server configuration. Choose a host name or IPv4 address that can be accessed by all wanted clients and only refer to that exact host name on all clients.

### An exception prevents the application from starting

**Possible cause:** The file path leading up to the JAR file contains a directory ending with an exclamation mark (`!`).

Java is unable to access such paths properly. Please rename the offending directory or move the JAR file to another location.

**Any other cause may exist.** Please run the proxy from command line to see all log output and debug information. That information may already contain a message indicating the reason for startup failure. If you are unable to resolve or think this may be a common issue please provide these details to the developer for analysis.

## Compilation

### Install unpublished dependencies

The following projects do not have Maven artifacts nor an official POM file, so they need to be fetched, repackaged and installed locally as well:

- [VAT-Spy Client Data](https://github.com/vatsimnetwork/vatspy-data-project) at revision `4f3ae5ecce72ace70c26c22ef751cd3bc03201de`

Automated checkout, build and installation can be performed on Linux systems by running [install-unpublished-dependencies.sh](install-unpublished-dependencies.sh).

#### Updating/packaging VAT-Spy data

Unfortunately, VAT-Spy data is not available through an official artifact and needs to be packaged locally using [deps/vatspy-data-project/install.sh](deps/vatspy-data-project/install.sh). To package a specific commit hash provide the hash as first argument, otherwise the latest official version will be determined from VATSIM API.

The script is only available for Linux systems but may work on MacOS or in Windows git bash.

Please do not deploy the unofficial package to any servers, only install it locally on the build machine.

### Compilation

Make sure all dependencies have been installed locally in Maven.

Run `mvn clean install` to compile, locally install the status proxy and generate a "shaded" JAR file containing all dependencies as `target/legacy-status-proxy-vatsim.jar`.

### Updating dependencies

[Attribution Maven Plugin](https://github.com/jinnovations/attribution-maven-plugin) is used on every build to ensure all artifacts are listed in `src-gen/main/resources/de/energiequant/vatsim/compatibility/legacyproxy/attribution/attribution.xml`. At application startup that file is used to check that all required licenses have been included and copyright notices have been recorded for every dependency.

This requires additional changes to be made after adding a new dependency or changing versions in the POM file:

- All copyright notices must be recorded in [CopyrightNotice](src/main/java/de/energiequant/vatsim/compatibility/legacyproxy/attribution/CopyrightNotice.java) class, otherwise an exception will be thrown when starting the proxy.
  - Use the official copyright notice if available, preferably from files included within the used JAR artifacts.
  - Some licenses or projects may require specific file contents to be included, for example some "notice" file for Apache 2.0 licensed projects.
  - Copyright, license or additional notices may change between version updates. Check that the copyright information is up-to-date. Fully replace the information if in doubt.
- If known licenses are referred by unknown aliases, adapt [License.java](src/main/java/de/energiequant/vatsim/compatibility/legacyproxy/attribution/License.java) enum.
- If new licenses are introduced:
  - check license compatibility and implications on effective license for binary distribution
  - add a copy of the official license text to [src/main/resources/de/energiequant/vatsim/compatibility/legacyproxy/attribution/](src/main/resources/de/energiequant/vatsim/compatibility/legacyproxy/attribution/)
  - adapt [License.java](src/main/java/de/energiequant/vatsim/compatibility/legacyproxy/attribution/License.java) enum
  - if distribution license is affected:
    - confirm with project lead that the new license will not lead to any complications for development or future use and are really okay to be introduced to the project
    - change `EFFECTIVE_LICENSE` in [Main](src/main/java/de/energiequant/vatsim/compatibility/legacyproxy/Main.java) class
    - check if the disclaimers or any program behaviour (mandatory license confirmation etc.) need to be adapted
    - update all accompanying documentation

## License

The implementation and accompanying files (not including copies of license texts themselves) are released under [MIT license](LICENSE.md). Dependencies are subject to their individual licenses. Binary redistribution is subject to the combination of all licenses. See the disclaimer for more information.

All processed and served data is subject to policies and restrictions set by VATSIM and your local regulations. See section *Disclaimer and Intended Use* for more details.
