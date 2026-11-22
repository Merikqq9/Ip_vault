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
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
    " OF 5" to " ИЗ 5"
)
private fun tr(value:String):String =
    if (activeVaultLanguage == "ru") RussianTranslations[value] ?: value else value

private val LanguageKey=androidx.datastore.preferences.core.stringPreferencesKey("app_language")
private val Green=Color(0xFF20E879); private val Black=Color(0xFF050805); private val Muted=Color(0xFF819187)
private val TerminalLineColor=Color(0xFF245A38)
private val TerminalAmber=Color(0xFFFFB347)
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
 var bootComplete by rememberSaveable{mutableStateOf(false)};var page by rememberSaveable{mutableStateOf("home")};var language by rememberSaveable{mutableStateOf("en")};var filter by remember{mutableStateOf("ALL")};var message by remember{mutableStateOf<String?>(null)}
 LaunchedEffect(Unit){val storedPrefs=withContext(Dispatchers.IO){context.vaultStore.data.first()};val stored=storedPrefs[AllowedIps]?:emptySet();language=storedPrefs[LanguageKey]?: "en";activeVaultLanguage=language;savedWhitelist=stored;ip=withContext(Dispatchers.IO){try{val c=URL("https://api.ipify.org").openConnection() as HttpsURLConnection;c.connectTimeout=5000;c.readTimeout=5000;c.inputStream.bufferedReader().use{it.readText().trim()}.also{c.disconnect()}}catch(_:Exception){null}};firstRun=stored.isEmpty();setupEmergency=stored.isNotEmpty()&&storedPrefs[EmergencyHash]==null;checking=false}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->if(uri!=null)scope.launch{try{
  val item=withContext(Dispatchers.IO){val name=context.contentResolver.query(uri,null,null,null,null)?.use{c->val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(c.moveToFirst()&&i>=0)c.getString(i)else null}?: "file";val mime=context.contentResolver.getType(uri)?:"application/octet-stream";val id=UUID.randomUUID().toString()+".vault";val input=context.contentResolver.openInputStream(uri)?:error("Cannot read file");VaultCrypto(context).encrypt(input,id);VaultItem(id,name,mime)}
  context.vaultStore.edit{it[VaultItems]=(it[VaultItems]?:emptySet())+item.encode()};message=tr("FILE ENCRYPTED AND SAVED")
 }catch(e:Exception){message=tr("IMPORT ERROR: ")+(e.localizedMessage?:"unknown")}}}
 MaterialTheme(colorScheme=darkColorScheme(primary=Green,background=Black,surface=Color(0xFF0D140F),onSurface=Color.White)){Surface(Modifier.fillMaxSize(),color=Black){
 Box(Modifier.fillMaxSize()) {
 MatrixRainBackground(Modifier.fillMaxSize())
 Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
 when{
 !bootComplete->TerminalBootScreen(language){bootComplete=true}
 checking->CenterStatus("IP VAULT",tr("CHECKING NETWORK"))
 firstRun->InitialTrustScreen(ip){v->scope.launch{context.vaultStore.edit{it[AllowedIps]=setOf(v)};savedWhitelist=setOf(v);firstRun=false;setupEmergency=true}}
 setupEmergency&&ip!=null&&ip in whitelist->EmergencyPasswordSetupScreen{password->scope.launch{val record=withContext(Dispatchers.Default){createEmergencyRecord(password)};context.vaultStore.edit{it[EmergencySalt]=record.first;it[EmergencyHash]=record.second};setupEmergency=false}}
 (ip==null||ip !in whitelist)&&!emergencyUnlocked->if(showEmergencyPassword)EmergencyPasswordEntryScreen(onCancel={showEmergencyPassword=false}){password->scope.launch{val p=context.vaultStore.data.first();val now=System.currentTimeMillis();val lockedUntil=p[EmergencyLockUntil]?:0L;if(now<lockedUntil){message=tr("TOO MANY ATTEMPTS. TRY AGAIN IN ")+((lockedUntil-now+999)/1000)+tr(" SECONDS")}else{val valid=withContext(Dispatchers.Default){checkEmergencyPassword(password,p[EmergencySalt],p[EmergencyHash])};if(valid){context.vaultStore.edit{it[EmergencyFailures]=0;it[EmergencyLockUntil]=0L};emergencyUnlocked=true;showEmergencyPassword=false;page="home"}else{val failures=(p[EmergencyFailures]?:0)+1;context.vaultStore.edit{it[EmergencyFailures]=if(failures>=5)0 else failures;if(failures>=5)it[EmergencyLockUntil]=now+60000L};message=if(failures>=5)tr("TOO MANY ATTEMPTS. LOCKED FOR 60 SECONDS") else tr("INCORRECT PASSWORD. ATTEMPT ")+failures+tr(" OF 5")}}}}else AccessDeniedGestureScreen{showEmergencyPassword=true}
 page=="settings"->WhitelistScreen(whitelist,ip,language,{selected->language=selected;activeVaultLanguage=selected;scope.launch{context.vaultStore.edit{it[LanguageKey]=selected}}},{page="home"},{v->scope.launch{context.vaultStore.edit{it[AllowedIps]=whitelist+v};savedWhitelist=whitelist+v}},{v->scope.launch{context.vaultStore.edit{it[AllowedIps]=whitelist-v};savedWhitelist=whitelist-v}},{old,new->scope.launch{val p=context.vaultStore.data.first();val valid=withContext(Dispatchers.Default){checkEmergencyPassword(old,p[EmergencySalt],p[EmergencyHash])};if(valid){val record=withContext(Dispatchers.Default){createEmergencyRecord(new)};context.vaultStore.edit{it[EmergencySalt]=record.first;it[EmergencyHash]=record.second};message=tr("EMERGENCY PASSWORD UPDATED")}else message=tr("INCORRECT CURRENT PASSWORD")}})
 page=="files"->FilesScreen(items,filter,{filter=it},{page="home"},{picker.launch(arrayOf("*/*"))},{item->scope.launch{try{val temp=withContext(Dispatchers.IO){val safeName=item.name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"),"_").ifBlank{"file"};val out=File(context.cacheDir,"open_"+UUID.randomUUID().toString()+"_"+safeName);out.outputStream().use{VaultCrypto(context).decrypt(File(File(context.filesDir,"vault_encrypted"),item.id),it)};out};val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",temp);val resolvedMime=if(item.mime.isNotBlank()&&item.mime!="application/octet-stream")item.mime else android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(item.name.substringAfterLast('.', "").lowercase()) ?: if(item.mime.startsWith("audio/"))"audio/*" else "*/*";val viewIntent=Intent(Intent.ACTION_VIEW).setDataAndType(uri,resolvedMime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);viewIntent.clipData=android.content.ClipData.newUri(context.contentResolver,item.name,uri);context.startActivity(Intent.createChooser(viewIntent,"Open with"))}catch(e:Exception){message=tr("OPEN ERROR: ")+(e.localizedMessage?:tr("No app can open this file"))}}},{item->scope.launch{context.vaultStore.edit{p->p[VaultItems]=(p[VaultItems]?:emptySet()).filterNot{VaultItem.decode(it)?.id==item.id}.toSet()};withContext(Dispatchers.IO){File(File(context.filesDir,"vault_encrypted"),item.id).delete()}}})
 else->HomeScreen(ip?: "UNVERIFIED",{page="settings"},{page="files";filter="ALL"},{page="files";filter="IMAGE"},{page="files";filter="VIDEO"},{page="files";filter="AUDIO"},{page="files";filter="OTHER"})
 }
 message?.let{AlertDialog(onDismissRequest={message=null},confirmButton={TextButton(onClick={message=null}){Text(tr("OK"),color=Green,fontFamily=FontFamily.Monospace)}},text={Text(it,color=Color.White,fontFamily=FontFamily.Monospace)},containerColor=Color(0xFF07130B))}
 }
 }
 }}
}


