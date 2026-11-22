package com.ipvault
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
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
import kotlinx.coroutines.delay
import java.io.File
import java.net.URL
import java.util.UUID
import java.security.SecureRandom
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.net.ssl.HttpsURLConnection

private val Context.vaultStore by preferencesDataStore(name="vault_settings")
private var activeVaultLanguage = "en"
private val RussianTranslations = mapOf(
"CHECKING NETWORK" to "ПРОВЕРКА СЕТИ",
"FILE ENCRYPTED AND SAVED" to "ФАЙЛ ЗАШИФРОВАН И СОХРАНЁН",
"INITIAL TRUST SETUP" to "ПЕРВОНАЧАЛЬНАЯ НАСТРОЙКА",
"Add your current public IP to initialize the global whitelist." to "Добавьте текущий публичный IP в белый список.",
"Public IP address" to "Публичный IP-адрес",
"TRUST THIS IP" to "ДОВЕРЯТЬ ЭТОМУ IP",
"Network unavailable. Connect and restart." to "Сеть недоступна. Подключитесь к сети и перезапустите приложение.",
"CONNECTION" to "ПОДКЛЮЧЕНИЕ",
"AUTHORIZED" to "ДОСТУП РАЗРЕШЁН",
"PUBLIC IP" to "ПУБЛИЧНЫЙ IP",
"MY VAULT" to "МОЁ ХРАНИЛИЩЕ",
"ALL FILES" to "ВСЕ ФАЙЛЫ",
"PHOTOS" to "ФОТО",
"VIDEO" to "ВИДЕО",
"AUDIO" to "АУДИО",
"DOCUMENTS & OTHER" to "ДОКУМЕНТЫ И ДРУГОЕ",
"NO FILES" to "НЕТ ФАЙЛОВ",
"OTHER FILES" to "ДРУГИЕ ФАЙЛЫ",
"SETTINGS" to "НАСТРОЙКИ",
"BACKUP PASSWORD" to "РЕЗЕРВНЫЙ ПАРОЛЬ",
"IP WHITELIST" to "БЕЛЫЙ СПИСОК IP",
"SECURITY" to "БЕЗОПАСНОСТЬ",
"CHANGE BACKUP PASSWORD" to "ИЗМЕНИТЬ РЕЗЕРВНЫЙ ПАРОЛЬ",
"Confirm your current password before setting a new one." to "Подтвердите текущий пароль, чтобы задать новый.",
"Current password" to "Текущий пароль",
"New password (8+ characters)" to "Новый пароль (не менее 8 символов)",
"Repeat new password" to "Повторите новый пароль",
"UPDATE PASSWORD" to "ИЗМЕНИТЬ ПАРОЛЬ",
"Manage IP addresses allowed to access your vault." to "Управляйте IP-адресами, которым разрешён доступ к хранилищу.",
"IPv4 / IPv6" to "IPv4 / IPv6",
"ADD IP" to "ДОБАВИТЬ IP",
"CURRENT IP" to "ТЕКУЩИЙ IP",
"ALLOWED" to "РАЗРЕШЁН",
"AUTHOR" to "АВТОР",
"LANGUAGE" to "ЯЗЫК",
"RUSSIAN" to "РУССКИЙ",
"ENGLISH" to "АНГЛИЙСКИЙ",
"EMERGENCY ACCESS" to "АВАРИЙНЫЙ ДОСТУП",
"Create a backup password. You will use it when connecting from an IP address that is not on your whitelist." to "Создайте резервный пароль. Он понадобится для входа с IP-адреса, которого нет в белом списке.",
"Backup password" to "Резервный пароль",
"SAVE BACKUP PASSWORD" to "СОХРАНИТЬ РЕЗЕРВНЫЙ ПАРОЛЬ",
"Use at least 8 characters." to "Используйте не менее 8 символов.",
"Passwords do not match." to "Пароли не совпадают.",
"Enter your backup password to unlock IP VAULT." to "Введите резервный пароль для разблокировки IP VAULT.",
"CANCEL" to "ОТМЕНА",
"UNLOCK" to "РАЗБЛОКИРОВАТЬ",
"ACCESS DENIED" to "ДОСТУП ЗАПРЕЩЁН",
"This IP address is not authorized." to "Этому IP-адресу не разрешён доступ.",
"IP VAULT // LOCKED" to "IP VAULT // ЗАБЛОКИРОВАНО",
"TOO MANY ATTEMPTS. LOCKED FOR 60 SECONDS" to "СЛИШКОМ МНОГО ПОПЫТОК. БЛОКИРОВКА НА 60 СЕКУНД",
"INCORRECT CURRENT PASSWORD" to "НЕВЕРНЫЙ ТЕКУЩИЙ ПАРОЛЬ",
"EMERGENCY PASSWORD UPDATED" to "РЕЗЕРВНЫЙ ПАРОЛЬ ИЗМЕНЁН",
"Open with" to "Открыть с помощью",
"OK" to "ОК",
    "IMPORT ERROR: " to "ОШИБКА ИМПОРТА: ",
    "OPEN ERROR: " to "ОШИБКА ОТКРЫТИЯ: ",
    "No app can open this file" to "Нет приложения для открытия этого файла",
    "TOO MANY ATTEMPTS. TRY AGAIN IN " to "СЛИШКОМ МНОГО ПОПЫТОК. ПОВТОРИТЕ ЧЕРЕЗ ",
    " SECONDS" to " СЕК.",
    "INCORRECT PASSWORD. ATTEMPT " to "НЕВЕРНЫЙ ПАРОЛЬ. ПОПЫТКА ",
    " OF 5" to " ИЗ 5",
    "WELCOME TO IP VAULT" to "ПРИВЕТСТВУЕМ В IP VAULT",
    "DEVICE STORAGE" to "ПАМЯТЬ УСТРОЙСТВА",
    "FREE" to "СВОБОДНО"
)
private fun tr(value:String):String =
    if (activeVaultLanguage == "ru") RussianTranslations[value] ?: value else value

private val LanguageKey=androidx.datastore.preferences.core.stringPreferencesKey("app_language")

