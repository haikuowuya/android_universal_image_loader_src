package com.nostra13.universalimageloader.core;

import android.content.Context;
import android.graphics.Bitmap;
import com.nostra13.universalimageloader.cache.disc.DiscCacheAware;
import com.nostra13.universalimageloader.cache.disc.impl.FileCountLimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.MemoryCacheAware;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.assist.deque.LIFOLinkedBlockingDeque;
import com.nostra13.universalimageloader.core.decode.BaseImageDecoder;
import com.nostra13.universalimageloader.core.decode.ImageDecoder;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 为 {@linkplain ImageLoaderConfiguration configuration}提供默认操作的工厂类
 */
public class DefaultConfigurationFactory
{

	/** 创建默认的任务执行者 */
	public static Executor createExecutor(int threadPoolSize, int threadPriority, QueueProcessingType tasksProcessingType)
	{
		boolean lifo = tasksProcessingType == QueueProcessingType.LIFO;
		BlockingQueue<Runnable> taskQueue = lifo ? new LIFOLinkedBlockingDeque<Runnable>() : new LinkedBlockingQueue<Runnable>();
		return new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, TimeUnit.MILLISECONDS, taskQueue, createThreadFactory(threadPriority));
	}

	/**
	 * Creates {@linkplain HashCodeFileNameGenerator default implementation} of
	 * FileNameGenerator
	 */
	public static FileNameGenerator createFileNameGenerator()
	{
		return new HashCodeFileNameGenerator();
	}

	/**
	 * 根据参数创建默认的本地缓存策略{@link DiscCacheAware}
	 */
	public static DiscCacheAware createDiscCache(Context context, FileNameGenerator discCacheFileNameGenerator, int discCacheSize, int discCacheFileCount)
	{
		if (discCacheSize > 0) //初始值默认的本地缓存大小为0
		{
			File individualCacheDir = StorageUtils.getIndividualCacheDirectory(context);
			return new TotalSizeLimitedDiscCache(individualCacheDir, discCacheFileNameGenerator, discCacheSize);
		}
		else if (discCacheFileCount > 0) //初始值默认的本地缓存文件个数为0
		{
			File individualCacheDir = StorageUtils.getIndividualCacheDirectory(context);
			return new FileCountLimitedDiscCache(individualCacheDir, discCacheFileNameGenerator, discCacheFileCount);
		}
		else
		{
			File cacheDir = StorageUtils.getCacheDirectory(context);
			return new UnlimitedDiscCache(cacheDir, discCacheFileNameGenerator);
		}
	}

	/**
	 * Creates reserve disc cache which will be used if primary disc cache
	 * becomes unavailable
	 */
	public static DiscCacheAware createReserveDiscCache(File cacheDir)
	{
		File individualDir = new File(cacheDir, "uil-images");
		if (individualDir.exists() || individualDir.mkdir())
		{
			cacheDir = individualDir;
		}
		return new TotalSizeLimitedDiscCache(cacheDir, 2 * 1024 * 1024); // limit
																			// -
																			// 2
																			// Mb
	}

	/**
	 * Creates default implementation of {@link MemoryCacheAware} -
	 * {@link LruMemoryCache}<br />
	 * Default cache size = 1/8 of available app memory.
	 */
	public static MemoryCacheAware<String, Bitmap> createMemoryCache(int memoryCacheSize)
	{
		if (memoryCacheSize == 0)
		{
			memoryCacheSize = (int) (Runtime.getRuntime().maxMemory() / 8);
		}
		return new LruMemoryCache(memoryCacheSize);
	}

	/**
	 * Creates default implementation of {@link ImageDownloader} -
	 * {@link BaseImageDownloader}
	 */
	public static ImageDownloader createImageDownloader(Context context)
	{
		return new BaseImageDownloader(context);
	}

	/**
	 * Creates default implementation of {@link ImageDecoder} -
	 * {@link BaseImageDecoder}
	 */
	public static ImageDecoder createImageDecoder(boolean loggingEnabled)
	{
		return new BaseImageDecoder(loggingEnabled);
	}

	/**
	 * Creates default implementation of {@link BitmapDisplayer} -
	 * {@link SimpleBitmapDisplayer}
	 */
	public static BitmapDisplayer createBitmapDisplayer()
	{
		return new SimpleBitmapDisplayer();
	}

	/**
	 * Creates default implementation of {@linkplain ThreadFactory thread
	 * factory} for task executor
	 */
	private static ThreadFactory createThreadFactory(int threadPriority)
	{
		return new DefaultThreadFactory(threadPriority);
	}

	private static class DefaultThreadFactory implements ThreadFactory
	{

		private static final AtomicInteger poolNumber = new AtomicInteger(1);

		private final ThreadGroup group;
		private final AtomicInteger threadNumber = new AtomicInteger(1);
		private final String namePrefix;
		private final int threadPriority;

		DefaultThreadFactory(int threadPriority)
		{
			this.threadPriority = threadPriority;
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = "uil-pool-" + poolNumber.getAndIncrement() + "-thread-";
		}

		@Override
		public Thread newThread(Runnable r)
		{
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
			if (t.isDaemon())
				t.setDaemon(false);
			t.setPriority(threadPriority);
			return t;
		}
	}
}
