package com.fahim.geminiapistarter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView chatRecyclerView;
    private ArrayList<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private ChatDB chatDB;

    private RecyclerView suggestionsRecyclerView;
    private SuggestionAdapter suggestionAdapter;
    private ArrayList<String> suggestionsList;

    private static final int SPEECH_REQUEST_CODE = 100;

    private static final String KEY_CHAT_MESSAGES = "chat_messages";
    private static final String KEY_PROMPT_TEXT = "prompt_text";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LAST_ACTIVE = "lastActiveTime";

    private static final long BACKGROUND_THRESHOLD = 2 * 60 * 1000; // 2 minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Automatically selects portrait or landscape layout based on orientation.
        setContentView(R.layout.activity_main);

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        // New: obtain the mic button for speech-to-text.
        ImageButton ttsButton = findViewById(R.id.ttsButton);
        progressBar = findViewById(R.id.progressBar);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        Switch modeToggle = findViewById(R.id.modeToggle);
        suggestionsRecyclerView = findViewById(R.id.suggestionsRecyclerView);

        // Set up dark/light mode toggle.
        modeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Set up speech-to-text on mic click.
        ttsButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.RECORD_AUDIO},
                        SPEECH_REQUEST_CODE);
            } else {
                startSpeechToText();
            }
        });

        // Initialize the Room database.
        chatDB = ChatDB.getInstance(this);

        // Check the last active time from SharedPreferences.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastActiveTime = prefs.getLong(KEY_LAST_ACTIVE, 0);
        long currentTime = System.currentTimeMillis();
        // If the app was in background longer than the threshold, clear chat history.
        if (lastActiveTime > 0 && (currentTime - lastActiveTime) > BACKGROUND_THRESHOLD) {
            chatDB.chatMessageDao().deleteAll();
        }

        // Load saved messages from Room.
        List<ChatUser> savedEntities = chatDB.chatMessageDao().getAllMessages();
        chatMessages = new ArrayList<>();
        for (ChatUser entity : savedEntities) {
            chatMessages.add(new ChatMessage(entity.message, entity.isUser));
        }

        // Restore prompt text from savedInstanceState (for configuration changes).
        if (savedInstanceState != null) {
            String savedPrompt = savedInstanceState.getString(KEY_PROMPT_TEXT, "");
            promptEditText.setText(savedPrompt);
        }

        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        suggestionsList = new ArrayList<>(Arrays.asList("Hello", "How are you?", "Tell me more", "Thanks!"));
        suggestionAdapter = new SuggestionAdapter(suggestionsList, suggestion -> {
            promptEditText.setText(suggestion);
            promptEditText.setSelection(suggestion.length());
        });
        LinearLayoutManager horizontalLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        suggestionsRecyclerView.setLayoutManager(horizontalLayoutManager);
        suggestionsRecyclerView.setAdapter(suggestionAdapter);

        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash",
                BuildConfig.API_KEY);

        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            // Clear the input field after submission.
            promptEditText.setText("");

            // Add user message to list and database.
            ChatMessage userMsg = new ChatMessage(prompt, true);
            chatMessages.add(userMsg);
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            chatDB.chatMessageDao().insert(new ChatUser(prompt, true));

            progressBar.setVisibility(VISIBLE);
            generativeModel.generateContent(prompt, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    GenerateContentResponse response = (GenerateContentResponse) o;
                    String responseString = response.getText();
                    Log.d("Response", responseString);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        // Add Gemini response to list and database.
                        ChatMessage geminiMsg = new ChatMessage(responseString, false);
                        chatMessages.add(geminiMsg);
                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                        chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                        chatDB.chatMessageDao().insert(new ChatUser(responseString, false));
                    });
                }
            });
        });
    }

    // Helper method to start speech-to-text.
    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now");
        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech to text not supported on your device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                // Set the recognized speech into the promptEditText
                promptEditText.setText(spokenText);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Record the time when the app goes to background.
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current prompt text for configuration changes.
        outState.putString(KEY_PROMPT_TEXT, promptEditText.getText().toString());
    }

    // Parcelable class representing each chat message (for in‑memory state only)
    public static class ChatMessage implements Parcelable {
        private String message;
        private boolean isUser;

        public ChatMessage(String message, boolean isUser) {
            this.message = message;
            this.isUser = isUser;
        }

        protected ChatMessage(Parcel in) {
            message = in.readString();
            isUser = in.readByte() != 0;
        }

        public static final Creator<ChatMessage> CREATOR = new Creator<ChatMessage>() {
            @Override
            public ChatMessage createFromParcel(Parcel in) {
                return new ChatMessage(in);
            }

            @Override
            public ChatMessage[] newArray(int size) {
                return new ChatMessage[size];
            }
        };

        public String getMessage() {
            return message;
        }

        public boolean isUser() {
            return isUser;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(message);
            parcel.writeByte((byte) (isUser ? 1 : 0));
        }
    }

    // RecyclerView Adapter for displaying chat messages with different layouts.
    public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private ArrayList<ChatMessage> messages;
        private static final int VIEW_TYPE_USER = 0;
        private static final int VIEW_TYPE_BOT = 1;

        public ChatAdapter(ArrayList<ChatMessage> messages) {
            this.messages = messages;
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage message = messages.get(position);
            return message.isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_USER) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_chat_message_user, parent, false);
            } else {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_chat_message_bot, parent, false);
            }
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage chatMessage = messages.get(position);
            holder.bind(chatMessage);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView messageTextView;

            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                messageTextView = itemView.findViewById(R.id.messageTextView);
            }

            public void bind(ChatMessage chatMessage) {
                messageTextView.setText(chatMessage.getMessage());
            }
        }
    }
}
