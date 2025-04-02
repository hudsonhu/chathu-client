import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ConsoleListener implements Runnable {
    private Client client;
    private boolean keyboardInput;

    /**
     * Any input from the console will be sent to the server
     */
    public ConsoleListener(Client client, boolean keyboardInput) {
        this.client = client;
        this.keyboardInput = keyboardInput;
    }

    public void run() {
        try {
            while (keyboardInput) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String command = reader.readLine();
                client.send(command);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
