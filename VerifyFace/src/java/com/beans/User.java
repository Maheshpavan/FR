/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.beans;

/**
 *
 * @author pavankumar.g
 */
public class User {
    
    private String name;
    private int age;
    private String mobile;
    private String email;
    private String address;
    private String imageName;
    private String imageLocation;
    private String uuid;
    private String createdOn;
    private String updatedOn;
    private String createdBy;
    private String updatedBy;
    private String imageLink;
    private String personSeqNumber;
    private String folderName;
    private String templateFolderName;

    public String getTemplateFolderName() {
        return templateFolderName;
    }

    public void setTemplateFolderName(String templateFolderName) {
        this.templateFolderName = templateFolderName;
    }
    
    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
    
    public String getPersonSeqNumber() {
        return personSeqNumber;
    }

    public void setPersonSeqNumber(String personSeqNumber) {
        this.personSeqNumber = personSeqNumber;
    }

    
    public String getImageLink() {
        return imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }
    
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImageLocation() {
        return imageLocation;
    }

    public void setImageLocation(String imageLocation) {
        this.imageLocation = imageLocation;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public String toString() {
        return "Customer{" + "name=" + name + ", age=" + age + ", mobile=" + mobile + ", email=" + email + ", address=" + address + ", imageName=" + imageName + ", imageLocation=" + imageLocation + ", createdBy=" + createdBy + ", updatedBy=" + updatedBy + '}';
    }
    
}
