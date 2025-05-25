package com.benchopo.firering.ui

import com.google.firebase.database.FirebaseDatabase

object FirebaseTest {
    fun testWrite() {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("message")
        myRef.setValue("Hola desde FireRing! jeje")  // Este dato se enviará
    }
}
