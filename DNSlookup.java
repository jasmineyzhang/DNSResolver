import java.net.*;
import java.util.ArrayList;
import java.util.Random;

/**
 * 
 */

/**
 * @author Donald Acton
 * This example is adapted from Kurose & Ross
 * Feel free to modify and rearrange code as you see fit
 */
public class DNSlookup {
    static final int MIN_PERMITTED_ARGUMENT_COUNT = 2;
    static final int MAX_PERMITTED_ARGUMENT_COUNT = 3;
    private static Random random = new Random();
    private static DatagramSocket socket;
    private static InetAddress rootNameServer;
    private static int queryCount = 0;
    private static String fqdn;
    private static ArrayList<String> cnames = new ArrayList<>();

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        int argCount = args.length;
        boolean tracingOn = false;
        boolean IPV6Query = false;
        try {
            rootNameServer = InetAddress.getByName(args[0]);
            fqdn = args[1];

            if (argCount == 3) {  // option provided
                if (args[2].equals("-t")) {
                    tracingOn = true;
                } else if (args[2].equals("-6")) {
                    IPV6Query = true;
                } else if (args[2].equals("-t6")) {
                    tracingOn = true;
                    IPV6Query = true;
                } else { // option present but wasn't valid option
                    usage();
                    return;
                }
            }

            DNSResponse fqdnResponse = getAuthRecordForDomainName(fqdn, IPV6Query, rootNameServer, tracingOn, false);
            int rCode = fqdnResponse.getrCode();
            // No error case
            if (rCode == 0) {
                ResponseRecord[] answers = fqdnResponse.getAnswerRecords();
                if (answers != null) {
                    // Case where it directly resolves fqdn IP address immediately
                    if (fqdn.equals(answers[0].name)) {
                        // Print all the answer records
                        for (ResponseRecord rr : answers) {
                            System.out.println(fqdn + " " + rr.timeToLive + "   " +
                                    rr.type + " " + rr.rdData);
                        }
                    }
                    else {
                        /* IP of next name server to query */
                        InetAddress nextNameServer = InetAddress.getByName(answers[0].rdData);
                        DNSResponse nextDNSResponse = getAuthRecordForDomainName(fqdn, IPV6Query, nextNameServer,
                                tracingOn, resolveIsNameServer(fqdn));

                        ResponseRecord[] nextDNSResponseAnswers = nextDNSResponse.getAnswerRecords();
                        // Print each answer record
                        for (ResponseRecord rr : nextDNSResponseAnswers) {
                            System.out.println(fqdn + " " + rr.timeToLive + "   " +
                                    rr.type + " " + rr.rdData);
                        }
                    }
                }
                else {
                    // Case when name does not have a corresponding IP address
                    printError(fqdn, -6, IPV6Query);
                }
            }
            else {
                // Find the error type
                switch (rCode) {
                    case 3:
                        printError(fqdn, -1, IPV6Query);
                        break;
                    case 5:
                        printError(fqdn, -4, IPV6Query);
                }
            }
        } catch (DNSLookupsExceededException e) {
            printError(fqdn, -3, IPV6Query);
        }
    }


/**
 * Method that recursively finds the authoritative record for the domain name that is being queried.
 * */

    public static DNSResponse getAuthRecordForDomainName(String fqdn, boolean isIPV6, InetAddress nameServer,
                                                         boolean tracingOn, boolean isNameServer) throws
            DNSLookupsExceededException, DNSTimeoutException {
        try {
            if (queryCount >= 30) throw new DNSLookupsExceededException(queryCount);
            socket = new DatagramSocket();
            socket.setSoTimeout(5000);
            boolean currIPV6 = isIPV6 && !isNameServer;

            DNSQuery queryObject = new DNSQuery(fqdn, currIPV6, nameServer);
            DatagramPacket queryPacket = queryObject.getDNSQueryPacket();
            socket.send(queryPacket);
            // increment the number of total queries sent
            queryCount++;
            // Get response from DNS server
            byte[] buf = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buf, buf.length);
            socket.receive(responsePacket); // throws socketTimeOutException
            //construct new response
            DNSResponse response = new DNSResponse(responsePacket.getData(), responsePacket.getLength());

            if (tracingOn) {
                queryObject.dumpQuery();
                response.dumpResponse();
            }

            if (response.isAuthoritative()) {
                ResponseRecord[] answers = response.getAnswerRecords();
                //
                if (answers[0].type == RecordType.A || answers[0].type == RecordType.AAAA) {
                    return response;
                } else {
                    // type is CNAME record
                    String cname = answers[0].rdData;
                    cnames.add(cname);
                    DNSResponse cnResponse = getAuthRecordForDomainName(cname, isIPV6, rootNameServer, tracingOn,false);
                    return cnResponse;
                }
            } else {
                // !Authoritative case, gotta keep querying
                if (response.getAdditionalCount() > 0) {
                    // Knows the IP address of the next name server
                    ResponseRecord[] additionalInfo = response.getAdditionalInfoRecords();
                    InetAddress nextServerIP = rootNameServer;
                    ResponseRecord[] nsRecords = response.getNSRecords();
                    String nameToSearch = nsRecords[0].rdData;
                    boolean ipFound = false;

                    for (ResponseRecord rr : additionalInfo) {
                        // found IP address that matches NSRecords name server in Additional Info
                        if (rr.name.equals(nameToSearch) && rr.type.equals(RecordType.A)) {
                            nextServerIP = InetAddress.getByName(rr.rdData);
                            ipFound = true;
                        }
                    }

                    if (ipFound) {
                        DNSResponse nsResponse = getAuthRecordForDomainName(fqdn, isIPV6, nextServerIP, tracingOn, resolveIsNameServer(fqdn));
                        return nsResponse;
                    }
                    else {
                        DNSResponse nsResponse = getAuthRecordForDomainName(nameToSearch, isIPV6, nextServerIP, tracingOn, resolveIsNameServer(fqdn));
                    }
                }
                // empty additional section, gotta resolve for NS IP address
                else {
                    ResponseRecord[] nameServers = response.getNSRecords();
                    String nsDomainName = nameServers[0].rdData;
                    DNSResponse nsDMResponse = getAuthRecordForDomainName(nsDomainName, isIPV6, rootNameServer, tracingOn, resolveIsNameServer(fqdn));
                    return nsDMResponse;
                }
            }
        }
        catch (DNSLookupsExceededException e) {
            printError(fqdn, -2, isIPV6);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return new DNSResponse(new byte[0], 0);
    }

        private static void printError(String fqdn, int ttl, boolean IPV6){
            RecordType type = IPV6 ? RecordType.AAAA : RecordType.A;
            System.err.println(fqdn + " " + ttl + " " + type + " " + "0.0.0.0");
            System.exit(ttl);

        }

        public static boolean resolveIsNameServer(String dn) {
        if (dn == fqdn)
            return false;

            for (String cn : cnames) {
                if (dn.equals(cn))
                    return false;
            }
            return true;
        }

        private static void usage () {
            System.out.println("Usage: java -jar DNSlookup.jar rootDNS name [-6|-t|t6]");
            System.out.println("   where");
            System.out.println("       rootDNS - the IP address (in dotted form) of the root");
            System.out.println("                 DNS server you are to start your search at");
            System.out.println("       name    - fully qualified domain name to lookup");
            System.out.println("       -6      - return an IPV6 address");
            System.out.println("       -t      - trace the queries made and responses received");
            System.out.println("       -t6     - trace the queries made, responses received and return an IPV6 address");
        }
    }


