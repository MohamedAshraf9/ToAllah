package com.megahed.eqtarebmenalla.common

import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import java.text.SimpleDateFormat
import java.util.*

object Constants {
    const val PRAYER_TIME_BASE_URL = "https://api.aladhan.com/"
    const val QURAN_LISTEN_BASE_URL = "https://www.mp3quran.net/"
    val SORA_OF_QURAN= listOf("",
        "سورة الفاتحة","سورة البقرة","سورة آل عمران","سورة النساء","سورة المائدة","سورة الأنعام","سورة الأعراف","سورة الأنفال","سورة التوبة","سورة يونس","سورة هود","سورة يوسف","سورة الرعد","سورة ابراهيم","سورة الحجر","سورة النحل","سورة الإسراء","سورة الكهف","سورة مريم","سورة طه","سورة الأنبياء","سورة الحج","سورة المؤمنون","سورة النور","سورة الفرقان","سورة الشعراء","سورة النمل","سورة القصص","سورة العنكبوت","سورة الروم","سورة لقمان","سورة السجدة","سورة الأحزاب","سورة سبإ","سورة فاطر","سورة يس","سورة الصافات","سورة ص","سورة الزمر","سورة غافر","سورة فصلت","سورة الشورى","سورة الزخرف","سورة الدخان","سورة الجاثية","سورة الأحقاف","سورة محمد","سورة الفتح","سورة الحجرات","سورة ق","سورة الذاريات","سورة الطور","سورة النجم","سورة القمر","سورة الرحمن","سورة الواقعة","سورة الحديد","سورة المجادلة","سورة الحشر","سورة الممتحنة","سورة الصف","سورة الجمعة","سورة المنافقون","سورة التغابن","سورة الطلاق","سورة التحريم","سورة الملك","سورة القلم","سورة الحاقة","سورة المعارج","سورة نوح","سورة الجن","سورة المزمل","سورة المدثر","سورة القيامة","سورة الانسان","سورة المرسلات","سورة النبإ","سورة النازعات","سورة عبس","سورة التكوير","سورة الإنفطار","سورة المطففين","سورة الإنشقاق","سورة البروج","سورة الطارق","سورة الأعلى","سورة الغاشية","سورة الفجر","سورة البلد","سورة الشمس","سورة الليل","سورة الضحى","سورة الشرح","سورة التين","سورة العلق","سورة القدر","سورة البينة","سورة الزلزلة","سورة العاديات","سورة القارعة","سورة التكاثر","سورة العصر","سورة الهمزة","سورة الفيل","سورة قريش","سورة الماعون","سورة الكوثر","سورة الكافرون","سورة النصر","سورة المسد","سورة الإخلاص","سورة الفلق","سورة الناس"
    )
    val songs=mutableListOf<Song>()
    const val FIRST_PART_QURAN = "001000"
    const val LAST_PART_QURAN = 114006
    const val DIGIT_NUMBER_PART_QURAN = 6



    const val SONG_COLLECTION = "songs"

    const val MEDIA_ROOT_ID = "root_id"

    const val NETWORK_ERROR = "NETWORK_ERROR"

    const val UPDATE_PLAYER_POSITION_INTERVAL = 100L

    const val NOTIFICATION_CHANNEL_ID = "music"
    const val NOTIFICATION_ID = 1


    fun toSoraOfQuran(s:String): MutableList<String> {
        val arr=s.split(",")
        val ints=arr.map { it.toInt() }
        val list = mutableListOf<String>()
            ints.forEach {
                list.add(SORA_OF_QURAN[it])
        }

        return list
    }

    fun getSoraLink(link:String,number:Int): String {
        return if (number<=9)
            "$link/00$number.mp3"
        else if (number in 10..99)
            "$link/0$number.mp3"
        else "$link/$number.mp3"

    }


}