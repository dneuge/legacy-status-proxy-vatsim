# Legacy status proxy for VATSIM

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE.md)

This project provides a simple HTTP proxy server translating current VATSIM status file formats to outdated legacy formats to provide compatibility to monitor applications.

## Disclaimer and Intended Use

*This is a copy of [disclaimer.txt](src/main/resources/de/energiequant/vatsim/compatibility/legacyproxy/disclaimer.txt). The disclaimer has to be accepted before the server can be started.*

This disclaimer is a more application-specific addition to the general points stated in the [MIT license](LICENSE.md).

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

## Current State

- legacy Whazzup data files can be generated from JSON v3 data files
- network information ("URL index") is combined from legacy and JSON formats

## How to configure and run

TODO

## Compilation

### Dependencies

Some dependencies have no stable release version and thus are currently not available from Maven central, so they have to be installed locally:

- [Web Data Retrieval](https://github.com/dneuge/web-data-retrieval)
- VATPlanner's [VATSIM Public Data Formats](https://github.com/vatplanner/dataformats-vatsim-public) library

### Compilation

Make sure all dependencies have been installed locally in Maven.

Run `mvn clean install assembly:single` to compile, locally install the status proxy and generate a JAR file containing all dependencies.

## License

The implementation and accompanying files are released under [MIT license](LICENSE.md). Dependencies are subject to their individual licenses.

All processed and served data is subject to policies and restrictions set by VATSIM and your local regulations. See section *Disclaimer and Intended Use* for more details.