private val VaultFont = FontFamily(Font(R.font.monocraft))
private fun minecraftTypography(): Typography {
    val base = Typography()
    fun pixel(style: TextStyle) = style.copy(fontFamily = VaultFont)
    return base.copy(
        displayLarge = pixel(base.displayLarge),
        displayMedium = pixel(base.displayMedium),
        displaySmall = pixel(base.displaySmall),
        headlineLarge = pixel(base.headlineLarge),
        headlineMedium = pixel(base.headlineMedium),
        headlineSmall = pixel(base.headlineSmall),
        titleLarge = pixel(base.titleLarge),
        titleMedium = pixel(base.titleMedium),
        titleSmall = pixel(base.titleSmall),
        bodyLarge = pixel(base.bodyLarge),
        bodyMedium = pixel(base.bodyMedium),
        bodySmall = pixel(base.bodySmall),
        labelLarge = pixel(base.labelLarge),
        labelMedium = pixel(base.labelMedium),
        labelSmall = pixel(base.labelSmall)
    )
}

private val Green=Color(0xFF20E879); private val Black=Color(0xFF050805); private val Muted=Color(0xFF819187)
private val AllowedIps=stringSetPreferencesKey("allowed_ips"); private val VaultItems=stringSetPreferencesKey("vault_items")
private val EmergencySalt=androidx.datastore.preferences.core.stringPreferencesKey("emergency_salt")
private val EmergencyHash=androidx.datastore.preferences.core.stringPreferencesKey("emergency_hash")
private val EmergencyFailures=androidx.datastore.preferences.core.intPreferencesKey("emergency_failures")
private val EmergencyLockUntil=androidx.datastore.preferences.core.longPreferencesKey("emergency_lock_until")

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
 var ip by remember{mutableStateOf<String?>(null)};var checking by remember{mutableStateOf(true)};var firstRun by remember{mutableStateOf(false)};var setupEmergency by rememberSaveable{mutableStateOf(false)};var emergencyUnlocked by rememberSaveable{mutableStateOf(false)};var showEmergencyPassword by rememberSaveable{mutableStateOf(false)}
 var page by rememberSaveable{mutableStateOf("home")};var welcomeDone by rememberSaveable{mutableStateOf(false)};var language by rememberSaveable{mutableStateOf("en")};var filter by remember{mutableStateOf("ALL")};var message by remember{mutableStateOf<String?>(null)};var emergencyPasswordResetKey by rememberSaveable{mutableIntStateOf(0)}
 LaunchedEffect(Unit){val storedPrefs=withContext(Dispatchers.IO){context.vaultStore.data.first()};val stored=storedPrefs[AllowedIps]?:emptySet();language=storedPrefs[LanguageKey]?: "en";activeVaultLanguage=language;savedWhitelist=stored;ip=withContext(Dispatchers.IO){try{val c=URL("https://api.ipify.org").openConnection() as HttpsURLConnection;c.connectTimeout=5000;c.readTimeout=5000;c.inputStream.bufferedReader().use{it.readText().trim()}.also{c.disconnect()}}catch(_:Exception){null}};firstRun=stored.isEmpty();setupEmergency=stored.isNotEmpty()&&storedPrefs[EmergencyHash]==null;checking=false}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->if(uri!=null)scope.launch{try{
  val item=withContext(Dispatchers.IO){val name=context.contentResolver.query(uri,null,null,null,null)?.use{c->val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(c.moveToFirst()&&i>=0)c.getString(i)else null}?: "file";val mime=context.contentResolver.getType(uri)?:"application/octet-stream";val id=UUID.randomUUID().toString()+".vault";val input=context.contentResolver.openInputStream(uri)?:error("Cannot read file");VaultCrypto(context).encrypt(input,id);VaultItem(id,name,mime)}
  context.vaultStore.edit{it[VaultItems]=(it[VaultItems]?:emptySet())+item.encode()};message=tr("FILE ENCRYPTED AND SAVED")
 }catch(e:Exception){message=tr("IMPORT ERROR: ")+(e.localizedMessage?:"unknown")}}}
 MaterialTheme(colorScheme=darkColorScheme(primary=Green,background=Black,surface=Color(0xFF0D140F),onSurface=Color.White), typography=minecraftTypography()){Surface(Modifier.fillMaxSize(),color=Black){
 Box(Modifier.fillMaxSize()) {
 when{
 checking->CenterStatus("IP VAULT",tr("CHECKING NETWORK"))
 firstRun->InitialTrustScreen(ip){v->scope.launch{context.vaultStore.edit{it[AllowedIps]=setOf(v)};savedWhitelist=setOf(v);firstRun=false;setupEmergency=true}}
 setupEmergency&&ip!=null&&ip in whitelist->EmergencyPasswordSetupScreen{password->scope.launch{val record=withContext(Dispatchers.Default){createEmergencyRecord(password)};context.vaultStore.edit{it[EmergencySalt]=record.first;it[EmergencyHash]=record.second};setupEmergency=false}}
 !welcomeDone&&!checking&&!firstRun&&!setupEmergency&&((ip!=null&&ip in whitelist)||emergencyUnlocked)->WelcomeScreen{welcomeDone=true}
 (ip==null||ip !in whitelist)&&!emergencyUnlocked->if(showEmergencyPassword)EmergencyPasswordEntryScreen(resetKey=emergencyPasswordResetKey,onCancel={showEmergencyPassword=false}){password->scope.launch{val p=context.vaultStore.data.first();val now=System.currentTimeMillis();val lockedUntil=p[EmergencyLockUntil]?:0L;if(now<lockedUntil){message=tr("TOO MANY ATTEMPTS. TRY AGAIN IN ")+((lockedUntil-now+999)/1000)+tr(" SECONDS")}else{val valid=withContext(Dispatchers.Default){checkEmergencyPassword(password,p[EmergencySalt],p[EmergencyHash])};if(valid){context.vaultStore.edit{it[EmergencyFailures]=0;it[EmergencyLockUntil]=0L};emergencyUnlocked=true;showEmergencyPassword=false;page="home"}else{val failures=(p[EmergencyFailures]?:0)+1;context.vaultStore.edit{it[EmergencyFailures]=if(failures>=5)0 else failures;if(failures>=5)it[EmergencyLockUntil]=now+60000L};emergencyPasswordResetKey++;message=if(failures>=5)tr("TOO MANY ATTEMPTS. LOCKED FOR 60 SECONDS") else tr("INCORRECT PASSWORD. ATTEMPT ")+failures+tr(" OF 5")}}}}else AccessDeniedGestureScreen{showEmergencyPassword=true}
 page=="settings"->WhitelistScreen(whitelist,ip,language,{selected->language=selected;activeVaultLanguage=selected;scope.launch{context.vaultStore.edit{it[LanguageKey]=selected}}},{page="home"},{v->scope.launch{context.vaultStore.edit{it[AllowedIps]=whitelist+v};savedWhitelist=whitelist+v}},{v->scope.launch{context.vaultStore.edit{it[AllowedIps]=whitelist-v};savedWhitelist=whitelist-v}},{old,new->scope.launch{val p=context.vaultStore.data.first();val valid=withContext(Dispatchers.Default){checkEmergencyPassword(old,p[EmergencySalt],p[EmergencyHash])};if(valid){val record=withContext(Dispatchers.Default){createEmergencyRecord(new)};context.vaultStore.edit{it[EmergencySalt]=record.first;it[EmergencyHash]=record.second};message=tr("EMERGENCY PASSWORD UPDATED")}else message=tr("INCORRECT CURRENT PASSWORD")}})
 page=="files"->FilesScreen(items,filter,{filter=it},{page="home"},{picker.launch(arrayOf("*/*"))},{item->scope.launch{try{val temp=withContext(Dispatchers.IO){val safeName=item.name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"),"_").ifBlank{"file"};val out=File(context.cacheDir,"open_"+UUID.randomUUID().toString()+"_"+safeName);out.outputStream().use{VaultCrypto(context).decrypt(File(File(context.filesDir,"vault_encrypted"),item.id),it)};out};val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",temp);val resolvedMime=if(item.mime.isNotBlank()&&item.mime!="application/octet-stream")item.mime else android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(item.name.substringAfterLast('.', "").lowercase()) ?: if(item.mime.startsWith("audio/"))"audio/*" else "*/*";val viewIntent=Intent(Intent.ACTION_VIEW).setDataAndType(uri,resolvedMime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);viewIntent.clipData=android.content.ClipData.newUri(context.contentResolver,item.name,uri);context.startActivity(Intent.createChooser(viewIntent,"Open with"))}catch(e:Exception){message=tr("OPEN ERROR: ")+(e.localizedMessage?:tr("No app can open this file"))}}},{item->scope.launch{context.vaultStore.edit{p->p[VaultItems]=(p[VaultItems]?:emptySet()).filterNot{VaultItem.decode(it)?.id==item.id}.toSet()};withContext(Dispatchers.IO){File(File(context.filesDir,"vault_encrypted"),item.id).delete()}}})
 else->HomeScreen(ip?: "UNVERIFIED",{page="settings"},{page="files";filter="ALL"},{page="files";filter="IMAGE"},{page="files";filter="VIDEO"},{page="files";filter="AUDIO"},{page="files";filter="OTHER"})
 }
 TypingNotice(message=message,onFinished={message=null},modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=28.dp))
 }
 }}
}


