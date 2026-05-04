package com.trustedgelabs.trustguard.util

import java.util.Locale

/**
 * Provides user-friendly, localized permission descriptions with risk explanations.
 * Instead of technical names, tells users what each permission means for their privacy.
 */
object PermissionDescriptions {

    data class PermissionDetail(
        val friendlyName: String,
        val description: String,
        val riskNote: String
    )

    private val descriptionsEN = mapOf(
        // Camera & Microphone
        "android.permission.CAMERA" to PermissionDetail(
            "Camera Access",
            "Can take photos and record video using your camera.",
            "Apps with this permission could potentially capture images or video without your knowledge when running in the background."
        ),
        "android.permission.RECORD_AUDIO" to PermissionDetail(
            "Microphone Access",
            "Can record audio through your microphone.",
            "This permission allows the app to listen to surrounding sounds. Be mindful of which apps you grant this to."
        ),

        // Location
        "android.permission.ACCESS_FINE_LOCATION" to PermissionDetail(
            "Precise Location",
            "Can access your exact GPS location.",
            "Your precise location can reveal your home address, workplace and daily routines. Only grant to apps that truly need it."
        ),
        "android.permission.ACCESS_COARSE_LOCATION" to PermissionDetail(
            "Approximate Location",
            "Can determine your approximate location.",
            "Provides your general area (within ~3km). Less sensitive than precise location but still reveals your whereabouts."
        ),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to PermissionDetail(
            "Background Location",
            "Can track your location even when the app is closed.",
            "The app can monitor your movements 24/7. Very few apps genuinely need this — consider whether it is necessary."
        ),

        // Contacts
        "android.permission.READ_CONTACTS" to PermissionDetail(
            "Read Contacts",
            "Can read your contact list.",
            "Your contacts include names, phone numbers and emails of the people you know. This data could be used for targeted advertising or shared with third parties."
        ),
        "android.permission.WRITE_CONTACTS" to PermissionDetail(
            "Modify Contacts",
            "Can add, edit or delete your contacts.",
            "The app can change your contact information. Rarely needed by most apps."
        ),
        "android.permission.GET_ACCOUNTS" to PermissionDetail(
            "Device Accounts",
            "Can see the accounts on your device.",
            "Reveals which services you use (Google, social media, etc.)."
        ),

        // Phone
        "android.permission.READ_PHONE_STATE" to PermissionDetail(
            "Phone Status",
            "Can read phone status and identity.",
            "Can access your phone number and device identifiers. Often used for tracking across apps."
        ),
        "android.permission.READ_PHONE_NUMBERS" to PermissionDetail(
            "Phone Number",
            "Can read your phone numbers.",
            "Directly accesses the phone numbers associated with your device."
        ),
        "android.permission.CALL_PHONE" to PermissionDetail(
            "Make Calls",
            "Can make phone calls without your interaction.",
            "The app can initiate calls without showing the dialer. This could potentially incur charges."
        ),
        "android.permission.READ_CALL_LOG" to PermissionDetail(
            "Call History",
            "Can read your call history.",
            "Reveals who you talk to, when and for how long. Highly personal information."
        ),
        "android.permission.WRITE_CALL_LOG" to PermissionDetail(
            "Modify Call History",
            "Can modify your call history.",
            "The app can edit or delete call records."
        ),
        "android.permission.ANSWER_PHONE_CALLS" to PermissionDetail(
            "Answer Calls",
            "Can answer incoming calls automatically.",
            "The app can pick up calls on your behalf."
        ),
        "android.permission.PROCESS_OUTGOING_CALLS" to PermissionDetail(
            "Monitor Outgoing Calls",
            "Can monitor and redirect outgoing calls.",
            "Can see which numbers you are calling."
        ),
        "android.permission.ADD_VOICEMAIL" to PermissionDetail(
            "Voicemail",
            "Can add voicemail messages.",
            "Allows modification of your voicemail."
        ),
        "android.permission.USE_SIP" to PermissionDetail(
            "SIP Calls",
            "Can make internet phone calls (SIP).",
            "Enables internet-based calling features."
        ),

        // SMS
        "android.permission.READ_SMS" to PermissionDetail(
            "Read Messages",
            "Can read your text messages.",
            "Your SMS messages may contain verification codes, personal conversations and sensitive information. This is one of the most privacy-sensitive permissions."
        ),
        "android.permission.SEND_SMS" to PermissionDetail(
            "Send Messages",
            "Can send text messages.",
            "Sending SMS may incur charges on your phone bill. Be cautious granting this permission."
        ),
        "android.permission.RECEIVE_SMS" to PermissionDetail(
            "Receive Messages",
            "Can receive and process incoming SMS messages.",
            "The app can intercept incoming messages, including verification codes."
        ),
        "android.permission.READ_MMS" to PermissionDetail(
            "Read MMS",
            "Can read your multimedia messages.",
            "Access to MMS messages including photos and videos sent to you."
        ),

        // Storage
        "android.permission.READ_EXTERNAL_STORAGE" to PermissionDetail(
            "Read Files",
            "Can read files stored on your device.",
            "Can access your photos, documents and downloads. On older Android versions, this grants broad file access."
        ),
        "android.permission.WRITE_EXTERNAL_STORAGE" to PermissionDetail(
            "Write Files",
            "Can create and modify files on your device.",
            "Can save, edit or delete files in shared storage."
        ),
        "android.permission.MANAGE_EXTERNAL_STORAGE" to PermissionDetail(
            "Full File Access",
            "Has access to all files on your device.",
            "This is the broadest storage permission — the app can read, modify and delete any file. Only grant to file managers and similar tools."
        ),
        "android.permission.READ_MEDIA_IMAGES" to PermissionDetail(
            "Photo Access",
            "Can access your photos.",
            "The app can view all photos stored on your device."
        ),
        "android.permission.READ_MEDIA_VIDEO" to PermissionDetail(
            "Video Access",
            "Can access your videos.",
            "The app can view all videos stored on your device."
        ),
        "android.permission.READ_MEDIA_AUDIO" to PermissionDetail(
            "Music & Audio",
            "Can access your music and audio files.",
            "The app can access your audio library."
        ),
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to PermissionDetail(
            "Selected Photos/Videos",
            "Can access photos and videos you specifically selected.",
            "A more privacy-friendly approach — only accesses files you choose."
        ),

        // Calendar
        "android.permission.READ_CALENDAR" to PermissionDetail(
            "Read Calendar",
            "Can read your calendar events.",
            "Your calendar reveals your schedule, meetings and plans."
        ),
        "android.permission.WRITE_CALENDAR" to PermissionDetail(
            "Modify Calendar",
            "Can create or modify calendar events.",
            "The app can add events or change existing ones in your calendar."
        ),

        // Body Sensors
        "android.permission.BODY_SENSORS" to PermissionDetail(
            "Body Sensors",
            "Can access health sensors (heart rate, etc.).",
            "Health data is extremely personal. Only grant to fitness and health apps."
        ),
        "android.permission.BODY_SENSORS_BACKGROUND" to PermissionDetail(
            "Background Health Monitoring",
            "Can access health sensors in the background.",
            "Continuous health monitoring even when the app is closed."
        ),
        "android.permission.ACTIVITY_RECOGNITION" to PermissionDetail(
            "Activity Detection",
            "Can detect your physical activity (walking, driving, etc.).",
            "Knows whether you are sitting, walking, running or in a vehicle."
        ),

        // Network
        "android.permission.INTERNET" to PermissionDetail(
            "Internet Access",
            "Can connect to the internet.",
            "Most apps need internet access. However, combined with other permissions, data could be sent to remote servers."
        ),
        "android.permission.ACCESS_NETWORK_STATE" to PermissionDetail(
            "Network Status",
            "Can check network connectivity.",
            "Can see if you are connected via Wi-Fi or mobile data. Low privacy impact."
        ),
        "android.permission.ACCESS_WIFI_STATE" to PermissionDetail(
            "Wi-Fi Info",
            "Can view Wi-Fi connection details.",
            "Can see your Wi-Fi network name, which could reveal your location."
        ),
        "android.permission.CHANGE_WIFI_STATE" to PermissionDetail(
            "Change Wi-Fi",
            "Can change Wi-Fi settings.",
            "Can enable/disable Wi-Fi and modify network configurations."
        ),
        "android.permission.CHANGE_NETWORK_STATE" to PermissionDetail(
            "Change Network",
            "Can change network settings.",
            "Can modify how your device connects to the internet."
        ),
        "android.permission.NEARBY_WIFI_DEVICES" to PermissionDetail(
            "Nearby Wi-Fi Devices",
            "Can discover nearby Wi-Fi devices.",
            "Can scan for surrounding Wi-Fi devices, which may be used for location tracking."
        ),

        // Bluetooth
        "android.permission.BLUETOOTH" to PermissionDetail(
            "Bluetooth",
            "Can use Bluetooth.",
            "Basic Bluetooth capability for connecting to accessories."
        ),
        "android.permission.BLUETOOTH_ADMIN" to PermissionDetail(
            "Bluetooth Settings",
            "Can manage Bluetooth settings.",
            "Can modify Bluetooth configuration on your device."
        ),
        "android.permission.BLUETOOTH_CONNECT" to PermissionDetail(
            "Bluetooth Connect",
            "Can connect to Bluetooth devices.",
            "Can pair and communicate with nearby Bluetooth devices."
        ),
        "android.permission.BLUETOOTH_SCAN" to PermissionDetail(
            "Bluetooth Scan",
            "Can scan for nearby Bluetooth devices.",
            "Scanning for Bluetooth devices can be used to determine your surroundings."
        ),
        "android.permission.BLUETOOTH_ADVERTISE" to PermissionDetail(
            "Bluetooth Advertise",
            "Can make your device visible to Bluetooth devices.",
            "Your device becomes discoverable to nearby Bluetooth devices."
        ),

        // System
        "android.permission.VIBRATE" to PermissionDetail(
            "Vibration",
            "Can make your device vibrate.",
            "Low privacy impact. Used for notifications and feedback."
        ),
        "android.permission.WAKE_LOCK" to PermissionDetail(
            "Prevent Sleep",
            "Can prevent your device from sleeping.",
            "May affect battery life by keeping the device awake."
        ),
        "android.permission.FOREGROUND_SERVICE" to PermissionDetail(
            "Background Service",
            "Can run services in the background.",
            "The app can perform tasks when you are not actively using it."
        ),
        "android.permission.POST_NOTIFICATIONS" to PermissionDetail(
            "Notifications",
            "Can send you notifications.",
            "You can manage notification preferences in system settings."
        ),
        "android.permission.RECEIVE_BOOT_COMPLETED" to PermissionDetail(
            "Start at Boot",
            "Can start automatically when your device boots.",
            "The app runs immediately after you turn on your device, even if you have not opened it."
        ),
        "android.permission.REQUEST_INSTALL_PACKAGES" to PermissionDetail(
            "Install Apps",
            "Can request to install other apps.",
            "Be cautious — this could be used to install unwanted software."
        ),
        "android.permission.REQUEST_DELETE_PACKAGES" to PermissionDetail(
            "Delete Apps",
            "Can request to remove other apps.",
            "Can prompt you to uninstall other applications."
        ),
        "android.permission.SYSTEM_ALERT_WINDOW" to PermissionDetail(
            "Draw Over Apps",
            "Can display content over other apps.",
            "Can show overlays on top of other apps. Sometimes used for legitimate features but can be misused."
        ),
        "android.permission.SCHEDULE_EXACT_ALARM" to PermissionDetail(
            "Exact Alarms",
            "Can set precise alarms.",
            "Allows the app to trigger actions at exact times."
        ),
        "android.permission.USE_EXACT_ALARM" to PermissionDetail(
            "Exact Alarms",
            "Can set precise alarms.",
            "Allows the app to trigger actions at exact times."
        ),
        "android.permission.USE_BIOMETRIC" to PermissionDetail(
            "Biometric Auth",
            "Can use fingerprint or face recognition.",
            "Used for secure authentication. The biometric data itself is not shared with the app."
        ),
        "android.permission.USE_FINGERPRINT" to PermissionDetail(
            "Fingerprint Auth",
            "Can use fingerprint authentication.",
            "Used for secure login. The fingerprint data itself is not shared."
        ),
        "android.permission.NFC" to PermissionDetail(
            "NFC",
            "Can use NFC hardware.",
            "Used for contactless payments and data transfer."
        ),
        "android.permission.PACKAGE_USAGE_STATS" to PermissionDetail(
            "Usage Statistics",
            "Can see which apps you use and how often.",
            "Reveals your app usage patterns and habits. Highly personal information."
        ),

        // Bind services
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to PermissionDetail(
            "Accessibility Service",
            "Can observe and interact with your screen content.",
            "One of the most powerful permissions — can read everything on screen and perform actions. Only grant to trusted accessibility tools."
        ),
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to PermissionDetail(
            "Read Notifications",
            "Can read all your notifications.",
            "Has access to every notification including message previews, emails and alerts."
        ),
        "android.permission.BIND_VPN_SERVICE" to PermissionDetail(
            "VPN Service",
            "Can create a VPN connection.",
            "All your internet traffic can pass through this app. Only use VPN services you trust."
        )
    )

