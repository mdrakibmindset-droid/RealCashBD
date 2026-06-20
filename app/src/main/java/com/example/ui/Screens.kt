package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.EarningLog
import com.example.data.SystemNotification
import com.example.data.User
import com.example.data.WithdrawRequest
import androidx.compose.ui.draw.drawBehind
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Navigation Routes
object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val MAIN_CONTAINER = "main_container"
}

// Sub-tabs in Main Container
enum class EarningTabs {
    HOME,
    EARN,
    REFER,
    WITHDRAW,
    PROFILE,
    HISTORY,
    ADMIN
}

// Helper to convert time to readable format
fun formatTime(timestamp: Long): String {
    val javaDate = java.util.Date(timestamp)
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    return sdf.format(javaDate)
}

// ==========================================
// SPLASH SCREEN
// ==========================================

@Composable
fun SplashScreen(
    viewModel: EarnViewModel,
    onNavigate: (String) -> Unit
) {
    val scale = remember { Animatable(0f) }
    
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(2200) // 2.2 seconds splash animation delay
        
        // Auto check if someone is logged in
        if (viewModel.currentUser.value != null) {
            onNavigate(Routes.MAIN_CONTAINER)
        } else {
            onNavigate(Routes.LOGIN)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B5E20), Color(0xFF0C140E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
                    .background(GoldAccent, CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .shadow(12.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = "Money Splash Logo",
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Real Cash BD 💰",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("splash_title")
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "সঠিক নিয়মে কাজ করুন, প্রতিদিন নিশ্চিত পেমেন্ট!",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                color = GoldAccent,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// ==========================================
// LOGIN SCREEN
// ==========================================

@Composable
fun LoginScreen(
    viewModel: EarnViewModel,
    onNavigate: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    
    // Auto navigate on authentication success
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigate(Routes.MAIN_CONTAINER)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Green header banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GreenPrimary, GreenSecondary)
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = "Logo",
                    tint = GoldAccent,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Real Cash BD",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "উপার্জনের নতুন রূপরেখা",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Login Card
        Card(
            modifier = Modifier
                .padding(top = 160.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "সাইন-ইন করুন 🔑",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ইমেইল ঠিকানা (Email)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "EmailIcon", tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("login_email"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("পাসওয়ার্ড (Password)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "LockIcon", tint = GreenPrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("login_password"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                // Forgot Password Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.resetPassword(email) }) {
                        Text("পাসওয়ার্ড ভুলে গেছেন?", color = GreenSecondary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(color = GreenPrimary)
                } else {
                    Button(
                        onClick = { viewModel.login(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit"),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("লগইন নিশ্চিত করুন 🔓", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("কোনো অ্যাকাউন্ট নেই? ", color = Color.Gray, fontSize = 13.sp)
                    TextButton(onClick = { onNavigate(Routes.REGISTER) }) {
                        Text("নতুন অ্যাকাউন্ট তৈরি করুন", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Pre-filled helpers for easy testing
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Text(
                    text = "টেস্টিং অ্যাকাউন্ট ট্রাই করুন (ওয়ান ট্যাপ কপি):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Regular user fill
                    OutlinedButton(
                        onClick = {
                            email = "rakib@gmail.com"
                            password = "user123"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("👤 রাউটার ইউজার", fontSize = 10.sp)
                    }

                    // Admin fill
                    OutlinedButton(
                        onClick = {
                            email = "admin@realcash.bd"
                            password = "admin123"
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("🛡️ এডমিন প্যানেল", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// REGISTER SCREEN
// ==========================================

@Composable
fun RegisterScreen(
    viewModel: EarnViewModel,
    onNavigate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referCode by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigate(Routes.MAIN_CONTAINER)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GreenPrimary, GreenSecondary)
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FiberNew,
                    contentDescription = "New Acc",
                    tint = GoldAccent,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "নতুন অ্যাকাউন্ট খুলুন",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Card(
            modifier = Modifier
                .padding(top = 130.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ব্যবহারকারীর পূর্ণ নাম") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "User", tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("reg_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ইমেইল ঠিকানা (Email)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Mail", tint = GreenPrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("reg_email"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("নতুন পাসওয়ার্ড") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Pass", tint = GreenPrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("reg_password"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = referCode,
                    onValueChange = { referCode = it },
                    label = { Text("রেফার কোড (ঐচ্ছিক - ৩০০ পয়েন্ট বোনাস)") },
                    leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = "RefCode", tint = GreenPrimary) },
                    placeholder = { Text("যেমন: RAKIB01") },
                    modifier = Modifier.fillMaxWidth().testTag("reg_referral"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "⚠️ সঠিক রেফার কোড ব্যবহার করলে ৩০০ পয়েন্ট বোনাস পাবেন। আর রেফার না থাকলে খালি রাখুন।",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(color = GreenPrimary)
                } else {
                    Button(
                        onClick = { viewModel.register(name, email, password, referCode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("reg_submit"),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("অ্যাকাউন্ট তৈরি করুন 🚀", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ইতিমধ্যে অ্যাকাউন্ট আছে? ", color = Color.Gray, fontSize = 13.sp)
                    TextButton(onClick = { onNavigate(Routes.LOGIN) }) {
                        Text("লগইন করুন", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// MAIN APP NAVIGATION & CONTAINER
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: EarnViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf<EarningTabs>(EarningTabs.HOME) }
    val user by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val toastMessage = viewModel.toastMessage

    // Collect and show local toasts in Bengali
    LaunchedEffect(key1 = true) {
        toastMessage.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Direct Ban redirection
    LaunchedEffect(user) {
        if (user != null && user?.status == "BANNED") {
            viewModel.logout()
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBehind {
                    val strokeWidth = 1 * density
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = Color(0xFFEADDFF),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(GreenPrimary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Real Cash BD",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                        }
                        
                        // Small current user visual badge
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "🏆 ${user?.name?.split(" ")?.firstOrNull() ?: "ইউজার"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimaryLight
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF3EDF7),
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars,
                modifier = Modifier.drawBehind {
                    val strokeWidth = 1 * density
                    drawLine(
                        color = Color(0xFFEADDFF),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                        strokeWidth = strokeWidth
                    )
                }
            ) {
                NavigationBarItem(
                    selected = selectedTab == EarningTabs.HOME,
                    onClick = { selectedTab = EarningTabs.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("হোম", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreenPrimary,
                        unselectedIconColor = Color(0xFF49454F),
                        selectedTextColor = GreenPrimary,
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == EarningTabs.EARN,
                    onClick = { selectedTab = EarningTabs.EARN },
                    icon = { Icon(Icons.Default.MonetizationOn, contentDescription = "Earn") },
                    label = { Text("আয় করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreenPrimary,
                        unselectedIconColor = Color(0xFF49454F),
                        selectedTextColor = GreenPrimary,
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == EarningTabs.REFER,
                    onClick = { selectedTab = EarningTabs.REFER },
                    icon = { Icon(Icons.Default.People, contentDescription = "Refer") },
                    label = { Text("রেফার", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreenPrimary,
                        unselectedIconColor = Color(0xFF49454F),
                        selectedTextColor = GreenPrimary,
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == EarningTabs.WITHDRAW,
                    onClick = { selectedTab = EarningTabs.WITHDRAW },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Withdraw") },
                    label = { Text("উত্তোলন", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GreenPrimary,
                        unselectedIconColor = Color(0xFF49454F),
                        selectedTextColor = GreenPrimary,
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = Color(0xFFE8DEF8)
                    )
                )

                // Render dynamic options (History, Profile OR Admin if role is Admin)
                val isAdmin = user?.role == "ADMIN"
                NavigationBarItem(
                    selected = selectedTab == if (isAdmin) EarningTabs.ADMIN else EarningTabs.PROFILE,
                    onClick = { selectedTab = if (isAdmin) EarningTabs.ADMIN else EarningTabs.PROFILE },
                    icon = {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                            contentDescription = "ProfileAdmin"
                        )
                    },
                    label = { Text(if (isAdmin) "এডমিন" else "প্রোফাইল", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (isAdmin) Color(0xFFC62828) else GreenPrimary,
                        unselectedIconColor = Color(0xFF49454F),
                        selectedTextColor = if (isAdmin) Color(0xFFC62828) else GreenPrimary,
                        unselectedTextColor = Color(0xFF49454F),
                        indicatorColor = if (isAdmin) Color(0xFFC62828).copy(alpha = 0.2f) else Color(0xFFE8DEF8)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                EarningTabs.HOME -> HomeScreenTab(viewModel, onEarnClick = { selectedTab = EarningTabs.EARN })
                EarningTabs.EARN -> EarnTab(viewModel)
                EarningTabs.REFER -> ReferTab(viewModel)
                EarningTabs.WITHDRAW -> WithdrawTab(viewModel, onHistoryClick = { selectedTab = EarningTabs.HISTORY })
                EarningTabs.PROFILE -> ProfileTab(viewModel, onNavigateToHistory = { selectedTab = EarningTabs.HISTORY }, onLogout = onLogout)
                EarningTabs.HISTORY -> HistoryTab(viewModel, onBack = { selectedTab = EarningTabs.WITHDRAW })
                EarningTabs.ADMIN -> if (user?.role == "ADMIN") AdminTab(viewModel) else selectedTab = EarningTabs.HOME
            }
        }
    }
}

// ==========================================
// TAB 1: HOME TAB
// ==========================================

@Composable
fun HomeScreenTab(
    viewModel: EarnViewModel,
    onEarnClick: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val notifications by viewModel.userNotifications.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp)
    ) {
        // Welcoming Point Statement Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(28.dp))
                    .drawBehind {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = 90.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(size.width - 20.dp.toPx(), 20.dp.toPx())
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = GreenPrimary), // #6750A4
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "বর্তমান ব্যালেন্স",
                        fontSize = 12.sp,
                        color = Color(0xFFEADDFF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = "${user?.points ?: 0}",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "পয়েন্ট",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEADDFF),
                            modifier = Modifier.align(Alignment.Bottom).padding(bottom = 6.dp)
                        )
                    }
                    
                    val bdtAmount = ((user?.points ?: 0) / 100.0)
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "টাকায় রূপান্তরিত",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "৳${String.format("%.2f", bdtAmount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = { viewModel.convertPointsToMoney() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Cached, contentDescription = "Convert", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("কনভার্ট", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- BENGALI GUIDE (কাজের সঠিক নিয়ম ও নির্দেশনাবলী) ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, GreenSecondary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LibraryBooks,
                            contentDescription = "Instruction",
                            tint = GreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "অ্যাপে কাজের নিয়ম (Task Steps) 📋",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Task Lists in Bengali (easy to read)
                    val stepsList = listOf(
                        "১. দৈনিক হাজিরা বোনাস: প্রতিদিন নিয়ম করে 'দৈনিক বোনাস' বাটন চেপে ১৫০ পয়েন্ট অর্জন করুন।",
                        "২. লাকি স্পিন হুইল: স্পিন ঘুরিয়ে ১০ বার পর্যন্ত পয়েন্ট সংগ্রহ করুন। প্রতিবার ভিন্ন ভিন্ন পয়েন্ট পাওয়া যায়।",
                        "৩. স্ক্র্যাচ অ্যান্ড উইন: কার্ড স্ক্র্যাচ করে দৈনিক ১০ বার নগদ পয়েন্ট সংগ্রহ করুন।",
                        "৪. ভিডিও বিজ্ঞাপন কুইজ: ভিডিও দেখে প্রতিবার ২০০ পয়েন্ট সংগ্রহ করুন। দৈনিক ১০ বার ভিডিও দেখা যাবে।",
                        "৫. বন্ধুদের সাথে রেফার: আপনার রেফার কোড ব্যবহার করালে আপনি পাবেন ৫০০ পয়েন্ট এবং বন্ধু পাবে ৩০০ পয়েন্ট !",
                        "৬. ন্যূনতম উথড্র সীমা: ১০০০ পয়েন্ট (১০ টাকা)। সঠিক উপায়ে কাজ করলেই ২৪ ঘণ্টার মধ্যে পেমেন্ট সম্পন্ন করা হয়।"
                    )
                    
                    stepsList.forEach { step ->
                        Text(
                            text = step,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333),
                            modifier = Modifier.padding(vertical = 4.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Quick Actions Grid Header
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "আজকের পকেট আয় টাস্ক 💰",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Quick Exercises Grid Row
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                // CheckIn button card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                        .clickable { viewModel.claimDailyCheckIn() },
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceLight),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, GeometricBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(RedBg, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Bonus",
                                tint = RedText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "দৈনিক বোনাস",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "প্রতিদিন ১ বার",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Earn Tab quick entry
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                        .clickable { onEarnClick() },
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceLight),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, GeometricBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(BlueBg, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = "Games",
                                tint = BlueText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "সহজ গেমস টাস্ক",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "স্পিন ও স্ক্র্যাচ",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Fake/Active Google AdMob simulation
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(Color.LightGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Ad", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google AdMob Sponsor: Watch & win premium coins!", fontSize = 10.sp, color = Color.DarkGray)
                    }
                    Icon(Icons.Default.Info, contentDescription = "AdInfo", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }

        // In-App Notifications from Firebase/Admin Panel
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "জরুরী ঘোষণা ও অফারসমূহ 🔔",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (notifications.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        text = "নতুন কোনো বিজ্ঞাপন বা নোটিফিকেশন নেই।",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(notifications) { notification ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = notification.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenPrimary
                            )
                            Text(
                                text = formatTime(notification.timestamp),
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = notification.message,
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 2: EARN EXERCISES TAB
// ==========================================

@Composable
fun EarnTab(viewModel: EarnViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val isWatching by viewModel.isVideoWatching.collectAsState()
    val watchSecondsLeft by viewModel.videoCountdown.collectAsState()
    var spinRotationDegrees by remember { mutableStateOf(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Video Watch simulated countdown timer
    LaunchedEffect(isWatching, watchSecondsLeft) {
        if (isWatching && watchSecondsLeft > 0) {
            delay(1000)
            if (watchSecondsLeft == 1) {
                // Complete
                viewModel.completeVideoWatch()
            } else {
                viewModel.tickVideoCountdown()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("আপনার আজকের টাস্ক লিমিট", fontSize = 11.sp, color = Color.Gray)
                        Text("স্পিন: ${user?.spinsLeftToday ?: 0}/১০ | স্ক্র্যাচ: ${user?.scratchesLeftToday ?: 0}/১০ | ভিডিও: ${user?.videosLeftToday ?: 0}/১০", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                    TextButton(onClick = { viewModel.claimDailyCheckIn() }) {
                        Text("রিসেট করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenSecondary)
                    }
                }
            }
        }

        // GAMES PANEL
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("১. লাকি হুইল স্পিন করুন 🎡", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))
            
            // Interactive Spin representation
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .rotate(spinRotationDegrees)
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFFE57373), Color(0xFFFFF176),
                                        Color(0xFF81C784), Color(0xFF64B5F6),
                                        Color(0xFFBA68C8), Color(0xFFE57373)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(3.dp, GreenPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Lines/text markers inside Wheel
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color.Black, radius = 5f)
                        }
                        Text("💰\nপয়েন্ট\nহুইল", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.DarkGray, textAlign = TextAlign.Center)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            if (user?.spinsLeftToday ?: 0 <= 0) {
                                scope.launch {
                                    android.widget.Toast.makeText(context, "আর স্পিন নেই!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                return@Button
                            }
                            if (!isSpinning) {
                                isSpinning = true
                                scope.launch {
                                    val spinsAngle = 360f * 5 + Random.nextInt(360)
                                    spinRotationDegrees = spinsAngle
                                    delay(1500) // Spin animation holds 1.5s
                                    isSpinning = false
                                    val possiblePoints = listOf(10, 20, 30, 50, 80, 100, 150)
                                    val finalAward = possiblePoints.random()
                                    viewModel.spinWheelAndEarn(finalAward)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        enabled = !isSpinning && (user?.spinsLeftToday ?: 0 > 0),
                        modifier = Modifier.testTag("spin_button")
                    ) {
                        Text(if (isSpinning) "ঘুরছে..." else "স্পিন করুন! 🎡")
                    }
                }
            }
        }

        // SCRATCH CARD
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("২. লাকি স্ক্র্যাচ কার্ড অফার 🎁", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var isCardScratched by remember { mutableStateOf(false) }
                    var scratchValue by remember { mutableIntStateOf(0) }
                    
                    // Scratch area simulation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCardScratched) GoldAccent.copy(alpha = 0.2f) else Color.LightGray)
                            .border(2.dp, if (isCardScratched) GoldAccent else Color.Gray, RoundedCornerShape(8.dp))
                            .clickable {
                                if (user?.scratchesLeftToday ?: 0 <= 0) {
                                    return@clickable
                                }
                                if (!isCardScratched) {
                                    scratchValue = Random.nextInt(20, 120)
                                    isCardScratched = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCardScratched) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Stars, contentDescription = "Points Won", tint = GoldAccent, modifier = Modifier.size(32.dp))
                                Text("অভিনন্দন! আপনি পেলেন +$scratchValue পয়েন্ট", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                            }
                        } else {
                            Text("👉 কার্ডটি ঘষুন বা এখানে ক্লিক করুন 👈", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.scratchCardAndEarn(scratchValue)
                            // Reset state for next card scratch
                            isCardScratched = false
                            scratchValue = 0
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSecondary),
                        enabled = isCardScratched && (user?.scratchesLeftToday ?: 0 > 0),
                        modifier = Modifier.testTag("scratch_claim_button")
                    ) {
                        Text("পয়েন্ট দাবি করুন 📥")
                    }
                }
            }
        }

        // WATCH AND EARN
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text("৩. ভিডিও অফার দেখে আয় 📺", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.OndemandVideo,
                        contentDescription = "Video",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("প্রতি ভিডিও কুইজ সম্পূর্ণ করলে পাবেন ২০০ পয়েন্ট!", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.startVideoWatch() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        enabled = !isWatching && (user?.videosLeftToday ?: 0 > 0),
                        modifier = Modifier.testTag("watch_video_button")
                    ) {
                        Text("ভিডিও বিজ্ঞাপন দেখুন 📺")
                    }
                }
            }
        }
    }

    // Video Play Fullscreen Dialog Simulation
    if (isWatching) {
        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("বিজ্ঞাপন ভিডিও চলছে...", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = GoldAccent, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Real Cash BD Sponsored Ad", fontSize = 10.sp, color = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("অনুগ্রহ করে অপেক্ষা করুন: $watchSecondsLeft সে.", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ভিডিওটি শেষ হওয়ার আগে ট্যাব বন্ধ করবেন না।", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

// ==========================================
// TAB 3: REFERRAL TAB
// ==========================================

@Composable
fun ReferTab(viewModel: EarnViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(GreenSecondary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Group, contentDescription = "Refer", tint = GreenPrimary, modifier = Modifier.size(54.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("বন্ধুদের রেফার করে আয় করুন 👥", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("আপনার রেফার কোড বন্ধুদের সাথে শেয়ার করলেই উপার্জন শুরু!", fontSize = 12.sp, color = Color.Gray)
        }

        // Referral Card code display
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("অনন্য রেফার কোড", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .border(1.dp, GreenPrimary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = user?.referralCode ?: "RAKIB01",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GreenPrimary,
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(user?.referralCode ?: ""))
                            android.widget.Toast.makeText(context, "কোড কপি করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        modifier = Modifier.testTag("copy_referral_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("কোড কপি করুন")
                    }
                }
            }
        }

        // Commission rules description
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text("রেফারেল আয়ের নিয়মাবলী 🏆", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• প্রতি সফল আমন্ত্রণে আপনি পাবেন ৫০০ পয়েন্ট বোনাস।", fontSize = 11.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• আপনার বন্ধু একাউন্ট খুলেই সাথে সাথে ৩০০ পয়েন্ট বোনাস পাবে।", fontSize = 11.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• আজীবন ১০% কমিশন বোনাস: আপনার রেফারকৃত বন্ধু যা আয় করবে তার ১০% অতিরিক্ত কমিশন আপনার একাউন্টে যুক্ত হতে থাকবে।", fontSize = 11.sp, color = Color.DarkGray)
                }
            }
        }

        // Referral stats list summary
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "আমন্ত্রণ ইতিবৃত্ত (Invite History)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        val invitedUsers = listOf("user2@gmail.com", "sumon77@yahoo.com")
        items(invitedUsers) { email ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = "User", tint = GreenSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(email, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Text("+৫০০ পয়েন্ট বোনাস", fontSize = 10.sp, color = GreenPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 4: WITHDRAW TAB
// ==========================================

@Composable
fun WithdrawTab(
    viewModel: EarnViewModel,
    onHistoryClick: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    var selectedMethod by remember { mutableStateOf("bKash") }
    var accountNo by remember { mutableStateOf("") }
    var pointsToWithdraw by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // My Points summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("বর্তমান পয়েন্ট ব্যালেন্স", fontSize = 11.sp, color = Color.Gray)
                        Text("${user?.points ?: 0} পয়েন্ট", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = GreenPrimary)
                    }
                    Button(
                        onClick = onHistoryClick,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSecondary)
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("যাচাই করুন", fontSize = 11.sp)
                    }
                }
            }
        }

        // Method Selector
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Text("উত্তোলনের মাধ্যম নির্বাচন করুন 🏦", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenPrimary, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("bKash", "Nagad", "Rocket", "Flexiload").forEach { method ->
                    val isSel = selectedMethod == method
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clickable { selectedMethod = method },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSel) GreenPrimary else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isSel) GreenPrimary else Color.LightGray)
                    ) {
                        Text(
                            text = method,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else Color.DarkGray,
                            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Limits Details card
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚠️ সীমা ও বিনিময় নির্দেশিকা:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    Text("• ফ্লেক্সিলোড (Flexiload): সর্বনিম্ন ১,০০০ পয়েন্ট = ১০ টাকা।", fontSize = 11.sp, color = Color.DarkGray)
                    Text("• বিকাশ, নগদ ও রকেট: সর্বনিম্ন ৫,০০০ পয়েন্ট = ৫০ টাকা।", fontSize = 11.sp, color = Color.DarkGray)
                }
            }
        }

        // Inputs
        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = accountNo,
                onValueChange = { accountNo = it },
                label = { Text("$selectedMethod নম্বর প্রদান করুন") },
                placeholder = { Text("যেমন: 017xxxxxxxx") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("withdraw_number"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = pointsToWithdraw,
                onValueChange = { pointsToWithdraw = it },
                label = { Text("পয়েন্টের পরিমাণ নির্ধারণ করুন") },
                placeholder = { Text("যেমন: 5000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("withdraw_points"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val pts = pointsToWithdraw.toIntOrNull() ?: 0
                    viewModel.requestWithdraw(selectedMethod, accountNo, pts)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("withdraw_submit"),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("উত্তোলন অনুরোধ পাঠান 🏦", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// TAB 5: HISTORY TAB
// ==========================================

@Composable
fun HistoryTab(
    viewModel: EarnViewModel,
    onBack: () -> Unit
) {
    val requests by viewModel.userWithdrawRequests.collectAsState()
    val logs by viewModel.userEarningLogs.collectAsState()

    var showWithdrawLogs by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("লেজার হিস্টোরি ও উথড্র 📊", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Box(modifier = Modifier.width(48.dp))
        }

        // Selector tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Button(
                onClick = { showWithdrawLogs = true },
                colors = ButtonDefaults.buttonColors(containerColor = if (showWithdrawLogs) GreenPrimary else Color.LightGray),
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Text("উত্তোলন রেকর্ড", fontSize = 11.sp, color = if (showWithdrawLogs) Color.White else Color.DarkGray)
            }

            Button(
                onClick = { showWithdrawLogs = false },
                colors = ButtonDefaults.buttonColors(containerColor = if (!showWithdrawLogs) GreenPrimary else Color.LightGray),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Text("পয়েন্ট পরিবর্তন লগ", fontSize = 11.sp, color = if (!showWithdrawLogs) Color.White else Color.DarkGray)
            }
        }

        if (showWithdrawLogs) {
            if (requests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("ইতিপূর্বে কোনো টাকা তোলার রেকর্ড খুঁজে পাওয়া যায়নি।")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = requests) { req ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${req.paymentMethod} • ${req.number}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(formatTime(req.requestTime), fontSize = 10.sp, color = Color.Gray)
                                    Text("${req.pointsAmount} পয়েন্ট (৳${req.cashAmount})", fontSize = 12.sp, color = GreenPrimary)
                                }
                                
                                // Status indicator wrapper
                                val pillBg = when (req.status) {
                                    "PENDING" -> Color(0xFFFFF8E1)
                                    "APPROVED" -> Color(0xFFE8F5E9)
                                    else -> Color(0xFFFFEBEE)
                                }
                                val pillColor = when (req.status) {
                                    "PENDING" -> Color(0xFFFFA000)
                                    "APPROVED" -> GreenPrimary
                                    else -> Color(0xFFC62828)
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = pillBg),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = when(req.status){
                                            "PENDING" -> "পেনন্ডিং"
                                            "APPROVED" -> "সফল হয়েছে"
                                            else -> "বাতিল"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pillColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("পয়েন্ট আয়ের ইতিহাস খালি আছে। টাস্ক পূরণ করে ক্রেডিট নিশ্চিত করুন।")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = logs) { log ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(log.description, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(formatTime(log.timestamp), fontSize = 9.sp, color = Color.Gray)
                                }
                                Text(
                                    text = if (log.points >= 0) "+${log.points}" else "${log.points}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (log.points >= 0) GreenPrimary else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 6: PROFILE TAB
// ==========================================

@Composable
fun ProfileTab(
    viewModel: EarnViewModel,
    onNavigateToHistory: () -> Unit,
    onLogout: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        
        // Avatar representation
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(GreenPrimary, CircleShape)
                .border(3.dp, GoldAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(user?.name ?: "ব্যবহারকারী", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
        Text(user?.email ?: "user@gmail.com", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        // Device & Role Stats Check (One Device Login Verification as asked)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = "Security", tint = Color(0xFF4C9A2A), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ডিভাইস ও নিরাপত্তা বিবরণী (One Device)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("• ডিভাইস আইডি: ${viewModel.deviceId}", fontSize = 11.sp, color = Color.DarkGray)
                Text("• স্ট্যাটাস: ${user?.status ?: "ACTIVE"}", fontSize = 11.sp, color = Color.DarkGray)
                Text("• ওয়ান ডিভাইস লগইন: সুরক্ষিত নিশ্চিত করা হয়েছে ✅", fontSize = 11.sp, color = Color.DarkGray)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions
        OutlinedButton(
            onClick = { onNavigateToHistory() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
            border = BorderStroke(1.dp, GreenPrimary)
        ) {
            Icon(Icons.Default.ListAlt, contentDescription = "History")
            Spacer(modifier = Modifier.width(8.dp))
            Text("আমার পেমেন্ট লেনদেন হিস্টোরি")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                viewModel.logout()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text("লগআউট অফলাইন করুন", fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// MASTER TAB 7: ADMIN PANEL TAB
// ==========================================

@Composable
fun AdminTab(viewModel: EarnViewModel) {
    val totalUsers by viewModel.totalUsersCount.collectAsState()
    val pendingCount by viewModel.pendingWithdrawCount.collectAsState()
    val totalWithdrawnPoints by viewModel.totalApprovedWithdrawPoints.collectAsState()
    
    val usersList by viewModel.allUsers.collectAsState()
    val withdrawsList by viewModel.allWithdrawRequests.collectAsState()

    var activeAdminTab by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Payments, 2: Users, 3: Notifications

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        // Admin header banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFC62828))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("এডমিন কন্ট্রোল প্যানেল 🛡️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Real Cash BD - রিয়েল-টাইম এডমিন", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text("🔒 রিলিজ", color = Color(0xFFC62828), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                }
            }
        }

        // Horizontal sub tabs selector
        ScrollableTabRow(
            selectedTabIndex = activeAdminTab,
            containerColor = Color.White,
            contentColor = Color(0xFFC62828)
        ) {
            Tab(selected = activeAdminTab == 0, onClick = { activeAdminTab = 0 }) {
                Text("ড্যাশবোর্ড", modifier = Modifier.padding(14.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeAdminTab == 1, onClick = { activeAdminTab = 1 }) {
                Text("পেমেন্ট পেন্ডিং ($pendingCount)", modifier = Modifier.padding(14.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeAdminTab == 2, onClick = { activeAdminTab = 2 }) {
                Text("ব্যবহারকারী তালিকা ($totalUsers)", modifier = Modifier.padding(14.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeAdminTab == 3, onClick = { activeAdminTab = 3 }) {
                Text("ঘোষণা ও নোটিফিকেশন", modifier = Modifier.padding(14.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (activeAdminTab) {
                0 -> {
                    // Dashboard stats View
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("মোট সিস্টেম এনালিটিক্স 📈", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("মোট নিবন্ধিত ইউজার", fontSize = 10.sp, color = Color.Gray)
                                    Text("$totalUsers জন", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("পেন্ডিং পেমেন্ট সংখ্যা", fontSize = 10.sp, color = Color.Gray)
                                    Text("$pendingCount টি আবেদন", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE65100))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("মোট এপ্রুভ হওয়া অর্থ উত্তোলিত", fontSize = 11.sp, color = Color.Gray)
                                val bdtTotal = totalWithdrawnPoints / 100.0
                                Text("$totalWithdrawnPoints Points (৳$bdtTotal BDT)", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                            }
                        }

                        // Analytics Chart Simulation
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("দৈনিক উপার্জন চার্ট (সিমুলেশন) 📊", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                // Draw bar lines using Compose drawing APIs or simple visual indicators
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val simulatedData = listOf(0.3f, 0.5f, 0.2f, 0.7f, 0.9f, 0.6f, 0.8f)
                                    val days = listOf("শনি", "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহ", "শুক্র")
                                    simulatedData.forEachIndexed { idx, bar ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .width(18.dp)
                                                    .height(70.dp * bar)
                                                    .background(Color(0xFFC62828), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(days[idx], fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                1 -> {
                    // Pending/Approved list controls
                    val pendingList = withdrawsList.filter { it.status == "PENDING" }
                    if (pendingList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("নতুন কোনো উথড্র আবেদন পেন্ডিং নেই! 👍")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = pendingList) { req ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text("নাম: ${req.userName}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("নম্বর ও মাধ্যম: ${req.paymentMethod} (${req.number})", fontSize = 12.sp, color = Color.DarkGray)
                                        Text("পয়েন্ট: ${req.pointsAmount} (কর: ৳${req.cashAmount})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                        Text("সময়: ${formatTime(req.requestTime)}", fontSize = 9.sp, color = Color.Gray)
                                        
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Button(
                                                onClick = { viewModel.approveWithdraw(req.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                                            ) {
                                                Text("এপ্রুভ করুন ✅", fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = { viewModel.rejectWithdraw(req.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                                            ) {
                                                Text("বাতিল করুন ❌", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Users list with dynamic blocking action as fake account prevention!
                    if (usersList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = usersList) { usr ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(usr.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(usr.email, fontSize = 11.sp, color = Color.Gray)
                                            Text("ব্যালেন্স: ${usr.points} পয়েন্ট", fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (usr.status == "ACTIVE") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                                )
                                            ) {
                                                Text(
                                                    text = if (usr.status == "ACTIVE") "ACTIVE (সক্রিয়)" else "BANNED (বন্ধ)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (usr.status == "ACTIVE") GreenPrimary else Color(0xFFC62828),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Action button to Ban / Unban
                                        Button(
                                            onClick = { viewModel.toggleUserBan(usr.id, usr.status) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (usr.status == "ACTIVE") Color(0xFFC62828) else GreenPrimary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(if (usr.status == "ACTIVE") "ব্লক করুন ⚠️" else "সক্রিয় করুন ✅", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Send notification controls
                    var notificationTitle by remember { mutableStateOf("") }
                    var notificationMsg by remember { mutableStateOf("") }

                    Column {
                        Text("বিজ্ঞপ্তি বা নোটিশ পাঠান 📢", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = notificationTitle,
                            onValueChange = { notificationTitle = it },
                            label = { Text("বিজ্ঞপ্তি টাইটেল (Title)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = notificationMsg,
                            onValueChange = { notificationMsg = it },
                            label = { Text("বিজ্ঞপ্তি বার্তা (Message body)") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.adminSendGlobalNotification(notificationTitle, notificationMsg)
                                // Reset fields
                                notificationTitle = ""
                                notificationMsg = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("সর্বত্র পুশ নোটিফিকেশন পাঠান 🚀", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
