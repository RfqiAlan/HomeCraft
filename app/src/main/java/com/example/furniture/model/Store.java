package com.example.furniture.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Model untuk satu toko Kohls (endpoint stores/list).
 */
public class Store {

    @SerializedName("storeNum")
    private String storeNum;

    @SerializedName("storeName")
    private String storeName;

    @SerializedName("distanceFromOrigin")
    private String distanceFromOrigin;

    @SerializedName("supportsBopus")
    private Boolean supportsBopus;

    @SerializedName("sephoraFlag")
    private String sephoraFlag;

    @SerializedName("address")
    private Address address;

    @SerializedName("storeHours")
    private StoreHours storeHours;

    @SerializedName("contactInfo")
    private List<ContactInfo> contactInfo;

    // ─── Getters / Setters ──────────────────────────────────────────────────────

    public String getStoreNum() { return storeNum; }
    public void setStoreNum(String storeNum) { this.storeNum = storeNum; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getDistanceFromOrigin() { return distanceFromOrigin; }
    public void setDistanceFromOrigin(String v) { this.distanceFromOrigin = v; }

    public Boolean getSupportsBopus() { return supportsBopus; }
    public void setSupportsBopus(Boolean supportsBopus) { this.supportsBopus = supportsBopus; }

    public String getSephoraFlag() { return sephoraFlag; }
    public void setSephoraFlag(String sephoraFlag) { this.sephoraFlag = sephoraFlag; }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    public StoreHours getStoreHours() { return storeHours; }
    public void setStoreHours(StoreHours storeHours) { this.storeHours = storeHours; }

    public List<ContactInfo> getContactInfo() { return contactInfo; }
    public void setContactInfo(List<ContactInfo> contactInfo) { this.contactInfo = contactInfo; }

    // ─── Nested classes ─────────────────────────────────────────────────────────

    public static class Address {
        @SerializedName("addr1") private String addr1;
        @SerializedName("addr2") private String addr2;
        @SerializedName("city") private String city;
        @SerializedName("state") private String state;
        @SerializedName("postalCode") private String postalCode;
        @SerializedName("countryCode") private String countryCode;
        @SerializedName("phoneNumber") private String phoneNumber;
        @SerializedName("location") private Location location;

        public String getAddr1() { return addr1; }
        public String getAddr2() { return addr2; }
        public String getCity() { return city; }
        public String getState() { return state; }
        public String getPostalCode() { return postalCode; }
        public String getCountryCode() { return countryCode; }
        public String getPhoneNumber() { return phoneNumber; }
        public Location getLocation() { return location; }
    }

    public static class Location {
        @SerializedName("latitude") private String latitude;
        @SerializedName("longitude") private String longitude;

        public String getLatitude() { return latitude; }
        public String getLongitude() { return longitude; }
    }

    public static class StoreHours {
        @SerializedName("days") private List<Day> days;
        public List<Day> getDays() { return days; }
    }

    public static class Day {
        @SerializedName("name") private String name;
        @SerializedName("hours") private Hours hours;

        public String getName() { return name; }
        public Hours getHours() { return hours; }
    }

    public static class Hours {
        @SerializedName("open") private String open;
        @SerializedName("close") private String close;

        public String getOpen() { return open; }
        public String getClose() { return close; }
    }

    public static class ContactInfo {
        @SerializedName("type") private String type;
        @SerializedName("value") private String value;

        public String getType() { return type; }
        public String getValue() { return value; }
    }
}
