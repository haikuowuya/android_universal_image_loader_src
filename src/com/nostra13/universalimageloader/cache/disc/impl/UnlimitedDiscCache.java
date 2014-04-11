package com.nostra13.universalimageloader.cache.disc.impl;

import com.nostra13.universalimageloader.cache.disc.BaseDiscCache;
import com.nostra13.universalimageloader.cache.disc.DiscCacheAware;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.core.DefaultConfigurationFactory;

import java.io.File;

/**
 * 本地缓存的默认实现{@linkplain DiscCacheAware disc cache} 缓存大小没有限制
 */
public class UnlimitedDiscCache extends BaseDiscCache
{
	/***
	 * 
	 * @param cacheDir
	 *            缓存目录
	 */
	public UnlimitedDiscCache(File cacheDir)
	{
		this(cacheDir, DefaultConfigurationFactory.createFileNameGenerator());
	}

	/**
	 * @param cacheDir
	 *            缓存目录
	 * 
	 * @param fileNameGenerator
	 *            缓存文件名生成器
	 * 
	 */
	public UnlimitedDiscCache(File cacheDir, FileNameGenerator fileNameGenerator)
	{
		super(cacheDir, fileNameGenerator);
	}

	@Override
	public void put(String key, File file)
	{}
}
