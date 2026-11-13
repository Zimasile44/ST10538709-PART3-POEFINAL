package com.mycompany.poe;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;

// Holds one chat message and the lists of sent, disregarded and stored messages (Parts 2 and 3)
public class Message {

    public static final int MAX_LENGTH = 250;

    // Options for what to do with a finished message
    public static final int SEND = 1;
    public static final int DISREGARD = 2;
    public static final int STORE = 3;

    // Every message sent or stored while the program runs
    private static ArrayList<Message> sentMessages = new ArrayList<>();
    private static ArrayList<Message> storedMessages = new ArrayList<>();

    // Part 3 lists: disregarded messages, plus every hash and ID that has been created
    private static ArrayList<Message> disregardedMessages = new ArrayList<>();
    private static ArrayList<String> messageHashes = new ArrayList<>();
    private static ArrayList<String> messageIDs = new ArrayList<>();

    // File the stored messages are written to
    private static String storageFile = "storedMessages.json";

    private String messageID;
    private int messageNumber;
    private String recipient;
    private String messageText;
    private String messageHash;

    public Message(String messageID, int messageNumber, String recipient, String messageText) {
        this.messageID = messageID;
        this.messageNumber = messageNumber;
        this.recipient = recipient;
        this.messageText = messageText;
        this.messageHash = createMessageHash();
    }

    // Makes a random ten digit ID, built one digit at a time so leading zeros are kept
    public static String generateMessageID() {
        Random random = new Random();
        String id = "";
        for (int i = 0; i < 10; i++) {
            id = id + random.nextInt(10);
        }
        return id;
    }

    // ID must be digits only and no more than ten characters
    public boolean checkMessageID() {
        return messageID != null && messageID.length() > 0
                && messageID.length() <= 10 && messageID.matches("[0-9]+");
    }

    // Recipient uses the same rules as the registered cell number
    public static String checkRecipientCell(String recipient) {
        if (Login.checkCellPhoneNumber(recipient)) {
            return Login.CELL_OK;
        }
        return Login.CELL_BAD;
    }

    // Message may not be longer than 250 characters
    public static String checkMessageLength(String text) {
        if (text.length() <= MAX_LENGTH) {
            return "Message ready to send.";
        }
        int extra = text.length() - MAX_LENGTH;
        return "Message exceeds 250 characters by " + extra + "; please reduce the size.";
    }

    // First two digits of the ID : message number : first word + last word, all in capitals
    public String createMessageHash() {
        String[] words = messageText.trim().split("\\s+");
        String first = words[0].replaceAll("[^A-Za-z0-9]", "");
        String last = words[words.length - 1].replaceAll("[^A-Za-z0-9]", "");
        String start = messageID.substring(0, Math.min(2, messageID.length()));
        return (start + ":" + messageNumber + ":" + first + last).toUpperCase();
    }

    // Send, disregard or store the message depending on the user's choice
    public String sentMessage(int choice) {
        if (choice == SEND) {
            addMessage(this, SEND);
            return "Message successfully sent.";
        } else if (choice == DISREGARD) {
            return "Press 0 to delete the message.";
        } else if (choice == STORE) {
            addMessage(this, STORE);
            if (storeMessage()) {
                return "Message successfully stored.";
            }
            return "Message could not be saved to the file.";
        }
        return "Invalid option, please choose 1, 2 or 3.";
    }

    // Full details of this message in the order the PoE asks for
    public String getDetails() {
        return "Message ID: " + messageID + "\n"
                + "Message Hash: " + messageHash + "\n"
                + "Recipient: " + recipient + "\n"
                + "Message: " + messageText;
    }

    // Details of every message sent so far
    public static String printMessages() {
        String output = "";
        for (Message message : sentMessages) {
            output = output + message.getDetails() + "\n\n";
        }
        return output;
    }

    // How many messages have been sent
    public static int returnTotalMessages() {
        return sentMessages.size();
    }

    // Writes all stored messages to the JSON file
    // TODO: add your reference for the JSON writing code here (the PoE says it must be attributed)
    public boolean storeMessage() {
        return saveStoredMessages();
    }