@Composable private fun WelcomeScreen(onFinished:()->Unit){
    val greeting=tr("WELCOME TO IP VAULT")
    var visibleCount by remember { mutableStateOf(0) }
    LaunchedEffect(greeting){
        delay(250)
        for(i in 1..greeting.length){visibleCount=i;delay(42)}
        delay(850)
        for(i in greeting.length downTo 0){visibleCount=i;delay(24)}
        delay(180)
        onFinished()
    }
    Box(Modifier.fillMaxSize().background(Black),contentAlignment=Alignment.Center){
        Column(horizontalAlignment=Alignment.CenterHorizontally){
            Text(greeting.take(visibleCount),color=Green,fontSize=22.sp,fontFamily=VaultFont,fontWeight=FontWeight.Medium,letterSpacing=0.5.sp)
            Spacer(Modifier.height(12.dp))
            Text("IP VAULT  •  SECURE LOCAL STORAGE",color=Muted,fontSize=10.sp,fontFamily=VaultFont,letterSpacing=1.sp)
        }
    }
}
@Composable private fun AsciiEarth(){
    var frame by remember { mutableStateOf(0) }
    LaunchedEffect(Unit){while(true){frame=(frame+1)%48;delay(95)}}
    Text(
        text=asciiEarthFrame(frame*0.1308997f),
        color=Green,
        fontFamily=VaultFont,
        fontSize=5.sp,
        lineHeight=5.sp,
        letterSpacing=0.sp
    )
}
private fun asciiEarthFrame(rotation:Float):String{
    // Dense character grid with corrected glyph aspect ratio for a round sphere.
    val rows=25
    val cols=41
    val xRadius=19.5f
    val yRadius=12f
    return buildString {
        for(y in 0 until rows){
            for(x in 0 until cols){
                val nx=(x-(cols-1)/2f)/xRadius
                val ny=(y-(rows-1)/2f)/yRadius
                val radius=nx*nx+ny*ny
                if(radius>1f){append(' ');continue}
                val nz=kotlin.math.sqrt((1f-radius).coerceAtLeast(0f))
                val lon=kotlin.math.atan2(nx,nz)+rotation
                val lat=kotlin.math.asin(ny.coerceIn(-1f,1f))
                val land=(
                    kotlin.math.sin(lon*2.1f+lat*1.4f)+
                    kotlin.math.cos(lon*3.3f-lat*2.2f)+
                    kotlin.math.sin(lon*4.7f+lat*3.1f)+
                    kotlin.math.cos(lon*7.2f-lat*4.4f)
                )*0.25f
                val light=(nx * -0.35f + ny * -0.25f + nz * 0.9f).coerceIn(0f,1f)
                val ch=when{
                    land>0.30f && light>0.35f -> "@%#"[(x+y+kotlin.math.abs((rotation*5).toInt()))%3]
                    light<0.22f -> ':'
                    light<0.42f -> '.'
                    land>0.18f -> "##%@"[(x*3+y*5)%4]
                    else -> " .:-=+*"[(light*7f).toInt().coerceIn(0,6)]
                }
                append(ch)
            }
            if(y<rows-1)append('\n')
        }
    }
}
@Composable private fun DeviceStorageBadge(){
    val stats=remember { runCatching { StatFs(Environment.getDataDirectory().absolutePath) }.getOrNull() }
    val total=stats?.totalBytes?:0L
    val free=stats?.availableBytes?:0L
    val used=(total-free).coerceAtLeast(0L)
    val fraction=if(total>0L)used.toFloat()/total.toFloat() else 0f
    fun format(bytes:Long):String{
        val gb=bytes/(1024.0*1024.0*1024.0)
        return if(gb>=10) "%.0f GB".format(java.util.Locale.US,gb) else "%.1f GB".format(java.util.Locale.US,gb)
    }
    Column(Modifier.width(142.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF0D140F)).border(1.dp,Color(0xFF203629),RoundedCornerShape(10.dp)).padding(10.dp)){
        Text(tr("DEVICE STORAGE"),color=Muted,fontSize=8.sp,letterSpacing=1.sp)
        Spacer(Modifier.height(5.dp))
        Text("${format(used)} / ${format(total)}",color=Color.White,fontSize=11.sp,fontFamily=VaultFont)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF26352B))){
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f,1f)).fillMaxHeight().background(Green))
        }
        Spacer(Modifier.height(5.dp))
        Text("${tr("FREE")}: ${format(free)}",color=Muted,fontSize=9.sp)
    }
}

