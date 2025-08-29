package com.dnac;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        System.out.println("Java App-Only Cisco DNAC Tutorial\n");

        final Properties props = new Properties();
        try {
            props.load(App.class.getResourceAsStream("/dnac.properties"));
        } catch (IOException e) {
            System.out.println("Unable to read dnac.properties from classpath (same package as App).");
            System.out.println("Create dnac.properties with dnac.host, dnac.username, dnac.password");
            return;
        }

        initializeDnac(props);

        Scanner input = new Scanner(System.in);
        int choice = -1;

        while (choice != 0) {
            System.out.println("\nPlease choose one of the following options:");
            System.out.println("0. Exit");
            System.out.println("1. Display access token");
            System.out.println("2. List devices");
            System.out.println("3. Make a DNAC GET call (enter path)");
            System.out.println("4. (Optional) Add device (requires admin role)");

            try {
                choice = input.nextInt();
            } catch (InputMismatchException ex) {
                // skip non-integer
            }
            input.nextLine();

            try {
                switch (choice) {
                    case 0:
                        System.out.println("Goodbye...");
                        break;
                    case 1:
                        displayAccessToken();
                        break;
                    case 2:
                        listDevices();
                        break;
                    case 3:
                        System.out.print("Enter DNAC GET path (e.g. /dna/intent/api/v1/network-device-count): ");
                        String path = input.nextLine().trim();
                        makeDnacGetCall(path);
                        break;
                    case 4:
                        addDeviceInteractive(input);
                        break;
                    default:
                        System.out.println("Invalid choice");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        input.close();
    }

    private static void initializeDnac(Properties properties) {
        try {
            Dnac.initialize(properties);
        } catch (Exception e) {
            System.out.println("Error initializing DNAC");
            System.out.println(e.getMessage());
        }
    }

    private static void displayAccessToken() {
        try {
            final String token = Dnac.getToken();
            System.out.println("Access token: " + token);
        } catch (Exception e) {
            System.out.println("Error getting token");
            System.out.println(e.getMessage());
        }
    }

    private static void listDevices() {
        try {
            List<Dnac.Device> devices = Dnac.getDevices();
            if (devices.isEmpty()) {
                System.out.println("No devices returned.");
                return;
            }
            for (Dnac.Device d : devices) {
                System.out.println("Device: " + nullSafe(d.hostname));
                System.out.println("  ID: " + nullSafe(d.id));
                System.out.println("  Mgmt IP: " + nullSafe(d.managementIpAddress));
                System.out.println("  Type: " + nullSafe(d.type));
                System.out.println("  SW: " + nullSafe(d.softwareVersion));
                System.out.println();
            }
            System.out.println("Total: " + devices.size());
        } catch (Exception e) {
            System.out.println("Error listing devices");
            System.out.println(e.getMessage());
        }
    }

    private static void makeDnacGetCall(String path) {
        try {
            String json = Dnac.getRaw(path);
            System.out.println("Response JSON:\n" + json);
        } catch (Exception e) {
            System.out.println("Error making DNAC GET call");
            System.out.println(e.getMessage());
        }
    }

    private static void addDeviceInteractive(Scanner input) {
        try {
            System.out.println("Enter device IP (e.g. 10.10.20.80): ");
            String ip = input.nextLine().trim();

            Dnac.AddDeviceRequest req = new Dnac.AddDeviceRequest();
            req.ipAddress = new String[] { ip };
            req.snmpVersion = "v2";
            req.snmpROCommunity = "public";
            req.snmpRWCommunity = "private";
            req.cliTransport = "ssh";
            req.userName = Dnac.username(); // reuse creds if desired
            req.password = Dnac.password();
            req.enablePassword = Dnac.password();

            String result = Dnac.addDeviceRaw(req);
            System.out.println("Add device response:\n" + result);
            System.out.println("(Note: On Always-On sandbox, this likely fails due to read-only role.)");

        } catch (Exception e) {
            System.out.println("Error adding device");
            System.out.println(e.getMessage());
        }
    }

    private static String nullSafe(Object o) { return o == null ? "" : o.toString(); }
}