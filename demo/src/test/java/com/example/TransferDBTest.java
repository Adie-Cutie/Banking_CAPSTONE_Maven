package com.example;

// Ensure this matches the capitalized Class name in your utils folder
import com.example.utils.mongoConfig; 
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.Assert; 

public class TransferDBTest {
    private MongoDatabase db;
    private MongoCollection<Document> accounts;

    @BeforeMethod 
    public void setup() {
        // Calling the Utility with Capital M
        db = mongoConfig.getDatabase();
        accounts = db.getCollection("accounts");
        
        // Wipe collection to ensure a clean state for every test
        accounts.deleteMany(new Document());
    }

    @Test
    public void testAccountTransfer() {
        // 1. Arrange: Setup initial balances
        accounts.insertOne(new Document("userId", "user1").append("balance", 1000.0));
        accounts.insertOne(new Document("userId", "user2").append("balance", 500.0));

        // 2. Act: Simulate the transfer logic
        // Use 200.0 (Double) to maintain type consistency in MongoDB
        accounts.updateOne(new Document("userId", "user1"), new Document("$inc", new Document("balance", -200.0)));
        accounts.updateOne(new Document("userId", "user2"), new Document("$inc", new Document("balance", 200.0)));

        // 3. Assert: Verify the results
        Document sender = accounts.find(new Document("userId", "user1")).first();
        Document receiver = accounts.find(new Document("userId", "user2")).first();

        Assert.assertNotNull(sender, "Sender record should exist in DB");
        Assert.assertNotNull(receiver, "Receiver record should exist in DB");
        
        // TestNG Assertions: (actual, expected)
        Assert.assertEquals(sender.getDouble("balance"), 800.0, "Sender balance mismatch");
        Assert.assertEquals(receiver.getDouble("balance"), 700.0, "Receiver balance mismatch");
    }

    @AfterClass
    // Removed 'static' to fix the TestNG configuration warning
    public void tearDown() {
        mongoConfig.close();
    }
}