# Northwind

Java 17+, Spring Boot, Spring Data JPA ve PostgreSQL ile ürün ve kullanıcı kayıt API'si;
aynı uygulamadan sunulan, Türkçe ve mobil uyumlu Northwind Market vitrini.

## Çalıştırma

PowerShell ortamında:

```powershell
$env:DB_PASSWORD = 'yerel-veritabani-sifreniz'
.\mvnw.cmd spring-boot:run
```

Varsayılan bağlantı: `jdbc:postgresql://localhost:5432/Northwind`.
Veritabanı önceden oluşturulmalıdır. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`DB_DDL_AUTO` ve `SHOW_SQL` ile yapılandırılabilir. Şifreyi Git'e eklemeyin.
Geliştirmede varsayılan şema modu `update`; üretim profilinde `validate`.

Geliştirme API belgeleri: `http://localhost:8080/swagger-ui.html`.

## Frontend

Uygulama çalıştıktan sonra `http://localhost:8080` adresini açın.
Frontend, Spring Boot tarafından `src/main/resources/static` klasöründen sunulur.
Ayrı bir frontend sunucusu, npm kurulumu veya CORS yapılandırması gerekmez.
HTML dosyasını doğrudan `file://` ile açmak yerine uygulama adresini kullanın.

- Ürün kataloğu: sayfalama, sayfa boyutu seçimi, ürün adına göre arama ve kategori filtresi.
  Ürün adı aramaları büyük/küçük harfe duyarsızdır (`Chai`, `chai`, `CHAI` eşleşir).
  Aramada `i`, `İ`, `ı`, `I` eşdeğer kabul edilir; kayıtlı ürün adları değiştirilmez.
- Market vitrini: turuncu/mor tema, market görseli, kategori reyonları ve ürün kartları.
- Ürün detayı: ürün adına veya kartın görseline tıklayın.
- Alışveriş listesi: ürün kartındaki `+` düğmesiyle ekleyin; `Listem` penceresinde
  adet artırın/azaltın ve toplamı görün. Adet sıfıra inince ürün listeden çıkar.
  Liste bu tarayıcının `localStorage` alanında tutulur; depolama kapalıysa bellekte çalışır.
  Miktarlar son alınan stokla sınırlıdır. Liste stok ayırmaz, sipariş veya ödeme oluşturmaz;
  saklanan fiyat ve stok verileri katalogdan eklendiği ana aittir.
- Yeni ürün: formdaki kategoriler `GET /api/categories` üzerinden veritabanından alınır.
  Kullanılmayan kategoriler de listelenir. Kategori yoksa ürün kaydı kapatılır;
  önce veritabanına kategori ekleyin. Kategori oluşturma arayüzü bulunmaz.
- Kullanıcı kaydı: e-posta ve şifreyle kayıt yapılır; oturum açılmaz.
- Form doğrulama, alan bazında sunucu hataları, yüklenme, boş sonuç ve tekrar deneme durumları.
- Stok durumu ürün kartlarında gösterilir. Düşük stok eşiği 10 adettir;
  tükenen ürünlerde listeye ekleme kapalıdır. Sonuç sayısı aktif filtrelere göre değişir.
- Fiyatlar iki ondalık basamakla gösterilir. API para birimi tanımlamadığından simge eklenmez.
- API ürün fotoğrafı sunmadığından kartlarda yerel, kategoriye ait temsili çizimler kullanılır.
  Ana market görseli de projeye kaydedilmiştir; harici görsel sunucusu gerektirmez.

Normal liste sunucunun sayfalı `/api/products` uç noktasını kullanır. Mevcut arama ve
kategori uçları sayfasız liste döndürdüğünden filtrelenmiş sonuçlar tarayıcıda sıralanır,
birlikte filtrelenir ve sayfalanır. Çok büyük kataloglarda bu uçların backend tarafında
sayfalama desteklemesi gerekir.

Frontend dosyaları:

- `static/index.html`: ekranın HTML yapısı ve form pencereleri.
- `static/css/styles.css`: görünüm, mobil düzen ve erişilebilirlik yardımcıları.
- `static/js/api.mjs`: ortak HTTP/hata yönetimi, sorgular, filtreli sayfalama ve stok eşikleri.
- `static/js/app.js`: ekran durumu, DOM oluşturma, formlar ve kullanıcı etkileşimleri.
- `static/js/cart.mjs`: alışveriş listesi, stok sınırları, yerel kayıt doğrulaması ve kategori görsel eşlemesi.
- `static/images/`: ana görsel, kategori çizimleri ve simgeler.

JavaScript modülleri doğrudan tarayıcıda çalışır. API verileri `textContent` ile eklenir.
Şifreler tarayıcı depolamasına yazılmaz; kayıt penceresi kapanınca şifre alanı temizlenir.

## API

Yanıtlar mevcut `success`, `message`, `data` yapısını kullanır.
Hatalar: doğrulama için 400, bulunamayan kaynak için 404, çakışma için 409,
beklenmeyen sunucu hatası için ayrıntıları gizlenmiş 500.
Doğrulama hatalarının `data` alanı, alan adı -> mesaj listesi eşlemesidir.

| HTTP | Adres | Amaç |
|---|---|---|
| GET | /api/products?pageNo=1&pageSize=10 | Sayfalı ürünler ve toplam sayılar |
| GET | /api/products/{id} | Tek ürün |
| GET | /api/categories | Kimliğe göre sıralı kategori özeti |
| POST | /api/products | Ürün oluşturma, 201 ve Location |
| POST | /api/users | Kullanıcı oluşturma, 201 |
| GET | /api/products/getProductWithCategoryDetails | Ürün/kategori projeksiyonu |

