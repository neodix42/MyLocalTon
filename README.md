![Atomix](https://ton.org/download/ton_symbol.svg)

## What is it

This is your personal local TON blockchain (www.ton.org) in a shape of cross-platform desktop application. It comes in a form of uber-jar with all dependencies and binaries. Please notice this an
alpha version and cannot be treated as production ready.

![demo animation](https://github.com/neodiX42/MyLocalTon/blob/main/screens/MyLocalTon-alpha-demo.gif)

## Matrix of supported OS & Java

| OS \ Java   | 11  | 13  | 15  | 17  |
|---|---|---|---|---|
| Ubuntu 18.04  | :heavy_check_mark:   | :heavy_check_mark:   | :heavy_check_mark:   | :heavy_check_mark:  |
| Ubuntu 20.04  | :heavy_check_mark:   | :heavy_check_mark:   | :heavy_check_mark:   | :heavy_check_mark:    |  |
| Debian 11.2  | not tested   | not tested   | not tested   | :heavy_check_mark:    |  |
| MacOS 11.6.1 M1  | :heavy_check_mark:   | not tested   |  not tested | :heavy_check_mark:    |  |
| MacOS 12.01  | :heavy_check_mark:   | :heavy_check_mark:   |  not tested | :heavy_check_mark:    |  |
| MacOS 11.6  | :heavy_check_mark:   | :heavy_check_mark:   |  not tested | :heavy_check_mark:    |  |
| Windows 10  | :heavy_check_mark:   | :heavy_check_mark:   |  not tested | :heavy_check_mark:   |  |
| Windows Server 2019  | :heavy_check_mark:   | :heavy_check_mark:   | not tested  | :heavy_check_mark:    |  |

Please make sure you are not using headless (no GUI) Java and OS/Java combination matches as per table above.

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

Download MyLocalTon.jar from https://github.com/neodiX42/MyLocalTon/actions. Click the latest successful build and find MyLocalTon under Artifacts section. Open console, go to location where you have
just placed the executable and execute the following command.

`java -jar MyLocalTon.jar`

Make sure you have write rights inside the directory.

## Upgrade

Today upgrade is not supported, that means once you have a new version of MyLocalTon.jar just overwrite the existing MyLocalTon.jar and delete myLocalTon directory next to it. The upgrade
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

| :point_up:    | On Windows don't forget to install Microsoft Visual C++ Redistributable installation (see above) |
|---------------|:------------------------|