@Composable private fun CenterStatus(t:String,d:String){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(t,color=Green,fontSize=24.sp,fontFamily=VaultFont);Spacer(Modifier.height(8.dp));Text(d,color=Muted,fontSize=10.sp,letterSpacing=2.sp)}}
@Composable private fun InitialTrustScreen(ip:String?,onTrust:(String)->Unit){var value by remember(ip){mutableStateOf(ip.orEmpty())};Column(Modifier.fillMaxSize().padding(26.dp),verticalArrangement=Arrangement.Center){Text("IP VAULT",color=Green,fontSize=27.sp,fontFamily=VaultFont);Spacer(Modifier.height(8.dp));Text(tr("INITIAL TRUST SETUP"),color=Muted,fontSize=10.sp,letterSpacing=2.sp);Spacer(Modifier.height(28.dp));Text(tr("Add your current public IP to initialize the global whitelist."),color=Color.White);Spacer(Modifier.height(16.dp));OutlinedTextField(value,{value=it},label={Text(tr("Public IP address"))},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii),colors=vaultFieldColors());Spacer(Modifier.height(16.dp));Button(onClick={onTrust(value.trim())},enabled=value.trim().matches(Regex("^[0-9a-fA-F:.]{3,45}$")),colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Black),shape=RoundedCornerShape(10.dp),border=BorderStroke(1.dp,Green.copy(alpha=.45f)),elevation=ButtonDefaults.buttonElevation(defaultElevation=4.dp,pressedElevation=1.dp)){Text(tr("TRUST THIS IP"))};if(ip==null)Text("Network unavailable. Connect and restart.",color=Muted,modifier=Modifier.padding(top=14.dp))}}
@Composable private fun PixelSettingsIcon(){
    Canvas(Modifier.size(25.dp)) {
        val cell = size.minDimension / 8f
        val blocks = listOf(
            2 to 0, 3 to 0, 4 to 0, 5 to 0,
            0 to 2, 1 to 2, 6 to 2, 7 to 2,
            0 to 3, 7 to 3, 0 to 4, 7 to 4,
            1 to 5, 6 to 5, 2 to 6, 5 to 6,
            3 to 7, 4 to 7
        )
        blocks.forEach { (x,y) ->
            drawRect(Green, Offset(x * cell, y * cell), androidx.compose.ui.geometry.Size(cell * 1.05f, cell * 1.05f))
        }
        drawRect(Green, Offset(2.5f * cell, 2.5f * cell), androidx.compose.ui.geometry.Size(3f * cell, 3f * cell))
        drawRect(Black, Offset(3.5f * cell, 3.5f * cell), androidx.compose.ui.geometry.Size(cell, cell))
    }
}
@Composable private fun HomeScreen(ip:String,settings:()->Unit,all:()->Unit,images:()->Unit,videos:()->Unit,audio:()->Unit,other:()->Unit){
    var activeRow by remember { mutableIntStateOf(-1) }
    val rowBounds = remember { mutableStateListOf<Rect>() }
    Column(
        Modifier.fillMaxSize().padding(start=24.dp,end=24.dp,top=52.dp,bottom=28.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { point -> activeRow = rowBounds.indexOfFirst { it.contains(point) } },
                    onDrag = { change, _ ->
                        activeRow = rowBounds.indexOfFirst { it.contains(change.position) }
                        change.consume()
                    },
                    onDragEnd = { activeRow = -1 },
                    onDragCancel = { activeRow = -1 }
                )
            }
    ){
        Row(verticalAlignment=Alignment.CenterVertically){Text("IP VAULT",color=Green,fontSize=23.sp,fontFamily=VaultFont,fontWeight=FontWeight.SemiBold);Spacer(Modifier.weight(1f));IconButton(onClick=settings){PixelSettingsIcon()}}
        Spacer(Modifier.height(32.dp))
        Text(tr("CONNECTION"),color=Muted,fontSize=10.sp,letterSpacing=2.sp)
        Spacer(Modifier.height(8.dp))
        Text(tr("AUTHORIZED"),color=Green,fontSize=13.sp,fontFamily=VaultFont)
        Spacer(Modifier.height(14.dp))
        Text(tr("PUBLIC IP"),color=Muted,fontSize=10.sp,letterSpacing=2.sp)
        AnimatedIpText(ip)
        Spacer(Modifier.height(30.dp))
        Text(tr("MY VAULT"),color=Muted,fontSize=10.sp,letterSpacing=2.sp)
        val actions = listOf(all,images,videos,audio,other)
        val labels = listOf("ALL FILES","PHOTOS","VIDEO","AUDIO","DOCUMENTS & OTHER")
        val icons = listOf(Icons.Outlined.Folder,Icons.Outlined.Image,Icons.Outlined.Videocam,Icons.Outlined.Headphones,Icons.Outlined.Description)
        labels.forEachIndexed { index, label ->
            VaultMenuRow(
                label=tr(label), icon=icons[index], click=actions[index], bump=activeRow==index,
                onBounds={ rect ->
                    if(rowBounds.size<=index) {
                        while(rowBounds.size<=index) rowBounds.add(Rect.Zero)
                    }
                    rowBounds[index]=rect
                }
            )
        }
    }
}

@Composable private fun AnimatedIpText(ip:String){
    var count by remember(ip){mutableIntStateOf(0)}
    LaunchedEffect(ip){
        count=0
        for(i in 1..ip.length){count=i;delay(115)}
    }
    Text(ip.take(count),color=Color.White,fontSize=17.sp,fontFamily=VaultFont)
}