@Composable private fun TerminalBootScreen(language:String,onFinished:()->Unit){
 val lines=remember(language){if(language=="ru")listOf(
  ">> ДОБРО ПОЖАЛОВАТЬ, ОПЕРАТОР.","[KERNEL] загрузка защищённого ядра .......... OK","[CRYPTO] инициализация AES-256-GCM .......... OK","[KEYS] Android Keystore ..................... SEALED","[NET] проверка сетевого интерфейса .......... OK","[FIREWALL] правила доступа .................. LOADED","[VAULT] локальный контейнер ................. MOUNTED","[AUTH] шлюз авторизации ..................... READY","[SYS] все подсистемы отвечают."
 )else listOf(
  ">> WELCOME BACK, OPERATOR.","[KERNEL] loading hardened runtime ........... OK","[CRYPTO] initializing AES-256-GCM ........... OK","[KEYS] Android Keystore ..................... SEALED","[NET] probing network interface ............. OK","[FIREWALL] access control rules ............. LOADED","[VAULT] local encrypted volume .............. MOUNTED","[AUTH] authentication gateway ............... READY","[SYS] all subsystems responding."
 )}
 val printed=remember{mutableStateListOf<String>()};var progress by remember{mutableIntStateOf(0)};var cursor by remember{mutableStateOf(true)}
 LaunchedEffect(Unit){while(true){cursor=!cursor;delay(380)}}
 LaunchedEffect(lines){printed.clear();lines.forEachIndexed{index,line->printed.add("");line.forEach{ch->printed[index]=printed[index]+ch;delay(if(index==0)20 else 8)};progress=(index+1)*100/lines.size;delay(170)};delay(700);onFinished()}
 Column(Modifier.fillMaxSize().padding(horizontal=13.dp,vertical=22.dp),verticalArrangement=Arrangement.Center){
  Text("  ___ ____    __     __",color=Green,fontSize=18.sp,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold)
  Text(" |_ _|  _ \\  \\ \\   / /",color=Green,fontSize=14.sp,fontFamily=FontFamily.Monospace)
  Text("  | || |_) |  \\ \\ / /",color=Green,fontSize=14.sp,fontFamily=FontFamily.Monospace)
  Text(" |___|____/    \\___/",color=Green,fontSize=14.sp,fontFamily=FontFamily.Monospace)
  Text(" [ IP_VAULT // SECURE STORAGE TERMINAL ]",color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
  Spacer(Modifier.height(18.dp));Text("┌─ BOOT SEQUENCE / AUTHENTICATED STARTUP",color=Green,fontSize=17.sp,fontFamily=FontFamily.Monospace)
  Column(Modifier.fillMaxWidth().background(Color(0xB8030905)).padding(9.dp)){printed.forEachIndexed{index,line->Text(line+(if(index==printed.lastIndex&&progress<100&&cursor)" ▌"else""),color=if(index==0)Green else Color(0xFFB4DCC0),fontSize=14.sp,fontFamily=FontFamily.Monospace,lineHeight=14.sp)}}
  Spacer(Modifier.height(12.dp));Text("TRANSFER  ["+"█".repeat(progress/5)+"·".repeat(20-progress/5)+"]  "+progress+"%",color=Green,fontSize=17.sp,fontFamily=FontFamily.Monospace)
  Text(if(progress<100)"root@ipvault:~$ initializing..."else"root@ipvault:~$ access gateway ready_"+(if(cursor)"▌"else""),color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
 }
}
@Composable private fun MatrixRainBackground(modifier:Modifier=Modifier) {
 val transition=rememberInfiniteTransition(label="matrix-rain")
 val phase by transition.animateFloat(0f,1f,infiniteRepeatable(tween(9000,easing=LinearEasing)),label="rain-phase")
 val glyphs=listOf("ア","イ","ウ","エ","オ","カ","キ","ク","ケ","コ","サ","シ","ス","セ","ソ","タ","チ","ツ","テ","ト","ナ","ニ","ヌ","ネ","ノ","ハ","ヒ","フ","ヘ","ホ","マ","ミ","ム","メ","モ","ヤ","ユ","ヨ","ラ","リ","ル","レ","ロ","ワ","ン","零","一","二","三","四","五","六","七","八","九")
 Canvas(modifier) {
  drawRect(Color(0xFF010302))
  val native=drawContext.canvas.nativeCanvas
  val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
   typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL)
   textSize=22f
  }
  val step=30f
  val columns=(size.width/step).toInt()+1
  val rows=(size.height/step).toInt()+2
  for(column in 0 until columns) {
   val speed=0.45f+(column%5)*0.13f
   val offset=(column*73)%rows
   val head=((phase*rows*2*speed+offset)% (rows+8)).toInt()
   for(tail in 0..9) {
    val row=head-tail
    if(row<0||row>=rows) continue
    val glyph=glyphs[(column*11+row*7+tail*3)%glyphs.size]
    val alpha=if(tail==0)210 else (105-tail*9).coerceAtLeast(18)
    paint.color=if(tail==0)android.graphics.Color.argb(235,170,255,190) else android.graphics.Color.argb(alpha,0,190,75)
    native.drawText(glyph,column*step+4f,row*step+step,paint)
   }
  }
 }
}
@Composable private fun TerminalHeader(title:String,path:String="SYS://IP_VAULT") {
 Column(Modifier.fillMaxWidth()){
  Text("IP_VAULT // SECURE NODE",color=Green,fontSize=17.sp,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold)
  Text(title.uppercase(),color=Color(0xFFD8F5DF),fontSize=14.sp,fontFamily=FontFamily.Monospace)
  Text("LINK: ENCRYPTED  •  SESSION: ACTIVE",color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
  Text(path,color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
  HorizontalDivider(color=TerminalLineColor)
 }
}
@Composable private fun TerminalFooter(text:String="END OF SESSION") {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color=TerminalLineColor)
        Spacer(Modifier.height(6.dp))
        Text("[" + text + "]  •  LOCAL ENCRYPTED STORAGE", color=Muted, fontSize=14.sp, fontFamily=FontFamily.Monospace, letterSpacing=1.sp)
    }
}
@Composable private fun TerminalPanel(title:String,modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
 Column(modifier.fillMaxWidth().padding(vertical=1.dp)){
  Text("[ "+title.uppercase()+" ]",color=Green,fontSize=17.sp,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold)
  HorizontalDivider(color=TerminalLineColor)
  Column(Modifier.fillMaxWidth().padding(start=2.dp,top=4.dp,bottom=4.dp)){content()}
 }
}
@Composable private fun TerminalButton(label:String,modifier:Modifier=Modifier,enabled:Boolean=true,danger:Boolean=false,onClick:()->Unit){
 val tint=if(danger)Color(0xFFFF625D)else Green
 Row(modifier.fillMaxWidth().clickable(enabled=enabled,onClick=onClick).padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
  Text("root@ipvault:~$",color=Muted,fontSize=12.sp,fontFamily=FontFamily.Monospace,maxLines=1,softWrap=false);Spacer(Modifier.width(5.dp))
  Text(label.lowercase().replace(" ","_"),modifier=Modifier.weight(1f),color=if(enabled)tint else Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,maxLines=1,softWrap=false,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
  Spacer(Modifier.width(5.dp))
  Text(if(enabled)"[ ENTER ↵ ]" else "[ LOCKED ]",modifier=Modifier.width(88.dp),color=Muted,fontSize=12.sp,fontFamily=FontFamily.Monospace,maxLines=1,softWrap=false,textAlign=androidx.compose.ui.text.style.TextAlign.End)
 }
}
@Composable private fun TerminalMenuRow(label:String,index:String,detail:String="",onClick:()->Unit){
 Column(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=3.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){Text("├─ ["+index+"]",color=Green,fontSize=14.sp,fontFamily=FontFamily.Monospace);Spacer(Modifier.width(8.dp));Text(keepParentheticalTogether(label.uppercase()),color=Color(0xFFD8F5DF),fontSize=14.sp,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text("▶",color=Green,fontSize=14.sp,fontFamily=FontFamily.Monospace)}
  if(detail.isNotEmpty())Text("│     "+keepParentheticalTogether(detail),color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
 }
}
@Composable private fun TerminalField(value:String,onValueChange:(String)->Unit,label:String,password:Boolean=false,keyboardOptions:KeyboardOptions=KeyboardOptions.Default){
 Column(Modifier.fillMaxWidth().padding(vertical=2.dp)){
  Text("# "+label.uppercase(),color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
  Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
   Text("root@ipvault:~$ ",color=Green,fontSize=14.sp,fontFamily=FontFamily.Monospace)
   BasicTextField(value=value,onValueChange=onValueChange,singleLine=true,visualTransformation=if(password)PasswordVisualTransformation()else androidx.compose.ui.text.input.VisualTransformation.None,keyboardOptions=keyboardOptions,textStyle=androidx.compose.ui.text.TextStyle(color=Color(0xFFE2FFE8),fontFamily=FontFamily.Monospace,fontSize=14.sp),cursorBrush=SolidColor(Green),modifier=Modifier.weight(1f))
  }
  Text("─".repeat(42),color=Color(0xFF245A38),fontSize=14.sp,fontFamily=FontFamily.Monospace)
 }
}
@Composable private fun TerminalPage(title:String, path:String, back:(()->Unit)?=null, content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=10.dp, vertical=8.dp)) {
        if(back!=null) Row(verticalAlignment=Alignment.CenterVertically) {
            Text("< ESC", Modifier.clickable(onClick=back).padding(end=14.dp), color=Green, fontSize=14.sp, fontFamily=FontFamily.Monospace)
            Spacer(Modifier.weight(1f))
            Text("TERMINAL SESSION", color=Muted, fontSize=14.sp, fontFamily=FontFamily.Monospace)
        }
        TerminalHeader(title,path)
        Spacer(Modifier.height(12.dp))
        content()
        Spacer(Modifier.height(14.dp))
        TerminalFooter()
    }
}
private fun keepParentheticalTogether(value:String):String {
    val start=value.indexOf('(')
    val end=if(start>=0)value.indexOf(')',start+1) else -1
    if(start<0||end<0)return value.replace(" (","\u00A0(")
    val inside=value.substring(start,end+1).replace(" ","\u00A0")
    return value.substring(0,start).replace(Regex(" +$"),"\u00A0")+inside+value.substring(end+1)
}

