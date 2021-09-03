/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 21:07:04
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/MojangGameProfileDTO.kt
 */

package io.github.yggdrasilofficialproxy

import com.google.gson.Gson
import com.google.gson.TypeAdapter


data class MojangGameProfileDTO(
    @JvmField var id: String,
    @JvmField var name: String,
    @JvmField var properties: MutableList<PropertyDTO>? = null
) {
    data class PropertyDTO(
        @JvmField var name: String,
        @JvmField var value: String,
        @JvmField var signature: String? = null,
    )

    companion object {
        @JvmField
        val ADAPTER: TypeAdapter<MojangGameProfileDTO> = Gson().getAdapter(MojangGameProfileDTO::class.java)
    }
}