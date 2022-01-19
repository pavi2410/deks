// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.ListDevicesRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.AndroidFile
import com.malinskiy.adam.request.sync.ListFilesRequest
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

@OptIn(ExperimentalTextApi::class)
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
                title = { Text("deks", fontWeight = FontWeight.Bold) },
                elevation = 0.dp,
                actions = {
                    Chip(
                        if (devices.isNotEmpty()) "CONNECTED" else "CONNECT DEVICE",
                        if (devices.isNotEmpty()) Color.Green else Color.Red
                    )
                }
            )
        },
        scaffoldState = scaffoldState
    ) { _ ->

        Column {
            var currentTabIndex by remember { mutableStateOf(0) }
            val tabs = listOf("General", "Files", "Shell", "Device", "Settings", "About")

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
            Column(Modifier.padding(16.dp)) {
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
                            Text("Connected devices:", style = MaterialTheme.typography.h6)
                            LazyColumn {
                                items(devices) { device ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.padding(8.dp).size(24.dp)
                                                .background(Color.Gray, RoundedCornerShape(50))
                                        )
                                        Text("${device.serial} (${device.state})")
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        val files = remember { mutableStateListOf<AndroidFile>() }

                        LaunchedEffect(Unit) {
                            if (devices.isEmpty()) {
                                scaffoldState.snackbarHostState.showSnackbar("No devices connected!")
                                return@LaunchedEffect
                            }

                            val adb = AndroidDebugBridgeClientFactory().build()

                            adb.execute(
                                request = ListFilesRequest(
                                    directory = "/sdcard/"
                                ),
                                serial = devices[0].serial
                            ).also {
                                files.clear()
                                files.addAll(it)
                            }
                        }

                        Text("Files")
                        LazyColumn {
                            items(files) { file ->
                                Text(file.name)
                            }
                        }
                    }
                    2 -> {
                        var shellCmd by remember { mutableStateOf("") }
                        var shellStdout by remember { mutableStateOf("") }
                        var shellExitCode by remember { mutableStateOf(0) }

                        val coroutineScope = rememberCoroutineScope()

                        TextField(shellCmd, { shellCmd = it }, textStyle = TextStyle(fontFamily = FontFamily.Monospace))

                        Button(onClick = {
                            coroutineScope.launch {
                                if (devices.isEmpty()) {
                                    scaffoldState.snackbarHostState.showSnackbar("No devices connected!")
                                    return@launch
                                }

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
                        val uriHandler = LocalUriHandler.current
                        val sourceUrl = "https://github.com/pavi2410/deks"

                        Text("Created by Pavitra Golchha (@pavi2410)")

                        ClickableText(buildAnnotatedString {
                            append("Source: ")
                            withAnnotation("source", sourceUrl) {
                                append(sourceUrl)
                            }
                        }) {
                            uriHandler.openUri(sourceUrl)
                        }
                    }
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

@Composable
fun Chip(text: String, color: Color) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(50)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            Box(Modifier.size(8.dp).background(color, RoundedCornerShape(50)))
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.caption)
        }
    }
}