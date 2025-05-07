## How to Run

This section describes how to build and run the ChatHu client application.

### Prerequisites

1.  **Java Development Kit (JDK):** Version 8 or higher. You can download it from [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html) or use an alternative like [OpenJDK](https://openjdk.java.net/).
2.  **Apache Maven:** To build the project and manage dependencies. You can download it from [maven.apache.org](https://maven.apache.org/download.cgi).
3.  **ChatHu Server:** The client application requires a running instance of the ChatHu server to connect to. Ensure the server is running and accessible from the machine where you plan to run the client. The client defaults to connecting to `127.0.0.1:2006`.

### Building the Application

1.  **Clone the Repository (if you haven't already):**
    ```bash
    git clone <your-repository-url>
    cd <your-repository-directory>
    ```

2.  **Compile the Project:**
    Navigate to the root directory of the project (where `pom.xml` is located) and run the following Maven command:
    ```bash
    mvn compile
    ```
    This command will download the necessary dependencies (FlatLaf for the UI and SQLite-JDBC for local message storage) and compile the source code.

3.  **(Optional) Package the Application:**
    To create a JAR file, you can run:
    ```bash
    mvn package
    ```
    This will create a JAR file (e.g., `chathu-client-1.0-SNAPSHOT.jar`) in the `target` directory. Note that this default JAR will not include dependencies. For a runnable JAR with dependencies, you would typically use a plugin like `maven-shade-plugin` or `maven-assembly-plugin`. For simplicity, the instructions below use `mvn exec:java`.

### Running the Application

Once the project is compiled, you can run the client application from the command line using Maven.

Open your terminal or command prompt, navigate to the project's root directory, and use the following command:

```bash
mvn exec:java -Dexec.mainClass="Client" -Dexec.args="[arguments]"
```

Replace `[arguments]` with any command-line options you wish to use (see below).

**Command-Line Arguments:**

The client accepts the following optional command-line arguments:

* `--user=<username>`: Specifies the username for the chat client.
    * Default: `User` followed by a random number (e.g., `User12345`).
* `--srv=<server_ip_address>`: Specifies the IP address of the ChatHu server.
    * Default: `127.0.0.1`
* `--port=<server_port>`: Specifies the port number of the ChatHu server.
    * Default: `2006`
## Testing the Application

To facilitate testing, especially for scenarios involving multiple users, you can run several instances of the ChatHu client simultaneously. Each client instance will represent a different user.

**Prerequisites for Testing:**

* Ensure the **ChatHu Server is running** and accessible. All client instances will connect to this server. By default, they will attempt to connect to `127.0.0.1:2006`. If your server is running on a different IP address or port, make sure to use the `--srv` and `--port` arguments for each client accordingly.
* You have already built the project (e.g., by running `mvn compile` in the project's root directory).

**Steps to Run Three Clients for Testing:**

You will need to open **three separate terminal windows** or command prompts. In each terminal, navigate to the root directory of the ChatHu client project (i.e., the directory containing the `pom.xml` file).

1.  **Terminal 1 - Launch Client for "Alice":**
    In the first terminal, run the following command:
    ```bash
    mvn exec:java -Dexec.mainClass="Client" -Dexec.args="--user=Alice"
    ```
    A client window will open for the user "Alice".

2.  **Terminal 2 - Launch Client for "Bob":**
    In the second terminal, run the following command:
    ```bash
    mvn exec:java -Dexec.mainClass="Client" -Dexec.args="--user=Bob"
    ```
    A new client window will open for the user "Bob".

3.  **Terminal 3 - Launch Client for "Charlie":**
    In the third terminal, run the following command:
    ```bash
    mvn exec:java -Dexec.mainClass="Client" -Dexec.args="--user=Charlie"
    ```
    A third client window will open for the user "Charlie".

**What to Expect and Test:**

* Three distinct ChatHu client GUI windows should now be open, each configured with the username specified (Alice, Bob, and Charlie).
* If the ChatHu server is running and accessible via `127.0.0.1:2006` (or the configured address/port), each client will attempt to connect automatically. You can also use the "Connect" button in the GUI if needed.
* Once connected, each client's "Online Users" list should update to show Alice, Bob, and Charlie.
* The tester can then proceed to:
    * Send direct messages between any two users (e.g., Alice to Bob, Bob to Charlie, Charlie to Alice).
    * Verify that messages are received correctly by the intended recipient.
    * Test the broadcast message functionality (if applicable, from any client).
    * Observe user join/left notifications in the chat or user list.
    * Close one client (e.g., Bob) and see if Alice and Charlie's user lists update correctly.
    * Check if chat history is being saved locally in the `chat_hu.db` file for each user's perspective.

**Notes for the Tester:**

* If you are using a different server IP or port than the default (`127.0.0.1:2006`), append the `--srv=<your_server_ip>` and `--port=<your_server_port>` arguments to each `mvn exec:java` command. For example:
    ```bash
    mvn exec:java -Dexec.mainClass="Client" -Dexec.args="--user=Alice --srv=192.168.0.100 --port=5000"
    ```
* Each client application manages its own P2P listening port (starting from `2009` and incrementing if the port is occupied). This process is automatic.
* The usernames "Alice", "Bob", and "Charlie" are examples. Any distinct usernames can be used.
### Local Database

The client uses an SQLite database (`chat_hu.db` by default, created in the directory where the application is run) to store chat history locally.
