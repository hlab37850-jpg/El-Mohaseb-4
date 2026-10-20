package com.example.ui.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.ui.theme.MohasebPrimaryBlue
import java.security.MessageDigest

object PinHasher {
  fun hash(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(("mohaseb-soft:" + pin).toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }
}

@Composable
fun PinLockScreen(expectedHash: String, onUnlocked: () -> Unit) {
  var pin by remember { mutableStateOf("") }
  var error by remember { mutableStateOf(false) }
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text("المحاسب", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MohasebPrimaryBlue)
    Spacer(modifier = Modifier.height(8.dp))
    Text("أدخل الرمز السري للدخول", fontSize = 14.sp, color = Color(0xFF475569))
    Spacer(modifier = Modifier.height(20.dp))
    OutlinedTextField(
      value = pin,
      onValueChange = { pin = it.filter { c -> c.isDigit() }.take(12); error = false },
      label = { Text("الرمز السري") },
      singleLine = true,
      isError = error,
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
      modifier = Modifier.fillMaxWidth()
    )
    if (error) {
      Text("الرمز غير صحيح", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = {
        if (PinHasher.hash(pin) == expectedHash) onUnlocked() else { error = true; pin = "" }
      },
      modifier = Modifier.fillMaxWidth()
    ) { Text("دخول") }
  }
}
