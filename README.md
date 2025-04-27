# ChatHu Client – Quick Start

This guide only covers **setting up the local environment and running the
client**. Packaging / installers are not included.

---

## 1  Prerequisites

| Tool   | Minimum version | Purpose |
|--------|-----------------|---------|
| JDK    | 1.8             | compile & run |
| Maven 3 | 3.8+            | fetch FlatLaf and sqlite-jdbc automatically |

> **Tip:** Any IDE that supports Maven (IntelliJ IDEA, VS Code, Eclipse) works out of the box—just open the project.

---

## 2  Clone & prepare

```bash
git clone https://github.com/hudsonhu/chathu-client.git
cd chathu-client
```

Maven will download external libraries on first run; no extra setup needed.

---

## 3  Run the client

### 3-a From the command line

```bash
mvn -q exec:java                                    \
    -Dexec.mainClass=Client                        \
    -Dexec.args="--user=Alice --srv=127.0.0.1 --port=2006"
```

### 3-b From an IDE

1. Import the project as a Maven project.
2. Add **Client** as the main class in a Run/Debug configuration.
3. Optional program arguments:

```
--user=<name>   # login name (default: User<random>)
--srv=<ip>      # server IP (default: 127.0.0.1)
--port=<num>    # server port (default: 2006)
```

You can omit any flag to use its default value.

---

## 4  What happens next?

* On first run a local SQLite file **chat_hu.db** is created in the working directory.
* A Swing window opens. Click **Connect** (or use auto-connect via flags) to join the server.
* Messages and chat history are stored locally.

That’s all you need to get the client up and running.
