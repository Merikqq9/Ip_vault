package com.ipvault

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private val Context.vaultStore by preferencesDataStore(name = "vault_settings")
private val Green = Color(0xFF20E879)
private val Black = Color(0xFF050805)
private val Muted = Color(0xFF819187)
private val AllowedIps = stringSetPreferencesKey("allowed_ips")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContent { IpVaultApp() }
    }
}

@Composable
private fun IpVaultApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by context.vaultStore.data.collectAsState(initial = androidx.datastore.preferences.core.emptyPreferences())
    val whitelist = prefs[AllowedIps] ?: emptySet()
    var ip by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(true) }
    var firstRun by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("home") }
    var pickedFiles by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) {
        ip = withContext(Dispatchers.IO) {
            try {
                val c = URL("https://api.ipify.org").openConnection() as HttpsURLConnection
                c.connectTimeout = 5000
                c.readTimeout = 5000
                c.inputStream.bufferedReader().use { it.readText().trim() }.also { c.disconnect() }
            } catch (_: Exception) { null }
        }
        checking = false
        firstRun = whitelist.isEmpty()
        if (!firstRun && (ip == null || ip !in whitelist)) {
            (context as? ComponentActivity)?.finish()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Black, surface = Color(0xFF0D140F), onSurface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Black) {
            when {
                checking -> CenterStatus("IP VAULT", "CHECKING NETWORK")
                firstRun -> InitialTrustScreen(ip) { value ->
                    scope.launch {
                        context.vaultStore.edit { it[AllowedIps] = setOf(value) }
                        firstRun = false
                    }
                }
                ip == null || ip !in whitelist -> CenterStatus("ACCESS DENIED", "CLOSING")
                page == "settings" -> WhitelistScreen(whitelist, ip, { page = "home" }, { value ->
                    scope.launch { context.vaultStore.edit { it[AllowedIps] = whitelist + value } }
                }, { value ->
                    scope.launch { context.vaultStore.edit { it[AllowedIps] = whitelist - value } }
                })
                page == "files" -> FilesScreen(pickedFiles, { page = "home" }) { pickedFiles = pickedFiles + it }
                else -> HomeScreen(ip!!, { page = "settings" }, { page = "files" })
            }
        }
    }
}

@Composable
private fun CenterStatus(title: String, detail: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, color = Green, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Text(detail, color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
    }
}

@Composable
private fun InitialTrustScreen(ip: String?, onTrust: (String) -> Unit) {
    var value by remember(ip) { mutableStateOf(ip.orEmpty()) }
    Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.Center) {
        Text("IP VAULT", color = Green, fontSize = 27.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        Text("INITIAL TRUST SETUP", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(28.dp))
        Text("Add your current public IP to initialize the global whitelist.", color = Color.White)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value, { value = it }, label = { Text("Public IP address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), colors = vaultFieldColors())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onTrust(value.trim()) }, enabled = value.trim().matches(Regex("^[0-9a-fA-F:.]{3,45}$")), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Black)) {
            Text("TRUST THIS IP")
        }
        if (ip == null) Text("Network unavailable. Connect and restart.", color = Muted, modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun HomeScreen(ip: String, settings: () -> Unit, files: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("IP VAULT", color = Green, fontSize = 23.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = settings) { Icon(Icons.Outlined.Settings, null, tint = Green) }
        }
        Spacer(Modifier.height(42.dp))
        Text("CONNECTION", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        Text("AUTHORIZED", color = Green, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(18.dp))
        Text("PUBLIC IP", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Text(ip, color = Color.White, fontSize = 19.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(42.dp))
        Row(Modifier.fillMaxWidth().clickable(onClick = files).padding(vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Folder, null, tint = Green)
            Spacer(Modifier.width(14.dp))
            Text("MY VAULT", color = Color.White, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
        }
        HorizontalDivider(color = Color(0xFF17231B))
        Row(Modifier.fillMaxWidth().clickable(onClick = settings).padding(vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Tune, null, tint = Green)
            Spacer(Modifier.width(14.dp))
            Text("SETTINGS", color = Color.White, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
private fun WhitelistScreen(ips: Set<String>, current: String?, back: () -> Unit, add: (String) -> Unit, remove: (String) -> Unit) {
    var entry by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Outlined.ArrowBack, null, tint = Green) }
            Text("SETTINGS", color = Green, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(28.dp))
        Text("IP WHITELIST", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(entry, { entry = it }, modifier = Modifier.fillMaxWidth(), label = { Text("IPv4 / IPv6") }, singleLine = true, colors = vaultFieldColors())
        Spacer(Modifier.height(8.dp))
        Button(onClick = { add(entry.trim()); entry = "" }, enabled = entry.trim().matches(Regex("^[0-9a-fA-F:.]{3,45}$")), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Black)) { Text("ADD IP") }
        Spacer(Modifier.height(20.dp))
        LazyColumn {
            items(ips.sorted()) { address ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(address, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text(if (address == current) "CURRENT IP" else "ALLOWED", color = Green, fontSize = 10.sp)
                    }
                    IconButton(onClick = { remove(address) }) { Icon(Icons.Outlined.DeleteOutline, null, tint = Muted) }
                }
                HorizontalDivider(color = Color(0xFF17231B))
            }
        }
    }
}

@Composable
private fun FilesScreen(names: List<String>, back: () -> Unit, onPicked: (String) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onPicked(it.lastPathSegment ?: "Selected file") }
    }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Outlined.ArrowBack, null, tint = Green) }
            Text("MY VAULT", color = Green, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { picker.launch(arrayOf("*/*")) }) { Icon(Icons.Outlined.Add, null, tint = Green) }
        }
        Text("LOCAL FILES", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 20.dp, bottom = 12.dp))
        if (names.isEmpty()) Text("No files yet", color = Muted)
        names.forEach { Text(it, color = Color.White, modifier = Modifier.padding(vertical = 10.dp)) }
    }
}

@Composable
private fun vaultFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Green,
    unfocusedBorderColor = Muted,
    focusedLabelColor = Green,
    cursorColor = Green
)