    // Does the actual writing, used by storeMessage() and when a stored message is deleted
    private static boolean saveStoredMessages() {
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < storedMessages.size(); i++) {
            Message m = storedMessages.get(i);
            json.append("  {\n");
            json.append("    \"messageID\": \"").append(escapeJson(m.messageID)).append("\",\n");
            json.append("    \"messageNumber\": ").append(m.messageNumber).append(",\n");
            json.append("    \"recipient\": \"").append(escapeJson(m.recipient)).append("\",\n");
            json.append("    \"message\": \"").append(escapeJson(m.messageText)).append("\",\n");
            json.append("    \"messageHash\": \"").append(escapeJson(m.messageHash)).append("\"\n");
            json.append("  }");
            if (i < storedMessages.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("]\n");

        try (FileWriter writer = new FileWriter(storageFile)) {
            writer.write(json.toString());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Escapes characters that would break a JSON string
    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Called after the user presses 0 to delete a message, keeps it in the disregarded list
    public void disregardMessage() {
        addMessage(this, DISREGARD);
    }

    // Adds a message to the list that matches its flag (SEND, DISREGARD or STORE)
    // and records its hash and ID. The unit tests use this to load the PoE test data.
    public static void addMessage(Message message, int flag) {
        if (flag == SEND) {
            sentMessages.add(message);
        } else if (flag == DISREGARD) {
            disregardedMessages.add(message);
        } else if (flag == STORE) {
            storedMessages.add(message);
        } else {
            return;
        }
        messageHashes.add(message.messageHash);
        messageIDs.add(message.messageID);
    }

    // Finds a sent or stored message using its message ID, returns null if there is no match
    public static Message searchByMessageID(String id) {
        for (Message message : sentMessages) {
            if (message.messageID.equals(id)) {
                return message;
            }
        }
        for (Message message : storedMessages) {
            if (message.messageID.equals(id)) {
                return message;
            }
        }
        return null;
    }

    // Collects the text of every sent or stored message for one recipient
    public static ArrayList<String> searchByRecipient(String recipient) {
        ArrayList<String> found = new ArrayList<>();
        for (Message message : sentMessages) {
            if (message.recipient.equals(recipient)) {
                found.add(message.messageText);
            }
        }
        for (Message message : storedMessages) {
            if (message.recipient.equals(recipient)) {
                found.add(message.messageText);
            }
        }
        return found;
    }

    // Returns the text of the longest stored message
    public static String getLongestStoredMessage() {
        if (storedMessages.isEmpty()) {
            return "There are no stored messages.";
        }
        Message longest = storedMessages.get(0);
        for (Message message : storedMessages) {
            if (message.messageText.length() > longest.messageText.length()) {
                longest = message;
            }
        }
        return longest.messageText;
    }

    // Lists the sender and recipient of every stored message
    public static String displayStoredSendersAndRecipients(String sender) {
        if (storedMessages.isEmpty()) {
            return "There are no stored messages.";
        }
        String output = "";
        for (Message message : storedMessages) {
            output = output + "Sender: " + sender + " | Recipient: " + message.recipient + "\n";
        }
        return output;
    }

    // Deletes a stored or sent message using its hash and tidies up the other lists
    public static String deleteByHash(String hash) {
        for (int i = 0; i < storedMessages.size(); i++) {
            if (storedMessages.get(i).messageHash.equals(hash)) {
                Message removed = storedMessages.remove(i);
                messageHashes.remove(removed.messageHash);
                messageIDs.remove(removed.messageID);
                // Keep the JSON file in step with the stored list
                saveStoredMessages();
                return "Message: \"" + removed.messageText + "\" successfully deleted.";
            }
        }
        for (int i = 0; i < sentMessages.size(); i++) {
            if (sentMessages.get(i).messageHash.equals(hash)) {
                Message removed = sentMessages.remove(i);
                messageHashes.remove(removed.messageHash);
                messageIDs.remove(removed.messageID);
                return "Message: \"" + removed.messageText + "\" successfully deleted.";
            }
        }
        return "No message was found with that hash.";
    }

    // Report of all sent messages: hash, recipient and message
    public static String getSentReport() {
        return buildReport("Sent messages report", sentMessages);
    }

    // Report with the full details of all stored messages
    public static String getStoredReport() {
        return buildReport("Stored messages report", storedMessages);
    }

    // Builds the text for a report from one of the lists
    private static String buildReport(String title, ArrayList<Message> list) {
        String output = "--- " + title + " ---\n";
        if (list.isEmpty()) {
            return output + "No messages to show.\n";
        }
        for (int i = 0; i < list.size(); i++) {
            Message message = list.get(i);
            output = output + (i + 1) + ") Message ID: " + message.messageID + "\n"
                    + "   Message Hash: " + message.messageHash + "\n"
                    + "   Recipient: " + message.recipient + "\n"
                    + "   Message: " + message.messageText + "\n";
        }
        return output;
    }

    // Reads the JSON file back into the stored list, returns how many messages were loaded.
    // TODO: add your reference for the JSON reading code here (the PoE says it must be attributed)
    public static int loadStoredMessages() {
        Path path = Paths.get(storageFile);
        if (!Files.exists(path)) {
            return 0;
        }
        String json;
        try {
            json = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return 0;
        }

        int loaded = 0;
        int[] pos = {0};
        while (true) {
            int open = json.indexOf('{', pos[0]);
            if (open == -1) {
                break;
            }
            pos[0] = open + 1;

            String id = "";
            int number = 0;
            String recipient = "";
            String text = "";

            // Read "key": value pairs until the closing brace of this object
            while (pos[0] < json.length() && json.charAt(pos[0]) != '}') {
                if (json.charAt(pos[0]) != '"') {
                    pos[0]++;
                    continue;
                }
                String key = readJsonString(json, pos);
                int colon = json.indexOf(':', pos[0]);
                if (colon == -1) {
                    break;
                }
                pos[0] = colon + 1;
                while (pos[0] < json.length() && Character.isWhitespace(json.charAt(pos[0]))) {
                    pos[0]++;
                }
                if (pos[0] >= json.length()) {
                    break;
                }

                String value;
                if (json.charAt(pos[0]) == '"') {
                    value = readJsonString(json, pos);
                } else {
                    int start = pos[0];
                    while (pos[0] < json.length() && ",}\n".indexOf(json.charAt(pos[0])) == -1) {
                        pos[0]++;
                    }
                    value = json.substring(start, pos[0]).trim();
                }

                if (key.equals("messageID")) {
                    id = value;
                } else if (key.equals("messageNumber")) {
                    try {
                        number = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        number = 0;
                    }
                } else if (key.equals("recipient")) {
                    recipient = value;
                } else if (key.equals("message")) {
                    text = value;
                }
            }
            pos[0]++;

            // Skip anything that is empty or already in the list
            if (!id.isEmpty() && !messageIDs.contains(id)) {
                addMessage(new Message(id, number, recipient, text), STORE);
                loaded++;
            }
        }
        return loaded;
    }

    // Reads one JSON string starting at the opening quote and moves pos past the closing quote
    private static String readJsonString(String json, int[] pos) {
        StringBuilder result = new StringBuilder();
        int i = pos[0] + 1;
        while (i < json.length() && json.charAt(i) != '"') {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                i++;
                char next = json.charAt(i);
                if (next == 'n') {
                    result.append('\n');
                } else if (next == 'r') {
                    result.append('\r');
                } else if (next == 't') {
                    result.append('\t');
                } else if (next == 'u' && i + 4 < json.length()) {
                    result.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                    i += 4;
                } else {
                    result.append(next);
                }
            } else {
                result.append(c);
            }
            i++;
        }
        pos[0] = i + 1;
        return result.toString();
    }

    // Used by the unit tests to start each test with empty lists and a test file
    public static void resetMessages(String testFile) {
        sentMessages.clear();
        storedMessages.clear();
        disregardedMessages.clear();
        messageHashes.clear();
        messageIDs.clear();
        storageFile = testFile;
    }

    public String getMessageID() {
        return messageID;
    }

    public int getMessageNumber() {
        return messageNumber;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getMessageHash() {
        return messageHash;
    }

    public static int getStoredCount() {
        return storedMessages.size();
    }

    public static ArrayList<Message> getSentMessages() {
        return sentMessages;
    }

    public static ArrayList<Message> getStoredMessages() {
        return storedMessages;
    }

    public static ArrayList<Message> getDisregardedMessages() {
        return disregardedMessages;
    }

    public static ArrayList<String> getMessageHashes() {
        return messageHashes;
    }

    public static ArrayList<String> getMessageIDs() {
        return messageIDs;
    }
}
