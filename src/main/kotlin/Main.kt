// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import com.malinskiy.adam.request.device.AsyncDeviceMonitorRequest
import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.ListDevicesRequest
import kotlinx.coroutines.channels.ReceiveChannel

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

    MaterialTheme {
        LazyColumn {
            items(devices) { device ->
                Text("${device.serial} (${device.state})")
            }
        }
    }
}

fun main() = application {
    Window(title = "deks", onCloseRequest = ::exitApplication) {
        App()
    }
}
