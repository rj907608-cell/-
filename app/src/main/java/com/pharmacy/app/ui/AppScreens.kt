package com.pharmacy.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.PharmacyPrimary
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusSuccessGreen
import com.example.ui.theme.StatusWarningAmber
import com.pharmacy.app.data.CartItem
import com.pharmacy.app.data.InvoiceSummary
import com.pharmacy.app.data.MedicineBatch
import com.pharmacy.app.data.MedicineEntity
import com.pharmacy.app.data.SaleRecordEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * علامات التبويب الرئيسية للتطبيق
 */
enum class AppTab(val title: String, val icon: ImageVector) {
    POS("نقطة البيع", Icons.Default.PointOfSale),
    INVENTORY("المخزون", Icons.Default.Inventory2),
    ALERTS("التنبيهات", Icons.Default.NotificationsActive),
    REPORTS("التقارير", Icons.Default.Assessment)
}

/**
 * الشاشة الحاوية الرئيسية (Main Container Screen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPharmacyScreen(viewModel: MainViewModel) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { AppTab.entries.size })
    val coroutineScope = rememberCoroutineScope()

    var showScannerDialog by remember { mutableStateOf(false) }
    var medicineToEdit by remember { mutableStateOf<MedicineEntity?>(null) }
    var showAddMedicineDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val pendingBarcode by viewModel.pendingBarcodeForAdd.collectAsStateWithLifecycle()

    val totalAlerts by viewModel.totalAlertsCount.collectAsStateWithLifecycle()
    val defaultMinStockAlert by viewModel.defaultMinStockAlert.collectAsStateWithLifecycle()

    var activeMessageJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(uiMessage) {
        val msg = uiMessage
        if (msg != null) {
            activeMessageJob?.cancel()
            activeMessageJob = coroutineScope.launch {
                val showJob = launch {
                    snackbarHostState.showSnackbar(
                        message = msg,
                        duration = SnackbarDuration.Indefinite
                    )
                }
                // تظهر وتختفي بسرعة فائقة (1.2 ثانية) بناءً على طلب المستخدم
                delay(1200L)
                showJob.cancel()
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            viewModel.clearUiMessage()
        }
    }

    LaunchedEffect(pendingBarcode) {
        if (pendingBarcode != null) {
            showAddMedicineDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocalPharmacy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "إدارة الصيدلية",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

                    // زر التبديل بين الوضع الداكن والفاتح
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("toggle_theme_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "الوضع النهاري" else "الوضع الليلي",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // زر تشغيل ماسح الباركود
                    IconButton(
                        onClick = { showScannerDialog = true },
                        modifier = Modifier.testTag("open_scanner_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "مسح الباركود",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // زر الملف الشخصي ومعلومات الحساب وتسجيل الخروج
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("open_profile_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "الملف الشخصي والحساب",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // زر إعدادات الصيدلية والتنبيهات
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("open_settings_action")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات والتنبيهات",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = pagerState.currentPage == tab.ordinal
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.scrollToPage(tab.ordinal)
                            }
                        },
                        icon = {
                            if (tab == AppTab.ALERTS && totalAlerts > 0) {
                                BadgedBox(badge = { Badge { Text("$totalAlerts") } }) {
                                    Icon(tab.icon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (pagerState.currentPage == AppTab.INVENTORY.ordinal) {
                FloatingActionButton(
                    onClick = {
                        medicineToEdit = null
                        showAddMedicineDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_medicine_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة دواء")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD4EDDA), // لون أخضر فاتح راقٍ ومريح
                        contentColor = Color(0xFF155724)    // نص أخضر داكن واضح
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF28A745).copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF28A745),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = data.visuals.message,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF155724),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            beyondViewportPageCount = 3,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (AppTab.entries[page]) {
                AppTab.POS -> PosScreen(
                    viewModel = viewModel,
                    onOpenScanner = { showScannerDialog = true }
                )
                AppTab.INVENTORY -> InventoryScreen(
                    viewModel = viewModel,
                    onEditMedicine = { med ->
                        medicineToEdit = med
                        showAddMedicineDialog = true
                    }
                )
                AppTab.ALERTS -> AlertsScreen(
                    viewModel = viewModel,
                    onOpenSettings = { showSettingsDialog = true }
                )
                AppTab.REPORTS -> ReportsScreen(viewModel = viewModel)
            }
        }
    }

    // نافذة إعدادات الصيدلية والتنبيهات
    val expiryAlertDays by viewModel.expiryAlertDays.collectAsStateWithLifecycle()
    if (showSettingsDialog) {
        SettingsDialog(
            currentExpiryDays = expiryAlertDays,
            currentDefaultMinStock = defaultMinStockAlert,
            viewModel = viewModel,
            onSaveSettings = { days, minStock ->
                viewModel.updateExpiryAlertDays(days)
                viewModel.updateDefaultMinStockAlert(minStock)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // نافذة الملف الشخصي ومعلومات الحساب وتسجيل الخروج
    if (showProfileDialog) {
        UserProfileDialog(
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false }
        )
    }

    // نافذة ماسح الباركود
    if (showScannerDialog) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                viewModel.onBarcodeScanned(barcode)
            },
            onDismiss = { showScannerDialog = false }
        )
    }

    // نافذة إضافة / تعديل دواء
    if (showAddMedicineDialog) {
        AddEditMedicineDialog(
            initialMedicine = medicineToEdit,
            presetBarcode = pendingBarcode ?: "",
            defaultMinStockAlert = defaultMinStockAlert,
            onSave = { savedMed ->
                if (medicineToEdit != null) {
                    viewModel.updateMedicine(savedMed)
                } else {
                    viewModel.saveMedicine(savedMed)
                }
                viewModel.clearPendingBarcode()
                showAddMedicineDialog = false
                medicineToEdit = null
            },
            onDismiss = {
                viewModel.clearPendingBarcode()
                showAddMedicineDialog = false
                medicineToEdit = null
            },
            onScanBarcodeRequested = {
                showScannerDialog = true
            }
        )
    }
}

/**
 * 1. شاشة نقطة البيع (POS Screen)
 */
