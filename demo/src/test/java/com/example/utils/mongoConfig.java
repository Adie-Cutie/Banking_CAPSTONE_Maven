package com.example.utils;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class mongoConfig {
    private static final String CONNECTION_STRING = "mongodb+srv://aditirathii5656_db_user:Adiyui098@cluster0.gh560f5.mongodb.net/";
    private static MongoClient mongoClient;

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(CONNECTION_STRING);
        }
        return mongoClient.getDatabase("test");
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
