package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.notification.OrderNotificationHelper
import com.example.ui.WarungViewModel
import com.example.ui.navigation.WarungRoutes
import com.example.ui.screens.OrderFormScreen
import com.example.ui.screens.OrdersInboxScreen
import com.example.ui.screens.PaymentScreen
import com.example.ui.theme.WarungNurbaitiTheme

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private var openOrdersTabSignal by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openOrdersTabSignal = intent?.getBooleanExtra(
            OrderNotificationHelper.EXTRA_NAVIGATE_TO_ORDERS,
            false
        ) == true

        setContent {
            WarungNurbaitiTheme {
                WarungNurbaitiApp(
                    shouldOpenOrders = openOrdersTabSignal,
                    onOrdersIntentHandled = { openOrdersTabSignal = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(OrderNotificationHelper.EXTRA_NAVIGATE_TO_ORDERS, false)) {
            openOrdersTabSignal = true
        }
    }
}

@Composable
fun WarungNurbaitiApp(
    shouldOpenOrders: Boolean,
    onOrdersIntentHandled: () -> Unit,
    viewModel: WarungViewModel = viewModel()
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val paymentConfig by viewModel.paymentConfig.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadOrderCount.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !OrderNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(shouldOpenOrders) {
        if (shouldOpenOrders) {
            navController.navigate(WarungRoutes.ROUTE_ORDERS) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onOrdersIntentHandled()
        }
    }

    val navItems = remember {
        listOf(
            NavDestinationItem(
                route = WarungRoutes.ROUTE_ORDER_FORM,
                labelRes = R.string.nav_order,
                selectedIcon = Icons.Filled.EditNote,
                unselectedIcon = Icons.Outlined.EditNote,
                testTag = "nav_tab_order"
            ),
            NavDestinationItem(
                route = WarungRoutes.ROUTE_PAYMENT,
                labelRes = R.string.nav_payment,
                selectedIcon = Icons.Filled.Payments,
                unselectedIcon = Icons.Outlined.Payments,
                testTag = "nav_tab_payment"
            ),
            NavDestinationItem(
                route = WarungRoutes.ROUTE_ORDERS,
                labelRes = R.string.nav_orders,
                selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
                unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
                testTag = "nav_tab_orders"
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: WarungRoutes.ROUTE_ORDER_FORM

    val navigateToRoute: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToRoute(item.route) },
                                icon = {
                                    NavTabIcon(
                                        item = item,
                                        selected = selected,
                                        unreadCount = unreadCount
                                    )
                                },
                                label = { Text(text = stringResource(id = item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToRoute(item.route) },
                                icon = {
                                    NavTabIcon(
                                        item = item,
                                        selected = selected,
                                        unreadCount = unreadCount
                                    )
                                },
                                label = { Text(text = stringResource(id = item.labelRes)) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = WarungRoutes.ROUTE_ORDER_FORM,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(WarungRoutes.ROUTE_ORDER_FORM) {
                        OrderFormScreen(
                            formState = formState,
                            paymentConfig = paymentConfig,
                            onCustomerNameChange = viewModel::onCustomerNameChanged,
                            onWhatsappChange = viewModel::onWhatsappChanged,
                            onIngredientsChange = viewModel::onIngredientsChanged,
                            onNotesChange = viewModel::onNotesChanged,
                            onPaymentMethodSelect = viewModel::onPaymentMethodSelected,
                            onSubmitOrder = viewModel::submitOrder,
                            onDismissConfirmation = viewModel::dismissOrderConfirmation,
                            onNavigateToPayment = { navigateToRoute(WarungRoutes.ROUTE_PAYMENT) }
                        )
                    }

                    composable(WarungRoutes.ROUTE_PAYMENT) {
                        BackHandler {
                            navigateToRoute(WarungRoutes.ROUTE_ORDER_FORM)
                        }
                        PaymentScreen(
                            paymentConfig = paymentConfig,
                            onUploadQris = viewModel::uploadQrisImage,
                            onRemoveQris = viewModel::removeQrisImage,
                            onSaveSettings = viewModel::savePaymentSettings
                        )
                    }

                    composable(WarungRoutes.ROUTE_ORDERS) {
                        BackHandler {
                            navigateToRoute(WarungRoutes.ROUTE_ORDER_FORM)
                        }
                        OrdersInboxScreen(
                            orders = orders,
                            unreadCount = unreadCount,
                            onUpdateStatus = viewModel::updateOrderStatus,
                            onMarkAllRead = viewModel::markAllOrdersRead,
                            onDeleteOrder = viewModel::deleteOrder,
                            onTestNotification = viewModel::triggerTestNotification
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavTabIcon(
    item: NavDestinationItem,
    selected: Boolean,
    unreadCount: Int
) {
    val iconVector = if (selected) item.selectedIcon else item.unselectedIcon
    if (item.route == WarungRoutes.ROUTE_ORDERS && unreadCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(text = if (unreadCount > 99) "99+" else unreadCount.toString())
                }
            }
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = stringResource(id = item.labelRes)
            )
        }
    } else {
        Icon(
            imageVector = iconVector,
            contentDescription = stringResource(id = item.labelRes)
        )
    }
}
