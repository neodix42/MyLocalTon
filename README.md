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

<img src='./screens/MyLocalTon-demo.gif'>

## Matrix of supported OS & Java

| OS \ Java            | 11  | 13  | 15  | 17  | 19 |
|----------------------|---|---|---|---|---|
| Ubuntu 18.04         | :heavy_check_mark:   | :heavy_check_mark:   | :heavy_check_mark:   | :heavy_check_mark:  | not tested |
| Ubuntu 20.04         | :heavy_check_mark:   | :heavy_check_mark:   | :heavy_check_mark:   | :heavy_check_mark:    | not tested |
| Ubuntu 22.04         | not tested   | not tested   | not tested   | :heavy_check_mark:    | not tested |
| Ubuntu 18.04 aarch64 | not tested   | not tested   | not tested   | :heavy_check_mark:    | not tested |
| Ubuntu 20.04 aarch64 | not tested   | not tested   | not tested   | :heavy_check_mark:    | not tested |
| Ubuntu 22.04 aarch64 | not tested   | not tested   | not tested   | :heavy_check_mark:    | not tested |
| Debian 11.2          | not tested   | not tested   | not tested   | :heavy_check_mark:    | not tested |
| MacOS 11.6.1 M1      | :heavy_check_mark:   | not tested   |  not tested | :heavy_check_mark:    | not tested |
| MacOS 12.2.1 M1      | not tested   | not tested   |  not tested | :heavy_check_mark:    | not tested |
| MacOS 12.01          | :heavy_check_mark:   | :heavy_check_mark:   |  not tested | :heavy_check_mark:    | not tested |
| MacOS 11.6           | :heavy_check_mark:   | :heavy_check_mark:   |  not tested | :heavy_check_mark:    | not tested |
| Windows 10           | :heavy_check_mark:   | :heavy_check_mark:   |  not tested | :heavy_check_mark:   | :heavy_check_mark: |
| Windows Server 2019  | :heavy_check_mark:   | :heavy_check_mark:   | not tested  | :heavy_check_mark:    | :heavy_check_mark: |

Please make sure you are not using headless (no GUI) Java and OS/Java combination matches as per table above.

### For MacOS users

In case you are using MacPorts instead of Homebrew on Mac please execute the following command:

`mkdir -p /usr/local/opt/readline/lib; ln -s /opt/local/lib/libreadline.8.dylib /usr/local/opt/readline/lib/`

Also please make sure you have OpenSSL installed. You can do it as follows:

`brew install openssl`

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

### For x86_64 architecture

Go to https://github.com/neodiX42/MyLocalTon/releases. Open Assets section and download MyLocalTon.jar.

Open console and execute the following command:

`java -jar MyLocalTon.jar`

### For aarch64/arm64 architecture (MacOS M1 and Linux)

Go to https://github.com/neodiX42/MyLocalTon/releases and select version with `arm64-aarch64` prefix.

## Upgrade

Today upgrade is not supported, that means once you have a new version of MyLocalTon.jar just overwrite the existing
MyLocalTon.jar and delete myLocalTon directory next to it. The upgrade
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

#### JavaFX 17 is not supported any longer, please install JavaFX 19

* **Install JavaFX 19**

1. Apparently JavaFX 17 is no longer available for download.
2. Download x64 JavaFX 19 Windows SDK from [here](https://gluonhq.com/products/javafx/)
3. Select Windows for the Operating System
4. Select x64 for the Architecture
5. Select SDK for the Type
6. Copy the javafx-sdk-191 folder from the .zip file to C:\Program Files\Java\

* **Set Project SDK to Java 17**

1. Go to File → New Projects Setup → Structure
2. Select Project under Project Settings on the left side
3. Set SDK to 17 (if available); otherwise:
4. Click Add SDK
5. Select JDK and browse to C:\Program Files\Java\jdk-17.0.52
6. Under Project language level select SDK default (17 - Sealed types, always-strict floating-point semantics)
7. Click OK

* **Set Project Global Library**

1. Go to File → New Projects Setup → Structure
2. Select Global Libraries under Platform Settings on the left side
3. Click the + (plus symbol) in the upper left of the second column
4. Select Java from the menu
5. Browse to C:\Program Files\Java\javafx-sdk-19\lib (adjust to match version)
6. Click OK twice

* **Set Run Configuration Template for Application**

1. Go to File → New Projects Setup → Run Configuration Templates...
2. Select Application
3. Click Modify options (or type Alt+M) and ensure Add VM options is checked
4. Paste the following in VM options:

```
   --module-path "C:\Program Files\Java\javafx-sdk-19\lib"
   --add-modules=javafx.controls,javafx.graphics,javafx.fxml,javafx.media,javafx.web,javafx.swing,javafx.base,javafx.web
   --add-reads javafx.graphics=ALL-UNNAMED
   --add-reads javafx.controls=ALL-UNNAMED
   --add-opens javafx.controls/com.sun.javafx.charts=ALL-UNNAMED
   --add-opens javafx.graphics/com.sun.javafx.iio=ALL-UNNAMED
   --add-opens javafx.graphics/com.sun.javafx.iio.common=ALL-UNNAMED
   --add-opens javafx.graphics/com.sun.javafx.css=ALL-UNNAMED
   --add-opens javafx.base/com.sun.javafx.runtime=ALL-UNNAMED
   --add-opens javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED
   --add-opens javafx.controls/com.sun.javafx.scene.control.behavior=ALL-UNNAMED
   --add-opens javafx.controls/com.sun.javafx.scene.control.LambdaMultiplePropertyChangeListenerHandler=ALL-UNNAMED
   --add-opens java.base/java.lang.reflect=ALL-UNNAMED
```

6. Here too, ensure that the version number matches what you downloaded
7. Click OK

| :point_up:    | On Windows don't forget to install Microsoft Visual C++ Redistributable installation (see above) |
|---------------|:------------------------|