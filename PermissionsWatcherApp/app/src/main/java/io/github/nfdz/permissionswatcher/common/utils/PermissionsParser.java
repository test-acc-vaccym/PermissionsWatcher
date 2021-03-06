package io.github.nfdz.permissionswatcher.common.utils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.nfdz.permissionswatcher.common.model.ApplicationInfo;
import io.github.nfdz.permissionswatcher.common.model.PermissionState;
import io.realm.RealmList;

import static android.content.pm.PermissionInfo.PROTECTION_DANGEROUS;

public class PermissionsParser {

    private static final boolean NOTIFY_FLAG_DEFAULT = true;

    private final PackageManager pm;

    public PermissionsParser(PackageManager pm) {
        this.pm = pm;
    }

    @NonNull
    public Map<String,ApplicationInfo> retrieveAllAppsWithPermissions() {
        Map<String,ApplicationInfo> applications = new HashMap<>();
        List<android.content.pm.ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        if (apps == null || apps.isEmpty()) return applications;
        for (android.content.pm.ApplicationInfo app : apps) {
            Map<String,Boolean> permissionsMap = tryToGetPermissions(app);
            if (permissionsMap == null || permissionsMap.isEmpty()) continue;
            RealmList<PermissionState> permissionsList = new RealmList<>();
            boolean anyGranted = addPermissions(permissionsMap, permissionsList);
            String packageName = app.packageName;
            String label = tryToGetLabel(app);
            Integer versionCode = tryToGetVersionCode(app);
            String versionName = tryToGetVersionName(app);
            boolean isSystemApplication = (app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0;
            applications.put(packageName, new ApplicationInfo(packageName,
                    label,
                    versionCode,
                    versionName,
                    isSystemApplication,
                    permissionsList,
                    NOTIFY_FLAG_DEFAULT,
                    anyGranted));
        }
        return applications;
    }

    private boolean addPermissions(Map<String,Boolean> permissionsMap,
                                RealmList<PermissionState> permissionsList) {
        boolean anyGranted = false;
        for (Map.Entry<String,Boolean> entry : permissionsMap.entrySet()) {
            boolean isGranted = entry.getValue();
            anyGranted = anyGranted || isGranted;
            permissionsList.add(new PermissionState(entry.getKey(),
                    isGranted,
                    isGranted, // has changes = true only if it is granted
                    NOTIFY_FLAG_DEFAULT));
        }
        return anyGranted;
    }

    @Nullable
    private Map<String,Boolean> tryToGetPermissions(android.content.pm.ApplicationInfo app) {
        try {
            PackageInfo pi = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS);
            if (pi.requestedPermissions == null || pi.requestedPermissions.length == 0) return null;
            Map<String,Boolean> permissions = new HashMap<>();
            for (int i = 0; i < pi.requestedPermissions.length; ++i) {
                String permission = pi.requestedPermissions[i];
                if (!TextUtils.isEmpty(permission) &&
                        PermissionsUtils.isAndroidPermission(permission) &&
                        isDangerousPermission(permission)) {
                    permissions.put(permission,
                            0 != (pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED));
                }
            }
            return permissions;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isDangerousPermission(String permission) {
        try {
            int protLevel = pm.getPermissionInfo(permission, 0).protectionLevel;
            return protLevel == PROTECTION_DANGEROUS;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Nullable
    private String tryToGetLabel(android.content.pm.ApplicationInfo app) {
        try {
            return pm.getApplicationLabel(app).toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private Integer tryToGetVersionCode(android.content.pm.ApplicationInfo app) {
        try {
            PackageInfo pi = pm.getPackageInfo(app.packageName, PackageManager.GET_META_DATA);
            return pi.versionCode;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private String tryToGetVersionName(android.content.pm.ApplicationInfo app) {
        try {
            PackageInfo pi = pm.getPackageInfo(app.packageName, PackageManager.GET_META_DATA);
            return pi.versionName;
        } catch (Exception e) {
            return null;
        }
    }

}