Sayfa numarası 1'den başlar; sayfa boyutu 1–100 arasındadır.
Yeni istemciler sayfalı `GET /api/products` adresini kullanmalıdır.

Ürün oluşturma örneği (kategori önceden mevcut olmalı):

```json
{
  "productName": "Chai",
  "unitPrice": 12.50,
  "unitsInStock": 5,
  "quantityPerUnit": "10 kutu",
  "categoryId": 1
}
```

Kullanıcı oluşturma örneği:

```json
{
  "email": "emirhan@example.com",
  "password": "StrongPass123!"
}
```

E-postalar küçük harfe dönüştürülür. Aynı e-posta ile eşzamanlı kayıt,
veritabanındaki benzersizlik kısıtıyla da engellenir. Şifre 8–64 karakter ve
UTF-8 olarak en fazla 72 bayt olmalıdır. DelegatingPasswordEncoder varsayılan
BCrypt hash'i kullanır; şifre veya hash yanıt DTO'suna dahil edilmez.

## Önceki API'den geçiş

- Eski `/api/products/add` ve `/api/users/add` yolları korunur.
- Ürün ekleme gövdesinde `category: { categoryId: 1 }` yerine `categoryId: 1` gönderilir.
- İsteklerden entity kimliği alınmaz; kayıt isteği mevcut kaydı güncelleyemez.
- Ekleme yanıtı artık 201 durum kodu ve oluşturulan kaydın DTO'sunu içerir.
- `/getall`, `/getallsorted`, `/getallbypage` liste biçimini korur.
- İsim/kategori arama yolları korunur; startsWith, kategori listesi ve OR aramaları da açılmıştır.
- `ProductWithCategoryDto` değişmez bir Java record oldu. JSON alanları aynı.
- Fiyat entity alanı `double` yerine `BigDecimal` kullanır.
- UserService içindeki kullanılmayan e-posta sorgusu kaldırıldı; herkese açık kullanıcı arama endpoint'i eklenmedi.

## Mevcut PostgreSQL verisine geçiş

Değişiklikler geliştirme/test ortamında doğrulandı; gerçek Northwind verisine
otomatik temizlik veya SQL geçişi uygulanmadı. Üretime geçmeden önce:

1. Yedek alın; kullanıcı e-postalarında boş/null, 254 karakteri aşan ve
   büyük/küçük harf veya çevre boşlukları dışında aynı olan kayıtları denetleyin.
2. `docs/postgresql-preflight.sql` salt okunur kontrollerini çalıştırın.
   Çakışmaları çözmeden e-postaları normalize etmeyin.
3. E-postaları uygulamayla aynı biçimde normalize edin; `users.email`
   alanını benzersiz ve NOT NULL, `password` alanını NOT NULL yapın.
   Hibernate geliştirme modunda kısıtları oluşturmaya çalışır; üretimde
   doğrulanmış bir veritabanı geçişiyle açıkça uygulayın.
4. Önceden düz metin kaydedilmiş şifreler kendiliğinden hash'e dönüşmez.
   Güvenli bir şifre sıfırlama/geçiş süreci gerekir. Mevcut şifreleri loglamayın.
5. Ürün adlarının 40, paket bilgisinin 20 karakter sınırına uygunluğunu ve
   fiyat sütununun sayısal tipini mevcut şemada kontrol edin.
6. Şema doğrulandıktan sonra `SPRING_PROFILES_ACTIVE=prod` ile çalıştırın.
   Bu profil otomatik şema güncellemez ve Swagger belgelerini kapatır.

Bu sürüm kullanıcı kaydı sunar. Giriş, token üretimi, rol ve endpoint yetkilendirmesi
henüz uygulanmadı; mevcut ürün endpoint'leri anonim erişime açıktır.

## Testler

```powershell
.\mvnw.cmd test
```

Frontend API katmanı testleri Node.js 18+ ile bağımlılıksız çalıştırılabilir:

```powershell
node --test src/test/frontend/api.test.mjs
node --test src/test/frontend/cart.test.mjs
```

Java entegrasyon testleri kategori listesini ve frontend dosyalarının Spring tarafından
sunulduğunu da kontrol eder. JavaScript testleri API sözleşmesi, arama/kategori birleşimi,
sayfalama, stok eşikleri ve hata durumlarını denetler.

Testler yalnızca test classpath'inde bulunan H2 bellekiçi veritabanını kullanır;
PostgreSQL kurulumu veya DB_PASSWORD gerektirmez. HTTP -> servis -> repository
akışı, şifre hashleme, eşzamanlı mükerrer kayıt, DTO, doğrulama, hata durumları,
arama, JPQL projeksiyonu ve sayfalama kontrol edilir.
Testler PostgreSQL'e özgü şema geçişlerinin doğrulaması yerine geçmez.

## Yapı

Controller -> Service interface -> Manager -> Repository -> Veritabanı.
HTTP sınırında istek/yanıt DTO'ları, servislerde transaction sınırları,
merkezi exception handler ve kategori erişimi için entity graph kullanılır.
Open Session in View kapalıdır; DTO dönüşümleri servis transaction'ında yapılır.

Şifre saklama yaklaşımı:
[Spring Security Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
