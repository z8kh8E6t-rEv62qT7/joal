# Disclamer
JOAL is not designed to help or encourage you downloading illegal materials ! You must respect the law applicable in your country. I couldn't be held responsible for illegal activities performed by your usage of JOAL.

## Official Docker Hub page:
https://hub.docker.com/r/anthonyraymond/joal

# JOAL
This is the server application (with an **optional** webui), if you are interested in the desktop app look at [here](https://github.com/anthonyraymond/joal-desktop).

### Which client can JOAL emulate?

| Client        | Support                       | Comment        |  | Client        | Support                       | Comment        |
| ------------- |:-----------------------------:|----------------|--|---------------|:-----------------------------:|----------------|
| BitComet      | ![Numwant mess][support-never]| Will never be !|  | Transmission  | ![Yes][support-yes]           |                |
| BitTorrent    | ![Yes][support-yes]           |                |  | µTorrent      | ![Yes][support-yes]           |                |
| Deluge        | ![Yes][support-yes]           |                |  | Vuze Azureus  | ![Yes][support-yes]           |                |
| qBittorrent   | ![Yes][support-yes]           |                |  | Vuze Leap     | ![Yes][support-yes]           |                |
| rTorrent      | ![Yes][support-yes]           |                |  |

If your favorite client is not yet supported feel free to ask (except for BitComet).<br/>
Ask for it in GitHub issues or mail <a href="mailto:joal.contact@gmail.com">joal.contact@gmail.com</a>.

## Preview
![preview](readme-assets/webui-preview.png?raw=true)


# HOW TO USE
## 1. Setting up configuration
In the folder of your choice (ie: /home/anthony/joal-conf), download the [latest tar.gz release](https://github.com/anthonyraymond/joal/releases/latest) and extract `config.json` `clients` and `torrents`, this folder will be our `joal-conf`.

It must look similar to this:<br/>
![joal-conf][joal-conf-folder]

## Build and run a macOS ARM64 native executable

This checkout targets **Java 21 and Spring Boot 3.5.16**. Install a GraalVM distribution for Java 21, Maven 3.6.3 or later, and the Xcode command-line tools. On a Mac with GraalVM installed in the standard location:

```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean test
mvn -Pnative native:compile
```

The executable is `target/jack-of-all-trades`. It runs directly, without `java -jar`, on macOS ARM64. It is not a Linux or Intel Mac executable. The build leaves the existing `build/` directory untouched. A JVM JAR can also be built with `mvn package`.

Log4j2 is pinned to 2.25.5 because it supplies GraalVM metadata missing from the version managed by Boot 3.5. JOAL adds metadata for its JSON models, UI resources, and Boot's logging plugins. Adding a new polymorphic JSON implementation or outgoing message requires updating `JoalRuntimeHints` and exercising that path in the executable.

Use an existing configuration directory containing `config.json`, `clients/`, and `torrents/`:

```sh
# Background mode: no HTTP listener and no UI credentials required.
./target/jack-of-all-trades --joal-conf="/absolute/path/to/joal-conf"

# The same executable with Web UI enabled.
./target/jack-of-all-trades \
  --joal-conf="/absolute/path/to/joal-conf" \
  --spring.main.web-environment=true \
  --server.port=8088 \
  --joal.ui.path.prefix=YourSecretPath123 \
  --joal.ui.secret-token=YourSecretToken
```

Open `http://localhost:8088/YourSecretPath123/ui/`. The UI flag is read at process startup, defaults to false, and controls HTTP listening while retaining one AOT-compatible Spring Web context. Changing the flag, port, configuration directory, prefix, or token requires a restart, not a rebuild. When the UI is disabled, setting `server.port` does not enable HTTP. The BitTorrent peer listener continues to operate independently.

An enabled UI requires a nonempty alphanumeric prefix and a nonblank token. Requests outside the prefix receive an empty 404 response; authorization denies other unexposed routes. Missing or malformed startup configuration produces a nonzero exit. Stop with Ctrl-C for graceful cleanup.

Console logging uses blue timestamps, severity-colored levels, magenta thread names and cyan logger names; message text retains the terminal's default color. Tomcat's JUL logs are routed through the same Log4j2 appender. The bundled `log4j2.xml` emits ANSI colors directly, so no additional startup flag is required. To customize the format without rebuilding, supply an external Log4j2 XML file using `--logging.config=/absolute/path/to/log4j2.xml`.

For a native process, pass proxy system properties directly instead of using `JAVA_TOOL_OPTIONS`:

```sh
./target/jack-of-all-trades -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=8888 \
  --joal-conf="/absolute/path/to/joal-conf"
```

### Executable acceptance tests

After building, run the black-box suite against the actual artifact:

```sh
mvn -Dtest=ApplicationProcessTest -Djoal.test.application=target/jack-of-all-trades test
# Optionally run the same suite against the JVM JAR:
mvn -Dtest=ApplicationProcessTest -Djoal.test.application=target/jack-of-all-trades-2.1.38-SNAPSHOT.jar test
```

The suite creates temporary configuration directories, exercises bundled client files, UI access, STOMP authentication and JSON messages, configuration writes, local HTTP/HTTPS tracker announcements, a runtime TLS trust store, file watching, invalid inputs, and shutdown. Outbound HTTP requests terminate at a local test proxy; no real trackers or personal seed directories are used. Without `joal.test.application`, these process tests are skipped by the normal unit-test run.

**Docker and CI are outside this native adaptation:** their existing Java 11 configuration has not been migrated and is not compatible with this checkout's Java 21 requirement. The Docker examples below describe the upstream distribution.

## 2. Run with Java

```
java -jar ./jack-of-all-trades-X.X.X.jar --joal-conf="PATH_TO_CONF"
```

- `--joal-conf=PATH_TO_CONF` is a **required** argument: path to the joal-conf folder (ie: /home/anthony/joal-conf).

<br />
By default the web-ui is disabled, you can enable it with some more arguments:

- `--spring.main.web-environment=true`: to enable the web context.
- `--server.port=YOUR_PORT`: the port to be used for both HTTP and WebSocket connection.
- `--joal.ui.path.prefix="SECRET_OBFUSCATION_PATH"`: use your own complicated path here (this will be your first layer of security to keep joal secret). This is security though obscurity, but it is required in our case.  *This must contains only alphanumeric characters (no slash, backslash, or any other non-alphanum char)*
- `--joal.ui.secret-token="SECRET_TOKEN"`: use your own secret token here (this is some kind of a password, choose a complicated one).

Once joal is started head to: `http://localhost:port/SECRET_OBFUSCATION_PATH/ui/` (obviously, replace `SECRET_OBFUSCATION_PATH`) by the value you had chosen
The `joal.ui.path.prefix` might seems useless but it's actually **crucial** to set it as complex as possible to prevent people to know that joal is running on your server.

If you want to use iframe you may also pass the `joal.iframe.enabled=true` argument. If you don't known what that is just ignore it.

## 2. Run with Docker

In next command you have to replace `PATH_TO_CONF`, `PORT`, `SECRET_OBFUSCATION_PATH` and `SECRET_TOKEN` with your desired values.
```
docker run -d \
    -p PORT:PORT \
    -v PATH_TO_CONF:/data \
    --name="joal" \
    anthonyraymond/joal:X.X.X \
    --joal-conf="/data" \
    --spring.main.web-environment=true \
    --server.port="PORT" \
    --joal.ui.path.prefix="SECRET_OBFUSCATION_PATH" \
    --joal.ui.secret-token="SECRET_TOKEN"
```
Or the equivalent docker-compose service.
```
version: "2"
services:
  joal:
    image: anthonyraymond/joal:X.X.X
    container_name: joal
    restart: unless-stopped
    volumes:
      - PATH_TO_CONF:/data
    ports:
      - PORT:PORT
    command: ["--joal-conf=/data", "--spring.main.web-environment=true", "--server.port=PORT", "--joal.ui.path.prefix=SECRET_OBFUSCATION_PATH", "--joal.ui.secret-token=SECRET_TOKEN"]
```

Replace the `X.X.X` in `anthonyraymond/joal:X.X.X` with the desired version of joal (all versions are available [here](https://hub.docker.com/r/anthonyraymond/joal/tags)).


## 3. Start seeding
Just add some `.torrent` files to the `joal-conf/torrents` folder. There is no need to restart JOAL to add more torrents, add it to the folder and JOAL will be aware of after few seconds.

If WebUi is enabled you can also drag and drop torrents in the joal ui.


## Configuration file
### Application configuration
The application configuration belongs in `joal-conf/config.json`.

```
{
  "minUploadRate" : 30,
  "maxUploadRate" : 160,
  "simultaneousSeed" : 20,
  "client" : "qbittorrent-3.3.16.client",
  "keepTorrentWithZeroLeechers" : true,
  "uploadRatioTarget": -1.0
}
```
- `minUploadRate` : The minimum uploadRate you want to fake (in kB/s) (**required**)
- `maxUploadRate` : The maximum uploadRate you want to fake (in kB/s) (**required**)
- `simultaneousSeed` : How many torrents should be seeding at the same time (**required**)
- `client` : The name of the .client file to use in `joal-conf/clients/` (**required**)
- `keepTorrentWithZeroLeechers`: should JOAL keep torrent with no leechers or seeders. If yes, torrent with no peers will be seed at 0kB/s. If false torrents will be deleted on 0 peers reached. (**required**)
- `uploadRatioTarget`: when JOAL has uploaded X times the size of the torrent **in a single session**, the torrent is removed. If -1.0 torrents are never removed.

## Proxy Configuration

You can route JOAL traffic (tracker announcements and IP checks) through an HTTP/HTTPS proxy. This is particularly useful if you run JOAL behind a VPN container (like Gluetun) or want to hide your real IP address.

To enable the proxy, set the `JAVA_TOOL_OPTIONS` environment variable with standard Java proxy properties.

### Docker Run Example

```bash
docker run -d \
    -p PORT:PORT \
    -v PATH_TO_CONF:/data \
    -e "JAVA_TOOL_OPTIONS=-Dhttp.proxyHost=10.10.10.10 -Dhttp.proxyPort=8888 -Dhttp.nonProxyHosts=localhost|127.*" \
    --name="joal" \
    anthonyraymond/joal:X.X.X \
    --joal-conf="/data" \
    --spring.main.web-environment=true \
    --server.port="PORT" \
    --joal.ui.path.prefix="SECRET_OBFUSCATION_PATH" \
    --joal.ui.secret-token="SECRET_TOKEN" \
```

### Docker Compose Example
```yaml
version: "2"
services:
  joal:
    image: anthonyraymond/joal:X.X.X
    container_name: joal
    restart: unless-stopped
    environment:
      # Configure the Proxy Host and Port here
      # Important: You MUST configure 'http.nonProxyHosts' to exclude localhost, 
      # otherwise the internal Web UI might become unreachable.
      - JAVA_TOOL_OPTIONS=-Dhttp.proxyHost=10.10.10.10 -Dhttp.proxyPort=8888 -Dhttp.nonProxyHosts="localhost|127.*|10.*|192.168.*"
    volumes:
      - PATH_TO_CONF:/data
    ports:
      - PORT:PORT
    command: ["--joal-conf=/data", "--spring.main.web-environment=true", "--server.port=PORT", "--joal.ui.path.prefix=SECRET_OBFUSCATION_PATH", "--joal.ui.secret-token=SECRET_TOKEN"]
```
### Supported Properties

* `-Dhttp.proxyHost`: The hostname or IP address of your proxy server.
* `-Dhttp.proxyPort`: The port of your proxy server.
* `-Dhttp.nonProxyHosts`: A pipe-separated list (`|`) of hosts that should be reached directly (bypassing the proxy). **It is highly recommended to include `localhost` and `127.*` to ensure the Web UI works correctly.**


## Supported browser (for web-ui)
| Client                              | Support                 | Comment                                              |
| ----------------------------------- |:-----------------------:|------------------------------------------------------|
| ![Google Chrome][browser-chrome]    | ![yes][support-yes]     |                                                      |
| ![Mozilla Firefox][browser-firefox] | ![yes][support-yes]     |                                                      |
| ![Opera][browser-opera]             | ![yes][support-yes]     |                                                      |
| ![Opera mini][browser-opera-mini]   | ![no][support-no]       | Lack of `referrer-policy` & No support for WebSocket |
| ![Safari][browser-safari]           | ![no][support-no]       | Lack of `referrer-policy`                            |
| ![Edge][browser-edge]               | ![no][support-no]       | Lack of `referrer-policy`                            |
| ![Internet explorer][browser-ie]    | ![no][support-never]    | Not enough space to explain...                       |

Some non-supported browser might work, but they may be unsafe due to the lack of support for `referrer-policy`.


## Community projects
Those projects are maintained by their individual authors, if you have any question on how to use it use the corresponding repository to ask questions. I do not offer any support nor responsability for these projets. But i want a say a special **thanks** to them for speinding some time on this project.
- [Addon for Home Assistant](https://github.com/alexbelgium/hassio-addons/tree/master/joal) by [alexbelgium](https://github.com/alexbelgium)
- [Ansible role](https://github.com/slundi/ansible-joal) by [slundi](https://github.com/slundi)


# Thanks:
This project use a modified version of the awesome [mpetazzoni/ttorrent](http://mpetazzoni.github.com/ttorrent/) library. Thanks to **mpetazzoni** for this.
Also this project has benefited from the help of several people, see [Thanks.md](THANKS.md)

## Supporters
[![Thanks for providing Jetbrain license](readme-assets/jetbrains.svg)](https://www.jetbrains.com/?from=joal)



[support-never]:readme-assets/warning.png
[support-no]:readme-assets/cross-mark.png
[support-yes]:readme-assets/check-mark.png
[joal-conf-folder]:readme-assets/joal-conf-folder.png
[browser-chrome]:readme-assets/browsers/chrome.png
[browser-firefox]:readme-assets/browsers/firefox.png
[browser-opera]:readme-assets/browsers/opera.png
[browser-opera-mini]:readme-assets/browsers/opera-mini.png
[browser-safari]:readme-assets/browsers/safari.png
[browser-ie]:readme-assets/browsers/ie.png
[browser-edge]:readme-assets/browsers/edge.png
[jetbrain-logo]:readme-assets/jetbrains.svg
