package com.nostra13.universalimageloader.cache.disc.naming;

/**
 * 根据图片的uri对应的哈希值作为文件名
 */
public class HashCodeFileNameGenerator implements FileNameGenerator {
	@Override
	public String generate(String imageUri) {
		return String.valueOf(imageUri.hashCode());
	}
}
