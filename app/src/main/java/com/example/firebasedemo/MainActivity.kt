package com.example.firebasedemo

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.firebasedemo.ui.theme.FirebaseDemoTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import java.util.concurrent.TimeUnit
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import com.google.firebase.firestore.ktx.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FirebaseDemoTheme {
                AppNavigation()
            }
        }
    }

    val firebaseDB = Firebase.firestore

    private suspend fun addUserToFirebaseDB(name: String, age: Int): String {
        val isAdult = age >= 18
        val firebaseUser = FirebaseUser(name, age, isAdult)
        return try {
            firebaseDB.collection("users").add(firebaseUser).await()
            "User Created Successfully!!"
        } catch (e: Exception) {
            Log.w(TAG, "Document couldn't be added. $e")
            "User Creation Failed!!"
        }
    }

    private suspend fun updateFirebaseUser(name: String, age: Int): String {
        val isAdult = age >= 18
        return try {
            val querySnapshot = firebaseDB.collection("users")
                .whereEqualTo("name", name)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                "User not found."
            } else {
                for (document in querySnapshot) {
                    firebaseDB.collection("users")
                        .document(document.id)
                        .update("age", age, "adult", isAdult)
                        .await()
                }
                "User Updated Successfully!!"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update unsuccessful. $e")
            "User Update Failed!!"
        }
    }

    private suspend fun deleteFirebaseUser(name: String): String {
        return try {
            val querySnapshot = firebaseDB.collection("users")
                .whereEqualTo("name", name)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                "User not found."
            } else {
                for (document in querySnapshot) {
                    firebaseDB.collection("users")
                        .document(document.id)
                        .delete()
                        .await()
                }
                "User Deletion Successful!!"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting document", e)
            "User Deletion Failed!!"
        }
    }

    private suspend fun retrieveFirebaseUsers(): List<FirebaseUser> {
        return try {
            val querySnapshot = firebaseDB.collection("users")
                .get()
                .await()

            querySnapshot.map { document ->
                document.toObject(FirebaseUser::class.java)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error getting documents: ", e)
            emptyList()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FirebaseUserScreen(navController: NavController) {
        val mode = remember { mutableStateOf("Create") }
        val name = remember { mutableStateOf("") }
        val age = remember { mutableStateOf("") }
        val message = remember { mutableStateOf("") }

        if (message.value.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { message.value = "" },
                title = { Text(text = "Message") },
                text = { Text(text = message.value) },
                confirmButton = {
                    Button(onClick = { message.value = "" }) {
                        Text(text = "OK")
                    }
                }
            )
        }

        val gradientColor = listOf(Color.Black, Color.Blue, Color.DarkGray)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                OutlinedButton(onClick = { mode.value = "Create" }) {
                    Text(text = "Create/Add")
                }
                OutlinedButton(onClick = { navController.navigate("DisplayUsersScreen") }) {
                    Text(text = "Retrieve")
                }
            }
            Row {
                OutlinedButton(onClick = { mode.value = "Update" }) {
                    Text(text = "Update")
                }
                OutlinedButton(onClick = { mode.value = "Delete" }) {
                    Text(text = "Delete")
                }
            }

            Text(
                text = when (mode.value) {
                    "Create" -> "Create / Add User"
                    "Update" -> "Update User"
                    "Delete" -> "Delete User By Name"
                    else -> "404 Error"
                },
                style = TextStyle(brush = Brush.linearGradient(gradientColor)),
                fontSize = 30.sp
            )
            Spacer(modifier = Modifier.height(25.dp))
            OutlinedTextField(
                value = name.value,
                onValueChange = { name.value = it },
                label = { Text(text = "Enter Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    focusedBorderColor = Color.DarkGray,
                    focusedLabelColor = Color.DarkGray,
                    unfocusedTextColor = Color.Blue,
                    unfocusedLabelColor = Color.Blue,
                    unfocusedBorderColor = Color.Blue
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = age.value,
                onValueChange = { age.value = it },
                label = { Text(text = "Enter Age") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    focusedBorderColor = Color.DarkGray,
                    focusedLabelColor = Color.DarkGray,
                    unfocusedTextColor = Color.Blue,
                    unfocusedLabelColor = Color.Blue,
                    unfocusedBorderColor = Color.Blue
                ),
                enabled = mode.value != "Delete"
            )
            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = {
                    lifecycleScope.launch {
                        message.value = when (mode.value) {
                            "Create" -> addUserToFirebaseDB(name.value, age.value.toInt())
                            "Update" -> updateFirebaseUser(name.value, age.value.toInt())
                            "Delete" -> deleteFirebaseUser(name.value)
                            else -> ""
                        }
                    }
                }
            ) {
                Text(
                    text = when (mode.value) {
                        "Create" -> "Add User"
                        "Update" -> "Update User"
                        "Delete" -> "Delete User"
                        else -> "404 Error"
                    }
                )
            }
        }
    }

    @Composable
    fun DisplayUsersScreen(navController: NavController) {
        val userList = remember { mutableStateOf<List<FirebaseUser>>(emptyList()) }
        LaunchedEffect(Unit) {
            userList.value = retrieveFirebaseUsers()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedButton(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color.Gray,
                    contentColor = Color.DarkGray
                )
            ) {
                Text(text = "Back")
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                items(userList.value) { user ->
                    Text(text = "Name: ${user.name}, Age: ${user.age}")
                    Divider()
                }
            }
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "FirebaseUserScreen") {
            composable("FirebaseUserScreen") { FirebaseUserScreen(navController) }
            composable("DisplayUsersScreen") { DisplayUsersScreen(navController) }
        }
    }

    companion object {
        const val TAG = "MainActivity"
    }
}

data class FirebaseUser(
    val name: String = "",
    val age: Int = 0,
    var isAdult: Boolean = false
)