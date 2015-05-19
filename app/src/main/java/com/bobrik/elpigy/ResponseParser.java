package com.bobrik.elpigy;

/**
 * Created by DBobrik on 017 17.01.2015.
 * Class for parsing the device response to GET_DATA request
 */
public class ResponseParser {

    // raw data and requests, note: some requests needs checksum update!
    public byte[] DATA;
    //public static final int RESPONSE_LENGTH = 90;
    public static byte[] REQUEST_STOP_DATA = new byte[] {0x62, 0x00, 0x00, 0x00, 0x00, 0x00, 0x62};
    public static byte[] REQUEST_GET_DATA = new byte[] {0x62, 0x00, 0x01, 0x00, 0x00, 0x00, 0x63};
    public static byte[] REQUEST_GET_OSA = new byte[] {0x62, 0x00, 0x02, 0x00, 0x00, 0x00, 0x64};
    public static byte[] REQUEST_ADD_LPG = new byte[] {0x62, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static byte[] REQUEST_ADD_PET = new byte[] {0x62, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static byte[] REQUEST_RESET_TRIP = new byte[] {0x62, 0x03, 0x00, 0x00, 0x00, 0x00, 0x65};
    public static byte[] REQUEST_SET_LPG_FLOW = new byte[] {0x62, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static byte[] REQUEST_SET_PET_FLOW = new byte[] {0x62, 0x09, 0x00, 0x00, 0x00, 0x00, 0x00};
    //public static byte[] REQUEST_REBOOT = new byte[] {0x62, (byte)0xFF, 0x00, 0x00, 0x00, 0x00, 0x61};

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

    public final int TYPE_RESP_FAST = 1;
    public final int TYPE_RESP_SLOW = 2;
    public final int TYPE_RESP_RARE = 4;
    public final int TYPE_RESP_PARK = 8;
    public final int TYPE_RESP_OSA1 = 16;


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

    public int PAA;
    public int PAB;
    public int PAC;
    public int PAD;
    public int PACM;
    public int PAstatus;
    public int workMode;
    public int packetType;

    public byte OSATable[] = new byte[25];
    public boolean OSAChanged = false;

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

    private void ParseFast() {

        LPGAvgInjTime = getWORD(6) / 4.0 / 187.0;  // in ms
        PETAvgInjTime = getWORD(4) / 4.0 / 187.0;
        LPGRPM = getWORD(8);
        LPGPcol = getWORD(10) / 100.0;
        LPGPsys = getWORD(12) / 100.0;
        LPGVbat = getWORD(14) / 100.0;
        OBDRPM = getWORD(16) / 4; // integer division!
        LPGStatus = getUBYTE(18);
        OBDSpeed = getUBYTE(19);
        OBDMap = getUBYTE(20) / 100.0;
        OBDTA = getUBYTE(21) / 2.0 - 64;
        OBDSTFT = (getUBYTE(22) - 128) / 1.27;
        OBDLTFT = (getUBYTE(23) - 128) / 1.27;
        OBDerror = getUBYTE(24);
        OBDTPS = getUBYTE(25) * 100.0 / 255.0;   // in %
        OBDLoad = getUBYTE(26) * 100.0 / 255.0;  // in %
        LPGerrBits = getUBYTE(27);
        LPGPerHour = getWORD(28) / 1000.0;
        PETPerHour = getWORD(30) / 1000.0;

    }

    private void ParseSlow() {

        OutsideTemp = getBYTE(5) / 2.0;
        LPGTred = (short) getWORD(6) / 100.0;
        LPGTgas = (short) getWORD(8) / 100.0;
        OBDECT = getUBYTE(10) - 40;
        OBDIAT = getUBYTE(12) - 40;

        LPGPer100Short = getWORD(14) / 1000.0;
        LPGPer100Long = getWORD(18) / 1000.0;
        LPGTripDist = getDWORD(38) / 100000.0; // in km (from cm)
        LPGTripSpent = getDWORD(30) / 1.0; // in some volume units
        LPGTripTime = getDWORD(46) / 1.0;  // in 10ms units
        if (LPGTripDist > 0.1)
            LPGPer100Trip = getDWORD(30) / 100.0 / (getDWORD(38) / 100.0);

        PETPer100Short = getWORD(16) / 1000.0;
        PETPer100Long = getWORD(20) / 1000.0;
        PETTripDist = getDWORD(42) / 100000.0; // in km (from cm)
        PETTripSpent = getDWORD(34) / 1.0; // in some volume units
        PETTripTime = getDWORD(50) / 1.0;  // in 10ms units
        if (PETTripDist > 0.1)
            PETPer100Trip = getDWORD(34) / 100.0 / (getDWORD(42) / 100.0);

        LPGInTank = getDWORD(22) / 10000000.0;
        PETInTank = getDWORD(26) / 10000000.0;

    }

    private void ParseRare() {

        EEPROMUpdateCount = getWORD(4);

        LPGinjFlow = getUBYTE(6);
        PETinjFlow = getUBYTE(7);

    }

    private void ParsePark() {

        PAA = getUBYTE(8);
        PAB = getUBYTE(9);
        PAC = getUBYTE(10);
        PAD = getUBYTE(11);
        PACM = getUBYTE(12);
        PAstatus = getUBYTE(13);
    }

    private void ParseOSA() {
        OSAChanged = false;
        for (int i = 0; i < 25; i++) {
            if (OSATable[i] != getBYTE(i+4))  OSAChanged = true;
            OSATable[i] = getBYTE(i+4);
        }
    }

    public void ClearOSA() {
        for (int i = 0; i < 25; i++) {
            OSATable[i] = 0;
        }
    }

    public boolean Parse() {
        int i, cs = 0;

        if (DATA[0] == 0x42)
            for (i = 0; i < getUBYTE(1); i++)
                cs += getUBYTE(i);
        else
            return false;

        // checksum not correct
        // TODO: checksum
        //if ((cs  & 0xFF) != (DATA[getUBYTE(1)-1] & 0xFF))
        //    return false;

        // parse data from response buffer
        workMode = getUBYTE(3);
        packetType = getUBYTE(2);
        switch (packetType) {
            case TYPE_RESP_FAST: ParseFast(); break;
            case TYPE_RESP_SLOW: ParseSlow(); break;
            case TYPE_RESP_RARE: ParseRare(); break;
            case TYPE_RESP_PARK: ParsePark(); break;
            case TYPE_RESP_OSA1: ParseOSA();  break;
        }

        return true;
    }
}
