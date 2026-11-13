package com.mycompany.poe;

import java.util.ArrayList;
import java.util.Scanner;

// Console app: register, log in, then send messages through the QuickChat menu
public class Main {

    static Scanner input = new Scanner(System.in);

    // Reads a whole number, asks again if the user types anything else
    public static int readNumber(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = input.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    // Registration: keeps asking until every detail is valid
    public static void register(Login login) {
        System.out.print("Enter your first name: ");
        String firstName = input.nextLine();
        System.out.print("Enter your last name: ");
        String lastName = input.nextLine();

        while (!login.isRegistered()) {
            System.out.print("Enter your username: ");
            String username = input.nextLine();
            System.out.print("Enter your password: ");
            String password = input.nextLine();
            System.out.print("Enter your cell phone number (+27): ");
            String cellphone = input.nextLine();

            System.out.println(login.registerUser(firstName, lastName, username, password, cellphone));
            if (!login.isRegistered()) {
                System.out.println();
            }
        }
    }

    // Login: three attempts, returns true if the user got in
    public static boolean logIn(Login login) {
        System.out.println("\n--- Login ---");
        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.print("Enter username to login: ");
            String username = input.nextLine();
            System.out.print("Enter password to login: ");
            String password = input.nextLine();

            boolean loginSuccessful = login.loginUser(username, password);
            System.out.println(login.returnLoginStatus(loginSuccessful));
            if (loginSuccessful) {
                return true;
            }
        }
        System.out.println("Too many failed attempts.");
        return false;
    }

    // Asks for the recipient until the number is valid
    public static String askRecipient() {
        while (true) {
            System.out.print("Enter the recipient's cell number: ");
            String recipient = input.nextLine().trim();
            String result = Message.checkRecipientCell(recipient);
            System.out.println(result);
            if (result.equals(Login.CELL_OK)) {
                return recipient;
            }
        }
    }

    // Asks for the message text until it is not empty and not too long
    public static String askMessageText() {
        while (true) {
            System.out.print("Enter your message: ");
            String text = input.nextLine();
            if (text.trim().isEmpty()) {
                System.out.println("The message cannot be empty.");
                continue;
            }
            String result = Message.checkMessageLength(text);
            System.out.println(result);
            if (text.length() <= Message.MAX_LENGTH) {
                return text;
            }
        }
    }

    // Loops once for each message the user said they would enter
    public static void sendMessages(int numberOfMessages) {
        for (int i = 0; i < numberOfMessages; i++) {
            System.out.println("\n--- Message " + (i + 1) + " of " + numberOfMessages + " ---");

            String recipient = askRecipient();
            String text = askMessageText();

            Message message = new Message(Message.generateMessageID(), i, recipient, text);
            if (!message.checkMessageID()) {
                System.out.println("The message ID is not valid, this message was skipped.");
                continue;
            }
            System.out.println("Message ID generated: " + message.getMessageID());

            System.out.println("1) Send Message");
            System.out.println("2) Disregard Message");
            System.out.println("3) Store Message to send later");
            int choice = readNumber("Choose an option: ");
            while (choice < 1 || choice > 3) {
                choice = readNumber("Please choose 1, 2 or 3: ");
            }

            System.out.println(message.sentMessage(choice));

            if (choice == Message.SEND) {
                System.out.println(message.getDetails());
            } else if (choice == Message.DISREGARD) {
                // Keep asking until the user confirms the delete with 0
                int confirm = readNumber("Enter 0 to confirm: ");
                while (confirm != 0) {
                    System.out.println("Press 0 to delete the message.");
                    confirm = readNumber("Enter 0 to confirm: ");
                }
                message.disregardMessage();
                System.out.println("Message deleted.");
            }
        }
        System.out.println("\nTotal messages sent: " + Message.returnTotalMessages());
    }

    // Stored Messages menu (Part 3)
    public static void storedMessagesMenu(Login login) {
        int option = 0;
        while (option != 7) {
            System.out.println("\n--- Stored Messages ---");
            System.out.println("1) Display the sender and recipient of all stored messages");
            System.out.println("2) Display the longest stored message");
            System.out.println("3) Search for a message ID");
            System.out.println("4) Search for all messages to a recipient");
            System.out.println("5) Delete a message using its hash");
            System.out.println("6) Display a report of the messages");
            System.out.println("7) Back to the main menu");
            option = readNumber("Choose an option: ");

            if (option == 1) {
                System.out.print(Message.displayStoredSendersAndRecipients(login.getRegisteredUsername()));
            } else if (option == 2) {
                System.out.println("Longest stored message: " + Message.getLongestStoredMessage());
            } else if (option == 3) {
                System.out.print("Enter the message ID: ");
                Message found = Message.searchByMessageID(input.nextLine().trim());
                if (found == null) {
                    System.out.println("No message was found with that ID.");
                } else {
                    System.out.println("Recipient: " + found.getRecipient());
                    System.out.println("Message: " + found.getMessageText());
                }
            } else if (option == 4) {
                System.out.print("Enter the recipient's cell number: ");
                ArrayList<String> found = Message.searchByRecipient(input.nextLine().trim());
                if (found.isEmpty()) {
                    System.out.println("No messages were found for that recipient.");
                } else {
                    for (String text : found) {
                        System.out.println("Message: " + text);
                    }
                }
            } else if (option == 5) {
                System.out.print("Enter the message hash: ");
                System.out.println(Message.deleteByHash(input.nextLine().trim()));
            } else if (option == 6) {
                System.out.println();
                System.out.print(Message.getSentReport());
                System.out.println();
                System.out.print(Message.getStoredReport());
            } else if (option != 7) {
                System.out.println("Please choose a number from 1 to 7.");
            }
        }
    }

    public static void main(String[] args) {
        Login login = new Login();
        register(login);

        // Messages can only be sent after a successful login
        if (!logIn(login)) {
            return;
        }

        System.out.println("\nWelcome to QuickChat.");

        int numberOfMessages = readNumber("How many messages would you like to enter? ");
        while (numberOfMessages < 1) {
            numberOfMessages = readNumber("Please enter at least 1 message: ");
        }
        boolean messagesEntered = false;

        // Read any messages stored in the JSON file on a previous run
        int loaded = Message.loadStoredMessages();
        if (loaded > 0) {
            System.out.println(loaded + " stored message(s) loaded from the file.");
        }

        int option = 0;
        while (option != 3) {
            System.out.println("\n1) Send Messages");
            System.out.println("2) Show recently sent messages");
            System.out.println("3) Quit");
            System.out.println("4) Stored Messages");
            option = readNumber("Choose an option: ");

            if (option == 1) {
                if (messagesEntered) {
                    System.out.println("You have already entered all " + numberOfMessages + " messages.");
                } else {
                    sendMessages(numberOfMessages);
                    messagesEntered = true;
                }
            } else if (option == 2) {
                System.out.println("Coming Soon.");
            } else if (option == 3) {
                System.out.println("Goodbye.");
            } else if (option == 4) {
                storedMessagesMenu(login);
            } else {
                System.out.println("Please choose 1, 2, 3 or 4.");
            }
        }
    }
}