@Composable private fun TerminalLine(value:String,color:Color=Color.White) {
    Text(keepParentheticalTogether(value),color=color,fontSize=17.sp,fontFamily=FontFamily.Monospace,softWrap=true)
}
@Composable private fun CenterStatus(t:String,d:String) {
    Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.Start,verticalArrangement=Arrangement.Center) {
        Text("┌── BOOT SEQUENCE ──────────────────",color=Green,fontSize=17.sp,fontFamily=FontFamily.Monospace)
        Spacer(Modifier.height(16.dp))
        Text("IP_VAULT",color=Green,fontSize=28.sp,fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,letterSpacing=2.sp)
        Spacer(Modifier.height(8.dp))
        Text("> " + t.uppercase(),color=Color.White,fontSize=17.sp,fontFamily=FontFamily.Monospace)
        Text("> " + d.uppercase(),color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
        Spacer(Modifier.height(16.dp))
        Text("INITIALIZING SECURE ENVIRONMENT  ▓▓▓▓▓▓░░░",color=Green,fontSize=17.sp,fontFamily=FontFamily.Monospace)
    }
}
@Composable private fun InitialTrustScreen(ip:String?,onTrust:(String)->Unit) {
    var value by remember(ip){mutableStateOf(ip.orEmpty())}
    TerminalPage("INITIAL TRUST","SYS://SETUP") {
        TerminalPanel("NETWORK DISCOVERY") {
            TerminalLine("> PUBLIC IP DETECTED: " + (ip ?: "UNAVAILABLE"), if(ip==null)TerminalAmber else Green)
            Spacer(Modifier.height(5.dp))
            Text(tr("Add your current public IP to initialize the global whitelist."),color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
        }
        Spacer(Modifier.height(16.dp))
        TerminalField(value,{value=it},tr("Public IP address"),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Ascii))
        Spacer(Modifier.height(12.dp))
        TerminalButton(tr("TRUST THIS IP"),enabled=value.trim().matches(Regex("^[0-9a-fA-F:.]{3,45}$"))){onTrust(value.trim())}
        if(ip==null) Text("! NETWORK OFFLINE — CONNECT AND RESTART",color=TerminalAmber,fontSize=17.sp,fontFamily=FontFamily.Monospace,modifier=Modifier.padding(top=12.dp))
    }
}
@Composable private fun HomeScreen(ip:String,settings:()->Unit,all:()->Unit,images:()->Unit,videos:()->Unit,audio:()->Unit,other:()->Unit){
 val pulse=rememberInfiniteTransition(label="telemetry");val tick by pulse.animateFloat(0f,1f,infiniteRepeatable(tween(1200,easing=LinearEasing),RepeatMode.Reverse),label="tick")
 TerminalPage("GHOST TERMINAL","SYS://ROOT"){
  TerminalPanel("LIVE SYSTEM TELEMETRY"){
   TerminalLine(" [●] CORE: ONLINE",Green);TerminalLine(" [●] AES-256-GCM: READY",Green);TerminalLine(" [●] KEYSTORE: SEALED",Green);TerminalLine(" [●] NETWORK: "+ip,Color(0xFFB8D8C0));TerminalLine(" [●] VAULT: ENCRYPTED",Green);TerminalLine(" ["+(if(tick>.5f)"●" else " ")+"] SESSION: ACTIVE",Green)
  }
  Spacer(Modifier.height(8.dp))
  TerminalPanel("NODE MAP // LOCAL ARCHITECTURE"){
   TerminalLine("          [ IP_VAULT ]",Green)
   TerminalLine("                |",Muted)
   TerminalLine("       +--------+--------+",Muted)
   TerminalLine("       |        |        |",Muted)
   TerminalLine("    [CIPHER] [ACCESS] [VAULT]",Green)
   TerminalLine("    AES-256   IP ACL   LOCAL",Color(0xFF9DC9AA))
  }
  Spacer(Modifier.height(8.dp))
  TerminalPanel("NIGHT SHIFT // EVENT STREAM"){
   TerminalLine("00:00:01  SYS   session handshake ........ OK",Color(0xFFB8D8C0));TerminalLine("00:00:02  CRYP  vault cipher .............. READY",Color(0xFFB8D8C0));TerminalLine("00:00:03  NET   trusted route ............. ACTIVE",Color(0xFFB8D8C0));TerminalLine("00:00:04  AUTH  access policy ............. ENFORCED",Green);TerminalLine("00:00:05  LOG   local audit ............... SEALED",Color(0xFFB8D8C0))
  }
  Spacer(Modifier.height(8.dp));Text("root@ipvault:~$ vault --list-sectors",color=Color(0xFFB8D8C0),fontSize=17.sp,fontFamily=FontFamily.Monospace)
  TerminalMenuRow("ALL ENCRYPTED OBJECTS","01","vault://files",all);TerminalMenuRow("IMAGE SECTOR","02","vault://media/images",images);TerminalMenuRow("VIDEO SECTOR","03","vault://media/video",videos);TerminalMenuRow("AUDIO SECTOR","04","vault://media/audio",audio);TerminalMenuRow("DOCUMENTS / OTHER","05","vault://data/misc",other)
  Spacer(Modifier.height(8.dp));TerminalButton("open system configuration",onClick=settings)
 }
}
@Composable private fun FilesScreen(items:List<VaultItem>,filter:String,setFilter:(String)->Unit,back:()->Unit,add:()->Unit,open:(VaultItem)->Unit,delete:(VaultItem)->Unit) {
    BackHandler(onBack=back)
    val shown=items.filter{when(filter){"IMAGE"->it.mime.startsWith("image/");"VIDEO"->it.mime.startsWith("video/");"AUDIO"->it.mime.startsWith("audio/");"OTHER"->!(it.mime.startsWith("image/")||it.mime.startsWith("video/")||it.mime.startsWith("audio/"));else->true}}
    val title=when(filter){"IMAGE"->"PHOTOS";"VIDEO"->"VIDEO";"AUDIO"->"AUDIO";"OTHER"->"OTHER FILES";else->"ALL FILES"}
    TerminalPage(title,"FS://VAULT",back) {
        TerminalPanel("DIRECTORY SCAN") {
            TerminalLine("> " + shown.size + " OBJECT(S) FOUND",Green)
            TerminalLine("> FILTER: " + filter)
        }
        Spacer(Modifier.height(9.dp))
        TerminalButton("+ IMPORT ENCRYPTED FILE",onClick=add)
        Spacer(Modifier.height(8.dp))
        if(shown.isEmpty()) TerminalPanel("EMPTY DIRECTORY") {
            TerminalLine("[ NO OBJECTS IN THIS SECTOR ]",Muted)
            TerminalLine("> Use import command to add files.")
        } else {
            LazyColumn(Modifier.weight(1f,fill=true)) {
                items(shown,key={it.id}) { item ->
                    Column(Modifier.fillMaxWidth().padding(vertical=7.dp)) {
                        Row(Modifier.fillMaxWidth().clickable{open(item)},verticalAlignment=Alignment.CenterVertically) {
                            Text("-rw-------",color=Green,fontSize=14.sp,fontFamily=FontFamily.Monospace)
                            Spacer(Modifier.width(7.dp))
                            Text(item.name,color=Color(0xFFE1F8E7),fontSize=14.sp,fontFamily=FontFamily.Monospace,maxLines=1,modifier=Modifier.weight(1f))
                            Text("[ OPEN ]",color=Green,fontSize=12.sp,fontFamily=FontFamily.Monospace,maxLines=1,softWrap=false)
                        }
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("│  MIME: "+item.mime,color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace,modifier=Modifier.weight(1f))
                            Text("[ DELETE ]",Modifier.clickable{delete(item)}.padding(horizontal=5.dp,vertical=3.dp),color=TerminalAmber,fontSize=12.sp,fontFamily=FontFamily.Monospace,maxLines=1,softWrap=false)
                        }
                        Text("└─ ID: "+item.id.take(18)+"…",color=Color(0xFF527A60),fontSize=17.sp,fontFamily=FontFamily.Monospace)
                    }
                    HorizontalDivider(color=TerminalLineColor)
                }
            }
        }
    }
}
@Composable private fun WhitelistScreen(ips:Set<String>,current:String?,language:String,setLanguage:(String)->Unit,back:()->Unit,add:(String)->Unit,remove:(String)->Unit,changePassword:(String,String)->Unit) {
    var section by rememberSaveable{mutableStateOf("main")}
    var entry by remember{mutableStateOf("")}
    var oldPassword by remember{mutableStateOf("")}
    var newPassword by remember{mutableStateOf("")}
    var confirmPassword by remember{mutableStateOf("")}
    BackHandler(enabled=section!="main"){section="main"}
    val title=when(section){"password"->tr("BACKUP PASSWORD");"ips"->tr("IP WHITELIST");"language"->tr("LANGUAGE");else->tr("SETTINGS")}
    TerminalPage(title,"SYS://CONFIG",if(section=="main") back else ({ section="main" })) {
        when(section) {
            "main" -> {
                TerminalPanel("CONFIGURATION MODULES") {
                    TerminalMenuRow(tr("CHANGE BACKUP PASSWORD"),"01","Credential rotation"){section="password"}
                    TerminalMenuRow(tr("IP WHITELIST"),"02","Trusted network addresses"){section="ips"}
                    TerminalMenuRow(tr("LANGUAGE"),"03","Interface locale"){section="language"}
                }
                Spacer(Modifier.height(14.dp))
                TerminalPanel("OPERATOR") {
                    TerminalLine("USER      : Merik")
                    TerminalLine("PRODUCT   : IP_VAULT")
                    TerminalLine("MODE      : LOCAL ONLY")
                }
            }
            "password" -> {
                TerminalLine("> CREDENTIAL ROTATION // VERIFY CURRENT KEY",Muted)
                Spacer(Modifier.height(12.dp))
                TerminalField(oldPassword,{oldPassword=it},tr("Current password"),true)
                Spacer(Modifier.height(9.dp))
                TerminalField(newPassword,{newPassword=it},tr("New password (8+ characters)"),true)
                Spacer(Modifier.height(9.dp))
                TerminalField(confirmPassword,{confirmPassword=it},tr("Repeat new password"),true)
                Spacer(Modifier.height(12.dp))
                TerminalButton(tr("UPDATE PASSWORD"),enabled=oldPassword.isNotEmpty()&&newPassword.length>=8&&newPassword==confirmPassword) {
                    changePassword(oldPassword,newPassword);oldPassword="";newPassword="";confirmPassword=""
                }
            }
            "language" -> {
                TerminalPanel("SELECT INTERFACE LOCALE") {
                    TerminalMenuRow("РУССКИЙ","RU",if(language=="ru")"ACTIVE" else "SELECT"){setLanguage("ru")}
                    TerminalMenuRow("ENGLISH","EN",if(language=="en")"ACTIVE" else "SELECT"){setLanguage("en")}
                }
            }
            "ips" -> {
                TerminalLine("> TRUSTED NETWORK ACCESS CONTROL",Muted)
                Spacer(Modifier.height(10.dp))
                TerminalField(entry,{entry=it},tr("IPv4 / IPv6"))
                Spacer(Modifier.height(10.dp))
                TerminalButton(tr("ADD IP"),enabled=entry.trim().matches(Regex("^[0-9a-fA-F:.]{3,45}$"))){add(entry.trim());entry=""}
                Spacer(Modifier.height(12.dp))
                Text("$ whitelist --entries",color=Green,fontSize=14.sp,fontFamily=FontFamily.Monospace)
                LazyColumn(Modifier.weight(1f,fill=true)) {
                    items(ips.sorted()) { address ->
                        Row(Modifier.fillMaxWidth().padding(vertical=9.dp),verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(address,color=Color.White,fontFamily=FontFamily.Monospace,fontSize=17.sp)
                                Text(if(address==current)"CURRENT NODE / ACTIVE" else "TRUSTED ADDRESS",color=Green,fontSize=14.sp,fontFamily=FontFamily.Monospace)
                            }
                            Text("[ REVOKE ]",Modifier.clickable{remove(address)}.padding(6.dp),color=TerminalAmber,fontFamily=FontFamily.Monospace,fontSize=12.sp,maxLines=1,softWrap=false)
                        }
                        HorizontalDivider(color=TerminalLineColor)
                    }
                }
            }
        }
    }
}
@Composable private fun vaultFieldColors()=OutlinedTextFieldDefaults.colors(focusedBorderColor=Green,unfocusedBorderColor=TerminalLineColor,focusedLabelColor=Green,cursorColor=Green)

