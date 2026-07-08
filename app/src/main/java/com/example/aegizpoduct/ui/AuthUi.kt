package com.example.aegizpoduct.ui


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aegizpoduct.logic.FirebaseRestClient
import com.example.aegizpoduct.logic.fetchUserFromFirebase
import com.example.aegizpoduct.logic.loginWithEmailPassword
import com.example.aegizpoduct.logic.registerWithEmailPassword
import com.example.aegizpoduct.logic.saveUserToFirebase
import com.example.aegizpoduct.model.AppRole
import com.example.aegizpoduct.model.AppUser
import com.example.aegizpoduct.model.DemoConfig
import com.example.aegizpoduct.session.AppSession
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.aegizpoduct.R
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

@Composable
fun AuthScreen(onLogin: (AppRole) -> Unit, onLoginSuccess: () -> Unit = {}) {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2500)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        LoginScreen(
            onLogin = onLogin,
            onLoginSuccess = onLoginSuccess,
            onBack = { }
        )
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AegizColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_img),
            contentDescription = "Splash Aegis",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun OnboardScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AegizColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(20.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(92.dp),
                shape = CircleShape,
                color = AegizColors.Surface,
                border = BorderStroke(2.dp, AegizColors.Red),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AegizColors.Red, modifier = Modifier.size(46.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Aegiz",
                color = AegizColors.Text,
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "SISTEM PEMANTAUAN SAR IOT & LORA",
                color = AegizColors.Red,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Platform terintegrasi untuk pemantauan keselamatan anggota tim SAR secara real-time melalui sensor vital Garmin dan komunikasi darurat LoRa tanpa jaringan seluler.",
                color = AegizColors.Muted,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OnboardFeatureCard(
                icon = Icons.Default.Bluetooth,
                title = "Relay LoRa & BLE Darurat",
                subtitle = "Komunikasi tetap lancar saat di luar jangkauan sinyal seluler",
            )
            OnboardFeatureCard(
                icon = Icons.Default.Favorite,
                title = "Monitoring Vital Garmin",
                subtitle = "Deteksi Heart Rate, SpO₂, Stress, dan Laju Napas secara langsung",
            )
            OnboardFeatureCard(
                icon = Icons.Default.Warning,
                title = "Tombol SOS & GPS Tracking",
                subtitle = "Kirim sinyal darurat beserta koordinat lokasi presisi dalam 1 klik",
            )
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AegizColors.Red, contentColor = Color.White),
        ) {
            Text("MULAI SEKARANG", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.5.sp)
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardFeatureCard(icon: ImageVector, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AegizColors.Surface),
        border = BorderStroke(1.dp, AegizColors.OutlineSoft),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(AegizColors.Background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = AegizColors.Red, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = AegizColors.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = AegizColors.Muted, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun LoginScreen(onLogin: (AppRole) -> Unit, onLoginSuccess: () -> Unit = {}, onBack: () -> Unit) {
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var fullname by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable { mutableStateOf(AppRole.RESCUER) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val client = remember { FirebaseRestClient() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_img),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Aegiz",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFC62828), Color(0xFF8E0000))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isRegister) "Daftar" else "Masuk",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRegister) "Lengkapi identitas tim SAR Anda" else "Jaga dirimu tetap aman", // Sub-judul
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }

                    if (isRegister) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Nama Lengkap",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedTextField(
                                value = fullname,
                                onValueChange = { fullname = it; error = null },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Masukan Nama Lengkap", color = Color.Gray) },
                                singleLine = true,
                                colors = authFieldColors(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Username",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(if (isRegister) "Masukan Alamat Email" else "Masukan Username", color = Color.Gray) }, // Placeholder dinamis
                            singleLine = true,
                            colors = authFieldColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Password",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Masukan Password", color = Color.Gray) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = authFieldColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    if (isRegister) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Role",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val isRescuer = selectedRole == AppRole.RESCUER
                                Button(
                                    onClick = { selectedRole = AppRole.RESCUER },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRescuer) Color.White else Color.White.copy(alpha = 0.55f),
                                        contentColor = Color.Black
                                    ),
                                    border = if (isRescuer) BorderStroke(2.dp, Color(0xFF8E0000)) else null,
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Rescuer", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                val isPengawas = selectedRole == AppRole.PENANGGUNG_JAWAB
                                Button(
                                    onClick = { selectedRole = AppRole.PENANGGUNG_JAWAB },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPengawas) Color.White else Color.White.copy(alpha = 0.55f),
                                        contentColor = Color.Black
                                    ),
                                    border = if (isPengawas) BorderStroke(2.dp, Color(0xFF8E0000)) else null,
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pengawas", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (error != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(error.orEmpty(), color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (loading) return@Button
                            if (isRegister) {
                                if (fullname.isBlank() || username.isBlank() || password.isBlank()) {
                                    error = "Semua kolom wajib diisi"
                                    return@Button
                                }
                                loading = true
                                scope.launch {
                                    runCatching {
                                        val fbUser = registerWithEmailPassword(username.trim(), password)
                                        val appUser = AppUser(
                                            uid = fbUser.uid,
                                            fullname = fullname.trim(),
                                            email = username.trim(),
                                            role = selectedRole.name,
                                            createdAt = System.currentTimeMillis() / 1000,
                                        )
                                        val saveResult = runCatching { saveUserToFirebase(client, appUser) }
                                        if (saveResult.isFailure) {
                                            runCatching { fbUser.delete() }
                                            throw saveResult.exceptionOrNull() ?: IllegalStateException("Gagal menyimpan profil ke database")
                                        }
                                        AppSession.setUser(appUser.uid, appUser.fullname, appUser.email, selectedRole)
                                        selectedRole
                                    }.onSuccess { role ->
                                        loading = false
                                        onLogin(role)
                                        onLoginSuccess()
                                    }.onFailure { e ->
                                        loading = false
                                        error = e.message ?: "Registrasi gagal. Coba email lain."
                                    }
                                }
                            } else {
                                loading = true
                                val account = DemoConfig.accounts.firstOrNull {
                                    it.username.equals(username.trim(), ignoreCase = true) &&
                                            it.password == password
                                }
                                if (account != null) {
                                    loading = false
                                    AppSession.setUser(account.userId, account.displayName, "${account.username}@aegiz.id", account.role)
                                    onLogin(account.role)
                                    onLoginSuccess()
                                } else {
                                    scope.launch {
                                        runCatching {
                                            val fbUser = loginWithEmailPassword(username.trim(), password)
                                            val appUser = fetchUserFromFirebase(client, fbUser.uid)
                                                ?: error("Profil pengguna tidak ditemukan di database. Silakan daftar ulang.")
                                            val role = runCatching { AppRole.valueOf(appUser.role) }.getOrNull()
                                                ?: error("Peran pengguna tidak valid di database.")
                                            val name = appUser.fullname.takeIf { it.isNotBlank() } ?: fbUser.displayName ?: "Pengguna"
                                            AppSession.setUser(fbUser.uid, name, fbUser.email ?: username.trim(), role)
                                            role
                                        }.onSuccess { role ->
                                            loading = false
                                            onLogin(role)
                                            onLoginSuccess()
                                        }.onFailure { e ->
                                            loading = false
                                            error = e.message ?: "Login gagal. Periksa email dan kata sandi."
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp), // Sudut melengkung tombol utama 12dp
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF262626), // Warna tombol abu-abu sangat gelap (charcoal)
                            contentColor = Color.White
                        )
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isRegister) "Daftar" else "Masuk", // Teks dinamis sesuai mode
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            isRegister = !isRegister
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isRegister) "Sudah punya akun? Masuk" else "Belum punya akun? Daftar", // Teks pemandu dinamis
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(Modifier.height(1.dp).weight(1f).background(Color.White.copy(alpha = 0.25f))) // Garis tipis pembatas kiri
                Text(
                    text = "AKSES DEMO CEPAT",
                    color = Color.White.copy(alpha = 0.65f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(1.dp).weight(1f).background(Color.White.copy(alpha = 0.25f)))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DemoRoleButton("Rescuer (Tim)", Modifier.weight(1f)) {
                    AppSession.setUser(DemoConfig.RESCUER_ID, DemoConfig.RESCUER_NAME, "rescuer01@aegiz.id", AppRole.RESCUER)
                    onLogin(AppRole.RESCUER)
                    onLoginSuccess()
                }
                DemoRoleButton("Posko (PJ)", Modifier.weight(1f)) {
                    AppSession.setUser(DemoConfig.RESPONSIBLE_ID, DemoConfig.RESPONSIBLE_NAME, "penanggungjawab@aegiz.id", AppRole.PENANGGUNG_JAWAB)
                    onLogin(AppRole.PENANGGUNG_JAWAB)
                    onLoginSuccess()
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Sistem Online • Keamanan Terenkripsi", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DemoRoleButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Black.copy(alpha = 0.3f),
            contentColor = Color.White
        ),
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    cursorColor = Color(0xFFC62828)
)

