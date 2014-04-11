/*******************************************************************************
 * Copyright 2011-2013 Sergey Tarasevich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.nostra13.universalimageloader.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * 文件存储路径工具类
 */
public final class StorageUtils
{

	private static final String EXTERNAL_STORAGE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE";
	private static final String INDIVIDUAL_DIR_NAME = "uil-images";

	private StorageUtils()
	{}

	/**
	 * 返回应用程序的缓存目录<br/>
	 * 如果当前设备有SD卡并且可以使用并且具有写的权限，那么缓存文件将保持在【/mnt/sdcard/android/data/应用程序包名/cache
	 * 】目录下<br/>
	 * 否则，缓存文件将保存在【/data/data/应用程序包名/cache】目录下
	 */
	public static File getCacheDirectory(Context context)
	{
		return getCacheDirectory(context, true);
	}

	/**
	 * 返回应用程序的缓存目录<br/>
	 * 参数 preferExternal 表示是否优先保存缓存文件到SD卡<br/>
	 * 如果当前设备有SD卡并且可以使用并且具有写的权限，缓存文件将保持在【/mnt/sdcard/android/data/应用程序包名/cache
	 * 】目录下<br/>
	 * 否则，缓存文件将保存在【/data/data/应用程序包名/cache】目录下
	 */
	public static File getCacheDirectory(Context context, boolean preferExternal)
	{
		File appCacheDir = null;
		if (preferExternal && MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && hasExternalStoragePermission(context))
		{
			appCacheDir = getExternalCacheDir(context);
		}
		if (appCacheDir == null)
		{
			appCacheDir = context.getCacheDir();
		}
		if (appCacheDir == null)
		{
			String cacheDirPath = "/data/data/" + context.getPackageName() + "/cache/";
			L.w("Can't define system cache directory! '%s' will be used.", cacheDirPath);
			appCacheDir = new File(cacheDirPath);
		}
		return appCacheDir;
	}

	/**
	 * Returns individual application cache directory (for only image caching
	 * from ImageLoader). Cache directory will be created on SD card
	 * <i>("/Android/data/[app_package_name]/cache/uil-images")</i> if card is
	 * mounted and app has appropriate permission. Else - Android defines cache
	 * directory on device's file system.
	 * 
	 * @param context
	 *            Application context
	 * @return Cache {@link File directory}
	 */
	public static File getIndividualCacheDirectory(Context context)
	{
		File cacheDir = getCacheDirectory(context);
		File individualCacheDir = new File(cacheDir, INDIVIDUAL_DIR_NAME);
		if (!individualCacheDir.exists())
		{
			if (!individualCacheDir.mkdir())
			{
				individualCacheDir = cacheDir;
			}
		}
		return individualCacheDir;
	}

	/**
	 * Returns specified application cache directory. Cache directory will be
	 * created on SD card by defined path if card is mounted and app has
	 * appropriate permission. Else - Android defines cache directory on
	 * device's file system.
	 * 
	 * @param context
	 *            Application context
	 * @param cacheDir
	 *            Cache directory path (e.g.: "AppCacheDir",
	 *            "AppDir/cache/images")
	 * @return Cache {@link File directory}
	 */
	public static File getOwnCacheDirectory(Context context, String cacheDir)
	{
		File appCacheDir = null;
		if (MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && hasExternalStoragePermission(context))
		{
			appCacheDir = new File(Environment.getExternalStorageDirectory(), cacheDir);
		}
		if (appCacheDir == null || (!appCacheDir.exists() && !appCacheDir.mkdirs()))
		{
			appCacheDir = context.getCacheDir();
		}
		return appCacheDir;
	}

	/**
	 * 获取外部SD卡缓存文件目录 【/mnt/sdcard/android/data/应用程序包名/cache】
	 */
	private static File getExternalCacheDir(Context context)
	{
		File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
		File appCacheDir = new File(new File(dataDir, context.getPackageName()), "cache");
		if (!appCacheDir.exists())
		{
			if (!appCacheDir.mkdirs())
			{
				L.w("Unable to create external cache directory");
				return null;
			}
			try
			{
				new File(appCacheDir, ".nomedia").createNewFile();
			}
			catch (IOException e)
			{
				L.i("Can't create \".nomedia\" file in application external cache directory");
			}
		}
		return appCacheDir;
	}

	/** 检查是否拥有写外部SD卡的权限 */
	private static boolean hasExternalStoragePermission(Context context)
	{
		int perm = context.checkCallingOrSelfPermission(EXTERNAL_STORAGE_PERMISSION);
		return perm == PackageManager.PERMISSION_GRANTED;
	}
}
