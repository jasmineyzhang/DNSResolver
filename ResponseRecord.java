import java.io.ByteArrayInputStream;

public class ResponseRecord {
    public DNSResponse dnsResponse;
    public RecordType type;
    public String name;
    public int typeCode;
    public int classCode;
    public int timeToLive;
    public int rdLength;
    public String rdData;

    public ResponseRecord(DNSResponse dnsResponse, String name, int typeCode, int classCode, int timeToLive, int rdLength, String rdData) {
        this.dnsResponse = dnsResponse;
        this.name = name;
        this.typeCode = typeCode;
        this.classCode = classCode;
        this.timeToLive = timeToLive;
        this.rdLength = rdLength;
        this.rdData = rdData;

        for (RecordType type : RecordType.values()) {
            if (typeCode == type.code)
                this.type = type;
        }
    }
    public DNSResponse getDNSResponse() {
        return this.dnsResponse;
    }


    public ResponseRecord(ByteArrayInputStream input) {

    }


    private int COLUMN_1_SIZE = 31;
    private int COLUMN_2_SIZE = 11;
    private int COLUMN_3_SIZE = 5;

    public void printRecord() {
        System.out.println("       " + this.name + getNumberOfSpaces(this.name, COLUMN_1_SIZE) + this.timeToLive + getNumberOfSpaces(String.valueOf(this.timeToLive), COLUMN_2_SIZE) +
                this.type.name() + getNumberOfSpaces(this.type.name(), COLUMN_3_SIZE) + this.rdData);

    }

    private String getNumberOfSpaces(String value, int columnSize) {
        int numberOfSpaces = columnSize - value.length();

        return new String(new char[numberOfSpaces]).replace('\0', ' ');
    }


}
