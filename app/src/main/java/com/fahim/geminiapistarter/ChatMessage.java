package com.fahim.geminiapistarter;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatMessage {
    @Insert
    void insert(ChatUser message);

    @Query("SELECT * FROM chat_messages")
    List<ChatUser> getAllMessages();

    @Query("DELETE FROM chat_messages")
    void deleteAll();
}
