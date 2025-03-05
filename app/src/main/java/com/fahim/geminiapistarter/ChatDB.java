package com.fahim.geminiapistarter;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ChatUser.class}, version = 1)
public abstract class ChatDB extends RoomDatabase {
    public abstract ChatMessage chatMessageDao();

    private static ChatDB instance;

    public static synchronized ChatDB getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            ChatDB.class, "chat_database")
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}
