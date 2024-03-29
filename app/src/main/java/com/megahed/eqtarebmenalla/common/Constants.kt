package com.megahed.eqtarebmenalla.common

import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import java.text.SimpleDateFormat
import java.util.*

object Constants {
    const val PRAYER_TIME_BASE_URL =  "https://api.aladhan.com/"
    const val QURAN_LISTEN_BASE_URL = "https://www.mp3quran.net/"
    const val QURAN_BY_EYA_BASE_URL = "https://api.alquran.cloud/"
    const val TAFSIR = "http://api.quran-tafseer.com/tafseer/"
    val SORA_OF_QURAN= listOf("",
        "سورة الفاتحة","سورة البقرة","سورة آل عمران","سورة النساء","سورة المائدة","سورة الأنعام","سورة الأعراف","سورة الأنفال","سورة التوبة","سورة يونس","سورة هود","سورة يوسف","سورة الرعد","سورة ابراهيم","سورة الحجر","سورة النحل","سورة الإسراء","سورة الكهف","سورة مريم","سورة طه","سورة الأنبياء","سورة الحج","سورة المؤمنون","سورة النور","سورة الفرقان","سورة الشعراء","سورة النمل","سورة القصص","سورة العنكبوت","سورة الروم","سورة لقمان","سورة السجدة","سورة الأحزاب","سورة سبإ","سورة فاطر","سورة يس","سورة الصافات","سورة ص","سورة الزمر","سورة غافر","سورة فصلت","سورة الشورى","سورة الزخرف","سورة الدخان","سورة الجاثية","سورة الأحقاف","سورة محمد","سورة الفتح","سورة الحجرات","سورة ق","سورة الذاريات","سورة الطور","سورة النجم","سورة القمر","سورة الرحمن","سورة الواقعة","سورة الحديد","سورة المجادلة","سورة الحشر","سورة الممتحنة","سورة الصف","سورة الجمعة","سورة المنافقون","سورة التغابن","سورة الطلاق","سورة التحريم","سورة الملك","سورة القلم","سورة الحاقة","سورة المعارج","سورة نوح","سورة الجن","سورة المزمل","سورة المدثر","سورة القيامة","سورة الانسان","سورة المرسلات","سورة النبإ","سورة النازعات","سورة عبس","سورة التكوير","سورة الإنفطار","سورة المطففين","سورة الإنشقاق","سورة البروج","سورة الطارق","سورة الأعلى","سورة الغاشية","سورة الفجر","سورة البلد","سورة الشمس","سورة الليل","سورة الضحى","سورة الشرح","سورة التين","سورة العلق","سورة القدر","سورة البينة","سورة الزلزلة","سورة العاديات","سورة القارعة","سورة التكاثر","سورة العصر","سورة الهمزة","سورة الفيل","سورة قريش","سورة الماعون","سورة الكوثر","سورة الكافرون","سورة النصر","سورة المسد","سورة الإخلاص","سورة الفلق","سورة الناس"
    )

    var maw3idha = arrayListOf<String>(
        "قَالَ -ص- : مَنْ صَلَّى أَرْبَعًا قَبْلَ الظُّهْرِ وَأَرْبَعًا بَعْدَهَا لَمْ تَمَسَّهُ النَّارُ  .",
        "قال الله تعالى : ( وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ وَإِنَّهَا لَكَبِيرَةٌ إِلَّا عَلَى الْخَاشِعِينَ ) البقرة /45.",
        "عن رسول الله صلى الله عليه وسلم قال: من صلى العشاء في جماعة، فكأنما قام نصف الليل، ومن صلى الصبح في جماعة فكأنما صلى الليل كله.",
    " قال الله تعالى: قَدْ أَفْلَحَ الْمُؤْمِنُونَ*الَّذِينَ هُمْ فِي صَلاتِهِمْ خَاشِعُونَ {المؤمنون:2}.",
        " قال الله تعالى: فَأَقِيمُوا الصَّلَاةَ إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا {النساء:103}.",
    " كان رسول الله صلى الله عليه وسلم إذا انصرف من صلاته استغفر ثلاثاً وقال: \" اللهم أنت السلام ومنك السلام ، تباركت ياذا الجلال والإكرام \".",
    "عن المغيرة بن شعبة رضي الله عنه أن النبي صلى الله عليه وسلم كان يقول في دبر كل صلاة مكتوبة : \"لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير. اللهم لا مانع لما أعطيت ، ولا معطي لما منعت ، ولا ينفع ذا الجد منك الجد\".",
    "قال -صلى الله عليه وسلم- : ما من عبد مسلم يصلي لله كل يوم اثنتي عشرة ركعة تطوعا غير فريضة، إلا بنى الله له بيتا في الجنة.",
    "عن عائشة -رضي الله عنها- أن النبي صلى الله عليه وسلم قال: رَكْعَتَا الْفَجْرِ، خَيْرٌ مِنْ الدُّنْيَا وَمَا فِيهَا. رواه مسلم.",
    "أخرج الترمذي في سننه  عن حريث بن قبيصة، قال: قدمت المدينة، فقلت: اللهم يسر لي جليسا صالحا، قال: فجلست إلى أبي هريرة، فقلت: إني سألت الله أن يرزقني جليسا صالحا، فحدثني بحديث سمعته من رسول الله صلى الله عليه وسلم، لعل الله أن ينفعني به. فقال: سمعت رسول الله صلى الله عليه وسلم يقول: إن أول ما يحاسب به العبد يوم القيامة من عمله صلاته؛ فإن صلحت فقد أفلح وأنجح. وإن فسدت؛ فقد خاب وخسر. فإن انتقص من فريضته شيء، قال الرب -عز وجل-: انظروا هل لعبدي من تطوع، فيكمل بها ما انتقص من الفريضة، ثم يكون سائر عمله على ذلك. وصححه الألباني.",
    "بي هريرة -رضي الله عنه-  أن النبي صلى الله عليه وسلم قال: قال الله -تعالى-: من عادى لي ولياً؛ فقد آذنته بالحرب، وما تقرب إليَّ عبدي بشيءٍ أحب إلي مما افترضته عليه. وما يزال عبدي يتقرب إليِّ بالنوافل حتى أحبه، فإذا أحببته كنت سمعه الذي يسمع به، وبصره الذي يبصر به، ويده التي يبطش بها، ورجله التي يمشي بها. ولئن سألني لأعطينه، ولئن استعاذني لأعيذنَّه. وما ترددتُ عن شيءٍّ أنا فاعله، ترددي عن نفس المؤمن يكره الموت، وأنا أكره مساءته. رواه البخاري.")

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


