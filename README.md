[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Telegram Foundation Group][telegram-foundation-badge]][telegram-foundation-url]
[![Based on TON][ton-svg]][ton]
![GitHub last commit](https://img.shields.io/github/last-commit/neodiX42/myLocalTon)

[telegram-foundation-url]: https://t.me/tonblockchain

[telegram-foundation-badge]: https://img.shields.io/badge/-TON%20Foundation-2CA5E0?style=flat&logo=telegram&logoColor=white

[ton-svg]: https://img.shields.io/badge/Based%20on-TON-blue

[ton]: https://ton.org

## What is it

This is your personal local TON blockchain (www.ton.org) in a shape of cross-platform desktop application. It comes in a
form of uber-jar with all dependencies and binaries.

<img alt="MyLocalton gif demo" src='./screens/MyLocalTon-demo.gif'>

## Supported OS & Java

| OS \ Java           | 21 and higher      | 
|---------------------|--------------------|
| Linux x86_64        | :heavy_check_mark: | 
| Linux arm64/aarch64 | :heavy_check_mark: |
| MacOS x86_64 (12+)  | :heavy_check_mark: |
| MacOS arm64/aarch64 | :heavy_check_mark: |
| Windows x86_64      | :heavy_check_mark: |

## Quick Java 21 installation

| Linux                                 | MacOS                         | Windows                                      |
|---------------------------------------|-------------------------------|----------------------------------------------|
| ```sudo apt install openjdk-21-jdk``` | ```brew install openjdk@21``` | ```choco install openjdk --version=21.0.2``` |

### For MacOS users

In case you are using MacPorts instead of Homebrew on Mac please execute the following command:

`mkdir -p /usr/local/opt/readline/lib; ln -s /opt/local/lib/libreadline.8.dylib /usr/local/opt/readline/lib/`

## Microsoft Visual C++ Redistributable installation (for Windows only)

Please install Microsoft Visual C++ Redistributable 2015 (and above) x64.
https://docs.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170

## MyLocalTon installation

| Archicture | Linux / MacOS                                                                                    | Windows                                                                                           |
|------------|--------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| x86-64     | ```wget https://github.com/neodix42/MyLocalTon/releases/latest/download/MyLocalTon-x86-64.jar``` | [download](https://github.com/neodix42/MyLocalTon/releases/latest/download/MyLocalTon-x86-64.jar) |
| arm64      | ```wget https://github.com/neodix42/MyLocalTon/releases/latest/download/MyLocalTon-arm64.jar```  | [download](https://github.com/neodix42/MyLocalTon/releases/latest/download/MyLocalTon-arm64.jar)  |

## MyLocalTon usage

`java -jar MyLocalTon-x86-64.jar [nogui] [ton-http-api] [explorer] [ip.addr.xxx.xxx] [test-binaries] [with-validators-N] [custom-binaries=] [debug]`

for ARM64 architecture use:

`java -jar MyLocalTon-arm64.jar`

### Parameters

* `nogui` - used to run MyLocalTon without GUI interface. Can be used in Docker or server with headless Java.
* `ton-http-api` - enables ton-http-api service on start. Runs on port `8081`.
* `explorer` - enables native ton blockchain explorer on start. Runs on port `8001`.
* `ip.addr.xxx.xxx` - used to bind specific IP to MyLocalTon instead of 127.0.0.1.
* `with-validators-N` - used to start MyLocalTon with N additional validators.
* `elections=int,int,int,int,int` - used to start MyLocalTon with specific election timing. 5 integers represent these parameters in seconds: **electedFor**, **electionStartBefore**, **electionEndBefore**, **electionStakesFrozenFor** and the last one - **how often to participate in elections**.
* `custom-binaries=absolute-path` - used to start MyLocalTon with custom TON binaries. The folder should contain
  validator-engine, validator-engine-console, lite-client, fift, func, generate-random-id, create-state, dht-server,
  tonlibjson, blockchain-explorer binaries and also **smartcont** and lib **folders** in its root folder.
* `debug` - used to start MyLocalTon in debug mode, that produces lots of useful log files.
* `data-generator` - enabling data-generator on start, more
  info [here](https://github.com/neodix42/mylocalton-docker/wiki/Data-(traffic-generation)-container).
* `version` - simply returns current version of MyLocalTon.

### HTTP server
By default, MyLocalTon starts with a simple HTTP server, that serves the following endpoints:

| Endpoint                                           | Description                                                                                                                       |
|----------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------|
| http://localhost:8000/localhost.global.config.json | Serves the TON configuration file, that can be used in various SDK libraries, like tonlib or ton4j.                               |
| http://localhost:8000/live                         | Returns OK when blockchain is ready.                                                                                              |
| http://localhost:8000/add-validator                | Add node that starts participating in elections. Can be added up to 5 validators.  Supports GET parameter participate=true/false. |

###

MyLocalTon uses wallets with predefined private keys. For the whole list refer
to [this documentation](https://github.com/neodix42/mylocalton-docker?tab=readme-ov-file#pre-installed-wallets)

### Download global.config.json

After successful launch `localhost.global.config.json` will be available at
`http://127.0.0.1:8000/localhost.global.config.json`

### Lite-client

MyLocalTon uses deterministic (permanent) private and public keys for lite-server and validator-engine-console access.
These keys can be found [here](./src/main/resources/org/ton/certs).
Once MyLocalTon is ready, you can use lite-client with the base64 key:

`lite-client -a 127.0.0.1:4443 -b E7XwFSQzNkcRepUC23J2nRpASXpnsEKmyyHYV4u/FZY= -c last`

or validator-engine-console in this way:

`validator-engine-console -a 127.0.0.1:4441 -k <absolute-path>/myLocalTon/genesis/bin/certs/client -p <absolute-path>/myLocalTon/genesis/bin/certs/server.pub`

### Log files

* **MyLocalTon** log files can be found under `./myLocalTon/MyLocalTon.log`.
* **validator-engine** log files can be found under `./myLocalTon/genesis/db/log`.

### Reporting an issue

* delete the current folder .`/myLocalTon` next to MyLocalTon*.jar
* execute `java -jar MyLocalTon*.jar debug`. (`*` replace with architecture and branch if needed)
* wait till the error appears and shutdown MyLocalTon.
* then send zipped above log files to Telegram @neodix or attach to issue at GitHub.
* Thanks for reporting. You are making MyLocalTon better.

## MyLocalTon inside Docker

It is not optimal to put this Java version of MyLocalTon to Docker container.
Please refer to this project https://github.com/neodix42/mylocalton-docker in order to have more optimized and stable
MyLocalTon inside Docker.

## Upgrade

Today upgrade is not supported, that means once you have a new version of MyLocalTon just overwrite the existing
MyLocalTon and delete myLocalTon directory next to it. The upgrade
functionality is in the backlog, so it will be implemented in future releases.

## Build from sources

### Common actions for all platforms

* Install OpenJDK 21 or higher
* Install IntelliJ IDEA Community Edition 2019 or higher
* Install SceneBuilder from https://gluonhq.com/products/scene-builder/
* Clone this repository and open it in IntelliJ as Maven project.
* Click Add configuration, select new "Application" type and specify main class "org.ton.mylocalton.main.Main".
* In Settings - JavaFX specify path to SceneBuilder.
* Now you can compile and run the application from IntelliJ.

| :point_up: | On Windows don't forget to install Microsoft Visual C++ Redistributable installation (see above) |
|------------|:-------------------------------------------------------------------------------------------------|

## Manual TON-HTTP-API installation (optional)

### Linux

```commandline
sudo apt install -y python3
sudo apt install -y python3-pip
pip3 install --user ton-http-api
```

### MacOS

Note: Python version must be 3.11 or greater

```commandline
brew install -q python3
python3 -m ensurepip --upgrade
pip3 install --user ton-http-api
```

### Windows

```commandline
wget https://www.python.org/ftp/python/3.12.0/python-3.12.0-amd64.exe
python -m ensurepip --upgrade
pip install uvicorn[standard]
start pip3 install -U ton-http-api
```

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=neodix42/MyLocalTon&type=Date)](https://star-history.com/#neodix42/mylocalton&Date)
