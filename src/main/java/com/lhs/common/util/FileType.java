package com.lhs.common.util;

public enum FileType {
    /**
     * JPEG
     */
    JPEG("JPEG", "FFD8FF"),

    /**
     * PNG
     */
    PNG("PNG", "89504E47"),

    /**
     * GIF
     */
    GIF("GIF", "47494638"),

    /**
     * TIFF
     */
    TIFF("TIFF", "49492A00"),

    /**
     * Windows bitmap
     */
    BMP("BMP", "424D"),

    /**
     * CAD
     */
    DWG("DWG", "41433130"),

    /**
     * Adobe photoshop
     */
    PSD("PSD", "38425053"),

    /**
     * Rich Text Format
     */
    RTF("RTF", "7B5C727466"),

    /**
     * XML
     */
    XML("XML", "3C3F786D6C"),

    /**
     * HTML
     */
    HTML("HTML", "68746D6C3E"),

    /**
     * Outlook Express
     */
    DBX("DBX", "CFAD12FEC5FD746F "),

    /**
     * Outlook
     */
    PST("PST", "2142444E"),

    /**
     * doc;xls;dot;ppt;xla;ppa;pps;pot;msi;sdw;db
     */
    OLE2("OLE2", "0xD0CF11E0A1B11AE1"),

    /**
     * Microsoft Word/Excel
     */
    OFFICE("OFFICE", "D0CF11E0"),

    /**
     * Microsoft Access
     */
    MDB("MDB", "5374616E64617264204A"),

    /**
     * Word Perfect
     */
    WPB("WPB", "FF575043"),

    /**
     * Postscript
     */
    EPS_PS("EPS_PS", "252150532D41646F6265"),

    /**
     * Adobe Acrobat
     */
    PDF("PDF", "255044462D312E"),

    /**
     * Windows Password
     */
    PWL("PWL", "E3828596"),

    /**
     * ZIP Archive
     */
    ZIP("ZIP", "504B0304"),

    /**
     * ARAR Archive
     */
    RAR("RAR", "52617221"),

    /**
     * WAVE
     */
    WAV("WAV", "57415645"),

    /**
     * AVI
     */
    AVI("AVI", "41564920"),

    /**
     * Real Audio
     */
    RAM("RAM", "2E7261FD"),

    /**
     * Real Media
     */
    RM("RM", "2E524D46"),

    /**
     * Quicktime
     */
    MOV("MOV", "6D6F6F76"),

    /**
     * Windows Media
     */
    ASF("ASF", "3026B2758E66CF11"),

    /**
     * MIDI
     */
    MID("MID", "4D546864");

    private String key;
    private String value;

    FileType(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }
}