@Composable private fun VaultMenuRow(
    label:String,
    icon:androidx.compose.ui.graphics.vector.ImageVector,
    click:()->Unit,
    bump:Boolean=false,
    onBounds:(Rect)->Unit={}
){
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (bump || pressed) 1.055f else 1f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 360f),
        label = "vaultMenuScale"
    )
    val lift by animateFloatAsState(
        targetValue = if (bump || pressed) 12f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 340f),
        label = "vaultMenuLift"
    )
    val offset by animateFloatAsState(
        targetValue = if (bump || pressed) (-8f) else 0f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 300f),
        label = "vaultMenuBounce"
    )
    Row(
        Modifier.fillMaxWidth().padding(vertical=5.dp)
            .onGloballyPositioned { onBounds(it.boundsInParent()) }
            .graphicsLayer { translationY=offset; scaleX=scale; scaleY=scale }
            .shadow(lift.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(if (bump || pressed) Color(0xFF14231A) else Color(0xFF0D140F))
            .border(1.dp, if (bump || pressed) Green.copy(alpha=.8f) else Color(0xFF203629), RoundedCornerShape(12.dp))
            .clickable(interactionSource=interaction, indication=null, onClick=click)
            .padding(horizontal=14.dp,vertical=15.dp),
        verticalAlignment=Alignment.CenterVertically
    ){
        Icon(icon,null,tint=Green)
        Spacer(Modifier.width(14.dp))
        Text(label,color=Color.White,modifier=Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight,null,tint=Muted)
    }
}
@Composable private fun FilesScreen(items:List<VaultItem>,filter:String,setFilter:(String)->Unit,back:()->Unit,add:()->Unit,open:(VaultItem)->Unit,delete:(VaultItem)->Unit){
    BackHandler(onBack=back)
    val shown=items.filter{when(filter){"IMAGE"->it.mime.startsWith("image/");"VIDEO"->it.mime.startsWith("video/");"AUDIO"->it.mime.startsWith("audio/");"OTHER"->!(it.mime.startsWith("image/")||it.mime.startsWith("video/")||it.mime.startsWith("audio/"));else->true}}
    val title=when(filter){"IMAGE"->"PHOTOS";"VIDEO"->"VIDEO";"AUDIO"->"AUDIO";"OTHER"->"OTHER FILES";else->"ALL FILES"}
    Column(Modifier.fillMaxSize().padding(start=20.dp,end=20.dp,top=52.dp,bottom=20.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            IconButton(onClick=back){Icon(Icons.Outlined.ArrowBack,null,tint=Green)}
            Text(tr(title),color=Green,fontSize=20.sp,fontFamily=VaultFont)
            Spacer(Modifier.weight(1f))
            IconButton(onClick=add){Icon(Icons.Outlined.Add,null,tint=Green)}
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){AsciiEarth();DeviceStorageBadge()}
        Spacer(Modifier.height(12.dp))
        if(shown.isEmpty())Text(tr("NO FILES"),color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=12.dp))
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
    ips: Set<String>, current: String?, language: String, setLanguage: (String) -> Unit, back: () -> Unit,
    add: (String) -> Unit, remove: (String) -> Unit,
    changePassword: (String,String) -> Unit
) {
    var section by rememberSaveable { mutableStateOf("main") }
    var entry by remember { mutableStateOf("") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    BackHandler(enabled = section != "main") { section = "main" }
    Column(Modifier.fillMaxSize().padding(start=24.dp,end=24.dp,top=52.dp,bottom=24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (section == "main") back() else section = "main" }) {
                Icon(Icons.Outlined.ArrowBack, null, tint = Green)
            }
            Text(when(section) { "password" -> tr("BACKUP PASSWORD"); "ips" -> tr("IP WHITELIST"); "language" -> tr("LANGUAGE"); else -> tr("SETTINGS") },
                color = Green, fontSize = 20.sp, fontFamily = VaultFont)
        }
        Spacer(Modifier.height(24.dp))
        when(section) {
            "main" -> {
                Text(tr("SECURITY"), color=Muted, fontSize=10.sp, letterSpacing=2.sp)
                Spacer(Modifier.height(12.dp))
                VaultMenuRow(tr("CHANGE BACKUP PASSWORD"), Icons.Outlined.Lock, click = { section="password" })
                VaultMenuRow(tr("IP WHITELIST"), Icons.Outlined.Shield, click = { section="ips" })
                VaultMenuRow(tr("LANGUAGE"), Icons.Outlined.Language, click = { section="language" })
                Spacer(Modifier.weight(1f))
                HorizontalDivider(color=Color(0xFF17231B))
                Spacer(Modifier.height(14.dp))
                Text(tr("AUTHOR"), color=Muted, fontSize=10.sp, letterSpacing=2.sp)
                Spacer(Modifier.height(5.dp))
                Text("Merik", color=Color.White, fontSize=14.sp, fontFamily=VaultFont)
            }
            "password" -> {
                var activePasswordField by rememberSaveable { mutableStateOf("current") }
                var textMode by rememberSaveable { mutableStateOf(false) }
                fun appendPasswordDigit(digit: String) {
                    when (activePasswordField) {
                        "current" -> if (oldPassword.length < 8) { oldPassword += digit; if(oldPassword.length==8) activePasswordField="new" }
                        "new" -> if (newPassword.length < 8) { newPassword += digit; if(newPassword.length==8) activePasswordField="confirm" }
                        else -> if (confirmPassword.length < 8) confirmPassword += digit
                    }
                }
                fun erasePasswordDigit() {
                    when (activePasswordField) {
                        "current" -> if (oldPassword.isNotEmpty()) oldPassword = oldPassword.dropLast(1)
                        "new" -> if (newPassword.isNotEmpty()) newPassword = newPassword.dropLast(1)
                        else -> if (confirmPassword.isNotEmpty()) confirmPassword = confirmPassword.dropLast(1)
                    }
                }
                Text(tr("Confirm your current password before setting a new one."), color=Muted, fontSize=12.sp)
                Spacer(Modifier.height(12.dp))
                if (textMode) {
                    OutlinedTextField(oldPassword,{oldPassword=it},modifier=Modifier.fillMaxWidth(),label={Text(tr("Current password"))},singleLine=true,visualTransformation=PasswordVisualTransformation(),colors=vaultFieldColors())
                    Spacer(Modifier.height(7.dp))
                    OutlinedTextField(newPassword,{newPassword=it},modifier=Modifier.fillMaxWidth(),label={Text(tr("New password (8+ characters)"))},singleLine=true,visualTransformation=PasswordVisualTransformation(),colors=vaultFieldColors())
                    Spacer(Modifier.height(7.dp))
                    OutlinedTextField(confirmPassword,{confirmPassword=it},modifier=Modifier.fillMaxWidth(),label={Text(tr("Repeat new password"))},singleLine=true,visualTransformation=PasswordVisualTransformation(),colors=vaultFieldColors())
                    TextButton(onClick={textMode=false}) { Text("123",color=Green,fontFamily=VaultFont) }
                } else {
                    @Composable fun passwordSlot(value:String,label:String,field:String) {
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value="• ".repeat(value.length).trim(), onValueChange={}, readOnly=true,
                                label={Text(tr(label))}, singleLine=true,
                                colors=vaultFieldColors(activePasswordField==field),
                                textStyle=TextStyle(fontFamily=VaultFont,color=if(activePasswordField==field)Green else Muted,letterSpacing=2.sp),
                                modifier=Modifier.fillMaxWidth().then(if(activePasswordField==field)Modifier.shadow(7.dp,RoundedCornerShape(6.dp),ambientColor=Green.copy(alpha=.35f)) else Modifier)
                            )
                            Box(Modifier.matchParentSize().clickable { activePasswordField=field })
                        }
                    }
                    passwordSlot(oldPassword,"Current password","current")
                    Spacer(Modifier.height(6.dp))
                    passwordSlot(newPassword,"New password (8+ characters)","new")
                    Spacer(Modifier.height(6.dp))
                    passwordSlot(confirmPassword,"Repeat new password","confirm")
                    Spacer(Modifier.height(9.dp))
                    val keypadRows=listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"))
                    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        keypadRows.forEach { row ->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                                row.forEach { digit ->
                                    Button(onClick={appendPasswordDigit(digit)},modifier=Modifier.weight(1f).height(43.dp),
                                        shape=androidx.compose.foundation.shape.CircleShape,
                                        colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF101B14),contentColor=Green),
                                        border=BorderStroke(1.dp,Green.copy(alpha=.55f)),
                                        elevation=ButtonDefaults.buttonElevation(defaultElevation=3.dp,pressedElevation=0.dp)
                                    ) { Text(digit,fontFamily=VaultFont,fontSize=18.sp) }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                            OutlinedButton(onClick={textMode=true},modifier=Modifier.weight(1f).height(43.dp),
                                shape=androidx.compose.foundation.shape.CircleShape,border=BorderStroke(1.dp,Muted.copy(alpha=.55f))
                            ) { Text("ABC",fontFamily=VaultFont,color=Muted,fontSize=11.sp) }
                            Button(onClick={appendPasswordDigit("0")},modifier=Modifier.weight(1f).height(43.dp),
                                shape=androidx.compose.foundation.shape.CircleShape,
                                colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF101B14),contentColor=Green),
                                border=BorderStroke(1.dp,Green.copy(alpha=.55f))
                            ) { Text("0",fontFamily=VaultFont,fontSize=18.sp) }
                            OutlinedButton(onClick={erasePasswordDigit()},modifier=Modifier.weight(1f).height(43.dp),
                                shape=androidx.compose.foundation.shape.CircleShape,border=BorderStroke(1.dp,Muted.copy(alpha=.55f))
                            ) { Text("⌫",fontFamily=VaultFont,color=Muted,fontSize=17.sp) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(onClick={changePassword(oldPassword,newPassword);oldPassword="";newPassword="";confirmPassword="";activePasswordField="current"},
                    enabled=oldPassword.isNotEmpty()&&newPassword.length>=8&&newPassword==confirmPassword,
                    colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Black),shape=RoundedCornerShape(10.dp),
                    border=BorderStroke(1.dp,Green.copy(alpha=.45f)),
                    elevation=ButtonDefaults.buttonElevation(defaultElevation=4.dp,pressedElevation=1.dp)
                ) { Text(tr("UPDATE PASSWORD")) }
            }
            "language" -> {
                Text(tr("LANGUAGE"), color=Muted, fontSize=10.sp, letterSpacing=2.sp)
                Spacer(Modifier.height(12.dp))
                VaultMenuRow(tr("RUSSIAN"), Icons.Outlined.Language, click = { setLanguage("ru") })
                VaultMenuRow(tr("ENGLISH"), Icons.Outlined.Language, click = { setLanguage("en") })
            }
            "ips" -> {
                Text(tr("Manage IP addresses allowed to access your vault."), color=Muted, fontSize=12.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value=entry,onValueChange={entry=it},modifier=Modifier.fillMaxWidth(),label={Text(tr("IPv4 / IPv6"))},singleLine=true,colors=vaultFieldColors())
                Spacer(Modifier.height(8.dp))
                Button(onClick={add(entry.trim());entry=""},enabled=entry.trim().matches(Regex("^[0-9a-fA-F:.]{3,45}$")),
                    colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Black),shape=RoundedCornerShape(10.dp),border=BorderStroke(1.dp,Green.copy(alpha=.45f)),elevation=ButtonDefaults.buttonElevation(defaultElevation=4.dp,pressedElevation=1.dp)){Text(tr("ADD IP"))}
                Spacer(Modifier.height(18.dp))
                LazyColumn(Modifier.weight(1f)) {
                    items(ips.sorted()) { address ->
                        Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(address,color=Color.White,fontFamily=VaultFont)
                                Text(tr(if(address==current)"CURRENT IP" else "ALLOWED"),color=Green,fontSize=10.sp)
                            }
                            IconButton(onClick={remove(address)}){Icon(Icons.Outlined.DeleteOutline,null,tint=Muted)}
                        }
                        HorizontalDivider(color=Color(0xFF17231B))
                    }
                }
            }
        }
    }
}
@Composable private fun vaultFieldColors(active:Boolean=false)=OutlinedTextFieldDefaults.colors(focusedBorderColor=if(active)Green else Muted,unfocusedBorderColor=if(active)Green else Muted,focusedLabelColor=if(active)Green else Muted,unfocusedLabelColor=if(active)Green else Muted,cursorColor=Green)

