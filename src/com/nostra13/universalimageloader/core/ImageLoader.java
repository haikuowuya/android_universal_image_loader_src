package com.nostra13.universalimageloader.core;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import com.nostra13.universalimageloader.cache.disc.DiscCacheAware;
import com.nostra13.universalimageloader.cache.memory.MemoryCacheAware;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.FlushedInputStream;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageNonViewAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SyncImageLoadingListener;
import com.nostra13.universalimageloader.utils.ImageSizeUtils;
import com.nostra13.universalimageloader.utils.L;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;

/**
 * 用于 Android ImageView 显示和加载图片的工具类，单例的， 注意：该类中的
 * {@linkplain com.nostra13.universalimageloader.core ImageLoader#init(ImageLoaderConfiguration)}必须在任何方法调用之前调用,因为 ImageLoaderEngine 是在该方法中实例化的
 */
public class ImageLoader
{
	public static final String TAG = ImageLoader.class.getSimpleName();
	static final String LOG_INIT_CONFIG = "Initialize ImageLoader with configuration";
	static final String LOG_DESTROY = "Destroy ImageLoader";
	static final String LOG_LOAD_IMAGE_FROM_MEMORY_CACHE = "Load image from memory cache [%s]";

	private static final String WARNING_RE_INIT_CONFIG = "试图初始化一个已经被初始化的ImageLoader对象出错   " + "如果想重新初始化ImageLoader ，请先调用  ImageLoader.destroy()  ";
	private static final String ERROR_WRONG_ARGUMENTS = "Wrong arguments were passed to displayImage() method (ImageView reference must not be null)";
	private static final String ERROR_NOT_INIT = "ImageLoader在使用之前必须先调用init方法进行初始化  ";
	private static final String ERROR_INIT_CONFIG_WITH_NULL = "ImageLoader 初始化时的配置参数信息不可以为空 ";

	private ImageLoaderConfiguration configuration;
	private ImageLoaderEngine engine;

	private final ImageLoadingListener emptyListener = new SimpleImageLoadingListener();

	private volatile static ImageLoader instance;

	/** 返回该类的单例实例 */
	public static ImageLoader getInstance()
	{
		if (instance == null)
		{
			synchronized (ImageLoader.class)
			{
				if (instance == null)
				{
					instance = new ImageLoader();
				}
			}
		}
		return instance;
	}

	protected ImageLoader()
	{}

	/**
	 * 根据给定的 {@link ImageLoaderConfiguration} 初始化 ImageLoader 对象
	 * 注意如果前面已经调用过init方法初始化了，那么该方法就不起作用了，如果想让重新设置
	 * {@link ImageLoaderConfiguration}应该先调用{@linkplain #destroy()}方法
	 * 
	 * @param configuration
	 *            {@linkplain ImageLoaderConfiguration ImageLoader
	 *            configuration}
	 * @throws IllegalArgumentException
	 *             if <b>configuration</b> parameter is null
	 */
	public synchronized void init(ImageLoaderConfiguration configuration)
	{
		if (configuration == null)
		{
			throw new IllegalArgumentException(ERROR_INIT_CONFIG_WITH_NULL);
		}
		if (this.configuration == null)
		{
			if (configuration.writeLogs)
			{
				L.d(LOG_INIT_CONFIG);
			}
			engine = new ImageLoaderEngine(configuration);
			this.configuration = configuration;
		}
		else
		{
			L.w(WARNING_RE_INIT_CONFIG);
		}
	}

	/** 如果已经初始化过 返回 true ,否则 false */
	public boolean isInited()
	{
		return configuration != null;
	}

