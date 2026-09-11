package com.pharmacy.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
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
import com.pharmacy.app.data.MedicineEntity
import com.pharmacy.app.data.SaleRecordEntity
import java.text.SimpleDateFormat
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

    val snackbarHostState = remember { SnackbarHostState() }
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val pendingBarcode by viewModel.pendingBarcodeForAdd.collectAsStateWithLifecycle()

    val totalAlerts by viewModel.totalAlertsCount.collectAsStateWithLifecycle()

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    onRestock = { med ->
                        viewModel.adjustStock(med.id, 10)
                    }
                )
                AppTab.REPORTS -> ReportsScreen(viewModel = viewModel)
            }
        }
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
    val cartProfit by viewModel.cartProfit.collectAsStateWithLifecycle()
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
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "الربح المتوقع: +${String.format(Locale.US, "%.2f", cartProfit)} ل.س",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = StatusSuccessGreen
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
                                    text = "${item.medicine.sellPrice} ل.س للقطعة | الباركود: ${item.medicine.barcode}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "الإجمالي: ${String.format(Locale.US, "%.2f", item.subtotal)} ل.س",
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStockAdjust: (Int) -> Unit,
    onAddToCart: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isLowStock = medicine.quantity <= medicine.minStockAlert
    val isExpired = medicine.expiryDate < System.currentTimeMillis()
    val isExpiringSoon = !isExpired && medicine.expiryDate <= (System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000))

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.US) }
    val formattedExpiry = remember(medicine.expiryDate) { dateFormat.format(Date(medicine.expiryDate)) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
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
                    Text(
                        text = "${medicine.category} • المكان: ${medicine.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // شارات التنبيه
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
            text = { Text("هل أنت متأكد من رغبتك في حذف '${medicine.name}' نهائياً من قاعدة بيانات الصيدلية؟") },
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
}

/**
 * 3. شاشة التنبيهات الذكية (Alerts Screen)
 */
@Composable
fun AlertsScreen(
    viewModel: MainViewModel,
    onRestock: (MedicineEntity) -> Unit
) {
    val lowStockMedicines by viewModel.lowStockMedicines.collectAsStateWithLifecycle()
    val expiredMedicines by viewModel.expiredMedicines.collectAsStateWithLifecycle()
    val expiringSoonMedicines by viewModel.expiringSoonMedicines.collectAsStateWithLifecycle()

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
                                actionLabel = "+ توريد 10 عبوات",
                                onAction = { onRestock(med) }
                            )
                        }
                    }
                }
            }
            2 -> {
                // قريبة الانتهاء
                if (expiringSoonMedicines.isEmpty()) {
                    EmptyAlertPlaceholder("لا توجد أدوية تنتهي صلاحيتها خلال الـ 90 يوماً القادمة.")
                } else {
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(expiringSoonMedicines) { med ->
                            val daysLeft = ((med.expiryDate - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)).coerceAtLeast(0)
                            AlertItemCard(
                                title = med.name,
                                subtitle = "تاريخ الانتهاء: ${dateFormat.format(Date(med.expiryDate))} (متبقي $daysLeft يوم)",
                                alertText = "الكمية: ${med.quantity} عبوة - يفضل تصريفها أولاً",
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
 * 4. شاشة التقارير والأرباح (Financial Reports Screen)
 */
@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val totalRevenue by viewModel.totalRevenue.collectAsStateWithLifecycle()
    val totalProfit by viewModel.totalProfit.collectAsStateWithLifecycle()
    val totalItemsSold by viewModel.totalItemsSold.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()

    val profitMargin = if (totalRevenue > 0) (totalProfit / totalRevenue) * 100 else 0.0
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "التقرير المالي والأرباح",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "حسابات دقيقة للمبيعات والأرباح بناءً على تكلفة الشراء وسعر البيع",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // شبكة البطاقات المالية
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReportMetricCard(
                title = "إجمالي المبيعات",
                value = "${String.format(Locale.US, "%.2f", totalRevenue)} ل.س",
                icon = Icons.Default.MonetizationOn,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
                title = "صافي الأرباح",
                value = "${String.format(Locale.US, "%.2f", totalProfit)} ل.س",
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
                title = "عدد القطع المباعة",
                value = "$totalItemsSold قطعة",
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
            text = "سجل المبيعات الأخيرة (${allSales.size} عملية):",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (allSales.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("لا توجد مبيعات مسجلة حتى الآن", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allSales.take(30).forEach { sale ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(sale.medicineName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = "فاتورة: ${sale.invoiceId} • ${dateFormat.format(Date(sale.timestamp))}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "الكمية: ${sale.quantitySold} × ${sale.unitSellPrice} ل.س",
                                    fontSize = 12.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", sale.totalSellPrice)} ل.س",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "ربح: +${String.format(Locale.US, "%.2f", sale.totalProfit)} ل.س",
                                    fontSize = 12.sp,
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
@Composable
fun AddEditMedicineDialog(
    initialMedicine: MedicineEntity?,
    presetBarcode: String,
    onSave: (MedicineEntity) -> Unit,
    onDismiss: () -> Unit,
    onScanBarcodeRequested: () -> Unit
) {
    var name by remember { mutableStateOf(initialMedicine?.name ?: "") }
    var barcode by remember { mutableStateOf(if (presetBarcode.isNotBlank()) presetBarcode else (initialMedicine?.barcode ?: "")) }
    var buyPriceText by remember { mutableStateOf(initialMedicine?.buyPrice?.toString() ?: "") }
    var sellPriceText by remember { mutableStateOf(initialMedicine?.sellPrice?.toString() ?: "") }
    var quantityText by remember { mutableStateOf(initialMedicine?.quantity?.toString() ?: "10") }
    var minStockText by remember { mutableStateOf(initialMedicine?.minStockAlert?.toString() ?: "5") }
    var category by remember { mutableStateOf(initialMedicine?.category ?: "أدوية عامة") }
    var location by remember { mutableStateOf(initialMedicine?.location ?: "رف A-1") }
    var expiryDaysAhead by remember { mutableStateOf("365") } // أيام متبقية للصلاحية

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
                    label = { Text("اسم الدواء (مثال: Panadol 500mg)") },
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
                        label = { Text("حد التنبيه") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // التصنيف ومكان الرف
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("التصنيف (مثال: مسكنات، مضادات...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("مكان التخزين (مثال: رف B-2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // صلاحية الدواء
                OutlinedTextField(
                    value = expiryDaysAhead,
                    onValueChange = { expiryDaysAhead = it },
                    label = { Text("الصلاحية بعد كم يوم؟ (مثال: 365 = سنة)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val buyPrice = buyPriceText.toDoubleOrNull() ?: 10.0
                    val sellPrice = sellPriceText.toDoubleOrNull() ?: 15.0
                    val quantity = quantityText.toIntOrNull() ?: 1
                    val minStock = minStockText.toIntOrNull() ?: 5
                    val days = expiryDaysAhead.toLongOrNull() ?: 365L
                    val expiryTimestamp = System.currentTimeMillis() + (days * 24L * 60 * 60 * 1000)

                    val newOrUpdated = if (initialMedicine != null) {
                        initialMedicine.copy(
                            name = name.ifBlank { "دواء بدون اسم" },
                            barcode = barcode.ifBlank { System.currentTimeMillis().toString() },
                            buyPrice = buyPrice,
                            sellPrice = sellPrice,
                            quantity = quantity,
                            minStockAlert = minStock,
                            category = category,
                            location = location,
                            expiryDate = expiryTimestamp
                        )
                    } else {
                        MedicineEntity(
                            name = name.ifBlank { "دواء بدون اسم" },
                            barcode = barcode.ifBlank { System.currentTimeMillis().toString() },
                            buyPrice = buyPrice,
                            sellPrice = sellPrice,
                            quantity = quantity,
                            minStockAlert = minStock,
                            category = category,
                            location = location,
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
