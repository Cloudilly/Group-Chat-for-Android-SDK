#Anonymous Group Chat for Android SDK
Repository contains sample codes to build an anonymous group chat application using Cloudilly Android SDK.

![Anonymous](https://github.com/Cloudilly/Images/blob/master/android_anonymous.png)

---

#####Create app
If you have not already done so, first create an account on [Cloudilly](https://cloudilly.com). Next create an app with a unique app identifier and a cool name. Once done, you should arrive at the app page with all the access keys for the different platforms. Under Android SDK, you will find the parameters required for your Cloudilly application. _"Access"_ refers to the access keys to be embedded in Android codes. _"Bundle ID"_ refers to the applicationID that you have assigned your app. This applicationID can be found in your Android Studio project under app >> build.gradle >> defaultConfig. Leave the GCM _"Server Key"_ for now. This sample project doesn't require GCM push notifications

![Android Console](https://github.com/cloudilly/images/blob/master/android_console.png)

#####Update Access
[Insert your _"App Name"_ and _"Access"_](../../blob/master/app/src/main/java/com/cloudilly/anonymous/ChatActivity.java#L33-L34). Once done, build and run the application. Open up developer console to verify connection to Cloudilly. If you have setup the anonymous chat app for other platforms, you should also test if you can send messages across platforms, ie from Android to Web / iOS and vice versa.