	/**
	 * Adds display image task to execution pool. Image will be set to
	 * ImageAware when it's turn. <br/>
	 * Default {@linkplain DisplayImageOptions display image options} from
	 * {@linkplain ImageLoaderConfiguration configuration} will be used.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param imageAware
	 *            {@linkplain com.nostra13.universalimageloader.core.imageaware.ImageAware
	 *            Image aware view} which should display image
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 * @throws IllegalArgumentException
	 *             if passed <b>imageAware</b> is null
	 */
	public void displayImage(String uri, ImageAware imageAware)
	{
		displayImage(uri, imageAware, null, null, null);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to
	 * ImageAware when it's turn.<br />
	 * Default {@linkplain DisplayImageOptions display image options} from
	 * {@linkplain ImageLoaderConfiguration configuration} will be used.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param imageAware
	 *            {@linkplain com.nostra13.universalimageloader.core.imageaware.ImageAware
	 *            Image aware view} which should display image
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 * @throws IllegalArgumentException
	 *             if passed <b>imageAware</b> is null
	 */
	public void displayImage(String uri, ImageAware imageAware, ImageLoadingListener listener)
	{
		displayImage(uri, imageAware, null, listener, null);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to
	 * ImageAware when it's turn.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param imageAware
	 *            {@linkplain com.nostra13.universalimageloader.core.imageaware.ImageAware
	 *            Image aware view} which should display image
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and displaying. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 * @throws IllegalArgumentException
	 *             if passed <b>imageAware</b> is null
	 */
	public void displayImage(String uri, ImageAware imageAware, DisplayImageOptions options)
	{
		displayImage(uri, imageAware, options, null, null);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to
	 * ImageAware when it's turn.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param imageAware
	 *            {@linkplain com.nostra13.universalimageloader.core.imageaware.ImageAware
	 *            Image aware view} which should display image
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and displaying. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 * @throws IllegalArgumentException
	 *             if passed <b>imageAware</b> is null
	 */
	public void displayImage(String uri, ImageAware imageAware, DisplayImageOptions options, ImageLoadingListener listener)
	{
		displayImage(uri, imageAware, options, listener, null);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to
	 * ImageAware when it's turn.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param imageAware
	 *            {@linkplain com.nostra13.universalimageloader.core.imageaware.ImageAware
	 *            Image aware view} which should display image
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and displaying. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @param progressListener
	 *            {@linkplain com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener
	 *            Listener} for image loading progress. Listener fires events on
	 *            UI thread if this method is called on UI thread. Caching on
	 *            disc should be enabled in
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            options} to make this listener work.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 * @throws IllegalArgumentException
	 *             if passed <b>imageAware</b> is null
	 */
	public void displayImage(String uri, ImageAware imageAware, DisplayImageOptions options, ImageLoadingListener listener, ImageLoadingProgressListener progressListener)
	{
		checkConfiguration();
		if (imageAware == null)
		{
			throw new IllegalArgumentException(ERROR_WRONG_ARGUMENTS);
		}
		if (listener == null)
		{
			listener = emptyListener;
		}
		if (options == null)
		{
			options = configuration.defaultDisplayImageOptions;
		}

		if (TextUtils.isEmpty(uri))
		{
			engine.cancelDisplayTaskFor(imageAware);
			listener.onLoadingStarted(uri, imageAware.getWrappedView());
			if (options.shouldShowImageForEmptyUri())
			{
				imageAware.setImageDrawable(options.getImageForEmptyUri(configuration.resources));
			}
			else
			{
				imageAware.setImageDrawable(null);
			}
			listener.onLoadingComplete(uri, imageAware.getWrappedView(), null);
			return;
		}

		ImageSize targetSize = ImageSizeUtils.defineTargetSizeForView(imageAware, configuration.getMaxImageSize());
		String memoryCacheKey = MemoryCacheUtils.generateKey(uri, targetSize);
		engine.prepareDisplayTaskFor(imageAware, memoryCacheKey);

		listener.onLoadingStarted(uri, imageAware.getWrappedView());

		Bitmap bmp = configuration.memoryCache.get(memoryCacheKey);
		if (bmp != null && !bmp.isRecycled())
		{
			if (configuration.writeLogs)
				L.d(LOG_LOAD_IMAGE_FROM_MEMORY_CACHE, memoryCacheKey);

			if (options.shouldPostProcess())
			{
				ImageLoadingInfo imageLoadingInfo = new ImageLoadingInfo(uri, imageAware, targetSize, memoryCacheKey, options, listener, progressListener, engine.getLockForUri(uri));
				ProcessAndDisplayImageTask displayTask = new ProcessAndDisplayImageTask(engine, bmp, imageLoadingInfo, defineHandler(options));
				if (options.isSyncLoading())
				{
					displayTask.run();
				}
				else
				{
					engine.submit(displayTask);
				}
			}
			else
			{
				options.getDisplayer().display(bmp, imageAware, LoadedFrom.MEMORY_CACHE);
				listener.onLoadingComplete(uri, imageAware.getWrappedView(), bmp);
			}
		}
		else
		{
			if (options.shouldShowImageOnLoading())
			{
				imageAware.setImageDrawable(options.getImageOnLoading(configuration.resources));
			}
			else if (options.isResetViewBeforeLoading())
			{
				imageAware.setImageDrawable(null);
			}

			ImageLoadingInfo imageLoadingInfo = new ImageLoadingInfo(uri, imageAware, targetSize, memoryCacheKey, options, listener, progressListener, engine.getLockForUri(uri));
			LoadAndDisplayImageTask displayTask = new LoadAndDisplayImageTask(engine, imageLoadingInfo, defineHandler(options));
			if (options.isSyncLoading())
			{
				displayTask.run();
			}
			else
			{
				engine.submit(displayTask);
			}
		}
	}

	/**
	 * Adds display image task to execution pool. Image will be set to ImageView
	 * when it's turn. <br/>
	 * Default {@linkplain DisplayImageOptions display image options} from
	 * {@linkplain ImageLoaderConfiguration configuration} will be used.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param imageView
	 *            {@link ImageView} which should display image
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 * @throws IllegalArgumentException
	 *             if passed <b>imageView</b> is null
	 */
	public void displayImage(String uri, ImageView imageView)
	{
		displayImage(uri, new ImageViewAware(imageView), null, null, null);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to ImageView
	 * when it's turn.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param imageView
	 *            {@link ImageView} which should display image
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and displaying. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 * @throws IllegalArgumentException
	 *             if passed <b>imageView</b> is null
	 */
	public void displayImage(String uri, ImageView imageView, DisplayImageOptions options)
	{
		displayImage(uri, new ImageViewAware(imageView), options, null, null);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to ImageView
	 * when it's turn.<br />
	 * Default {@linkplain DisplayImageOptions display image options} from
	 * {@linkplain ImageLoaderConfiguration configuration} will be used.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param imageView
	 *            {@link ImageView} which should display image
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 * @throws IllegalArgumentException
	 *             if passed <b>imageView</b> is null
	 */
	public void displayImage(String uri, ImageView imageView, ImageLoadingListener listener)
	{
		displayImage(uri, new ImageViewAware(imageView), null, listener, null);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to ImageView
	 * when it's turn.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param imageView
	 *            {@link ImageView} which should display image
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and displaying. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 * @throws IllegalArgumentException
	 *             if passed <b>imageView</b> is null
	 */
	public void displayImage(String uri, ImageView imageView, DisplayImageOptions options, ImageLoadingListener listener)
	{
		displayImage(uri, imageView, options, listener, null);
	}

	/**
	 * Adds display image task to execution pool. Image will be set to ImageView
	 * when it's turn.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param imageView
	 *            {@link ImageView} which should display image
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and displaying. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @param progressListener
	 *            {@linkplain com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener
	 *            Listener} for image loading progress. Listener fires events on
	 *            UI thread if this method is called on UI thread. Caching on
	 *            disc should be enabled in
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            options} to make this listener work.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 * @throws IllegalArgumentException
	 *             if passed <b>imageView</b> is null
	 */
	public void displayImage(String uri, ImageView imageView, DisplayImageOptions options, ImageLoadingListener listener, ImageLoadingProgressListener progressListener)
	{
		displayImage(uri, new ImageViewAware(imageView), options, listener, progressListener);
	}

	/**
	 * Adds load image task to execution pool. Image will be returned with
	 * {@link ImageLoadingListener#onLoadingComplete(String, android.view.View, android.graphics.Bitmap)}
	 * callback}. <br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public void loadImage(String uri, ImageLoadingListener listener)
	{
		loadImage(uri, null, null, listener, null);
	}

	/**
	 * Adds load image task to execution pool. Image will be returned with
	 * {@link ImageLoadingListener#onLoadingComplete(String, android.view.View, android.graphics.Bitmap)}
	 * callback}. <br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param targetImageSize
	 *            Minimal size for {@link Bitmap} which will be returned in
	 *            {@linkplain ImageLoadingListener#onLoadingComplete(String, android.view.View, android.graphics.Bitmap)}
	 *            callback}. Downloaded image will be decoded and scaled to
	 *            {@link Bitmap} of the size which is <b>equal or larger</b>
	 *            (usually a bit larger) than incoming targetImageSize.
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public void loadImage(String uri, ImageSize targetImageSize, ImageLoadingListener listener)
	{
		loadImage(uri, targetImageSize, null, listener, null);
	}

	/**
	 * Adds load image task to execution pool. Image will be returned with
	 * {@link ImageLoadingListener#onLoadingComplete(String, android.view.View, android.graphics.Bitmap)}
	 * callback}. <br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and displaying. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.<br />
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public void loadImage(String uri, DisplayImageOptions options, ImageLoadingListener listener)
	{
		loadImage(uri, null, options, listener, null);
	}

	/**
	 * Adds load image task to execution pool. Image will be returned with
	 * {@link ImageLoadingListener#onLoadingComplete(String, android.view.View, android.graphics.Bitmap)}
	 * callback}. <br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param targetImageSize
	 *            Minimal size for {@link Bitmap} which will be returned in
	 *            {@linkplain ImageLoadingListener#onLoadingComplete(String, android.view.View, android.graphics.Bitmap)}
	 *            callback}. Downloaded image will be decoded and scaled to
	 *            {@link Bitmap} of the size which is <b>equal or larger</b>
	 *            (usually a bit larger) than incoming targetImageSize.
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and displaying. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.<br />
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public void loadImage(String uri, ImageSize targetImageSize, DisplayImageOptions options, ImageLoadingListener listener)
	{
		loadImage(uri, targetImageSize, options, listener, null);
	}

	/**
	 * Adds load image task to execution pool. Image will be returned with
	 * {@link ImageLoadingListener#onLoadingComplete(String, android.view.View, android.graphics.Bitmap)}
	 * callback}. <br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param targetImageSize
	 *            Minimal size for {@link Bitmap} which will be returned in
	 *            {@linkplain ImageLoadingListener#onLoadingComplete(String, android.view.View, android.graphics.Bitmap)}
	 *            callback}. Downloaded image will be decoded and scaled to
	 *            {@link Bitmap} of the size which is <b>equal or larger</b>
	 *            (usually a bit larger) than incoming targetImageSize.
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and displaying. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.<br />
	 * @param listener
	 *            {@linkplain ImageLoadingListener Listener} for image loading
	 *            process. Listener fires events on UI thread if this method is
	 *            called on UI thread.
	 * @param progressListener
	 *            {@linkplain com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener
	 *            Listener} for image loading progress. Listener fires events on
	 *            UI thread if this method is called on UI thread. Caching on
	 *            disc should be enabled in
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            options} to make this listener work.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public void loadImage(String uri, ImageSize targetImageSize, DisplayImageOptions options, ImageLoadingListener listener, ImageLoadingProgressListener progressListener)
	{
		checkConfiguration();
		if (targetImageSize == null)
		{
			targetImageSize = configuration.getMaxImageSize();
		}
		if (options == null)
		{
			options = configuration.defaultDisplayImageOptions;
		}

		ImageNonViewAware imageAware = new ImageNonViewAware(uri, targetImageSize, ViewScaleType.CROP);
		displayImage(uri, imageAware, options, listener, progressListener);
	}

	/**
	 * Loads and decodes image synchronously.<br />
	 * Default display image options
	 * {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 * from configuration} will be used.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @return Result image Bitmap. Can be <b>null</b> if image loading/decoding
	 *         was failed or cancelled.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public Bitmap loadImageSync(String uri)
	{
		return loadImageSync(uri, null, null);
	}

	/**
	 * Loads and decodes image synchronously.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and scaling. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.
	 * @return Result image Bitmap. Can be <b>null</b> if image loading/decoding
	 *         was failed or cancelled.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public Bitmap loadImageSync(String uri, DisplayImageOptions options)
	{
		return loadImageSync(uri, null, options);
	}

	/**
	 * Loads and decodes image synchronously.<br />
	 * Default display image options
	 * {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 * from configuration} will be used.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param targetImageSize
	 *            Minimal size for {@link Bitmap} which will be returned.
	 *            Downloaded image will be decoded and scaled to {@link Bitmap}
	 *            of the size which is <b>equal or larger</b> (usually a bit
	 *            larger) than incoming targetImageSize.
	 * @return Result image Bitmap. Can be <b>null</b> if image loading/decoding
	 *         was failed or cancelled.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public Bitmap loadImageSync(String uri, ImageSize targetImageSize)
	{
		return loadImageSync(uri, targetImageSize, null);
	}

	/**
	 * Loads and decodes image synchronously.<br />
	 * <b>NOTE:</b> {@link #init(ImageLoaderConfiguration)} method must be
	 * called before this method call
	 * 
	 * @param uri
	 *            Image URI (i.e. "http://site.com/image.png",
	 *            "file:///mnt/sdcard/image.png")
	 * @param targetImageSize
	 *            Minimal size for {@link Bitmap} which will be returned.
	 *            Downloaded image will be decoded and scaled to {@link Bitmap}
	 *            of the size which is <b>equal or larger</b> (usually a bit
	 *            larger) than incoming targetImageSize.
	 * @param options
	 *            {@linkplain com.nostra13.universalimageloader.core.DisplayImageOptions
	 *            Options} for image decoding and scaling. If <b>null</b> -
	 *            default display image options
	 *            {@linkplain ImageLoaderConfiguration.Builder#defaultDisplayImageOptions(DisplayImageOptions)
	 *            from configuration} will be used.
	 * @return Result image Bitmap. Can be <b>null</b> if image loading/decoding
	 *         was failed or cancelled.
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public Bitmap loadImageSync(String uri, ImageSize targetImageSize, DisplayImageOptions options)
	{
		if (options == null)
		{
			options = configuration.defaultDisplayImageOptions;
		}
		options = new DisplayImageOptions.Builder().cloneFrom(options).syncLoading(true).build();

		SyncImageLoadingListener listener = new SyncImageLoadingListener();
		loadImage(uri, targetImageSize, options, listener);
		return listener.getLoadedBitmap();
	}

	/**
	 * 检查 ImageLoader 是否初始化
	 * 
	 * @throws IllegalStateException
	 *             if configuration wasn't initialized
	 */
	private void checkConfiguration()
	{
		if (configuration == null)
		{
			throw new IllegalStateException(ERROR_NOT_INIT);
		}
	}

	/**
	 * Returns memory cache
	 * 
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public MemoryCacheAware<String, Bitmap> getMemoryCache()
	{
		checkConfiguration();
		return configuration.memoryCache;
	}

	/**
	 * Clears memory cache
	 * 
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public void clearMemoryCache()
	{
		checkConfiguration();
		configuration.memoryCache.clear();
	}

	/**
	 * Returns disc cache
	 * 
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public DiscCacheAware getDiscCache()
	{
		checkConfiguration();
		return configuration.discCache;
	}

	/**
	 * Clears disc cache.
	 * 
	 * @throws IllegalStateException
	 *             if {@link #init(ImageLoaderConfiguration)} method wasn't
	 *             called before
	 */
	public void clearDiscCache()
	{
		checkConfiguration();
		configuration.discCache.clear();
	}

	/**
	 * Returns URI of image which is loading at this moment into passed
	 * {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
	 * ImageAware}
	 */
	public String getLoadingUriForView(ImageAware imageAware)
	{
		return engine.getLoadingUriForView(imageAware);
	}

	/**
	 * Returns URI of image which is loading at this moment into passed
	 * {@link android.widget.ImageView ImageView}
	 */
	public String getLoadingUriForView(ImageView imageView)
	{
		return engine.getLoadingUriForView(new ImageViewAware(imageView));
	}

	/**
	 * Cancel the task of loading and displaying image for passed
	 * {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
	 * ImageAware}.
	 * 
	 * @param imageAware
	 *            {@link com.nostra13.universalimageloader.core.imageaware.ImageAware
	 *            ImageAware} for which display task will be cancelled
	 */
	public void cancelDisplayTask(ImageAware imageAware)
	{
		engine.cancelDisplayTaskFor(imageAware);
	}

	/**
	 * Cancel the task of loading and displaying image for passed
	 * {@link android.widget.ImageView ImageView}.
	 * 
	 * @param imageView
	 *            {@link android.widget.ImageView ImageView} for which display
	 *            task will be cancelled
	 */
	public void cancelDisplayTask(ImageView imageView)
	{
		engine.cancelDisplayTaskFor(new ImageViewAware(imageView));
	}

	/**
	 * Denies or allows ImageLoader to download images from the network.<br />
	 * <br />
	 * If downloads are denied and if image isn't cached then
	 * {@link ImageLoadingListener#onLoadingFailed(String, View, FailReason)}
	 * callback will be fired with {@link FailReason.FailType#NETWORK_DENIED}
	 * 
	 * @param denyNetworkDownloads
	 *            pass <b>true</b> - to deny engine to download images from the
	 *            network; <b>false</b> - to allow engine to download images
	 *            from network.
	 */
	public void denyNetworkDownloads(boolean denyNetworkDownloads)
	{
		engine.denyNetworkDownloads(denyNetworkDownloads);
	}

	/**
	 * Sets option whether ImageLoader will use {@link FlushedInputStream} for
	 * network downloads to handle <a
	 * href="http://code.google.com/p/android/issues/detail?id=6066">this known
	 * problem</a> or not.
	 * 
	 * @param handleSlowNetwork
	 *            pass <b>true</b> - to use {@link FlushedInputStream} for
	 *            network downloads; <b>false</b> - otherwise.
	 */
	public void handleSlowNetwork(boolean handleSlowNetwork)
	{
		engine.handleSlowNetwork(handleSlowNetwork);
	}

	/**
	 * Pause ImageLoader. All new "load&display" tasks won't be executed until
	 * ImageLoader is {@link #resume() resumed}. <br />
	 * Already running tasks are not paused.
	 */
	public void pause()
	{
		engine.pause();
	}

	/** Resumes waiting "load&display" tasks */
	public void resume()
	{
		engine.resume();
	}

	/**
	 * Cancels all running and scheduled display image tasks.<br />
	 * ImageLoader still can be used after calling this method.
	 */
	public void stop()
	{
		engine.stop();
	}

	/**
	 * {@linkplain #stop() Stops ImageLoader} and clears current configuration. <br />
	 * You can {@linkplain #init(ImageLoaderConfiguration) init} ImageLoader
	 * with new configuration after calling this method.
	 */
	public void destroy()
	{
		if (configuration != null && configuration.writeLogs)
			L.d(LOG_DESTROY);
		stop();
		engine = null;
		configuration = null;
	}

	private static Handler defineHandler(DisplayImageOptions options)
	{
		Handler handler = options.getHandler();
		if (options.isSyncLoading())
		{
			handler = null;
		}
		else if (handler == null && Looper.myLooper() == Looper.getMainLooper())
		{
			handler = new Handler();
		}
		return handler;
	}
}