@Composable private fun TypingNotice(message:String?,onFinished:()->Unit,modifier:Modifier=Modifier){
    var visible by remember { mutableIntStateOf(0) }
    LaunchedEffect(message){
        visible=0
        if(message!=null){
            while(visible<message.length){visible++;delay(28)}
            delay(1500)
            while(visible>0){visible--;delay(24)}
            onFinished()
        }
    }
    if(message!=null) Text(text=message.take(visible),modifier=modifier,
        color=Green,fontFamily=VaultFont,fontSize=11.sp,
        textAlign=androidx.compose.ui.text.style.TextAlign.Center)
}

@Composable private fun EmergencyPasswordSetupScreen(onSave:(String)->Unit){
    var password by remember{mutableStateOf("")}
    var confirm by remember{mutableStateOf("")}
    var activeField by remember{mutableStateOf("password")}
    var textMode by remember{mutableStateOf(false)}
    fun appendDigit(digit:String){
        if(activeField=="password"&&password.length<8){password+=digit;if(password.length==8)activeField="confirm"}
        else if(activeField=="confirm"&&confirm.length<8)confirm+=digit
    }
    fun eraseDigit(){
        if(activeField=="password"&&password.isNotEmpty())password=password.dropLast(1)
        else if(activeField=="confirm"&&confirm.isNotEmpty())confirm=confirm.dropLast(1)
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal=24.dp,vertical=18.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center
    ){
        Text(tr("EMERGENCY ACCESS"),color=Green,fontSize=23.sp,fontFamily=VaultFont)
        Spacer(Modifier.height(8.dp))
        Text(tr("Create a backup password. You will use it when connecting from an IP address that is not on your whitelist."),
            color=Muted,fontSize=11.sp)
        Spacer(Modifier.height(14.dp))
        if(textMode){
            OutlinedTextField(password,{password=it},modifier=Modifier.fillMaxWidth(),
                label={Text(tr("Backup password"))},singleLine=true,
                visualTransformation=PasswordVisualTransformation(),colors=vaultFieldColors())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(confirm,{confirm=it},modifier=Modifier.fillMaxWidth(),
                label={Text(tr("Repeat new password"))},singleLine=true,
                visualTransformation=PasswordVisualTransformation(),colors=vaultFieldColors())
            TextButton(onClick={textMode=false}){Text("123",color=Green,fontFamily=VaultFont)}
        }else{
            Box(Modifier.fillMaxWidth()){
                OutlinedTextField(
                    value="• ".repeat(password.length).trim(),
                    onValueChange={},readOnly=true,
                    label={Text(tr("Backup password"))},singleLine=true,colors=vaultFieldColors(activeField=="password"),
                    textStyle=TextStyle(fontFamily=VaultFont,color=if(activeField=="password")Green else Muted,letterSpacing=2.sp),
                    modifier=Modifier.fillMaxWidth().then(if(activeField=="password")Modifier.shadow(7.dp,RoundedCornerShape(6.dp),ambientColor=Green.copy(alpha=.35f)) else Modifier)
                )
                Box(Modifier.matchParentSize().clickable{activeField="password"})
            }
            Spacer(Modifier.height(7.dp))
            Box(Modifier.fillMaxWidth()){
                OutlinedTextField(
                    value="• ".repeat(confirm.length).trim(),
                    onValueChange={},readOnly=true,
                    label={Text(tr("Repeat new password"))},singleLine=true,colors=vaultFieldColors(activeField=="confirm"),
                    textStyle=TextStyle(fontFamily=VaultFont,color=if(activeField=="confirm")Green else Muted,letterSpacing=2.sp),
                    modifier=Modifier.fillMaxWidth().then(if(activeField=="confirm")Modifier.shadow(7.dp,RoundedCornerShape(6.dp),ambientColor=Green.copy(alpha=.35f)) else Modifier)
                )
                Box(Modifier.matchParentSize().clickable{activeField="confirm"})
            }
            Spacer(Modifier.height(12.dp))
            val keypadRows=listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"))
            Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){
                keypadRows.forEach{row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        row.forEach{digit->
                            Button(onClick={appendDigit(digit)},modifier=Modifier.weight(1f).height(48.dp),
                                shape=androidx.compose.foundation.shape.CircleShape,
                                colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF101B14),contentColor=Green),
                                border=BorderStroke(1.dp,Green.copy(alpha=.55f)),
                                elevation=ButtonDefaults.buttonElevation(defaultElevation=3.dp,pressedElevation=0.dp)
                            ){Text(digit,fontFamily=VaultFont,fontSize=19.sp)}
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                    OutlinedButton(onClick={textMode=true},modifier=Modifier.weight(1f).height(48.dp),
                        shape=androidx.compose.foundation.shape.CircleShape,border=BorderStroke(1.dp,Muted.copy(alpha=.55f))
                    ){Text("ABC",fontFamily=VaultFont,color=Muted,fontSize=12.sp)}
                    Button(onClick={appendDigit("0")},modifier=Modifier.weight(1f).height(48.dp),
                        shape=androidx.compose.foundation.shape.CircleShape,
                        colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF101B14),contentColor=Green),
                        border=BorderStroke(1.dp,Green.copy(alpha=.55f))
                    ){Text("0",fontFamily=VaultFont,fontSize=19.sp)}
                    OutlinedButton(onClick={eraseDigit()},modifier=Modifier.weight(1f).height(48.dp),
                        shape=androidx.compose.foundation.shape.CircleShape,border=BorderStroke(1.dp,Muted.copy(alpha=.55f))
                    ){Text("⌫",fontFamily=VaultFont,color=Muted,fontSize=18.sp)}
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if(password.isNotEmpty()&&password.length<8)Text(tr("Use at least 8 characters."),color=Muted,fontSize=10.sp)
        if(confirm.isNotEmpty()&&password!=confirm)Text(tr("Passwords do not match."),color=Muted,fontSize=10.sp)
        Spacer(Modifier.height(10.dp))
        Button(onClick={onSave(password)},enabled=password.length>=8&&password==confirm,
            colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Black),
            shape=RoundedCornerShape(10.dp),border=BorderStroke(1.dp,Green.copy(alpha=.45f)),
            elevation=ButtonDefaults.buttonElevation(defaultElevation=4.dp,pressedElevation=1.dp)
        ){Text(tr("SAVE BACKUP PASSWORD"))}
    }
}
@Composable private fun EmergencyPasswordEntryScreen(resetKey:Int,onCancel:()->Unit,onUnlock:(String)->Unit){
    var password by remember{mutableStateOf("")}
    LaunchedEffect(resetKey){password=""}
    var textMode by remember{mutableStateOf(false)}
    Column(
        Modifier.fillMaxSize().padding(horizontal=28.dp, vertical=24.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.Center
    ){
        Text("EMERGENCY ACCESS",color=Green,fontSize=23.sp,fontFamily=VaultFont)
        Spacer(Modifier.height(10.dp))
        Text(tr("Enter your backup password to unlock IP VAULT."),color=Muted)
        Spacer(Modifier.height(22.dp))
        Text(if(password.isEmpty()) "ENTER PASSWORD" else "• ".repeat(password.length).trim(),
            color=Green,fontSize=if(password.isEmpty())10.sp else 22.sp,fontFamily=VaultFont,
            letterSpacing=if(password.isEmpty())2.sp else 3.sp)
        Spacer(Modifier.height(18.dp))
        if(textMode){
            OutlinedTextField(
                value=password,onValueChange={password=it},
                modifier=Modifier.fillMaxWidth(),
                label={Text("Backup password")},singleLine=true,
                visualTransformation=PasswordVisualTransformation(),colors=vaultFieldColors()
            )
        }else{
            val rows=listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"))
            Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){
                rows.forEach{row->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        row.forEach{digit->
                            Button(
                                onClick={if(password.length<128)password+=digit},
                                modifier=Modifier.weight(1f).height(62.dp),
                                shape=androidx.compose.foundation.shape.CircleShape,
                                colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF101B14),contentColor=Green),
                                border=BorderStroke(1.dp,Green.copy(alpha=.55f)),
                                elevation=ButtonDefaults.buttonElevation(defaultElevation=3.dp,pressedElevation=0.dp)
                            ){Text(digit,fontFamily=VaultFont,fontSize=22.sp)}
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    OutlinedButton(
                        onClick={textMode=true},
                        modifier=Modifier.weight(1f).height(62.dp),
                        shape=androidx.compose.foundation.shape.CircleShape,
                        border=BorderStroke(1.dp,Muted.copy(alpha=.55f))
                    ){Text("ABC",fontFamily=VaultFont,color=Muted,fontSize=13.sp)}
                    Button(
                        onClick={if(password.length<128)password+="0"},
                        modifier=Modifier.weight(1f).height(62.dp),
                        shape=androidx.compose.foundation.shape.CircleShape,
                        colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF101B14),contentColor=Green),
                        border=BorderStroke(1.dp,Green.copy(alpha=.55f)),
                        elevation=ButtonDefaults.buttonElevation(defaultElevation=3.dp,pressedElevation=0.dp)
                    ){Text("0",fontFamily=VaultFont,fontSize=22.sp)}
                    OutlinedButton(
                        onClick={if(password.isNotEmpty())password=password.dropLast(1)},
                        modifier=Modifier.weight(1f).height(62.dp),
                        shape=androidx.compose.foundation.shape.CircleShape,
                        border=BorderStroke(1.dp,Muted.copy(alpha=.55f))
                    ){Text("⌫",fontFamily=VaultFont,color=Muted,fontSize=20.sp)}
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            TextButton(onClick=onCancel){Text(tr("CANCEL"),color=Muted)}
            if(textMode)TextButton(onClick={textMode=false}){Text("123",color=Green,fontFamily=VaultFont)}
            Button(
                onClick={onUnlock(password)},enabled=password.isNotEmpty(),
                colors=ButtonDefaults.buttonColors(containerColor=Green,contentColor=Black),
                shape=RoundedCornerShape(10.dp),
                border=BorderStroke(1.dp,Green.copy(alpha=.45f)),
                elevation=ButtonDefaults.buttonElevation(defaultElevation=4.dp,pressedElevation=1.dp)
            ){Text(tr("UNLOCK"))}
        }
    }
}
@Composable private fun AccessDeniedGestureScreen(onRecognized:()->Unit){
    Box(
        Modifier.fillMaxSize().pointerInput(onRecognized) {
            val gesturePoints = mutableListOf<Offset>()
            detectDragGestures(
                onDragStart={start->gesturePoints.clear();gesturePoints.add(start)},
                onDrag={change,_->gesturePoints.add(change.position)},
                onDragEnd={
                    if(isEmergencyM(gesturePoints.toList(),size.width.toFloat(),size.height.toFloat())) onRecognized()
                    gesturePoints.clear()
                },
                onDragCancel={gesturePoints.clear()}
            )
        },
        contentAlignment=Alignment.Center
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Text("IP VAULT",color=Green,fontSize=25.sp,fontFamily=VaultFont)
            Spacer(Modifier.height(12.dp))
            Text(tr("ACCESS DENIED"),color=Color.White,fontSize=18.sp,fontFamily=VaultFont)
            Spacer(Modifier.height(8.dp))
            Text(tr("This IP address is not authorized."),color=Muted,fontSize=12.sp)
            Spacer(Modifier.height(24.dp))
            Text(tr("IP VAULT // LOCKED"),color=Muted,fontSize=9.sp,letterSpacing=2.sp)
        }
    }
}
private fun isEmergencyM(points:List<Offset>,width:Float,height:Float):Boolean{
    if(points.size<12)return false
    val minX=points.minOf{it.x};val maxX=points.maxOf{it.x};val minY=points.minOf{it.y};val maxY=points.maxOf{it.y}
    if(maxX-minX<width*.35f||maxY-minY<height*.35f)return false
    val normalized=points.map{Offset((it.x-minX)/(maxX-minX),(it.y-minY)/(maxY-minY))}
    val lengths=normalized.zipWithNext().map{(a,b)->kotlin.math.hypot((b.x-a.x).toDouble(),(b.y-a.y).toDouble())}
    val total=lengths.sum()
    if(total<2.0)return false
    val samples=(0..4).map{index->
        val target=total*index/4.0
        var traversed=0.0
        var result=normalized.last()
        for(i in lengths.indices){
            val segment=lengths[i]
            if(traversed+segment>=target){
                val fraction=if(segment==0.0)0.0 else (target-traversed)/segment
                result=Offset(
                    (normalized[i].x+(normalized[i+1].x-normalized[i].x)*fraction).toFloat(),
                    (normalized[i].y+(normalized[i+1].y-normalized[i].y)*fraction).toFloat()
                )
                break
            }
            traversed+=segment
        }
        result
    }
    val target= listOf(Offset(.08f,.92f),Offset(.08f,.08f),Offset(.5f,.62f),Offset(.92f,.08f),Offset(.92f,.92f))
    val direct=samples.zip(target).sumOf{(a,b)->kotlin.math.hypot((a.x-b.x).toDouble(),(a.y-b.y).toDouble())}
    val reverse=samples.reversed().zip(target).sumOf{(a,b)->kotlin.math.hypot((a.x-b.x).toDouble(),(a.y-b.y).toDouble())}
    return minOf(direct,reverse)/5.0<.32
}
