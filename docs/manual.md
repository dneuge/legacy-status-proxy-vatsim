This is the user manual for the legacy status proxy for VATSIM.

For general information and the [disclaimer](../disclaimer.txt) please refer to the [readme file](../README.md).

The proxy server can be used as a GUI desktop application or via command line (CLI). You can use `--no-gui` to enforce the CLI version on a desktop system.

When running in default settings a local host name of `localhost` and a server port of `8080` will be assumed. By default, the server only serves content to clients with IP addresses `127.0.0.1` (IPv4 "localhost") or `::1` (IPv6 "localhost"). However, that IP filter is mainly useful to protect against unintentional serving of VATSIM data to third parties and should not be considered a security feature in regards to software vulnerabilities. It is still necessary to use a properly configured restrictive firewall/packet filter and to regularly check for updates as the server will be listening on all network interfaces and thus may pose a security risk even when configured correctly.

# Required software

You will need to have Java installed in order to run the proxy server. Minimum supported Java version is 1.8, maximum tested version is Java 11. If you don't have Java installed and don't know where to get it you may want to check [AdoptOpenJDK](https://adoptopenjdk.net/) and download a their Java 11 HotSpot JVM.

# Using the GUI (desktop application)

Download a released version as a JAR file and start it.

On the first run or after you have revoked your earlier agreement the "About" dialog will open automatically and present the disclaimer. You can switch to the other tabs for more information on included software dependencies and associated software licenses. Please read the disclaimer carefully and click the checkbox if you accept it. The checkbox is part of the server configuration. Click "Save configuration" to save your choice. Saving the configuration will create a new file (by default: `legacy-status-proxy-vatsim.properties`) in the folder you started the JAR file from. Close or move the "About" dialog when you are done. You can always reopen the dialog using the "About" button on the main window.

The proxy server will complete starting up in default configuration after you have accepted the disclaimer. It will stop automatically if you change your mind and revoke the agreement. You cannot run the server without accepting the disclaimer and software licenses.

The main window contains a log of everything happening in the proxy server. Errors will appear in red. Black messages are just shown for information and do not indicate errors. During normal operation no red messages should be visible and the status line below the log should indicate "Server is running." You can stop and (re)start the HTTP server using the "Run/Stop" button.

To test if everything works correctly, you can visit [http://localhost:8080/](http://localhost:8080/) with a browser on your local machine to check if everything works correctly. You should see a text file telling you that you just accessed the proxy and that refers to `url0=http://localhost:8080/vatsim-data.txt`. Try opening [that URL](http://localhost:8080/vatsim-data.txt) as well and you should see a large text file showing the current VATSIM network status. Check the log if this does not work.

It is strongly recommended to run the proxy server on the same machine you are running the other application(s) on that require(s) a compatibility layer. To allow access from other machines their IP addresses need to be added to the list in the "Configure" dialog, otherwise the server will only log `rejecting connection from ...`. If you see such messages confirm that the IP address is known to you and belongs to a computer you want to authorize, then copy the logged IP address to the configuration dialog and add the IP as described below.

Closing the main window or clicking "Quit" will stop the server.

## Configuration options

Click the "Configure" button on the main window to open see the available options.

**Save configuration** will, if not already done, create a file to persist your current settings (by default: `legacy-status-proxy-vatsim.properties` in the directory you started the JAR from). This will also save whether you accepted the disclaimer or rejected it (same as in the "About" dialog).

### General options

**Upstream connection**/**Base URL** should usually not be changed (default: `http://status.vatsim.net`). The base URL is the first part of the address that will be used to fetch data from VATSIM. **Use VATSIM default URL** is used to protect the URL against any unwanted modification. Changing the base URL requires the whole application to be restarted for the new setting to become effective.

Settings in **HTTP Server** may need to be changed if you want to open access to other computers or the default port is already used by another application. **Host name** should be set to the domain name or IPv4 address seen by all clients that are supposed to access the proxy (default: `localhost`). **Port** (default: `8080`) can be chosen freely but ports below 1024 may be reserved and generally unavailable depending on your system (e.g. on Linux). **Encode data file in UTF-8 instead of ISO-8859-1** should be toggled if the client you are using displays ATIS information without line-breaks and free-text information such as pilot or controller names are truncated or contain garbage. Note that this setting depends on the client being used; serving multiple clients at the same time may require a second instance configured differently (see the CLI options below). To apply any of the HTTP server settings the server needs to be restarted using the "Run/Stop" button. Changing the host name may also require a restart of all previously connected clients. **Log parser errors** can be enabled to help debugging possible errors in processing upstream data (usually not required).

**Access (IP Filter)** controls which clients are allowed to access the proxy server. You can add a new IPv4 or IPv6 address by entering it in the **IP Address** field and clicking **Add**. Addresses need to be entered exactly as printed on the log; if in doubt first access the server from a blocked machine and copy & paste the IP address from the log to the configuration window. To block access for a previously allowed client select its IP address from the list and click **Remove**. Clicking **Reset** will revert to the default configuration ("localhost" meaning IPv4 address `127.0.0.1` and IPv6 address `::1` expanded to `0:0:0:0:0:0:0:1`). All changes to the IP filter are immediately effective as shown in the list. Every change is also confirmed by a log message `Access is now allowed from ...`.

### Station Locator options

Any change to the Station Locator options requires a server restart to become effective.

If **locate stations using VAT-Spy data** is enabled, the proxy will attempt to substitute missing station coordinates by looking up the callsign on a VAT-Spy database. Disabling the option will cause only the coordinates originally present in JSON v3 data to be used (which is currently missing all controller coordinates). Other options of the Station Locator will have no effect when disabling VAT-Spy data. It is highly recommended to leave this option enabled.

**Assume callsigns ending in OBS to be observers:** Observers can be indicated by client permission level but sometimes that information is not sufficient. Enable this option to additionally assume any station calling itself `something_OBS` or `something-OBS` to be an observer as well.

**Ignore clients on placeholder frequencies:** ATC clients indicate their primary frequency on data files. Controllers who do not actively provide any service to pilots usually indicate a placeholder frequency far beyond the air band used by real-world ATC. If you are only interested in actively services stations from a pilot's perspective, it is generally safe to ignore those clients. However, if you are monitoring overall network activity, it may be useful to also look up such "unserviced" stations.

**Warn about unlocatable ATC stations in log:** If an ATC client, who is not an observer, was supposed to be located according to other options but no location could be determined, the affected callsign will be logged as a warning on the main window, when enabled. This option is mainly useful for debugging; it is safe to keep it disabled.

**Warn about unlocateable observers in log:** The same as above but for observers. It is safe to keep this option disabled.

#### VAT-Spy data

This group allows to configure how VAT-Spy data should be handled by the proxy.

The proxy ships with a database that will eventually outdate if the proxy is not being updated regularly. Outdated data can cause issues such as stations being unknown/unlocatable or showing up at wrong locations.

When the proxy's internal database is being used, it checks the data age and by default warns at startup when the data is older than 270 days (9 months). This can be disabled via the **warn if integrated database older than 270 days is used** option if you want to silence that warning while continuing to use the outdated internal database. It is highly recommended to leave this option enabled and instead switch to an external database if no update is possible.

In case no updates for the proxy server should be available, you can [download the required data manually](https://github.com/vatsimnetwork/vatspy-data-project) and set it up as an external database: Enable **use external database** and **browse** to the directory containing the `VATSpy.dat` and `FIRBoundaries.dat` files. A **pre-check** will attempt to load the database from that location and report either **OK** or an error message. Note that even if an external database has been set up, the proxy will automatically fall back to its internal database in case the database is unreadable.

**Alias US stations to omit ICAO prefix K unless conflicted:** Multiple US stations have been observed to be online with callsigns that are not registered in the VAT-Spy database. The callsign is usually simply the FAA 3-letter code which can also be guessed by omitting the leading `K` from ICAO codes. As this is a commonly known and repeating issue, this option allows all ICAO codes starting with `K` to be aliased to a 3-letter code omitting the leading `K` which increases the chances of finding a location for such stations. Aliases will only be registered if no other station is known by the resulting shortened callsign. It is recommended to leave this option enabled.

**Locate observers by assuming callsign to indicate an ATC station:** Only if this option is enabled, ATC observers will be attempted to be located.

# Setting up clients

Before setting up a client please check if an updated version of the client has already been made available. It may not be necessary to use this proxy server.

If your wanted client is not listed here check the client settings if it allows to enter a data source URL (maybe already indicating `http://status.vatsim.net/`) and change it to point to your proxy server (by default: `http://localhost:8080/`). Restart the client if it does not seem to update and check if the proxy server reports any errors. Double-check with a browser on the same machine if the URL you specified delivers the "index" as seen on [http://status.vatsim.net/](http://status.vatsim.net/) and the URL listed as `url0` shows a CSV-like dump of the network status.

In addition to this description please refer to the GitHub discussions for [compatibility](https://github.com/dneuge/legacy-status-proxy-vatsim/discussions/categories/compatibility) for any questions or to describe how to set up additional clients.

## QuteScoop

Open QuteScoop's Preferences dialog. In the "Network" tab change "network" from "VATSIM" to "user defined network" and enter the URL to your proxy server (default: `http://localhost:8080/`). Re-enable "ATC bookings" or bookings will be missing. Restart QuteScoop to get rid of any cached information. The status line should display "User Network - today ....z: ... clients" and you should see all pilots and controllers as usual.

View the text of some ATIS station. If you do not see line breaks try changing the *encode data file in UTF-8 instead of ISO-8859-1* option as described above, restart the proxy server and reload the data in QuteScoop (F5).

## ServInfo

The status proxy needs to be configured as a network, do *not* configure anything in *Proxy settings*.

### as a custom network

Open *Options/Connection Parameters*. In section *Custom Network Settings* select a free slot such as *Custom Network 1*. Choose a *Network name* (for example `VATSIM Legacy`) and enter the proxy address as *Locator path url* (default: `http://localhost:8080/`). Confirm the settings with *OK*.

Use the icon with two networked computers to retrieve data through the proxy; do not click the VATSIM logo.

### as "VATSIM"

Close ServInfo and navigate to the application's folder. Open `servinfo.ini` with a text editor. Find the `VATSIM Servers Locator Url` setting and change the URL to your server address (default: `http://localhost:8080/`). Save the file, close the editor and start ServInfo.

Click the VATSIM logo to retrieve data through the proxy.

# Using the CLI (command-line)

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

# FAQ / Common issues

## General troubleshooting

When experiencing issues, a solution may be easily possible by:

- making sure you use the latest release version
- resetting the proxy configuration by renaming/moving or deleting the generated configuration file (default: `legacy-status-proxy-vatsim.properties` in same directory as the JAR file)

Support is primarily available through:

- [GitHub Discussions](https://github.com/dneuge/legacy-status-proxy-vatsim/discussions) (English preferred)
- [thread on main VATSIM forums](https://forums.vatsim.net/topic/31116-legacy-status-proxy-providing-data-feed-compatibility-to-passive-clients-not-migrated-to-json-yet/) (English)
- [thread on VATSIM Germany forums](https://board.vatsim-germany.org/threads/legacy-status-proxy-data-feed-kompatibilit%C3%A4t-f%C3%BCr-unmigrierte-passive-clients.66112/) (German)

## ATIS has no line breaks / free text is truncated or contains weird characters

**Cause:** Clients are not served in their expected character set.

Try to change the **encode data file in UTF-8 instead of ISO-8859-1** option and restart the server (see above for details). This will depend on the client you are using: VATSIM changed the character set in the last years of the old CSV-like data file format and some (pre-release versions of) clients had already been migrated, others not. Serving multiple clients at the same time may require a second server instance to work around this incompatibility.

## Some controllers are not visible or not shown correctly (e.g. missing labels in QuteScoop)

Check that you are running the latest version. Version 0.90 introduced a "Station Locator" to add estimated controller coordinates back to the legacy format (they are missing in the new JSON v3 format).

**Possible cause 1:** Station Locator has been deactivated or set up wrong.

Open *Configure/Station Locator* and check that *locate stations using VAT-Spy data* is enabled. Check if any options for the Station Locator have been set up in a way that could cause a disappearance of certain controllers from your client. Remember to restart the server after making any changes.

**Possible cause 2:** The client misses airspace data for affected controllers.

Check if there is updated airspace information for the affected client.

## Saving configuration is not possible (disabled)

**Cause:** The proxy was launched from a system directory, for example by using special functions such as "recent files" on Windows.

The configuration file is assumed to be located at the current working directory which may not be a valid location in some cases. Try to launch the proxy from the directory it is actually located in (i.e. navigate to the correct folder in Windows Explorer). You can also create a shortcut which allows you to choose the working directory. Alternatively, you can run the proxy with a specific configuration file path provided by command-line argument `--config FILE`.

## Server cannot be started

### Socket bind failure for socket / Address already in use

**Cause 1:** Another application already runs on the selected port.

Configure the proxy server to use another port or terminate or reconfigure the other application.

**Cause 2:** Another instance of the proxy server is already running.

Quit either instance.

**Cause 3:** The proxy server has recently been restarted.

Check if you are running the latest version (an issue causing that behaviour has been fixed in version 0.90).

It may still occasionally happen that proxy restarts in quick succession are not possible if clients have accessed the server recently. Wait a moment and try again; the port should become available again after one to four minutes. Check that any previously started instance of the proxy really has been terminated.

### Permission denied

**Cause:** You are not allowed to run a server on the selected port.

Configure the proxy to use another port or check your system permissions (e.g. ports below 1024 are reserved on Linux).

## Server responds with "Forbidden" (log: rejecting connection from...)

**Cause:** The affected client's IP address has not been allowed to access the server. By default, proxy service is limited to "localhost" (`127.0.0.1` or `::1`).

Check if the client should really be allowed to access the server and add the IP address exactly as printed on the server log to your configuration.

## Server responds with "Not authoritative" (HTTP status 421 Misdirected Request)

**Cause:** The affected client did not call the proxy server by its configured host name.

Check the local host name (default: `localhost`) in your server configuration. Choose a host name or IPv4 address that can be accessed by all wanted clients and only refer to that exact host name on all clients.

## An exception prevents the application from starting

**Possible cause:** The file path leading up to the JAR file contains a directory ending with an exclamation mark (`!`).

Java is unable to access such paths properly. Please rename the offending directory or move the JAR file to another location.

**Any other cause may exist.** Please run the proxy from command line to see all log output and debug information. That information may already contain a message indicating the reason for startup failure. If you are unable to resolve or think this may be a common issue please provide these details to the developer for analysis.
