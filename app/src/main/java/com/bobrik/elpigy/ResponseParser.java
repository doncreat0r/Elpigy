package com.bobrik.elpigy;

/**
 * Created by DBobrik on 017 17.01.2015.
 * Class for parsing the device response to GET_DATA request
 */
public class ResponseParser {

    // raw data and requests, note: some requests needs checksum update!
    public byte[] DATA;
    public static final int RESPONSE_LENGTH = 90;
    public static byte[] REQUEST_GET_DATA = new byte[] {0x62, 0x00, 0x00, 0x00, 0x00, 0x00, 0x62};
    public static byte[] REQUEST_ADD_LPG = new byte[] {0x62, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static byte[] REQUEST_ADD_PET = new byte[] {0x62, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static byte[] REQUEST_RESET_TRIP = new byte[] {0x62, 0x03, 0x00, 0x00, 0x00, 0x00, 0x65};
    public static byte[] REQUEST_SET_LPG_FLOW = new byte[] {0x62, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static byte[] REQUEST_SET_PET_FLOW = new byte[] {0x62, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static byte[] REQUEST_REBOOT = new byte[] {0x62, (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x61};

    public static final int CELL_SMALL_1_1 = 0;
    public static final int CELL_SMALL_1_2 = 1;
    public static final int CELL_SMALL_1_3 = 2;
    public static final int CELL_SMALL_2_1 = 3;
    public static final int CELL_SMALL_2_2 = 4;
    public static final int CELL_SMALL_2_3 = 5;
    public static final int CELL_SMALL_3_1 = 6;
    public static final int CELL_SMALL_3_2 = 7;
    public static final int CELL_SMALL_3_3 = 8;

    public static final int CELL_BIG_1_1 = 0;
    public static final int CELL_BIG_1_2 = 1;
    public static final int CELL_BIG_2_1 = 2;
    public static final int CELL_BIG_2_2 = 3;

    // parsed data
    public double LPGAvgInjTime;
    public double PETAvgInjTime;
    public int LPGRPM;
    public double LPGPcol;
    public double LPGPsys;
    public double LPGTred;
    public double LPGTgas;
    public double LPGVbat;
    public int OBDRPM;
    public int LPGStatus;
    public int LPGStatus_old;
    public int OBDSpeed;
    public double OBDLoad;
    public int OBDECT;
    public double OBDMap;
    public double OBDTA;
    public int OBDIAT;
    public double OBDSTFT;
    public double OBDLTFT;
    public int OBDerror;
    public double OBDTPS;

    public double OutsideTemp;
    public int EEPROMUpdateCount;

    public double LPGPerHour;
    public double LPGPer100Short;
    public double LPGPer100Long;
    public double LPGPer100Trip;

    public double PETPerHour;
    public double PETPer100Short;
    public double PETPer100Long;
    public double PETPer100Trip;

    public double LPGTripSpent;
    public double LPGTripDist;
    public double LPGTripTime;

    public double PETTripSpent;
    public double PETTripDist;
    public double PETTripTime;

    public double LPGInTank;
    public double PETInTank;

    public int LPGinjFlow;
    public int PETinjFlow;
    public int LPGerrBits;

    /**
     *  Get unsigned byte as integer from buffer
     */
    private int getUBYTE(int os) {
        if (DATA.length > os)
            return (DATA[os] & 0xFF);
        else
            return 0;
    }

    private int getWORD(int os) {
        return (getUBYTE(os+1) << 8) + getUBYTE(os);
    }

    private int getDWORD(int os) {
        return (getUBYTE(os + 3) << 24) + (getUBYTE(os + 2) << 16) + (getUBYTE(os + 1) << 8) + getUBYTE(os);
    }

    /**
     * Cast to int when required!
     */
    private byte getBYTE(int os) {
        if (DATA.length > os)
            return DATA[os];
        else
            return 0;
    }

    /**
     * Constructor.
     */
    public ResponseParser(){
        DATA = new byte[8192];  // buffer for device response
    }

    public void UpdateChecksum(byte[] REQ) {
        int cs = 0;
        for (int i=0; i < REQ.length-1; i++)
            cs = cs + REQ[i];
        REQ[REQ.length-1] = (byte)cs;
    }

    public boolean Parse() {
        if (DATA[0] != 0x42 || DATA[1] > 64)
            return false;

        // parse data from response buffer
        LPGAvgInjTime = getWORD(4) / 4.0 / 187.0;  // in ms
        PETAvgInjTime = getWORD(2) / 4.0 / 187.0;
        LPGRPM = getWORD(6);
        LPGPcol = getWORD(8) / 100.0;
        LPGPsys = getWORD(10) / 100.0;
        LPGTred = (short) getWORD(12) / 100.0;
        LPGTgas = (short) getWORD(14) / 100.0;
        LPGVbat = getWORD(16) / 100.0;

        OBDRPM = getWORD(18) / 4; // integer division!
        LPGStatus = getUBYTE(20);
        OBDSpeed = getUBYTE(21);
        OBDLoad = getUBYTE(22) * 100.0 / 255.0;  // in %
        OBDECT = getUBYTE(23) - 40;
        OBDMap = getUBYTE(24) / 100.0;
        OBDTA = getUBYTE(25) / 2.0 - 64;
        OBDIAT = getUBYTE(26) - 40;
        OBDSTFT = (getUBYTE(27) - 128) / 1.27;
        OBDLTFT = (getUBYTE(28) - 128) / 1.27;
        OBDerror = getUBYTE(29);
        OBDTPS = getUBYTE(30) * 100.0 / 255.0;   // in %

        LPGPerHour = getWORD(32) / 1000.0;
        LPGPer100Short = getWORD(36) / 1000.0;
        LPGPer100Long = getWORD(42) / 1000.0;
        LPGTripDist = getDWORD(66) / 100000.0; // in km (from cm)
        LPGTripSpent = getDWORD(58) / 1.0; // in some volume units
        LPGTripTime = getDWORD(74) / 1.0;  // in 10ms units
        if (LPGTripDist > 0.1)
            LPGPer100Trip = getDWORD(58) / 100.0 / (getDWORD(66) / 100.0);

        PETPerHour = getWORD(34) / 1000.0;
        PETPer100Short = getWORD(38) / 1000.0;
        PETPer100Long = getWORD(44) / 1000.0;
        PETTripDist = getDWORD(70) / 100000.0; // in km (from cm)
        PETTripSpent = getDWORD(62) / 1.0; // in some volume units
        PETTripTime = getDWORD(80) / 1.0;  // in 10ms units
        if (PETTripDist > 0.1)
            PETPer100Trip = getDWORD(62) / 100.0 / (getDWORD(70) / 100.0);

        LPGInTank = getDWORD(46) / 10000000.0;
        PETInTank = getDWORD(50) / 10000000.0;

        OutsideTemp = getBYTE(41) / 2.0;
        EEPROMUpdateCount = getWORD(84);

        LPGinjFlow = getUBYTE(86);
        PETinjFlow = getUBYTE(87);
        LPGerrBits = getUBYTE(31);

        return true;
    }
}
