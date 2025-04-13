package com.sl.ms.carriage.domain.constant;

/**
 * 运费模板常量
 */
public class CarriageConstant {
	/**
	 * 1-同城寄
	 */
	public static final Integer SAME_CITY = 1;
	/**
	 * 2-省内寄
	 */
	public static final Integer SAME_PROVINCE = 2;
	/**
	 * 3-经济区互寄
	 */
	public static final Integer ECONOMIC_ZONE = 3;
	/**
	 * 4-跨省
	 */
	public static final Integer TRANS_PROVINCE = 4;
	/**
	 * 5-特殊跨省
	 */
	public static final Integer PROVINCE_ZONE = 5;
	/**
	 * 1-默认关联城市-全国
	 */
	public static final String DEFAULT_ASSOCIATED_CITY = "1";

	/**
	 * 1-普快
	 */
	public static final Integer REGULAR_FAST = 1;

	/**
	 * 2-特快
	 */
	public static final Integer EXPRESS = 2;
}
