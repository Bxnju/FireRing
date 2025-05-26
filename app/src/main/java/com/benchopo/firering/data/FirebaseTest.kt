package com.benchopo.firering.data

import android.util.Log
import com.benchopo.firering.model.GameState
import com.benchopo.firering.model.RuleType
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

object FirebaseTest {
    private const val TAG = "FirebaseTest"

    fun testNewDatabaseStructure() {
        val database = FirebaseDatabase.getInstance()

        // Generate a unique test room code
        val testRoomCode = "TEST-${UUID.randomUUID().toString().substring(0, 4)}"
        Log.d(TAG, "Testing with room code: $testRoomCode")

        // Get reference to our test room
        val roomRef = database.getReference("rooms").child(testRoomCode)

        // Create test player
        val testPlayerId = UUID.randomUUID().toString()
        val testPlayer =
                mapOf(
                        "id" to testPlayerId,
                        "name" to "Test Player",
                        "isHost" to true,
                        "isGuest" to false,
                        "selectedDrinkId" to "beer",
                        "drinkCount" to 0,
                        "mateIds" to listOf<String>(),
                        "isConnected" to true,
                        "lastActiveTimestamp" to System.currentTimeMillis(),
                        "turnIndex" to 0
                )

        // Create test room info
        val roomInfo =
                mapOf(
                        "roomCode" to testRoomCode,
                        "hostId" to testPlayerId,
                        "gameState" to GameState.WAITING.name,
                        "kingsCupCount" to 0,
                        "createdAt" to System.currentTimeMillis()
                )

        // Create test rule
        val testRule =
                mapOf(
                        "id" to "test_rule",
                        "title" to "Test Rule",
                        "description" to "This is a test rule",
                        "createdByPlayerId" to testPlayerId,
                        "createdAt" to System.currentTimeMillis(),
                        "expiresAfterPlayerId" to testPlayerId,
                        "ruleType" to RuleType.CUSTOM.name
                )

        // Create complete test room structure
        val testRoomData =
                mapOf(
                        "info" to roomInfo,
                        "players" to mapOf(testPlayerId to testPlayer),
                        "turnOrder" to listOf(testPlayerId),
                        "activeRules" to mapOf("test_rule" to testRule)
                )

        // Write test data
        roomRef.setValue(testRoomData)
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Test room created successfully")

                    // Read back the data to verify
                    roomRef.addListenerForSingleValueEvent(
                            object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    Log.d(TAG, "📚 Retrieved data: ${snapshot.value}")

                                    // Cleanup test data after verification
                                    roomRef.removeValue()
                                            .addOnSuccessListener {
                                                Log.d(TAG, "🧹 Test data cleaned up")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e(TAG, "❌ Failed to clean up test data", e)
                                            }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.e(TAG, "❌ Database read failed", error.toException())
                                }
                            }
                    )
                }
                .addOnFailureListener { e -> Log.e(TAG, "❌ Failed to create test room", e) }
    }
}
