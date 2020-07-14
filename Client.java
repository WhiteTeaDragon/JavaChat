import java.io.*;
import java.net.Socket;
import java.net.InetAddress;
import java.util.Date;
import message.Message;

public class Client {
    private final static String commandChangeName = "change_my_nickname";
    private final static String commandChangeStatus = "change_my_status";

    private static BufferedReader reader = new BufferedReader(
                                            new InputStreamReader(System.in));

    private static InetAddress currAddr;

    private static String name, status;

    private static void endConversation() {
        try {
            reader.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            currAddr = InetAddress.getLocalHost();
        } catch (Exception e) {
            System.out.println("Can't identify the host, goodbye!");
            System.out.println(e.getMessage());
            return;
        }
        try (Socket clSocket = new Socket("localhost", 1234)) {
            System.out.println("Enter your name");
            try {
                name = reader.readLine();
            } catch (Exception e) {
                System.out.println("Sorry, can't read your name. Goodbye!");
                endConversation();
                return;
            }
            System.out.println("Enter your status (e.g. sleeping, eating, " +
                                                                    "working)");
            try {
                status = reader.readLine();
            } catch (Exception e) {
                System.out.println("Sorry, can't read your status. Goodbye!");
                endConversation();
                return;
            }
            System.out.println("Hello, " + name + "! Glad you're " + status +
                                                    "! Welcome to the chat!");
            System.out.println("Available commands:");
            System.out.println("1. " + commandChangeName + " <Name> - changes" +
                                                    " your nickname to <Name>");
            System.out.println("2. " + commandChangeStatus + " <Status> - " +
                                            "changes your status to <Status>");
            System.out.println("You will get a message about connection to " +
                                                                "the server");
            Thread consoleToServer = new Thread(new Runnable() {
                public void run() {
                    try (ObjectOutputStream out = new ObjectOutputStream(
                                                clSocket.getOutputStream()))
                                            // try closes the Writer by itself
                    {
                        String systemMessage = "New user " + name + " with " +
                                                            "status " + status;
                        out.writeObject(new Message(systemMessage, "System", "",
                                                        new Date(), currAddr));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Date date = new Date();
                            String firstWord = line.split(" ")[0];
                            String currLine, currName, currStatus;
                            if (firstWord.equals(commandChangeName)) {
                                String newName = line.substring(
                                                        firstWord.length() + 1);
                                currLine = "User " + name + " has changed " +
                                                        "name to " + newName;
                                currName = "System";
                                currStatus = "";
                                name = newName;
                            } else if (firstWord.equals(commandChangeStatus)) {
                                String newStatus = line.substring(
                                                        firstWord.length() + 1);
                                currLine = "User " + name + " has changed " +
                                                    "status to " + newStatus;
                                currName = "System";
                                currStatus = "";
                                status = newStatus;
                            } else {
                                currLine = line;
                                currName = name;
                                currStatus = status;
                            }
                            out.writeObject(new Message(currLine, currName,
                                                currStatus, date, currAddr));
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            });

            Thread serverToConsole = new Thread(new Runnable() {
                public void run() {
                    try (ObjectInputStream in = new ObjectInputStream(
                                                    clSocket.getInputStream()))
                    {
                        Message newMessage;
                        while (true) {
                            try {
                                newMessage = (Message) in.readObject();
                            } catch (Exception e) {
                                break;
                            }
                            System.out.println(newMessage.toString());
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            });

            consoleToServer.start();
            serverToConsole.start();
            consoleToServer.join();
            serverToConsole.join();
            endConversation();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
