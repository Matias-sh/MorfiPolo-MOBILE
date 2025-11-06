package com.cocido.morfipolo.data.local.database

import androidx.room.TypeConverter
import com.cocido.morfipolo.domain.model.MenuStatus

class Converters {
    @TypeConverter
    fun fromMenuStatus(status: MenuStatus): String {
        return status.name
    }

    @TypeConverter
    fun toMenuStatus(status: String): MenuStatus {
        return MenuStatus.valueOf(status)
    }
}


