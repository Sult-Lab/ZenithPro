package com.techsultan.zenithpro

import android.app.Application
import androidx.work.Configuration
import com.techsultan.zenithpro.core.di.commonModule
import com.techsultan.zenithpro.core.di.databaseModule
import com.techsultan.zenithpro.core.di.receiptModule
import com.techsultan.zenithpro.core.di.sessionModule
import com.techsultan.zenithpro.core.di.supabaseModule
import com.techsultan.zenithpro.features.analytics.di.reportsModule
import com.techsultan.zenithpro.features.auth.di.authModule
import com.techsultan.zenithpro.features.branch.di.branchModule
import com.techsultan.zenithpro.features.category.di.categoryModule
import com.techsultan.zenithpro.features.customer.di.customerModule
import com.techsultan.zenithpro.features.dashboard.di.dashboardModule
import com.techsultan.zenithpro.features.expenses.di.expenseModule
import com.techsultan.zenithpro.features.material.di.materialModule
import com.techsultan.zenithpro.features.inventory.di.productModule
import com.techsultan.zenithpro.features.production.di.productionModule
import com.techsultan.zenithpro.features.sales.di.salesModule
import com.techsultan.zenithpro.features.settings.di.businessModule
import com.techsultan.zenithpro.features.settings.di.paymentModule
import com.techsultan.zenithpro.features.payment.di.paymentProcessingModule
import com.techsultan.zenithpro.features.settings.di.settingsModule
import com.techsultan.zenithpro.core.util.ZenithAnalytics
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import com.techsultan.zenithpro.core.worker.SyncWorker

class ZenithApplication : Application(), KoinComponent, Configuration.Provider {

    companion object {
        lateinit var appContext: ZenithApplication
            private set
    }

    private val auth: Auth by inject()

    val applicationScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(getKoin().get<KoinWorkerFactory>())
            .build()

    override fun onCreate() {
        super.onCreate()
        appContext = this
        ZenithAnalytics.log("App cold start")

        startKoin {
            androidLogger()
            androidContext(this@ZenithApplication)
            workManagerFactory()
            modules(
                supabaseModule, 
                authModule, 
                commonModule, 
                productModule,
                databaseModule,
                salesModule,
                dashboardModule,
                customerModule,
                expenseModule,
                settingsModule,
                reportsModule,
                branchModule,
                sessionModule,
                productionModule,
                materialModule,
                categoryModule,
                businessModule,
                receiptModule,
                paymentModule,
                paymentProcessingModule
            )
        }

        applicationScope.launch {
            auth.awaitInitialization()
        }

        SyncWorker.schedule(this)
    }
}
