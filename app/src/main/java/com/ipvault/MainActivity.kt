package com.ipvault
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ipvault.security.VaultCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.UUID
import java.security.SecureRandom
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.net.ssl.HttpsURLConnection

private val Context.vaultStore by preferencesDataStore(name="vault_settings")
private val Green=Color(0xFF20E879); private val Black=Color(0xFF050805); private val Muted=Color(0xFF819187)
private val AllowedIps=stringSetPreferencesKey("allowed_ips"); private val VaultItems=stringSetPreferencesKey("vault_items")
private val EmergencySalt=androidx.datastore.preferences.core.stringPreferencesKey("emergency_salt")
private val EmergencyHash=androidx.datastore.preferences.core.stringPreferencesKey("emergency_hash")

private fun createEmergencyRecord(password:String):Pair<String,String>{
    val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}
    val spec=PBEKeySpec(password.toCharArray(),salt,210000,256)
    val hash=try{SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded}finally{spec.clearPassword()}
    return android.util.Base64.encodeToString(salt,android.util.Base64.NO_WRAP) to android.util.Base64.encodeToString(hash,android.util.Base64.NO_WRAP)
}
private fun checkEmergencyPassword(password:String,saltText:String?,hashText:String?):Boolean=try{
    if(saltText==null||hashText==null)false else{
        val salt=android.util.Base64.decode(saltText,android.util.Base64.NO_WRAP)
        val expected=android.util.Base64.decode(hashText,android.util.Base64.NO_WRAP)
        val spec=PBEKeySpec(password.toCharArray(),salt,210000,256)
        val actual=try{SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded}finally{spec.clearPassword()}
        MessageDigest.isEqual(actual,expected)
    }
}catch(_:Exception){false}

private data class VaultItem(val id:String,val name:String,val mime:String){
 fun encode()=listOf(id,name,mime).joinToString("|"){android.util.Base64.encodeToString(it.toByteArray(),android.util.Base64.NO_WRAP)}
 companion object{fun decode(s:String):VaultItem?=try{val p=s.split("|");VaultItem(String(android.util.Base64.decode(p[0],0)),String(android.util.Base64.decode(p[1],0)),String(android.util.Base64.decode(p[2],0)))}catch(_:Exception){null}}
}
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);setContent{IpVaultApp()}}}
@Composable private fun IpVaultApp(){
 val context=androidx.compose.ui.platform.LocalContext.current;val scope=rememberCoroutineScope()
 val prefs by context.vaultStore.data.collectAsState(initial=androidx.datastore.preferences.core.emptyPreferences())
 var savedWhitelist by remember { mutableStateOf<Set<String>?>(null) };val whitelist=savedWhitelist ?: (prefs[AllowedIps]?:emptySet());val items=(prefs[VaultItems]?:emptySet()).mapNotNull(VaultItem::decode).sortedBy{it.name.lowercase()}
 var ip by remember{mutableStateOf<String?>(null)};var checking by remember{mutableStateOf(true)};var firstRun by remember{mutableStateOf(false)}
 var page by remember{mutableStateOf("home")};var filter by remember{mutableStateOf("ALL")};var message by remember{mutableStateOf<String?>(null)}
 LaunchedEffect(Unit){val stored=withContext(Dispatchers.IO){context.vaultStore.data.first()[AllowedIps]?:emptySet()};savedWhitelist=stored;ip=withContext(Dispatchers.IO){try{val c=URL("https://api.ipify.org").openConnection() as HttpsURLConnection;c.connectTimeout=5000;c.readTimeout=5000;c.inputStream.bufferedReader().use{it.readText().trim()}.also{c.disconnect()}}catch(_:Exception){null}};firstRun=stored.isEmpty();checking=false;if(stored.isNotEmpty()&&(ip==null||ip !in stored))(context as? ComponentActivity)?.finish()}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->if(uri!=null)scope.launch{try{
  val item=withContext(Dispatchers.IO){val name=context.contentResolver.query(uri,null,null,null,null)?.use{c->val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(c.moveToFirst()&&i>=0)c.getString(i)else null}?: "file";val mime=context.contentResolver.getType(uri)?:"application/octet-stream";val id=UUID.randomUUID().toString()+".vault";val input=context.contentResolver.openInputStream(uri)?:error("Cannot read file");VaultCrypto(context).encrypt(input,id);VaultItem(id,name,mime)}
  context.vaultStore.edit{it[VaultItems]=(it[VaultItems]?:emptySet())+item.encode()};message="FILE ENCRYPTED AND SAVED"
 }catch(e:Exception){message="IMPORT ERROR: "+(e.localizedMessage?:"unknown")}}}
 MaterialTheme(colorScheme=darkColorScheme(primary=Green,background=Black,surface=Color(0xFF0D140F),onSurface=Color.White)){Surface(Modifier.fillMaxSize(),color=Black){
 when{
 checking->CenterStatus("IP VAULT","CHECKING NETWORK")
 firstRun->InitialTrustScreen(ip){v->scope.launch{context.vaultStore.edit{it[AllowedIps]=setOf(v)};savedWhitelist=setOf(v);firstRun=false}}
 ip==null||ip !in whitelist->CenterStatus("ACCESS DENIED","CLOSING")
 page=="settings"->WhitelistScreen(whitelist,ip,{page="home"},{v->scope.launch{context.vaultStore.edit{it[AllowedIps]=whitelist+v};savedWhitelist=whitelist+v}},{v->scope.launch{context.vaultStore.edit{it[AllowedIps]=whitelist-v};savedWhitelist=whitelist-v}})
 page=="files"->FilesScreen(items,filter,{filter=it},{page="home"},{picker.launch(arrayOf("*/*"))},{item->scope.launch{try{val temp=withContext(Dispatchers.IO){val safeName=item.name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"),"_").ifBlank{"file"};val out=File(context.cacheDir,"open_"+UUID.randomUUID().toString()+"_"+safeName);out.outputStream().use{VaultCrypto(context).decrypt(File(File(context.filesDir,"vault_encrypted"),item.id),it)};out};val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",temp);val resolvedMime=if(item.mime.isNotBlank()&&item.mime!="application/octet-stream")item.mime else android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(item.name.substringAfterLast('.', "").lowercase()) ?: if(item.mime.startsWith("audio/"))"audio/*" else "*/*";val viewIntent=Intent(Intent.ACTION_VIEW).setDataAndType(uri,resolvedMime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);viewIntent.clipData=android.content.ClipData.newUri(context.contentResolver,item.name,uri);context.startActivity(Intent.createChooser(viewIntent,"Open with"))}catch(e:Exception){message="OPEN ERROR: "+(e.localizedMessage?:"No app can open this file")}}},{item->scope.launch{context.vaultStore.edit{p->p[VaultItems]=(p[VaultItems]?:emptySet()).filterNot{VaultItem.decode(it)?.id==item.id}.toSet()};withContext(Dispatchers.IO){File(File(context.filesDir,"vault_encrypted"),item.id).delete()}}})
 else->ip?.let { currentIp -> HomeScreen(currentIp,{page="settings"},{page="files";filter="ALL"},{page="files";filter="IMAGE"},{page="files";filter="VIDEO"},{page="files";filter="AUDIO"},{page="files";filter="OTHER"}) } ?: CenterStatus("IP VAULT","CHECKING NETWORK")
 }
 message?.let{AlertDialog(onDismissRequest={message=null},confirmButton={TextButton(onClick={message=null}){Text("OK",color=Green)}},text={Text(it,color=Color.White)},containerColor=Color(0xFF101812))}
 }}
}

