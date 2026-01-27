package com.example.fencing_project.di

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.example.fencing_project.data.repository.AuthRepository
import com.example.fencing_project.data.repository.BoutRepository
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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

    @Provides
    @Singleton
    fun provideBoutRepository(db: FirebaseFirestore): BoutRepository {
        return BoutRepository(db)
    }
}
