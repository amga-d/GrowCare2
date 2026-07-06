package com.example.growCare.data.mapper

import com.example.growCare.data.local.database.entity.UserEntity
import com.example.growCare.domain.model.User
import javax.inject.Inject

/**
 * Mapper to convert between User domain model and UserEntity
 */
class UserMapper @Inject constructor() {
    
    /**
     * Convert domain model to entity
     */
    fun toEntity(user: User): UserEntity {
        return UserEntity(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName,
            profilePictureUrl = user.profilePictureUrl,
            phoneNumber = user.phoneNumber,
            location = user.location,
            farmSize = user.farmSize,
            preferredCrops = user.preferredCrops,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    /**
     * Convert entity to domain model
     */
    fun toDomain(entity: UserEntity): User {
        return User(
            uid = entity.uid,
            email = entity.email,
            displayName = entity.displayName,
            profilePictureUrl = entity.profilePictureUrl,
            phoneNumber = entity.phoneNumber,
            location = entity.location,
            farmSize = entity.farmSize,
            preferredCrops = entity.preferredCrops,
            createdAt = entity.createdAt,
            lastLoginAt = entity.lastLoginAt
        )
    }
    
    /**
     * Convert list of entities to domain models
     */
    fun toDomainList(entities: List<UserEntity>): List<User> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Convert list of domain models to entities
     */
    fun toEntityList(users: List<User>): List<UserEntity> {
        return users.map { toEntity(it) }
    }
}
