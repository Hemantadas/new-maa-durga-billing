# Cloud Sync Authentication (Development only)

এই ফাংশনটি Shop ID + PIN যাচাই করে Firebase Custom Token দেয়। SMS OTP ব্যবহার করে না।

## নিরাপত্তা
- Android app থেকে PIN সরাসরি Firestore-এ যাচাই করা হয় না।
- PIN hash ও salt কেবল server-side `shopAuthIndex` collection-এ থাকে।
- `shopAuthIndex` Firestore Rules-এ client-এর জন্য সম্পূর্ণ বন্ধ।
- App Check বাধ্যতামূলক।
- প্রতি Shop ID-তে ৫ বার ভুল PIN হলে ১৫ মিনিট lock হয়।
- Secret `SHOP_PIN_PEPPER` Secret Manager-এ থাকবে; APK বা Git-এ নয়।
- Firestore data path হবে `shops/{Firebase Auth UID}/...`।

## Deploy করার আগে আবশ্যক
1. Firebase project-কে Blaze plan-এ নিতে হবে (Cloud Functions deployment-এর জন্য billing account প্রয়োজন; ব্যবহার না করলে খরচ নাও হতে পারে, তবে usage অনুযায়ী charge হতে পারে)।
2. Android app-কে Firebase App Check / Play Integrity-তে register ও verify করতে হবে।
3. Secret Manager-এ দীর্ঘ random `SHOP_PIN_PEPPER` secret সেট করতে হবে।
4. Owner-এর অনুমোদিত secure provisioning process দিয়ে `shopAuthIndex`-এ UID, random salt, scrypt PIN hash তৈরি করতে হবে।
5. Firebase Authentication-এ custom authentication token sign-in ব্যবহার করতে Android native bridge যোগ করতে হবে।
6. তারপরেই Firestore Rules deploy এবং test করা যাবে।

**এই scaffold এখনো deploy করা হয়নি।** Live Firebase Rules এবং বর্তমান APK অপরিবর্তিত।