@Composable private fun EmergencyPasswordSetupScreen(onSave:(String)->Unit) {
    var password by remember{mutableStateOf("")}
    var confirm by remember{mutableStateOf("")}
    TerminalPage("EMERGENCY CREDENTIAL","SEC://RECOVERY") {
        TerminalPanel("RECOVERY KEY SETUP") {
            TerminalLine("> CREATE A SECONDARY ACCESS KEY",Green)
            Text(tr("Create a backup password. You will use it when connecting from an IP address that is not on your whitelist."),color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
        }
        Spacer(Modifier.height(13.dp))
        TerminalField(password,{password=it},tr("Backup password"),true)
        Spacer(Modifier.height(9.dp))
        TerminalField(confirm,{confirm=it},tr("Repeat new password"),true)
        Spacer(Modifier.height(12.dp))
        TerminalButton(tr("SAVE BACKUP PASSWORD"),enabled=password.length>=8&&password==confirm){onSave(password)}
        if(password.isNotEmpty()&&password.length<8) Text("! MINIMUM 8 CHARACTERS",color=TerminalAmber,fontSize=17.sp,fontFamily=FontFamily.Monospace)
        if(confirm.isNotEmpty()&&password!=confirm) Text("! PASSWORDS DO NOT MATCH",color=TerminalAmber,fontSize=17.sp,fontFamily=FontFamily.Monospace)
    }
}
@Composable private fun EmergencyPasswordEntryScreen(onCancel:()->Unit,onUnlock:(String)->Unit) {
    var password by remember{mutableStateOf("")}
    TerminalPage("RECOVERY AUTHENTICATION","SEC://UNLOCK") {
        TerminalPanel("UNAUTHORIZED NETWORK DETECTED") {
            TerminalLine("> SECONDARY CREDENTIAL REQUIRED",TerminalAmber)
            Text(tr("Enter your backup password to unlock IP VAULT."),color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
        }
        Spacer(Modifier.height(14.dp))
        TerminalField(password,{password=it},tr("Backup password"),true)
        Spacer(Modifier.height(12.dp))
        TerminalButton(tr("UNLOCK"),enabled=password.isNotEmpty()){onUnlock(password)}
        Spacer(Modifier.height(8.dp))
        Text("< CANCEL / ESC",Modifier.clickable(onClick=onCancel).padding(8.dp),color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
    }
}
@Composable private fun AccessDeniedGestureScreen(onRecognized:()->Unit) {
    Box(Modifier.fillMaxSize().pointerInput(onRecognized) {
        val gesturePoints=mutableListOf<Offset>()
        detectDragGestures(onDragStart={start->gesturePoints.clear();gesturePoints.add(start)},onDrag={change,_->gesturePoints.add(change.position)},onDragEnd={if(isEmergencyM(gesturePoints.toList(),size.width.toFloat(),size.height.toFloat()))onRecognized();gesturePoints.clear()},onDragCancel={gesturePoints.clear()})
    }.padding(22.dp),contentAlignment=Alignment.Center) {
        Column(Modifier.fillMaxWidth()) {
            TerminalHeader("ACCESS DENIED","SEC://FIREWALL")
            Spacer(Modifier.height(20.dp))
            TerminalPanel("FIREWALL RESPONSE") {
                TerminalLine("!!! CONNECTION REJECTED",TerminalAmber)
                TerminalLine("> SOURCE IP NOT IN TRUST DATABASE")
                TerminalLine("> SESSION STATE: LOCKED",TerminalAmber)
                Spacer(Modifier.height(8.dp))
                Text(tr("This IP address is not authorized."),color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
            }
            Spacer(Modifier.height(18.dp))
            Text("SYSTEM IS LISTENING...",color=Green,fontSize=17.sp,fontFamily=FontFamily.Monospace,letterSpacing=1.sp)
            Text("HIDDEN RECOVERY GESTURE ENABLED",color=Muted,fontSize=14.sp,fontFamily=FontFamily.Monospace)
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
