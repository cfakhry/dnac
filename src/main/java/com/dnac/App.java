package com.dnac;

import com.dnac.sdk.DnacClient;
import com.dnac.sdk.DnacClientImpl;
import com.dnac.sdk.api.*;
import com.dnac.sdk.config.DnacConfig;
import com.dnac.sdk.model.common.CountResponse;
import com.dnac.sdk.model.device.AddDeviceRequest;
import com.dnac.sdk.model.device.Device;
import com.dnac.sdk.model.site.Site;
import com.dnac.sdk.model.site.SiteListResponse;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Optional;
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

        // Build config from properties
        DnacConfig cfg = buildConfig(props);

        try (DnacClient client = new DnacClientImpl(cfg)) {

            // API singletons
            DevicesApi devices = client.devices();
            SitesApi sites = client.sites();
            TemplatesApi templates = client.templates();
            CommandRunnerApi cmd = client.commandRunner();
            AuthApi auth = client.auth();
            MiscApi misc = client.misc();

            Scanner input = new Scanner(System.in);
            int choice = -1;

            while (choice != 0) {
                System.out.println("\nPlease choose one of the following options:");
                System.out.println("0. Exit");
                System.out.println("1. Display access token");
                System.out.println("2. List devices");
                System.out.println("3. Make a DNAC GET call (enter path)");
                System.out.println("4. (Optional) Add device (requires admin role)");
                System.out.println("5. Run 'show version' & 'show ip int brief' on platformId=C9500-40X and print output");
                System.out.println("6. List Sites");
                System.out.println("7. Create Project");
                System.out.println("8. List Projects");
                System.out.println("9. List Device by ID");
                System.out.println("10. List Device by serial number");
                System.out.println("11. Devices count");

                try {
                    choice = input.nextInt();
                } catch (InputMismatchException ex) {
                    // skip non-integer
                }
                input.nextLine();

                try {
                    switch (choice) {
                        case 0 -> System.out.println("Goodbye...");
                        case 1 -> displayAccessToken(auth);
                        case 2 -> listDevices(devices);
                        case 3 -> {
                            System.out.print("Enter DNAC GET path (e.g. /dna/intent/api/v1/network-device/count): ");
                            String path = input.nextLine().trim();
                            makeDnacGetCall(misc, path);
                        }
                        case 4 -> addDeviceInteractive(input, devices, props);
                        case 5 -> runPythonParityDemo(cmd, devices);
                        case 6 -> listSites(sites);
                        //case 7 -> createProjectInteractive(input, templates);
                        //case 8 -> listProjects(templates);
                        case 9 -> listDeviceById(devices);
                        case 10 -> listDeviceBySerialNumber(devices);
                        case 11 -> devicesCount(misc);
                        default -> System.out.println("Invalid choice");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
            input.close();
        } catch (Exception e) {
            System.out.println("Fatal error initializing client");
            System.out.println(e.getMessage());
        }
    }

    private static DnacConfig buildConfig(Properties p) {
        String host = trimTrailingSlash(required(p, "dnac.host"));
        String username = required(p, "dnac.username");
        String password = required(p, "dnac.password");
        boolean insecure = Boolean.parseBoolean(p.getProperty("dnac.insecure", "false"));

        return new DnacConfig(
                URI.create(host),
                username,
                password,
                insecure,
                Duration.ofSeconds(15),   // connect timeout
                Duration.ofSeconds(30)    // request timeout
        );
    }

    // ---- Menu handlers (now calling the new APIs) ----

    private static void displayAccessToken(AuthApi auth) {
        try {
            final String token = auth.getToken();
            System.out.println("Access token: " + token);
        } catch (Exception e) {
            System.out.println("Error getting token");
            System.out.println(e.getMessage());
        }
    }

    private static void listDevices(DevicesApi devices) {
        try {
            List<Device> list = devices.listAll();
            if (list.isEmpty()) {
                System.out.println("No devices returned.");
                return;
            }
            for (Device d : list) {
                printDevice(d);
            }
            System.out.println("Total: " + list.size());
        } catch (Exception e) {
            System.out.println("Error listing devices");
            System.out.println(e.getMessage());
        }
    }

    private static void listDeviceById(DevicesApi devices) {
        final String id = "d145ac01-2409-46cf-86e8-8b7703c0598a";
        try {
            Device device = devices.getById(id);
            if (device == null) {
                System.out.println("Device not found");
                return;
            }
            printDevice(device);
        } catch (Exception e) {
            System.out.println("Error fetching device by id.");
            System.out.println(e.getMessage());
        }
    }

    private static void listDeviceBySerialNumber(DevicesApi devices) {
        final String serialNumber = "CML12345ABC";
        try {
            Device device = devices.getBySerial(serialNumber);
            if (device == null) {
                System.out.println("Device not found");
                return;
            }
            printDevice(device);
        } catch (Exception e) {
            System.out.println("Error fetching device by serial number.");
            System.out.println(e.getMessage());
        }
    }

    private static void devicesCount(MiscApi misc) {
        try {
            // Uses a tiny CountResponse DTO instead of JsonNode parsing
            CountResponse count = misc.getCount("/dna/intent/api/v1/network-device/count");
            System.out.println("Devices count: " + count.response);
        } catch (Exception e) {
            System.out.println("Error getting device count");
            System.out.println(e.getMessage());
        }
    }

    private static void listSites(SitesApi sites) {
        try {
            SiteListResponse resp = sites.list();
            List<Site> list = (resp == null || resp.response == null) ? List.of() : resp.response;
            if (list.isEmpty()) {
                System.out.println("No sites returned.");
                return;
            }
            for (Site s : list) {
                System.out.println("Site: " + nullSafe(s.name));
                System.out.println("  ID: " + nullSafe(s.id));
                System.out.println("  Tenant: " + nullSafe(s.instanceTenantId));
                System.out.println("  Hierarchy: " + nullSafe(s.siteHierarchy));
                System.out.println("  NameHierarchy: " + nullSafe(s.siteNameHierarchy));
                System.out.println("  AdditionalInfo: " + (s.additionalInfo == null ? "[]" : s.additionalInfo));
                System.out.println();
            }
        } catch (Exception e) {
            System.out.println("Error listing sites");
            System.out.println(e.getMessage());
        }
    }

    private static void makeDnacGetCall(MiscApi misc, String path) {
        try {
            String json = misc.getRaw(path);
            System.out.println("Response JSON:\n" + json);
        } catch (Exception e) {
            System.out.println("Error making DNAC GET call");
            System.out.println(e.getMessage());
        }
    }

    private static void addDeviceInteractive(Scanner input, DevicesApi devices, Properties props) {
        try {
            System.out.println("Enter device IP (e.g. 10.10.20.80): ");
            String ip = input.nextLine().trim();

            AddDeviceRequest req = new AddDeviceRequest();
            req.ipAddress = new String[]{ ip };
            req.snmpVersion = "v2";
            req.snmpROCommunity = "public";
            req.snmpRWCommunity = "private";
            req.cliTransport = "ssh";
            // reuse creds from properties
            req.userName = props.getProperty("dnac.username");
            req.password = props.getProperty("dnac.password");
            req.enablePassword = props.getProperty("dnac.password");

            String result = devices.addDeviceRaw(req);  // small addition in DevicesApi
            System.out.println("Add device response:\n" + result);
            System.out.println("(Note: On Always-On sandbox, this likely fails due to read-only role.)");

        } catch (Exception e) {
            System.out.println("Error adding device");
            System.out.println(e.getMessage());
        }
    }

    /** Mirrors your Python main(): filter by platformId, run commands, print 'show ip int brief'. */
    private static void runPythonParityDemo(CommandRunnerApi cmd, DevicesApi devices) {
        String platformId = "C9500-40X";
        try {
            Optional<String> output = cmd.runShowIpIntBriefOnPlatform(platformId, devices);
            if (output.isEmpty()) {
                System.out.println("No output received (no devices or no SUCCESS results).");
            } else {
                System.out.println("\n---- 'show ip int brief' Output ----\n");
                System.out.println(output.get());
            }
        } catch (Exception e) {
            System.out.println("Error running command runner demo");
            System.out.println(e.getMessage());
        }
    }

    private static void printDevice(Device device) {
        System.out.println("Device: " + nullSafe(device.hostname));
        System.out.println("  ID: " + nullSafe(device.id));
        System.out.println("  Mgmt IP: " + nullSafe(device.managementIpAddress));
        System.out.println("  Type: " + nullSafe(device.type));
        System.out.println("  PlatformId: " + nullSafe(device.platformId));
        System.out.println("  SW: " + nullSafe(device.softwareVersion));
        System.out.println("  Serial Number: " + nullSafe(device.serialNumber));
        System.out.println();
    }

    private static String nullSafe(Object o) { return o == null ? "" : o.toString(); }

    private static String required(Properties p, String k) {
        String v = p.getProperty(k);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing property: " + k);
        }
        return v.trim();
    }

    private static String trimTrailingSlash(String s) {
        return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }
}