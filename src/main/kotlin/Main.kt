// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    val devices = remember { mutableStateListOf<Device>() }

    LaunchedEffect(Unit) {
        StartAdbInteractor().execute()

        val adb = AndroidDebugBridgeClientFactory().build()

        devices.addAll(adb.execute(ListDevicesRequest()))

        val deviceEventsChannel: ReceiveChannel<List<Device>> = adb.execute(
            request = AsyncDeviceMonitorRequest(),
            scope = this
        )

        for (currentDeviceList in deviceEventsChannel) {
            devices.clear()
            devices.addAll(currentDeviceList)
        }
    }

    val scaffoldState = rememberScaffoldState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("deks") }
            )
        },
        scaffoldState = scaffoldState
    ) { _ ->

        Column {
            var currentTabIndex by remember { mutableStateOf(0) }
            val tabs = listOf("General", "File Transfer", "Shell", "Device", "Settings", "About")

            TabRow(
                selectedTabIndex = currentTabIndex,
                modifier = Modifier.height(48.dp)
            ) {
                tabs.forEachIndexed { i, tab ->
                    Tab(selected = currentTabIndex == i, onClick = { currentTabIndex = i }) {
                        Text(tab)
                    }
                }
            }

            when (currentTabIndex) {
                0 -> {
                    if (devices.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Close,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp).background(Color.Red, RoundedCornerShape(50))
                                )
                                Text("No devices connected! Plug-in your device and try again.")
                            }
                        }
                    } else {
                        Text("Connected devices:")
                        LazyColumn {
                            items(devices) { device ->
                                Text("${device.serial} (${device.state})")
                            }
                        }
                    }
                }
                1 -> {
                    Text("File Transfer")
                }
                2 -> {
                    var shellCmd by remember { mutableStateOf("") }
                    var shellStdout by remember { mutableStateOf("") }
                    var shellExitCode by remember { mutableStateOf(0) }

                    val coroutineScope = rememberCoroutineScope()

                    TextField(shellCmd, { shellCmd = it }, textStyle = TextStyle(fontFamily = FontFamily.Monospace))

                    Button(onClick = {
                        coroutineScope.launch {
                            val adb = AndroidDebugBridgeClientFactory().build()

                            val shellResult = adb.execute(
                                request = ShellCommandRequest(shellCmd),
                                serial = devices[0].serial
                            )

                            shellStdout = shellResult.output
                            shellExitCode = shellResult.exitCode
                        }
                    }) {
                        Text("Run")
                    }

                    Text("# output will appear here ($shellExitCode)")
                    Text(shellStdout, fontFamily = FontFamily.Monospace)
                }
                3 -> {
                    Text("Device")
                }
                4 -> {
                    Text("Settings")
                }
                5 -> {
                    Text("Created by Pavitra Golchha (@pavi2410)")

                    Text("Source: https://github.com/pavi2410/deks")
                }
            }
        }
    }
}

fun main() = application {
    Window(title = "deks", onCloseRequest = ::exitApplication) {
        MaterialTheme {
            App()
        }
    }
}
