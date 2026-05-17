package org.example.splitwise.models;

import java.util.ArrayList;
import java.util.List;

public class Group {
    String id;
    String description;
    List<User> users;

    public Group(String id, String description) {
        this.id = id;
        this.description = description;
        this.users = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
