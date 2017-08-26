package com.fanny.healthcare.bean;

/**
 * Created by Fanny on 17/4/5.
 */

public class User {
    private String name;
    private String sex;
    private String birthday;
    private String idcardno;


    public User() {
        super();
    }

    public User(String name, String sex, String birthday,String idcardno) {
        super();
        this.name=name;
        this.sex=sex;
        this.birthday=birthday;
        this.idcardno=idcardno;
    }

    public String getName() {
        return name;
    }

    public String getSex() {
        return sex;
    }



    public String getBirthday() {
        return birthday;
    }

    public String getIdcardno() {
        return idcardno;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }



    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public void setIdcardno(String idcardno) {
        this.idcardno = idcardno;
    }
}