@Composable
fun PosScreen(
    viewModel: MainViewModel,
    onOpenScanner: () -> Unit
) {
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val cartTotal by viewModel.cartTotal.collectAsStateWithLifecycle()
    val medicines by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showQuickAddList by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // شريط البحث المباشر ومسح الباركود
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("ابحث باسم الدواء أو رقمه...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pos_search_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onOpenScanner,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                modifier = Modifier.testTag("pos_barcode_btn")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("مسح")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // شريط الأدوية السريعة عند البحث
        AnimatedVisibility(visible = searchQuery.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(bottom = 8.dp)
            ) {
                if (medicines.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("لا يوجد دواء مطابق للبحث", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(medicines) { med ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addToCart(med, 1)
                                        viewModel.onSearchQueryChanged("")
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(med.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("المخزون: ${med.quantity} | السعر: ${med.sellPrice} ل.س", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.addToCart(med, 1)
                                        viewModel.onSearchQueryChanged("")
                                    },
                                    enabled = med.quantity > 0,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("إضافة", fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // بطاقة ملخص السلة وحالتها
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "إجمالي الفاتورة",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", cartTotal)} ل.س",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الأصناف: ${cart.sumOf { it.quantity }} قطعة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // قائمة عناصر السلة الحالية
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "محتويات السلة الحالية:",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            if (cart.isNotEmpty()) {
                TextButton(onClick = { viewModel.clearCart() }) {
                    Text("إفراغ السلة", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (cart.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "السلة فارغة حالياً",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "امسح باركود الدواء أو اختر من القائمة للبيع المباشر",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cart, key = { it.medicine.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.medicine.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "سعر البيع: ${item.effectiveSellPrice} ل.س | الباركود: ${item.medicine.barcode}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "حساب الفاتورة: ${item.quantity} × ${item.effectiveSellPrice} = ${String.format(Locale.US, "%.2f", item.subtotal)} ل.س",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // أزرار زيادة ونقصان الكمية
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(item.medicine.id, item.quantity - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "تقليل")
                                }
                                Text(
                                    text = "${item.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.updateCartQuantity(item.medicine.id, item.quantity + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "زيادة")
                                }
                                IconButton(
                                    onClick = { viewModel.removeFromCart(item.medicine.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // زر إتمام البيع النهائي
        Button(
            onClick = { viewModel.checkoutCart() },
            enabled = cart.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = StatusSuccessGreen
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("checkout_button")
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "إتمام البيع وخصم المخزون (${String.format(Locale.US, "%.2f", cartTotal)} ل.س)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 2. شاشة إدارة المخزون (Inventory Screen)
 */
@Composable
fun InventoryScreen(
    viewModel: MainViewModel,
    onEditMedicine: (MedicineEntity) -> Unit
) {
    val medicines by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("الكل") }

    val categories = listOf("الكل", "مسكنات وخافض حرارة", "مضادات حيوية", "أدوية المعدة والجهاز الهضمي", "مكملات غذائية وفيتامينات", "حساسية ومضادات الهيستامين")

    val filteredList = remember(medicines, selectedCategory) {
        if (selectedCategory == "الكل") {
            medicines
        } else {
            medicines.filter { it.category == selectedCategory }
        }
    }
    val totalPieces = remember(filteredList) { filteredList.sumOf { it.quantity } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // حقل البحث
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            placeholder = { Text("بحث في المخزون (اسم، باركود)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "مسح")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inventory_search_field")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // تصنيفات الأدوية
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "إجمالي الأدوية: ${filteredList.size}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "إجمالي القطع: $totalPieces",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val expiryAlertDays by viewModel.expiryAlertDays.collectAsStateWithLifecycle()

        // قائمة بطاقات الأدوية
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد أدوية مطابقة للمعايير", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { med ->
                    MedicineItemCard(
                        medicine = med,
                        expiryAlertDays = expiryAlertDays,
                        onEdit = { onEditMedicine(med) },
                        onDelete = { viewModel.deleteMedicine(med) },
                        onStockAdjust = { delta -> viewModel.adjustStock(med.id, delta) },
                        onAddToCart = { viewModel.addToCart(med, 1) }
                    )
                }
            }
        }
    }
}

/**
 * بطاقة عرض الدواء في المخزون
 */
@Composable
fun MedicineItemCard(
    medicine: MedicineEntity,
    expiryAlertDays: Int = 30,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStockAdjust: (Int) -> Unit,
    onAddToCart: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBatchDetailsDialog by remember { mutableStateOf(false) }

    val isLowStock = medicine.quantity <= medicine.minStockAlert
    val isExpired = medicine.expiryDate > 0L && medicine.expiryDate < System.currentTimeMillis()
    val isExpiringSoon = medicine.expiryDate > 0L && !isExpired && medicine.expiryDate <= (System.currentTimeMillis() + (expiryAlertDays.toLong() * 24 * 60 * 60 * 1000))

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    val formattedExpiry = remember(medicine.expiryDate) {
        if (medicine.expiryDate > 0L) dateFormat.format(Date(medicine.expiryDate)) else "غير محدد"
    }
    val batches = remember(medicine.batchesJson, medicine.quantity) { medicine.getBatches() }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showBatchDetailsDialog = true }
            .testTag("medicine_card_${medicine.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // الصف العلوي: الاسم والشارات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val locationText = if (medicine.location.isNotBlank()) " • المكان: ${medicine.location}" else ""
                    Text(
                        text = "${medicine.category}$locationText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // شارات التنبيه والدفعات
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (batches.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${batches.size} دفعات",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isExpired) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "منتهي!",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isExpiringSoon) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusWarningAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "صلاحية قريبة",
                                color = StatusWarningAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (isLowStock) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusAlertRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "نقص مخزون",
                                color = StatusAlertRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // الباركود ومعلومات السعر
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = medicine.barcode,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "سعر البيع: ${medicine.sellPrice} ل.س (شراء: ${medicine.buyPrice} ل.س)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // الصلاحية وكمية المخزون مع التحكم
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "الصلاحية: $formattedExpiry",
                        fontSize = 12.sp,
                        color = if (isExpired) StatusAlertRed else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // عداد المخزون وأزرار + و -
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { onStockAdjust(-1) },
                        enabled = medicine.quantity > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "نقص 1", modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = "${medicine.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isLowStock) StatusAlertRed else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    IconButton(
                        onClick = { onStockAdjust(1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "زيادة 1", modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // شريط الإجراءات: بيع، تعديل، حذف
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onAddToCart,
                    enabled = medicine.quantity > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بيع", fontSize = 12.sp)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("حذف الدواء") },
            text = { Text("هل تريد حذف الدواء بالكامل من الصيدلية؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، احذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showBatchDetailsDialog) {
        MedicineBatchDetailsDialog(
            medicine = medicine,
            onDismiss = { showBatchDetailsDialog = false }
        )
    }
}

/**
 * 3. شاشة التنبيهات الذكية (Alerts Screen)
 */
@Composable
fun AlertsScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val lowStockMedicines by viewModel.lowStockMedicines.collectAsStateWithLifecycle()
    val expiredMedicines by viewModel.expiredMedicines.collectAsStateWithLifecycle()
    val expiringSoonMedicines by viewModel.expiringSoonMedicines.collectAsStateWithLifecycle()
    val expiryAlertDays by viewModel.expiryAlertDays.collectAsStateWithLifecycle()

    var activeAlertTab by remember { mutableIntStateOf(0) }
    val alertTabs = listOf(
        "منتهية الصلاحية (${expiredMedicines.size})",
        "نواقص المخزون (${lowStockMedicines.size})",
        "قريبة الانتهاء (${expiringSoonMedicines.size})"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = activeAlertTab) {
            alertTabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeAlertTab == index,
                    onClick = { activeAlertTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = if (activeAlertTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeAlertTab) {
            0 -> {
                // منتهية الصلاحية
                if (expiredMedicines.isEmpty()) {
                    EmptyAlertPlaceholder("لا توجد أي أدوية منتهية الصلاحية حالياً. ممتاز!")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(expiredMedicines) { med ->
                            AlertItemCard(
                                title = med.name,
                                subtitle = "الكمية المتبقية: ${med.quantity} عبوة | الباركود: ${med.barcode}",
                                alertText = "منتهي الصلاحية! يجب إتلافه أو إرجاعه للمورد",
                                alertColor = StatusAlertRed,
                                actionLabel = "إتلاف / حذف",
                                onAction = { viewModel.deleteMedicine(med) }
                            )
                        }
                    }
                }
            }
            1 -> {
                // نواقص المخزون
                if (lowStockMedicines.isEmpty()) {
                    EmptyAlertPlaceholder("جميع الأدوية متوفرة بكميات كافية في المخزون.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(lowStockMedicines) { med ->
                            AlertItemCard(
                                title = med.name,
                                subtitle = "المتبقي: ${med.quantity} عبوة (الحد الأدنى للتنبيه: ${med.minStockAlert})",
                                alertText = "المخزون أوشك على النفاد، قم بطلب توريد جديد",
                                alertColor = StatusWarningAmber,
                                actionLabel = "تم التوريد",
                                onAction = { viewModel.dismissLowStockAlert(med.id) }
                            )
                        }
                    }
                }
            }
            2 -> {
                // قريبة الانتهاء
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التنبيه عند بقاء $expiryAlertDays يوماً أو أقل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onOpenSettings,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل المهلة ($expiryAlertDays يوم)", fontSize = 12.sp)
                    }
                }

                if (expiringSoonMedicines.isEmpty()) {
                    EmptyAlertPlaceholder("لا توجد أدوية تنتهي صلاحيتها خلال الـ $expiryAlertDays يوماً القادمة.")
                } else {
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(expiringSoonMedicines) { med ->
                            val daysLeft = ((med.expiryDate - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)).coerceAtLeast(0)
                            AlertItemCard(
                                title = med.name,
                                subtitle = "تاريخ الانتهاء: ${dateFormat.format(Date(med.expiryDate))} (متبقي $daysLeft يوم)",
                                alertText = "الدواء على وشك انتهاء الصلاحية",
                                alertColor = StatusWarningAmber,
                                actionLabel = "بيع الآن",
                                onAction = { viewModel.addToCart(med, 1) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyAlertPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = StatusSuccessGreen,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AlertItemCard(
    title: String,
    subtitle: String,
    alertText: String,
    alertColor: Color,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = alertColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = alertText,
                        color = alertColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(actionLabel, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * فترات التقرير المالي (يومي / شهري)
 */
enum class ReportPeriod(val label: String) {
    DAILY("تقرير يومي"),
    MONTHLY("تقرير شهري")
}

/**
 * 4. شاشة التقارير والأرباح (Financial Reports Screen)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.DAILY) }

    val filteredInvoices = remember(invoices, selectedPeriod) {
        val calendarNow = Calendar.getInstance()
        invoices.filter { inv ->
            val cal = Calendar.getInstance().apply { timeInMillis = inv.timestamp }
            when (selectedPeriod) {
                ReportPeriod.DAILY -> {
                    calendarNow.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    calendarNow.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                }
                ReportPeriod.MONTHLY -> {
                    calendarNow.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    calendarNow.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                }
            }
        }
    }

    val periodRevenue = remember(filteredInvoices) {
        filteredInvoices.sumOf { it.totalAmount }
    }
    val periodProfit = remember(filteredInvoices) {
        filteredInvoices.sumOf { it.totalProfit }
    }
    val periodItemsSold = remember(filteredInvoices) {
        filteredInvoices.sumOf { it.totalUnitsSold }
    }
    val profitMargin = if (periodRevenue > 0) (periodProfit / periodRevenue) * 100 else 0.0

    // توقيت 12 ساعة (ص/م أو AM/PM)
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }

    var selectedInvoiceForDetails by remember { mutableStateOf<InvoiceSummary?>(null) }
    var invoiceToDelete by remember { mutableStateOf<InvoiceSummary?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // زر التبديل بين تقرير يومي وتقرير شهري
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("reports_period_selector")
        ) {
            SegmentedButton(
                selected = selectedPeriod == ReportPeriod.DAILY,
                onClick = { selectedPeriod = ReportPeriod.DAILY },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {
                    if (selectedPeriod == ReportPeriod.DAILY) {
                        SegmentedButtonDefaults.Icon(active = true)
                    }
                },
                modifier = Modifier.testTag("report_daily_btn")
            ) {
                Text(
                    text = "تقرير يومي",
                    fontWeight = if (selectedPeriod == ReportPeriod.DAILY) FontWeight.Bold else FontWeight.Normal
                )
            }
            SegmentedButton(
                selected = selectedPeriod == ReportPeriod.MONTHLY,
                onClick = { selectedPeriod = ReportPeriod.MONTHLY },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {
                    if (selectedPeriod == ReportPeriod.MONTHLY) {
                        SegmentedButtonDefaults.Icon(active = true)
                    }
                },
                modifier = Modifier.testTag("report_monthly_btn")
            ) {
                Text(
                    text = "تقرير شهري",
                    fontWeight = if (selectedPeriod == ReportPeriod.MONTHLY) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // شبكة البطاقات المالية
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReportMetricCard(
                title = if (selectedPeriod == ReportPeriod.DAILY) "مبيعات اليوم" else "مبيعات الشهر",
                value = "${String.format(Locale.US, "%.2f", periodRevenue)} ل.س",
                icon = Icons.Default.MonetizationOn,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
                title = if (selectedPeriod == ReportPeriod.DAILY) "أرباح اليوم" else "أرباح الشهر",
                value = "${String.format(Locale.US, "%.2f", periodProfit)} ل.س",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = StatusSuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReportMetricCard(
                title = "القطع المباعة",
                value = "$periodItemsSold قطعة",
                icon = Icons.Default.Inventory2,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
                title = "هامش الربح",
                value = "${String.format(Locale.US, "%.1f", profitMargin)}%",
                icon = Icons.Default.Assessment,
                color = StatusWarningAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (selectedPeriod == ReportPeriod.DAILY) {
                "سجل المبيعات اليومية (${filteredInvoices.size} فاتورة):"
            } else {
                "سجل المبيعات الشهرية (${filteredInvoices.size} فاتورة):"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "اضغط على الفاتورة لعرض تفاصيلها، أو اضغط مطولاً لحذفها",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredInvoices.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedPeriod == ReportPeriod.DAILY) {
                            "لا توجد فواتير مسجلة اليوم"
                        } else {
                            "لا توجد فواتير مسجلة خلال هذا الشهر"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredInvoices.take(30).forEach { invoice ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { selectedInvoiceForDetails = invoice },
                                onLongClick = { invoiceToDelete = invoice }
                            )
                            .testTag("invoice_card_${invoice.invoiceId}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = invoice.invoiceId,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dateFormat.format(Date(invoice.timestamp)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${invoice.itemsCount} أدوية • ${invoice.totalUnitsSold} قطعة",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", invoice.totalAmount)} ل.س",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "ربح: +${String.format(Locale.US, "%.2f", invoice.totalProfit)} ل.س",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusSuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة عرض تفاصيل الأدوية المباعة في الفاتورة المحددة
    selectedInvoiceForDetails?.let { invoice ->
        InvoiceDetailsDialog(
            invoice = invoice,
            onDismiss = { selectedInvoiceForDetails = null }
        )
    }

    // نافذة تأكيد حذف الفاتورة عند الضغط المطول
    invoiceToDelete?.let { invoice ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "حذف الفاتورة",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف الفاتورة '${invoice.invoiceId}' وإلغاء جميع سجلات بيع الأدوية المرتبطة بها نهائياً؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInvoice(invoice.invoiceId)
                        invoiceToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، احذف الفاتورة")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { invoiceToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ReportMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * نافذة حوار إضافة وتعديل دواء (Add / Edit Medicine Dialog)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicineDialog(
    initialMedicine: MedicineEntity?,
    presetBarcode: String,
    defaultMinStockAlert: Int = 5,
    onSave: (MedicineEntity) -> Unit,
    onDismiss: () -> Unit,
    onScanBarcodeRequested: () -> Unit
) {
    var name by remember { mutableStateOf(initialMedicine?.name ?: "") }
    var barcode by remember { mutableStateOf(if (presetBarcode.isNotBlank()) presetBarcode else (initialMedicine?.barcode ?: "")) }
    var buyPriceText by remember { mutableStateOf(if (initialMedicine != null && initialMedicine.buyPrice > 0.0) initialMedicine.buyPrice.toString() else "") }
    var sellPriceText by remember { mutableStateOf(if (initialMedicine != null && initialMedicine.sellPrice > 0.0) initialMedicine.sellPrice.toString() else "") }
    var quantityText by remember { mutableStateOf(if (initialMedicine != null && initialMedicine.quantity > 0) initialMedicine.quantity.toString() else "") }
    var minStockText by remember {
        mutableStateOf(
            if (initialMedicine != null) {
                if (initialMedicine.minStockAlert > 0) initialMedicine.minStockAlert.toString() else ""
            } else {
                "" // يترك فارغاً ليأخذ القيمة الافتراضية المحددة في الإعدادات
            }
        )
    }
    var category by remember { mutableStateOf(initialMedicine?.category?.ifBlank { "أدوية عامة" } ?: "أدوية عامة") }
    var location by remember { mutableStateOf(initialMedicine?.location ?: "") }
    var expiryDaysAhead by remember { mutableStateOf("") } // يترك فارغاً افتراضياً

    val systemCategories = remember {
        listOf(
            "أدوية عامة",
            "مسكنات وخافض حرارة",
            "مضادات حيوية",
            "أدوية المعدة والجهاز الهضمي",
            "مكملات غذائية وفيتامينات",
            "حساسية ومضادات الهيستامين",
            "أدوية القلب والضغط",
            "أدوية السكري",
            "أدوية العيون والأنف والأذن",
            "أدوية جلدية وتجميلية"
        )
    }
    var categoryExpanded by remember { mutableStateOf(false) }

    val isEditing = initialMedicine != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "تعديل بيانات الدواء" else "إضافة دواء جديد للمخزون",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // اسم الدواء
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الدواء") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medicine_name_input")
                )

                // الباركود مع زر المسح بالكاميرا
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("رقم الباركود") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("medicine_barcode_input")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onScanBarcodeRequested,
                        modifier = Modifier.testTag("scan_barcode_for_add_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "مسح بالكاميرا",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // الأسعار (شراء وبيع)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = buyPriceText,
                        onValueChange = { buyPriceText = it },
                        label = { Text("سعر الشراء (ل.س)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellPriceText,
                        onValueChange = { sellPriceText = it },
                        label = { Text("سعر البيع (ل.س)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // الكميات (المخزون الحالي وتنبيه النواقص)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("الكمية الحالية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockText,
                        onValueChange = { minStockText = it },
                        label = { Text("حد التنبيه (الافتراضي: $defaultMinStockAlert)") },
                        placeholder = { Text("$defaultMinStockAlert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // التصنيف كقائمة منسدلة
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("التصنيف") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("medicine_category_dropdown")
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        systemCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cat,
                                        fontWeight = if (cat == category) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                // مكان التخزين
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("مكان التخزين") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // صلاحية الدواء
                OutlinedTextField(
                    value = expiryDaysAhead,
                    onValueChange = { expiryDaysAhead = it },
                    label = { Text("الصلاحية بعد كم يوم؟") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val buyPrice = if (initialMedicine != null && buyPriceText.isBlank()) {
                        initialMedicine.buyPrice
                    } else {
                        buyPriceText.toDoubleOrNull() ?: 0.0
                    }

                    val sellPrice = if (initialMedicine != null && sellPriceText.isBlank()) {
                        initialMedicine.sellPrice
                    } else {
                        val entered = sellPriceText.toDoubleOrNull() ?: 0.0
                        if (entered <= 0.0 && buyPrice > 0.0) buyPrice else entered
                    }

                    val quantity = if (initialMedicine != null && quantityText.isBlank()) {
                        initialMedicine.quantity
                    } else {
                        quantityText.toIntOrNull() ?: 0
                    }

                    // إذا لم يدخل المستخدم حداً أدنى يدوياً، يتم اعتماد القيمة الافتراضية المحددة بشريط التمرير في الإعدادات
                    val minStock = if (initialMedicine != null && minStockText.isBlank()) {
                        initialMedicine.minStockAlert
                    } else if (minStockText.isBlank()) {
                        defaultMinStockAlert
                    } else {
                        minStockText.toIntOrNull() ?: defaultMinStockAlert
                    }

                    val days = expiryDaysAhead.toLongOrNull()
                    val expiryTimestamp = if (days != null && days > 0L) {
                        System.currentTimeMillis() + (days * 24L * 60 * 60 * 1000)
                    } else if (initialMedicine != null && expiryDaysAhead.isBlank()) {
                        initialMedicine.expiryDate
                    } else {
                        0L
                    }

                    val newOrUpdated = if (initialMedicine != null) {
                        initialMedicine.copy(
                            name = name.trim(),
                            barcode = barcode.trim(),
                            buyPrice = buyPrice,
                            sellPrice = sellPrice,
                            quantity = quantity,
                            minStockAlert = minStock,
                            category = category.trim().ifBlank { initialMedicine.category.ifBlank { "أدوية عامة" } },
                            location = if (location.isBlank()) initialMedicine.location else location.trim(),
                            expiryDate = expiryTimestamp
                        )
                    } else {
                        MedicineEntity(
                            name = name.trim(),
                            barcode = barcode.trim(),
                            buyPrice = buyPrice,
                            sellPrice = sellPrice,
                            quantity = quantity,
                            minStockAlert = minStock,
                            category = category.trim().ifBlank { "أدوية عامة" },
                            location = location.trim(),
                            expiryDate = expiryTimestamp
                        )
                    }
                    onSave(newOrUpdated)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_medicine_button")
            ) {
                Text(if (isEditing) "حفظ التعديل" else "إضافة الدواء")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * نافذة عرض تفاصيل كل دفعة للدواء (الكمية وسعرها)
 */
@Composable
fun MedicineBatchDetailsDialog(
    medicine: MedicineEntity,
    onDismiss: () -> Unit
) {
    val batches = remember(medicine.batchesJson, medicine.quantity) { medicine.getBatches() }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "تفاصيل دفعات الدواء",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = medicine.name,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ملخص إجمالي المخزون
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("إجمالي الكمية بالمخزون", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${medicine.quantity} علبة", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "عدد الدفعات: ${batches.size}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (batches.isEmpty()) {
                    Text(
                        text = "لا توجد دفعات مخزون مسجلة حالياً.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    batches.forEach { batch ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "دفعة ${batch.batchNumber}",
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = "${batch.quantity} علبة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "سعر البيع: ${batch.sellPrice} ل.س",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    if (batch.buyPrice > 0.0) {
                                        Text(
                                            text = "سعر الشراء: ${batch.buyPrice} ل.س",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (batch.dateAdded > 0L) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "تاريخ الإضافة: ${dateFormat.format(Date(batch.dateAdded))}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * نافذة عرض تفاصيل الفاتورة وقائمة الأدوية المباعة فيها
 * (الاسم، الكمية، السعر)
 */
@Composable
fun InvoiceDetailsDialog(
    invoice: InvoiceSummary,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "تفاصيل الفاتورة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${invoice.invoiceId} • ${dateFormat.format(Date(invoice.timestamp))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ملخص مالي للفاتورة
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "إجمالي الفاتورة",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", invoice.totalAmount)} ل.س",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "الأدوية: ${invoice.itemsCount} أصناف",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "إجمالي القطع: ${invoice.totalUnitsSold} قطعة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = "الأدوية المباعة في الفاتورة:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                invoice.items.forEach { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.medicineName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "الكمية: ${item.quantitySold} علبة × سعر البيع: ${item.unitSellPrice} ل.س",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.2f", item.totalSellPrice)} ل.س",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

/**
 * نافذة إعدادات الصيدلية وتخصيص التنبيهات الافتراضية
 * تتيح تحديد القيمة الافتراضية للحد الأدنى للتنبيه بنفاذ الكمية (شريط تمرير)
 * وتنبيه اقتراب انتهاء الصلاحية (شريط تمرير)
 */
@Composable
fun SettingsDialog(
    currentExpiryDays: Int,
    currentDefaultMinStock: Int,
    viewModel: MainViewModel? = null,
    onSaveSettings: (expiryDays: Int, defaultMinStock: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var expiryDaysSlider by remember { mutableFloatStateOf(currentExpiryDays.coerceIn(5, 180).toFloat()) }
    var minStockSlider by remember { mutableFloatStateOf(currentDefaultMinStock.coerceIn(1, 50).toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "إعدادات التنبيهات الافتراضية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. شريط تمرير لتحديد الحد الأدنى الافتراضي للتنبيه بنفاذ الكمية
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الحد الأدنى الافتراضي لنقص الكمية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${minStockSlider.roundToInt()} علب",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "القيمة الافتراضية للحد الأدنى لتنبيه نفاذ الكمية عند إضافة دواء جديد إذا لم تدخلها يدوياً:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = minStockSlider,
                            onValueChange = { minStockSlider = it },
                            valueRange = 1f..50f,
                            steps = 48,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("min_stock_alert_slider")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1 علبة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("25 علبة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("50 علبة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 2. شريط تمرير لتحديد مهلة تنبيه اقتراب انتهاء الصلاحية
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تنبيه قبل انتهاء الصلاحية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${expiryDaysSlider.roundToInt()} يوماً",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "عدد الأيام المتبقية على انتهاء الصلاحية ليظهر تنبيه 'اقتراب انتهاء الصلاحية':",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = expiryDaysSlider,
                            onValueChange = { expiryDaysSlider = it },
                            valueRange = 5f..180f,
                            steps = 34,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expiry_alert_slider")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("5 أيام", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("90 يوماً", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("180 يوماً", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSettings(expiryDaysSlider.roundToInt(), minStockSlider.roundToInt())
                    onDismiss()
                }
            ) {
                Text("حفظ الإعدادات")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * نافذة الملف الشخصي ومعلومات الحساب وتسجيل الخروج
 */
@Composable
fun UserProfileDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val session by viewModel.authManager.currentSession.collectAsStateWithLifecycle()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "الملف الشخصي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "بيانات حساب الصيدلية",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // بطاقة بيانات المستخدم
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اسم المستخدم:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = session?.name?.ifBlank { "صيدلية" } ?: "صيدلية",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "البريد الإلكتروني:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = session?.email ?: "غير محدد",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "معرّف الحساب (ID):",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val rawId = session?.userId ?: ""
                            val numericId = if (rawId.isNotBlank()) {
                                val digits = rawId.filter { it.isDigit() }
                                if (digits.length >= 6) {
                                    digits.take(8)
                                } else {
                                    val hash = Math.abs(rawId.hashCode().toLong())
                                    String.format("%08d", hash % 100000000)
                                }
                            } else "---"
                            Text(
                                text = numericId,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "حالة الحساب:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "نشط ومفعل",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // زر تأكيد تسجيل الخروج
                if (showLogoutConfirm) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "هل أنت متأكد من رغبتك في تسجيل الخروج من هذا الحساب؟",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        viewModel.signOut()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("نعم، خروج", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { showLogoutConfirm = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إلغاء", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { showLogoutConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل الخروج من الحساب",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

