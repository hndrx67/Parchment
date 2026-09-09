package org.hndrx.parchment.data

import android.content.Context
import org.hndrx.parchment.pdf.PdfStorage
import org.hndrx.parchment.settings.SettingsRepository

class AppContainer(context: Context) {
    private val database = ParchmentDatabase.create(context)
    val repository = LibraryRepository(database.books(), PdfStorage(context))
    val collections = database.collections()
    val settings = SettingsRepository(context.applicationContext)
    val profiles = database.profiles()
    val recovery = RecoveryRepository(context.applicationContext, database, settings)
    val storage = PdfStorage(context.applicationContext)
}
