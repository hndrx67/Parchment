package org.hndrx.parchment

import android.app.Application
import org.hndrx.parchment.data.AppContainer

class ParchmentApplication : Application() {
    val container by lazy { AppContainer(this) }
}
