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
form of uber-jar with all dependencies and binaries. Please notice this an
alpha version and cannot be treated as production ready.

<img alt="MyLocalton gif demo" src='./screens/MyLocalTon-demo.gif'>

## Matrix of supported OS & Java

| OS \ Java           | 11                 | 13                 | 15                 | 17                 | 19                 |
|---------------------|--------------------|--------------------|--------------------|--------------------|--------------------|
| Linux x86_64        | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| Linux arm64/aarch64 | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| MacOS x86_64 (12+)  | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| MacOS arm64/aarch64 | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |
| Windows x86_64      | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: |

Please make sure you are not using headless (no GUI) Java and OS/Java combination matches as per table above.

### For MacOS users

In case you are using MacPorts instead of Homebrew on Mac please execute the following command:

`mkdir -p /usr/local/opt/readline/lib; ln -s /opt/local/lib/libreadline.8.dylib /usr/local/opt/readline/lib/`

## Java installation

If you are new to Java, please follow this guide on how to install OpenJDK 17:

- On Ubuntu
  https://techviewleo.com/install-java-openjdk-on-ubuntu-linux/
- On Windows
  https://java.tutorials24x7.com/blog/how-to-install-openjdk-17-on-windows
- On MacOS
  https://knasmueller.net/how-to-install-java-openjdk-17-on-macos-big-sur

In case you have several versions of Java use the following command in order to select the default Java version:

`sudo update-alternatives --config java`

## Microsoft Visual C++ Redistributable installation (for Windows only)

Please install Microsoft Visual C++ Redistributable 2015 (and above) x64.
https://docs.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist?view=msvc-170

## Installation and usage

Go to https://github.com/neodiX42/MyLocalTon/releases. Open Assets section and download MyLocalTon for your architecture

Open console and execute the following command:

`java -jar MyLocalTon-x86-64.jar` or
`java -jar MyLocalTon-arm64.jar`

## Upgrade

Today upgrade is not supported, that means once you have a new version of MyLocalTon just overwrite the existing
MyLocalTon and delete myLocalTon directory next to it. The upgrade
functionality is in the backlog, so it will be implemented in future releases.

## Build from sources

### Common actions for all platforms

* Install OpenJDK 11 or higher
* Install IntelliJ IDEA Community Edition 2019 or higher
* Install SceneBuilder from https://gluonhq.com/products/scene-builder/
* Clone this repository and open it in IntelliJ as Maven project.
* Click Add configuration, select new "Application" type and specify main class "org.ton.main.Main".
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
start pip3 install -U ton-http-api
```