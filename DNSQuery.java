import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Random;

public class DNSQuery {
    private byte[] queryInBytes;
    private String fqdn;
    private int queryID;
    private boolean isIPV6;
    private Random random = new Random();
    private InetAddress serverIP;
    final int DNS_PORT_NUMBER = 53;


    public DNSQuery(String fqdn, boolean isIPV6, InetAddress serverIP) {
        this.fqdn = fqdn;
        this.isIPV6 = isIPV6;
        this.serverIP = serverIP;


        try {
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteArrayOut);
            // build DNS query
            queryID = random.nextInt(65535);
            short queryIDAsShort = (short) queryID;
            dataOut.writeShort(queryIDAsShort);
            // query parameters
            dataOut.writeShort(0x0000);
            // number of questions
            dataOut.writeShort(0x0001);
            // number of answers
            dataOut.writeShort(0x0000);
            // number of authority records
            dataOut.writeShort(0x0000);
            // number of additional records
            dataOut.writeShort(0x0000);

            String[] domainParts = fqdn.split("\\.");

            for (int i = 0; i<domainParts.length; i++) {
                byte[] domainBytes = domainParts[i].getBytes("UTF-8");
                dataOut.writeByte(domainBytes.length);
                dataOut.write(domainBytes);
            }
            // Terminate QNAME
            dataOut.writeByte(0x00);
            // Search for A / AAAA records
            if (isIPV6) {
                dataOut.writeShort(0x11100);
            }
            else {
                dataOut.writeShort(0x0001);
            }
            // QCLASS Use 1 for IN
            dataOut.writeShort(0x0001);
            // encode everything to byte array
            queryInBytes = byteArrayOut.toByteArray();

        }  catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public int getQueryID() {
        return this.queryID;
    }

    public String getFqdn() {
        return this.fqdn;
    }

    public byte[] getDNSQuery() {
        return this.queryInBytes;
    }

    public DatagramPacket getDNSQueryPacket() {
        DatagramPacket queryPacket = new DatagramPacket(queryInBytes, queryInBytes.length, serverIP, DNS_PORT_NUMBER);
        return queryPacket;
    }

    public String getQType() {
        if (isIPV6)
            return "AAAA";
        else
            return "A";
    }

    public InetAddress getServerIP() {
        return this.serverIP;
    }

    public void dumpQuery() {
        System.out.println("\n\n" + "Query ID     " + this.queryID + " " + fqdn + " " + getQType() + " --> " + serverIP.getHostAddress());
    }

}