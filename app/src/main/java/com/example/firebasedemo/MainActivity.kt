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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FirebaseDemoTheme {
                AppNavigation()
            }
        }
    }

    val auth = FirebaseAuth.getInstance()
    var storedVerificationId: String? = null
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    fun signUP(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    println(user)
                } else {
                    println("User couldn't be created.")
                    println(task.exception?.message)
                }
            }
    }

    fun signIN(email: String, password: String) {
        auth.signInWithEmailAndPassword(email,password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser?.email
                    println("$user Logged In")
                } else {
                    println("User not found")
                    println(task.exception?.message)
                }
            }
    }

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:$credential")
            signINWithPhoneCred(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w(TAG, "onVerificationFailed", e)

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
            } else if (e is FirebaseAuthMissingActivityForRecaptchaException) {
                // reCAPTCHA verification attempted with null Activity
            }

            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId
            resendToken = token
        }
    }
    fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    fun verifyPhoneWithCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signINWithPhoneCred(credential)
    }
    fun signINWithPhoneCred(cred: PhoneAuthCredential) {
        auth.signInWithCredential(cred)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    val user = task.result?.user
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
            }
    }

    @Composable
    fun LoginScreen(navController: NavController) {
        val email = remember {
            mutableStateOf("")
        }
        val password = remember {
            mutableStateOf("")
        }
        val existingUser = remember {
            mutableStateOf(true)
        }
        val gradientColor = listOf(Color.Black,Color.Blue,Color.DarkGray)

        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (existingUser.value) {
                Text(
                    text = "Welcome Back",
                    style = TextStyle(brush = Brush.linearGradient(gradientColor)),
                    fontSize = 30.sp
                )
            } else {
                Text(
                    text = "Become an Authorized User",
                    style = TextStyle(brush = Brush.linearGradient(gradientColor)),
                    fontSize = 30.sp
                )
            }
            Spacer(modifier = Modifier.height(25.dp))
            OutlinedTextField(value = email.value, onValueChange = {
                email.value = it
            },
                label = {
                    Text(text = "Enter Email")
                },
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

            OutlinedTextField(value = password.value, onValueChange = {
                password.value = it
            },
                label = {
                    Text(text = "Enter Password")
                },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
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

            if (existingUser.value) {
                FilledTonalButton(onClick = { signIN(email.value, password.value) }) {
                    Text(text = "Sign In")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Text(text = "Don't have an account?")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sign Up",
                        modifier = Modifier.clickable { existingUser.value = false },
                        color = Color.Blue
                    )
                }
            } else {
                FilledTonalButton(onClick = { signUP(email.value, password.value) }) {
                    Text(text = "Sign Up")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Text(text = "Already have an account?")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sign In",
                        modifier = Modifier.clickable { existingUser.value = true },
                        color = Color.Blue
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { navController.navigate("OTPScreen") }) {
                Text(text = "Sign IN using Phone Number", color = Color.Blue)
            }
        }
    }

    @Composable
    fun OTPScreen(navController: NavController) {
        val phoneNumber = remember { mutableStateOf("") }
        val otpCode = remember { mutableStateOf("") }
        val gradientColor = listOf(Color.Black,Color.Blue,Color.DarkGray)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Text(
                text = "Welcome",
                style = TextStyle(brush = Brush.linearGradient(gradientColor)),
                fontSize = 30.sp
            )
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(value = phoneNumber.value, onValueChange = {
                phoneNumber.value = it
            },
                label = { Text(text = "Enter Phone Number")},
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
            FilledTonalButton(onClick = {
                startPhoneNumberVerification("+91" + phoneNumber.value)
            }) {
                Text(text = "Send OTP")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = otpCode.value, onValueChange = {
                if (it.length <= 6) otpCode.value = it
            },
                label = { Text(text = "Enter OTP")},
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
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
            FilledTonalButton(onClick = {
                verifyPhoneWithCode(otpCode.value)
            }) {
                Text(text = "Verify OTP")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(thickness = 2.dp)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { navController.navigate("LoginScreen") }) {
                Text(text = "Sign IN using Email", color = Color.Blue)
            }
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "LoginScreen") {
            composable("LoginScreen") { LoginScreen(navController) }
            composable("OTPScreen") { OTPScreen(navController) }
        }
    }
}