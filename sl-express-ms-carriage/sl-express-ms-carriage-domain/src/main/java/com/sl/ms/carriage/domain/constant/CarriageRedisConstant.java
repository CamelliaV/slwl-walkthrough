package com.sl.ms.carriage.domain.constant;

/**
 * @author CamelliaV
 * @since 2025/4/13 / 16:26
 */

public interface CarriageRedisConstant {
	// * 补充cityId
	String CITY_TO_PROVINCE_KEY = "carriage:city-province:{}";
	// * 补充templateType-transportType-associatedCity
	// * 关联城市不传默认1 经济区传入code 特殊跨省传入排序拼接的字符串
	String CARRIAGE_TEMPLATE_KEY = "carriage:template:{}-{}-{}";
}
