package fr.tchyze.vigie.watch

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import fr.tchyze.vigie.VigieApp
import fr.tchyze.vigie.data.DataError
import java.util.concurrent.TimeUnit

/** Actualisation périodique en arrière-plan, puis notification des nouveautés. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as VigieApp).container
        val result = container.repository.refresh()
        val snapshot = result.current
        if (result.error == null && snapshot != null) {
            container.watcher.check(snapshot, container.repository.state.value.demo)
        }
        // Réseau indisponible : WorkManager réessaiera plus tard. Autre erreur : on attend le prochain cycle.
        return if (result.error is DataError.Network) Result.retry() else Result.success()
    }

    companion object {
        private const val WORK_NAME = "actualisation"

        fun schedule(context: Context, hours: Int) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(hours.toLong(), TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
