package com.sl.ms.carriage.handler;

import com.sl.ms.carriage.domain.constant.CarriageConstant;
import com.sl.ms.carriage.entity.CarriageEntity;
import com.sl.transport.common.util.ObjectUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 省内寄
 */
@Order(200) //定义顺序
@Component
public class SameProvinceChainHandler extends AbstractCarriageChainHandler {

	@Resource
	private CarriageTemplateRedisHelper redisHelper;

	@Override
	public CarriageEntity doHandler(WrappedWayBillDTO wrappedWayBillDTO) {
		CarriageEntity carriageEntity = null;
		// * 原则上顺序可变，所以也需要判断是否有省份ID了
		if (ObjectUtil.isNull(wrappedWayBillDTO.getReceiverProvId())) {
			redisHelper.setProvIdByCityId(wrappedWayBillDTO);
		}
		// * 获取省份ID后
		if (ObjectUtil.equals(wrappedWayBillDTO.getReceiverProvId(), wrappedWayBillDTO.getSenderProvId())) {
			carriageEntity = redisHelper.getCarriage(CarriageConstant.SAME_PROVINCE, CarriageConstant.REGULAR_FAST,
					CarriageConstant.DEFAULT_ASSOCIATED_CITY);
		}
		return doNextHandler(wrappedWayBillDTO, carriageEntity);
	}
}
