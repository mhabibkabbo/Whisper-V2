# WHISPER 💬

**Whisper** is a JavaFX-based real-time chat application built using socket programming and a multi-threaded client-server architecture.

It currently works over a Local Area Network (LAN) and can be extended to a Wide Area Network (WAN) using NAT port forwarding or a public TCP server.

---

## Features

- Real-time messaging  
- Multi-user support  
- Voice call functionality  
- Group chat support  
- SQLite database integration  

---

## Tech Stack

- Java (JDK 21)
- JavaFX SDK 21
- Socket Programming
- SQLite
- Gradle

---

## Installation & Setup

**This installation process is for Windows Users! If you are not a windows user, you can look up on the internet for the equivalent command according to your operating system.**

### Prerequisites

Make sure you have installed:

- Java JDK 21  
- JavaFX SDK 21 → https://openjfx.io  
- Gradle  
- IntelliJ IDEA  

---

### Step 1: Clone the Repository
```bash
git clone https://github.com/mhabibkabbo/Whisper-V2.git
cd Whisper
```
---

### Step 2: Configure IntelliJ IDEA

Add JavaFX SDK:
```bash
File → Project Structure → Libraries → '+' → Select JavaFX SDK
```
Add SQLite JDBC driver:
```bash
File → Project Structure → Libraries → '+' → Select sqlite-jdbc-3.51.1.1.jar
```
Add VM Options (Run → Edit Configurations):
```bash
--module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml,javafx.base,javafx.media
```

---

### Step 3: Configure Server (Host Machine)

Run the following command:
```bash
ipconfig
```
Copy your IPv4 Address.

Open NetworkConfig.java and replace:
```bash
public static final String SERVER_HOST =
System.getProperty("whisper.server.host", "localhost");
```
with:
```bash
public static final String SERVER_HOST =
System.getProperty("whisper.server.host", "YOUR_IPV4_ADDRESS");
```
If you don't have JAVA_HOME environment variable added, add this before running the commands.
```bash
JAVA_HOME: /path/to/jdk/directory
```
Example path: ``C:\Users\<username>\.jdks\ms-21.0.10``

---
Make sure that JAVA_HOME variable is set by using the following command :
```bash
echo %JAVA_HOME%
```

### Step 4: Build the Application

Run these commands sequentially where **build.gradle** file is located:
- To clean the previous build!
```bash
.\gradlew clean
```
- To build the current project
```bash
.\gradlew jpackage
```
---
### Step 5: Install WixToolSet
- Install **wix314.exe** provided in the main folder. 
### Step 6: Run the Application

- Start the **SERVER**:\
Run **Server.java** from **Server/src**
- Start the **CLIENT**:\
Go to **build/jpackage/Whisper/App** and run **Whisper.jar** or **Whisper.exe**. 

IMPORTANT: Start the server before running the client. Also do not use the installer provided in the **build/jpackage** folder.

---

## Running on Multiple Devices

- Ensure all devices are on the same network
- Use the host machine's IPv4 address
- Share the jpackage folder with clients
- Clients run the .jar file

---

## WAN Deployment (Optional)

- Enable port forwarding on your router  
- OR use a public server  
- Update SERVER_HOST to your public IP  

---

## Future Improvements

- End-to-end encryption  
- User authentication  
- Cloud database  
- Full WAN support  

---

## License
Educational and personal use.

---

## Contributors
**Md. Mishkatul Habib**
\
Undergrad Student\
CSE-24, BUET\
\
**Yeasin Anzam Rifat**
\
Undergrad Student\
CSE-24, BUET

## Support
If you like this project, give it a star on GitHub!
