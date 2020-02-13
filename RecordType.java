public enum RecordType {
    A(1),
    AAAA(28),
    CN(5),
    NS(2),
    OTHER (-1);
    public final int code;

    private RecordType(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public RecordType getByCode(int code) {
        for (RecordType type : values()) {
            if (type.code == code)
                return type;
        }
        // RecordType is something else
        return OTHER;
    }
}

