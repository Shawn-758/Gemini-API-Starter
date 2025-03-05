package com.fahim.geminiapistarter;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatUser {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String message;
    public boolean isUser;

    public ChatUser(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }
}