    val SORA_OF_QURAN_WITH_NB_EYA = mapOf<String, Int>(
        "الفَاتِحَة" to 7,
        "البَقَرَة" to 286,
        "آل عِمرَان" to 200,
        "النِّسَاء" to 176,
        "المَائدة" to 120,
        "الأنعَام" to 165,
        "الأعرَاف" to 206,
        "الأنفَال" to 75,
        "التوبَة" to 129,
        "يُونس" to 109,
        "هُود" to 123,
        "يُوسُف" to 111,
        "الرَّعْد" to 43,
        "إبراهِيم" to 52,
        "الحِجْر" to 99,
        "النَّحْل" to 128,
        "الإسْرَاء" to 111,
        "الكهْف" to 110,
        "مَريَم" to 98,
        "طه" to 135,
        "الأنبيَاء" to 112,
        "الحَج" to 78,
        "المُؤمنون" to 118,
        "النُّور" to 64,
        "الفُرْقان" to 77,
        "الشُّعَرَاء" to 227,
        "النَّمْل" to 93,
        "القَصَص" to 88,
        "العَنكبوت" to 69,
        "الرُّوم" to 60,
        "لقمَان" to 34,
        "السَّجدَة" to 30,
        "الأحزَاب" to 73,
        "سَبَأ" to 54,
        "فَاطِر" to 45,
        "يس" to 83,
        "الصَّافات" to 182,
        "ص" to 88,
        "الزُّمَر" to 75,
        "غَافِر" to 85,
        "فُصِّلَتْ" to 54,
        "الشُّورَى" to 53,
        "الزُّخْرُف" to 89,
        "الدخَان" to 59,
        "الجَاثيَة" to 37,
        "الأحْقاف" to 35,
        "محَمَّد" to 38,
        "الفَتْح" to 29,
        "الحُجرَات" to 18,
        "ق" to 45,
        "الذَّاريَات" to 60,
        "الطُّور" to 49,
        "النَّجْم" to 62,
        "القَمَر" to 55,
        "الرَّحمن" to 78,
        "الوَاقِعَة" to 96,
        "الحَديد" to 29,
        "المجَادلة" to 22,
        "الحَشر" to 24,
        "المُمتَحنَة" to 13,
        "الصَّف" to 14,
        "الجُمُعَة" to 11,
        "المنَافِقون" to 11,
        "التغَابُن" to 18,
        "الطلَاق" to 12,
        "التحْريم" to 12,
        "المُلْك" to 30,
        "القَلَم" to 52,
        "الحَاقَّة" to 52,
        "المعَارج" to 44,
        "نُوح" to 28,
        "الجِن" to 28,
        "المُزَّمِّل" to 20,
        "المُدَّثِّر" to 56,
        "القِيَامَة" to 40,
        "الإنسَان" to 31,
        "المُرسَلات" to 50,
        "النَّبَأ" to 40,
        "النّازعَات" to 46,
        "عَبَس" to 42,
        "التَّكوير" to 29,
        "الانفِطار" to 19,
        "المطفِّفِين" to 36,
        "الانْشِقَاق" to 25,
        "البرُوج" to 22,
        "الطَّارِق" to 17,
        "الأَعْلى" to 19,
        "الغَاشِية" to 26,
        "الفَجْر" to 30,
        "البَلَد" to 20,
        "الشَّمْس" to 15,
        "الليْل" to 21,
        "الضُّحَى" to 11,
        "الشَّرْح" to 8,
        "التِّين" to 8,
        "العَلَق" to 19,
        "القَدْر" to 5,
        "البَينَة" to 8,
        "الزلزَلة" to 8,
        "العَادِيات" to 11,
        "القَارِعة" to 11,
        "التَّكَاثر" to 8,
        "العَصْر" to 3,
        "الهُمَزَة" to 9,
        "الفِيل" to 5,
        "قُرَيْش" to 4,
        "المَاعُون" to 7,
        "الكَوْثَر" to 3,
        "الكَافِرُون" to 6,
        "النَّصر" to 3,
        "المَسَد" to 5,
        "الإخْلَاص" to 4,
        "الفَلَق" to 5,
        "النَّاس" to 6
    )



}