@Composable private fun CenterStatus(t:String,d:String){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(t,color=Green,fontSize=24.sp,fontFamily=FontFamily.Monospace);Spacer(Modifier.height(8.dp));Text(d,color=Muted,fontSize=10.sp,letterSpacing=2.sp)}}
@Composable private fun InitialTrustScreen(ip:String?,onTrust:(String)->Unit){var value by remember(ip){mutableStateOf(ip.orEmpty())};Column(Modifier.fillMaxSize().padding(26.dp),verticalArrangement=Arrangement.Center){Text("IP VAULT",color=Green,fontSize=27.sp,fontFamily=FontFamily.Monospace);Spacer(Modifier.height(8.dp));Text("INITIAL TRUST SETUP",color=Muted,fontSize=10.sp,letterSpacing=2.sp);Spacer(Modifier.height(28.dp));Text("Add your current public IP to initialize the global whitelist.",color=Color.White);Spacer(Modifier.height(16.dp));OutlinedTextField(value,{value=it},label={Text("Public IP address")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),colors=vaultFieldColors());Spacer(Modifier.height(16.dp));Button(onClick={onTrust(value.trim())},enabled=value.trim().matches(Regex("^[0-9a-fA-F:.]{3,45}$")),colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Black)){Text("TRUST THIS IP")};if(ip==null)Text("Network unavailable. Connect and restart.",color=Muted,modifier=Modifier.padding(top=14.dp))}}
@Composable private fun HomeScreen(ip:String,settings:()->Unit,all:()->Unit,images:()->Unit,videos:()->Unit,audio:()->Unit,other:()->Unit){Column(Modifier.fillMaxSize().padding(start=24.dp,end=24.dp,top=52.dp,bottom=28.dp)){Row(verticalAlignment=Alignment.CenterVertically){Text("IP VAULT",color=Green,fontSize=23.sp,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));IconButton(onClick=settings){Icon(Icons.Outlined.Settings,null,tint=Green)}};Spacer(Modifier.height(32.dp));Text("CONNECTION",color=Muted,fontSize=10.sp,letterSpacing=2.sp);Spacer(Modifier.height(8.dp));Text("AUTHORIZED",color=Green,fontSize=13.sp,fontFamily=FontFamily.Monospace);Spacer(Modifier.height(14.dp));Text("PUBLIC IP",color=Muted,fontSize=10.sp,letterSpacing=2.sp);Text(ip,color=Color.White,fontSize=17.sp,fontFamily=FontFamily.Monospace);Spacer(Modifier.height(30.dp));Text("MY VAULT",color=Muted,fontSize=10.sp,letterSpacing=2.sp);VaultMenuRow("ALL FILES",Icons.Outlined.Folder,all);VaultMenuRow("PHOTOS",Icons.Outlined.Image,images);VaultMenuRow("VIDEO",Icons.Outlined.Videocam,videos);VaultMenuRow("AUDIO",Icons.Outlined.Headphones,audio);VaultMenuRow("DOCUMENTS & OTHER",Icons.Outlined.Description,other)}}
@Composable private fun VaultMenuRow(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,click:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=click).padding(vertical=15.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=Green);Spacer(Modifier.width(14.dp));Text(label,color=Color.White,modifier=Modifier.weight(1f));Icon(Icons.Outlined.ChevronRight,null,tint=Muted)};HorizontalDivider(color=Color(0xFF17231B))}
@Composable private fun FilesScreen(items:List<VaultItem>,filter:String,setFilter:(String)->Unit,back:()->Unit,add:()->Unit,open:(VaultItem)->Unit,delete:(VaultItem)->Unit){
    BackHandler(onBack=back)
    val shown=items.filter{when(filter){"IMAGE"->it.mime.startsWith("image/");"VIDEO"->it.mime.startsWith("video/");"AUDIO"->it.mime.startsWith("audio/");"OTHER"->!(it.mime.startsWith("image/")||it.mime.startsWith("video/")||it.mime.startsWith("audio/"));else->true}}
    val title=when(filter){"IMAGE"->"PHOTOS";"VIDEO"->"VIDEO";"AUDIO"->"AUDIO";"OTHER"->"OTHER FILES";else->"ALL FILES"}
    Column(Modifier.fillMaxSize().padding(start=20.dp,end=20.dp,top=52.dp,bottom=20.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=back){Icon(Icons.Outlined.ArrowBack,null,tint=Green)}
            Text(title,color=Green,fontSize=20.sp,fontFamily=FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            IconButton(onClick=add){Icon(Icons.Outlined.Add,null,tint=Green)}
        }
        Spacer(Modifier.height(12.dp))
        if(shown.isEmpty())Text("NO FILES",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=12.dp))
        LazyColumn{items(shown,key={it.id}){item->
            Row(Modifier.fillMaxWidth().clickable{open(item)}.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically){
                Icon(when{item.mime.startsWith("image/")->Icons.Outlined.Image;item.mime.startsWith("video/")->Icons.Outlined.Videocam;item.mime.startsWith("audio/")->Icons.Outlined.Headphones;else->Icons.Outlined.Description},null,tint=Green)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)){Text(item.name,color=Color.White,maxLines=1);Text(item.mime,color=Muted,fontSize=10.sp)}
                IconButton(onClick={delete(item)}){Icon(Icons.Outlined.DeleteOutline,null,tint=Muted)}
            }
            HorizontalDivider(color=Color(0xFF17231B))
        }}
    }
}
@Composable
private fun WhitelistScreen(
    ips: Set<String>,
    current: String?,
    back: () -> Unit,
    add: (String) -> Unit,
    remove: (String) -> Unit
) {
    var entry by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Outlined.ArrowBack, null, tint = Green) }
            Text("SETTINGS", color = Green, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(28.dp))
        Text("IP WHITELIST", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = entry,
            onValueChange = { entry = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("IPv4 / IPv6") },
            singleLine = true,
            colors = vaultFieldColors()
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { add(entry.trim()); entry = "" },
            enabled = entry.trim().matches(Regex("^[0-9a-fA-F:.]{3,45}$")),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Black)
        ) { Text("ADD IP") }
        Spacer(Modifier.height(20.dp))
        LazyColumn {
            items(ips.sorted()) { address ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(address, color = Color.White, fontFamily = FontFamily.Monospace)
                        Text(if (address == current) "CURRENT IP" else "ALLOWED", color = Green, fontSize = 10.sp)
                    }
                    IconButton(onClick = { remove(address) }) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = Muted)
                    }
                }
                HorizontalDivider(color = Color(0xFF17231B))
            }
        }
    }
}
@Composable private fun vaultFieldColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=Green,unfocusedBorderColor=Muted,focusedLabelColor=Green,cursorColor=Green)
