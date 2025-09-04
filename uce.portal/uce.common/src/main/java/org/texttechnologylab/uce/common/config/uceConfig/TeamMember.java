package org.texttechnologylab.uce.common.config.uceConfig;

public class TeamMember {

    private String name;
    private String role;
    private String description;
    private String image;
    private ContactConfig contact;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public ContactConfig getContact() {
        return contact;
    }

    public void setContact(ContactConfig contact) {
        this.contact = contact;
    }
}
