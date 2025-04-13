package com.sl.ms.carriage.handler;

import com.sl.ms.carriage.domain.constant.CarriageConstant;
import com.sl.ms.carriage.entity.CarriageEntity;
import com.sl.transport.common.util.ObjectUtil;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 特殊省份间互寄
 */
@Order(350) //定义顺序
@Component
public class ProvinceZoneChainHandler extends AbstractCarriageChainHandler {

	@Resource
	private CarriageTemplateRedisHelper redisHelper;

	@Override
	public CarriageEntity doHandler(WrappedWayBillDTO wrappedWayBillDTO) {
		CarriageEntity carriageEntity = null;

		if (ObjectUtil.isNull(wrappedWayBillDTO.getReceiverProvId())) {
			redisHelper.setProvIdByCityId(wrappedWayBillDTO);
		}

		// * 特殊省份类型 + 涉及此两个省份
		String associatedCity = null;
		if (wrappedWayBillDTO.getReceiverProvId().compareTo(wrappedWayBillDTO.getReceiverProvId()) < 0) {
			associatedCity = wrappedWayBillDTO.getReceiverProvId() + "," + wrappedWayBillDTO.getSenderProvId();
		} else {
			associatedCity = wrappedWayBillDTO.getSenderProvId() + "," + wrappedWayBillDTO.getReceiverProvId();
		}
		carriageEntity = redisHelper.getCarriage(CarriageConstant.PROVINCE_ZONE, CarriageConstant.REGULAR_FAST,
				associatedCity);

		return doNextHandler(wrappedWayBillDTO, carriageEntity);
	}
}
