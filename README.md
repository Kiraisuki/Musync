# Musync
A simple file syncing program for Android

# How to use

Open the app on an Android device, enter a URL into the text box, and tap "GET FILES". The URL should point to a text file containing a list of all the files available to download, with each filename on a separate line. Files will be downloaded sequentially in the order they appear in the text document and they will be stored in the /musync folder on your Android device's internal storage. Musync does not currently support downloading files in separate directories from the filelist.

Musync skips downloading of files already in the /musync directory based on their filenames.

Written using Android Studio 3.1.4 and tested on a Galaxy S5 (Android 5.0.0). Min API version 14 (Ice Cream Sandwich), target API version 28 (Pie).
