package com.example.fencing_project.di

import android.app.Application
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.data.repository.BoutRepository
import com.example.fencing_project.utils.SupabaseConfig
import com.example.fencing_project.utils.SupabaseStorageManager

import com.google.firebase.BuildConfig
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
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository {
        return AuthRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

//    @Provides
//    @Singleton
//    fun provideBoutRepository(db: FirebaseFirestore): BoutRepository {
//        return BoutRepository(db)
//    }
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
}
