# Mobile Tracker

Android application for mobile sensor data collection, storage, and synchronization.

## Required Configuration

Please make sure
you [downloaded the Samsung Health SDKs](https://github.com/Kaist-ICLab/EnPULSE/blob/main/README.md#download-samsung-health-sensordata-sdk).

### Connecting the App with Supabase Backend

Supabase annonymous keys and server address are used to join campaign and send data. In
`local.properties`, set these 2 variables.

```
sdk.dir=C\:\\Your\\Android\\SDK\\directory
#Supabase key should go in here
SUPABASE_ANON_KEY=your_annoymous_key
SUPABASE_URL=your_self_hosted_supabase_server_address
```

* `SUPABASE_ANON_KEY`: The annonymous key can be found in the .env file in your supabase folder.
* `SUPABASE_URL`: The url is the same address that you access the supabase dashboard. Without manual
  setting, it uses port 8000.

### Connecting the App with Firebase Backend (Google Sign-In)

The `google-services.json` file is ignored by Git to keep project credentials secure. To enable Google Sign-In and Firebase services:

1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2. Register an Android App using the package name `kaist.iclab.trackerSystem`.
3. Download the generated `google-services.json` file.
4. Place the `google-services.json` file inside the `app-mobile-tracker/` directory.
5. Configure Google Sign-In in the Firebase Console (Authentication > Sign-in method > Google).

## Related Modules

- [`tracker-library`](../tracker-library/README.md) - Core sensor tracking library
- [`app-wearable-tracker`](../app-wearable-tracker/README.md) - Watch companion app

## Technical Information

Refer
to [structure.md](https://github.com/Kaist-ICLab/EnPULSE/blob/main/app-mobile-tracker/structure.md)
for overall architecture of the app.
