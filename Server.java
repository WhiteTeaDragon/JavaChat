import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import message.Message;
import java.net.InetAddress;

public class Server {
    private static Set<ObjectOutputStream> allClients =
                Collections.synchronizedSet(new HashSet<ObjectOutputStream>());
    private static Object lock = new Object();
    private static volatile int numOfConnections = 0;

    private static Queue<Message> lastMessages = new LinkedList<Message>();

    private final static int maxConnections = 1;
    private static InetAddress currAddr;

    private static void closeClient(Socket clSocket) {
        synchronized (lock) {
            --numOfConnections;
            if (numOfConnections == maxConnections - 1) {
                lock.notify();
            }
        }
        try {
            clSocket.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            currAddr = InetAddress.getLocalHost();
        } catch (Exception e) {
            System.out.println("Can't identify the host, goodbye!");
            System.out.println(e.getMessage());
            return;
        }
        try (ServerSocket servSocket = new ServerSocket(1234)) {
            while (!servSocket.isClosed()) {
                Socket clSocket;
                try {
                    synchronized (lock) {
                        while (numOfConnections >= maxConnections) {
                            lock.wait();
                        }
                    }
                    clSocket = servSocket.accept();
                    ++numOfConnections;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(e.getMessage());
                    break;
                }
                Thread currThread = new Thread(new Runnable() {
                    public void run() {
                        ObjectOutputStream out;
                        try {
                            out = new ObjectOutputStream(
                                                    clSocket.getOutputStream());
                        } catch (Exception e) {
                            closeClient(clSocket);
                            return;
                        }
                        allClients.add(out);
                        try {
                            out.writeObject(new Message("You are now " +
                                        "connected to the server", "System", "",
                                                        new Date(), currAddr));
                        } catch (Exception e) {
                            allClients.remove(out);
                            closeClient(clSocket);
                            return;
                        }
                        synchronized (lastMessages) {
                            try {
                                for (Message message : lastMessages) {
                                    out.writeObject(message);
                                }
                            } catch (Exception e) {
                                allClients.remove(out);
                                closeClient(clSocket);
                                return;
                            }
                        }
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
                                synchronized (allClients) {
                                    for (ObjectOutputStream elem : allClients) {
                                        if (!elem.equals(out)) {
                                            try {
                                                elem.writeObject(newMessage);
                                            } catch (Exception e) {}
                                        }
                                    }
                                }
                                synchronized (lastMessages) {
                                    lastMessages.add(newMessage);
                                    if (lastMessages.size() > 10) {
                                        lastMessages.remove();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                        allClients.remove(out);
                        closeClient(clSocket);
                    }
                });
                currThread.start();
            }
            System.out.println("End of while");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
