# Barcode Localizer App for Helios Project
==========================================

Android app to integrate with BarcodeLocalizer code - takes pictures and uploads to Picasa web account.

## Downloading compiled APK file
--------------------------------

The compiled APK file BarcodeApp.apk is in the directory BarcodeApp/bin. First download OpenCV manager from the Google Play store and then install this APK file using adb install.

## Building from sources
------------------------
All the source files, AndroidManifest.xml and res files are available and can be importing in Eclipse using Import -> Android -> Existing Android code into Workspace.

The libraries that need to be included are as follows:

OpenCV4Android library project - needs to be referenced as a library project.

google-play-services.jar - needs to be referenced as a library project

Guava.jar - from Google, contains various helper classes

GData API JARs - gdata-core-1.0.jar, gdata-media-1.0.jar, gdata-photos-2.0.jar, gdata-photos-meta-2.0.jar. 

These can be downloaded from https://code.google.com/p/gdata-java-client/downloads/list. The samples zip contains pre-compiled jars.

NOTE: GData(based on XML) is the older version of the Google Data API's(based on JSON) and is generally deprecated for everything except Picasa Web API. https://code.google.com/p/gdata-java-client/ contains a migration timetable for various Google API's from GData to the newer API.

JavaMail for Android JAR's - mail.jar, activation.jar, additionnal.jar(sic).

These can be downloaded from https://code.google.com/p/javamail-android/.

NOTE: These are a modified version of the JAR's with the same names and the same purpose that are available from Sun. The Sun JAR's will not work properly with Android - these ones will. 