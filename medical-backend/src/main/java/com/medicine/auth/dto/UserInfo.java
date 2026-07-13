package com.medicine.auth.dto;

public class UserInfo {

    private Long id;
    private String realname;
    private String uname;
    private String phonenumber;
    private int utype;

    public UserInfo() {
    }

    public UserInfo(Long id, String realname, String uname, String phonenumber, int utype) {
        this.id = id;
        this.realname = realname;
        this.uname = uname;
        this.phonenumber = phonenumber;
        this.utype = utype;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRealname() {
        return realname;
    }

    public void setRealname(String realname) {
        this.realname = realname;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public int getUtype() {
        return utype;
    }

    public void setUtype(int utype) {
        this.utype = utype;
    }
}
