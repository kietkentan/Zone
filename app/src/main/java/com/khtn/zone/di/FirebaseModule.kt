package com.khtn.zone.di

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.khtn.zone.utils.FirebaseStorageConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseDatabaseInstance(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    @Singleton
    @Provides
    fun provideUsersCollectionReference(firestore: FirebaseFirestore): CollectionReference {
        return firestore.collection("users")
    }

    /*@GroupCollection
    @Singleton
    @Provides
    fun provideGroupCollectionReference(firestore: FirebaseFirestore): CollectionReference {
        return firestore.collection("groups")
    }

    @MessageCollection
    @Singleton
    @Provides
    fun provideMessagesCollectionReference(firestore: FirebaseFirestore): CollectionReference {
        return firestore.collection("messages")
    }*/

    @Singleton
    @Provides
    fun provideFirebaseStorageInstance(): StorageReference {
        return FirebaseStorage.getInstance().getReference(FirebaseStorageConstants.ROOT_DIRECTORY)
    }
}