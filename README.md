# Legacy status proxy for VATSIM

[![Build Status](https://app.travis-ci.com/dneuge/legacy-status-proxy-vatsim.svg?branch=master)](https://app.travis-ci.com/dneuge/legacy-status-proxy-vatsim)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE.md)

This project provides a simple HTTP proxy server translating current VATSIM status file formats to outdated legacy formats to provide compatibility to monitor applications.

Click [here](https://github.com/dneuge/legacy-status-proxy-vatsim/releases/latest/download/legacy-status-proxy-vatsim.jar) to download the latest released version.

Please read the [user manual](docs/manual.md) for a detailed explanation on how to set up and run the proxy server. An FAQ for the common troubleshooting is also available from the manual.

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
  - coordinates missing in JSON files are by default computed from [VAT-Spy data](https://github.com/vatsimnetwork/vatspy-data-project) and [online transceivers](https://api.vatsim.dev/#operation/TransceiverData)
- network information ("URL index") is combined from legacy and JSON formats

## Known limitations

- providing service via IPv6 requires a host name (i.e. just an IPv6 address cannot be used as a local host name)
- non-ASCII characters on served data may be wrong
- some information may have been removed from recent formats and may be replaced by placeholders
- some information may only be available in recent formats (e.g. pilot ratings); no compatibility is provided for such data
- HTTP server listens on all network interfaces, even if only localhost is served (see [#6](https://github.com/dneuge/legacy-status-proxy-vatsim/issues/6))

## How to configure and run

The proxy server requires Java between versions 1.8 and 11 and can be used as a GUI desktop application or via command line (CLI). You can use `--no-gui` to enforce the CLI version on a desktop system.

For further details including an FAQ please refer to the [user manual](docs/manual.md).

## Development

If you are interested in developing the proxy, please refer to the [development notes](docs/development.md) for details. Compilation requires installation of unpublished Maven artifacts, which is described in further detail.

## License

The implementation and accompanying files (not including copies of license texts themselves) are released under [MIT license](LICENSE.md). Dependencies are subject to their individual licenses. Binary redistribution is subject to the combination of all licenses. See the disclaimer for more information.

All processed and served data is subject to policies and restrictions set by VATSIM and your local regulations. See section *Disclaimer and Intended Use* for more details.
