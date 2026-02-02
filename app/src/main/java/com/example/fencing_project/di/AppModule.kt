package com.example.fencing_project.di


import android.app.Application
import android.content.Context
import androidx.work.WorkerParameters
import com.example.fencing_project.data.local.BoutDao
import com.example.fencing_project.data.local.FencingDatabase
import com.example.fencing_project.data.local.LocalBoutRepository
import com.example.fencing_project.data.local.LocalStorageManager
import com.example.fencing_project.data.local.OpponentDao


import com.google.firebase.auth.FirebaseAuth
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.data.repository.SyncRepository
import com.example.fencing_project.utils.AvatarStorageManager
import com.example.fencing_project.utils.NetworkUtils


import com.example.fencing_project.utils.SupabaseConfig
import com.example.fencing_project.utils.SupabaseStorageManager
import com.example.fencing_project.work.AlarmSyncScheduler
import com.example.fencing_project.work.SyncServiceManager


import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(app: Application): FirebaseAuth {
        // Гарантируем, что Firebase инициализирован
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(app)
        }
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth,
                              @ApplicationContext context: Context): AuthRepository {
        return AuthRepository(firebaseAuth, context)
    }

    @Provides
    @Singleton
    fun provideBoutRepository(
        database: FirebaseDatabase,
        storageManager: SupabaseStorageManager,
    ): BoutRepository {
        return BoutRepository(database, storageManager)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }


    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
        // Если нужно указать URL базы данных:
        // return FirebaseDatabase.getInstance("https://your-project.firebaseio.com/")
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        println("DEBUG: Инициализация Supabase с URL: ${SupabaseConfig.SUPABASE_URL}")

        return try {
            createSupabaseClient(
                supabaseUrl = SupabaseConfig.SUPABASE_URL,
                supabaseKey = SupabaseConfig.SUPABASE_KEY
            ) {
                install(Postgrest)
                install(Storage)
            }
        } catch (e: Exception) {
            println("DEBUG: Ошибка создания Supabase клиента: ${e.message}")
            throw e
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseStorageManager(
        supabaseClient: SupabaseClient,
        @ApplicationContext context: Context
    ): SupabaseStorageManager {
        return SupabaseStorageManager(supabaseClient, context)
    }

    @Provides
    @Singleton
    fun provideFencingDatabase(@ApplicationContext context: Context): FencingDatabase {
        return FencingDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideBoutDao(database: FencingDatabase) = database.boutDao()

    @Provides
    @Singleton
    fun provideOpponentDao(database: FencingDatabase) = database.opponentDao()

    @Provides
    @Singleton
    fun provideLocalBoutRepository(
        boutDao: BoutDao,
        opponentDao: OpponentDao,
        avatarStorageManager: AvatarStorageManager
    ): LocalBoutRepository {
        return LocalBoutRepository(boutDao, opponentDao, avatarStorageManager)
    }

    @Provides
    @Singleton
    fun provideAvatarStorageManager(
        localStorageManager: LocalStorageManager
    ): AvatarStorageManager {
        // LocalStorageManager реализует AvatarStorageManager
        return localStorageManager
    }

    @Provides
    @Singleton
    fun provideLocalStorageManager(@ApplicationContext context: Context): LocalStorageManager {
        return LocalStorageManager(context)
    }

    @Provides
    @Singleton
    fun providerSyncServiceManager(@ApplicationContext context: Context): SyncServiceManager{
        return SyncServiceManager(context = context)
    }

//    @Provides
//    @Singleton
//    fun providerSyncScheduler(@ApplicationContext context: Context): SyncScheduler{
//        return SyncScheduler(context = context)
//    }

    @Provides
    @Singleton
    fun providerAlarmSyncScheduler(@ApplicationContext context: Context): AlarmSyncScheduler{
        return AlarmSyncScheduler(context = context)
    }

    @Provides
    @Singleton
    fun providerNetworkUtils(@ApplicationContext context: Context): NetworkUtils{
        return NetworkUtils(context = context)
    }







}