    private val descriptionsTR = mapOf(
        // Kamera & Mikrofon
        "android.permission.CAMERA" to PermissionDetail(
            "Kamera Erişimi",
            "Kameranızı kullanarak fotoğraf ve video çekebilir.",
            "Bu izne sahip uygulamalar arka planda çalışırken potansiyel olarak bilginiz dışında görüntü kaydedebilir."
        ),
        "android.permission.RECORD_AUDIO" to PermissionDetail(
            "Mikrofon Erişimi",
            "Mikrofonunuzdan ses kaydı yapabilir.",
            "Bu izin uygulamanın çevredeki sesleri dinlemesine olanak tanır. Hangi uygulamalara bu izni verdiğinize dikkat edin."
        ),

        // Konum
        "android.permission.ACCESS_FINE_LOCATION" to PermissionDetail(
            "Hassas Konum",
            "Tam GPS konumunuza erişebilir.",
            "Hassas konumunuz ev adresinizi, işyerinizi ve günlük rutinlerinizi ortaya çıkarabilir. Yalnızca gerçekten ihtiyaç duyan uygulamalara verin."
        ),
        "android.permission.ACCESS_COARSE_LOCATION" to PermissionDetail(
            "Yaklaşık Konum",
            "Yaklaşık konumunuzu belirleyebilir.",
            "Genel bölgenizi (~3km dahilinde) sağlar. Hassas konumdan daha az riskli ama yine de nerede olduğunuzu gösterir."
        ),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to PermissionDetail(
            "Arka Plan Konumu",
            "Uygulama kapalıyken bile konumunuzu takip edebilir.",
            "Uygulama hareketlerinizi 7/24 izleyebilir. Çok az uygulama buna gerçekten ihtiyaç duyar — gerekli olup olmadığını düşünün."
        ),

        // Kişiler
        "android.permission.READ_CONTACTS" to PermissionDetail(
            "Kişileri Oku",
            "Rehberinizi okuyabilir.",
            "Rehberiniz tanıdığınız kişilerin adlarını, telefon numaralarını ve e-postalarını içerir. Bu veriler hedefli reklamcılık için kullanılabilir."
        ),
        "android.permission.WRITE_CONTACTS" to PermissionDetail(
            "Kişileri Düzenle",
            "Rehberinize kişi ekleyebilir veya düzenleyebilir.",
            "Uygulama rehber bilgilerinizi değiştirebilir. Çoğu uygulama için gerekli değildir."
        ),
        "android.permission.GET_ACCOUNTS" to PermissionDetail(
            "Cihaz Hesapları",
            "Cihazınızdaki hesapları görebilir.",
            "Hangi servisleri kullandığınızı (Google, sosyal medya vb.) ortaya çıkarır."
        ),

        // Telefon
        "android.permission.READ_PHONE_STATE" to PermissionDetail(
            "Telefon Durumu",
            "Telefon durumunu ve kimliğini okuyabilir.",
            "Telefon numaranıza ve cihaz tanımlayıcılarınıza erişebilir. Genellikle uygulamalar arası izleme için kullanılır."
        ),
        "android.permission.READ_PHONE_NUMBERS" to PermissionDetail(
            "Telefon Numarası",
            "Telefon numaralarınızı okuyabilir.",
            "Cihazınıza bağlı telefon numaralarına doğrudan erişir."
        ),
        "android.permission.CALL_PHONE" to PermissionDetail(
            "Arama Yap",
            "Siz müdahale etmeden telefon araması yapabilir.",
            "Uygulama arama ekranını göstermeden arama başlatabilir. Bu durum ücrete yol açabilir."
        ),
        "android.permission.READ_CALL_LOG" to PermissionDetail(
            "Arama Geçmişi",
            "Arama geçmişinizi okuyabilir.",
            "Kiminle, ne zaman ve ne kadar süre konuştuğunuzu gösterir. Son derece kişisel bir bilgidir."
        ),
        "android.permission.WRITE_CALL_LOG" to PermissionDetail(
            "Arama Geçmişini Düzenle",
            "Arama geçmişinizi değiştirebilir.",
            "Uygulama arama kayıtlarını düzenleyebilir veya silebilir."
        ),
        "android.permission.ANSWER_PHONE_CALLS" to PermissionDetail(
            "Aramaları Yanıtla",
            "Gelen aramaları otomatik yanıtlayabilir.",
            "Uygulama sizin adınıza aramaları cevaplayabilir."
        ),
        "android.permission.PROCESS_OUTGOING_CALLS" to PermissionDetail(
            "Giden Aramaları İzle",
            "Giden aramaları izleyebilir ve yönlendirebilir.",
            "Hangi numaraları aradığınızı görebilir."
        ),
        "android.permission.ADD_VOICEMAIL" to PermissionDetail(
            "Sesli Mesaj",
            "Sesli mesaj ekleyebilir.",
            "Sesli mesaj kutunuzda değişiklik yapabilir."
        ),
        "android.permission.USE_SIP" to PermissionDetail(
            "SIP Araması",
            "İnternet üzerinden telefon araması (SIP) yapabilir.",
            "İnternet tabanlı arama özelliklerini etkinleştirir."
        ),

        // SMS
        "android.permission.READ_SMS" to PermissionDetail(
            "Mesajları Oku",
            "SMS mesajlarınızı okuyabilir.",
            "Mesajlarınızda doğrulama kodları, kişisel konuşmalar ve hassas bilgiler bulunabilir. Gizlilik açısından en hassas izinlerden biridir."
        ),
        "android.permission.SEND_SMS" to PermissionDetail(
            "Mesaj Gönder",
            "SMS mesajı gönderebilir.",
            "SMS göndermek faturanıza ücret yansıtabilir. Bu izni verirken dikkatli olun."
        ),
        "android.permission.RECEIVE_SMS" to PermissionDetail(
            "Mesaj Al",
            "Gelen SMS mesajlarını alabilir ve işleyebilir.",
            "Uygulama doğrulama kodları dahil gelen mesajları yakalayabilir."
        ),
        "android.permission.READ_MMS" to PermissionDetail(
            "MMS Oku",
            "MMS mesajlarınızı okuyabilir.",
            "Size gönderilen fotoğraf ve videolar dahil multimedya mesajlarına erişebilir."
        ),

        // Depolama
        "android.permission.READ_EXTERNAL_STORAGE" to PermissionDetail(
            "Dosya Okuma",
            "Cihazınızdaki dosyaları okuyabilir.",
            "Fotoğraflarınıza, belgelerinize ve indirmelerinize erişebilir."
        ),
        "android.permission.WRITE_EXTERNAL_STORAGE" to PermissionDetail(
            "Dosya Yazma",
            "Cihazınızda dosya oluşturabilir ve düzenleyebilir.",
            "Paylaşılan depolamada dosya kaydedebilir, düzenleyebilir veya silebilir."
        ),
        "android.permission.MANAGE_EXTERNAL_STORAGE" to PermissionDetail(
            "Tam Dosya Erişimi",
            "Cihazınızdaki tüm dosyalara erişebilir.",
            "En geniş depolama iznidir — uygulama herhangi bir dosyayı okuyabilir, değiştirebilir ve silebilir. Yalnızca dosya yöneticilerine verin."
        ),
        "android.permission.READ_MEDIA_IMAGES" to PermissionDetail(
            "Fotoğraf Erişimi",
            "Fotoğraflarınıza erişebilir.",
            "Uygulama cihazınızdaki tüm fotoğrafları görüntüleyebilir."
        ),
        "android.permission.READ_MEDIA_VIDEO" to PermissionDetail(
            "Video Erişimi",
            "Videolarınıza erişebilir.",
            "Uygulama cihazınızdaki tüm videoları görüntüleyebilir."
        ),
        "android.permission.READ_MEDIA_AUDIO" to PermissionDetail(
            "Müzik ve Ses",
            "Müzik ve ses dosyalarınıza erişebilir.",
            "Uygulama ses kütüphanenize erişebilir."
        ),
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to PermissionDetail(
            "Seçili Fotoğraf/Video",
            "Yalnızca seçtiğiniz fotoğraf ve videolara erişebilir.",
            "Gizliliğe daha saygılı bir yaklaşım — yalnızca sizin seçtiğiniz dosyalara erişir."
        ),

        // Takvim
        "android.permission.READ_CALENDAR" to PermissionDetail(
            "Takvimi Oku",
            "Takvim etkinliklerinizi okuyabilir.",
            "Takviminiz programınızı, toplantılarınızı ve planlarınızı ortaya çıkarır."
        ),
        "android.permission.WRITE_CALENDAR" to PermissionDetail(
            "Takvimi Düzenle",
            "Takvim etkinliklerini oluşturabilir veya değiştirebilir.",
            "Uygulama takviminize etkinlik ekleyebilir veya mevcut etkinlikleri değiştirebilir."
        ),

        // Vücut Sensörleri
        "android.permission.BODY_SENSORS" to PermissionDetail(
            "Vücut Sensörleri",
            "Sağlık sensörlerine (nabız vb.) erişebilir.",
            "Sağlık verileri son derece kişiseldir. Yalnızca fitness ve sağlık uygulamalarına verin."
        ),
        "android.permission.BODY_SENSORS_BACKGROUND" to PermissionDetail(
            "Arka Plan Sağlık İzleme",
            "Arka planda sağlık sensörlerine erişebilir.",
            "Uygulama kapalıyken bile sürekli sağlık izleme yapabilir."
        ),
        "android.permission.ACTIVITY_RECOGNITION" to PermissionDetail(
            "Aktivite Algılama",
            "Fiziksel aktivitenizi (yürüme, araç kullanma vb.) algılayabilir.",
            "Oturduğunuzu, yürüdüğünüzü, koştuğunuzu veya araçta olduğunuzu bilir."
        ),

        // Ağ
        "android.permission.INTERNET" to PermissionDetail(
            "İnternet Erişimi",
            "İnternete bağlanabilir.",
            "Çoğu uygulama internet erişimine ihtiyaç duyar. Ancak diğer izinlerle birleştiğinde veriler uzak sunuculara gönderilebilir."
        ),
        "android.permission.ACCESS_NETWORK_STATE" to PermissionDetail(
            "Ağ Durumu",
            "Ağ bağlantısını kontrol edebilir.",
            "Wi-Fi veya mobil veri ile bağlı olup olmadığınızı görebilir. Düşük gizlilik etkisi."
        ),
        "android.permission.ACCESS_WIFI_STATE" to PermissionDetail(
            "Wi-Fi Bilgisi",
            "Wi-Fi bağlantı detaylarını görebilir.",
            "Wi-Fi ağ adınızı görebilir, bu da konumunuzu ortaya çıkarabilir."
        ),
        "android.permission.CHANGE_WIFI_STATE" to PermissionDetail(
            "Wi-Fi Değiştir",
            "Wi-Fi ayarlarını değiştirebilir.",
            "Wi-Fi\'yi açıp kapatabilir ve ağ yapılandırmasını değiştirebilir."
        ),
        "android.permission.CHANGE_NETWORK_STATE" to PermissionDetail(
            "Ağ Değiştir",
            "Ağ ayarlarını değiştirebilir.",
            "Cihazınızın internete bağlanma şeklini değiştirebilir."
        ),
        "android.permission.NEARBY_WIFI_DEVICES" to PermissionDetail(
            "Yakın Wi-Fi Cihazları",
            "Yakındaki Wi-Fi cihazlarını keşfedebilir.",
            "Çevredeki Wi-Fi cihazlarını tarayabilir, bu konum takibi için kullanılabilir."
        ),

        // Bluetooth
        "android.permission.BLUETOOTH" to PermissionDetail(
            "Bluetooth",
            "Bluetooth kullanabilir.",
            "Aksesuarlara bağlanmak için temel Bluetooth yeteneği."
        ),
        "android.permission.BLUETOOTH_ADMIN" to PermissionDetail(
            "Bluetooth Ayarları",
            "Bluetooth ayarlarını yönetebilir.",
            "Cihazınızdaki Bluetooth yapılandırmasını değiştirebilir."
        ),
        "android.permission.BLUETOOTH_CONNECT" to PermissionDetail(
            "Bluetooth Bağlantısı",
            "Bluetooth cihazlarına bağlanabilir.",
            "Yakındaki Bluetooth cihazlarıyla eşleşebilir ve iletişim kurabilir."
        ),
        "android.permission.BLUETOOTH_SCAN" to PermissionDetail(
            "Bluetooth Taraması",
            "Yakındaki Bluetooth cihazlarını tarayabilir.",
            "Bluetooth cihazlarını taramak çevrenizdekiler hakkında bilgi edinmek için kullanılabilir."
        ),
        "android.permission.BLUETOOTH_ADVERTISE" to PermissionDetail(
            "Bluetooth Yayını",
            "Cihazınızı Bluetooth cihazlarına görünür yapabilir.",
            "Cihazınız yakındaki Bluetooth cihazları tarafından keşfedilebilir hale gelir."
        ),

        // Sistem
        "android.permission.VIBRATE" to PermissionDetail(
            "Titreşim",
            "Cihazınızı titretebilir.",
            "Düşük gizlilik etkisi. Bildirimler ve geri bildirim için kullanılır."
        ),
        "android.permission.WAKE_LOCK" to PermissionDetail(
            "Uyumayı Önle",
            "Cihazınızın uyumasını engelleyebilir.",
            "Cihazı uyanık tutarak pil ömrünü etkileyebilir."
        ),
        "android.permission.FOREGROUND_SERVICE" to PermissionDetail(
            "Arka Plan Servisi",
            "Arka planda servis çalıştırabilir.",
            "Uygulama aktif olarak kullanmadığınız zamanlarda görev gerçekleştirebilir."
        ),
        "android.permission.POST_NOTIFICATIONS" to PermissionDetail(
            "Bildirimler",
            "Size bildirim gönderebilir.",
            "Bildirim tercihlerini sistem ayarlarından yönetebilirsiniz."
        ),
        "android.permission.RECEIVE_BOOT_COMPLETED" to PermissionDetail(
            "Açılışta Başlat",
            "Cihaz açıldığında otomatik başlayabilir.",
            "Uygulama açmadan bile cihazınızı açtığınızda hemen çalışır."
        ),
        "android.permission.REQUEST_INSTALL_PACKAGES" to PermissionDetail(
            "Uygulama Yükle",
            "Başka uygulamalar yüklemeyi isteyebilir.",
            "Dikkatli olun — istenmeyen yazılımlar yüklemek için kullanılabilir."
        ),
        "android.permission.REQUEST_DELETE_PACKAGES" to PermissionDetail(
            "Uygulama Sil",
            "Başka uygulamaları silmeyi isteyebilir.",
            "Diğer uygulamaları kaldırmanızı isteyebilir."
        ),
        "android.permission.SYSTEM_ALERT_WINDOW" to PermissionDetail(
            "Üzerine Çiz",
            "Diğer uygulamaların üzerine içerik görüntüleyebilir.",
            "Diğer uygulamaların üstünde katman gösterebilir. Bazen meşru özellikler için kullanılır ancak kötüye kullanılabilir."
        ),
        "android.permission.SCHEDULE_EXACT_ALARM" to PermissionDetail(
            "Kesin Alarm",
            "Hassas zamanlayıcı kurabilir.",
            "Uygulamanın tam olarak belirli zamanlarda eylem tetiklemesine izin verir."
        ),
        "android.permission.USE_EXACT_ALARM" to PermissionDetail(
            "Kesin Alarm",
            "Hassas zamanlayıcı kurabilir.",
            "Uygulamanın tam olarak belirli zamanlarda eylem tetiklemesine izin verir."
        ),
        "android.permission.USE_BIOMETRIC" to PermissionDetail(
            "Biyometrik Doğrulama",
            "Parmak izi veya yüz tanıma kullanabilir.",
            "Güvenli kimlik doğrulama için kullanılır. Biyometrik veri uygulamayla paylaşılmaz."
        ),
        "android.permission.USE_FINGERPRINT" to PermissionDetail(
            "Parmak İzi",
            "Parmak izi doğrulaması kullanabilir.",
            "Güvenli giriş için kullanılır. Parmak izi verisi paylaşılmaz."
        ),
        "android.permission.NFC" to PermissionDetail(
            "NFC",
            "NFC donanımını kullanabilir.",
            "Temassız ödeme ve veri aktarımı için kullanılır."
        ),
        "android.permission.PACKAGE_USAGE_STATS" to PermissionDetail(
            "Kullanım İstatistikleri",
            "Hangi uygulamaları ne sıklıkla kullandığınızı görebilir.",
            "Uygulama kullanım kalıplarınızı ve alışkanlıklarınızı ortaya çıkarır. Son derece kişisel bilgidir."
        ),

        // Bind servisleri
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to PermissionDetail(
            "Erişilebilirlik Servisi",
            "Ekran içeriğinizi gözlemleyebilir ve etkileşimde bulunabilir.",
            "En güçlü izinlerden biri — ekrandaki her şeyi okuyabilir ve işlem yapabilir. Yalnızca güvendiğiniz erişilebilirlik araçlarına verin."
        ),
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" to PermissionDetail(
            "Bildirimleri Oku",
            "Tüm bildirimlerinizi okuyabilir.",
            "Mesaj önizlemeleri, e-postalar ve uyarılar dahil her bildirime erişebilir."
        ),
        "android.permission.BIND_VPN_SERVICE" to PermissionDetail(
            "VPN Servisi",
            "VPN bağlantısı oluşturabilir.",
            "Tüm internet trafiğiniz bu uygulamadan geçebilir. Yalnızca güvendiğiniz VPN servislerini kullanın."
        )
    )

    private val defaultDetail = PermissionDetail("System Permission", "System permission", "Standard Android permission.")
    private val defaultDetailTR = PermissionDetail("Sistem İzni", "Sistem izni", "Standart Android izni.")

    /**
     * Get localized permission details.
     */
    fun getDetail(permission: String): PermissionDetail {
        val isTurkish = Locale.getDefault().language == "tr"
        val map = if (isTurkish) descriptionsTR else descriptionsEN
        val fallback = if (isTurkish) defaultDetailTR else defaultDetail
        return map[permission] ?: fallback
    }

    /**
     * Legacy compatibility: returns just the description string.
     */
    fun get(permission: String): String {
        return getDetail(permission).description
    }
}
